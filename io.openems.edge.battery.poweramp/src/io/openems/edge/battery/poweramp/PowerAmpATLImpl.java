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
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
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
				new FC3ReadRegistersTask(500, Priority.LOW, //
						m(new BitsWordElement(500, this) //
								.bit(0, PowerAmpATL.ChannelId.RACK_PRE_ALARM_CELL_OVER_VOLTAGE) //
								.bit(1, PowerAmpATL.ChannelId.RACK_PRE_ALARM_CELL_UNDER_VOLTAGE) //
								.bit(2, PowerAmpATL.ChannelId.RACK_PRE_ALARM_OVER_CHARGING_CURRENT) //
								.bit(3, PowerAmpATL.ChannelId.RACK_PRE_ALARM_OVER_DISCHARGING_CURRENT) //
								.bit(4, PowerAmpATL.ChannelId.RACK_PRE_ALARM_OVER_TEMPERATURE) //
								.bit(5, PowerAmpATL.ChannelId.RACK_PRE_ALARM_UNDER_TEMPERATURE) //
								.bit(6, PowerAmpATL.ChannelId.RACK_PRE_ALARM_CELL_VOLTAGE_DIFFERENCE) //
								.bit(7, PowerAmpATL.ChannelId.RACK_PRE_ALARM_BCU_TEMP_DIFFERENCE) //
								.bit(8, PowerAmpATL.ChannelId.RACK_PRE_ALARM_UNDER_SOC) //
								.bit(9, PowerAmpATL.ChannelId.RACK_PRE_ALARM_UNDER_SOH) //
								.bit(10, PowerAmpATL.ChannelId.RACK_PRE_ALARM_OVER_CHARGING_POWER) //
								.bit(11, PowerAmpATL.ChannelId.RACK_PRE_ALARM_OVER_DISCHARGING_POWER) //
						), //
						m(new BitsWordElement(501, this) //
								.bit(0, PowerAmpATL.ChannelId.RACK_LEVEL_1_CELL_OVER_VOLTAGE) //
								.bit(1, PowerAmpATL.ChannelId.RACK_LEVEL_1_CELL_UNDER_VOLTAGE) //
								.bit(2, PowerAmpATL.ChannelId.RACK_LEVEL_1_OVER_CHARGING_CURRENT) //
								.bit(3, PowerAmpATL.ChannelId.RACK_LEVEL_1_OVER_DISCHARGING_CURRENT) //
								.bit(4, PowerAmpATL.ChannelId.RACK_LEVEL_1_OVER_TEMPERATURE) //
								.bit(5, PowerAmpATL.ChannelId.RACK_LEVEL_1_UNDER_TEMPERATURE) //
								.bit(6, PowerAmpATL.ChannelId.RACK_LEVEL_1_CELL_VOLTAGE_DIFFERENCE) //
								.bit(7, PowerAmpATL.ChannelId.RACK_LEVEL_1_BCU_TEMP_DIFFERENCE) //
								.bit(8, PowerAmpATL.ChannelId.RACK_LEVEL_1_UNDER_SOC) //
								.bit(9, PowerAmpATL.ChannelId.RACK_LEVEL_1_UNDER_SOH) //
								.bit(10, PowerAmpATL.ChannelId.RACK_LEVEL_1_OVER_CHARGING_POWER) //
								.bit(11, PowerAmpATL.ChannelId.RACK_LEVEL_1_OVER_DISCHARGING_POWER) //
						), //
						m(new BitsWordElement(502, this) //
								.bit(0, PowerAmpATL.ChannelId.RACK_LEVEL_2_CELL_OVER_VOLTAGE) //
								.bit(1, PowerAmpATL.ChannelId.RACK_LEVEL_2_CELL_UNDER_VOLTAGE) //
								.bit(2, PowerAmpATL.ChannelId.RACK_LEVEL_2_OVER_CHARGING_CURRENT) //
								.bit(3, PowerAmpATL.ChannelId.RACK_LEVEL_2_OVER_DISCHARGING_CURRENT) //
								.bit(4, PowerAmpATL.ChannelId.RACK_LEVEL_2_OVER_TEMPERATURE) //
								.bit(5, PowerAmpATL.ChannelId.RACK_LEVEL_2_UNDER_TEMPERATURE) //
								.bit(6, PowerAmpATL.ChannelId.RACK_LEVEL_2_CELL_VOLTAGE_DIFFERENCE) //
								.bit(7, PowerAmpATL.ChannelId.RACK_LEVEL_2_BCU_TEMP_DIFFERENCE) //
								.bit(8, PowerAmpATL.ChannelId.RACK_LEVEL_2_CELL_TEMPERATURE_DIFFERENCE) //
								.bit(9, PowerAmpATL.ChannelId.RACK_LEVEL_2_INTERNAL_COMMUNICATION) //
								.bit(10, PowerAmpATL.ChannelId.RACK_LEVEL_2_EXTERNAL_COMMUNICATION) //
								.bit(11, PowerAmpATL.ChannelId.RACK_LEVEL_2_PRE_CHARGE_FAIL) //
								.bit(12, PowerAmpATL.ChannelId.RACK_LEVEL_2_PARALLEL_FAIL) //
								.bit(13, PowerAmpATL.ChannelId.RACK_LEVEL_2_SYSTEM_FAIL) //
								.bit(14, PowerAmpATL.ChannelId.RACK_LEVEL_2_HARDWARE_FAIL)), //
						m(new BitsWordElement(503, this) //
								.bit(0, PowerAmpATL.ChannelId.ALARM_POSITION_BCU_1) //
								.bit(1, PowerAmpATL.ChannelId.ALARM_POSITION_BCU_2) //
								.bit(2, PowerAmpATL.ChannelId.ALARM_POSITION_BCU_3) //
								.bit(3, PowerAmpATL.ChannelId.ALARM_POSITION_BCU_4) //
								.bit(4, PowerAmpATL.ChannelId.ALARM_POSITION_BCU_5) //
								.bit(5, PowerAmpATL.ChannelId.ALARM_POSITION_BCU_6) //
								.bit(6, PowerAmpATL.ChannelId.ALARM_POSITION_BCU_7) //
								.bit(7, PowerAmpATL.ChannelId.ALARM_POSITION_BCU_8) //
								.bit(8, PowerAmpATL.ChannelId.ALARM_POSITION_BCU_9) //
								.bit(9, PowerAmpATL.ChannelId.ALARM_POSITION_BCU_10)), //
						m(new BitsWordElement(504, this) //
								.bit(0, PowerAmpATL.ChannelId.WARNING_POSITION_BCU_1) //
								.bit(1, PowerAmpATL.ChannelId.WARNING_POSITION_BCU_2) //
								.bit(2, PowerAmpATL.ChannelId.WARNING_POSITION_BCU_3) //
								.bit(3, PowerAmpATL.ChannelId.WARNING_POSITION_BCU_4) //
								.bit(4, PowerAmpATL.ChannelId.WARNING_POSITION_BCU_5) //
								.bit(5, PowerAmpATL.ChannelId.WARNING_POSITION_BCU_6) //
								.bit(6, PowerAmpATL.ChannelId.WARNING_POSITION_BCU_7) //
								.bit(7, PowerAmpATL.ChannelId.WARNING_POSITION_BCU_8) //
								.bit(8, PowerAmpATL.ChannelId.WARNING_POSITION_BCU_9) //
								.bit(9, PowerAmpATL.ChannelId.WARNING_POSITION_BCU_10)), //
						m(new BitsWordElement(505, this) //
								.bit(0, PowerAmpATL.ChannelId.FAULT_POSITION_BCU_1) //
								.bit(1, PowerAmpATL.ChannelId.FAULT_POSITION_BCU_2) //
								.bit(2, PowerAmpATL.ChannelId.FAULT_POSITION_BCU_3) //
								.bit(3, PowerAmpATL.ChannelId.FAULT_POSITION_BCU_4) //
								.bit(4, PowerAmpATL.ChannelId.FAULT_POSITION_BCU_5) //
								.bit(5, PowerAmpATL.ChannelId.FAULT_POSITION_BCU_6) //
								.bit(6, PowerAmpATL.ChannelId.FAULT_POSITION_BCU_7) //
								.bit(7, PowerAmpATL.ChannelId.FAULT_POSITION_BCU_8) //
								.bit(8, PowerAmpATL.ChannelId.FAULT_POSITION_BCU_9) //
								.bit(9, PowerAmpATL.ChannelId.FAULT_POSITION_BCU_10))//
				), //

				new FC3ReadRegistersTask(506, Priority.HIGH, //
						m(new UnsignedWordElement(506)) //
								.m(PowerAmpATL.ChannelId.BATTERY_RACK_VOLTAGE,
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1) // [mV]
								.m(Battery.ChannelId.VOLTAGE, ElementToChannelConverter.SCALE_FACTOR_MINUS_1) // [V]
								.build(), //
						m(new UnsignedWordElement(507)) //
								.m(PowerAmpATL.ChannelId.BATTERY_RACK_CURRENT,
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1) // [mV]
								.m(Battery.ChannelId.CURRENT, ElementToChannelConverter.SCALE_FACTOR_MINUS_1) // [V]
								.build(),
						m(new UnsignedWordElement(508))//
								.m(PowerAmpATL.ChannelId.BATTERY_RACK_SOC,
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1) // [%]
								.m(Battery.ChannelId.SOC, ElementToChannelConverter.SCALE_FACTOR_MINUS_1) // [%]
								.build(), //
						m(new UnsignedWordElement(509)) //
								.m(PowerAmpATL.ChannelId.BATTERY_RACK_SOH,
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1) // [%]
								.m(Battery.ChannelId.SOH, ElementToChannelConverter.SCALE_FACTOR_MINUS_1) // [%]
								.build(), //
						m(PowerAmpATL.ChannelId.CELL_VOLTAGE_MIN, new UnsignedWordElement(510)),
						m(PowerAmpATL.ChannelId.ID_OF_CELL_VOLTAGE_MIN, new UnsignedWordElement(511)), //
						m(PowerAmpATL.ChannelId.CELL_VOLTAGE_MAX, new UnsignedWordElement(512)), //
						m(PowerAmpATL.ChannelId.ID_OF_CELL_VOLTAGE_MAX, new UnsignedWordElement(513)), //
						m(PowerAmpATL.ChannelId.MIN_TEMPERATURE, new UnsignedWordElement(514), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(PowerAmpATL.ChannelId.ID_OF_MIN_TEMPERATURE, new UnsignedWordElement(515)), //
						m(PowerAmpATL.ChannelId.MAX_TEMPERATURE, new UnsignedWordElement(516)), //
						m(PowerAmpATL.ChannelId.ID_OF_MAX_TEMPERATURE, new UnsignedWordElement(517)), //
						m(new UnsignedWordElement(518)) //
								.m(PowerAmpATL.ChannelId.MAX_CHARGE_CURRENT,
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1) //
								.m(Battery.ChannelId.CHARGE_MAX_CURRENT, ElementToChannelConverter.SCALE_FACTOR_MINUS_1) // [%]
								.build(), //
						m(new UnsignedWordElement(518)) //
								.m(PowerAmpATL.ChannelId.MAX_DISCHARGE_CURRENT,
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1)
								.m(Battery.ChannelId.DISCHARGE_MAX_CURRENT, ElementToChannelConverter.SCALE_FACTOR_MINUS_1) // [%]
								.build(), //
						m(PowerAmpATL.ChannelId.MAX_DC_CHARGE_CURRENT_LIMIT_PER_BCU, new UnsignedWordElement(520), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(PowerAmpATL.ChannelId.MAX_DC_DISCHARGE_CURRENT_LIMIT_PER_BCU, new UnsignedWordElement(521), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(PowerAmpATL.ChannelId.RACK_NUMBER_OF_BATTERY_BCU, new UnsignedWordElement(522)), //
						m(PowerAmpATL.ChannelId.RACK_NUMBER_OF_CELLS_IN_SERIES_PER_MODULE,
								new UnsignedWordElement(523)), //
						m(new UnsignedWordElement(524)) //
								.m(PowerAmpATL.ChannelId.RACK_MAX_CELL_VOLTAGE_LIMIT,
										ElementToChannelConverter.DIRECT_1_TO_1)
								.m(Battery.ChannelId.MAX_CELL_VOLTAGE, ElementToChannelConverter.DIRECT_1_TO_1) // [%]
								.build(), //
						m(new UnsignedWordElement(525)) //
								.m(PowerAmpATL.ChannelId.RACK_MAX_CELL_VOLTAGE_LIMIT,
										ElementToChannelConverter.DIRECT_1_TO_1)
								.m(Battery.ChannelId.MAX_CELL_VOLTAGE, ElementToChannelConverter.DIRECT_1_TO_1) // [%]
								.build(), //
						m(new BitsWordElement(526, this) //
								.bit(0, PowerAmpATL.ChannelId.RACK_HW_AFE_COMMUNICATION_FAULT) //
								.bit(1, PowerAmpATL.ChannelId.RACK_HW_ACTOR_DRIVER_FAULT) //
								.bit(2, PowerAmpATL.ChannelId.RACK_HW_EEPROM_COMMUNICATION_FAULT) //
								.bit(3, PowerAmpATL.ChannelId.RACK_HW_VOLTAGE_DETECT_FAULT) //
								.bit(4, PowerAmpATL.ChannelId.RACK_HW_TEMPERATURE_DETECT_FAULT) //
								.bit(5, PowerAmpATL.ChannelId.RACK_HW_CURRENT_DETECT_FAULT) //
								.bit(6, PowerAmpATL.ChannelId.RACK_HW_ACTOR_NOT_CLOSE) //
								.bit(7, PowerAmpATL.ChannelId.RACK_HW_ACTOR_NOT_OPEN) //
								.bit(8, PowerAmpATL.ChannelId.RACK_HW_FUSE_BROKEN)), //
						m(new BitsWordElement(527, this) //
								.bit(0, PowerAmpATL.ChannelId.RACK_SYSTEM_AFE_OVER_TEMPERATURE) //
								.bit(1, PowerAmpATL.ChannelId.RACK_SYSTEM_AFE_UNDER_TEMPERATURE) //
								.bit(2, PowerAmpATL.ChannelId.RACK_SYSTEM_AFE_OVER_VOLTAGE) //
								.bit(3, PowerAmpATL.ChannelId.RACK_SYSTEM_AFE_UNDER_VOLTAGE) //
								.bit(4, PowerAmpATL.ChannelId.RACK_SYSTEM_HIGH_TEMPERATURE_PERMANENT_FAILURE) //
								.bit(5, PowerAmpATL.ChannelId.RACK_SYSTEM_LOW_TEMPERATURE_PERMANENT_FAILURE) //
								.bit(6, PowerAmpATL.ChannelId.RACK_SYSTEM_HIGH_CELL_VOLTAGE_PERMANENT_FAILURE) //
								.bit(7, PowerAmpATL.ChannelId.RACK_SYSTEM_LOW_CELL_VOLTAGE_PERMANENT_FAILURE) //
								.bit(8, PowerAmpATL.ChannelId.RACK_SYSTEM_SHORT_CIRCUIT)), //
						m(PowerAmpATL.ChannelId.UPPER_VOLTAGE, new UnsignedWordElement(528))), //
				new FC3ReadRegistersTask(10002, Priority.LOW, //
						m(new BitsWordElement(10002, this) //
								.bit(0, PowerAmpATL.ChannelId.BCU_STATUS_ALARM) //
								.bit(1, PowerAmpATL.ChannelId.BCU_STATUS_WARNING) //
								.bit(2, PowerAmpATL.ChannelId.BCU_STATUS_FAULT) //
								.bit(3, PowerAmpATL.ChannelId.BCU_STATUS_PFET) //
								.bit(4, PowerAmpATL.ChannelId.BCU_STATUS_CFET) //
								.bit(5, PowerAmpATL.ChannelId.BCU_STATUS_DFET) //
								.bit(6, PowerAmpATL.ChannelId.BCU_STATUS_BATTERY_IDLE) //
								.bit(7, PowerAmpATL.ChannelId.BCU_STATUS_BATTERY_CHARGING) //
								.bit(8, PowerAmpATL.ChannelId.BCU_STATUS_BATTERY_DISCHARGING)), //
						m(new BitsWordElement(10003, this) //
								.bit(0, PowerAmpATL.ChannelId.BCU_PRE_ALARM_CELL_OVER_VOLTAGE) //
								.bit(1, PowerAmpATL.ChannelId.BCU_PRE_ALARM_CELL_UNDER_VOLTAGE) //
								.bit(2, PowerAmpATL.ChannelId.BCU_PRE_ALARM_OVER_CHARGING_CURRENT) //
								.bit(3, PowerAmpATL.ChannelId.BCU_PRE_ALARM_OVER_DISCHARGING_CURRENT) //
								.bit(4, PowerAmpATL.ChannelId.BCU_PRE_ALARM_OVER_TEMPERATURE) //
								.bit(5, PowerAmpATL.ChannelId.BCU_PRE_ALARM_UNDER_TEMPERATURE) //
								.bit(6, PowerAmpATL.ChannelId.BCU_PRE_ALARM_CELL_VOLTAGE_DIFFERENCE) //
								.bit(7, PowerAmpATL.ChannelId.BCU_PRE_ALARM_BCU_TEMP_DIFFERENCE) //
								.bit(8, PowerAmpATL.ChannelId.BCU_PRE_ALARM_UNDER_SOC) //
								.bit(9, PowerAmpATL.ChannelId.BCU_PRE_ALARM_UNDER_SOH) //
								.bit(10, PowerAmpATL.ChannelId.BCU_PRE_ALARM_OVER_CHARGING_POWER) //
								.bit(11, PowerAmpATL.ChannelId.BCU_PRE_ALARM_OVER_DISCHARGING_POWER) //
						), //
						m(new BitsWordElement(10004, this) //
								.bit(0, PowerAmpATL.ChannelId.BCU_LEVEL_1_CELL_OVER_VOLTAGE) //
								.bit(1, PowerAmpATL.ChannelId.BCU_LEVEL_1_CELL_UNDER_VOLTAGE) //
								.bit(2, PowerAmpATL.ChannelId.BCU_LEVEL_1_OVER_CHARGING_CURRENT) //
								.bit(3, PowerAmpATL.ChannelId.BCU_LEVEL_1_OVER_DISCHARGING_CURRENT) //
								.bit(4, PowerAmpATL.ChannelId.BCU_LEVEL_1_OVER_TEMPERATURE) //
								.bit(5, PowerAmpATL.ChannelId.BCU_LEVEL_1_UNDER_TEMPERATURE) //
								.bit(6, PowerAmpATL.ChannelId.BCU_LEVEL_1_CELL_VOLTAGE_DIFFERENCE) //
								.bit(7, PowerAmpATL.ChannelId.BCU_LEVEL_1_BCU_TEMP_DIFFERENCE) //
								.bit(8, PowerAmpATL.ChannelId.BCU_LEVEL_1_UNDER_SOC) //
								.bit(9, PowerAmpATL.ChannelId.BCU_LEVEL_1_UNDER_SOH) //
								.bit(10, PowerAmpATL.ChannelId.BCU_LEVEL_1_OVER_CHARGING_POWER) //
								.bit(11, PowerAmpATL.ChannelId.BCU_LEVEL_1_OVER_DISCHARGING_POWER) //
						), //
						m(new BitsWordElement(10005, this) //
								.bit(0, PowerAmpATL.ChannelId.BCU_LEVEL_2_CELL_OVER_VOLTAGE) //
								.bit(1, PowerAmpATL.ChannelId.BCU_LEVEL_2_CELL_UNDER_VOLTAGE) //
								.bit(2, PowerAmpATL.ChannelId.BCU_LEVEL_2_OVER_CHARGING_CURRENT) //
								.bit(3, PowerAmpATL.ChannelId.BCU_LEVEL_2_OVER_DISCHARGING_CURRENT) //
								.bit(4, PowerAmpATL.ChannelId.BCU_LEVEL_2_OVER_TEMPERATURE) //
								.bit(5, PowerAmpATL.ChannelId.BCU_LEVEL_2_UNDER_TEMPERATURE) //
								.bit(6, PowerAmpATL.ChannelId.BCU_LEVEL_2_CELL_VOLTAGE_DIFFERENCE) //
								.bit(7, PowerAmpATL.ChannelId.BCU_LEVEL_2_BCU_TEMP_DIFFERENCE) //
								.bit(8, PowerAmpATL.ChannelId.BCU_LEVEL_2_TEMPERATURE_DIFFERENCE) //
								.bit(9, PowerAmpATL.ChannelId.BCU_LEVEL_2_INTERNAL_COMMUNICATION) //
								.bit(10, PowerAmpATL.ChannelId.BCU_LEVEL_2_EXTERNAL_COMMUNICATION) //
								.bit(11, PowerAmpATL.ChannelId.BCU_LEVEL_2_PRECHARGE_FAIL) //
								.bit(12, PowerAmpATL.ChannelId.BCU_LEVEL_2_PARALLEL_FAIL) //
								.bit(13, PowerAmpATL.ChannelId.BCU_LEVEL_2_SYSTEM_FAIL) //
								.bit(14, PowerAmpATL.ChannelId.BCU_LEVEL_2_HARDWARE_FAIL)), //
						m(new BitsWordElement(10006, this) //
								.bit(0, PowerAmpATL.ChannelId.BCU_HW_AFE_COMMUNICAITON_FAULT) //
								.bit(1, PowerAmpATL.ChannelId.BCU_HW_ACTOR_DRIVER_FAULT) //
								.bit(2, PowerAmpATL.ChannelId.BCU_HW_EEPROM_COMMUNICATION_FAULT) //
								.bit(3, PowerAmpATL.ChannelId.BCU_HW_VOLTAGE_DETECT_FAULT) //
								.bit(4, PowerAmpATL.ChannelId.BCU_HW_TEMPERATURE_DETECT_FAULT) //
								.bit(5, PowerAmpATL.ChannelId.BCU_HW_CURRENT_DETECT_FAULT) //
								.bit(6, PowerAmpATL.ChannelId.BCU_HW_ACTOR_NOT_CLOSE) //
								.bit(7, PowerAmpATL.ChannelId.BCU_HW_ACTOR_NOT_OPEN) //
								.bit(8, PowerAmpATL.ChannelId.BCU_HW_FUSE_BROKEN)), //
						m(new BitsWordElement(10007, this) //
								.bit(0, PowerAmpATL.ChannelId.BCU_SYSTEM_AFE_OVER_TEMPERATURE) //
								.bit(1, PowerAmpATL.ChannelId.BCU_SYSTEM_AFE_UNDER_TEMPERATURE) //
								.bit(2, PowerAmpATL.ChannelId.BCU_SYSTEM_AFE_OVER_VOLTAGE) //
								.bit(3, PowerAmpATL.ChannelId.BCU_SYSTEM_AFE_UNDER_VOLTAGE) //
								.bit(4, PowerAmpATL.ChannelId.BCU_SYSTEM_HIGH_TEMPERATURE_PERMANENT_FAILURE) //
								.bit(5, PowerAmpATL.ChannelId.BCU_SYSTEM_LOW_TEMPERATURE_PERMANENT_FAILURE) //
								.bit(6, PowerAmpATL.ChannelId.BCU_SYSTEM_HIGH_CELL_VOLTAGE_PERMANENT_FAILURE) //
								.bit(7, PowerAmpATL.ChannelId.BCU_SYSTEM_LOW_CELL_VOLTAGE_PERMANENT_FAILURE) //
								.bit(8, PowerAmpATL.ChannelId.BCU_SYSTEM_SHORT_CIRCUIT)), //
						m(PowerAmpATL.ChannelId.BCU_SOC, new UnsignedWordElement(10008), // [%]
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(PowerAmpATL.ChannelId.BCU_SOH, new UnsignedWordElement(10009), // [%]
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(PowerAmpATL.ChannelId.BCU_VOLTAGE, new UnsignedWordElement(10010), // [V]
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(PowerAmpATL.ChannelId.BCU_CURRENT, new UnsignedWordElement(10011), // [A]
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(PowerAmpATL.ChannelId.BCU_MIN_CELL_VOLTAGE, new UnsignedWordElement(10012)), // [mV]
						m(PowerAmpATL.ChannelId.BCU_MAX_CELL_VOLTAGE, new UnsignedWordElement(10013)), // [mV]
						m(PowerAmpATL.ChannelId.BCU_AVARAGE_CELL_VOLTAGE, new UnsignedWordElement(10014)), //
						m(PowerAmpATL.ChannelId.BCU_MAX_CHARGE_CURRENT, new UnsignedWordElement(10015)), //
						m(PowerAmpATL.ChannelId.BCU_MIN_CHARGE_CURRENT, new UnsignedWordElement(10016)), //
						m(PowerAmpATL.ChannelId.BMS_SERIAL_NUMBER, new UnsignedWordElement(10017)), //
						m(PowerAmpATL.ChannelId.NO_OF_CYCLES, new UnsignedWordElement(10018)), //
						m(PowerAmpATL.ChannelId.DESIGN_CAPACITY, new UnsignedWordElement(10019), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // [Ah]
						m(new UnsignedWordElement(10020)) //
								.m(PowerAmpATL.ChannelId.USEABLE_CAPACITY,
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1) // [mV]
								.m(Battery.ChannelId.CAPACITY, ElementToChannelConverter.DIRECT_1_TO_1) // [V]
								.build(), //
						m(PowerAmpATL.ChannelId.REMAINING_CAPACITY, new UnsignedWordElement(10021), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // [Ah]
						m(PowerAmpATL.ChannelId.BCU_MAX_CELL_VOLTAGE_LIMIT, new UnsignedWordElement(10022)), //
						m(PowerAmpATL.ChannelId.BCU_MIN_CELL_VOLTAGE_LIMIT, new UnsignedWordElement(10023)), //
						m(PowerAmpATL.ChannelId.BMU_NUMBER, new UnsignedWordElement(10024)) //
				));//
		return protocol;
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc();
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
