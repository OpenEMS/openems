package io.openems.edge.braiinsos;

import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.types.MeterType;
import io.openems.edge.braiinsos.api.BraiinsApi;
import io.openems.edge.braiinsos.api.MinerStats;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.JSCalendarApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.energy.api.EnergySchedulable;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EshWithDifferentModes;
import io.openems.edge.energy.api.handler.RescheduleMode;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.SinglePhaseMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.BraiinsOS.Single", //
		immediate = true, //
		configurationPolicy = REQUIRE)
@EventTopics({ //
		TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
})
public class ControllerBraiinsSingleImpl extends AbstractOpenemsComponent implements Controller, ElectricityMeter,
		ControllerBraiinsSingle, OpenemsComponent, EventHandler, TimedataProvider, ComponentJsonApi, EnergySchedulable {

	private final Logger log = LoggerFactory.getLogger(ControllerBraiinsSingleImpl.class);
	private final CalculateEnergyFromPower calculateEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata;

	@Reference(cardinality = MANDATORY)
	private BridgeHttpFactory httpBridgeFactory;

	@Reference
	private ComponentManager componentManager;

	@Reference
	private ConfigurationAdmin configurationAdmin;

	private BraiinsApi braiinsApi;
	private final Supplier<BraiinsApi> braiinsApiSupplier;
	private Config config;
	private EshWithDifferentModes<Mode, EnergyScheduler.OptimizationContext, Void> energyScheduleHandler;
	private Mode lastSentMode = null;
	private CompletableFuture<Void> runFuture = null;
	private volatile JSCalendar.Tasks<Payload> tasks = JSCalendar.Tasks.empty();

	public ControllerBraiinsSingleImpl() {
		this(null);
	}

	@VisibleForTesting
	ControllerBraiinsSingleImpl(Supplier<BraiinsApi> braiinsApiSupplier) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerBraiinsSingle.ChannelId.values() //
		);

		this.braiinsApiSupplier = braiinsApiSupplier != null //
				? braiinsApiSupplier //
				: () -> new BraiinsApi(this.httpBridgeFactory, this.config.ip(), this.config.username(),
						this.config.password(), this::applyMinerStats);

		SinglePhaseMeter.calculateSingleOrAllPhaseFromActivePower(this, () -> this.config.phase());
	}

	@Activate
	protected void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.applyConfig(config);

		this.energyScheduleHandler = EnergyScheduler.buildEnergyScheduleHandler(//
				this, //
				this.componentManager, //
				this::buildEnergySchedulerConfig);
	}

	@Modified
	protected void modified(ComponentContext context, Config config) {
		super.modified(context, config.id(), config.alias(), config.enabled());
		this.applyConfig(config);
		this.energyScheduleHandler.triggerReschedule("ControllerBraiinsSingleImpl:modified()",
				RescheduleMode.OPTIMIZE_CURRENT_PERIOD);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.cleanupApi();
	}

	private EnergyScheduler.Config buildEnergySchedulerConfig() {
		if (this.config == null || !this.config.enabled()) {
			return null;
		}

		return new EnergyScheduler.Config(this.config.mode(), this.config.defaultConsumptionW(), this.tasks);
	}

	private void applyConfig(Config config) {
		this.config = config;

		this.tasks = JSCalendar.Tasks.fromStringOrEmpty(//
				this.componentManager.getClock(), config.jsCalendar(), Payload.serializer());

		this.cleanupApi();
		if (!config.enabled()) {
			return;
		}
		this.startApi();
	}

	private void cleanupApi() {
		if (this.braiinsApi == null) {
			return;
		}
		this.braiinsApi.deactivate();
		this.braiinsApi = null;
	}

	private void startApi() {
		this.braiinsApi = this.braiinsApiSupplier.get();
		this.braiinsApi.activate();
	}

	@Override
	public void run() {
		final var targetMode = this.resolveTargetMode();
		setValue(this, ControllerBraiinsSingle.ChannelId.EFFECTIVE_MODE, targetMode);
		if (targetMode == null || targetMode == this.lastSentMode) {
			return; // no changes -> stop
		}

		if (this.runFuture == null) {
			this.runFuture = switch (targetMode) {
			case ON -> this.braiinsApi.callActionResume();
			case OFF -> this.braiinsApi.callActionPause();
			};
			this.lastSentMode = targetMode;
		}

		if (this.runFuture.isDone()) {
			try {
				this.runFuture.get();
			} catch (InterruptedException | ExecutionException e) {
				// Mode change did not work -> retry
				this.logError(this.log, "Unable to set mode [" + targetMode + "]: " + e.getMessage());
				e.printStackTrace();
				this.lastSentMode = null;
			}
			this.runFuture = null;
		}
	}

	private Mode resolveTargetMode() {
		final var activeTask = this.tasks.getActiveOneTask();
		if (activeTask != null && activeTask.payload() instanceof Payload.Manual(Mode mode)) {
			return mode;
		}

		final var currentPeriod = this.energyScheduleHandler.getCurrentPeriod();
		if (currentPeriod != null) {
			return currentPeriod.mode();
		}

		return this.config.mode();
	}

	/**
	 * Regularly called by {@link BraiinsApi}.
	 *
	 * @param ms the {@link MinerStats}
	 */
	private void applyMinerStats(MinerStats ms) {
		setValue(this, ControllerBraiinsSingle.ChannelId.COMMUNICATION_FAILED, ms == null);
		setValue(this, ElectricityMeter.ChannelId.ACTIVE_POWER, ms == null ? null : ms.approximatedConsumption());
		setValue(this, ControllerBraiinsSingle.ChannelId.EFFICIENCY, ms == null ? null : ms.efficiency());
		setValue(this, ControllerBraiinsSingle.ChannelId.REAL_HASHRATE_LAST_15S,
				ms == null ? null : ms.realHashRateLast15s());
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
			-> this.calculateEnergy();
		}
	}

	/**
	 * Calculate the Energy values from ActivePower.
	 */
	private void calculateEnergy() {
		final var activePower = this.getActivePower().get();
		if (activePower == null) {
			this.calculateEnergy.update(null);
		} else if (activePower >= 0) {
			this.calculateEnergy.update(activePower);
		} else {
			this.calculateEnergy.update(0);
		}
	}

	@Override
	public String debugLog() {
		return new StringBuilder() //
				.append("L:").append(this.getActivePower()) //
				.append("|Efficiency:").append(this.channel(ControllerBraiinsSingle.ChannelId.EFFICIENCY).value()) //
				.append("|Hashrate:")
				.append(this.channel(ControllerBraiinsSingle.ChannelId.REAL_HASHRATE_LAST_15S).value()) //
				.toString();
	}

	@Override
	public MeterType getMeterType() {
		return this.config.type();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public EnergyScheduleHandler getEnergyScheduleHandler() {
		return this.energyScheduleHandler;
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		JSCalendarApi.buildJsonApiRoutes(builder, Payload.serializer(), //
				() -> this.tasks, //
				() -> new JSCalendarApi.UpdateJsCalendarRecord(this.configurationAdmin, this.componentManager,
						this.servicePid(), "jsCalendar"));
	}
}
