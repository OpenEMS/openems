package io.openems.edge.controller.evse.single;

import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE;

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
import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.JSCalendarApi;
import io.openems.edge.common.jsonapi.JSCalendarApi.UpdateJsCalendarRecord;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.evse.single.Types.History;
import io.openems.edge.controller.evse.single.Types.Payload;
import io.openems.edge.controller.evse.single.statemachine.Context;
import io.openems.edge.controller.evse.single.statemachine.StateMachine;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
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
@GenerateTargetsFromReferences({ "chargePoint", "electricVehicle" })
public class ControllerEvseSingleImpl extends AbstractOpenemsComponent
		implements Controller, ControllerEvseSingle, OpenemsComponent, EventHandler, ComponentJsonApi {

	private final Logger log = LoggerFactory.getLogger(ControllerEvseSingleImpl.class);
	private final StateMachine stateMachine = new StateMachine(EvseSingleState.UNDEFINED);
	private final SessionEnergyHandler sessionEnergyHandler = new SessionEnergyHandler();
	private final History history = new History();

	@Reference
	private ComponentManager componentManager;

	@Reference
	private ConfigurationAdmin cm;

	@Reference(target = "(&(id=${config.chargePoint_id})(enabled=true))")
	private EvseChargePoint chargePoint;

	// TODO Optional Reference
	@Reference(target = "(&(id=${config.electricVehicle_id})(enabled=true))")
	private EvseElectricVehicle electricVehicle;

	private Config config;
	private JSCalendar.Tasks<Payload> tasks;
	private BiConsumer<Value<Boolean>, Value<Boolean>> onChargePointIsReadyForChargingChange = null;

	public ControllerEvseSingleImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerEvseSingle.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.applyConfig(config);
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		super.modified(context, config.id(), config.alias(), config.enabled());
		this.applyConfig(config);
	}

	private synchronized void applyConfig(Config config) {
		this.config = config;
		this.tasks = JSCalendar.Tasks.fromStringOrEmpty(this.componentManager.getClock(), config.jsCalendar(),
				Payload.serializer());

		if (!config.enabled()) {
			return;
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
		// TODO announce Cluster
		// this.eshEvseSingle.onChargePointIsReadyForChargingChange(before, after);

		// Set AppearsToBeFullyCharged false
		this.history.unsetAppearsToBeFullyCharged();
	}

	@Override
	public Params getParams() {
		final boolean isSessionLimitReached = this.stateMachine
				.getCurrentState() == EvseSingleState.FINISHED_ENERGY_SESSION_LIMIT;
		final var chargePointAbilities = this.chargePoint.getChargePointAbilities();
		final var activePower = this.chargePoint.getActivePower().get();

		final var sessionEnergy = this.getSessionEnergy().orElse(0);
		final var sessionEnergyLimit = this.config.manualEnergySessionLimit() < 1 //
				? null // No Session Energy Limit configured
				: this.config.manualEnergySessionLimit();

		final var electricVehicleAbilities = this.electricVehicle.getElectricVehicleAbilities();
		final var combinedAbilities = CombinedAbilities.createFrom(chargePointAbilities, electricVehicleAbilities) //
				.setIsReadyForCharging(!isSessionLimitReached) //
				.build();

		return new Params(this.id(), this.config.chargePoint_id(), this.config.mode(), activePower, //
				sessionEnergy, sessionEnergyLimit, //
				this.history, this.stateMachine.getCurrentState(), this.config.phaseSwitching(), combinedAbilities,
				this.tasks);
	}

	@Override
	public void run() throws OpenemsNamedException {
		// Actual logic is carried out in the EVSE Cluster
	}

	@Override
	public void apply(Mode mode, ChargePointActions input) {
		// Set ACTUAL_MODE Channel. Always ZERO if there is no ActivePower
		final var activePower = this.chargePoint.getActivePower().get();
		setValue(this, ControllerEvseSingle.ChannelId.ACTUAL_MODE, //
				activePower != null && activePower == 0 //
						? Mode.ZERO //
						: mode);

		final var state = this.stateMachine.getCurrentState();
		setValue(this, ControllerEvseSingle.ChannelId.STATE_MACHINE, state);

		if (mode != Mode.SURPLUS) {
			this.history.setLastChargeStateChangeTriggeredBySurplus(null);
		}

		try {
			var context = new Context(this, this.componentManager.getClock(), mode, input, this.chargePoint,
					this.history, this.config.phaseSwitching(), this.isSessionLimitReached(), actions -> {
						if (this.chargePoint.isReadOnly()) {
							return;
						}

						// Callback: forward actions
						this.chargePoint.apply(actions);
						this.addHistoryEntry(actions);
					}, //
					b -> setValue(this, ControllerEvseSingle.ChannelId.PHASE_SWITCH_FAILED, b));

			this.stateMachine.run(context);
			this._setRunFailed(false);

		} catch (OpenemsNamedException e) {
			this._setRunFailed(true);
			this.logError(this.log, "StateMachine failed: " + e.getMessage());
		}
	}

	void addHistoryEntry(ChargePointActions actions) {
		final var setPointInWatt = actions.abilities().applySetPoint().toPower(actions.applySetPoint().value());
		final var idealSetPoint = actions.idealSetPointInWatt() == null ? setPointInWatt
				: actions.idealSetPointInWatt();
		this.history.addEntry(Instant.now(this.componentManager.getClock()), this.chargePoint.getActivePower().get(),
				setPointInWatt, idealSetPoint, actions.abilities().isReadyForCharging());
	}

	private boolean isSessionLimitReached() {
		final var energy = this.getSessionEnergy().get();
		final var limit = this.config.manualEnergySessionLimit();

		return energy != null && limit > 0 && energy >= limit;
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

	@Override
	public String debugLog() {
		return switch (this.config.logVerbosity()) {
		case NONE -> null;
		case DEBUG_LOG -> new StringBuilder() //
				.append("Mode:")
				.append(this.channel(ControllerEvseSingle.ChannelId.ACTUAL_MODE).value().asOptionString()) //
				.append("|").append(this.stateMachine.debugLog()) //
				.toString();
		};
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		JSCalendarApi.buildJsonApiRoutes(builder, Payload.serializer(), //
				() -> this.tasks, //
				() -> new UpdateJsCalendarRecord(this.cm, this.componentManager, this.servicePid(), "jsCalendar"));
	}
}
