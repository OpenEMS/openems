package io.openems.edge.controller.evse.single;

import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE;
import static io.openems.edge.controller.evse.single.Utils.isSessionLimitReached;
import static io.openems.edge.controller.evse.single.Utils.parseTasksConfig;
import static io.openems.edge.controller.evse.single.Utils.serializeTasksConfig;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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

import io.openems.common.OpenemsConstants;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jscalendar.AddTask;
import io.openems.common.jscalendar.DeleteTask;
import io.openems.common.jscalendar.GetAllTasks;
import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.jscalendar.JSCalendar.Tasks;
import io.openems.common.jscalendar.UpdateTask;
import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.EdgeKeys;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.user.User;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.evse.single.Types.History;
import io.openems.edge.controller.evse.single.Types.Payload;
import io.openems.edge.controller.evse.single.statemachine.Context;
import io.openems.edge.controller.evse.single.statemachine.StateMachine;
import io.openems.edge.controller.evse.single.statemachine.StateMachine.State;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Mode;
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
public class ControllerEvseSingleImpl extends AbstractOpenemsComponent
		implements Controller, ControllerEvseSingle, OpenemsComponent, EventHandler, ComponentJsonApi {

	private final Logger log = LoggerFactory.getLogger(ControllerEvseSingleImpl.class);
	private final StateMachine stateMachine = new StateMachine(State.UNDEFINED);
	private final SessionEnergyHandler sessionEnergyHandler = new SessionEnergyHandler();
	private final History history = new History();

	@Reference
	private ComponentManager componentManager;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private EvseChargePoint chargePoint;

	// TODO Optional Reference
	@Reference
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
		this.tasks = parseTasksConfig(config.jsCalendar());

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

		// Listen on changes to 'isReadyForCharging'
		this.chargePoint.getIsReadyForChargingChannel().onChange(this::onChargePointIsReadyForChargingChange);

		// Reset StateMachine
		this.stateMachine.forceNextState(State.UNDEFINED);
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
				.getCurrentState() == State.FINISHED_ENERGY_SESSION_LIMIT;
		final var chargePointAbilities = this.chargePoint.getChargePointAbilities();
		final var activePower = this.chargePoint.getActivePower().get();
		final var sessionEnergy = this.getSessionEnergy().orElse(0);
		final var electricVehicleAbilities = this.electricVehicle.getElectricVehicleAbilities();
		final var combinedAbilities = CombinedAbilities.createFrom(chargePointAbilities, electricVehicleAbilities) //
				.setIsReadyForCharging(!isSessionLimitReached) //
				.build();

		return new Params(this.id(), this.config.mode(), activePower, sessionEnergy,
				this.config.manualEnergySessionLimit(), this.history, this.config.phaseSwitching(), combinedAbilities,
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

		final State forceNextState = this.getForceNextState(input, state);
		if (forceNextState != null && state != forceNextState) {
			this.stateMachine.forceNextState(forceNextState);
		}

		try {
			var context = new Context(this, this.componentManager.getClock(), input, this.chargePoint, this.history,
					(actions) -> {
						// Callback: forward actions
						this.chargePoint.apply(actions);
						this.history.addEntry(Instant.now(this.componentManager.getClock()),
								this.chargePoint.getActivePower().get(),
								actions.abilities().applySetPoint().toPower(actions.applySetPoint().value()),
								actions.abilities().isReadyForCharging());
					}, //
					b -> setValue(this, ControllerEvseSingle.ChannelId.PHASE_SWITCH_FAILED, b));

			this.stateMachine.run(context);
			this._setRunFailed(false);

		} catch (OpenemsNamedException e) {
			this._setRunFailed(true);
			this.logError(this.log, "StateMachine failed: " + e.getMessage());
		}
	}

	private State getForceNextState(ChargePointActions input, State state) {
		// Force State when...
		return switch (input.phaseSwitch()) {
		// NOTE: this is before EV_NOT_CONNECTED to allow phase-switching with
		// not-connected EVs
		case TO_SINGLE_PHASE -> {
			// ...phase switching to Single-Phase
			yield State.PHASE_SWITCH_TO_SINGLE_PHASE;
		}
		case TO_THREE_PHASE -> {
			// ...phase switching to Three-Phase
			yield State.PHASE_SWITCH_TO_THREE_PHASE;
		}
		case null -> {
			if (state == State.PHASE_SWITCH_TO_SINGLE_PHASE || state == State.PHASE_SWITCH_TO_THREE_PHASE) {
				yield null; // Do not interrupt Phase-Switch; it has a timeout
			}
			if (state != State.EV_NOT_CONNECTED && !input.abilities().isEvConnected()) {
				// ...EV is not connected
				yield State.EV_NOT_CONNECTED;
			}
			if (state != State.FINISHED_ENERGY_SESSION_LIMIT && isSessionLimitReached(this.config.mode(),
					this.getSessionEnergy().get(), this.config.manualEnergySessionLimit())) {
				// ...Session Energy Limit was reached
				yield State.FINISHED_ENERGY_SESSION_LIMIT;
			}
			yield null;
		}
		};
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
		builder.handleRequest(new AddTask<Payload>(Payload.serializer()), call -> {
			var newTask = call.getRequest().task();
			var updatedTasks = this.tasks.withAddedTask(newTask);
			this.updateJsCalendar(updatedTasks, call.get(EdgeKeys.USER_KEY));
			return new AddTask.Response(newTask.uid());
		});

		builder.handleRequest(new UpdateTask<Payload>(Payload.serializer()), call -> {
			var updatedTask = call.getRequest().task();
			var updatedTasks = this.tasks.withUpdatedTask(updatedTask);
			this.updateJsCalendar(updatedTasks, call.get(EdgeKeys.USER_KEY));
			return EmptyObject.INSTANCE;
		});

		builder.handleRequest(new DeleteTask(), call -> {
			var uidToRemove = call.getRequest().uid();
			var updatedTasks = this.tasks.withRemovedTask(uidToRemove);
			this.updateJsCalendar(updatedTasks, call.get(EdgeKeys.USER_KEY));
			return EmptyObject.INSTANCE;
		});

		builder.handleRequest(new GetAllTasks<Payload>(Payload.serializer()), call -> {
			return new GetAllTasks.Response<Payload>(this.tasks.tasks);
		});
	}

	private void updateJsCalendar(Tasks<Payload> tasks, User user) {
		try {
			var config = this.cm.getConfiguration(this.servicePid(), "?");
			var properties = config.getProperties();

			properties.put("jsCalendar", serializeTasksConfig(tasks));

			var lastChangeBy = (user != null) //
					? user.getId() + ": " + user.getName() //
					: "UNDEFINED";
			properties.put(OpenemsConstants.PROPERTY_LAST_CHANGE_BY, lastChangeBy);
			properties.put(OpenemsConstants.PROPERTY_LAST_CHANGE_AT,
					LocalDateTime.now(this.componentManager.getClock()).truncatedTo(ChronoUnit.SECONDS).toString());

			config.update(properties);
		} catch (IOException e) {
			this.logError(this.log, e.getMessage());
		}
	}
}
