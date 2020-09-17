package io.openems.edge.battery.poweramp;

import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.poweramp.statemachine.Context;
import io.openems.edge.battery.poweramp.statemachine.StateMachine;
import io.openems.edge.battery.poweramp.statemachine.StateMachine.State;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.taskmanager.Priority;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Bms.PowerAmp.ATL", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		})

public class PowerAmpATLImpl extends AbstractOpenemsModbusComponent
		implements OpenemsComponent, Battery, EventHandler, ModbusSlave, StartStoppable, PowerAmpATL {

	private final Logger log = LoggerFactory.getLogger(PowerAmpATLImpl.class);

	@Reference
	protected ConfigurationAdmin cm;

	/**
	 * Manages the {@link State}s of the StateMachine.
	 */
	private final StateMachine stateMachine = new StateMachine(State.UNDEFINED);

	private Config config;

	public PowerAmpATLImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				PowerAmpATL.ChannelId.values() //
		);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id());
		this.config = config;
		// TODO Calculate Capacity
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {

		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {

		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.handleStateMachine();
			break;
		}
	}

	/**
	 * Handles the State-Machine.
	 */
	private void handleStateMachine() {
		// Store the current State
		this.channel(PowerAmpATL.ChannelId.STATE_MACHINE).setNextValue(this.stateMachine.getCurrentState());

		// Initialize 'Start-Stop' Channel
		this._setStartStop(StartStop.UNDEFINED);

		// Prepare Context
		Context context = new Context(this, this.config);

		// Call the StateMachine
		try {
			this.stateMachine.run(context);

			this.channel(PowerAmpATL.ChannelId.RUN_FAILED).setNextValue(false);

		} catch (OpenemsNamedException e) {
			this.channel(PowerAmpATL.ChannelId.RUN_FAILED).setNextValue(true);
			this.logError(this.log, "StateMachine failed: " + e.getMessage());
		}
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		ModbusProtocol protocol = new ModbusProtocol(this, //
				new FC6WriteRegisterTask(44000, //
						m(PowerAmpATL.ChannelId.BMS_CONTROL, new UnsignedWordElement(44000))), //
				new FC3ReadRegistersTask(44000, Priority.HIGH, //
						m(PowerAmpATL.ChannelId.BMS_CONTROL, new UnsignedWordElement(44000))), //
				new FC3ReadRegistersTask(506, Priority.HIGH, //
						m(new UnsignedWordElement(506)) //
								.m(PowerAmpATL.ChannelId.BATTERY_RACK_VOLTAGE, ElementToChannelConverter.SCALE_FACTOR_1) // [mV]
								.m(Battery.ChannelId.VOLTAGE, ElementToChannelConverter.SCALE_FACTOR_MINUS_1) // [V]
								.build(), //
						m(new UnsignedWordElement(507)) //
								.m(PowerAmpATL.ChannelId.BATTERY_RACK_CURRENT, ElementToChannelConverter.SCALE_FACTOR_1) // [mV]
								.m(Battery.ChannelId.CURRENT, ElementToChannelConverter.SCALE_FACTOR_MINUS_1) // [V]
								.build(),
						m(new UnsignedWordElement(508))//
								.m(PowerAmpATL.ChannelId.BATTERY_RACK_SOC, ElementToChannelConverter.DIRECT_1_TO_1) // [%]
								.m(Battery.ChannelId.SOC, ElementToChannelConverter.DIRECT_1_TO_1) // [%]
								.build(), //
						m(new UnsignedWordElement(509)) //
								.m(PowerAmpATL.ChannelId.BATTERY_RACK_SOH, ElementToChannelConverter.DIRECT_1_TO_1) // [%]
								.m(Battery.ChannelId.SOH, ElementToChannelConverter.DIRECT_1_TO_1) // [%]
								.build(), //
						m(PowerAmpATL.ChannelId.CELL_VOLTAGE_MIN, new UnsignedWordElement(510)), //
						m(PowerAmpATL.ChannelId.ID_OF_CELL_VOLTAGE_MIN, new UnsignedWordElement(511)), //
						m(PowerAmpATL.ChannelId.CELL_VOLTAGE_MAX, new UnsignedWordElement(512)), //
						m(PowerAmpATL.ChannelId.ID_OF_CELL_VOLTAGE_MAX, new UnsignedWordElement(513)), //
						m(PowerAmpATL.ChannelId.MIN_TEMPERATURE, new UnsignedWordElement(514)), //
						m(PowerAmpATL.ChannelId.ID_OF_MIN_TEMPERATURE, new UnsignedWordElement(515)), //
						m(PowerAmpATL.ChannelId.MAX_TEMPERATURE, new UnsignedWordElement(516)), //
						m(PowerAmpATL.ChannelId.ID_OF_MAX_TEMPERATURE, new UnsignedWordElement(517)), //
						m(PowerAmpATL.ChannelId.BATTERY_RACK_DC_CHARGE_CURRENT_LIMIT, new UnsignedWordElement(518))//
				));//
		return protocol;
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc() //
				+ "|State:" + this.stateMachine.getCurrentState();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				Battery.getModbusSlaveNatureTable(accessMode) //
		);
	}

	private AtomicReference<StartStop> startStopTarget = new AtomicReference<StartStop>(StartStop.UNDEFINED);

	@Override
	public void setStartStop(StartStop value) {
		if (this.startStopTarget.getAndSet(value) != value) {
			// Set only if value changed
			this.stateMachine.forceNextState(State.UNDEFINED);
		}
	}

	@Override
	public StartStop getStartStopTarget() {
		switch (this.config.startStop()) {
		case AUTO:
			// read StartStop-Channel
			return this.startStopTarget.get();

		case START:
			// force START
			return StartStop.START;

		case STOP:
			// force STOP
			return StartStop.STOP;
		}

		assert false;
		return StartStop.UNDEFINED; // can never happen
	}
}
