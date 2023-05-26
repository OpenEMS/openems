package io.openems.edge.battery.fenecon.home;

import java.util.Objects;
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
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.fenecon.home.statemachine.Context;
import io.openems.edge.battery.fenecon.home.statemachine.StateMachine;
import io.openems.edge.battery.fenecon.home.statemachine.StateMachine.State;
import io.openems.edge.battery.protection.BatteryProtection;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.internal.OpenemsTypeDoc;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Battery.Fenecon.Home", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class FeneconHomeBatteryImpl extends AbstractOpenemsModbusComponent implements ModbusComponent, OpenemsComponent,
		Battery, EventHandler, ModbusSlave, StartStoppable, FeneconHomeBattery {

	private static final int SENSORS_PER_MODULE = 14;
	private static final int MODULE_MIN_VOLTAGE = 42; // [V]
	private static final int MODULE_MAX_VOLTAGE = 49; // [V]; 3.5 V x 14 Cells per Module
	private static final int CAPACITY_PER_MODULE = 2200; // [Wh]
	private static final String SERIAL_NUMBER_PREFIX_BMS = "519100001009";
	private static final String SERIAL_NUMBER_PREFIX_MODULE = "519110001210";

	private final Logger log = LoggerFactory.getLogger(FeneconHomeBatteryImpl.class);

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected ComponentManager componentManager;

	/**
	 * Manages the {@link State}s of the StateMachine.
	 */
	private final StateMachine stateMachine = new StateMachine(State.UNDEFINED);

	private final AtomicReference<StartStop> startStopTarget = new AtomicReference<>(StartStop.UNDEFINED);

	private Config config;
	private BatteryProtection batteryProtection = null;

	public FeneconHomeBatteryImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				BatteryProtection.ChannelId.values(), //
				FeneconHomeBattery.ChannelId.values() //
		);
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;

		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}

		// Initialize Battery-Protection
		this.batteryProtection = BatteryProtection.create(this) //
				.applyBatteryProtectionDefinition(new FeneconHomeBatteryProtection(), this.componentManager) //
				.build();
	}

	@Override
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
		BooleanWriteChannel batteryStartUpRelayChannel;
		try {
			batteryStartUpRelayChannel = this.componentManager
					.getChannel(ChannelAddress.fromString(this.config.batteryStartUpRelay()));
		} catch (IllegalArgumentException | OpenemsNamedException e1) {
			batteryStartUpRelayChannel = null;
		}
		var context = new Context(this, batteryStartUpRelayChannel);

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
						m(FeneconHomeBattery.ChannelId.TOWER_2_BMS_SOFTWARE_VERSION, new UnsignedWordElement(14000))), //
				new FC3ReadRegistersTask(12000, Priority.LOW, //
						m(FeneconHomeBattery.ChannelId.TOWER_1_BMS_SOFTWARE_VERSION, new UnsignedWordElement(12000))), //
				new FC3ReadRegistersTask(10000, Priority.LOW, //
						m(FeneconHomeBattery.ChannelId.TOWER_0_BMS_SOFTWARE_VERSION, new UnsignedWordElement(10000)), //
						new DummyRegisterElement(10001, 10023), //
						m(FeneconHomeBattery.ChannelId.NUMBER_OF_MODULES_PER_TOWER, new UnsignedWordElement(10024))), //
				new FC3ReadRegistersTask(44000, Priority.HIGH, //
						m(new BitsWordElement(44000, this) //
								.bit(0, FeneconHomeBattery.ChannelId.BMS_CONTROL)) //
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
	 * @param tower               number of the Tower
	 * @param channelIdSuffix     e.g. "STATUS_ALARM"
	 * @param openemsType         specified type e.g. "INTEGER"
	 * @param additionalDocConfig the additional doc configuration
	 * @return a channel with Channel-ID "TOWER_1_STATUS_ALARM"
	 */
	private ChannelIdImpl generateTowerChannel(int tower, String channelIdSuffix, OpenemsType openemsType,
			Consumer<OpenemsTypeDoc<?>> additionalDocConfig) {
		final var doc = Doc.of(openemsType);
		if (additionalDocConfig != null) {
			additionalDocConfig.accept(doc);
		}
		var channelId = new ChannelIdImpl("TOWER_" + tower + "_" + channelIdSuffix, doc);
		this.addChannel(channelId);
		return channelId;
	}

	private ChannelIdImpl generateTowerChannel(int tower, String channelIdSuffix, OpenemsType openemsType) {
		return this.generateTowerChannel(tower, channelIdSuffix, openemsType, null);
	}

	/**
	 * Generates a Channel-ID for channels that are specific to a tower.
	 *
	 * @param tower           number of the Tower
	 * @param channelIdSuffix e.g. "STATUS_ALARM"
	 * @param level           specified level e.g. "INFO"
	 * @return a channel with Channel-ID "TOWER_1_STATUS_ALARM"
	 */
	private ChannelIdImpl generateTowerChannel(int tower, String channelIdSuffix, Level level) {
		var channelId = new ChannelIdImpl("TOWER_" + tower + "_" + channelIdSuffix, Doc.of(level));
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
	 * Update Number of towers and modules; called on onChange event.
	 * 
	 * <p>
	 * Recalculate the number of towers and modules. Unfortunately the battery may
	 * report too small wrong values in the beginning, so we need to recalculate on
	 * every change.
	 */
	protected synchronized void updateNumberOfTowersAndModules() {
		Channel<Integer> numberOfModulesPerTowerChannel = this
				.channel(FeneconHomeBattery.ChannelId.NUMBER_OF_MODULES_PER_TOWER);
		var numberOfModulesPerTowerOpt = numberOfModulesPerTowerChannel.value();
		Channel<Integer> tower2BmsSoftwareVersionChannel = this
				.channel(FeneconHomeBattery.ChannelId.TOWER_1_BMS_SOFTWARE_VERSION);
		var tower2BmsSoftwareVersion = tower2BmsSoftwareVersionChannel.value();
		Channel<Integer> tower3BmsSoftwareVersionChannel = this
				.channel(FeneconHomeBattery.ChannelId.TOWER_2_BMS_SOFTWARE_VERSION);
		var tower3BmsSoftwareVersion = tower3BmsSoftwareVersionChannel.value();

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
	private synchronized void initializeTowerModulesChannels(int numberOfTowers, int numberOfModulesPerTower)
			throws OpenemsException {
		try {
			for (var tower = this.lastNumberOfTowers; tower < numberOfTowers; tower++) {
				/*
				 * Number Of Towers increased
				 */
				final var towerOffset = tower * 2000 + 10000;
				this.getModbusProtocol().addTasks(//
						new FC3ReadRegistersTask(towerOffset + 1, Priority.HIGH, //
								m(this.generateTowerChannel(tower, "BMS_HARDWARE_VERSION", OpenemsType.INTEGER),
										new UnsignedWordElement(towerOffset + 1)), //
								m(new BitsWordElement(towerOffset + 2, this)//
										.bit(0, this.generateTowerChannel(tower, "STATUS_ALARM", OpenemsType.BOOLEAN)) //
										.bit(1, this.generateTowerChannel(tower, "STATUS_WARNING", OpenemsType.BOOLEAN)) //
										.bit(2, this.generateTowerChannel(tower, "STATUS_FAULT", OpenemsType.BOOLEAN)) //
										.bit(3, this.generateTowerChannel(tower, "STATUS_PFET", OpenemsType.BOOLEAN)) //
										// CFET (1: Charge FET ON, 0: OFF)
										.bit(4, this.generateTowerChannel(tower, "STATUS_CFET", OpenemsType.BOOLEAN)) //
										// DFET (1: Discharge FET ON, 0: OFF)
										.bit(5, this.generateTowerChannel(tower, "STATUS_DFET", OpenemsType.BOOLEAN)) //
										// BATTERY_IDLE (1: Idle)
										.bit(6, this.generateTowerChannel(tower, "STATUS_BATTERY_IDLE",
												OpenemsType.BOOLEAN)) //
										// BATTERY_CHARGING (1: charging)
										.bit(7, this.generateTowerChannel(tower, "STATUS_BATTERY_CHARGING",
												OpenemsType.BOOLEAN)) //
										// BATTERY_DISCHARGING (1: discharging)
										.bit(8, this.generateTowerChannel(tower, "STATUS_BATTERY_DISCHARGING",
												OpenemsType.BOOLEAN)) //
								), //
								m(new BitsWordElement(towerOffset + 3, this)
										.bit(0, this.generateTowerChannel(tower, "PRE_ALARM_CELL_OVER_VOLTAGE",
												OpenemsType.BOOLEAN)) //
										.bit(1, this.generateTowerChannel(tower, "PRE_ALARM_CELL_UNDER_VOLTAGE",
												OpenemsType.BOOLEAN)) //
										.bit(2, this.generateTowerChannel(tower, "PRE_ALARM_OVER_CHARGING_CURRENT",
												OpenemsType.BOOLEAN)) //
										.bit(3, this.generateTowerChannel(tower, "PRE_ALARM_OVER_DISCHARGING_CURRENT",
												OpenemsType.BOOLEAN)) //
										.bit(4, this.generateTowerChannel(tower, "PRE_ALARM_OVER_TEMPERATURE",
												OpenemsType.BOOLEAN)) //
										.bit(5, this.generateTowerChannel(tower, "PRE_ALARM_UNDER_TEMPERATURE",
												OpenemsType.BOOLEAN)) //
										.bit(6, this.generateTowerChannel(tower, "PRE_ALARM_CELL_VOLTAGE_DIFFERENCE",
												OpenemsType.BOOLEAN)) //
										.bit(7, this.generateTowerChannel(tower, "PRE_ALARM_BCU_TEMP_DIFFERENCE",
												OpenemsType.BOOLEAN)) //
										.bit(8, this.generateTowerChannel(tower, "PRE_ALARM_UNDER_SOC",
												OpenemsType.BOOLEAN)) //
										.bit(9, this.generateTowerChannel(tower, "PRE_ALARM_UNDER_SOH",
												OpenemsType.BOOLEAN)) //
										.bit(10, this.generateTowerChannel(tower, "PRE_ALARM_OVER_CHARGING_POWER",
												OpenemsType.BOOLEAN)) //
										.bit(11, this.generateTowerChannel(tower, "PRE_ALARM_OVER_DISCHARGING_POWER",
												OpenemsType.BOOLEAN))
										.bit(12, this.generateTowerChannel(tower, "PRE_ALARM_BAT_OVER_VOLTAGE",
												OpenemsType.BOOLEAN))
										.bit(13, this.generateTowerChannel(tower, "PRE_ALARM_BAT_UNDER_VOLTAGE",
												OpenemsType.BOOLEAN))), //
								m(new BitsWordElement(towerOffset + 4, this)
										.bit(0, this.generateTowerChannel(tower, "LEVEL_1_CELL_OVER_VOLTAGE",
												OpenemsType.BOOLEAN)) //
										.bit(1, this.generateTowerChannel(tower, "LEVEL_1_CELL_UNDER_VOLTAGE",
												OpenemsType.BOOLEAN)) //
										.bit(2, this.generateTowerChannel(tower, "LEVEL_1_OVER_CHARGING_CURRENT",
												OpenemsType.BOOLEAN)) //
										.bit(3, this.generateTowerChannel(tower, "LEVEL_1_OVER_DISCHARGING_CURRENT",
												OpenemsType.BOOLEAN)) //
										.bit(4, this.generateTowerChannel(tower, "LEVEL_1_OVER_TEMPERATURE",
												OpenemsType.BOOLEAN)) //
										.bit(5, this.generateTowerChannel(tower, "LEVEL_1_UNDER_TEMPERATURE",
												OpenemsType.BOOLEAN)) //
										.bit(6, this.generateTowerChannel(tower, "LEVEL_1_CELL_VOLTAGE_DIFFERENCE",
												OpenemsType.BOOLEAN)) //
										.bit(7, this.generateTowerChannel(tower, "LEVEL_1_BCU_TEMP_DIFFERENCE",
												OpenemsType.BOOLEAN)) //
										.bit(8, this.generateTowerChannel(tower, "LEVEL_1_UNDER_SOC",
												OpenemsType.BOOLEAN)) //
										.bit(9, this.generateTowerChannel(tower, "LEVEL_1_UNDER_SOH",
												OpenemsType.BOOLEAN)) //
										.bit(10, this.generateTowerChannel(tower, "LEVEL_1_OVER_CHARGING_POWER",
												OpenemsType.BOOLEAN)) //
										.bit(11, this.generateTowerChannel(tower, "LEVEL_1_OVER_DISCHARGING_POWER",
												OpenemsType.BOOLEAN))
										.bit(12, this.generateTowerChannel(tower, "LEVEL_1_BAT_OVER_VOLTAGE",
												OpenemsType.BOOLEAN))
										.bit(13, this.generateTowerChannel(tower, "LEVEL_1_BAT_UNDER_VOLTAGE",
												OpenemsType.BOOLEAN))),
								m(new BitsWordElement(towerOffset + 5, this)
										.bit(0, this.generateTowerChannel(tower, "LEVEL_2_CELL_OVER_VOLTAGE",
												Level.WARNING)) //
										.bit(1, this.generateTowerChannel(tower, "LEVEL_2_CELL_UNDER_VOLTAGE",
												Level.WARNING)) //
										.bit(2, this.generateTowerChannel(tower, "LEVEL_2_OVER_CHARGING_CURRENT",
												Level.WARNING)) //
										.bit(3, this.generateTowerChannel(tower, "LEVEL_2_OVER_DISCHARGING_CURRENT",
												Level.WARNING)) //
										.bit(4, this.generateTowerChannel(tower, "LEVEL_2_OVER_TEMPERATURE",
												Level.WARNING)) //
										.bit(5, this.generateTowerChannel(tower, "LEVEL_2_UNDER_TEMPERATURE",
												Level.WARNING)) //
										.bit(6, this.generateTowerChannel(tower, "LEVEL_2_CELL_VOLTAGE_DIFFERENCE",
												Level.WARNING)) //
										.bit(7, this.generateTowerChannel(tower, "LEVEL_2_BCU_TEMP_DIFFERENCE",
												Level.WARNING)) //
										.bit(8, this.generateTowerChannel(tower, "LEVEL_2_BAT_OVER_VOLTAGE",
												Level.WARNING)) //
										.bit(9, this.generateTowerChannel(tower, "LEVEL_2_INTERNAL_COMMUNICATION",
												Level.WARNING)) //
										.bit(10, this.generateTowerChannel(tower, "LEVEL_2_EXTERNAL_COMMUNICATION",
												Level.WARNING)) //
										.bit(11, this.generateTowerChannel(tower, "LEVEL_2_PRECHARGE_FAIL",
												Level.WARNING)) //
										.bit(12, this.generateTowerChannel(tower, "LEVEL_2_PARALLEL_FAIL",
												Level.WARNING)) //
										.bit(13, this.generateTowerChannel(tower, "LEVEL_2_SYSTEM_FAIL", Level.WARNING)) //
										.bit(14, this.generateTowerChannel(tower, "LEVEL_2_HARDWARE_FAIL",
												Level.WARNING)) //
										.bit(14, this.generateTowerChannel(tower, "LEVEL_2_BAT_UNDER_VOLTAGE",
												Level.WARNING))), //
								m(new BitsWordElement(towerOffset + 6, this)
										.bit(0, this.generateTowerChannel(tower, "HW_AFE_COMMUNICAITON_FAULT",
												Level.WARNING)) //
										.bit(1, this.generateTowerChannel(tower, "HW_ACTOR_DRIVER_FAULT",
												Level.WARNING)) //
										.bit(2, this.generateTowerChannel(tower, "HW_EEPROM_COMMUNICATION_FAULT",
												Level.WARNING)) //
										.bit(3, this.generateTowerChannel(tower, "HW_VOLTAGE_DETECT_FAULT",
												Level.WARNING)) //
										.bit(4, this.generateTowerChannel(tower, "HW_TEMPERATURE_DETECT_FAULT",
												Level.WARNING)) //
										.bit(5, this.generateTowerChannel(tower, "HW_CURRENT_DETECT_FAULT",
												Level.WARNING)) //
										.bit(6, this.generateTowerChannel(tower, "HW_ACTOR_NOT_CLOSE", Level.WARNING)) //
										.bit(7, this.generateTowerChannel(tower, "HW_ACTOR_NOT_OPEN", Level.WARNING)) //
										.bit(8, this.generateTowerChannel(tower, "HW_FUSE_BROKEN", Level.WARNING))), //
								m(new BitsWordElement(towerOffset + 7, this)
										.bit(0, this.generateTowerChannel(tower, "SYSTEM_AFE_OVER_TEMPERATURE",
												Level.WARNING)) //
										.bit(1, this.generateTowerChannel(tower, "SYSTEM_AFE_UNDER_TEMPERATURE",
												Level.WARNING)) //
										.bit(2, this.generateTowerChannel(tower, "SYSTEM_AFE_OVER_VOLTAGE",
												Level.WARNING)) //
										.bit(3, this.generateTowerChannel(tower, "SYSTEM_AFE_UNDER_VOLTAGE",
												Level.WARNING)) //
										.bit(4, this.generateTowerChannel(tower,
												"SYSTEM_HIGH_TEMPERATURE_PERMANENT_FAILURE", Level.WARNING)) //
										.bit(5, this.generateTowerChannel(tower,
												"SYSTEM_LOW_TEMPERATURE_PERMANENT_FAILURE", Level.WARNING)) //
										.bit(6, this.generateTowerChannel(tower,
												"SYSTEM_HIGH_CELL_VOLTAGE_PERMANENT_FAILURE", Level.WARNING)) //
										.bit(7, this.generateTowerChannel(tower,
												"SYSTEM_LOW_CELL_VOLTAGE_PERMANENT_FAILURE", Level.WARNING)) //
										.bit(8, this.generateTowerChannel(tower, "SYSTEM_SHORT_CIRCUIT",
												Level.WARNING))), //
								m(this.generateTowerChannel(tower, "_SOC", OpenemsType.INTEGER),
										new UnsignedWordElement(towerOffset + 8), // [%]
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
								m(this.generateTowerChannel(tower, "_SOH", OpenemsType.INTEGER),
										new UnsignedWordElement(towerOffset + 9), // [%]
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
								m(this.generateTowerChannel(tower, "_VOLTAGE", OpenemsType.INTEGER),
										new UnsignedWordElement(towerOffset + 10), // [V]
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
								m(this.generateTowerChannel(tower, "_CURRENT", OpenemsType.INTEGER),
										new SignedWordElement(towerOffset + 11), // [A]
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
								m(this.generateTowerChannel(tower, "_MIN_CELL_VOLTAGE", OpenemsType.INTEGER),
										new UnsignedWordElement(towerOffset + 12)), // [mV]
								m(this.generateTowerChannel(tower, "_MAX_CELL_VOLTAGE", OpenemsType.INTEGER),
										new UnsignedWordElement(towerOffset + 13)), // [mV]
								m(this.generateTowerChannel(tower, "_AVARAGE_CELL_VOLTAGE", OpenemsType.INTEGER),
										new UnsignedWordElement(towerOffset + 14)), //
								m(this.generateTowerChannel(tower, "_MAX_CHARGE_CURRENT", OpenemsType.INTEGER),
										new UnsignedWordElement(towerOffset + 15)), //
								m(this.generateTowerChannel(tower, "_MIN_CHARGE_CURRENT", OpenemsType.INTEGER),
										new UnsignedWordElement(towerOffset + 16)), //
								new DummyRegisterElement(towerOffset + 17), //
								m(this.generateTowerChannel(tower, "_NO_OF_CYCLES", OpenemsType.INTEGER),
										new UnsignedWordElement(towerOffset + 18)), //
								m(this.generateTowerChannel(tower, "_DESIGN_CAPACITY", OpenemsType.INTEGER),
										new UnsignedWordElement(towerOffset + 19),
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // [Ah]
								m(this.generateTowerChannel(tower, "_USABLE_CAPACITY", OpenemsType.INTEGER),
										new UnsignedWordElement(towerOffset + 20), //
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // [Ah]
								m(this.generateTowerChannel(tower, "_REMAINING_CAPACITY", OpenemsType.INTEGER),
										new UnsignedWordElement(towerOffset + 21), //
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // [Ah]
								m(this.generateTowerChannel(tower, "_MAX_CELL_VOLTAGE_LIMIT", OpenemsType.INTEGER),
										new UnsignedWordElement(towerOffset + 22)), //
								m(this.generateTowerChannel(tower, "_MIN_CELL_VOLTAGE_LIMIT", OpenemsType.INTEGER),
										new UnsignedWordElement(towerOffset + 23)),
								m(this.generateTowerChannel(tower, "BMU_NUMBER", OpenemsType.INTEGER),
										new UnsignedWordElement(towerOffset + 24)),
								new DummyRegisterElement(towerOffset + 25), //
								new DummyRegisterElement(towerOffset + 26), //
								new DummyRegisterElement(towerOffset + 27), //
								m(new BitsWordElement(towerOffset + 28, this) //
										.bit(0, this.generateTowerChannel(tower,
												"BCU_SYSTEM_FAULT_DETAIL_EXPAND_ASSIGN_FAIL", Level.INFO))),
								new DummyRegisterElement(towerOffset + 29), //
								new DummyRegisterElement(towerOffset + 30), //
								new DummyRegisterElement(towerOffset + 31), //
								new DummyRegisterElement(towerOffset + 32), //
								new DummyRegisterElement(towerOffset + 33), //
								m(this.generateTowerChannel(tower, "PACK_VOLTAGE", OpenemsType.INTEGER), //
										new UnsignedWordElement(towerOffset + 34)),
								m(this.generateTowerChannel(tower, "MAX_TEMPERATURE", OpenemsType.INTEGER), //
										new SignedWordElement(towerOffset + 35)),
								m(this.generateTowerChannel(tower, "MIN_TEMPERATURE", OpenemsType.INTEGER), //
										new SignedWordElement(towerOffset + 36)),
								new DummyRegisterElement(towerOffset + 37), //
								new DummyRegisterElement(towerOffset + 38), //
								new DummyRegisterElement(towerOffset + 39), //
								new DummyRegisterElement(towerOffset + 40), //
								new DummyRegisterElement(towerOffset + 41), //
								new DummyRegisterElement(towerOffset + 42), //
								m(this.generateTowerChannel(tower, "TEMPERATURE_PRE_MOS", OpenemsType.INTEGER), //
										new SignedWordElement(towerOffset + 43)),
								new DummyRegisterElement(towerOffset + 44), //
								new DummyRegisterElement(towerOffset + 45), //
								new DummyRegisterElement(towerOffset + 46), //
								m(this.generateTowerChannel(tower, "ACC_CHARGE_ENERGY", OpenemsType.INTEGER),
										new UnsignedDoublewordElement(towerOffset + 47)),
								m(this.generateTowerChannel(tower, "ACC_DISCHARGE_ENERGY", OpenemsType.INTEGER),
										new UnsignedDoublewordElement(towerOffset + 49)),
								m(this.generateTowerChannel(tower, "BMS_SERIAL_NUMBER", OpenemsType.STRING,
										doc -> doc.persistencePriority(PersistencePriority.HIGH)),
										new UnsignedDoublewordElement(towerOffset + 51),
										new ElementToChannelConverter(value -> {
											Integer intValue = TypeUtils.getAsType(OpenemsType.INTEGER, value);
											return buildSerialNumber(SERIAL_NUMBER_PREFIX_BMS, intValue);
										}))));
			}

			var towerToUse = 0;
			var moduleToUse = this.lastNumberOfModulesPerTower;
			if (this.lastNumberOfTowers < numberOfTowers) {
				towerToUse = this.lastNumberOfTowers;
				moduleToUse = 0;
			}

			for (var tower = towerToUse; tower < numberOfTowers; tower++) {
				final var towerOffset = tower * 2000 + 10000;
				final var moduleOffset = towerOffset + 100;

				for (var module = moduleToUse; module < numberOfModulesPerTower; module++) {
					/*
					 * Number Of Modules per Tower increased.
					 *
					 * Dynamically generate Channels and Modbus mappings for Cell-Temperatures and
					 * for Cell-Voltages.Channel-IDs are like "TOWER_0_OFFSET_2_TEMPERATURE_003".
					 * Channel-IDs are like "TOWER_0_OFFSET_2_VOLTAGE_003".
					 */
					var ameVolt = new AbstractModbusElement<?>[SENSORS_PER_MODULE];
					var ameTemp = new AbstractModbusElement<?>[SENSORS_PER_MODULE];
					for (var j = 0; j < SENSORS_PER_MODULE; j++) {
						{
							// Create Voltage Channel
							var channelId = new ChannelIdImpl(//
									getSingleCellPrefix(tower, module, j) + "_VOLTAGE",
									Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT));
							this.addChannel(channelId);

							// Create Modbus-Mapping for Voltages
							var uwe = new UnsignedWordElement(moduleOffset + module * 100 + 2 + j);
							ameVolt[j] = m(channelId, uwe);
						}
						{
							// TODO only 8 temperatures

							// Create Temperature Channel
							var channelId = new ChannelIdImpl(//
									getSingleCellPrefix(tower, module, j) + "_TEMPERATURE",
									Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS));
							this.addChannel(channelId);

							// Create Modbus-Mapping for Temperatures
							// Cell Temperatures Read Registers for Tower_1 starts from 10000, for Tower_2
							// 12000, for Tower_3 14000
							// (t-1)*2000+10000) calculates Tower Offset value
							var uwe = new SignedWordElement(moduleOffset + module * 100 + 18 + j);
							ameTemp[j] = m(channelId, uwe);
						}
					}

					var channelId = new ChannelIdImpl(//
							"TOWER_" + tower + "_MODULE_" + module + "_SERIAL_NUMBER", //
							Doc.of(OpenemsType.STRING)//
									.persistencePriority(PersistencePriority.HIGH));
					this.addChannel(channelId);

					this.getModbusProtocol().addTasks(//
							new FC3ReadRegistersTask(moduleOffset + module * 100 + 2, Priority.LOW, ameVolt),
							new FC3ReadRegistersTask(moduleOffset + module * 100 + 18, Priority.LOW, ameTemp),
							new FC3ReadRegistersTask(moduleOffset + module * 100 + 83, Priority.LOW,
									m(channelId, new UnsignedDoublewordElement(moduleOffset + module * 100 + 83),
											new ElementToChannelConverter(value -> {
												Integer intValue = TypeUtils.getAsType(OpenemsType.INTEGER, value);
												return buildSerialNumber(SERIAL_NUMBER_PREFIX_MODULE, intValue);
											}))));
				}
			}
		} finally {
			// Always store the last numbers
			this.lastNumberOfTowers = numberOfTowers;
			this.lastNumberOfModulesPerTower = numberOfModulesPerTower;
		}
	}

	/**
	 * Build the serial number with prefix.
	 *
	 * @param prefix the serial number prefix
	 * @param value  the serial number
	 * @return The serial number
	 */
	protected static String buildSerialNumber(String prefix, Integer value) {
		if (value == null || value == 0) {
			// Old BMS firmware versions do not provide serial number
			return null;
		}

		var year = extractNumber(value, 7, 26);
		var month = extractNumber(value, 4, 22);
		var day = extractNumber(value, 5, 17);
		var number = extractNumber(value, 16, 1);

		var serialNumber = new StringBuilder();
		serialNumber.append(prefix);
		serialNumber.append(year < 10 ? "0" + year : year);
		serialNumber.append(month < 10 ? "0" + month : month);
		serialNumber.append(day < 10 ? "0" + day : day);

		var digits = String.valueOf(number).length();
		if (digits <= 6) {
			var maxDigits = "000000";
			var formattedNumber = maxDigits.substring(0, maxDigits.length() - digits) + number;
			serialNumber.append(formattedNumber);
		} else {
			serialNumber.append(number);
		}

		return serialNumber.toString();
	}

	/**
	 * Gets number from given value via bit shifting.
	 *
	 * @param value    to get number from
	 * @param length   of the number
	 * @param position to start extracting
	 * @return Number
	 */
	private static int extractNumber(int value, int length, int position) {
		return (1 << length) - 1 & value >> position - 1;
	}
}
