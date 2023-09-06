package io.openems.edge.energy;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
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
import io.openems.edge.scheduler.api.Scheduler;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Energy", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class EnergyImpl extends AbstractOpenemsComponent implements OpenemsComponent {

	private final ScheduledExecutorService taskExecutor = Executors.newSingleThreadScheduledExecutor();
	private final ScheduledExecutorService triggerExecutor = Executors.newSingleThreadScheduledExecutor();

	@Reference
	protected ComponentManager componentManager;

//	@Reference
//	protected PredictorManager predictor;
//
//	@Reference
//	protected TimeOfUseTariff timeOfUseTariff;

	@Reference
	protected Scheduler scheduler;

	private ManualTask task;

	public EnergyImpl() throws OpenemsNamedException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Energy.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		if (!this.isEnabled()) {
			return;
		}

		this.task = switch (config.mode()) {
		case OFF -> null;
		case MANUAL -> new ManualTask(this.componentManager, config.manualSchedule());
		case SMART -> null; // TODO
		};

		if (this.task == null) {
			return;
		}

		// Run Worker:
		// - once now (in 5 seconds)
		// - on next full 15 minutes
		// - afterwards every 15 minutes
		final AtomicReference<Future<?>> future = new AtomicReference<>();
		var now = ZonedDateTime.now();
		var nowRoundedDown = DateUtils.roundZonedDateTimeDownToMinutes(now, 15);
		var nextQuarter = nowRoundedDown.plusMinutes(15);
		var durationTillNextQuarter = Duration.between(now, nextQuarter).toMillis();
		Runnable taskSingleton = () -> {
			// Cancel previous run
			Optional.ofNullable(future.get()).ifPresent(f -> f.cancel(true));
			future.set(this.taskExecutor.submit(this.task));
		};

		if (durationTillNextQuarter > 60_000 /* 1 minute */) {
			// Wait for Controllers to become available
			this.triggerExecutor.schedule(taskSingleton, 5, TimeUnit.SECONDS);
		}

		this.triggerExecutor.scheduleAtFixedRate(taskSingleton,
				// Wait till next full 15 minutes
				durationTillNextQuarter, //
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
