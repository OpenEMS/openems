package io.openems.edge.battery.fenecon.home;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

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
import io.openems.edge.battery.protection.BatteryProtection;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.ComponentManager;
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
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		})
public class FeneconHomeBatteryImpl extends AbstractOpenemsModbusComponent
		implements OpenemsComponent, Battery, EventHandler, ModbusSlave, StartStoppable, FeneconHomeBattery {

	private static final int TEMPERATURE_ADDRESS_OFFSET = 18;
	private static final int VOLTAGE_ADDRESS_OFFSET = 2;
	private static final int SENSORS_PER_MODULE = 14;
	private static final int ADDRESS_OFFSET_FOR_CELL_VOLT_AND_TEMP = 100;
	private static final int MODULE_MIN_VOLTAGE = 42; // [V]
	private static final int MODULE_MAX_VOLTAGE = 49; // [V]; 3.5 V x 14 Cells per Module
	private static final int CAPACITY_PER_MODULE = 2200; // [Wh]

	private final Logger log = LoggerFactory.getLogger(FeneconHomeBatteryImpl.class);

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected ComponentManager componentManager;

	/**
	 * Manages the {@link State}s of the StateMachine.
	 */
	private final StateMachine stateMachine = new StateMachine(State.UNDEFINED);

	private final AtomicReference<StartStop> startStopTarget = new AtomicReference<StartStop>(StartStop.UNDEFINED);

	private Config config;
	private BatteryProtection batteryProtection = null;

	public FeneconHomeBatteryImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				BatteryProtection.ChannelId.values(), //
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

		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id());

		// Initialize Battery-Protection
		this.batteryProtection = BatteryProtection.create(this) //
				.applyBatteryProtectionDefinition(new FeneconHomeBatteryProtection(), this.componentManager) //
				.build();
	}

	@Override
	public void handleEvent(Event event) {

		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.batteryProtection.apply();
			break;
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

				new FC3ReadRegistersTask(506, Priority.LOW, //
						m(Battery.ChannelId.VOLTAGE, new UnsignedWordElement(506),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // [V]
						m(Battery.ChannelId.CURRENT, new SignedWordElement(507),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // [A]
						m(Battery.ChannelId.SOC, new UnsignedWordElement(508),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // [%]
						m(Battery.ChannelId.SOH, new UnsignedWordElement(509),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // [%]
						m(Battery.ChannelId.MIN_CELL_VOLTAGE, new UnsignedWordElement(510)), // [mV]
						m(FeneconHomeBattery.ChannelId.ID_OF_CELL_VOLTAGE_MIN, new UnsignedWordElement(511)), //
						m(Battery.ChannelId.MAX_CELL_VOLTAGE, new UnsignedWordElement(512)), // [mV]
						m(FeneconHomeBattery.ChannelId.ID_OF_CELL_VOLTAGE_MAX, new UnsignedWordElement(513)), //
						m(Battery.ChannelId.MIN_CELL_TEMPERATURE, new SignedWordElement(514),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(FeneconHomeBattery.ChannelId.ID_OF_MIN_TEMPERATURE, new UnsignedWordElement(515)), //
						m(Battery.ChannelId.MAX_CELL_TEMPERATURE, new SignedWordElement(516),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(FeneconHomeBattery.ChannelId.ID_OF_MAX_TEMPERATURE, new UnsignedWordElement(517)), //
						m(BatteryProtection.ChannelId.BP_CHARGE_BMS, new UnsignedWordElement(518),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // [A]
						m(BatteryProtection.ChannelId.BP_DISCHARGE_BMS, new UnsignedWordElement(519), //
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
				new FC3ReadRegistersTask(14000, Priority.LOW, //
						m(FeneconHomeBattery.ChannelId.TOWER_3_BMS_SOFTWARE_VERSION, new UnsignedWordElement(14000))), //
				new FC3ReadRegistersTask(12000, Priority.LOW, //
						m(FeneconHomeBattery.ChannelId.TOWER_2_BMS_SOFTWARE_VERSION, new UnsignedWordElement(12000))), //
				new FC3ReadRegistersTask(10000, Priority.LOW, //
						m(FeneconHomeBattery.ChannelId.TOWER_1_BMS_SOFTWARE_VERSION, new UnsignedWordElement(10000)), //
						new DummyRegisterElement(10001, 10023), //
						m(FeneconHomeBattery.ChannelId.NUMBER_OF_MODULES_PER_TOWER, new UnsignedWordElement(10024))), //
				new FC3ReadRegistersTask(44000, Priority.LOW, //
						m(FeneconHomeBattery.ChannelId.BMS_CONTROL, new UnsignedWordElement(44000)) //
				));
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
	private static String getSingleCellPrefix(int tower, int module, int num) {
		return "TOWER_" + tower + "_MODULE_" + module + "_CELL_" + String.format("%03d", num);
	}

	/**
	 * Generates a Channel-ID for channels that are specific to a tower.
	 * 
	 * @param tower           number of the Tower
	 * @param channelIdSuffix e.g. "STATUS_ALARM"
	 * @return a channel with Channel-ID "TOWER_1_STATUS_ALARM"
	 */
	private ChannelIdImpl generateTowerChannel(int tower, String channelIdSuffix, OpenemsType openemsType) {
		ChannelIdImpl channelId = new ChannelIdImpl("TOWER_" + tower + "_" + channelIdSuffix, Doc.of(openemsType));
		this.addChannel(channelId);
		return channelId;
	}

	@Override
	public String debugLog() {
		return "Actual:" + this.getVoltage() + ";" + this.getCurrent() //
				+ "|Charge:" + this.getChargeMaxVoltage() + ";" + this.getChargeMaxCurrent() //
				+ "|Discharge:" + this.getDischargeMinVoltage() + ";" + this.getDischargeMaxCurrent(); //
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

	/**
	 * Callback for Channels to recalculate the number of towers and modules.
	 * Unfortunately the battery may report too small wrong values in the beginning,
	 * so we need to recalculate on every change.
	 */
	protected final static Consumer<Channel<Integer>> UPDATE_NUMBER_OF_TOWERS_AND_MODULES_CALLBACK = channel -> {
		channel.onChange((ignore, value) -> {
			((FeneconHomeBatteryImpl) channel.getComponent()).updateNumberOfTowersAndModules();
		});
	};

	/**
	 * Update Number of towers and modules; called by
	 * UPDATE_NUMBER_OF_TOWERS_AND_MODULES_CALLBACK.
	 */
	private synchronized void updateNumberOfTowersAndModules() {
		Channel<Integer> numberOfModulesPerTowerChannel = this
				.channel(FeneconHomeBattery.ChannelId.NUMBER_OF_MODULES_PER_TOWER);
		Value<Integer> numberOfModulesPerTowerOpt = numberOfModulesPerTowerChannel.value();
		Channel<Integer> tower2BmsSoftwareVersionChannel = this
				.channel(FeneconHomeBattery.ChannelId.TOWER_2_BMS_SOFTWARE_VERSION);
		Value<Integer> tower2BmsSoftwareVersion = tower2BmsSoftwareVersionChannel.value();
		Channel<Integer> tower3BmsSoftwareVersionChannel = this
				.channel(FeneconHomeBattery.ChannelId.TOWER_3_BMS_SOFTWARE_VERSION);
		Value<Integer> tower3BmsSoftwareVersion = tower3BmsSoftwareVersionChannel.value();

		// Were all required registers read?
		if (!numberOfModulesPerTowerOpt.isDefined() || !tower3BmsSoftwareVersion.isDefined()
				|| !tower2BmsSoftwareVersion.isDefined()) {
			return;
		}
		int numberOfModulesPerTower = numberOfModulesPerTowerOpt.get();

		// Evaluate the total number of towers by reading the software versions of
		// towers 2 and 3: they are '0' when the respective tower is not available.
		final int numberOfTowers;
		if (!Objects.equals(tower3BmsSoftwareVersion.get(), 0)) {
			numberOfTowers = 3;
		} else if (!Objects.equals(tower2BmsSoftwareVersion.get(), 0)) {
			numberOfTowers = 2;
		} else {
			numberOfTowers = 1;
		}

		// Write 'TOWER_NUMBER' Debug Channel
		Channel<?> numberOfTowersChannel = this.channel(FeneconHomeBattery.ChannelId.NUMBER_OF_TOWERS);
		numberOfTowersChannel.setNextValue(numberOfTowers);

		// Set Battery Channels
		this._setChargeMaxVoltage(numberOfModulesPerTower * MODULE_MAX_VOLTAGE);
		this._setDischargeMinVoltage(numberOfModulesPerTower * MODULE_MIN_VOLTAGE);
		this._setCapacity(numberOfTowers * numberOfModulesPerTower * CAPACITY_PER_MODULE);

		// Initialize available Tower- and Module-Channels dynamically.
		try {
			this.initializeTowerModulesChannels(numberOfTowers, numberOfModulesPerTower);
		} catch (OpenemsException e) {
			this.logError(this.log, "Unable to initialize tower modules channels: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private int lastNumberOfTowers = 0;
	private int lastNumberOfModulesPerTower = 0;

	/**
	 * Initialize channels per towers and modules.
	 * 
	 * @param numberOfTowers          the number of towers
	 * @param numberOfModulesPerTower the number of modulers per tower
	 * @throws OpenemsException on error
	 */
	private void initializeTowerModulesChannels(int numberOfTowers, int numberOfModulesPerTower)
			throws OpenemsException {
		try {
			for (int t = this.lastNumberOfTowers + 1; t <= numberOfTowers; t++) {
				/*
				 * Number Of Towers increased
				 */
				final int towerOffset = (t - 1) * 2000 + 10000;
				this.getModbusProtocol().addTasks(//
						new FC3ReadRegistersTask(towerOffset + 1, Priority.HIGH, //
								m(this.generateTowerChannel(t, "BMS_HARDWARE_VERSION", OpenemsType.INTEGER),
										new UnsignedWordElement(towerOffset + 1)), //
								m(new BitsWordElement(towerOffset + 2, this)//
										.bit(0, this.generateTowerChannel(t, "STATUS_ALARM", OpenemsType.BOOLEAN)) //
										.bit(1, this.generateTowerChannel(t, "STATUS_WARNING", OpenemsType.BOOLEAN)) //
										.bit(2, this.generateTowerChannel(t, "STATUS_FAULT", OpenemsType.BOOLEAN)) //
										.bit(3, this.generateTowerChannel(t, "STATUS_PFET", OpenemsType.BOOLEAN)) //
										.bit(4, this.generateTowerChannel(t, "STATUS_CFET", OpenemsType.BOOLEAN)) //
										.bit(5, this.generateTowerChannel(t, "STATUS_DFET", OpenemsType.BOOLEAN)) //
										.bit(6, this.generateTowerChannel(t, "STATUS_BATTERY_IDLE",
												OpenemsType.BOOLEAN)) //
										.bit(7, this.generateTowerChannel(t, "STATUS_BATTERY_CHARGING",
												OpenemsType.BOOLEAN)) //
										.bit(8, this.generateTowerChannel(t, "STATUS_BATTERY_DISCHARGING",
												OpenemsType.BOOLEAN)) //
								), //
								m(new BitsWordElement(towerOffset + 3, this)
										.bit(0, this.generateTowerChannel(t, "PRE_ALARM_CELL_OVER_VOLTAGE",
												OpenemsType.BOOLEAN)) //
										.bit(1, this.generateTowerChannel(t, "PRE_ALARM_CELL_UNDER_VOLTAGE",
												OpenemsType.BOOLEAN)) //
										.bit(2, this.generateTowerChannel(t, "PRE_ALARM_OVER_CHARGING_CURRENT",
												OpenemsType.BOOLEAN)) //
										.bit(3, this.generateTowerChannel(t, "PRE_ALARM_OVER_DISCHARGING_CURRENT",
												OpenemsType.BOOLEAN)) //
										.bit(4, this.generateTowerChannel(t, "PRE_ALARM_OVER_TEMPERATURE",
												OpenemsType.BOOLEAN)) //
										.bit(5, this.generateTowerChannel(t, "PRE_ALARM_UNDER_TEMPERATURE",
												OpenemsType.BOOLEAN)) //
										.bit(6, this.generateTowerChannel(t, "PRE_ALARM_CELL_VOLTAGE_DIFFERENCE",
												OpenemsType.BOOLEAN)) //
										.bit(7, this.generateTowerChannel(t, "PRE_ALARM_BCU_TEMP_DIFFERENCE",
												OpenemsType.BOOLEAN)) //
										.bit(8, this.generateTowerChannel(t, "PRE_ALARM_UNDER_SOC",
												OpenemsType.BOOLEAN)) //
										.bit(9, this.generateTowerChannel(t, "PRE_ALARM_UNDER_SOH",
												OpenemsType.BOOLEAN)) //
										.bit(10, this.generateTowerChannel(t, "PRE_ALARM_OVER_CHARGING_POWER",
												OpenemsType.BOOLEAN)) //
										.bit(11, this.generateTowerChannel(t, "PRE_ALARM_OVER_DISCHARGING_POWER",
												OpenemsType.BOOLEAN))), //
								m(new BitsWordElement(towerOffset + 4, this)
										.bit(0, this.generateTowerChannel(t, "LEVEL_1_CELL_OVER_VOLTAGE",
												OpenemsType.BOOLEAN)) //
										.bit(1, this.generateTowerChannel(t, "LEVEL_1_CELL_UNDER_VOLTAGE",
												OpenemsType.BOOLEAN)) //
										.bit(2, this.generateTowerChannel(t, "LEVEL_1_OVER_CHARGING_CURRENT",
												OpenemsType.BOOLEAN)) //
										.bit(3, this.generateTowerChannel(t, "LEVEL_1_OVER_DISCHARGING_CURRENT",
												OpenemsType.BOOLEAN)) //
										.bit(4, this.generateTowerChannel(t, "LEVEL_1_OVER_TEMPERATURE",
												OpenemsType.BOOLEAN)) //
										.bit(5, this.generateTowerChannel(t, "LEVEL_1_UNDER_TEMPERATURE",
												OpenemsType.BOOLEAN)) //
										.bit(6, this.generateTowerChannel(t, "LEVEL_1_CELL_VOLTAGE_DIFFERENCE",
												OpenemsType.BOOLEAN)) //
										.bit(7, this.generateTowerChannel(t, "LEVEL_1_BCU_TEMP_DIFFERENCE",
												OpenemsType.BOOLEAN)) //
										.bit(8, this.generateTowerChannel(t, "LEVEL_1_UNDER_SOC", OpenemsType.BOOLEAN)) //
										.bit(9, this.generateTowerChannel(t, "LEVEL_1_UNDER_SOH", OpenemsType.BOOLEAN)) //
										.bit(10, this.generateTowerChannel(t, "LEVEL_1_OVER_CHARGING_POWER",
												OpenemsType.BOOLEAN)) //
										.bit(11, this.generateTowerChannel(t, "LEVEL_1_OVER_DISCHARGING_POWER",
												OpenemsType.BOOLEAN))),
								m(new BitsWordElement(towerOffset + 5, this)
										.bit(0, this.generateTowerChannel(t, "LEVEL_2_CELL_OVER_VOLTAGE",
												OpenemsType.BOOLEAN)) //
										.bit(1, this.generateTowerChannel(t, "LEVEL_2_CELL_UNDER_VOLTAGE",
												OpenemsType.BOOLEAN)) //
										.bit(2, this.generateTowerChannel(t, "LEVEL_2_OVER_CHARGING_CURRENT",
												OpenemsType.BOOLEAN)) //
										.bit(3, this.generateTowerChannel(t, "LEVEL_2_OVER_DISCHARGING_CURRENT",
												OpenemsType.BOOLEAN)) //
										.bit(4, this.generateTowerChannel(t, "LEVEL_2_OVER_TEMPERATURE",
												OpenemsType.BOOLEAN)) //
										.bit(5, this.generateTowerChannel(t, "LEVEL_2_UNDER_TEMPERATURE",
												OpenemsType.BOOLEAN)) //
										.bit(6, this.generateTowerChannel(t, "LEVEL_2_CELL_VOLTAGE_DIFFERENCE",
												OpenemsType.BOOLEAN)) //
										.bit(7, this.generateTowerChannel(t, "LEVEL_2_BCU_TEMP_DIFFERENCE",
												OpenemsType.BOOLEAN)) //
										.bit(8, this.generateTowerChannel(t, "LEVEL_2_TEMPERATURE_DIFFERENCE",
												OpenemsType.BOOLEAN)) //
										.bit(9, this.generateTowerChannel(t, "LEVEL_2_INTERNAL_COMMUNICATION",
												OpenemsType.BOOLEAN)) //
										.bit(10, this.generateTowerChannel(t, "LEVEL_2_EXTERNAL_COMMUNICATION",
												OpenemsType.BOOLEAN)) //
										.bit(11, this.generateTowerChannel(t, "LEVEL_2_PRECHARGE_FAIL",
												OpenemsType.BOOLEAN)) //
										.bit(12, this.generateTowerChannel(t, "LEVEL_2_PARALLEL_FAIL",
												OpenemsType.BOOLEAN)) //
										.bit(13, this.generateTowerChannel(t, "LEVEL_2_SYSTEM_FAIL",
												OpenemsType.BOOLEAN)) //
										.bit(14, this.generateTowerChannel(t, "LEVEL_2_HARDWARE_FAIL",
												OpenemsType.BOOLEAN))), //
								m(new BitsWordElement(towerOffset + 6, this)
										.bit(0, this.generateTowerChannel(t, "HW_AFE_COMMUNICAITON_FAULT",
												OpenemsType.BOOLEAN)) //
										.bit(1, this.generateTowerChannel(t, "HW_ACTOR_DRIVER_FAULT",
												OpenemsType.BOOLEAN)) //
										.bit(2, this.generateTowerChannel(t, "HW_EEPROM_COMMUNICATION_FAULT",
												OpenemsType.BOOLEAN)) //
										.bit(3, this.generateTowerChannel(t, "HW_VOLTAGE_DETECT_FAULT",
												OpenemsType.BOOLEAN)) //
										.bit(4, this.generateTowerChannel(t, "HW_TEMPERATURE_DETECT_FAULT",
												OpenemsType.BOOLEAN)) //
										.bit(5, this.generateTowerChannel(t, "HW_CURRENT_DETECT_FAULT",
												OpenemsType.BOOLEAN)) //
										.bit(6, this.generateTowerChannel(t, "HW_ACTOR_NOT_CLOSE", OpenemsType.BOOLEAN)) //
										.bit(7, this.generateTowerChannel(t, "HW_ACTOR_NOT_OPEN", OpenemsType.BOOLEAN)) //
										.bit(8, this.generateTowerChannel(t, "HW_FUSE_BROKEN", OpenemsType.BOOLEAN))), //
								m(new BitsWordElement(towerOffset + 7, this)
										.bit(0, this.generateTowerChannel(t, "SYSTEM_AFE_OVER_TEMPERATURE",
												OpenemsType.BOOLEAN)) //
										.bit(1, this.generateTowerChannel(t, "SYSTEM_AFE_UNDER_TEMPERATURE",
												OpenemsType.BOOLEAN)) //
										.bit(2, this.generateTowerChannel(t, "SYSTEM_AFE_OVER_VOLTAGE",
												OpenemsType.BOOLEAN)) //
										.bit(3, this.generateTowerChannel(t, "SYSTEM_AFE_UNDER_VOLTAGE",
												OpenemsType.BOOLEAN)) //
										.bit(4, this.generateTowerChannel(t,
												"SYSTEM_HIGH_TEMPERATURE_PERMANENT_FAILURE", OpenemsType.BOOLEAN)) //
										.bit(5, this.generateTowerChannel(t, "SYSTEM_LOW_TEMPERATURE_PERMANENT_FAILURE",
												OpenemsType.BOOLEAN)) //
										.bit(6, this.generateTowerChannel(t,
												"SYSTEM_HIGH_CELL_VOLTAGE_PERMANENT_FAILURE", OpenemsType.BOOLEAN)) //
										.bit(7, this.generateTowerChannel(t,
												"SYSTEM_LOW_CELL_VOLTAGE_PERMANENT_FAILURE", OpenemsType.BOOLEAN)) //
										.bit(8, this.generateTowerChannel(t, "SYSTEM_SHORT_CIRCUIT",
												OpenemsType.BOOLEAN))), //
								m(this.generateTowerChannel(t, "_SOC", OpenemsType.INTEGER),
										new UnsignedWordElement(towerOffset + 8), // [%]
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
								m(this.generateTowerChannel(t, "_SOH", OpenemsType.INTEGER),
										new UnsignedWordElement(towerOffset + 9), // [%]
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
								m(this.generateTowerChannel(t, "_VOLTAGE", OpenemsType.INTEGER),
										new UnsignedWordElement(towerOffset + 10), // [V]
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
								m(this.generateTowerChannel(t, "_CURRENT", OpenemsType.INTEGER),
										new SignedWordElement(towerOffset + 11), // [A]
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
								m(this.generateTowerChannel(t, "_MIN_CELL_VOLTAGE", OpenemsType.INTEGER),
										new UnsignedWordElement(towerOffset + 12)), // [mV]
								m(this.generateTowerChannel(t, "_MAX_CELL_VOLTAGE", OpenemsType.INTEGER),
										new UnsignedWordElement(towerOffset + 13)), // [mV]
								m(this.generateTowerChannel(t, "_AVARAGE_CELL_VOLTAGE", OpenemsType.INTEGER),
										new UnsignedWordElement(towerOffset + 14)), //
								m(this.generateTowerChannel(t, "_MAX_CHARGE_CURRENT", OpenemsType.INTEGER),
										new UnsignedWordElement(towerOffset + 15)), //
								m(this.generateTowerChannel(t, "_MIN_CHARGE_CURRENT", OpenemsType.INTEGER),
										new UnsignedWordElement(towerOffset + 16)), //
								m(this.generateTowerChannel(t, "_BMS_SERIAL_NUMBER", OpenemsType.INTEGER),
										new UnsignedWordElement(towerOffset + 17)), //
								m(this.generateTowerChannel(t, "_NO_OF_CYCLES", OpenemsType.INTEGER),
										new UnsignedWordElement(towerOffset + 18)), //
								m(this.generateTowerChannel(t, "_DESIGN_CAPACITY", OpenemsType.INTEGER),
										new UnsignedWordElement(towerOffset + 19),
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // [Ah]
								m(this.generateTowerChannel(t, "_USABLE_CAPACITY", OpenemsType.INTEGER),
										new UnsignedWordElement(towerOffset + 20), //
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // [Ah]
								m(this.generateTowerChannel(t, "_REMAINING_CAPACITY", OpenemsType.INTEGER),
										new UnsignedWordElement(towerOffset + 21), //
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // [Ah]
								m(this.generateTowerChannel(t, "_MAX_CELL_VOLTAGE_LIMIT", OpenemsType.INTEGER),
										new UnsignedWordElement(towerOffset + 22)), //
								m(this.generateTowerChannel(t, "_MIN_CELL_VOLTAGE_LIMIT", OpenemsType.INTEGER),
										new UnsignedWordElement(towerOffset + 23))));
			}

			for (int t = 1; t <= numberOfTowers; t++) {
				final int towerOffset = (t - 1) * 2000 + 10000;

				for (int i = this.lastNumberOfModulesPerTower; i < numberOfModulesPerTower + 1; i++) {
					/*
					 * Number Of Modules per Tower increased.
					 * 
					 * Dynamically generate Channels and Modbus mappings for Cell-Temperatures and
					 * for Cell-Voltages.Channel-IDs are like "TOWER_1_OFFSET_2_TEMPERATURE_003".
					 * Channel-IDs are like "TOWER_1_OFFSET_2_VOLTAGE_003".
					 */
					AbstractModbusElement<?>[] ameVolt = new AbstractModbusElement<?>[SENSORS_PER_MODULE];
					AbstractModbusElement<?>[] ameTemp = new AbstractModbusElement<?>[SENSORS_PER_MODULE];
					for (int j = 0; j < SENSORS_PER_MODULE; j++) {
						{
							// Create Voltage Channel
							ChannelIdImpl channelId = new ChannelIdImpl(//
									getSingleCellPrefix(t, i, j) + "_VOLTAGE",
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
									getSingleCellPrefix(t, i, j) + "_TEMPERATURE",
									Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS));
							this.addChannel(channelId);

							// Create Modbus-Mapping for Temperatures
							// Cell Temperatures Read Registers for Tower_1 starts from 10000, for Tower_2
							// 12000, for Tower_3 14000
							// (t-1)*2000+10000) calculates Tower Offset value
							SignedWordElement uwe = new SignedWordElement(towerOffset
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

		} finally {
			// Always store the last numbers
			this.lastNumberOfTowers = numberOfTowers;
			this.lastNumberOfModulesPerTower = numberOfModulesPerTower;
		}
	}
}
