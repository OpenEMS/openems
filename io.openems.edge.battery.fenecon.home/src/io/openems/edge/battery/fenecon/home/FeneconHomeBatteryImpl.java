package io.openems.edge.battery.fenecon.home;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
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
import io.openems.edge.common.channel.Doc;
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

	private static final int TEMPERATURE_ADDRESS_OFFSET = 18;
	private static final int VOLTAGE_ADDRESS_OFFSET = 2;
	private static final int SENSORS_PER_MODULE = 14;
	private static final int ADDRESS_OFFSET_FOR_CELL_VOLT_AND_TEMP = 100;
	private static final int MODULE_MIN_VOLTAGE = 42; // [V]
	private static final int MODULE_MAX_VOLTAGE = 49; // [V]
	private static final int CAPACITY_PER_MODULE = 2200; // [Wh]

	private final Logger log = LoggerFactory.getLogger(FeneconHomeBatteryImpl.class);

	@Reference
	protected ConfigurationAdmin cm;

	/**
	 * Manages the {@link State}s of the StateMachine.
	 */
	private final StateMachine stateMachine = new StateMachine(State.UNDEFINED);

	private final AtomicReference<StartStop> startStopTarget = new AtomicReference<StartStop>(StartStop.UNDEFINED);

	private Config config;

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
		// Asynchronously read numberOfTowers and numberOfModulesPerTower
		this.getNumberOfTowers().thenAccept(numberOfTowers -> {
			this.getNumberOfModulesPerTowers().thenAccept(numberOfModulesPerTower -> {
				int chargeMaxVoltageValue = numberOfModulesPerTower * MODULE_MAX_VOLTAGE;
				// Set Battery Charge Max Voltage
				this._setChargeMaxVoltage(chargeMaxVoltageValue);
				// Set Battery Discharge Min Voltage
				int minDischargeVoltageValue = numberOfModulesPerTower * MODULE_MIN_VOLTAGE;
				this._setDischargeMinVoltage(minDischargeVoltageValue);
				// Initialize available Tower- and Module-Channels dynamically.
				this._setCapacity(numberOfTowers * numberOfModulesPerTower * CAPACITY_PER_MODULE);
				this.initializeTowerModulesChannels(numberOfTowers, numberOfModulesPerTower);
			});
		});
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id());
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
		return new ModbusProtocol(this, //
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
				), //

				new FC3ReadRegistersTask(506, Priority.HIGH, //
						m(Battery.ChannelId.VOLTAGE, new UnsignedWordElement(506),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // [V]
						m(Battery.ChannelId.CURRENT, new UnsignedWordElement(507),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // [A]
						m(Battery.ChannelId.SOC, new UnsignedWordElement(508),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // [%]
						m(Battery.ChannelId.SOH, new UnsignedWordElement(509),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // [%]
						m(Battery.ChannelId.MIN_CELL_VOLTAGE, new UnsignedWordElement(510)), // [mV]
						m(FeneconHomeBattery.ChannelId.ID_OF_CELL_VOLTAGE_MIN, new UnsignedWordElement(511)), //
						m(Battery.ChannelId.MAX_CELL_VOLTAGE, new UnsignedWordElement(512)), // [mV]
						m(FeneconHomeBattery.ChannelId.ID_OF_CELL_VOLTAGE_MAX, new UnsignedWordElement(513)), //
						m(Battery.ChannelId.MIN_CELL_TEMPERATURE, new UnsignedWordElement(514),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(FeneconHomeBattery.ChannelId.ID_OF_MIN_TEMPERATURE, new UnsignedWordElement(515)), //
						m(Battery.ChannelId.MAX_CELL_TEMPERATURE, new UnsignedWordElement(516),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(FeneconHomeBattery.ChannelId.ID_OF_MAX_TEMPERATURE, new UnsignedWordElement(517)), //
						m(Battery.ChannelId.CHARGE_MAX_CURRENT, new UnsignedWordElement(518),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // [A]
						m(Battery.ChannelId.DISCHARGE_MAX_CURRENT, new UnsignedWordElement(519), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // [A]
						m(FeneconHomeBattery.ChannelId.MAX_DC_CHARGE_CURRENT_LIMIT_PER_BCU,
								new UnsignedWordElement(520), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(FeneconHomeBattery.ChannelId.MAX_DC_DISCHARGE_CURRENT_LIMIT_PER_BCU,
								new UnsignedWordElement(521), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(FeneconHomeBattery.ChannelId.RACK_NUMBER_OF_BATTERY_BCU, new UnsignedWordElement(522)), //
						m(FeneconHomeBattery.ChannelId.RACK_NUMBER_OF_CELLS_IN_SERIES_PER_MODULE,
								new UnsignedWordElement(523)), //
						m(FeneconHomeBattery.ChannelId.RACK_MAX_CELL_VOLTAGE_LIMIT, new UnsignedWordElement(524)), //
						m(FeneconHomeBattery.ChannelId.RACK_MIN_CELL_VOLTAGE_LIMIT, new UnsignedWordElement(525)), //
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
						m(FeneconHomeBattery.ChannelId.UPPER_VOLTAGE, new UnsignedWordElement(528))), //
				new FC3ReadRegistersTask(44000, Priority.HIGH, //
						m(FeneconHomeBattery.ChannelId.BMS_CONTROL, new UnsignedWordElement(44000)) //
				));
	}

	private void initializeTowerModulesChannels(int numberOfTowers, int numberOfModulePerTower) {
		try {
			for (int t = 1; t <= numberOfTowers; t++) {
				final int towerOffset = (t - 1) * 2000 + 10000;
				this.getModbusProtocol().addTasks(//
						new FC3ReadRegistersTask(towerOffset + 2, Priority.LOW, //
								m(new BitsWordElement(towerOffset + 2, this)//
										.bit(0, this.generateTowerChannel(t, "STATUS_ALARM")) //
										.bit(1, this.generateTowerChannel(t, "STATUS_WARNING")) //
										.bit(2, this.generateTowerChannel(t, "STATUS_FAULT")) //
										.bit(3, this.generateTowerChannel(t, "STATUS_PFET")) //
										.bit(4, this.generateTowerChannel(t, "STATUS_CFET")) //
										.bit(5, this.generateTowerChannel(t, "STATUS_DFET")) //
										.bit(6, this.generateTowerChannel(t, "STATUS_BATTERY_IDLE")) //
										.bit(7, this.generateTowerChannel(t, "STATUS_BATTERY_CHARGING")) //
										.bit(8, this.generateTowerChannel(t, "STATUS_BATTERY_DISCHARGING"))//
								), //
								m(new BitsWordElement(towerOffset + 3, this)
										.bit(0, this.generateTowerChannel(t, "PRE_ALARM_CELL_OVER_VOLTAGE")) //
										.bit(1, this.generateTowerChannel(t, "PRE_ALARM_CELL_UNDER_VOLTAGE")) //
										.bit(2, this.generateTowerChannel(t, "PRE_ALARM_OVER_CHARGING_CURRENT")) //
										.bit(3, this.generateTowerChannel(t, "PRE_ALARM_OVER_DISCHARGING_CURRENT")) //
										.bit(4, this.generateTowerChannel(t, "PRE_ALARM_OVER_TEMPERATURE")) //
										.bit(5, this.generateTowerChannel(t, "PRE_ALARM_UNDER_TEMPERATURE")) //
										.bit(6, this.generateTowerChannel(t, "PRE_ALARM_CELL_VOLTAGE_DIFFERENCE")) //
										.bit(7, this.generateTowerChannel(t, "PRE_ALARM_BCU_TEMP_DIFFERENCE")) //
										.bit(8, this.generateTowerChannel(t, "PRE_ALARM_UNDER_SOC")) //
										.bit(9, this.generateTowerChannel(t, "PRE_ALARM_UNDER_SOH")) //
										.bit(10, this.generateTowerChannel(t, "PRE_ALARM_OVER_CHARGING_POWER")) //
										.bit(11, this.generateTowerChannel(t, "PRE_ALARM_OVER_DISCHARGING_POWER"))), //
								m(new BitsWordElement(towerOffset + 4, this)
										.bit(0, this.generateTowerChannel(t, "LEVEL_1_CELL_OVER_VOLTAGE")) //
										.bit(1, this.generateTowerChannel(t, "LEVEL_1_CELL_UNDER_VOLTAGE")) //
										.bit(2, this.generateTowerChannel(t, "LEVEL_1_OVER_CHARGING_CURRENT")) //
										.bit(3, this.generateTowerChannel(t, "LEVEL_1_OVER_DISCHARGING_CURRENT")) //
										.bit(4, this.generateTowerChannel(t, "LEVEL_1_OVER_TEMPERATURE")) //
										.bit(5, this.generateTowerChannel(t, "LEVEL_1_UNDER_TEMPERATURE")) //
										.bit(6, this.generateTowerChannel(t, "LEVEL_1_CELL_VOLTAGE_DIFFERENCE")) //
										.bit(7, this.generateTowerChannel(t, "LEVEL_1_BCU_TEMP_DIFFERENCE")) //
										.bit(8, this.generateTowerChannel(t, "LEVEL_1_UNDER_SOC")) //
										.bit(9, this.generateTowerChannel(t, "LEVEL_1_UNDER_SOH")) //
										.bit(10, this.generateTowerChannel(t, "LEVEL_1_OVER_CHARGING_POWER")) //
										.bit(11, this.generateTowerChannel(t, "LEVEL_1_OVER_DISCHARGING_POWER"))),
								m(new BitsWordElement(towerOffset + 5, this)
										.bit(0, this.generateTowerChannel(t, "LEVEL_2_CELL_OVER_VOLTAGE")) //
										.bit(1, this.generateTowerChannel(t, "LEVEL_2_CELL_UNDER_VOLTAGE")) //
										.bit(2, this.generateTowerChannel(t, "LEVEL_2_OVER_CHARGING_CURRENT")) //
										.bit(3, this.generateTowerChannel(t, "LEVEL_2_OVER_DISCHARGING_CURRENT")) //
										.bit(4, this.generateTowerChannel(t, "LEVEL_2_OVER_TEMPERATURE")) //
										.bit(5, this.generateTowerChannel(t, "LEVEL_2_UNDER_TEMPERATURE")) //
										.bit(6, this.generateTowerChannel(t, "LEVEL_2_CELL_VOLTAGE_DIFFERENCE")) //
										.bit(7, this.generateTowerChannel(t, "LEVEL_2_BCU_TEMP_DIFFERENCE")) //
										.bit(8, this.generateTowerChannel(t, "LEVEL_2_TEMPERATURE_DIFFERENCE")) //
										.bit(9, this.generateTowerChannel(t, "LEVEL_2_INTERNAL_COMMUNICATION")) //
										.bit(10, this.generateTowerChannel(t, "LEVEL_2_EXTERNAL_COMMUNICATION")) //
										.bit(11, this.generateTowerChannel(t, "LEVEL_2_PRECHARGE_FAIL")) //
										.bit(12, this.generateTowerChannel(t, "LEVEL_2_PARALLEL_FAIL")) //
										.bit(13, this.generateTowerChannel(t, "LEVEL_2_SYSTEM_FAIL")) //
										.bit(14, this.generateTowerChannel(t, "LEVEL_2_HARDWARE_FAIL"))), //
								m(new BitsWordElement(towerOffset + 6, this)
										.bit(0, this.generateTowerChannel(t, "HW_AFE_COMMUNICAITON_FAULT")) //
										.bit(1, this.generateTowerChannel(t, "HW_ACTOR_DRIVER_FAULT")) //
										.bit(2, this.generateTowerChannel(t, "HW_EEPROM_COMMUNICATION_FAULT")) //
										.bit(3, this.generateTowerChannel(t, "HW_VOLTAGE_DETECT_FAULT")) //
										.bit(4, this.generateTowerChannel(t, "HW_TEMPERATURE_DETECT_FAULT")) //
										.bit(5, this.generateTowerChannel(t, "HW_CURRENT_DETECT_FAULT")) //
										.bit(6, this.generateTowerChannel(t, "HW_ACTOR_NOT_CLOSE")) //
										.bit(7, this.generateTowerChannel(t, "HW_ACTOR_NOT_OPEN")) //
										.bit(8, this.generateTowerChannel(t, "HW_FUSE_BROKEN"))), //
								m(new BitsWordElement(towerOffset + 7, this)
										.bit(0, this.generateTowerChannel(t, "SYSTEM_AFE_OVER_TEMPERATURE")) //
										.bit(1, this.generateTowerChannel(t, "SYSTEM_AFE_UNDER_TEMPERATURE")) //
										.bit(2, this.generateTowerChannel(t, "SYSTEM_AFE_OVER_VOLTAGE")) //
										.bit(3, this.generateTowerChannel(t, "SYSTEM_AFE_UNDER_VOLTAGE")) //
										.bit(4, this.generateTowerChannel(t,
												"SYSTEM_HIGH_TEMPERATURE_PERMANENT_FAILURE")) //
										.bit(5, this.generateTowerChannel(t,
												"SYSTEM_LOW_TEMPERATURE_PERMANENT_FAILURE")) //
										.bit(6, this.generateTowerChannel(t,
												"SYSTEM_HIGH_CELL_VOLTAGE_PERMANENT_FAILURE")) //
										.bit(7, this.generateTowerChannel(t,
												"SYSTEM_LOW_CELL_VOLTAGE_PERMANENT_FAILURE")) //
										.bit(8, this.generateTowerChannel(t, "SYSTEM_SHORT_CIRCUIT"))), //
								m(this.generateTowerChannel(t, "_SOC"), new UnsignedWordElement(towerOffset + 8), // [%]
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
								m(this.generateTowerChannel(t, "_SOH"), new UnsignedWordElement(towerOffset + 9), // [%]
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
								m(this.generateTowerChannel(t, "_VOLTAGE"), new UnsignedWordElement(towerOffset + 10), // [V]
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
								m(this.generateTowerChannel(t, "_CURRENT"), new UnsignedWordElement(towerOffset + 11), // [A]
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
								m(this.generateTowerChannel(t, "_MIN_CELL_VOLTAGE"),
										new UnsignedWordElement(towerOffset + 12)), // [mV]
								m(this.generateTowerChannel(t, "_MAX_CELL_VOLTAGE"),
										new UnsignedWordElement(towerOffset + 13)), // [mV]
								m(this.generateTowerChannel(t, "_AVARAGE_CELL_VOLTAGE"),
										new UnsignedWordElement(towerOffset + 14)), //
								m(this.generateTowerChannel(t, "_MAX_CHARGE_CURRENT"),
										new UnsignedWordElement(towerOffset + 15)), //
								m(this.generateTowerChannel(t, "_MIN_CHARGE_CURRENT"),
										new UnsignedWordElement(towerOffset + 16)), //
								m(this.generateTowerChannel(t, "_BMS_SERIAL_NUMBER"),
										new UnsignedWordElement(towerOffset + 17)), //
								m(this.generateTowerChannel(t, "_NO_OF_CYCLES"),
										new UnsignedWordElement(towerOffset + 18)), //
								m(new UnsignedWordElement(towerOffset + 19)) //
										.m(this.generateTowerChannel(t, "_DESIGN_CAPACITY"),
												ElementToChannelConverter.SCALE_FACTOR_MINUS_1) // [Ah]
										.build(), //
								m(this.generateTowerChannel(t, "_USABLE_CAPACITY"),
										new UnsignedWordElement(towerOffset + 20), //
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // [Ah]
								m(this.generateTowerChannel(t, "_REMAINING_CAPACITY"),
										new UnsignedWordElement(towerOffset + 21), //
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // [Ah]
								m(this.generateTowerChannel(t, "_MAX_CELL_VOLTAGE_LIMIT"),
										new UnsignedWordElement(towerOffset + 22)), //
								m(this.generateTowerChannel(t, "_MIN_CELL_VOLTAGE_LIMIT"),
										new UnsignedWordElement(towerOffset + 23))));

				/*
				 * Dynamically generate Channels and Modbus mappings for Cell-Temperatures and
				 * for Cell-Voltages.Channel-IDs are like "TOWER_1_OFFSET_2_TEMPERATURE_003".
				 * Channel-IDs are like "TOWER_1_OFFSET_2_VOLTAGE_003".
				 */
				for (int i = 1; i < numberOfModulePerTower + 1; i++) {
					AbstractModbusElement<?>[] ameVolt = new AbstractModbusElement<?>[SENSORS_PER_MODULE];
					AbstractModbusElement<?>[] ameTemp = new AbstractModbusElement<?>[SENSORS_PER_MODULE];
					for (int j = 0; j < SENSORS_PER_MODULE; j++) {
						{
							// Create Voltage Channel
							ChannelIdImpl channelId = new ChannelIdImpl(//
									this.getSingleCellPrefix(t, i, j) + "_VOLTAGE",
									Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT));
							this.addChannel(channelId);
							// Create Modbus-Mapping for Voltages
							UnsignedWordElement uwe = new UnsignedWordElement(towerOffset
									+ i * ADDRESS_OFFSET_FOR_CELL_VOLT_AND_TEMP + VOLTAGE_ADDRESS_OFFSET + j);
							ameVolt[j] = m(channelId, uwe);
						}
						{
							// Create Temperature Channel
							ChannelIdImpl channelId = new ChannelIdImpl(//
									this.getSingleCellPrefix(t, i, j) + "_TEMPERATURE",
									Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS));
							this.addChannel(channelId);

							// Create Modbus-Mapping for Temperatures
							// Cell Temperatures Read Registers for Tower_1 starts from 10000, for Tower_2
							// 12000, for Tower_3 14000
							// (t-1)*2000+10000) calculates Tower Offset value
							UnsignedWordElement uwe = new UnsignedWordElement(towerOffset
									+ i * ADDRESS_OFFSET_FOR_CELL_VOLT_AND_TEMP + TEMPERATURE_ADDRESS_OFFSET + j);
							ameTemp[j] = m(channelId, uwe);
						}
					}
					this.getModbusProtocol().addTasks(//
							new FC3ReadRegistersTask(
									towerOffset + ADDRESS_OFFSET_FOR_CELL_VOLT_AND_TEMP * i + VOLTAGE_ADDRESS_OFFSET,
									Priority.LOW, ameVolt), //
							new FC3ReadRegistersTask(//
									towerOffset + ADDRESS_OFFSET_FOR_CELL_VOLT_AND_TEMP * i
											+ TEMPERATURE_ADDRESS_OFFSET,
									Priority.LOW, ameTemp));
				}
			}
		} catch (OpenemsException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Gets the Number of Modules Per Tower.
	 * 
	 * @return the Number of Modules Per Tower as a {@link CompletableFuture}.
	 * @throws OpenemsException on error
	 */
	private CompletableFuture<Integer> getNumberOfModulesPerTowers() {
		final CompletableFuture<Integer> result = new CompletableFuture<Integer>();
		try {
			ModbusUtils.readELementOnce(this.getModbusProtocol(), new UnsignedWordElement(10024), true)
					.thenAccept(numberOfModulesPerTower -> {
						if (numberOfModulesPerTower == null) {
							return;
						}
						result.complete(numberOfModulesPerTower);
					});
		} catch (OpenemsException e) {
			result.completeExceptionally(e);
		}
		return result;
	}

	/**
	 * Gets the Number of Towers.
	 * 
	 * @return the Number of Towers as a {@link CompletableFuture}.
	 */
	private CompletableFuture<Integer> getNumberOfTowers() {
		final CompletableFuture<Integer> result = new CompletableFuture<Integer>();
		try {
			ModbusUtils.readELementOnce(this.getModbusProtocol(), new UnsignedWordElement(14000), true)
					.thenAccept(softwareVersionOfTower3 -> {
						if (softwareVersionOfTower3 == null) {
							return;
						}
						if (softwareVersionOfTower3 != 0) {
							// Three Towers available
							result.complete(3);
						} else {
							try {
								ModbusUtils
										.readELementOnce(this.getModbusProtocol(), new UnsignedWordElement(12000), true)
										.thenAccept(softwareVersionOfTower2 -> {
											if (softwareVersionOfTower2 == null) {
												return;
											}
											if (softwareVersionOfTower2 != 0) {
												// Two Towers available
												result.complete(2);
											} else {
												// One Tower available
												result.complete(1);
											}
										});
							} catch (OpenemsException e) {
								result.completeExceptionally(e);
							}
						}
					});
		} catch (OpenemsException e) {
			result.completeExceptionally(e);
		}
		return result;
	}

	/**
	 * Generates prefix for Channel-IDs for Cell Temperature and Voltage channels.
	 * 
	 * <p>
	 * "%03d" creates string number with leading zeros
	 * 
	 * @param num    number of the Cell
	 * @param module number of the Module
	 * @param tower  number of the Tower
	 * @return a prefix e.g. "TOWER_1_MODULE_2_CELL_003"
	 */
	private String getSingleCellPrefix(int tower, int module, int num) {
		return "TOWER_" + tower + "_MODULE_" + module + "_CELL_" + String.format("%03d", num);
	}

	/**
	 * Generates a Channel-ID for channels that are specific to a tower.
	 * 
	 * @param tower           number of the Tower
	 * @param channelIdSuffix e.g. "STATUS_ALARM"
	 * @return a channel with Channel-ID "TOWER_1_STATUS_ALARM"
	 */
	private ChannelIdImpl generateTowerChannel(int tower, String channelIdSuffix) {
		ChannelIdImpl channelId = new ChannelIdImpl("TOWER_" + tower + "_" + channelIdSuffix,
				Doc.of(OpenemsType.BOOLEAN));
		this.addChannel(channelId);
		return channelId;
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
