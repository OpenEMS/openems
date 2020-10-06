package io.openems.edge.battery.bmw;

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
import io.openems.edge.battery.bmw.enums.BmsState;
import io.openems.edge.battery.bmw.statemachine.Context;
import io.openems.edge.battery.bmw.statemachine.StateMachine;
import io.openems.edge.battery.bmw.statemachine.StateMachine.State;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.taskmanager.Priority;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Bmw.Battery", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
)
public class BmwBatteryImpl extends AbstractOpenemsModbusComponent
		implements BmwBattery, Battery, OpenemsComponent, StartStoppable, EventHandler, ModbusSlave {

	private final Logger log = LoggerFactory.getLogger(BmwBatteryImpl.class);

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	//private State state = State.UNDEFINED;

	/**
	 * Manages the {@link State}s of the StateMachine.
	 */
	private final StateMachine stateMachine = new StateMachine(State.UNDEFINED);

	private Config config;

	public BmwBatteryImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				Battery.ChannelId.values(), //
				BmwBattery.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id());
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

		this.channel(BmwBattery.ChannelId.STATE_MACHINE).setNextValue(this.stateMachine.getCurrentState());

		// Initialize 'Start-Stop' Channel
		this._setStartStop(StartStop.UNDEFINED);

		// Prepare Context
		Context context = new Context(this, this.config);

		// Call the StateMachine
		try {
			this.stateMachine.run(context);

			this.channel(BmwBattery.ChannelId.RUN_FAILED).setNextValue(false);

		} catch (OpenemsNamedException e) {
			this.channel(BmwBattery.ChannelId.RUN_FAILED).setNextValue(true);
			this.logError(this.log, "StateMachine failed: " + e.getMessage());
		}

	}

	public boolean isSystemRunning() {
		EnumReadChannel bmsStateChannel = this.channel(BmwBattery.ChannelId.BMS_STATE);
		BmsState bmsState = bmsStateChannel.value().asEnum();
		return bmsState == BmsState.OPERATION;
	}

	public boolean isSystemStopped() {
		EnumReadChannel bmsStateChannel = this.channel(BmwBattery.ChannelId.BMS_STATE);
		BmsState bmsState = bmsStateChannel.value().asEnum();
		return bmsState == BmsState.OFF;
	}

	public void clearError() {
		BooleanWriteChannel clearErrorChannel = this.channel(BmwBattery.ChannelId.BMS_STATE_COMMAND_CLEAR_ERROR);
		try {
			clearErrorChannel.setNextWriteValue(true);
		} catch (OpenemsNamedException e) {
			// TODO should Fault state channel, but after start stop feature
			log.error("Error while trying to reset the system!");
		}
	}

	public boolean isError() {
		EnumReadChannel bmsStateChannel = this.channel(BmwBattery.ChannelId.BMS_STATE);
		BmsState bmsState = bmsStateChannel.value().asEnum();
		return bmsState == BmsState.ERROR;
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc() //
				+ "|Discharge:" + this.getDischargeMinVoltage() + ";" + this.getDischargeMaxCurrent() //
				+ "|Charge:" + this.getChargeMaxVoltage() + ";" + this.getChargeMaxCurrent() //
				+ "|State:" + this.stateMachine.getCurrentState();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {

		return new ModbusProtocol(this, //

				new FC16WriteRegistersTask(1399, //
						m(BmwBattery.ChannelId.HEART_BEAT, new UnsignedWordElement(1399)), //
						m(BmwBattery.ChannelId.BMS_STATE_COMMAND, new UnsignedWordElement(1400)), //
						m(BmwBattery.ChannelId.OPERATING_STATE_INVERTER, new UnsignedWordElement(1401)), //
						m(BmwBattery.ChannelId.DC_LINK_VOLTAGE, new UnsignedWordElement(1402),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BmwBattery.ChannelId.DC_LINK_CURRENT, new UnsignedWordElement(1403)), //
						m(BmwBattery.ChannelId.OPERATION_MODE_REQUEST_GRANTED, new UnsignedWordElement(1404)), //
						m(BmwBattery.ChannelId.OPERATION_MODE_REQUEST_CANCELED, new UnsignedWordElement(1405)), //
						m(new BitsWordElement(1406, this) //
								.bit(1, BmwBattery.ChannelId.CONNECTION_STRATEGY_HIGH_SOC_FIRST) //
								.bit(0, BmwBattery.ChannelId.CONNECTION_STRATEGY_LOW_SOC_FIRST) //
						), //
						m(BmwBattery.ChannelId.SYSTEM_TIME, new UnsignedDoublewordElement(1407)) //
				),

				new FC4ReadInputRegistersTask(999, Priority.HIGH, //
						m(BmwBattery.ChannelId.LIFE_SIGN, new UnsignedWordElement(999)), //
						m(BmwBattery.ChannelId.BMS_STATE, new UnsignedWordElement(1000)) //
				), //

				new FC4ReadInputRegistersTask(1001, Priority.LOW, //
						m(new BitsWordElement(1001, this) //
								.bit(0, BmwBattery.ChannelId.ERROR_BITS_1_UNSPECIFIED_ERROR) //
								.bit(1, BmwBattery.ChannelId.ERROR_BITS_1_LOW_VOLTAGE_ERROR) //
								.bit(2, BmwBattery.ChannelId.ERROR_BITS_1_HIGH_VOLTAGE_ERROR) //
								.bit(3, BmwBattery.ChannelId.ERROR_BITS_1_CHARGE_CURRENT_ERROR) //
								.bit(4, BmwBattery.ChannelId.ERROR_BITS_1_DISCHARGE_CURRENT_ERROR) //
								.bit(5, BmwBattery.ChannelId.ERROR_BITS_1_CHARGE_POWER_ERROR) //
								.bit(6, BmwBattery.ChannelId.ERROR_BITS_1_DISCHARGE_POWER_ERROR) //
								.bit(7, BmwBattery.ChannelId.ERROR_BITS_1_LOW_SOC_ERROR) //
								.bit(8, BmwBattery.ChannelId.ERROR_BITS_1_HIGH_SOC_ERROR) //
								.bit(9, BmwBattery.ChannelId.ERROR_BITS_1_LOW_TEMPERATURE_ERROR) //
								.bit(10, BmwBattery.ChannelId.ERROR_BITS_1_HIGH_TEMPERATURE_ERROR) //
								.bit(11, BmwBattery.ChannelId.ERROR_BITS_1_INSULATION_ERROR) //
								.bit(12, BmwBattery.ChannelId.ERROR_BITS_1_CONTACTOR_FUSE_ERROR) //
								.bit(13, BmwBattery.ChannelId.ERROR_BITS_1_SENSOR_ERROR) //
								.bit(14, BmwBattery.ChannelId.ERROR_BITS_1_IMBALANCE_ERROR) //
								.bit(15, BmwBattery.ChannelId.ERROR_BITS_1_COMMUNICATION_ERROR) //
						), //
						m(new BitsWordElement(1002, this) //
								.bit(0, BmwBattery.ChannelId.ERROR_BITS_2_CONTAINER_ERROR) //
								.bit(1, BmwBattery.ChannelId.ERROR_BITS_2_SOH_ERROR)
								.bit(2, BmwBattery.ChannelId.ERROR_BITS_2_RACK_STRING_ERROR) //
						), //
						m(new BitsWordElement(1003, this) //
								.bit(0, BmwBattery.ChannelId.WARNING_BITS_1_UNSPECIFIED_WARNING) //
								.bit(1, BmwBattery.ChannelId.WARNING_BITS_1_LOW_VOLTAGE_WARNING) //
								.bit(2, BmwBattery.ChannelId.WARNING_BITS_1_HIGH_VOLTAGE_WARNING) //
								.bit(3, BmwBattery.ChannelId.WARNING_BITS_1_CHARGE_CURRENT_WARNING) //
								.bit(4, BmwBattery.ChannelId.WARNING_BITS_1_DISCHARGE_CURRENT_WARNING) //
								.bit(5, BmwBattery.ChannelId.WARNING_BITS_1_CHARGE_POWER_WARNING) //
								.bit(6, BmwBattery.ChannelId.WARNING_BITS_1_DISCHARGE_POWER_WARNING) //
								.bit(7, BmwBattery.ChannelId.WARNING_BITS_1_LOW_SOC_WARNING) //
								.bit(8, BmwBattery.ChannelId.WARNING_BITS_1_HIGH_SOC_WARNING) //
								.bit(9, BmwBattery.ChannelId.WARNING_BITS_1_LOW_TEMPERATURE_WARNING) //
								.bit(10, BmwBattery.ChannelId.WARNING_BITS_1_HIGH_TEMPERATURE_WARNING) //
								.bit(11, BmwBattery.ChannelId.WARNING_BITS_1_INSULATION_WARNING) //
								.bit(12, BmwBattery.ChannelId.WARNING_BITS_1_CONTACTOR_FUSE_WARNING) //
								.bit(13, BmwBattery.ChannelId.WARNING_BITS_1_SENSOR_WARNING) //
								.bit(14, BmwBattery.ChannelId.WARNING_BITS_1_IMBALANCE_WARNING) //
								.bit(15, BmwBattery.ChannelId.WARNING_BITS_1_COMMUNICATION_WARNING) //
						), //
						m(new BitsWordElement(1004, this) //
								.bit(0, BmwBattery.ChannelId.WARNING_BITS_2_CONTAINER_WARNING)
								.bit(1, BmwBattery.ChannelId.WARNING_BITS_2_SOH_WARNING)
								.bit(2, BmwBattery.ChannelId.WARNING_BITS_2_RACK_STRING_WARNING) //
						)), //

				new FC4ReadInputRegistersTask(1005, Priority.HIGH, //
						m(BmwBattery.ChannelId.INFO_BITS, new UnsignedWordElement(1005)), //
						m(BmwBattery.ChannelId.MAXIMUM_OPERATING_CURRENT, new UnsignedWordElement(1006)), //
						m(BmwBattery.ChannelId.MINIMUM_OPERATING_CURRENT, new SignedWordElement(1007)), //
						m(Battery.ChannelId.CHARGE_MAX_VOLTAGE, new UnsignedWordElement(1008),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(Battery.ChannelId.DISCHARGE_MIN_VOLTAGE, new UnsignedWordElement(1009),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(Battery.ChannelId.DISCHARGE_MAX_CURRENT, new UnsignedWordElement(1010)), //
						m(Battery.ChannelId.CHARGE_MAX_CURRENT, new SignedWordElement(1011),
								ElementToChannelConverter.INVERT), //
						m(BmwBattery.ChannelId.MAXIMUM_LIMIT_DYNAMIC_VOLTAGE, new UnsignedWordElement(1012),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BmwBattery.ChannelId.MINIMUM_LIMIT_DYNAMIC_VOLTAGE, new UnsignedWordElement(1013),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BmwBattery.ChannelId.NUMBER_OF_STRINGS_CONNECTED, new UnsignedWordElement(1014)), //
						m(BmwBattery.ChannelId.NUMBER_OF_STRINGS_INSTALLED, new UnsignedWordElement(1015)), //
						m(BmwBattery.ChannelId.SOC_ALL_STRINGS, new UnsignedWordElement(1016),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(Battery.ChannelId.SOC, new UnsignedWordElement(1017),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(BmwBattery.ChannelId.REMAINING_CHARGE_CAPACITY, new UnsignedWordElement(1018)), //
						m(BmwBattery.ChannelId.REMAINING_DISCHARGE_CAPACITY, new UnsignedWordElement(1019)), //
						m(BmwBattery.ChannelId.REMAINING_CHARGE_ENERGY, new UnsignedWordElement(1020)), //
						m(BmwBattery.ChannelId.REMAINING_DISCHARGE_ENERGY, new UnsignedWordElement(1021)), //
						m(BmwBattery.ChannelId.NOMINAL_ENERGY, new UnsignedWordElement(1022)), //
						m(BmwBattery.ChannelId.TOTAL_ENERGY, new UnsignedWordElement(1023)), //
						m(BmwBattery.ChannelId.NOMINAL_CAPACITY, new UnsignedWordElement(1024)), //
						m(Battery.ChannelId.CAPACITY, new UnsignedWordElement(1025)), //
						m(Battery.ChannelId.SOH, new UnsignedWordElement(1026),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(Battery.ChannelId.VOLTAGE, new UnsignedWordElement(1027),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BmwBattery.ChannelId.DC_VOLTAGE_AVERAGE, new UnsignedWordElement(1028),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)), //

				new FC4ReadInputRegistersTask(1029, Priority.HIGH, //
						m(new UnsignedWordElement(1029)) //
								.m(BmwBattery.ChannelId.DC_CURRENT, ElementToChannelConverter.SCALE_FACTOR_MINUS_1) //
								.m(Battery.ChannelId.CURRENT, ElementToChannelConverter.SCALE_FACTOR_3) //
								.build()), //

				new FC4ReadInputRegistersTask(1030, Priority.HIGH, //
						m(BmwBattery.ChannelId.AVERAGE_TEMPERATURE, new UnsignedWordElement(1030))), //

				new FC4ReadInputRegistersTask(1031, Priority.HIGH, //
						m(new UnsignedWordElement(1031)) //
								.m(BmwBattery.ChannelId.MINIMUM_TEMPERATURE, ElementToChannelConverter.DIRECT_1_TO_1) //
								.m(Battery.ChannelId.MIN_CELL_TEMPERATURE, ElementToChannelConverter.DIRECT_1_TO_1) //
								.build()), //

				new FC4ReadInputRegistersTask(1032, Priority.HIGH, //
						m(new UnsignedWordElement(1032)) //
								.m(BmwBattery.ChannelId.MAXIMUM_TEMPERATURE, ElementToChannelConverter.DIRECT_1_TO_1) //
								.m(Battery.ChannelId.MAX_CELL_TEMPERATURE, ElementToChannelConverter.DIRECT_1_TO_1) //
								.build()), //

				new FC4ReadInputRegistersTask(1033, Priority.HIGH,
						m(Battery.ChannelId.MIN_CELL_VOLTAGE, new UnsignedWordElement(1033)), //
						m(Battery.ChannelId.MAX_CELL_VOLTAGE, new UnsignedWordElement(1034)), //
						m(BmwBattery.ChannelId.AVERAGE_CELL_VOLTAGE, new UnsignedWordElement(1035)), //
						m(BmwBattery.ChannelId.INTERNAL_RESISTANCE, new UnsignedWordElement(1036)), //
						m(BmwBattery.ChannelId.INSULATION_RESISTANCE, new UnsignedWordElement(1037),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BmwBattery.ChannelId.CONTAINER_TEMPERATURE, new UnsignedWordElement(1038),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BmwBattery.ChannelId.AMBIENT_TEMPERATURE, new UnsignedWordElement(1039),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BmwBattery.ChannelId.HUMIDITY_CONTAINER, new UnsignedWordElement(1040),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BmwBattery.ChannelId.MAXIMUM_LIMIT_DYNAMIC_CURRENT_HIGH_RES, new UnsignedWordElement(1041),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(BmwBattery.ChannelId.MINIMUM_LIMIT_DYNAMIC_CURRENT_HIGH_RES, new UnsignedWordElement(1042),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(BmwBattery.ChannelId.FULL_CYCLE_COUNT, new UnsignedWordElement(1043)), //
						m(BmwBattery.ChannelId.OPERATING_TIME_COUNT, new UnsignedDoublewordElement(1044)), //
						m(BmwBattery.ChannelId.COM_PRO_VERSION, new UnsignedDoublewordElement(1046)), //
						m(BmwBattery.ChannelId.SERIAL_NUMBER, new UnsignedDoublewordElement(1048)), //
						m(BmwBattery.ChannelId.SERIAL_NUMBER, new UnsignedDoublewordElement(1050)), //
						m(BmwBattery.ChannelId.SOFTWARE_VERSION, new UnsignedDoublewordElement(1052)) //
				)

		);
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable( //
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				Battery.getModbusSlaveNatureTable(accessMode) //
		);
	}

	private AtomicReference<StartStop> startStopTarget = new AtomicReference<StartStop>(StartStop.UNDEFINED);

	@Override
	public void setStartStop(StartStop value) throws OpenemsNamedException {
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