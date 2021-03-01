package io.openems.edge.battery.fenecon.home;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

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
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.fenecon.home.statemachine.Context;
import io.openems.edge.battery.fenecon.home.statemachine.StateMachine;
import io.openems.edge.battery.fenecon.home.statemachine.StateMachine.State;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.ModbusUtils;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.taskmanager.Priority;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Battery.Fenecon.Home", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		})
public class FeneconHomeBatteryImpl extends AbstractOpenemsModbusComponent
		implements OpenemsComponent, Battery, EventHandler, ModbusSlave, StartStoppable, FeneconHomeBattery {

	private final Logger log = LoggerFactory.getLogger(FeneconHomeBatteryImpl.class);

	@Reference
	protected ConfigurationAdmin cm;

	/**
	 * Manages the {@link State}s of the StateMachine.
	 */
	private final StateMachine stateMachine = new StateMachine(State.UNDEFINED);

	private final AtomicReference<StartStop> startStopTarget = new AtomicReference<StartStop>(StartStop.UNDEFINED);

	private Config config;
	private Map<String, Channel<?>> channelMap;
	private static final String KEY_TEMPERATURE = "_TEMPERATURE";
	private static final String KEY_VOLTAGE = "_VOLTAGE";
	private static final String NUMBER_FORMAT = "%03d"; // creates string number with leading zeros
	private int towerNum = 1;

	public FeneconHomeBatteryImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				FeneconHomeBattery.ChannelId.values() //
		);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;
		this.identifyBcuNumberChannels();
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id());
	}

	@Deactivate
	protected void deactivate() {
		// Remove dynamically created channels when component is deactivated
		for (Channel<?> c : this.channelMap.values()) {
			this.removeChannel(c);
		}
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
		this.channel(FeneconHomeBattery.ChannelId.STATE_MACHINE).setNextValue(this.stateMachine.getCurrentState());

		// Initialize 'Start-Stop' Channel
		this._setStartStop(StartStop.UNDEFINED);

		// Prepare Context
		Context context = new Context(this);

		// Call the StateMachine
		try {
			this.stateMachine.run(context);

			this.channel(FeneconHomeBattery.ChannelId.RUN_FAILED).setNextValue(false);

		} catch (OpenemsNamedException e) {
			this.channel(FeneconHomeBattery.ChannelId.RUN_FAILED).setNextValue(true);
			this.logError(this.log, "StateMachine failed: " + e.getMessage());
		}
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		ModbusProtocol protocol = new ModbusProtocol(this, //
				new FC3ReadRegistersTask(500, Priority.LOW, //
						m(new BitsWordElement(500, this) //
								.bit(0, FeneconHomeBattery.ChannelId.RACK_PRE_ALARM_CELL_OVER_VOLTAGE) //
								.bit(1, FeneconHomeBattery.ChannelId.RACK_PRE_ALARM_CELL_UNDER_VOLTAGE) //
								.bit(2, FeneconHomeBattery.ChannelId.RACK_PRE_ALARM_OVER_CHARGING_CURRENT) //
								.bit(3, FeneconHomeBattery.ChannelId.RACK_PRE_ALARM_OVER_DISCHARGING_CURRENT) //
								.bit(4, FeneconHomeBattery.ChannelId.RACK_PRE_ALARM_OVER_TEMPERATURE) //
								.bit(5, FeneconHomeBattery.ChannelId.RACK_PRE_ALARM_UNDER_TEMPERATURE) //
								.bit(6, FeneconHomeBattery.ChannelId.RACK_PRE_ALARM_CELL_VOLTAGE_DIFFERENCE) //
								.bit(7, FeneconHomeBattery.ChannelId.RACK_PRE_ALARM_BCU_TEMP_DIFFERENCE) //
								.bit(8, FeneconHomeBattery.ChannelId.RACK_PRE_ALARM_UNDER_SOC) //
								.bit(9, FeneconHomeBattery.ChannelId.RACK_PRE_ALARM_UNDER_SOH) //
								.bit(10, FeneconHomeBattery.ChannelId.RACK_PRE_ALARM_OVER_CHARGING_POWER) //
								.bit(11, FeneconHomeBattery.ChannelId.RACK_PRE_ALARM_OVER_DISCHARGING_POWER) //
						), //
						m(new BitsWordElement(501, this) //
								.bit(0, FeneconHomeBattery.ChannelId.RACK_LEVEL_1_CELL_OVER_VOLTAGE) //
								.bit(1, FeneconHomeBattery.ChannelId.RACK_LEVEL_1_CELL_UNDER_VOLTAGE) //
								.bit(2, FeneconHomeBattery.ChannelId.RACK_LEVEL_1_OVER_CHARGING_CURRENT) //
								.bit(3, FeneconHomeBattery.ChannelId.RACK_LEVEL_1_OVER_DISCHARGING_CURRENT) //
								.bit(4, FeneconHomeBattery.ChannelId.RACK_LEVEL_1_OVER_TEMPERATURE) //
								.bit(5, FeneconHomeBattery.ChannelId.RACK_LEVEL_1_UNDER_TEMPERATURE) //
								.bit(6, FeneconHomeBattery.ChannelId.RACK_LEVEL_1_CELL_VOLTAGE_DIFFERENCE) //
								.bit(7, FeneconHomeBattery.ChannelId.RACK_LEVEL_1_BCU_TEMP_DIFFERENCE) //
								.bit(8, FeneconHomeBattery.ChannelId.RACK_LEVEL_1_UNDER_SOC) //
								.bit(9, FeneconHomeBattery.ChannelId.RACK_LEVEL_1_UNDER_SOH) //
								.bit(10, FeneconHomeBattery.ChannelId.RACK_LEVEL_1_OVER_CHARGING_POWER) //
								.bit(11, FeneconHomeBattery.ChannelId.RACK_LEVEL_1_OVER_DISCHARGING_POWER) //
						), //
						m(new BitsWordElement(502, this) //
								.bit(0, FeneconHomeBattery.ChannelId.RACK_LEVEL_2_CELL_OVER_VOLTAGE) //
								.bit(1, FeneconHomeBattery.ChannelId.RACK_LEVEL_2_CELL_UNDER_VOLTAGE) //
								.bit(2, FeneconHomeBattery.ChannelId.RACK_LEVEL_2_OVER_CHARGING_CURRENT) //
								.bit(3, FeneconHomeBattery.ChannelId.RACK_LEVEL_2_OVER_DISCHARGING_CURRENT) //
								.bit(4, FeneconHomeBattery.ChannelId.RACK_LEVEL_2_OVER_TEMPERATURE) //
								.bit(5, FeneconHomeBattery.ChannelId.RACK_LEVEL_2_UNDER_TEMPERATURE) //
								.bit(6, FeneconHomeBattery.ChannelId.RACK_LEVEL_2_CELL_VOLTAGE_DIFFERENCE) //
								.bit(7, FeneconHomeBattery.ChannelId.RACK_LEVEL_2_BCU_TEMP_DIFFERENCE) //
								.bit(8, FeneconHomeBattery.ChannelId.RACK_LEVEL_2_CELL_TEMPERATURE_DIFFERENCE) //
								.bit(9, FeneconHomeBattery.ChannelId.RACK_LEVEL_2_INTERNAL_COMMUNICATION) //
								.bit(10, FeneconHomeBattery.ChannelId.RACK_LEVEL_2_EXTERNAL_COMMUNICATION) //
								.bit(11, FeneconHomeBattery.ChannelId.RACK_LEVEL_2_PRE_CHARGE_FAIL) //
								.bit(12, FeneconHomeBattery.ChannelId.RACK_LEVEL_2_PARALLEL_FAIL) //
								.bit(13, FeneconHomeBattery.ChannelId.RACK_LEVEL_2_SYSTEM_FAIL) //
								.bit(14, FeneconHomeBattery.ChannelId.RACK_LEVEL_2_HARDWARE_FAIL)), //
						m(new BitsWordElement(503, this) //
								.bit(0, FeneconHomeBattery.ChannelId.ALARM_POSITION_BCU_1) //
								.bit(1, FeneconHomeBattery.ChannelId.ALARM_POSITION_BCU_2) //
								.bit(2, FeneconHomeBattery.ChannelId.ALARM_POSITION_BCU_3) //
								.bit(3, FeneconHomeBattery.ChannelId.ALARM_POSITION_BCU_4) //
								.bit(4, FeneconHomeBattery.ChannelId.ALARM_POSITION_BCU_5) //
								.bit(5, FeneconHomeBattery.ChannelId.ALARM_POSITION_BCU_6) //
								.bit(6, FeneconHomeBattery.ChannelId.ALARM_POSITION_BCU_7) //
								.bit(7, FeneconHomeBattery.ChannelId.ALARM_POSITION_BCU_8) //
								.bit(8, FeneconHomeBattery.ChannelId.ALARM_POSITION_BCU_9) //
								.bit(9, FeneconHomeBattery.ChannelId.ALARM_POSITION_BCU_10)), //
						m(new BitsWordElement(504, this) //
								.bit(0, FeneconHomeBattery.ChannelId.WARNING_POSITION_BCU_1) //
								.bit(1, FeneconHomeBattery.ChannelId.WARNING_POSITION_BCU_2) //
								.bit(2, FeneconHomeBattery.ChannelId.WARNING_POSITION_BCU_3) //
								.bit(3, FeneconHomeBattery.ChannelId.WARNING_POSITION_BCU_4) //
								.bit(4, FeneconHomeBattery.ChannelId.WARNING_POSITION_BCU_5) //
								.bit(5, FeneconHomeBattery.ChannelId.WARNING_POSITION_BCU_6) //
								.bit(6, FeneconHomeBattery.ChannelId.WARNING_POSITION_BCU_7) //
								.bit(7, FeneconHomeBattery.ChannelId.WARNING_POSITION_BCU_8) //
								.bit(8, FeneconHomeBattery.ChannelId.WARNING_POSITION_BCU_9) //
								.bit(9, FeneconHomeBattery.ChannelId.WARNING_POSITION_BCU_10)), //
						m(new BitsWordElement(505, this) //
								.bit(0, FeneconHomeBattery.ChannelId.FAULT_POSITION_BCU_1) //
								.bit(1, FeneconHomeBattery.ChannelId.FAULT_POSITION_BCU_2) //
								.bit(2, FeneconHomeBattery.ChannelId.FAULT_POSITION_BCU_3) //
								.bit(3, FeneconHomeBattery.ChannelId.FAULT_POSITION_BCU_4) //
								.bit(4, FeneconHomeBattery.ChannelId.FAULT_POSITION_BCU_5) //
								.bit(5, FeneconHomeBattery.ChannelId.FAULT_POSITION_BCU_6) //
								.bit(6, FeneconHomeBattery.ChannelId.FAULT_POSITION_BCU_7) //
								.bit(7, FeneconHomeBattery.ChannelId.FAULT_POSITION_BCU_8) //
								.bit(8, FeneconHomeBattery.ChannelId.FAULT_POSITION_BCU_9) //
								.bit(9, FeneconHomeBattery.ChannelId.FAULT_POSITION_BCU_10))//
				), // //

				new FC3ReadRegistersTask(506, Priority.HIGH, //
						m(new UnsignedWordElement(506)) //
								.m(FeneconHomeBattery.ChannelId.BATTERY_RACK_VOLTAGE,
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1) // [mV]
								.m(Battery.ChannelId.VOLTAGE, ElementToChannelConverter.SCALE_FACTOR_MINUS_1) // [V]
								.build(), //
						m(new UnsignedWordElement(507)) //
								.m(FeneconHomeBattery.ChannelId.BATTERY_RACK_CURRENT,
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1) // [mV]
								.m(Battery.ChannelId.CURRENT, ElementToChannelConverter.SCALE_FACTOR_MINUS_1) // [V]
								.build(),
						m(new UnsignedWordElement(508))//
								.m(FeneconHomeBattery.ChannelId.BATTERY_RACK_SOC,
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1) // [%]
								.m(Battery.ChannelId.SOC, ElementToChannelConverter.SCALE_FACTOR_MINUS_1) // [%]
								.build(), //
						m(new UnsignedWordElement(509)) //
								.m(FeneconHomeBattery.ChannelId.BATTERY_RACK_SOH,
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1) // [%]
								.m(Battery.ChannelId.SOH, ElementToChannelConverter.SCALE_FACTOR_MINUS_1) // [%]
								.build(), //
						m(FeneconHomeBattery.ChannelId.CELL_VOLTAGE_MIN, new UnsignedWordElement(510)),
						m(FeneconHomeBattery.ChannelId.ID_OF_CELL_VOLTAGE_MIN, new UnsignedWordElement(511)), //
						m(FeneconHomeBattery.ChannelId.CELL_VOLTAGE_MAX, new UnsignedWordElement(512)), //
						m(FeneconHomeBattery.ChannelId.ID_OF_CELL_VOLTAGE_MAX, new UnsignedWordElement(513)), //
						m(FeneconHomeBattery.ChannelId.MIN_TEMPERATURE, new UnsignedWordElement(514), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(FeneconHomeBattery.ChannelId.ID_OF_MIN_TEMPERATURE, new UnsignedWordElement(515)), //
						m(FeneconHomeBattery.ChannelId.MAX_TEMPERATURE, new UnsignedWordElement(516),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(FeneconHomeBattery.ChannelId.ID_OF_MAX_TEMPERATURE, new UnsignedWordElement(517)), //
						m(new UnsignedWordElement(518)) //
								.m(FeneconHomeBattery.ChannelId.MAX_CHARGE_CURRENT,
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1) //
								.m(Battery.ChannelId.CHARGE_MAX_CURRENT, ElementToChannelConverter.SCALE_FACTOR_MINUS_1) // [A]
								.build(), //
						m(new UnsignedWordElement(519)) //
								.m(FeneconHomeBattery.ChannelId.MAX_DISCHARGE_CURRENT,
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1)
								.m(Battery.ChannelId.DISCHARGE_MAX_CURRENT,
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1) // [%]
								.build(), //
						m(FeneconHomeBattery.ChannelId.MAX_DC_CHARGE_CURRENT_LIMIT_PER_BCU,
								new UnsignedWordElement(520), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(FeneconHomeBattery.ChannelId.MAX_DC_DISCHARGE_CURRENT_LIMIT_PER_BCU,
								new UnsignedWordElement(521), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(FeneconHomeBattery.ChannelId.RACK_NUMBER_OF_BATTERY_BCU, new UnsignedWordElement(522)), //
						m(FeneconHomeBattery.ChannelId.RACK_NUMBER_OF_CELLS_IN_SERIES_PER_MODULE,
								new UnsignedWordElement(523)), //
						m(new UnsignedWordElement(524)) //
								.m(FeneconHomeBattery.ChannelId.RACK_MAX_CELL_VOLTAGE_LIMIT,
										ElementToChannelConverter.DIRECT_1_TO_1)
								.m(Battery.ChannelId.MAX_CELL_VOLTAGE, ElementToChannelConverter.DIRECT_1_TO_1) // [%]
								.build(), //
						m(new UnsignedWordElement(525)) //
								.m(FeneconHomeBattery.ChannelId.RACK_MAX_CELL_VOLTAGE_LIMIT,
										ElementToChannelConverter.DIRECT_1_TO_1)
								.m(Battery.ChannelId.MAX_CELL_VOLTAGE, ElementToChannelConverter.DIRECT_1_TO_1) // [%]
								.build(), //
						m(new BitsWordElement(526, this) //
								.bit(0, FeneconHomeBattery.ChannelId.RACK_HW_AFE_COMMUNICATION_FAULT) //
								.bit(1, FeneconHomeBattery.ChannelId.RACK_HW_ACTOR_DRIVER_FAULT) //
								.bit(2, FeneconHomeBattery.ChannelId.RACK_HW_EEPROM_COMMUNICATION_FAULT) //
								.bit(3, FeneconHomeBattery.ChannelId.RACK_HW_VOLTAGE_DETECT_FAULT) //
								.bit(4, FeneconHomeBattery.ChannelId.RACK_HW_TEMPERATURE_DETECT_FAULT) //
								.bit(5, FeneconHomeBattery.ChannelId.RACK_HW_CURRENT_DETECT_FAULT) //
								.bit(6, FeneconHomeBattery.ChannelId.RACK_HW_ACTOR_NOT_CLOSE) //
								.bit(7, FeneconHomeBattery.ChannelId.RACK_HW_ACTOR_NOT_OPEN) //
								.bit(8, FeneconHomeBattery.ChannelId.RACK_HW_FUSE_BROKEN)), //
						m(new BitsWordElement(527, this) //
								.bit(0, FeneconHomeBattery.ChannelId.RACK_SYSTEM_AFE_OVER_TEMPERATURE) //
								.bit(1, FeneconHomeBattery.ChannelId.RACK_SYSTEM_AFE_UNDER_TEMPERATURE) //
								.bit(2, FeneconHomeBattery.ChannelId.RACK_SYSTEM_AFE_OVER_VOLTAGE) //
								.bit(3, FeneconHomeBattery.ChannelId.RACK_SYSTEM_AFE_UNDER_VOLTAGE) //
								.bit(4, FeneconHomeBattery.ChannelId.RACK_SYSTEM_HIGH_TEMPERATURE_PERMANENT_FAILURE) //
								.bit(5, FeneconHomeBattery.ChannelId.RACK_SYSTEM_LOW_TEMPERATURE_PERMANENT_FAILURE) //
								.bit(6, FeneconHomeBattery.ChannelId.RACK_SYSTEM_HIGH_CELL_VOLTAGE_PERMANENT_FAILURE) //
								.bit(7, FeneconHomeBattery.ChannelId.RACK_SYSTEM_LOW_CELL_VOLTAGE_PERMANENT_FAILURE) //
								.bit(8, FeneconHomeBattery.ChannelId.RACK_SYSTEM_SHORT_CIRCUIT)), //
						m(FeneconHomeBattery.ChannelId.UPPER_VOLTAGE, new UnsignedWordElement(528)))); //
		new FC3ReadRegistersTask(44000, Priority.HIGH, //
				m(FeneconHomeBattery.ChannelId.BMS_CONTROL, new UnsignedWordElement(44000)) //
		);
		return protocol;
	}

	private void BcuDynamicChannels(int bcuNumber) throws OpenemsException {
		try {
			for (int i = 1; i <= bcuNumber; i++) {
				String towerString = "TOWER_" + i + "_OFFSET";
				int towerOffset = ModuleParameters.valueOf(towerString).getValue();
				this.getModbusProtocol().addTasks(//
						new FC3ReadRegistersTask(towerOffset + 2, Priority.LOW, //
								m(new BitsWordElement(towerOffset + 2, this)//
										.bit(0, generateBcuChannel(i, "STATUS_ALARM")) //
										.bit(1, generateBcuChannel(i, "STATUS_WARNING")) //
										.bit(2, generateBcuChannel(i, "STATUS_FAULT")) //
										.bit(3, generateBcuChannel(i, "STATUS_PFET")) //
										.bit(4, generateBcuChannel(i, "STATUS_CFET")) //
										.bit(5, generateBcuChannel(i, "STATUS_DFET")) //
										.bit(6, generateBcuChannel(i, "STATUS_BATTERY_IDLE")) //
										.bit(7, generateBcuChannel(i, "STATUS_BATTERY_CHARGING")) //
										.bit(8, generateBcuChannel(i, "STATUS_BATTERY_DISCHARGING"))//
								), //
								m(new BitsWordElement(towerOffset + 3, this)
										.bit(0, generateBcuChannel(i, "PRE_ALARM_CELL_OVER_VOLTAGE")) //
										.bit(1, generateBcuChannel(i, "PRE_ALARM_CELL_UNDER_VOLTAGE")) //
										.bit(2, generateBcuChannel(i, "PRE_ALARM_OVER_CHARGING_CURRENT")) //
										.bit(3, generateBcuChannel(i, "PRE_ALARM_OVER_DISCHARGING_CURRENT")) //
										.bit(4, generateBcuChannel(i, "PRE_ALARM_OVER_TEMPERATURE")) //
										.bit(5, generateBcuChannel(i, "PRE_ALARM_UNDER_TEMPERATURE")) //
										.bit(6, generateBcuChannel(i, "PRE_ALARM_CELL_VOLTAGE_DIFFERENCE")) //
										.bit(7, generateBcuChannel(i, "PRE_ALARM_BCU_TEMP_DIFFERENCE")) //
										.bit(8, generateBcuChannel(i, "PRE_ALARM_UNDER_SOC")) //
										.bit(9, generateBcuChannel(i, "PRE_ALARM_UNDER_SOH")) //
										.bit(10, generateBcuChannel(i, "PRE_ALARM_OVER_CHARGING_POWER")) //
										.bit(11, generateBcuChannel(i, "PRE_ALARM_OVER_DISCHARGING_POWER"))), //
								m(new BitsWordElement(towerOffset + 4, this)
										.bit(0, generateBcuChannel(i, "LEVEL_1_CELL_OVER_VOLTAGE")) //
										.bit(1, generateBcuChannel(i, "LEVEL_1_CELL_UNDER_VOLTAGE")) //
										.bit(2, generateBcuChannel(i, "LEVEL_1_OVER_CHARGING_CURRENT")) //
										.bit(3, generateBcuChannel(i, "LEVEL_1_OVER_DISCHARGING_CURRENT")) //
										.bit(4, generateBcuChannel(i, "LEVEL_1_OVER_TEMPERATURE")) //
										.bit(5, generateBcuChannel(i, "LEVEL_1_UNDER_TEMPERATURE")) //
										.bit(6, generateBcuChannel(i, "LEVEL_1_CELL_VOLTAGE_DIFFERENCE")) //
										.bit(7, generateBcuChannel(i, "LEVEL_1_BCU_TEMP_DIFFERENCE")) //
										.bit(8, generateBcuChannel(i, "LEVEL_1_UNDER_SOC")) //
										.bit(9, generateBcuChannel(i, "LEVEL_1_UNDER_SOH")) //
										.bit(10, generateBcuChannel(i, "LEVEL_1_OVER_CHARGING_POWER")) //
										.bit(11, generateBcuChannel(i, "LEVEL_1_OVER_DISCHARGING_POWER"))),
								m(new BitsWordElement(towerOffset + 5, this)
										.bit(0, generateBcuChannel(i, "LEVEL_2_CELL_OVER_VOLTAGE")) //
										.bit(1, generateBcuChannel(i, "LEVEL_2_CELL_UNDER_VOLTAGE")) //
										.bit(2, generateBcuChannel(i, "LEVEL_2_OVER_CHARGING_CURRENT")) //
										.bit(3, generateBcuChannel(i, "LEVEL_2_OVER_DISCHARGING_CURRENT")) //
										.bit(4, generateBcuChannel(i, "LEVEL_2_OVER_TEMPERATURE")) //
										.bit(5, generateBcuChannel(i, "LEVEL_2_UNDER_TEMPERATURE")) //
										.bit(6, generateBcuChannel(i, "LEVEL_2_CELL_VOLTAGE_DIFFERENCE")) //
										.bit(7, generateBcuChannel(i, "LEVEL_2_BCU_TEMP_DIFFERENCE")) //
										.bit(8, generateBcuChannel(i, "LEVEL_2_TEMPERATURE_DIFFERENCE")) //
										.bit(9, generateBcuChannel(i, "LEVEL_2_INTERNAL_COMMUNICATION")) //
										.bit(10, generateBcuChannel(i, "LEVEL_2_EXTERNAL_COMMUNICATION")) //
										.bit(11, generateBcuChannel(i, "LEVEL_2_PRECHARGE_FAIL")) //
										.bit(12, generateBcuChannel(i, "LEVEL_2_PARALLEL_FAIL")) //
										.bit(13, generateBcuChannel(i, "LEVEL_2_SYSTEM_FAIL")) //
										.bit(14, generateBcuChannel(i, "LEVEL_2_HARDWARE_FAIL"))), //
								m(new BitsWordElement(towerOffset + 6, this)
										.bit(0, generateBcuChannel(i, "HW_AFE_COMMUNICAITON_FAULT")) //
										.bit(1, generateBcuChannel(i, "HW_ACTOR_DRIVER_FAULT")) //
										.bit(2, generateBcuChannel(i, "HW_EEPROM_COMMUNICATION_FAULT")) //
										.bit(3, generateBcuChannel(i, "HW_VOLTAGE_DETECT_FAULT")) //
										.bit(4, generateBcuChannel(i, "HW_TEMPERATURE_DETECT_FAULT")) //
										.bit(5, generateBcuChannel(i, "HW_CURRENT_DETECT_FAULT")) //
										.bit(6, generateBcuChannel(i, "HW_ACTOR_NOT_CLOSE")) //
										.bit(7, generateBcuChannel(i, "HW_ACTOR_NOT_OPEN")) //
										.bit(8, generateBcuChannel(i, "HW_FUSE_BROKEN"))), //
								m(new BitsWordElement(towerOffset + 7, this)
										.bit(0, generateBcuChannel(i, "SYSTEM_AFE_OVER_TEMPERATURE")) //
										.bit(1, generateBcuChannel(i, "SYSTEM_AFE_UNDER_TEMPERATURE")) //
										.bit(2, generateBcuChannel(i, "SYSTEM_AFE_OVER_VOLTAGE")) //
										.bit(3, generateBcuChannel(i, "SYSTEM_AFE_UNDER_VOLTAGE")) //
										.bit(4, generateBcuChannel(i, "SYSTEM_HIGH_TEMPERATURE_PERMANENT_FAILURE")) //
										.bit(5, generateBcuChannel(i, "SYSTEM_LOW_TEMPERATURE_PERMANENT_FAILURE")) //
										.bit(6, generateBcuChannel(i, "SYSTEM_HIGH_CELL_VOLTAGE_PERMANENT_FAILURE")) //
										.bit(7, generateBcuChannel(i, "SYSTEM_LOW_CELL_VOLTAGE_PERMANENT_FAILURE")) //
										.bit(8, generateBcuChannel(i, "SYSTEM_SHORT_CIRCUIT"))), //
								m(generateBcuChannel(i, "_SOC"), new UnsignedWordElement(towerOffset + 8), // [%]
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
								m(generateBcuChannel(i, "_SOH"), new UnsignedWordElement(towerOffset + 9), // [%]
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
								m(generateBcuChannel(i, "_VOLTAGE"), new UnsignedWordElement(towerOffset + 10), // [V]
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
								m(generateBcuChannel(i, "_CURRENT"), new UnsignedWordElement(towerOffset + 11), // [A]
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
								m(generateBcuChannel(i, "_MIN_CELL_VOLTAGE"),
										new UnsignedWordElement(towerOffset + 12)), // [mV]
								m(generateBcuChannel(i, "_MAX_CELL_VOLTAGE"),
										new UnsignedWordElement(towerOffset + 13)), // [mV]
								m(generateBcuChannel(i, "_AVARAGE_CELL_VOLTAGE"),
										new UnsignedWordElement(towerOffset + 14)), //
								m(generateBcuChannel(i, "_MAX_CHARGE_CURRENT"),
										new UnsignedWordElement(towerOffset + 15)), //
								m(generateBcuChannel(i, "_MIN_CHARGE_CURRENT"),
										new UnsignedWordElement(towerOffset + 16)), //
								m(generateBcuChannel(i, "_BMS_SERIAL_NUMBER"),
										new UnsignedWordElement(towerOffset + 17)), //
								m(generateBcuChannel(i, "_NO_OF_CYCLES"), new UnsignedWordElement(towerOffset + 18)), //
								m(new UnsignedWordElement(towerOffset + 19)) //
										.m(generateBcuChannel(i, "_DESIGN_CAPACITY"),
												ElementToChannelConverter.SCALE_FACTOR_MINUS_1) // [Ah]
										.m(Battery.ChannelId.CAPACITY, ElementToChannelConverter.DIRECT_1_TO_1) // [%]
										.build(), //
								m(generateBcuChannel(i, "_USABLE_CAPACITY"), new UnsignedWordElement(towerOffset + 20), //
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // [Ah]
								m(generateBcuChannel(i, "_REMAINING_CAPACITY"),
										new UnsignedWordElement(towerOffset + 21), //
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // [Ah]
								m(generateBcuChannel(i, "_MAX_CELL_VOLTAGE_LIMIT"),
										new UnsignedWordElement(towerOffset + 22)), //
								m(generateBcuChannel(i, "_MIN_CELL_VOLTAGE_LIMIT"),
										new UnsignedWordElement(towerOffset + 23))), //
						new FC3ReadRegistersTask(towerOffset + 24, Priority.HIGH, //
								m(new UnsignedWordElement(towerOffset + 24)
										.onUpdateCallback(this.onRegister10024Update))//
												.m(generateBcuChannel(i, "_BMU_NUMBER"), new ElementToChannelConverter( //
														value -> {
															if (value == null) {
																return null;
															}
															int moduleNumber = (Integer) value;
															IntegerReadChannel maxChargeVoltageChannel = this
																	.channel(Battery.ChannelId.CHARGE_MAX_VOLTAGE);
															int chargeMaxVoltageValue = moduleNumber
																	* ModuleParameters.MODULE_MAX_VOLTAGE.getValue();
															maxChargeVoltageChannel.setNextValue(chargeMaxVoltageValue);

															IntegerReadChannel minDischargeVoltageChannel = this
																	.channel(Battery.ChannelId.DISCHARGE_MIN_VOLTAGE);
															int minDischargeVoltageValue = moduleNumber
																	* ModuleParameters.MODULE_MIN_VOLTAGE.getValue();
															minDischargeVoltageChannel
																	.setNextValue(minDischargeVoltageValue);
															return value;
														}, // channel -> element
														value -> value) //
												).build()));//
			}
		} catch (OpenemsException e) {
			this.log.info("Dynamic Bcu Channels could not created");
		} //
	}

	protected final void identifyBcuNumberChannels() {
		this.getBcuNumberIdentifier().thenAccept(value -> {
			try {
				this.BcuDynamicChannels(value);
				this.towerNum = value;
			} catch (OpenemsException e) {
				e.printStackTrace();
			}
		});
	}

	/**
	 * Gets the Bcu/Tower Number identifier via Modbus.
	 * 
	 * @return the future Integer; returns a default value as 1 on error
	 */
	private CompletableFuture<Integer> getBcuNumberIdentifier() {
		final CompletableFuture<Integer> numbOfBCU = new CompletableFuture<Integer>();
		try {
			ModbusUtils.readELementOnce(this.getModbusProtocol(), new UnsignedWordElement(12000), true)
					.thenAccept(value -> {
						if ((value != 0 && value != null)) {
							numbOfBCU.complete(2);
						}
						try {
							ModbusUtils.readELementOnce(this.getModbusProtocol(), new UnsignedWordElement(14000), true)
									.thenAccept(name -> {
										if (value != 0 && value != null) {
											numbOfBCU.complete(3);
										}
									});
						} catch (OpenemsException e) {
							this.logWarn(this.log, "Error while trying to identify Bcu Number: " + e.getMessage());
							e.printStackTrace();
							numbOfBCU.complete(2);
						}
					});
		} catch (OpenemsException e) {
			this.logWarn(this.log, "Error while trying to identify Bcu Number: " + e.getMessage());
			e.printStackTrace();
			numbOfBCU.complete(1);
		}
		return numbOfBCU;
	}

	private String getSingleCellPrefix(int num, int module, int tower) {
		return "TOWER_" + tower + "_MODULE_" + module + "_CELL_" + String.format(NUMBER_FORMAT, num);
	}

	io.openems.edge.common.channel.ChannelId generateBcuChannel(int bcuNumber, String channelIdSuffix) {
		io.openems.edge.common.channel.ChannelId channelId = new ChannelIdImpl(
				"BCU_" + bcuNumber + "_" + channelIdSuffix, Doc.of(OpenemsType.BOOLEAN));
		this.addChannel(channelId);
		return channelId;
	}

	/*
	 * creates a map containing channels for voltage and temperature depending on
	 * the number of modules
	 */
	private Map<String, Channel<?>> createCellVoltAndTempDynamicChannels(int bmuNumber) {
		Map<String, Channel<?>> map = new HashMap<>();
		int voltSensors = ModuleParameters.VOLTAGE_SENSORS_PER_MODULE.getValue();
		for (int t = 1; t <= towerNum; t++) {
			for (int i = 1; i <= bmuNumber; i++) {
				for (int j = 0; j < voltSensors; j++) {
					String key = this.getSingleCellPrefix(j, i, t) + KEY_VOLTAGE;
					IntegerDoc doc = new IntegerDoc();
					io.openems.edge.common.channel.ChannelId channelId = new ChannelIdImpl(key,
							doc.unit(Unit.MILLIVOLT));
					IntegerReadChannel integerReadChannel = (IntegerReadChannel) this.addChannel(channelId);
					map.put(key, integerReadChannel);
				}
			}
		}
		int tempSensors = ModuleParameters.TEMPERATURE_SENSORS_PER_MODULE.getValue();
		for (int t = 1; t <= this.towerNum; t++) {
			for (int i = 1; i <= bmuNumber; i++) {
				for (int j = 0; j < tempSensors; j++) {
					String key = this.getSingleCellPrefix(j, i, t) + KEY_TEMPERATURE;
					IntegerDoc doc = new IntegerDoc();
					io.openems.edge.common.channel.ChannelId channelId = new ChannelIdImpl(key,
							doc.unit(Unit.DEZIDEGREE_CELSIUS));
					IntegerReadChannel integerReadChannel = (IntegerReadChannel) this.addChannel(channelId);
					map.put(key, integerReadChannel);
				}
			}
		}
		return map;
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

	@Override
	public void setStartStop(StartStop value) {
		if (this.startStopTarget.getAndSet(value) != value) {
			// Set only if value changed
			this.stateMachine.forceNextState(State.UNDEFINED);
		}
	}

	/*
	 * Handle incompatibility with old hardware protocol.
	 * 
	 * 'onRegister0x10024update()' callback is called when register 0x10024 is read.
	 * 10024 is the read register for Module Number of Bcu 1 All Tower should have
	 * same amount of module number
	 */
	private boolean areChannelsInitialized = false;
	private final Consumer<Integer> onRegister10024Update = (value) -> {
		if (value == null) {
			// ignore invalid values; modbus bridge has no connection yet
			return;
		}
		// Try to read MODULE_QTY Register
		try {
			ModbusUtils.readELementOnce(this.getModbusProtocol(), new UnsignedWordElement(10024), false)
					.thenAccept(moduleQtyValue -> {
						if (moduleQtyValue != null) {
							// Are Channel Initialized ?
							if (!FeneconHomeBatteryImpl.this.areChannelsInitialized) {
								this.channelMap = this.createCellVoltAndTempDynamicChannels(moduleQtyValue);
								FeneconHomeBatteryImpl.this.areChannelsInitialized = true;
							}
							// Register is available -> add Registers for current hardware to protocol
							try {
								int offset = ModuleParameters.ADDRESS_OFFSET_FOR_CELL_VOLT_AND_TEMP.getValue();
								int voltOffset = ModuleParameters.VOLTAGE_ADDRESS_OFFSET.getValue();
								int voltSensors = ModuleParameters.VOLTAGE_SENSORS_PER_MODULE.getValue();
								for (int t = 1; t <= this.towerNum; t++) {
									String towerString = "TOWER_" + t + "_OFFSET";
									int towerOffset = ModuleParameters.valueOf(towerString).getValue();
									for (int i = 1; i < moduleQtyValue + 1; i++) {
										Collection<AbstractModbusElement<?>> elements = new ArrayList<>();
										for (int j = 0; j < voltSensors; j++) {
											String key = this.getSingleCellPrefix(j, i, t) + KEY_VOLTAGE;
											UnsignedWordElement uwe = new UnsignedWordElement(
													towerOffset + i * offset + voltOffset + j);
											AbstractModbusElement<?> ame = m(this.channelMap.get(key).channelId(), uwe);
											elements.add(ame);
										}
										this.getModbusProtocol()
												.addTask(new FC3ReadRegistersTask(towerOffset + offset * i + voltOffset,
														Priority.HIGH,
														elements.toArray(new AbstractModbusElement<?>[0])));
									}
								}

								int tempOffset = ModuleParameters.TEMPERATURE_ADDRESS_OFFSET.getValue();
								int tempSensors = ModuleParameters.TEMPERATURE_SENSORS_PER_MODULE.getValue();
								for (int t = 1; t <= this.towerNum; t++) {
									String towerString = "TOWER_" + t + "_OFFSET";
									int towerOffset = ModuleParameters.valueOf(towerString).getValue();
									for (int i = 1; i < moduleQtyValue + 1; i++) {
										Collection<AbstractModbusElement<?>> elements = new ArrayList<>();
										for (int j = 0; j < tempSensors; j++) {
											String key = this.getSingleCellPrefix(j, i, t) + KEY_TEMPERATURE;
											UnsignedWordElement uwe = new UnsignedWordElement(
													towerOffset + i * offset + tempOffset + j);
											AbstractModbusElement<?> ame = m(this.channelMap.get(key).channelId(), uwe);
											elements.add(ame);
										}
										this.getModbusProtocol()
												.addTask(new FC3ReadRegistersTask(towerOffset + offset * i + tempOffset,
														Priority.HIGH,
														elements.toArray(new AbstractModbusElement<?>[0])));
									}
								}
							} catch (OpenemsException e) {
								FeneconHomeBatteryImpl.this.logError(FeneconHomeBatteryImpl.this.log,
										"Unable to add registers for detected hardware version: " + e.getMessage());
								e.printStackTrace();
							} //
						}
					});
		} catch (OpenemsException e) {
			FeneconHomeBatteryImpl.this.logError(FeneconHomeBatteryImpl.this.log,
					"Unable to detect hardware version: " + e.getMessage());
			e.printStackTrace();
		}
	};

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
