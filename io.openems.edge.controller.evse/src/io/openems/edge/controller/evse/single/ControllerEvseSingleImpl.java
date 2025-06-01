package io.openems.edge.controller.evse.single;

import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE;
import static io.openems.edge.controller.evse.single.EnergyScheduler.buildManualEnergyScheduleHandler;
import static io.openems.edge.controller.evse.single.EnergyScheduler.buildSmartEnergyScheduleHandler;
import static io.openems.edge.controller.evse.single.Utils.getSessionLimitReached;
import static io.openems.edge.controller.evse.single.Utils.mergeLimits;

import java.time.Clock;
import java.time.Instant;
import java.util.function.BiConsumer;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.evse.single.EnergyScheduler.Config.ManualOptimizationContext;
import io.openems.edge.controller.evse.single.EnergyScheduler.Config.SmartOptimizationConfig;
import io.openems.edge.controller.evse.single.EnergyScheduler.ScheduleContext;
import io.openems.edge.controller.evse.single.EnergyScheduler.SmartOptimizationContext;
import io.openems.edge.controller.evse.single.Types.History;
import io.openems.edge.controller.evse.single.Types.Hysteresis;
import io.openems.edge.controller.evse.single.jsonrpc.GetSchedule;
import io.openems.edge.energy.api.EnergySchedulable;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EshWithDifferentModes;
import io.openems.edge.energy.api.handler.EshWithOnlyOneMode;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Mode;
import io.openems.edge.evse.api.chargepoint.Mode.Actual;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.api.electricvehicle.EvseElectricVehicle;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evse.Controller.Single", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class ControllerEvseSingleImpl extends AbstractOpenemsComponent implements Controller, ControllerEvseSingle,
		EnergySchedulable, OpenemsComponent, ComponentJsonApi, EventHandler {

	private final Logger log = LoggerFactory.getLogger(ControllerEvseSingleImpl.class);
	private final SessionEnergyHandler sessionEnergyHandler = new SessionEnergyHandler();
	private final History history = new History();

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private EvseChargePoint chargePoint;

	// TODO Optional Reference
	@Reference
	private EvseElectricVehicle electricVehicle;

	private Config config;
	private BiConsumer<Value<Boolean>, Value<Boolean>> onChargePointIsReadyForChargingChange = null;
	private EshWithDifferentModes<Actual, SmartOptimizationContext, ScheduleContext> smartEnergyScheduleHandler = null;
	private EshWithOnlyOneMode<ManualOptimizationContext, ScheduleContext> manualEnergyScheduleHandler = null;

	public ControllerEvseSingleImpl() {
		this(Clock.systemDefaultZone());
	}

	protected ControllerEvseSingleImpl(Clock clock) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerEvseSingle.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.applyConfig(config);
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		super.modified(context, config.id(), config.alias(), config.enabled());
		this.applyConfig(config);
	}

	private synchronized void applyConfig(Config config) {
		this.config = config;

		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "chargePoint",
				config.chargePoint_id())) {
			return;
		}
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "electricVehicle",
				config.electricVehicle_id())) {
			return;
		}
		if (!config.enabled()) {
			return;
		}

		switch (config.mode()) {
		case SMART -> {
			this.manualEnergyScheduleHandler = null;
			this.smartEnergyScheduleHandler = buildSmartEnergyScheduleHandler(this, //
					() -> SmartOptimizationConfig.from(//
							this.chargePoint.getChargeParams(), this.electricVehicle.getChargeParams(), //
							this.history.getAppearsToBeFullyCharged(), //
							config.smartConfig()));
		}

		case ZERO, MINIMUM, SURPLUS, FORCE -> {
			this.smartEnergyScheduleHandler = null;
			this.manualEnergyScheduleHandler = buildManualEnergyScheduleHandler(this, //
					() -> ManualOptimizationContext.from(//
							config.mode().actual, //
							this.chargePoint.getChargeParams(), this.electricVehicle.getChargeParams(), //
							this.history.getAppearsToBeFullyCharged(), //
							this.getSessionEnergy().orElse(0), //
							config.manualEnergySessionLimit() > 0 //
									? config.manualEnergySessionLimit() //
									: 30_000 /* fallback */));
		}
		}

		// Listen on changes to 'isReadyForCharging'
		this.chargePoint.getIsReadyForChargingChannel().onChange(this::onChargePointIsReadyForChargingChange);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		this.chargePoint.getIsReadyForChargingChannel()
				.removeOnChangeCallback(this.onChargePointIsReadyForChargingChange);
		super.deactivate();
	}

	private synchronized void onChargePointIsReadyForChargingChange(Value<Boolean> before, Value<Boolean> after) {
		if (this.smartEnergyScheduleHandler != null) {
			// Trigger Reschedule on change of IS_READY_FOR_CHARGING
			this.smartEnergyScheduleHandler
					.triggerReschedule("ControllerEvseSingle::onChargePointIsReadyForChargingChange from [" + before
							+ "] to [" + after + "]");
		}

		// Set AppearsToBeFullyCharged false
		this.history.unsetAppearsToBeFullyCharged();
	}

	@Override
	public Params getParams() {
		// Handle Manual Energy Session Limit
		final boolean sessionLimitReached = getSessionLimitReached(this.config.mode(), this.getSessionEnergy().get(),
				this.config.manualEnergySessionLimit());
		setValue(this, ControllerEvseSingle.ChannelId.SESSION_LIMIT_REACHED, sessionLimitReached);

		// Evaluate Actual Mode
		final var actualMode = switch (this.config.mode()) {
		case ZERO, MINIMUM, SURPLUS, FORCE //
			-> sessionLimitReached //
					? Mode.Actual.ZERO //
					: this.config.mode().actual;

		case SMART //
			-> this.getSmartModeActual(Mode.Actual.ZERO);
		};

		final var chargePoint = this.chargePoint.getChargeParams();
		final var activePower = this.chargePoint.getActivePower().get();
		final var electricVehicle = this.electricVehicle.getChargeParams();

		// Set ACTUAL_MODE Channel. Always ZERO if there is no ActivePower
		setValue(this, ControllerEvseSingle.ChannelId.ACTUAL_MODE, //
				activePower != null && activePower == 0 //
						? Mode.Actual.ZERO //
						: actualMode);

		var limits = mergeLimits(chargePoint, electricVehicle);
		if (limits == null) {
			return null;
		}

		// Is Ready for Charging?
		var isReadyForCharging = sessionLimitReached //
				? false //
				: chargePoint.isReadyForCharging();

		var hysteresis = Hysteresis.from(this.history);
		var appearsToBeFullyCharged = this.history.getAppearsToBeFullyCharged();

		return new Params(isReadyForCharging, actualMode, activePower, limits, hysteresis, appearsToBeFullyCharged,
				chargePoint.abilities());
	}

	@Override
	public void run() throws OpenemsNamedException {
		// Actual logic is carried out in the EVSE Cluster
	}

	@Override
	public void apply(ChargePointActions actions) {
		this.chargePoint.apply(actions);

		this.history.addEntry(Instant.now(), this.chargePoint.getActivePower().get(), actions.applySetPoint().value());
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
			-> this._setSessionEnergy(this.sessionEnergyHandler.onBeforeProcessImage(this.chargePoint));
		case TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
			-> this.sessionEnergyHandler.onAfterProcessImage(this.chargePoint);
		}
	}

	private Mode.Actual getSmartModeActual(Mode.Actual orElse) {
		var esh = this.smartEnergyScheduleHandler;
		if (esh == null) {
			return orElse;
		}
		var period = esh.getCurrentPeriod();
		if (period == null) {
			return orElse;
		}
		return period.mode();
	}

	protected void logDebug(String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}

	@Override
	public EnergyScheduleHandler getEnergyScheduleHandler() {
		var esh = this.smartEnergyScheduleHandler;
		if (esh != null) {
			return esh;
		}
		return this.manualEnergyScheduleHandler;
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.handleRequest(new GetSchedule(), call -> {
			return GetSchedule.Response.create(this.smartEnergyScheduleHandler, this.manualEnergyScheduleHandler);
		});
	}
}
