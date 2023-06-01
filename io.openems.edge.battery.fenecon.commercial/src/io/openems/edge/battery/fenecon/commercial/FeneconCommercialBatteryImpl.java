package io.openems.edge.battery.fenecon.commercial;

import static io.openems.common.types.OpenemsType.BOOLEAN;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.DIRECT_1_TO_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_2;
import static io.openems.edge.bridge.modbus.api.element.WordOrder.LSWMSW;

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
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.fenecon.commercial.statemachine.Context;
import io.openems.edge.battery.fenecon.commercial.statemachine.StateMachine;
import io.openems.edge.battery.fenecon.commercial.statemachine.StateMachine.State;
import io.openems.edge.battery.protection.BatteryProtection;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.LongWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
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
		name = "Battery.Fenecon.Commercial", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
		})
public class FeneconCommercialBatteryImpl extends AbstractOpenemsModbusComponent implements FeneconCommercialBattery,
		Battery, ModbusComponent, OpenemsComponent, EventHandler, ModbusSlave, StartStoppable {

	private static final long DEFAULT_HEART_BEAT = 1200; /* 4*second: for 30 min (4*1800second) */
	private static final int MODULE_3_5_KWH = 3500;
	private static final int NUMBER_OF_TEMPERATURE_CELLS_PER_MODULE = 8;
	private static final int DEFAULT_UNIT_NUMBER = 24320;

	private final Logger log = LoggerFactory.getLogger(FeneconCommercialBatteryImpl.class);
	private final StateMachine stateMachine = new StateMachine(State.UNDEFINED);

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	private Config config = null;
	private BatteryProtection batteryProtection = null;

	public FeneconCommercialBatteryImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				BatteryProtection.ChannelId.values(), //
				FeneconCommercialBattery.ChannelId.values() //
		);
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}

		// Initialize Battery-Protection
		this.batteryProtection = BatteryProtection.create(this) //
				.applyBatteryProtectionDefinition(new BatteryProtectionDefinition(), this.componentManager) //
				.build();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	private final ElementToChannelConverter ignoreZero = IgnoreZeroConverter.from(this, DIRECT_1_TO_1);

	private final ElementToChannelConverter ignoreZeroAndScaleFactorMinus2 = IgnoreZeroConverter.from(this,
			SCALE_FACTOR_MINUS_2);

	/**
	 * Generates serial number based on specific bitwise operation. Helps to build
	 * Master, Sub-master and Modules Serial Numbers.
	 *
	 * @param value Read value from the Modbus register.
	 * @return {@link String} buildSerialNumber.
	 */
	protected static final ElementToChannelConverter SERIAL_NUMBER_CONVERTER = new ElementToChannelConverter(v -> {
		if (v == null) {
			return null;
		}
		String value = TypeUtils.getAsType(OpenemsType.STRING, v);
		var readString = new StringBuilder(value);
		var result = new StringBuilder();
		var reverse = new StringBuilder();
		for (var i = 0; i <= value.length() - 2; i += 2) {
			var subString = new StringBuilder(readString.substring(i, i + 2));
			reverse = subString.reverse();
			result = result.append(reverse);
			reverse = null;
		}
		return result.toString();
	});

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this, //
				// Versions
				new FC3ReadRegistersTask(0, Priority.LOW, //
						m(FeneconCommercialBattery.ChannelId.MASTER_MCU_HARDWARE_VERSION, new StringWordElement(0, 5),
								SERIAL_NUMBER_CONVERTER),
						m(FeneconCommercialBattery.ChannelId.MASTER_MCU_FIRMWARE_VERSION, new StringWordElement(5, 2),
								SERIAL_NUMBER_CONVERTER)), //
				new FC3ReadRegistersTask(2176, Priority.LOW, //
						m(FeneconCommercialBattery.ChannelId.SLAVE_MCU_HARDWARE_VERSION, new StringWordElement(2176, 5),
								SERIAL_NUMBER_CONVERTER),
						m(FeneconCommercialBattery.ChannelId.SLAVE_MCU_FIRMWARE_VERSION, new StringWordElement(2181, 2),
								SERIAL_NUMBER_CONVERTER)),

				new FC3ReadRegistersTask(17, Priority.LOW, //
						m(new UnsignedWordElement(17)).build().onUpdateCallback(new ByteElement(this, //
								ByteElement.Shifter.ONLY_SECOND_CHANNEL, //
								FeneconCommercialBattery.ChannelId.UNIT_ID)),
						m(new UnsignedWordElement(18)).build().onUpdateCallback(new ByteElement(this, //
								ByteElement.Shifter.ONLY_FIRST_CHANNEL, //
								FeneconCommercialBattery.ChannelId.UNIT_NUMBER)),
						new DummyRegisterElement(19), //
						m(FeneconCommercialBattery.ChannelId.SUBMASTER_MAP, new UnsignedDoublewordElement(20))
								.wordOrder(LSWMSW)), //
				new FC3ReadRegistersTask(161, Priority.LOW, //
						m(new UnsignedWordElement(161)).build().onUpdateCallback(value -> {
							Integer baudrate;
							if (value == null) {
								baudrate = null;
							} else {
								baudrate = (int) (value & 0xf00) >> 8;
							}
							this.channel(FeneconCommercialBattery.ChannelId.BAUDRATE).setNextValue(baudrate);
						})), //

				// Master BMS RO
				new FC3ReadRegistersTask(2628, Priority.HIGH, //
						m(FeneconCommercialBattery.ChannelId.ONLINE_TOWER,
								new UnsignedDoublewordElement(2628).wordOrder(LSWMSW)),
						m(FeneconCommercialBattery.ChannelId.RUNNING_TOWER,
								new UnsignedDoublewordElement(2630).wordOrder(LSWMSW)),
						new DummyRegisterElement(2632, 2679), //
						m(Battery.ChannelId.VOLTAGE, new SignedDoublewordElement(2680).wordOrder(LSWMSW),
								SCALE_FACTOR_MINUS_2), //
						m(Battery.ChannelId.CURRENT, new SignedDoublewordElement(2682).wordOrder(LSWMSW),
								SCALE_FACTOR_MINUS_2), //
						m(FeneconCommercialBattery.ChannelId.BATTERY_SOC, new UnsignedWordElement(2684),
								this.ignoreZeroAndScaleFactorMinus2), //
						m(Battery.ChannelId.SOH, new UnsignedWordElement(2685), SCALE_FACTOR_MINUS_2), //
						m(FeneconCommercialBattery.ChannelId.NOMINAL_CAPACITY,
								new SignedDoublewordElement(2686).wordOrder(LSWMSW)), //
						m(FeneconCommercialBattery.ChannelId.TOTAL_CHARGE_CAPACITY_AMPERE_HOURS,
								new SignedQuadruplewordElement(2688).wordOrder(LSWMSW)), //
						m(FeneconCommercialBattery.ChannelId.TOTAL_DISCHARGE_CAPACITY_AMPERE_HOURS,
								new SignedQuadruplewordElement(2692).wordOrder(LSWMSW), SCALE_FACTOR_MINUS_1), //
						m(FeneconCommercialBattery.ChannelId.MAX_SOC, new UnsignedWordElement(2696),
								SCALE_FACTOR_MINUS_2), //
						m(FeneconCommercialBattery.ChannelId.MAX_SOC_TOWER_ID, new UnsignedWordElement(2697)), //
						m(FeneconCommercialBattery.ChannelId.MIN_SOC, new UnsignedWordElement(2698),
								SCALE_FACTOR_MINUS_2), //
						m(FeneconCommercialBattery.ChannelId.MIN_SOC_TOWER_ID, new UnsignedWordElement(2699)), //
						m(Battery.ChannelId.MAX_CELL_VOLTAGE, new UnsignedWordElement(2700), this.ignoreZero), //
						m(new UnsignedWordElement(2701)).build().onUpdateCallback(new ByteElement(this, //
								ByteElement.Shifter.SEPARATE_BITS_AS_6_AND_10_FOR_TWO_CHANNELS, //
								FeneconCommercialBattery.ChannelId.MAX_CELL_VOLTAGE_TOWER_ID, //
								FeneconCommercialBattery.ChannelId.MAX_CELL_VOLTAGE_CELL_ID)),
						m(Battery.ChannelId.MIN_CELL_VOLTAGE, new UnsignedWordElement(2702), this.ignoreZero), //
						m(new UnsignedWordElement(2703)).build().onUpdateCallback(new ByteElement(this, //
								ByteElement.Shifter.SEPARATE_BITS_AS_6_AND_10_FOR_TWO_CHANNELS, //
								FeneconCommercialBattery.ChannelId.MIN_CELL_VOLTAGE_TOWER_ID, //
								FeneconCommercialBattery.ChannelId.MIN_CELL_VOLTAGE_CELL_ID)),
						m(Battery.ChannelId.MAX_CELL_TEMPERATURE, new SignedWordElement(2704),
								this.ignoreZeroAndScaleFactorMinus2), //
						m(new UnsignedWordElement(2705)).build().onUpdateCallback(new ByteElement(this, //
								ByteElement.Shifter.SEPERATE_TO_TWO_8_BIT_CHANNELS, //
								FeneconCommercialBattery.ChannelId.MAX_TEMPERATURE_TOWER_ID, //
								FeneconCommercialBattery.ChannelId.MAX_TEMPERATURE_MODULE_ID)),
						m(Battery.ChannelId.MIN_CELL_TEMPERATURE, new SignedWordElement(2706),
								this.ignoreZeroAndScaleFactorMinus2), //
						m(new UnsignedWordElement(2707)).build().onUpdateCallback(new ByteElement(this, //
								ByteElement.Shifter.SEPERATE_TO_TWO_8_BIT_CHANNELS, //
								FeneconCommercialBattery.ChannelId.MIN_TEMPERATURE_TOWER_ID,
								FeneconCommercialBattery.ChannelId.MIN_TEMPERATURE_MODULE_ID)), //
						m(FeneconCommercialBattery.ChannelId.INSULATION_RESISTANCE_AT_POSITIVE_POLE,
								new SignedDoublewordElement(2708).wordOrder(LSWMSW), SCALE_FACTOR_MINUS_1), //
						m(FeneconCommercialBattery.ChannelId.INSULATION_RESISTANCE_AT_NEGATIVE_POLE,
								new SignedDoublewordElement(2710).wordOrder(LSWMSW), SCALE_FACTOR_MINUS_1)), //

				new FC3ReadRegistersTask(2720, Priority.LOW, //
						m(FeneconCommercialBattery.ChannelId.TOTAL_CHARGE_CAPACITY_WATT_HOURS,
								new SignedQuadruplewordElement(2720).wordOrder(LSWMSW), SCALE_FACTOR_MINUS_1), //
						m(FeneconCommercialBattery.ChannelId.TOTAL_DISCHARGE_CAPACITY_WATT_HOURS,
								new SignedQuadruplewordElement(2724).wordOrder(LSWMSW), SCALE_FACTOR_MINUS_1), //
						m(FeneconCommercialBattery.ChannelId.SYSTEM_FAULT_COUNTERS, new UnsignedWordElement(2728)), //
						m(new UnsignedWordElement(2729)).build().onUpdateCallback(value -> {
							if (value == null) {
								return;
							}
							this.channel(FeneconCommercialBattery.ChannelId.FAULT_STATUS)
									.setNextValue((value & 0x1) == 1);
							this.channel(FeneconCommercialBattery.ChannelId.POWER_ON)
									.setNextValue((value & 0x100) >> 8);
							this.channel(FeneconCommercialBattery.ChannelId.LOW_SELF_CONSUMPTION_STATUS)
									.setNextValue((value & 0x200) >> 9);
							this.channel(FeneconCommercialBattery.ChannelId.FAULT).setNextValue((value & 0x400) >> 10);
							this.channel(FeneconCommercialBattery.ChannelId.RUNNING)
									.setNextValue((value & 0x1000) >> 12);
							this.channel(FeneconCommercialBattery.ChannelId.EXTERNAL_COMMUNICATION_ONLY_UNDER_STANDBY)
									.setNextValue((value & 0x2000) >> 13);
						}), //

						m(new BitsWordElement(2730, this) //
								.bit(0, FeneconCommercialBattery.ChannelId.MAIN_SWITCH_STATUS) //
								.bit(1, FeneconCommercialBattery.ChannelId.BATTERY_ONLINE) //
								.bit(2, FeneconCommercialBattery.ChannelId.PCS_ONLINE) //
								.bit(3, FeneconCommercialBattery.ChannelId.UPS_ONLINE) //
								.bit(4, FeneconCommercialBattery.ChannelId.STS_ONLINE) //
								.bit(5, FeneconCommercialBattery.ChannelId.BATTERY_18650_LOW) //
								.bit(8, FeneconCommercialBattery.ChannelId.MASTER_CPU_INITIALIZE) //
								.bit(9, FeneconCommercialBattery.ChannelId.SLAVE_CPU_INITIALIZE) //
								.bit(10, FeneconCommercialBattery.ChannelId.BATTERY_SYSTEM_INITIALIZE_ACTIVE) //
								.bit(11, FeneconCommercialBattery.ChannelId.PCS_INITIALIZE_ACTIVE) //
								.bit(12, FeneconCommercialBattery.ChannelId.UPS_INITIALIZE_ACTIVE) //
						), //
						m(new BitsWordElement(2731, this) //
								.bit(0, FeneconCommercialBattery.ChannelId.MASTER_CPU_INITIALIZE_FINISH) //
								.bit(1, FeneconCommercialBattery.ChannelId.SLAVE_CPU_INITIALIZE_FINISH) //
								.bit(2, FeneconCommercialBattery.ChannelId.BATTERY_SYSTEM_INITIALIZE_FINISH) //
								.bit(3, FeneconCommercialBattery.ChannelId.PCS_INITIALIZE_FINISH) //
								.bit(4, FeneconCommercialBattery.ChannelId.UPS_INITIALIZE_FINISH) //
								.bit(8, FeneconCommercialBattery.ChannelId.MASTER_CPU_INITIALIZE_FAIL) //
								.bit(9, FeneconCommercialBattery.ChannelId.SLAVE_CPU_INITIALIZE_FAIL) //
								.bit(10, FeneconCommercialBattery.ChannelId.BATTERY_SYSTEM_INITIALIZE_FAIL) //
								.bit(11, FeneconCommercialBattery.ChannelId.PCS_INITIALIZE_FAIL) //
								.bit(12, FeneconCommercialBattery.ChannelId.UPS_INITIALIZE_FAIL) //
						), //
						m(new BitsWordElement(2732, this) //
								.bit(0, FeneconCommercialBattery.ChannelId.DRY_CONTACT_FAIL) //
								.bit(1, FeneconCommercialBattery.ChannelId.POWER_SUPPLY_24V_FAIL) //
								.bit(2, FeneconCommercialBattery.ChannelId.EEPROM2_FAULT) //
								.bit(3, FeneconCommercialBattery.ChannelId.BATTERY_18650_FAULT) //
								.bit(4, FeneconCommercialBattery.ChannelId.BATTERY_SYSTEM_FAULT) //
								.bit(5, FeneconCommercialBattery.ChannelId.NO_BATTERY) //
								.bit(6, FeneconCommercialBattery.ChannelId.PCS_FAULT) //
								.bit(7, FeneconCommercialBattery.ChannelId.NO_PCS) //
								.bit(8, FeneconCommercialBattery.ChannelId.UPS_FAULT) //
								.bit(9, FeneconCommercialBattery.ChannelId.NO_UPS) //
								.bit(10, FeneconCommercialBattery.ChannelId.INSULATION_RESISTANCE_DETECTION_FAULT) //
								.bit(11, FeneconCommercialBattery.ChannelId.SLAVE_MCU_FAULT) //
								.bit(12, FeneconCommercialBattery.ChannelId.SYSTEM_TEMPERATURE_FAULT) //
								.bit(13, FeneconCommercialBattery.ChannelId.PCS_STOP) //
								.bit(14, FeneconCommercialBattery.ChannelId.METER_FAULT) //
								.bit(15, FeneconCommercialBattery.ChannelId.BATTERY_TOWERS_TEMPERATURE_SENSORS_FAULT) //
						), //
						m(new BitsWordElement(2733, this) //
								.bit(0, FeneconCommercialBattery.ChannelId.SYSTEM_TEMPERATURE_SENSORS_FAULT) //
								.bit(1, FeneconCommercialBattery.ChannelId.SYSTEM_OVER_TEMPERATURE_FAULT) //
								.bit(2, FeneconCommercialBattery.ChannelId.SYSTEM_LOW_TEMPERATURE_FAULT) //
								.bit(3, FeneconCommercialBattery.ChannelId.STS_FAULT) //
								.bit(4, FeneconCommercialBattery.ChannelId.PCS_OVER_TEMPERATURE_FAULT) //
								.bit(5, FeneconCommercialBattery.ChannelId.EEPROM_FAULT) //
								.bit(6, FeneconCommercialBattery.ChannelId.FLASH_FAULT) //
								.bit(7, FeneconCommercialBattery.ChannelId.EMS_FAULT) //
								.bit(8, FeneconCommercialBattery.ChannelId.SD_FAULT) //
						),

						m(new BitsWordElement(2734, this) //
								.bit(0, FeneconCommercialBattery.ChannelId.BATTERY_18650_WARNING) //
								.bit(1, FeneconCommercialBattery.ChannelId.MASTER_BATTERY_WARNING) //
								.bit(2, FeneconCommercialBattery.ChannelId.PCS_WARNING) //
								.bit(3, FeneconCommercialBattery.ChannelId.UPS_WARNING) //
								.bit(4, FeneconCommercialBattery.ChannelId.SLAVE_MCU_WARNING) //
								.bit(5, FeneconCommercialBattery.ChannelId.SYSTEM_TOO_MUCH_OVER_TEMPERATURE_WARNING) //
								.bit(6, FeneconCommercialBattery.ChannelId.SYSTEM_OVER_TEMPERATURE_WARNING) //
								.bit(7, FeneconCommercialBattery.ChannelId.SYSTEM_TOO_MUCH_LOW_TEMPERATURE_WARNING) //
								.bit(8, FeneconCommercialBattery.ChannelId.SYSTEM_LOW_TEMPERATURE_WARNING) //
								.bit(9, FeneconCommercialBattery.ChannelId.FAN_FAULT) //
								.bit(10, FeneconCommercialBattery.ChannelId.BATTERY_TOWERS_TEMPERATURE_SENSORS_WARNING) //
								.bit(11, FeneconCommercialBattery.ChannelId.SYSTEM_TEMPERATURE_SENSORS_WARNING) //
								.bit(12, FeneconCommercialBattery.ChannelId.STS_WARNING) //
								.bit(13, FeneconCommercialBattery.ChannelId.PCS_TEMPERATURE_WARNING) //
								.bit(14, FeneconCommercialBattery.ChannelId.PCS_OVER_TEMPERATURE) //
						), //
						m(new BitsWordElement(2735, this) //
								.bit(1, FeneconCommercialBattery.ChannelId.OVER_TEMPERATURE_STOP_PCS) //
								.bit(2, FeneconCommercialBattery.ChannelId.LOW_TEMPERATURE_STOP_PCS) //
								.bit(3, FeneconCommercialBattery.ChannelId.OVER_CURRENT_STOP_CHARGING) //
								.bit(4, FeneconCommercialBattery.ChannelId.OVER_CURRENT_STOP_DISCHARGING) //
								.bit(5, FeneconCommercialBattery.ChannelId.OVER_TEMPERATURE_STOP_CHARGING) //
								.bit(6, FeneconCommercialBattery.ChannelId.LOW_TEMPERATURE_STOP_DISCHARGING) //
								.bit(7, FeneconCommercialBattery.ChannelId.VOLTAGE_DIFFERENCE_HIGH_STOP_PCS) //
								.bit(8, FeneconCommercialBattery.ChannelId.POWER_HIGH_STOP_PCS) //
								.bit(9, FeneconCommercialBattery.ChannelId.VOLTAGE_HIGH) //
								.bit(10, FeneconCommercialBattery.ChannelId.VOLTAGE_LOW) //
								.bit(11, FeneconCommercialBattery.ChannelId.TEMPERATURE_HIGH) //
								.bit(12, FeneconCommercialBattery.ChannelId.TEMPERATURE_LOW) //
						), //
						new DummyRegisterElement(2736, 2737), //
						m(FeneconCommercialBattery.ChannelId.INSULATION_RESISTANCE_DETECTION_STATUS,
								new UnsignedDoublewordElement(2738).wordOrder(LSWMSW), SCALE_FACTOR_MINUS_1), //
						m(FeneconCommercialBattery.ChannelId.RELAY_STATUS,
								new UnsignedDoublewordElement(2740).wordOrder(LSWMSW)), //
						m(FeneconCommercialBattery.ChannelId.BATTERY_MAX_CELL_VOLT,
								new UnsignedDoublewordElement(2742).wordOrder(LSWMSW), SCALE_FACTOR_1), //
						m(FeneconCommercialBattery.ChannelId.BATTERY_MIN_CELL_VOLT,
								new UnsignedDoublewordElement(2744).wordOrder(LSWMSW), SCALE_FACTOR_1), //
						new DummyRegisterElement(2746, 2773), //
						m(FeneconCommercialBattery.ChannelId.BATTERY_NOMINAL_POWER,
								new UnsignedDoublewordElement(2774).wordOrder(LSWMSW)), //
						m(FeneconCommercialBattery.ChannelId.BATTERY_AVAILABLE_POWER,
								new UnsignedDoublewordElement(2776).wordOrder(LSWMSW)), //
						new DummyRegisterElement(2778, 2784), //
						m(new UnsignedWordElement(2785)).build().onUpdateCallback(value -> {
							if (value == null) {
								return;
							}
							this.channel(FeneconCommercialBattery.ChannelId.NUMBER_OF_TOWERS).setNextValue(value >> 8);

						}), //
						m(FeneconCommercialBattery.ChannelId.START_CHARGE_VOLTAGE_LIMIT, new UnsignedWordElement(2786)), //
						m(FeneconCommercialBattery.ChannelId.START_DISCHARGE_VOLTAGE_LIMIT,
								new UnsignedWordElement(2787)), //
						new DummyRegisterElement(2788, 2789), //
						m(BatteryProtection.ChannelId.BP_CHARGE_BMS,
								new SignedDoublewordElement(2790).wordOrder(LSWMSW), SCALE_FACTOR_MINUS_1), //
						m(BatteryProtection.ChannelId.BP_DISCHARGE_BMS,
								new SignedDoublewordElement(2792).wordOrder(LSWMSW), SCALE_FACTOR_MINUS_1), //
						m(FeneconCommercialBattery.ChannelId.CHARGE_MAX_POWER,
								new SignedDoublewordElement(2794).wordOrder(LSWMSW)), //
						m(FeneconCommercialBattery.ChannelId.DISCHARGE_MAX_POWER,
								new SignedDoublewordElement(2796).wordOrder(LSWMSW)), //
						m(FeneconCommercialBattery.ChannelId.BATTERY_NOMINAL_CURRENT,
								new SignedDoublewordElement(2798).wordOrder(LSWMSW), SCALE_FACTOR_MINUS_1), //
						m(FeneconCommercialBattery.ChannelId.BATTERY_AVAILABLE_CURRENT,
								new SignedDoublewordElement(2800).wordOrder(LSWMSW), SCALE_FACTOR_MINUS_1), //
						m(FeneconCommercialBattery.ChannelId.SOC_FOR_INVERTER, new UnsignedWordElement(2802),
								SCALE_FACTOR_MINUS_2), //
						new DummyRegisterElement(2803), //
						m(Battery.ChannelId.CHARGE_MAX_VOLTAGE, new UnsignedWordElement(2804), SCALE_FACTOR_MINUS_1), //
						m(Battery.ChannelId.DISCHARGE_MIN_VOLTAGE, new UnsignedWordElement(2805), SCALE_FACTOR_MINUS_1), //
						new DummyRegisterElement(2806, 2812), //
						m(FeneconCommercialBattery.ChannelId.FORCE_TO_CHARGE, new UnsignedWordElement(2813)), //
						m(FeneconCommercialBattery.ChannelId.CHARGE_READY, new UnsignedWordElement(2814)), //
						m(FeneconCommercialBattery.ChannelId.DISCHARGE_READY, new UnsignedWordElement(2815))), //

				// Sub Master BMS RW
				new FC3ReadRegistersTask(3064, Priority.LOW, //
						m(FeneconCommercialBattery.ChannelId.HEART_BEAT,
								new UnsignedDoublewordElement(3064).wordOrder(LSWMSW)), //
						new DummyRegisterElement(3066, 3097), //
						m(FeneconCommercialBattery.ChannelId.NUMBER_OF_MODULES_PER_TOWER,
								new UnsignedWordElement(3098)), //
						m(FeneconCommercialBattery.ChannelId.NUMBER_OF_CELLS_PER_MODULE, new UnsignedWordElement(3099),
								new ElementToChannelConverter(value -> {
									if (value == null) {
										return null;
									}
									Channel<Integer> numberOfModulesPerTowerChannel = this
											.channel(FeneconCommercialBattery.ChannelId.NUMBER_OF_MODULES_PER_TOWER);
									var numberOfModulesPerTower = numberOfModulesPerTowerChannel.value();
									if (!numberOfModulesPerTower.isDefined() || numberOfModulesPerTower.get() == 0) {
										return null;
									}
									return (int) value / numberOfModulesPerTower.get();
								}))), //

				new FC16WriteRegistersTask(17, //
						m(FeneconCommercialBattery.ChannelId.UNIT_ID, new UnsignedWordElement(17)), //
						m(FeneconCommercialBattery.ChannelId.UNIT_NUMBER, new UnsignedWordElement(18)), //
						new DummyRegisterElement(19), //
						m(FeneconCommercialBattery.ChannelId.SUBMASTER_MAP,
								new UnsignedDoublewordElement(20).wordOrder(LSWMSW)), //
						new DummyRegisterElement(22, 160), //
						m(FeneconCommercialBattery.ChannelId.BAUDRATE, new UnsignedWordElement(161))//
				), //

				new FC16WriteRegistersTask(3064, //
						m(FeneconCommercialBattery.ChannelId.HEART_BEAT,
								new UnsignedDoublewordElement(3064).wordOrder(LSWMSW)))//
		);
	}

	/**
	 * Initialize channels per towers and modules.
	 *
	 * @param numberOfTowers          the number of towers
	 * @param numberOfModulesPerTower the number of modules per tower
	 * @param numberOfCellsPerModule  the number of cells per module
	 * @throws OpenemsException on error
	 */
	private synchronized void initializeTowerModulesChannels(int numberOfTowers, int numberOfModulesPerTower,
			int numberOfCellsPerModule) throws OpenemsException {
		for (var tower = 0; tower < numberOfTowers; tower++) {
			final var towerNum = tower;
			/*
			 * Number Of Towers increased
			 */
			final var towerOffset = towerNum * 768 + 3072; /* Offset is 768, start address is 3072 */
			generateStatusChannels(this, towerNum);
			this.getModbusProtocol().addTasks(//
					// Cold Data, 2s frequency ==> Master Machine Summary Low Map 1.3
					new FC3ReadRegistersTask(towerOffset + 8, Priority.LOW, //
							m(generateTowerChannel(this, towerNum, "SUB_MASTER_HARDWARE_VERSION", OpenemsType.STRING,
									Unit.NONE), new StringWordElement(towerOffset + 8, 5), SERIAL_NUMBER_CONVERTER),
							m(generateTowerChannel(this, towerNum, "SUB_MASTER_FIRMWARE_VERSION", OpenemsType.STRING,
									Unit.NONE), new StringWordElement(towerOffset + 13, 2), SERIAL_NUMBER_CONVERTER)),
					// Sub-Master BMS RO, Hot Data, 250ms frequency ==> Master Machine Summary Fast
					// Map 1.2
					new FC3ReadRegistersTask(towerOffset, Priority.LOW, //
							m(generateTowerChannel(this, towerNum, "MAX_CELL_VOLTAGE", OpenemsType.INTEGER,
									Unit.MILLIVOLT), new UnsignedWordElement(towerOffset)), //
							m(generateTowerChannel(this, towerNum, "MIN_CELL_VOLTAGE", OpenemsType.INTEGER,
									Unit.MILLIVOLT), new UnsignedWordElement(towerOffset + 1)), //
							m(generateTowerChannel(this, towerNum, "MAX_CHARGE_POWER", OpenemsType.INTEGER, Unit.WATT),
									new SignedDoublewordElement(towerOffset + 2).wordOrder(LSWMSW)), //
							m(generateTowerChannel(this, towerNum, "MAX_DISCHARGE_POWER", OpenemsType.INTEGER,
									Unit.WATT), new SignedDoublewordElement(towerOffset + 4).wordOrder(LSWMSW)), //
							m(new UnsignedDoublewordElement(towerOffset + 6).wordOrder(LSWMSW)).build()
									.onUpdateCallback(value -> {
										final Boolean chargeReady;
										final Boolean dischargeReady;
										final Boolean mainRelaySwitchOnFlag;
										final Boolean slaveRelaySwitchOnFlag;
										final Integer currentPower;
										if (value == null) {
											chargeReady = null;
											dischargeReady = null;
											mainRelaySwitchOnFlag = null;
											slaveRelaySwitchOnFlag = null;
											currentPower = null;
										} else {
											chargeReady = (value & 0x1) == 1;
											dischargeReady = (value & 0x2) >> 1 == 1;
											mainRelaySwitchOnFlag = (value & 0x4) >> 2 == 1;
											slaveRelaySwitchOnFlag = (value & 0x8) >> 3 == 1;
											currentPower = (int) ((value & 0xffffff00) >> 8);
										}
										this.channel(toChannelIdString("TOWER_" + towerNum + "_CHARGE_READY"))
												.setNextValue(chargeReady);
										this.channel(toChannelIdString("TOWER_" + towerNum + "_DISCHARGE_READY"))
												.setNextValue(dischargeReady);
										this.channel(
												toChannelIdString("TOWER_" + towerNum + "_MAIN_RELAY_SWITCH_ON_FLAG"))
												.setNextValue(mainRelaySwitchOnFlag);
										this.channel(
												toChannelIdString("TOWER_" + towerNum + "_SLAVE_RELAY_SWITCH_ON_FLAG"))
												.setNextValue(slaveRelaySwitchOnFlag);
										this.channel(toChannelIdString("TOWER_" + towerNum + "_CURRENT_POWER"))
												.setNextValue(currentPower);
									}), //
							new DummyRegisterElement(towerOffset + 8, towerOffset + 13), //
							// Cold Data, 2s frequency ==> Master Machine Summary Low Map 1.3
							m(generateTowerChannel(this, towerNum, "VOLTAGE", OpenemsType.INTEGER, Unit.VOLT),
									new UnsignedWordElement(towerOffset + 14), SCALE_FACTOR_MINUS_1), //
							m(generateTowerChannel(this, towerNum, "CURRENT", OpenemsType.INTEGER, Unit.AMPERE),
									new SignedWordElement(towerOffset + 15), SCALE_FACTOR_MINUS_1), //
							m(generateTowerChannel(this, towerNum, "SOC", OpenemsType.INTEGER, Unit.PERCENT),
									new UnsignedWordElement(towerOffset + 16), SCALE_FACTOR_MINUS_2), //
							m(generateTowerChannel(this, towerNum, "SOH", OpenemsType.INTEGER, Unit.PERCENT),
									new UnsignedWordElement(towerOffset + 17)),
							m(generateTowerChannel(this, towerNum, "MAX_CELL_VOLTAGE_2", OpenemsType.INTEGER,
									Unit.MILLIVOLT), new UnsignedWordElement(towerOffset + 18)), //
							m(generateTowerChannel(this, towerNum, "MIN_CELL_VOLTAGE_2", OpenemsType.INTEGER,
									Unit.MILLIVOLT), new UnsignedWordElement(towerOffset + 19)), //
							m(generateTowerChannel(this, towerNum, "MAX_CELL_VOLTAGE_ID_2", OpenemsType.INTEGER,
									Unit.NONE), new UnsignedWordElement(towerOffset + 20)), //
							m(generateTowerChannel(this, towerNum, "MIN_CELL_VOLTAGE_ID_2", OpenemsType.INTEGER,
									Unit.NONE), new UnsignedWordElement(towerOffset + 21)), //
							m(generateTowerChannel(this, towerNum, "MAX_TEMPERATURE", OpenemsType.INTEGER,
									Unit.DEGREE_CELSIUS), new SignedWordElement(towerOffset + 22),
									SCALE_FACTOR_MINUS_2), //
							m(generateTowerChannel(this, towerNum, "MIN_TEMPERATURE", OpenemsType.INTEGER,
									Unit.DEGREE_CELSIUS), new SignedWordElement(towerOffset + 23),
									SCALE_FACTOR_MINUS_2), //
							m(new UnsignedWordElement(towerOffset + 24)).build().onUpdateCallback(new ByteElement(this, //
									ByteElement.Shifter.SEPERATE_TO_TWO_8_BIT_CHANNELS, //
									generateTowerChannel(this, towerNum, "MAX_TEMPERATURE_MODULE_ID",
											OpenemsType.INTEGER, Unit.NONE),
									generateTowerChannel(this, towerNum, "MIN_TEMPERATURE_MODULE_ID",
											OpenemsType.INTEGER, Unit.NONE))),
							m(generateTowerChannel(this, towerNum, "SLAVE_CELL_VOLTAGE_SENSOR_MAP", OpenemsType.INTEGER,
									Unit.NONE), new UnsignedWordElement(towerOffset + 25)), //
							new DummyRegisterElement(towerOffset + 26, towerOffset + 27), //
							m(generateTowerChannel(this, towerNum, "PRODUCT_SERIAL_NUMBER", OpenemsType.STRING,
									Unit.NONE), new StringWordElement(towerOffset + 28, 8), SERIAL_NUMBER_CONVERTER),
							m(generateTowerChannel(this, towerNum, "NOMINAL_CAPACITY", OpenemsType.LONG,
									Unit.AMPERE_HOURS), new UnsignedWordElement(towerOffset + 36),
									SCALE_FACTOR_MINUS_1), //
							m(generateTowerChannel(this, towerNum, "NOMINAL_VOLTAGE", OpenemsType.INTEGER, Unit.VOLT),
									new UnsignedWordElement(towerOffset + 37), SCALE_FACTOR_MINUS_1), //
							m(generateTowerChannel(this, towerNum, "NOMINAL_CHARGE_CURRENT", OpenemsType.INTEGER,
									Unit.AMPERE), new UnsignedWordElement(towerOffset + 38), SCALE_FACTOR_MINUS_1), //
							m(generateTowerChannel(this, towerNum, "NOMINAL_DISCHARGE_CURRENT", OpenemsType.INTEGER,
									Unit.AMPERE), new UnsignedWordElement(towerOffset + 39), SCALE_FACTOR_MINUS_1), //
							m(generateTowerChannel(this, towerNum, "NOMINAL_CHARGE_POWER", OpenemsType.INTEGER,
									Unit.KILOWATT), new UnsignedWordElement(towerOffset + 40)), //
							m(generateTowerChannel(this, towerNum, "NOMINAL_DISCHARGE_POWER", OpenemsType.INTEGER,
									Unit.KILOWATT), new UnsignedWordElement(towerOffset + 41)), //
							m(generateTowerChannel(this, towerNum, "CYCLES", OpenemsType.INTEGER, Unit.NONE),
									new UnsignedWordElement(towerOffset + 42)), //
							m(generateTowerChannel(this, towerNum, "REMAINING_CAPACITY", OpenemsType.LONG,
									Unit.AMPERE_HOURS), new UnsignedWordElement(towerOffset + 43),
									SCALE_FACTOR_MINUS_1), //
							m(generateTowerChannel(this, towerNum, "TOTAL_CHARGE_CAPACITY_AH", OpenemsType.LONG,
									Unit.AMPERE_HOURS), new SignedDoublewordElement(towerOffset + 44),
									SCALE_FACTOR_MINUS_1), //
							m(generateTowerChannel(this, towerNum, "TOTAL_DISCHARGE_CAPACITY_AH", OpenemsType.LONG,
									Unit.AMPERE_HOURS), new SignedDoublewordElement(towerOffset + 46),
									SCALE_FACTOR_MINUS_1)), //

					// 1.3.1 SysStateMessage
					new FC3ReadRegistersTask(towerOffset + 48, Priority.LOW, //
							m(new BitsWordElement(towerOffset + 48, this) //
									.bit(0, generateTowerChannel(this, towerNum, "STATUS", BOOLEAN, Unit.NONE)) //
									.bit(1, generateTowerChannel(this, towerNum, "24V_STATUS", BOOLEAN, Unit.NONE)) //
									.bit(2, generateTowerChannel(this, towerNum, "BATTERY_CHARGING_18650_STATUS",
											BOOLEAN, Unit.NONE)) //
									.bit(3, generateTowerChannel(this, towerNum, "MAIN_SWITCH_STATUS", BOOLEAN,
											Unit.NONE)) //
									.bit(9, generateTowerChannel(this, towerNum, "NO_24V_POWER_SUPPLY", BOOLEAN,
											Unit.NONE)) //
									.bit(10, generateTowerChannel(this, towerNum, "MAIN_SWITCH_OFF", BOOLEAN,
											Unit.NONE)) //
							), //
							new DummyRegisterElement(towerOffset + 49), //
							m(new BitsWordElement(towerOffset + 50, this) //
									.bit(0, generateTowerChannel(this, towerNum, "E2PROM_FAULT", Level.WARNING)) //
									.bit(1, generateTowerChannel(this, towerNum, "BATTERY_18650_FAULT", Level.WARNING)) //
									.bit(2, generateTowerChannel(this, towerNum, "RELAY_FAULT", Level.WARNING)) //
									.bit(3, generateTowerChannel(this, towerNum, "HALL_FAULT", Level.WARNING)) //
									.bit(4, generateTowerChannel(this, towerNum, "AD_SENSOR_FAULT", Level.WARNING)) //
									.bit(5, generateTowerChannel(this, towerNum, "SERIES_BATTERIES_FAULT",
											Level.WARNING)) //
									.bit(6, generateTowerChannel(this, towerNum, "COM_FAULT", Level.WARNING)) //
									.bit(7, generateTowerChannel(this, towerNum,
											"INSULATION_RESISTANCE_DETECTION_FAULT", Level.WARNING)) //
									.bit(8, generateTowerChannel(this, towerNum, "CELL_OVER_VOLTAGE_LEVEL_1", BOOLEAN,
											Unit.NONE)) //
									.bit(9, generateTowerChannel(this, towerNum, "CELL_LOW_VOLTAGE_LEVEL_1", BOOLEAN,
											Unit.NONE)) //
									.bit(10, generateTowerChannel(this, towerNum, "CELL_OVER_TEMPERATURE_LEVEL_1",
											BOOLEAN, Unit.NONE)) //
									.bit(11, generateTowerChannel(this, towerNum, "CELL_LOW_TEMPERATURE_LEVEL_1",
											BOOLEAN, Unit.NONE)) //
									.bit(12, generateTowerChannel(this, towerNum, "OVER_CURRENT_LEVEL_1", BOOLEAN,
											Unit.NONE)) //
									.bit(13, generateTowerChannel(this, towerNum, "OVER_SOC_LEVEL_1", BOOLEAN,
											Unit.NONE)) //
									.bit(14, generateTowerChannel(this, towerNum, "LOW_SOC_LEVEL_1", BOOLEAN,
											Unit.NONE)) //
									.bit(15, generateTowerChannel(this, towerNum, "CELL_OVER_VOLTAGE_LEVEL_2", BOOLEAN,
											Unit.NONE))), //
							m(new BitsWordElement(towerOffset + 51, this) //
									.bit(0, generateTowerChannel(this, towerNum, "CELL_LOW_VOLTAGE_LEVEL_2", BOOLEAN,
											Unit.NONE)) //
									.bit(1, generateTowerChannel(this, towerNum, "CELL_OVER_TEMPERATURE_LEVEL_2",
											BOOLEAN, Unit.NONE)) //
									.bit(2, generateTowerChannel(this, towerNum, "CELL_LOW_TEMPERATURE_LEVEL_2",
											BOOLEAN, Unit.NONE)) //
									.bit(3, generateTowerChannel(this, towerNum, "OVER_CURRENT_LEVEL_2", BOOLEAN,
											Unit.NONE)) //
									.bit(4, generateTowerChannel(this, towerNum, "OVER_SOC_LEVEL_2", BOOLEAN,
											Unit.NONE)) //
									.bit(5, generateTowerChannel(this, towerNum, "LOW_SOC_LEVEL_2", BOOLEAN, Unit.NONE)) //
									.bit(7, generateTowerChannel(this, towerNum, "CELL_OVER_VOLTAGE_LEVEL_3",
											Level.WARNING)) //
									.bit(8, generateTowerChannel(this, towerNum, "CELL_LOW_VOLTAGE_LEVEL_3", BOOLEAN,
											Unit.NONE)) //
									.bit(9, generateTowerChannel(this, towerNum, "CELL_OVER_TEMPERATURE_LEVEL_3",
											Level.WARNING)) //
									.bit(10, generateTowerChannel(this, towerNum, "CELL_LOW_TEMPERATURE_LEVEL_3",
											Level.WARNING)) //
									.bit(11, generateTowerChannel(this, towerNum, "OVER_CURRENT_LEVEL_3",
											Level.WARNING)) //
									.bit(12, generateTowerChannel(this, towerNum, "OVER_SOC_LEVEL_3", Level.WARNING)) //
									.bit(13, generateTowerChannel(this, towerNum, "LOW_SOC_LEVEL_3", Level.WARNING))), //
							m(generateTowerChannel(this, towerNum, "CURRENT_SCALE_5", OpenemsType.LONG,
									Unit.MICROAMPERE), new UnsignedDoublewordElement(towerOffset + 52), SCALE_FACTOR_1)
									.wordOrder(LSWMSW), //
							m(generateTowerChannel(this, towerNum, "CURRENT_VALUES_AT_DIFFERENT_C_RATE_1",
									OpenemsType.INTEGER, Unit.MICROAMPERE),
									new SignedDoublewordElement(towerOffset + 54), SCALE_FACTOR_1).wordOrder(LSWMSW), //
							m(generateTowerChannel(this, towerNum, "CURRENT_VALUES_AT_DIFFERENT_C_RATE_2",
									OpenemsType.LONG, Unit.MICROAMPERE), new SignedDoublewordElement(towerOffset + 56),
									SCALE_FACTOR_1).wordOrder(LSWMSW), //
							m(generateTowerChannel(this, towerNum, "CURRENT_VALUES_AT_DIFFERENT_C_RATE_3",
									OpenemsType.LONG, Unit.MICROAMPERE), new SignedDoublewordElement(towerOffset + 58),
									SCALE_FACTOR_1).wordOrder(LSWMSW), //
							m(generateTowerChannel(this, towerNum, "CURRENT_VALUES_AT_DIFFERENT_C_RATE_4",
									OpenemsType.LONG, Unit.MICROAMPERE), new SignedDoublewordElement(towerOffset + 60),
									SCALE_FACTOR_1).wordOrder(LSWMSW), //
							m(generateTowerChannel(this, towerNum, "CURRENT_VALUES_AT_DIFFERENT_C_RATE_5",
									OpenemsType.LONG, Unit.MICROAMPERE), new SignedDoublewordElement(towerOffset + 62),
									SCALE_FACTOR_1).wordOrder(LSWMSW), //
							m(generateTowerChannel(this, towerNum, "CURRENT_VALUES_AT_DIFFERENT_C_RATE_6",
									OpenemsType.LONG, Unit.MICROAMPERE), new SignedDoublewordElement(towerOffset + 64),
									SCALE_FACTOR_1).wordOrder(LSWMSW), //
							m(generateTowerChannel(this, towerNum, "CURRENT_VALUES_AT_DIFFERENT_C_RATE_7",
									OpenemsType.LONG, Unit.MICROAMPERE), new SignedDoublewordElement(towerOffset + 66),
									SCALE_FACTOR_1).wordOrder(LSWMSW), //
							m(generateTowerChannel(this, towerNum, "CURRENT_VALUES_AT_DIFFERENT_C_RATE_8",
									OpenemsType.LONG, Unit.MICROAMPERE), new SignedDoublewordElement(towerOffset + 68),
									SCALE_FACTOR_1).wordOrder(LSWMSW), //
							m(new UnsignedWordElement(towerOffset + 70)).build().onUpdateCallback(new ByteElement(this, //
									ByteElement.Shifter.ONLY_FIRST_CHANNEL, //
									generateTowerChannel(
											this, towerNum, "VALID_CURRENT_VALUE_ID", OpenemsType.INTEGER, Unit.NONE))),
							new DummyRegisterElement(towerOffset + 71), //
							m(generateTowerChannel(this, towerNum, "CURRENT_RACK_CAPACITY", OpenemsType.LONG,
									Unit.NONE), new SignedDoublewordElement(towerOffset + 72)), //
							m(generateTowerChannel(this, towerNum, "RACK_VOLTAGE_SCALE_5", OpenemsType.INTEGER,
									Unit.MILLIVOLT), new SignedDoublewordElement(towerOffset + 74),
									SCALE_FACTOR_MINUS_2), //
							m(generateTowerChannel(this, towerNum, "INSULATION_RESISTANCE_CONTROL", OpenemsType.INTEGER,
									Unit.OHM), new UnsignedDoublewordElement(towerOffset + 76)), //
							m(generateTowerChannel(this, towerNum, "INSULATION_RESISTANCE_POSITIVE_POLE",
									OpenemsType.INTEGER, Unit.OHM), new SignedDoublewordElement(towerOffset + 78)), //
							m(generateTowerChannel(this, towerNum, "INSULATION_RESISTANCE_NEGATIVE_POLE",
									OpenemsType.INTEGER, Unit.OHM), new SignedDoublewordElement(towerOffset + 80)), //
							m(generateTowerChannel(this, towerNum, "SENSOR_OUTPUT_VOLTAGE", OpenemsType.LONG,
									Unit.MICROVOLT), new SignedDoublewordElement(towerOffset + 82),
									SCALE_FACTOR_MINUS_2), //
							m(generateTowerChannel(this, towerNum, "TOTAL_CHARGE_CAPACITY_WH", OpenemsType.LONG,
									Unit.WATT_HOURS),
									new SignedQuadruplewordElement(towerOffset + 84).wordOrder(LSWMSW)), //
							m(generateTowerChannel(this, towerNum, "TOTAL_DISCHARGE_CAPACITY_WH", OpenemsType.LONG,
									Unit.WATT_HOURS),
									new SignedQuadruplewordElement(towerOffset + 88).wordOrder(LSWMSW)), //
							m(generateTowerChannel(this, towerNum, "SLAVE_MAP", OpenemsType.LONG, Unit.NONE),
									new UnsignedDoublewordElement(towerOffset + 92)), //
							m(generateTowerChannel(this, towerNum, "DATA_COMMUNICATION_COUNTER", OpenemsType.INTEGER,
									Unit.NONE), new UnsignedWordElement(towerOffset + 94)), //
							new DummyRegisterElement(towerOffset + 95), //
							m(generateTowerChannel(this, towerNum, "OUTSIDE_TEMPERATURE_0", OpenemsType.INTEGER,
									Unit.DEGREE_CELSIUS), new SignedWordElement(towerOffset + 96),
									SCALE_FACTOR_MINUS_2), //
							m(generateTowerChannel(this, towerNum, "OUTSIDE_TEMPERATURE_1", OpenemsType.INTEGER,
									Unit.DEGREE_CELSIUS), new SignedWordElement(towerOffset + 97),
									SCALE_FACTOR_MINUS_2), //
							m(generateTowerChannel(this, towerNum, "PCB_TEMPERATURE", OpenemsType.INTEGER,
									Unit.DEGREE_CELSIUS), new SignedWordElement(towerOffset + 98),
									SCALE_FACTOR_MINUS_2), //
							new DummyRegisterElement(towerOffset + 99, towerOffset + 111), //
							m(generateTowerChannel(this, towerNum, "CURRENT_WITHIN_40_MS", OpenemsType.LONG,
									Unit.MICROAMPERE), new SignedDoublewordElement(towerOffset + 112), SCALE_FACTOR_1), //
							m(generateTowerChannel(this, towerNum, "CURRENT_WITHIN_250_MS", OpenemsType.LONG,
									Unit.MICROAMPERE), new SignedDoublewordElement(towerOffset + 114),
									SCALE_FACTOR_1))); //
			for (var i = 0; i < numberOfModulesPerTower; i++) {
				final var module = i;
				generateCellTemperatureChannels(this, towerNum, module);
				generateCellVoltageChannels(this, towerNum, module, numberOfCellsPerModule);
				this.getModbusProtocol().addTask(//
						new FC3ReadRegistersTask(towerOffset + 128 //
								+ module * 20 /* Start address towerOffset0 +128 =3200 */, Priority.LOW, //
								m(new UnsignedDoublewordElement(towerOffset + 128 + module * 20).wordOrder(LSWMSW))
										.build().onUpdateCallback(value -> {
											final Integer cellVoltage0;
											final Integer cellVoltage1;
											final Integer cellTemperature0;
											if (value == null) {
												cellVoltage0 = null;
												cellVoltage1 = null;
												cellTemperature0 = null;
											} else {
												cellVoltage0 = (int) (value & 0xfff);
												cellVoltage1 = (int) ((value & 0xfff000) >> 12);
												cellTemperature0 = (int) (value & 0xff000000) >> 24;
											}
											this.channel(toChannelIdString(
													"TOWER_" + towerNum + "_MODULE_" + module + "_CELL_000_VOLTAGE"))
													.setNextValue(cellVoltage0);
											this.channel(toChannelIdString(
													"TOWER_" + towerNum + "_MODULE_" + module + "_CELL_001_VOLTAGE"))
													.setNextValue(cellVoltage1);
											this.channel(toChannelIdString("TOWER_" + towerNum + "_MODULE_" + module
													+ "_CELL_000_TEMPERATURE")).setNextValue(cellTemperature0);
										}), //
								m(new UnsignedDoublewordElement(towerOffset + 128 + module * 20 + 2).wordOrder(LSWMSW))
										.build().onUpdateCallback(value -> {
											final Integer cellVoltage2;
											final Integer cellVoltage3;
											final Integer cellTemperature1;
											if (value == null) {
												cellVoltage2 = null;
												cellVoltage3 = null;
												cellTemperature1 = null;

											} else {
												cellVoltage2 = (int) (value & 0xfff);
												cellVoltage3 = (int) ((value & 0xfff000) >> 12);
												cellTemperature1 = (int) (value & 0xff000000) >> 24;
											}
											this.channel(toChannelIdString(
													"TOWER_" + towerNum + "_MODULE_" + module + "_CELL_002_VOLTAGE"))
													.setNextValue(cellVoltage2);
											this.channel(toChannelIdString(
													"TOWER_" + towerNum + "_MODULE_" + module + "_CELL_003_VOLTAGE"))
													.setNextValue(cellVoltage3);
											this.channel(toChannelIdString("TOWER_" + towerNum + "_MODULE_" + module
													+ "_CELL_001_TEMPERATURE")).setNextValue(cellTemperature1);
										}), //
								m(new UnsignedDoublewordElement(towerOffset + 128 + module * 20 + 4).wordOrder(LSWMSW))
										.build().onUpdateCallback(value -> {
											final Integer cellVoltage4;
											final Integer cellVoltage5;
											final Integer cellTemperature2;
											if (value == null) {
												cellVoltage4 = null;
												cellVoltage5 = null;
												cellTemperature2 = null;

											} else {
												cellVoltage4 = (int) (value & 0xfff);
												cellVoltage5 = (int) ((value & 0xfff000) >> 12);
												cellTemperature2 = (int) (value & 0xff000000) >> 24;
											}
											this.channel(toChannelIdString(
													"TOWER_" + towerNum + "_MODULE_" + module + "_CELL_004_VOLTAGE"))
													.setNextValue(cellVoltage4);
											this.channel(toChannelIdString(
													"TOWER_" + towerNum + "_MODULE_" + module + "_CELL_005_VOLTAGE"))
													.setNextValue(cellVoltage5);
											this.channel(toChannelIdString("TOWER_" + towerNum + "_MODULE_" + module
													+ "_CELL_002_TEMPERATURE")).setNextValue(cellTemperature2);
										}), //
								m(new UnsignedDoublewordElement(towerOffset + 128 + module * 20 + 6).wordOrder(LSWMSW))
										.build().onUpdateCallback(value -> {
											final Integer cellVoltage6;
											final Integer cellVoltage7;
											final Integer cellTemperature3;
											if (value == null) {
												cellVoltage6 = null;
												cellVoltage7 = null;
												cellTemperature3 = null;

											} else {
												cellVoltage6 = (int) (value & 0xfff);
												cellVoltage7 = (int) ((value & 0xfff000) >> 12);
												cellTemperature3 = (int) (value & 0xff000000) >> 24;
											}
											this.channel(toChannelIdString(
													"TOWER_" + towerNum + "_MODULE_" + module + "_CELL_006_VOLTAGE"))
													.setNextValue(cellVoltage6);
											this.channel(toChannelIdString(
													"TOWER_" + towerNum + "_MODULE_" + module + "_CELL_007_VOLTAGE"))
													.setNextValue(cellVoltage7);
											this.channel(toChannelIdString("TOWER_" + towerNum + "_MODULE_" + module
													+ "_CELL_003_TEMPERATURE")).setNextValue(cellTemperature3);
										}), //
								m(new UnsignedDoublewordElement(towerOffset + 128 + module * 20 + 8).wordOrder(LSWMSW))
										.build().onUpdateCallback(value -> {
											final Integer cellVoltage8;
											final Integer cellVoltage9;
											final Integer cellTemperature4;
											if (value == null) {
												cellVoltage8 = null;
												cellVoltage9 = null;
												cellTemperature4 = null;

											} else {
												cellVoltage8 = (int) (value & 0xfff);
												cellVoltage9 = (int) ((value & 0xfff000) >> 12);
												cellTemperature4 = (int) (value & 0xff000000) >> 24;
											}
											this.channel(toChannelIdString(
													"TOWER_" + towerNum + "_MODULE_" + module + "_CELL_008_VOLTAGE"))
													.setNextValue(cellVoltage8);
											this.channel(toChannelIdString(
													"TOWER_" + towerNum + "_MODULE_" + module + "_CELL_009_VOLTAGE"))
													.setNextValue(cellVoltage9);
											this.channel(toChannelIdString("TOWER_" + towerNum + "_MODULE_" + module
													+ "_CELL_004_TEMPERATURE")).setNextValue(cellTemperature4);
										}), //
								m(new UnsignedDoublewordElement(towerOffset + 128 + module * 20 + 10).wordOrder(LSWMSW))
										.build().onUpdateCallback(value -> {
											final Integer cellVoltage10;
											final Integer cellVoltage11;
											final Integer cellTemperature5;
											if (value == null) {
												cellVoltage10 = null;
												cellVoltage11 = null;
												cellTemperature5 = null;
											} else {
												cellVoltage10 = (int) (value & 0xfff);
												cellVoltage11 = (int) ((value & 0xfff000) >> 12);
												cellTemperature5 = (int) (value & 0xff000000) >> 24;
											}
											this.channel(toChannelIdString(
													"TOWER_" + towerNum + "_MODULE_" + module + "_CELL_010_VOLTAGE"))
													.setNextValue(cellVoltage10);
											this.channel(toChannelIdString(
													"TOWER_" + towerNum + "_MODULE_" + module + "_CELL_011_VOLTAGE"))
													.setNextValue(cellVoltage11);
											this.channel(toChannelIdString("TOWER_" + towerNum + "_MODULE_" + module
													+ "_CELL_005_TEMPERATURE")).setNextValue(cellTemperature5);
										}), //
								m(new UnsignedDoublewordElement(towerOffset + 128 + module * 20 + 12).wordOrder(LSWMSW))
										.build().onUpdateCallback(value -> {
											final Integer cellTemperature6;
											if (value == null) {
												cellTemperature6 = null;
											} else {
												cellTemperature6 = (int) (value & 0xff000000) >> 24;
											}
											this.channel(toChannelIdString("TOWER_" + towerNum + "_MODULE_" + module
													+ "_CELL_006_TEMPERATURE")).setNextValue(cellTemperature6);
										}), //
								m(new UnsignedDoublewordElement(towerOffset + 128 + module * 20 + 14).wordOrder(LSWMSW))
										.build().onUpdateCallback(value -> {
											final Integer cellTemperature7;
											if (value == null) {
												cellTemperature7 = null;
											} else {
												cellTemperature7 = (int) (value & 0xff000000) >> 24;
											}
											this.channel(toChannelIdString("TOWER_" + towerNum + "_MODULE_" + module
													+ "_CELL_007_TEMPERATURE")).setNextValue(cellTemperature7);
										}), //
								m(generateTowerChannel(this, towerNum,
										getSingleModulePrefix(module) + "_BALANCING_FLAG", BOOLEAN, Unit.NONE),
										new UnsignedWordElement(towerOffset + 128 + module * 20 + 16)), //
								m(new UnsignedWordElement(towerOffset + 128 + module * 20
										+ 17/*
											 * Start Address is 3200 and the tower offset: 768 and the module offset: 12
											 */)).build().onUpdateCallback(new ByteElement(this, //
												ByteElement.Shifter.ONLY_FIRST_CHANNEL, //
												generateTowerChannel(this, towerNum,
														getSingleModulePrefix(module) + "_MODULE_STATUS",
														OpenemsType.INTEGER, Unit.NONE)))));
			}
		}
	}

	/**
	 * It generates dynamic voltage channels according to certain prefixes from 0 to
	 * specified numbers for each tower module and cell number. e.g
	 * TOWER_0_MODULE_0_CELL_000_VOLTAGE
	 *
	 * @param parent the parent component
	 * @param tower  number of towers.
	 * @param module number of modules per tower.
	 * @param cell   number of cells per module.
	 */
	private static void generateCellVoltageChannels(FeneconCommercialBatteryImpl parent, int tower, int module,
			int cell) {
		for (var c = 0; c < cell; c++) {
			generateTowerChannel(parent, tower, getSingleModulePrefix(module) + getSingleCellPrefix(c) + "_VOLTAGE",
					OpenemsType.INTEGER, Unit.MILLIVOLT);
		}
	}

	/**
	 * It generates dynamic voltage channels according to certain prefixes from 0 to
	 * specified numbers for each tower module and cell number. e.g
	 * TOWER_0_MODULE_0_CELL_000_TEMPERATURE
	 *
	 * @param parent the parent component
	 * @param tower  tower number of towers
	 * @param module module number of modules per tower.
	 */
	private static void generateCellTemperatureChannels(FeneconCommercialBatteryImpl parent, int tower, int module) {
		for (var t = 0; t < NUMBER_OF_TEMPERATURE_CELLS_PER_MODULE; t++) {
			generateTowerChannel(parent, tower, getSingleModulePrefix(module) + getSingleCellPrefix(t) + "_TEMPERATURE",
					OpenemsType.INTEGER, Unit.DEGREE_CELSIUS);
		}
	}

	/**
	 * Generates the tower status channels.
	 *
	 * @param parent the parent component
	 * @param tower  number of towers.
	 */
	private static void generateStatusChannels(FeneconCommercialBatteryImpl parent, int tower) {
		generateTowerChannel(parent, tower, "CHARGE_READY", BOOLEAN, Unit.NONE);
		generateTowerChannel(parent, tower, "DISCHARGE_READY", BOOLEAN, Unit.NONE);
		generateTowerChannel(parent, tower, "MAIN_RELAY_SWITCH_ON_FLAG", BOOLEAN, Unit.NONE);
		generateTowerChannel(parent, tower, "SLAVE_RELAY_SWITCH_ON_FLAG", BOOLEAN, Unit.NONE);
		generateTowerChannel(parent, tower, "CURRENT_POWER", OpenemsType.INTEGER, Unit.WATT);
	}

	/**
	 * Creates a Channel-ID String from the Enum and returns it.
	 *
	 * @param channelId the {@link channelId}
	 * @return the ChannelId as camel-case String
	 */
	private static String toChannelIdString(String channelId) {
		return io.openems.edge.common.channel.ChannelId.channelIdUpperToCamel(channelId);
	}

	/**
	 * Generates cell prefix for Channel-IDs for Cell Temperature and Voltage
	 * channels.
	 *
	 * <p>
	 * "%03d" creates string number with leading zeros
	 *
	 * @param cell number of the cell
	 * @return a prefix e.g. "_CELL_003"
	 */
	private static String getSingleCellPrefix(int cell) {
		return "_CELL_" + String.format("%03d", cell);
	}

	/**
	 * Generates module prefix for Channel-IDs for Cell Temperature and Voltage
	 * channels.
	 *
	 * @param module number of the Module
	 * @return a prefix e.g. "MODULE_2"
	 */
	private static String getSingleModulePrefix(int module) {
		return "MODULE_" + module;
	}

	/**
	 * Generates a tower channel with a specific channelIdSuffix,openemsType and
	 * channelUnit.
	 *
	 * @param parent          the parent component
	 * @param tower           number of the Tower
	 * @param channelIdSuffix e.g. "STATUS_ALARM"
	 * @param openemsType     specified type e.g. "INTEGER"
	 * @param channelUnit     specified type e.g. "NONE"
	 * @return a channel with Channel-ID "TOWER_1_STATUS_ALARM"
	 */
	private static io.openems.edge.common.channel.ChannelId generateTowerChannel(FeneconCommercialBatteryImpl parent,
			int tower, String channelIdSuffix, OpenemsType openemsType, Unit channelUnit) {
		io.openems.edge.common.channel.ChannelId channelId = new DynamicChannelId(
				"TOWER_" + tower + "_" + channelIdSuffix, Doc.of(openemsType).unit(channelUnit));
		parent.addChannel(channelId);
		return channelId;
	}

	/**
	 * Generates a Channel-ID for channels that are specific to a tower.
	 *
	 * @param parent          the parent component
	 * @param tower           number of the Tower
	 * @param channelIdSuffix e.g. "STATUS_ALARM"
	 * @param level           specified level e.g. "INFO"
	 * @return a channel with Channel-ID "TOWER_1_STATUS_ALARM"
	 */
	private static io.openems.edge.common.channel.ChannelId generateTowerChannel(FeneconCommercialBatteryImpl parent,
			int tower, String channelIdSuffix, Level level) {
		io.openems.edge.common.channel.ChannelId channelId = new DynamicChannelId(
				"TOWER_" + tower + "_" + channelIdSuffix, Doc.of(level));
		parent.addChannel(channelId);
		return channelId;
	}

	/**
	 * Update Number of towers,modules and cells; called on onChange event.
	 * 
	 * <p>
	 * Recalculate the number of towers, modules and cells. Unfortunately the
	 * battery may report too small wrong values in the beginning, so we need to
	 * recalculate on every change.
	 */
	protected synchronized void updateNumberOfTowersAndModulesAndCells() {
		Channel<Integer> numberOfModulesPerTowerChannel = this
				.channel(FeneconCommercialBattery.ChannelId.NUMBER_OF_MODULES_PER_TOWER);
		var numberOfModulesPerTowerOpt = numberOfModulesPerTowerChannel.value();

		Channel<Integer> numberOfTowersChannel = this.channel(FeneconCommercialBattery.ChannelId.NUMBER_OF_TOWERS);
		var numberOfTowersOpt = numberOfTowersChannel.value();

		Channel<Integer> numberOfCellsPerModuleChannel = this
				.channel(FeneconCommercialBattery.ChannelId.NUMBER_OF_CELLS_PER_MODULE);
		var numberOfCellsPerModuleOpt = numberOfCellsPerModuleChannel.value();

		// Were all required registers read?
		if (!numberOfModulesPerTowerOpt.isDefined() || !numberOfTowersOpt.isDefined()
				|| !numberOfCellsPerModuleOpt.isDefined()) {
			return;
		}

		// At the beginning values can be read as 0
		if (numberOfCellsPerModuleOpt.get() == 0 || numberOfModulesPerTowerOpt.get() == 0
				|| numberOfTowersOpt.get() == 0) {
			return;
		}
		int numberOfTowers = numberOfTowersOpt.get();
		int numberOfModulesPerTower = numberOfModulesPerTowerOpt.get();
		int numberOfCellsPerModule = numberOfCellsPerModuleOpt.get();
		// Initialize available Tower- and Module-Channels dynamically.
		try {
			this.initializeTowerModulesChannels(numberOfTowers, numberOfModulesPerTower, numberOfCellsPerModule);
			this.calculateCapacity(numberOfTowers, numberOfModulesPerTower);
		} catch (OpenemsException e) {
			this.logError(this.log, "Unable to initialize tower modules channels: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * In case having DC Cluster system, Sub Master Map and Unit Number should be
	 * updated.
	 */
	private void initializeTowerSettings() {
		IntegerReadChannel numberOfTowersChannel = this.channel(FeneconCommercialBattery.ChannelId.NUMBER_OF_TOWERS);
		var numberOfTowers = numberOfTowersChannel.value();
		if (!numberOfTowers.isDefined()) {
			return;
		}
		var unitNumber = DEFAULT_UNIT_NUMBER + numberOfTowers.get();
		var submasterMap = (int) (Math.pow(2, numberOfTowers.get()) - 1);
		this.updateIfNotEqual(FeneconCommercialBattery.ChannelId.UNIT_NUMBER, unitNumber);
		this.updateIfNotEqual(FeneconCommercialBattery.ChannelId.SUBMASTER_MAP, submasterMap);
	}

	/**
	 * Updates the Channel if its current value is not equal to the new value.
	 *
	 * @param channelId Sinexcel Channel-Id
	 * @param newValue  Integer value.
	 * @throws IllegalArgumentException on error
	 */
	private void updateIfNotEqual(FeneconCommercialBattery.ChannelId channelId, Integer newValue)
			throws IllegalArgumentException {
		WriteChannel<Integer> channel = this.channel(channelId);
		var currentValue = channel.value();
		if (currentValue.isDefined() && !Objects.equals(currentValue.get(), newValue)) {
			try {
				channel.setNextWriteValue(newValue);
			} catch (OpenemsNamedException e) {
				this.logWarn(this.log, "Unable to update Channel [" + channel.address() + "] from [" + currentValue
						+ "] to [" + newValue + "]");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Calculates the Capacity as Capacity per module multiplied with number of
	 * modules and sets the CAPACITY channel.
	 *
	 * @param numberOfTowers  the number of battery towers
	 * @param numberOfModules the number of battery modules
	 */
	private void calculateCapacity(int numberOfTowers, int numberOfModules) {
		var capacity = numberOfTowers * numberOfModules * MODULE_3_5_KWH;
		this._setCapacity(capacity);
	}

	/**
	 * SoC to be set maximum(100) or minimum(0) based on discharge and charge
	 * current of the battery.
	 */
	protected synchronized void updateSoc() {
		Channel<Integer> batterySocChannel = this.channel(FeneconCommercialBattery.ChannelId.BATTERY_SOC);
		var batterySoc = batterySocChannel.value();
		var batteryChargeMaxCurrent = this.getChargeMaxCurrent();
		var batteryDischargeMaxCurrent = this.getDischargeMaxCurrent();
		final Integer soc;
		if (batterySoc.isDefined()) {
			if (batteryDischargeMaxCurrent.isDefined() //
					&& batterySoc.get() <= 4 //
					&& batteryDischargeMaxCurrent.get() <= 0) {
				// Make soc to 0 if it is less than 5 %
				soc = 0;

			} else if (batteryChargeMaxCurrent.isDefined() //
					&& batterySoc.get() >= 98 //
					&& batteryChargeMaxCurrent.get() <= 0) {
				// Make soc to 100 if it is more than 97 %
				soc = 100;

			} else {
				// Apply the normal Soc if it not in the above ranges.
				soc = batterySoc.get();
			}

		} else {
			// Original Battery-SoC is undefined
			soc = null;
		}
		this._setSoc(soc);
	}

	@Override
	public String debugLog() {
		return new StringBuilder() //
				.append("SoC:").append(this.getSoc()) //
				.append("|Discharge:").append(this.getDischargeMinVoltage()) //
				.append(";").append(this.getDischargeMaxCurrent()) //
				.append("|Charge:") //
				.append(this.getChargeMaxVoltage()) //
				.append(";").append(this.getChargeMaxCurrent()) //
				.append("|State:").append(this.stateMachine.getCurrentState()) //
				.toString();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				Battery.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(FeneconCommercialBattery.class, accessMode, 100) //
						.build());
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
			this.setBatteryCurrentLimits();
			this.initializeTowerSettings();
			break;
		}
	}

	/**
	 * TODO, to be updated. Update the BATTERY_CHARGE_MAX_CURRENT accordingly, and
	 * trigger updateSoc onChange.
	 */
	private void setBatteryCurrentLimits() {
		var chargeMaxCurrent = this.getChargeMaxCurrent();
		this.channel(FeneconCommercialBattery.ChannelId.BATTERY_CHARGE_MAX_CURRENT).setNextValue(chargeMaxCurrent);

		var dischargeMaxCurrent = this.getDischargeMaxCurrent();
		this.channel(FeneconCommercialBattery.ChannelId.BATTERY_DISCHARGE_MAX_CURRENT)
				.setNextValue(dischargeMaxCurrent);
	}

	/**
	 * Handles the State-Machine.
	 */
	private void handleStateMachine() {
		// Store the current State
		this.channel(FeneconCommercialBattery.ChannelId.STATE_MACHINE)
				.setNextValue(this.stateMachine.getCurrentState());

		// Initialize 'Start-Stop' Channel
		this._setStartStop(StartStop.UNDEFINED);

		if (this.startStopTarget.get() == StartStop.START) {
			try {
				LongWriteChannel heartbeatChannel = this.channel(FeneconCommercialBattery.ChannelId.HEART_BEAT);
				heartbeatChannel.setNextWriteValue(DEFAULT_HEART_BEAT);
			} catch (IllegalArgumentException | OpenemsNamedException e1) {
				this.logError(this.log, "Setting HeartBeat failed: " + e1.getMessage());
				e1.printStackTrace();
			}
		}

		// Prepare Context
		BooleanWriteChannel batteryStartStopRelayChannel = null;
		try {
			batteryStartStopRelayChannel = this.componentManager
					.getChannel(ChannelAddress.fromString(this.config.batteryStartStopRelay()));
		} catch (IllegalArgumentException | OpenemsNamedException e1) {
			this.logError(this.log, //
					"Setting BatteryStartStopRelay [" + this.config.batteryStartStopRelay() + "] failed: "
							+ e1.getMessage());
			e1.printStackTrace();
		}
		// Prepare Context
		var context = new Context(this, batteryStartStopRelayChannel);

		// Call the StateMachine
		try {
			this.stateMachine.run(context);

			this.channel(FeneconCommercialBattery.ChannelId.RUN_FAILED).setNextValue(false);

		} catch (OpenemsNamedException e) {
			this.channel(FeneconCommercialBattery.ChannelId.RUN_FAILED).setNextValue(true);
			this.logError(this.log, "StateMachine failed: " + e.getMessage());
		}
	}

	private final AtomicReference<StartStop> startStopTarget = new AtomicReference<>(StartStop.UNDEFINED);

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
