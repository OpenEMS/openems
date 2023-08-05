package io.openems.edge.energy;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.DateUtils;
import io.openems.common.utils.ThreadPoolUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.predictor.api.manager.PredictorManager;
import io.openems.edge.scheduler.api.Scheduler;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Energy", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class EnergyImpl extends AbstractOpenemsComponent implements OpenemsComponent {

	private final ScheduledExecutorService taskExecutor = Executors.newSingleThreadScheduledExecutor();
	private final ScheduledExecutorService triggerExecutor = Executors.newSingleThreadScheduledExecutor();
	private final Task task = new Task(this);

	@Reference
	protected ComponentManager componentManager;

	// @Reference(target = "(id=ctrlFixActivePower0)")
	// protected EssFixActivePower ctrlFixActivePower0;

	// @Reference(target = "(id=ctrlEvcs0)")
	// protected EvcsController ctrlEvcs0;

	@Reference
	protected PredictorManager predictor;

	@Reference
	protected TimeOfUseTariff timeOfUseTariff;

	@Reference
	protected Scheduler scheduler;

	// private Config config;

	public EnergyImpl() throws OpenemsNamedException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Energy.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		// this.config = config;
		if (!this.isEnabled()) {
			return;
		}

		/*
		 * Run Worker once now and afterwards every 15 minutes
		 */
		final AtomicReference<Future<?>> future = new AtomicReference<>();
		future.set(this.taskExecutor.submit(this.task));

		var now = ZonedDateTime.now();
		var nowRoundedDown = DateUtils.roundZonedDateTimeDownToMinutes(now, 15);
		var nextQuarter = nowRoundedDown.plusMinutes(15);

		this.triggerExecutor.scheduleAtFixedRate(//
				() -> {
					// Cancel previous run
					future.get().cancel(true);
					future.set(this.taskExecutor.submit(this.task));
				}, //

				// Wait till next full 15 minutes
				Duration.between(now, nextQuarter).toMillis(), //
				// then execute every 15 minutes
				Duration.of(15, ChronoUnit.MINUTES).toMillis(), TimeUnit.MILLISECONDS);
	}

	@Deactivate
	protected void deactivate() {
		ThreadPoolUtils.shutdownAndAwaitTermination(this.taskExecutor, 0);
		ThreadPoolUtils.shutdownAndAwaitTermination(this.triggerExecutor, 0);
		super.deactivate();
	}

	@Override
	public String debugLog() {
		return this.task.debugLog();
	}

}
