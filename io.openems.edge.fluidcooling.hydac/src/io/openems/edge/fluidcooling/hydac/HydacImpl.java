package io.openems.edge.fluidcooling.hydac;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_2;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.startstop.StartStop.START;
import static io.openems.edge.common.startstop.StartStop.STOP;
import static io.openems.edge.common.startstop.StartStop.UNDEFINED;
import static io.openems.edge.common.startstop.StartStoppable.ChannelId.START_STOP;
import static io.openems.edge.fluidcooling.hydac.Hydac.ChannelId.MANUAL_MODE_ACTIVATED;
import static io.openems.edge.fluidcooling.hydac.Hydac.ChannelId.RUN_FAILED;
import static io.openems.edge.fluidcooling.hydac.Hydac.ChannelId.STATE_MACHINE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.common.session.Role;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC1ReadCoilsTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC5WriteCoilTask;
import io.openems.edge.common.channel.ChannelUtils;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.EdgeGuards;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.fluidcooling.hydac.statemachine.Context;
import io.openems.edge.fluidcooling.hydac.statemachine.StateMachine;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "FluidCooling.Hydac", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
@GenerateTargetsFromReferences("Modbus")
public class HydacImpl extends AbstractOpenemsModbusComponent
		implements Hydac, OpenemsComponent, ModbusComponent, StartStoppable, EventHandler, ComponentJsonApi {

	private static final int MODBUS_UNIT_ID = 1;

	private final Logger log = LoggerFactory.getLogger(HydacImpl.class);
	private final StateMachine stateMachine = new StateMachine(StateMachine.State.WAIT_FOR_TARGET);

	private Config config;
	private StartStop startStopTarget = StartStop.UNDEFINED;
	private HydacErrorHandler errorHandler;

	@Override
	@Reference(//
			policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, //
			target = "(&(id=${config.modbus_id})(enabled=true))" //
	)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public HydacImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				Hydac.ChannelId.values()//
		);
	}

	@Reference
	private ComponentManager componentManager;

	@Reference
	private ConfigurationAdmin cm;

	@Activate
	protected void activate(ComponentContext context, Config config) {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled(), MODBUS_UNIT_ID);
		this.errorHandler = new HydacErrorHandler(this, this.componentManager.getClock());
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> {
			this.handleStateMachine();
			this.updateCoolingTemperatureLimits();
			this.errorHandler.update();
			// If not set to AUTO, set info. Cooling should be managed by a superior system.
			setValue(this, MANUAL_MODE_ACTIVATED, this.config.startStop() != StartStopConfig.AUTO);
		}
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC4ReadInputRegistersTask(1020, Priority.LOW, //
						m(Hydac.ChannelId.OUTLET_TEMPERATURE, new SignedWordElement(1020), SCALE_FACTOR_MINUS_2), //
						m(Hydac.ChannelId.PUMP_OUTLET_TEMPERATURE, new SignedWordElement(1021), SCALE_FACTOR_MINUS_2), //
						new DummyRegisterElement(1022), //
						m(Hydac.ChannelId.COOLING_CIRCUIT_1_FLOW, new UnsignedWordElement(1023), SCALE_FACTOR_1), //
						m(Hydac.ChannelId.COOLING_CIRCUIT_1_CONDUCTANCE, new UnsignedWordElement(1024),
								SCALE_FACTOR_MINUS_1), //
						m(Hydac.ChannelId.AMBIENT_TEMPERATURE, new UnsignedWordElement(1025), SCALE_FACTOR_MINUS_2), //
						new DummyRegisterElement(1026, 1029), //
						m(new BitsWordElement(1030, this) //
								.bit(0, Hydac.ChannelId.CHILLER_READY) //
								.bit(1, Hydac.ChannelId.COOLING_REQUIRED) //
								.bit(2, Hydac.ChannelId.CHILLER_RUNNING) //
								.bit(3, Hydac.ChannelId.WARNING_ACTIVE) //
								.bit(4, Hydac.ChannelId.ALARM_ACTIVE) //
								.bit(5, Hydac.ChannelId.SYSTEM_ERROR) //
								.bit(6, Hydac.ChannelId.GATEWAY_ERROR) //
						), //
						m(new BitsWordElement(1031, this) //
								.bit(0, Hydac.ChannelId.COMPRESSOR) //
								.bit(1, Hydac.ChannelId.PUMP) //
								.bit(2, Hydac.ChannelId.HEATER) //
						), //
						m(new BitsWordElement(1032, this) //
								.bit(0, Hydac.ChannelId.DRY_RUN) //
								.bit(2, Hydac.ChannelId.UNDER_TEMPERATURE_ALARM) //
								.bit(3, Hydac.ChannelId.OVER_TEMPERATURE_ALARM) //
						), //
						m(new BitsWordElement(1033, this) //
								.bit(0, Hydac.ChannelId.WATER_LEVEL_WARNING) //
								.bit(2, Hydac.ChannelId.UNDER_TEMPERATURE_WARNING) //
								.bit(3, Hydac.ChannelId.OVER_TEMPERATURE_WARNING) //
						), //
						m(new BitsWordElement(1034, this) //
								.bit(0, Hydac.ChannelId.HIGH_PRESSURE_ALARM) //
								.bit(1, Hydac.ChannelId.LOW_PRESSURE_ALARM) //
						), //
						m(new BitsWordElement(1035, this) //
								.bit(0, Hydac.ChannelId.SENSOR_ERROR) //
								.bit(1, Hydac.ChannelId.INVERTER_ERROR_COMPRESSOR) //
								.bit(2, Hydac.ChannelId.MOTOR_PROTECTION_SWITCH_PUMP) //
								.bit(5, Hydac.ChannelId.FAN_ERROR) //
						) //
				), //
				new FC3ReadRegistersTask(2000, Priority.LOW, //
						m(Hydac.ChannelId.COOLING_CIRCUIT_1_SET_POINT, new SignedWordElement(2000),
								SCALE_FACTOR_MINUS_2), //
						m(Hydac.ChannelId.COOLING_CIRCUIT_1_SET_POINT_OFFSET, new UnsignedWordElement(2001),
								SCALE_FACTOR_MINUS_2), //
						m(Hydac.ChannelId.COOLING_CIRCUIT_1_FLOW_ALARM_SETPOINT, new UnsignedWordElement(2002),
								SCALE_FACTOR_MINUS_2), //
						m(Hydac.ChannelId.COOLING_CIRCUIT_1_CONDUCTANCE_SETPOINT, new UnsignedWordElement(2003),
								SCALE_FACTOR_MINUS_2), //
						m(Hydac.ChannelId.COOLING_CIRCUIT_1_CONTROL_MODE, new UnsignedWordElement(2004)), //
						new DummyRegisterElement(2005, 2010), //
						m(Hydac.ChannelId.COOLING_CIRCUIT_1_SET_POINT_MIN, new UnsignedWordElement(2011),
								SCALE_FACTOR_MINUS_2), //
						m(Hydac.ChannelId.COOLING_CIRCUIT_1_SET_POINT_MAX, new UnsignedWordElement(2012),
								SCALE_FACTOR_MINUS_2) //
				), //

				new FC16WriteRegistersTask(2000, //
						m(Hydac.ChannelId.COOLING_CIRCUIT_1_SET_POINT, new UnsignedWordElement(2000),
								SCALE_FACTOR_MINUS_2), //
						m(Hydac.ChannelId.COOLING_CIRCUIT_1_SET_POINT_OFFSET, new UnsignedWordElement(2001))//
				), //

				new FC16WriteRegistersTask(2011, //
						m(Hydac.ChannelId.COOLING_CIRCUIT_1_SET_POINT_MIN, new UnsignedWordElement(2011)), //
						m(Hydac.ChannelId.COOLING_CIRCUIT_1_SET_POINT_MAX, new UnsignedWordElement(2012))//
				), //

				new FC1ReadCoilsTask(4100, Priority.LOW, //
						m(Hydac.ChannelId.CHILLER_ON, new CoilElement(4100))), //
				new FC1ReadCoilsTask(4101, Priority.LOW, //
						m(Hydac.ChannelId.CHILLER_OFF, new CoilElement(4101))), //
				new FC1ReadCoilsTask(4103, Priority.LOW, //
						m(Hydac.ChannelId.CLEAR_ERROR, new CoilElement(4103))), //

				new FC5WriteCoilTask(4100, //
						m(Hydac.ChannelId.CHILLER_ON, new CoilElement(4100))), //
				new FC5WriteCoilTask(4101, //
						m(Hydac.ChannelId.CHILLER_OFF, new CoilElement(4101))), //
				new FC5WriteCoilTask(4103, //
						m(Hydac.ChannelId.CLEAR_ERROR, new CoilElement(4103)))//
		);
	}

	private void handleStateMachine() {
		final var currentState = this.stateMachine.getCurrentState();
		setValue(this, STATE_MACHINE, currentState);

		// Initialize 'Start-Stop' Channel
		setValue(this, START_STOP, UNDEFINED);

		try {
			var context = new Context(this, this.componentManager.getClock());
			this.stateMachine.run(context);
			setValue(this, RUN_FAILED, false);
		} catch (RuntimeException | OpenemsNamedException e) {
			setValue(this, RUN_FAILED, true);
			this.logError(this.log, "StateMachine failed: " + e.getMessage());
		}
	}

	private void updateCoolingTemperatureLimits() {
		try {
			ChannelUtils.setWriteValueIfNotRead((IntegerWriteChannel) this.getCoolingCircuit1SetPointChannel(),
					(Integer) (this.config.temperatureSetpoint() / 100));
		} catch (OpenemsNamedException e) {
			this.logWarn(this.log, "The cooling unit temperatures can not be updated" + e.getMessage());
		}
	}

	@Override
	public StartStop getStartStopTarget() {
		return switch (this.config.startStop()) {
		case AUTO -> this.startStopTarget;
		case START -> START;
		case STOP -> STOP;
		};
	}

	@Override
	public void setStartStop(StartStop value) {
		this.startStopTarget = value;
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.handleRequest(EndpointRequestType.ofEmpty("clearErrorRequest"),
				endpoint -> endpoint.setGuards(EdgeGuards.roleIsAtleast(Role.INSTALLER)), //
				call -> {
					this.clearCoolingUnitErrors();
					return EmptyObject.INSTANCE;
				});
	}

	protected void clearCoolingUnitErrors() {
		this.setClearError(true);
		this.logInfo(this.log, "Clearing Hydac Errors.");
		CompletableFuture.runAsync(() -> {
			this.setClearError(false); //
			this.logInfo(this.log, "Clearing Hydac Errors done."); //
		}, CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS));
	}

	@Override
	public String debugLog() {
		return new StringBuilder() //
				.append(this.stateMachine.debugLog()) //
				.append("|CoolingUnit")//
				.append("|Temp: ")//
				.append(this.getOutletTemperature())//
				.append("|CoolingRequired: ")//
				.append(this.getCoolingRequired())//
				.append("|Pump: ")//
				.append(this.getPump())//
				.append("|Compressor: ")//
				.append(this.getCompressor())//
				.toString();
	}
}