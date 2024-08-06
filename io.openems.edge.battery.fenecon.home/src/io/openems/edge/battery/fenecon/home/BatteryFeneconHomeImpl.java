package io.openems.edge.battery.fenecon.home;

import static io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent.BitConverter.INVERT;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1;
import static io.openems.edge.bridge.modbus.api.ModbusUtils.readElementOnce;

import java.time.Instant;
import java.util.List;
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
import io.openems.common.types.OptionsEnum;
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
import io.openems.edge.bridge.modbus.api.ModbusUtils;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId.ChannelIdImpl;
import io.openems.edge.common.channel.ChannelUtils;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.internal.OpenemsTypeDoc;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
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
public class BatteryFeneconHomeImpl extends AbstractOpenemsModbusComponent implements ModbusComponent, OpenemsComponent,
		Battery, EventHandler, ModbusSlave, StartStoppable, BatteryFeneconHome, ModbusHelper {

	public static final int DEFAULT_CRITICAL_MIN_VOLTAGE = 2800;
	protected static final int TIMEOUT = 600; // [10 minutes in seconds]
	private Instant timeCriticalMinVoltage;

	protected final StateMachine stateMachine = new StateMachine(State.UNDEFINED);

	private final Logger log = LoggerFactory.getLogger(BatteryFeneconHomeImpl.class);
	private final AtomicReference<StartStop> startStopTarget = new AtomicReference<>(StartStop.UNDEFINED);

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private Config config;
	private BatteryProtection batteryProtection;

	public BatteryFeneconHomeImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				BatteryProtection.ChannelId.values(), //
				BatteryFeneconHome.ChannelId.values() //
		);
		this.updateHardwareType(BatteryFeneconHomeHardwareType.DEFAULT);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;

		// Predefine BatteryProtection. Later adapted to the hardware type.
		this.batteryProtection = BatteryProtection.create(this) //
				.applyBatteryProtectionDefinition(new FeneconHomeBatteryProtection52(), this.componentManager) //
				.build();

		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}

		this.detectHardwareType();
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
			this.checkCriticalMinVoltage();
			this.handleStateMachine();
			break;
		}
	}

	/**
	 * Handles the State-Machine.
	 */
	private void handleStateMachine() {
		// Store the current State
		this.channel(BatteryFeneconHome.ChannelId.STATE_MACHINE).setNextValue(this.stateMachine.getCurrentState());

		// Initialize 'Start-Stop' Channel
		this._setStartStop(StartStop.UNDEFINED);

		// Prepare Context
		var batteryStartUpRelayChannel = this.getBatteryStartUpRelayChannel();
		var batteryStartUpRelay = batteryStartUpRelayChannel != null ? batteryStartUpRelayChannel.value().get() : null;
		var context = new Context(this, this.componentManager.getClock(), //
				batteryStartUpRelay,
				(value) -> setBatteryStartUpRelay(batteryStartUpRelayChannel, value, this::logInfo, this::logWarn), //
				this.getBmsControl(), //
				this.getModbusCommunicationFailed(), //
				() -> this.retryModbusCommunication());
		// Call the StateMachine
		try {

			this.stateMachine.run(context);

			this.channel(BatteryFeneconHome.ChannelId.RUN_FAILED).setNextValue(false);

		} catch (OpenemsNamedException e) {
			this.channel(BatteryFeneconHome.ChannelId.RUN_FAILED).setNextValue(true);
			this.logError(this.log, "StateMachine failed: " + e.getMessage());
		}
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(500, Priority.LOW, //
						m(new BitsWordElement(500, this) //
								.bit(0, BatteryFeneconHome.ChannelId.RACK_PRE_ALARM_CELL_OVER_VOLTAGE) //
								.bit(1, BatteryFeneconHome.ChannelId.RACK_PRE_ALARM_CELL_UNDER_VOLTAGE) //
								.bit(2, BatteryFeneconHome.ChannelId.RACK_PRE_ALARM_OVER_CHARGING_CURRENT) //
								.bit(3, BatteryFeneconHome.ChannelId.RACK_PRE_ALARM_OVER_DISCHARGING_CURRENT) //
								.bit(4, BatteryFeneconHome.ChannelId.RACK_PRE_ALARM_OVER_TEMPERATURE) //
								.bit(5, BatteryFeneconHome.ChannelId.RACK_PRE_ALARM_UNDER_TEMPERATURE) //
								.bit(6, BatteryFeneconHome.ChannelId.RACK_PRE_ALARM_CELL_VOLTAGE_DIFFERENCE) //
								.bit(7, BatteryFeneconHome.ChannelId.RACK_PRE_ALARM_BCU_TEMP_DIFFERENCE) //
								.bit(8, BatteryFeneconHome.ChannelId.RACK_PRE_ALARM_UNDER_SOC) //
								.bit(9, BatteryFeneconHome.ChannelId.RACK_PRE_ALARM_UNDER_SOH) //
								.bit(10, BatteryFeneconHome.ChannelId.RACK_PRE_ALARM_OVER_CHARGING_POWER) //
								.bit(11, BatteryFeneconHome.ChannelId.RACK_PRE_ALARM_OVER_DISCHARGING_POWER) //
						), //
						m(new BitsWordElement(501, this) //
								.bit(0, BatteryFeneconHome.ChannelId.RACK_LEVEL_1_CELL_OVER_VOLTAGE) //
								.bit(1, BatteryFeneconHome.ChannelId.RACK_LEVEL_1_CELL_UNDER_VOLTAGE) //
								.bit(2, BatteryFeneconHome.ChannelId.RACK_LEVEL_1_OVER_CHARGING_CURRENT) //
								.bit(3, BatteryFeneconHome.ChannelId.RACK_LEVEL_1_OVER_DISCHARGING_CURRENT) //
								.bit(4, BatteryFeneconHome.ChannelId.RACK_LEVEL_1_OVER_TEMPERATURE) //
								.bit(5, BatteryFeneconHome.ChannelId.RACK_LEVEL_1_UNDER_TEMPERATURE) //
								.bit(6, BatteryFeneconHome.ChannelId.RACK_LEVEL_1_CELL_VOLTAGE_DIFFERENCE) //
								.bit(7, BatteryFeneconHome.ChannelId.RACK_LEVEL_1_BCU_TEMP_DIFFERENCE) //
								.bit(8, BatteryFeneconHome.ChannelId.RACK_LEVEL_1_UNDER_SOC) //
								.bit(9, BatteryFeneconHome.ChannelId.RACK_LEVEL_1_UNDER_SOH) //
								.bit(10, BatteryFeneconHome.ChannelId.RACK_LEVEL_1_OVER_CHARGING_POWER) //
								.bit(11, BatteryFeneconHome.ChannelId.RACK_LEVEL_1_OVER_DISCHARGING_POWER) //
						), //
						m(new BitsWordElement(502, this) //
								.bit(0, BatteryFeneconHome.ChannelId.RACK_LEVEL_2_CELL_OVER_VOLTAGE) //
								.bit(1, BatteryFeneconHome.ChannelId.RACK_LEVEL_2_CELL_UNDER_VOLTAGE) //
								.bit(2, BatteryFeneconHome.ChannelId.RACK_LEVEL_2_OVER_CHARGING_CURRENT) //
								.bit(3, BatteryFeneconHome.ChannelId.RACK_LEVEL_2_OVER_DISCHARGING_CURRENT) //
								.bit(4, BatteryFeneconHome.ChannelId.RACK_LEVEL_2_OVER_TEMPERATURE) //
								.bit(5, BatteryFeneconHome.ChannelId.RACK_LEVEL_2_UNDER_TEMPERATURE) //
								.bit(6, BatteryFeneconHome.ChannelId.RACK_LEVEL_2_CELL_VOLTAGE_DIFFERENCE) //
								.bit(7, BatteryFeneconHome.ChannelId.RACK_LEVEL_2_BCU_TEMP_DIFFERENCE) //
								.bit(8, BatteryFeneconHome.ChannelId.RACK_LEVEL_2_CELL_TEMPERATURE_DIFFERENCE) //
								.bit(9, BatteryFeneconHome.ChannelId.RACK_LEVEL_2_INTERNAL_COMMUNICATION) //
								.bit(10, BatteryFeneconHome.ChannelId.RACK_LEVEL_2_EXTERNAL_COMMUNICATION) //
								.bit(11, BatteryFeneconHome.ChannelId.RACK_LEVEL_2_PRE_CHARGE_FAIL) //
								.bit(12, BatteryFeneconHome.ChannelId.RACK_LEVEL_2_PARALLEL_FAIL) //
								.bit(13, BatteryFeneconHome.ChannelId.RACK_LEVEL_2_SYSTEM_FAIL) //
								.bit(14, BatteryFeneconHome.ChannelId.RACK_LEVEL_2_HARDWARE_FAIL)), //
						m(new BitsWordElement(503, this) //
								.bit(0, BatteryFeneconHome.ChannelId.ALARM_POSITION_BCU_1) //
								.bit(1, BatteryFeneconHome.ChannelId.ALARM_POSITION_BCU_2) //
								.bit(2, BatteryFeneconHome.ChannelId.ALARM_POSITION_BCU_3) //
								.bit(3, BatteryFeneconHome.ChannelId.ALARM_POSITION_BCU_4) //
								.bit(4, BatteryFeneconHome.ChannelId.ALARM_POSITION_BCU_5) //
								.bit(5, BatteryFeneconHome.ChannelId.ALARM_POSITION_BCU_6) //
								.bit(6, BatteryFeneconHome.ChannelId.ALARM_POSITION_BCU_7) //
								.bit(7, BatteryFeneconHome.ChannelId.ALARM_POSITION_BCU_8) //
								.bit(8, BatteryFeneconHome.ChannelId.ALARM_POSITION_BCU_9) //
								.bit(9, BatteryFeneconHome.ChannelId.ALARM_POSITION_BCU_10)), //
						m(new BitsWordElement(504, this) //
								.bit(0, BatteryFeneconHome.ChannelId.WARNING_POSITION_BCU_1) //
								.bit(1, BatteryFeneconHome.ChannelId.WARNING_POSITION_BCU_2) //
								.bit(2, BatteryFeneconHome.ChannelId.WARNING_POSITION_BCU_3) //
								.bit(3, BatteryFeneconHome.ChannelId.WARNING_POSITION_BCU_4) //
								.bit(4, BatteryFeneconHome.ChannelId.WARNING_POSITION_BCU_5) //
								.bit(5, BatteryFeneconHome.ChannelId.WARNING_POSITION_BCU_6) //
								.bit(6, BatteryFeneconHome.ChannelId.WARNING_POSITION_BCU_7) //
								.bit(7, BatteryFeneconHome.ChannelId.WARNING_POSITION_BCU_8) //
								.bit(8, BatteryFeneconHome.ChannelId.WARNING_POSITION_BCU_9) //
								.bit(9, BatteryFeneconHome.ChannelId.WARNING_POSITION_BCU_10)), //
						m(new BitsWordElement(505, this) //
								.bit(0, BatteryFeneconHome.ChannelId.FAULT_POSITION_BCU_1) //
								.bit(1, BatteryFeneconHome.ChannelId.FAULT_POSITION_BCU_2) //
								.bit(2, BatteryFeneconHome.ChannelId.FAULT_POSITION_BCU_3) //
								.bit(3, BatteryFeneconHome.ChannelId.FAULT_POSITION_BCU_4) //
								.bit(4, BatteryFeneconHome.ChannelId.FAULT_POSITION_BCU_5) //
								.bit(5, BatteryFeneconHome.ChannelId.FAULT_POSITION_BCU_6) //
								.bit(6, BatteryFeneconHome.ChannelId.FAULT_POSITION_BCU_7) //
								.bit(7, BatteryFeneconHome.ChannelId.FAULT_POSITION_BCU_8) //
								.bit(8, BatteryFeneconHome.ChannelId.FAULT_POSITION_BCU_9) //
								.bit(9, BatteryFeneconHome.ChannelId.FAULT_POSITION_BCU_10))//
				), //

				new FC3ReadRegistersTask(506, Priority.LOW, //
						m(Battery.ChannelId.VOLTAGE, new UnsignedWordElement(506), SCALE_FACTOR_MINUS_1), // [V]
						m(Battery.ChannelId.CURRENT, new SignedWordElement(507), SCALE_FACTOR_MINUS_1), // [A]
						m(Battery.ChannelId.SOC, new UnsignedWordElement(508), SCALE_FACTOR_MINUS_1), // [%]
						m(Battery.ChannelId.SOH, new UnsignedWordElement(509), SCALE_FACTOR_MINUS_1), // [%]
						m(Battery.ChannelId.MIN_CELL_VOLTAGE, new UnsignedWordElement(510)), // [mV]
						m(BatteryFeneconHome.ChannelId.ID_OF_CELL_VOLTAGE_MIN, new UnsignedWordElement(511)), //
						m(Battery.ChannelId.MAX_CELL_VOLTAGE, new UnsignedWordElement(512)), // [mV]
						m(BatteryFeneconHome.ChannelId.ID_OF_CELL_VOLTAGE_MAX, new UnsignedWordElement(513)), //
						m(Battery.ChannelId.MIN_CELL_TEMPERATURE, new SignedWordElement(514), SCALE_FACTOR_MINUS_1), //
						m(BatteryFeneconHome.ChannelId.ID_OF_MIN_TEMPERATURE, new UnsignedWordElement(515)), //
						m(Battery.ChannelId.MAX_CELL_TEMPERATURE, new SignedWordElement(516), SCALE_FACTOR_MINUS_1), //
						m(BatteryFeneconHome.ChannelId.ID_OF_MAX_TEMPERATURE, new UnsignedWordElement(517)), //
						m(BatteryProtection.ChannelId.BP_CHARGE_BMS, new UnsignedWordElement(518),
								SCALE_FACTOR_MINUS_1), // [A]
						m(BatteryProtection.ChannelId.BP_DISCHARGE_BMS, new UnsignedWordElement(519), //
								SCALE_FACTOR_MINUS_1), // [A]
						m(BatteryFeneconHome.ChannelId.MAX_DC_CHARGE_CURRENT_LIMIT_PER_BCU,
								new UnsignedWordElement(520), //
								SCALE_FACTOR_MINUS_1), //
						m(BatteryFeneconHome.ChannelId.MAX_DC_DISCHARGE_CURRENT_LIMIT_PER_BCU,
								new UnsignedWordElement(521), //
								SCALE_FACTOR_MINUS_1), //
						m(BatteryFeneconHome.ChannelId.RACK_NUMBER_OF_BATTERY_BCU, new UnsignedWordElement(522)), //
						m(BatteryFeneconHome.ChannelId.RACK_NUMBER_OF_CELLS_IN_SERIES_PER_MODULE,
								new UnsignedWordElement(523)), //
						m(BatteryFeneconHome.ChannelId.RACK_MAX_CELL_VOLTAGE_LIMIT, new UnsignedWordElement(524)), //
						m(BatteryFeneconHome.ChannelId.RACK_MIN_CELL_VOLTAGE_LIMIT, new UnsignedWordElement(525)), //
						m(new BitsWordElement(526, this) //
								.bit(0, BatteryFeneconHome.ChannelId.RACK_HW_AFE_COMMUNICATION_FAULT) //
								.bit(1, BatteryFeneconHome.ChannelId.RACK_HW_ACTOR_DRIVER_FAULT) //
								.bit(2, BatteryFeneconHome.ChannelId.RACK_HW_EEPROM_COMMUNICATION_FAULT) //
								.bit(3, BatteryFeneconHome.ChannelId.RACK_HW_VOLTAGE_DETECT_FAULT) //
								.bit(4, BatteryFeneconHome.ChannelId.RACK_HW_TEMPERATURE_DETECT_FAULT) //
								.bit(5, BatteryFeneconHome.ChannelId.RACK_HW_CURRENT_DETECT_FAULT) //
								.bit(6, BatteryFeneconHome.ChannelId.RACK_HW_ACTOR_NOT_CLOSE) //
								.bit(7, BatteryFeneconHome.ChannelId.RACK_HW_ACTOR_NOT_OPEN) //
								.bit(8, BatteryFeneconHome.ChannelId.RACK_HW_FUSE_BROKEN)), //
						m(new BitsWordElement(527, this) //
								.bit(0, BatteryFeneconHome.ChannelId.RACK_SYSTEM_AFE_OVER_TEMPERATURE) //
								.bit(1, BatteryFeneconHome.ChannelId.RACK_SYSTEM_AFE_UNDER_TEMPERATURE) //
								.bit(2, BatteryFeneconHome.ChannelId.RACK_SYSTEM_AFE_OVER_VOLTAGE) //
								.bit(3, BatteryFeneconHome.ChannelId.RACK_SYSTEM_AFE_UNDER_VOLTAGE) //
								.bit(4, BatteryFeneconHome.ChannelId.RACK_SYSTEM_HIGH_TEMPERATURE_PERMANENT_FAILURE) //
								.bit(5, BatteryFeneconHome.ChannelId.RACK_SYSTEM_LOW_TEMPERATURE_PERMANENT_FAILURE) //
								.bit(6, BatteryFeneconHome.ChannelId.RACK_SYSTEM_HIGH_CELL_VOLTAGE_PERMANENT_FAILURE) //
								.bit(7, BatteryFeneconHome.ChannelId.RACK_SYSTEM_LOW_CELL_VOLTAGE_PERMANENT_FAILURE) //
								.bit(8, BatteryFeneconHome.ChannelId.RACK_SYSTEM_SHORT_CIRCUIT)), //
						m(BatteryFeneconHome.ChannelId.UPPER_VOLTAGE, new UnsignedWordElement(528))), //
				new FC3ReadRegistersTask(18000, Priority.LOW, //
						m(BatteryFeneconHome.ChannelId.TOWER_4_BMS_SOFTWARE_VERSION, new UnsignedWordElement(18000))), //
				new FC3ReadRegistersTask(16000, Priority.LOW, //
						m(BatteryFeneconHome.ChannelId.TOWER_3_BMS_SOFTWARE_VERSION, new UnsignedWordElement(16000))), //
				new FC3ReadRegistersTask(14000, Priority.LOW, //
						m(BatteryFeneconHome.ChannelId.TOWER_2_BMS_SOFTWARE_VERSION, new UnsignedWordElement(14000))), //
				new FC3ReadRegistersTask(12000, Priority.LOW, //
						m(BatteryFeneconHome.ChannelId.TOWER_1_BMS_SOFTWARE_VERSION, new UnsignedWordElement(12000))), //
				new FC3ReadRegistersTask(10000, Priority.LOW, //
						m(BatteryFeneconHome.ChannelId.TOWER_0_BMS_SOFTWARE_VERSION, new UnsignedWordElement(10000)), //
						new DummyRegisterElement(10001, 10018), //
						m(BatteryFeneconHome.ChannelId.BATTERY_HARDWARE_TYPE, new UnsignedWordElement(10019),
								SCALE_FACTOR_MINUS_1), //
						new DummyRegisterElement(10020, 10023), //
						m(BatteryFeneconHome.ChannelId.NUMBER_OF_MODULES_PER_TOWER, new UnsignedWordElement(10024))), //
				new FC3ReadRegistersTask(44000, Priority.HIGH, //
						m(new BitsWordElement(44000, this) //
								.bit(0, BatteryFeneconHome.ChannelId.BMS_CONTROL, INVERT)) //
				));
	}

	/**
	 * Detects the Hardware Type and updates the HardwareType Channel.
	 * 
	 * @throws OpenemsException on error
	 */
	private void detectHardwareType() throws OpenemsException {
		// Set Battery-Protection
		readElementOnce(this.getModbusProtocol(), ModbusUtils::retryOnNull, new UnsignedWordElement(10019))
				.thenAccept(value -> {
					if (value == null) {
						return;
					}

					var hardwareType = parseHardwareTypeFromRegisterValue(value);
					if (hardwareType == null) {
						this.logWarn(this.log, "Unable to Identify Hardware Type from Register value [" + value + "]");
						hardwareType = BatteryFeneconHomeHardwareType.DEFAULT;
					}
					this.updateHardwareType(hardwareType);
				});
	}

	/**
	 * Get GoodWe hardware version from register value.
	 * 
	 * @param value Register value not formated with SCALE_FACTOR_MINUS_1
	 * @return type as {@link GoodweHardwareType} or null
	 */
	public static BatteryFeneconHomeHardwareType parseHardwareTypeFromRegisterValue(int value) {
		return OptionsEnum.getOption(BatteryFeneconHomeHardwareType.class, value / 10);
	}

	/**
	 * Sets the BatteryHardwareTypeChannel and updates the BatteryProtection.
	 * 
	 * @param hardwareType the {@link BatteryFeneconHomeHardwareType}
	 */
	private void updateHardwareType(BatteryFeneconHomeHardwareType hardwareType) {
		this.getBatteryHardwareTypeChannel().setNextValue(hardwareType);

		// Set Battery Protection depending on the hardware type
		this.batteryProtection = BatteryProtection.create(this) //
				.applyBatteryProtectionDefinition(hardwareType.batteryProtection, this.componentManager) //
				.build();
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
		return Battery.generateDebugLog(this, this.stateMachine);
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				Battery.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(BatteryFeneconHome.class, accessMode, 100) //
						.build());
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
		return switch (this.config.startStop()) {
		case AUTO -> this.startStopTarget.get();
		case START -> StartStop.START;
		case STOP -> StartStop.STOP;
		};
	}

	/**
	 * Update Number of towers and modules; called on onChange event.
	 * 
	 * <p>
	 * Recalculate the number of towers and modules. Unfortunately the battery may
	 * report too small wrong values in the beginning, so we need to recalculate on
	 * every change.
	 * 
	 * <p>
	 * As an alternative, these channels may also be introduced in a record, and the
	 * associated channel value could be read with the aid of
	 * {@link ChannelUtils#getValues}. However, startup time is once again involved
	 * in this process. This indicates that the last callback will have been made
	 * before the record is set. Furthermore, there is no certainty that the
	 * "software version channel value change" will occur, making it unlikely for
	 * this to trigger a callback.
	 */
	protected synchronized void updateNumberOfTowersAndModules() {
		Channel<Integer> numberOfModulesPerTowerChannel = this
				.channel(BatteryFeneconHome.ChannelId.NUMBER_OF_MODULES_PER_TOWER);
		var numberOfModulesPerTowerOpt = numberOfModulesPerTowerChannel.value();

		// Were all required registers read?
		if (!numberOfModulesPerTowerOpt.isDefined()) {
			return;
		}

		// Evaluate the total number of towers by reading the software versions of
		// towers 2 and 3: they are '0' when the respective tower is not available.
		final var softwareVersionlist = List.of(//
				BatteryFeneconHome.ChannelId.TOWER_0_BMS_SOFTWARE_VERSION, //
				BatteryFeneconHome.ChannelId.TOWER_1_BMS_SOFTWARE_VERSION, //
				BatteryFeneconHome.ChannelId.TOWER_2_BMS_SOFTWARE_VERSION, //
				BatteryFeneconHome.ChannelId.TOWER_3_BMS_SOFTWARE_VERSION, //
				BatteryFeneconHome.ChannelId.TOWER_4_BMS_SOFTWARE_VERSION//
		) //
				.stream() //
				.map(c -> {
					IntegerReadChannel channel = this.channel(c);
					return channel.value().get();
				}) //
				.toList();

		final var numberOfTowers = calculateTowerNumberFromSoftwareVersion(softwareVersionlist);

		// Write 'TOWER_NUMBER' Debug Channel
		Channel<?> numberOfTowersChannel = this.channel(BatteryFeneconHome.ChannelId.NUMBER_OF_TOWERS);
		numberOfTowersChannel.setNextValue(numberOfTowers);
		if (numberOfTowers == null) {
			return;
		}

		final var moduleMaxVoltage = this.getBatteryHardwareType().moduleMaxVoltage;
		final var moduleMinVoltage = this.getBatteryHardwareType().moduleMinVoltage;
		final var capacityPerModule = this.getBatteryHardwareType().capacityPerModule;
		final int numberOfModulesPerTower = numberOfModulesPerTowerOpt.get();

		// Set Battery Channels
		this._setChargeMaxVoltage(Math.round(numberOfModulesPerTower * moduleMaxVoltage));
		this._setDischargeMinVoltage(Math.round(numberOfModulesPerTower * moduleMinVoltage));
		this._setCapacity(numberOfTowers * numberOfModulesPerTower * capacityPerModule);

		// Initialize available Tower- and Module-Channels dynamically.
		try {
			this.initializeTowerModulesChannels(numberOfTowers, numberOfModulesPerTower);
		} catch (OpenemsException e) {
			this.logError(this.log, "Unable to initialize tower modules channels: " + e.getMessage());
			e.printStackTrace();
		}
	}

	protected static Integer calculateTowerNumberFromSoftwareVersion(List<Integer> versionList) {
		var numberOfTowers = 0;
		for (var version : versionList) {
			if (version == null) {
				return null;
			}
			if (version == 0 || version == 256) {
				// Ensure number of towers is never '0' if registers are not null.
				return Math.max(1, numberOfTowers);
			}
			numberOfTowers++;
		}
		return numberOfTowers;
	}

	private int lastNumberOfTowers = 0;
	private int lastNumberOfModulesPerTower = 0;

	/**
	 * Initialize channels per towers and modules.
	 *
	 * @param numberOfTowers          the number of towers
	 * @param numberOfModulesPerTower the number of modules per tower
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
												Level.INFO)) //
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
										SCALE_FACTOR_MINUS_1), //
								m(this.generateTowerChannel(tower, "_SOH", OpenemsType.INTEGER),
										new UnsignedWordElement(towerOffset + 9), // [%]
										SCALE_FACTOR_MINUS_1), //
								m(this.generateTowerChannel(tower, "_VOLTAGE", OpenemsType.INTEGER),
										new UnsignedWordElement(towerOffset + 10), // [V]
										SCALE_FACTOR_MINUS_1), //
								m(this.generateTowerChannel(tower, "_CURRENT", OpenemsType.INTEGER),
										new SignedWordElement(towerOffset + 11), // [A]
										SCALE_FACTOR_MINUS_1), //
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
										new UnsignedWordElement(towerOffset + 19), SCALE_FACTOR_MINUS_1), // [Ah]
								m(this.generateTowerChannel(tower, "_USABLE_CAPACITY", OpenemsType.INTEGER),
										new UnsignedWordElement(towerOffset + 20), //
										SCALE_FACTOR_MINUS_1), // [Ah]
								m(this.generateTowerChannel(tower, "_REMAINING_CAPACITY", OpenemsType.INTEGER),
										new UnsignedWordElement(towerOffset + 21), //
										SCALE_FACTOR_MINUS_1), // [Ah]
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
											return buildSerialNumber(this.getBatteryHardwareType().serialNrPrefixBms,
													intValue);
										}))));
			}

			var towerToUse = 0;
			var moduleToUse = this.lastNumberOfModulesPerTower;
			if (this.lastNumberOfTowers < numberOfTowers) {
				towerToUse = this.lastNumberOfTowers;
				moduleToUse = 0;
			}

			final var cellsPerModule = this.getBatteryHardwareType().cellsPerModule;
			final var tempSensorsPerModule = this.getBatteryHardwareType().tempSensorsPerModule;

			for (var tower = towerToUse; tower < numberOfTowers; tower++) {
				final var towerOffset = tower * 2000 + 10000;
				final var moduleOffset = towerOffset + 100;

				for (var module = moduleToUse; module < numberOfModulesPerTower; module++) {
					/*
					 * Number Of Modules per Tower increased.
					 *
					 * Dynamically generate Channels and Modbus mappings for Cell-Voltages.
					 * Channel-IDs are like "TOWER_0_MODULE_2_CELL_001_VOLTAGE".
					 */
					var ameVolt = new ModbusElement[cellsPerModule];
					for (var cell = 0; cell < cellsPerModule; cell++) {

						// Create Voltage Channel
						var channelId = new ChannelIdImpl(//
								generateSingleCellPrefix(tower, module, cell) + "_VOLTAGE",
								Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT));
						this.addChannel(channelId);

						// Create Modbus-Mapping for Voltages
						var uwe = new UnsignedWordElement(moduleOffset + module * 100 + 2 + cell);
						ameVolt[cell] = m(channelId, uwe);
					}

					/*
					 * Dynamically generate Channels and Modbus mappings for temperature sensors.
					 * Channel-IDs are like "TOWER_0_MODULE_2_TEMPERATURE_SENSOR_1".
					 */
					var ameTemp = new ModbusElement[tempSensorsPerModule];
					for (var sensor = 0; sensor < tempSensorsPerModule; sensor++) {

						// Create Temperature Channel
						var channelId = new ChannelIdImpl(//
								generateTempSensorChannelName(tower, module, sensor + 1),
								Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS));
						this.addChannel(channelId);

						// Create Modbus-Mapping for Temperatures
						// Cell Temperatures Read Registers for Tower_1 starts from 10000, for Tower_2
						// 12000, for Tower_3 14000
						// (t-1)*2000+10000) calculates Tower Offset value
						var uwe = new SignedWordElement(moduleOffset + module * 100 + 18 + sensor);
						ameTemp[sensor] = m(channelId, uwe);
					}

					/*
					 * Temperature balancing sensors
					 */
					final var defaultBalancingTemperatures = 2;
					var ameTempBalancing = new ModbusElement[defaultBalancingTemperatures];
					for (var j = 0; j < defaultBalancingTemperatures; j++) {

						// Create Temperature Channel
						var channelId = new ChannelIdImpl(//
								generateTempBalancingChannelName(tower, module, j + 1),
								Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS));
						this.addChannel(channelId);

						var uwe = new SignedWordElement(moduleOffset + module * 100 + 18 + tempSensorsPerModule + j);
						ameTempBalancing[j] = m(channelId, uwe);
					}

					var channelId = new ChannelIdImpl(//
							"TOWER_" + tower + "_MODULE_" + module + "_SERIAL_NUMBER", //
							Doc.of(OpenemsType.STRING)//
									.persistencePriority(PersistencePriority.HIGH));
					this.addChannel(channelId);

					this.getModbusProtocol().addTasks(//
							new FC3ReadRegistersTask(moduleOffset + module * 100 + 2, Priority.LOW, ameVolt),
							new FC3ReadRegistersTask(moduleOffset + module * 100 + 18, Priority.LOW, ameTemp),
							new FC3ReadRegistersTask(moduleOffset + module * 100 + 18 + tempSensorsPerModule,
									Priority.LOW, ameTempBalancing),
							new FC3ReadRegistersTask(moduleOffset + module * 100 + 83, Priority.LOW,
									m(channelId, new UnsignedDoublewordElement(moduleOffset + module * 100 + 83),
											new ElementToChannelConverter(value -> {
												Integer intValue = TypeUtils.getAsType(OpenemsType.INTEGER, value);
												return buildSerialNumber(
														this.getBatteryHardwareType().serialNrPrefixModule, intValue);
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

	private void logInfo(String message) {
		this.logInfo(this.log, message);
	}

	private void logWarn(String message) {
		this.logWarn(this.log, message);
	}

	/**
	 * Gets the Battery-Start-Up-Relay Channel.
	 * 
	 * @return {@link BooleanWriteChannel} or null
	 */
	private BooleanWriteChannel getBatteryStartUpRelayChannel() {
		try {
			var channel = this.componentManager
					.<BooleanWriteChannel>getChannel(ChannelAddress.fromString(this.config.batteryStartUpRelay()));
			return channel;
		} catch (Exception e) {
			this.logWarn("Unable to get Battery-Start-Up-Relay: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Switch Battery-Start-Up-Relay ON or OFF.
	 *
	 * @param batteryStartUpRelayChannel the Battery-Start-Up-Relay
	 *                                   {@link BooleanWriteChannel}; or null
	 * @param value                      true to switch the relay on; <br/>
	 *                                   false to switch the relay off
	 * @param logInfo                    Consumer for log messages
	 * @param logWarn                    Consumer for warn messages
	 */
	private static void setBatteryStartUpRelay(BooleanWriteChannel batteryStartUpRelayChannel, boolean value,
			Consumer<String> logInfo, Consumer<String> logWarn) {
		var valueString = value ? "ON" : "OFF";

		// Validate availability of batteryStartUpRelay, otherwise ignore
		if (batteryStartUpRelayChannel == null) {
			logWarn.accept("Switching Battery Start Up Relay " + valueString + " failed. Relay is missing");
			return;
		}

		// Switch StartUpRelay
		try {
			batteryStartUpRelayChannel.setNextWriteValue(value);
			logInfo.accept("Switching Battery Start Up Relay " + valueString //
					+ " [" + batteryStartUpRelayChannel.address() + "]");
		} catch (OpenemsNamedException e) {
			logWarn.accept("Switching Battery Start Up Relay " + valueString //
					+ " failed [" + batteryStartUpRelayChannel.address() + "]: " + e.getMessage());
		}
	}

	/**
	 * Generates prefix for Channel-IDs for Cell Temperature and Voltage channels.
	 *
	 * @param tower  number to use
	 * @param module number to use
	 * @return a prefix e.g. "TOWER_1_MODULE_2"
	 */
	private static String generateModulePrefix(int tower, int module) {
		return "TOWER_" + tower + "_MODULE_" + module;
	}

	/**
	 * Generates Channel names for Cell Voltage Channel-IDs.
	 *
	 * <p>
	 * "%03d" creates string number with leading zeros
	 * 
	 * @param tower  number to use
	 * @param module number to use
	 * @param cell   number to user
	 * @return a Channel name e.g. "TOWER_1_MODULE_2_CELL_003_VOLTAGE"
	 */
	public static String generateCellVoltageChannelName(int tower, int module, int cell) {
		return generateModulePrefix(tower, module) + "_CELL_" + String.format("%03d", cell) + "_VOLTAGE";
	}

	/**
	 * Generates Channel names for Temperature Sensor Channel-IDs.
	 *
	 * @param tower  number to use
	 * @param module number to use
	 * @param sensor number to user
	 * @return a Channel name e.g. "TOWER_1_MODULE_2_TEMPERATURE_SENSOR_2"
	 */
	public static String generateTempSensorChannelName(int tower, int module, int sensor) {
		return generateModulePrefix(tower, module) + "_TEMPERATURE_SENSOR_" + sensor;
	}

	/**
	 * Generates Channel names for Temperature Balancing Channel-IDs.
	 *
	 * @param tower  number to use
	 * @param module number to use
	 * @param value  number to user
	 * @return a Channel name e.g. "TOWER_1_MODULE_2_TEMPERATURE_BALANCING_1"
	 */
	public static String generateTempBalancingChannelName(int tower, int module, int value) {
		return generateModulePrefix(tower, module) + "_TEMPERATURE_BALANCING_" + value;
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
	private static String generateSingleCellPrefix(int tower, int module, int num) {
		return "TOWER_" + tower + "_MODULE_" + module + "_CELL_" + String.format("%03d", num);
	}

	@Override
	public BridgeModbus getModbus() {
		return this.getBridgeModbus();
	}

	@Override
	public ModbusProtocol getDefinedModbusProtocol() throws OpenemsException {
		return this.getModbusProtocol();
	}

	private void checkCriticalMinVoltage() {

		final var subState = getMinVoltageSubState(DEFAULT_CRITICAL_MIN_VOLTAGE,
				this.getMinCellVoltage().orElse(Integer.MAX_VALUE), this.getCurrent().orElse(0));
		var now = Instant.now(this.componentManager.getClock());

		switch (subState) {
		case ABOVE_LIMIT -> {
			this._setLowMinVoltageFault(false);
			this._setLowMinVoltageWarning(false);
			this._setLowMinVoltageFaultBatteryStopped(false);
			this.timeCriticalMinVoltage = null;
		}
		case BELOW_LIMIT -> {

			if (this.stateMachine.getCurrentState() == StateMachine.State.STOPPED) {
				this._setLowMinVoltageFaultBatteryStopped(true);
				this._setLowMinVoltageFault(false);
				this._setLowMinVoltageWarning(false);
				return;
			}

			this._setLowMinVoltageFaultBatteryStopped(false);

			if (this.timeCriticalMinVoltage == null) {
				this.timeCriticalMinVoltage = now;
			}

			if (this.timeCriticalMinVoltage.isBefore(now.minusSeconds(TIMEOUT))) {
				this._setLowMinVoltageFault(true);
				this._setLowMinVoltageWarning(false);
				return;
			}
			this._setLowMinVoltageWarning(true);
			this._setLowMinVoltageFault(false);
		}
		case BELOW_LIMIT_CHARGING -> {
			this._setLowMinVoltageFaultBatteryStopped(false);
			this._setLowMinVoltageWarning(true);
			this._setLowMinVoltageFault(false);
			this.timeCriticalMinVoltage = null;
		}
		}
	}

	protected static MinVoltageSubState getMinVoltageSubState(int minVoltageLimit, int currentMinVoltage, int current) {
		if (currentMinVoltage > minVoltageLimit) {
			return MinVoltageSubState.ABOVE_LIMIT;
		}
		if (current < 0) {
			return MinVoltageSubState.BELOW_LIMIT_CHARGING;
		}
		return MinVoltageSubState.BELOW_LIMIT;
	}

	protected static enum MinVoltageSubState {
		ABOVE_LIMIT, //
		BELOW_LIMIT, //
		BELOW_LIMIT_CHARGING; //
	}
}
