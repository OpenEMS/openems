package io.openems.edge.battery.bydcommercial;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.DIRECT_1_TO_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1;

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
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.bydcommercial.statemachine.Context;
import io.openems.edge.battery.bydcommercial.statemachine.StateMachine;
import io.openems.edge.battery.bydcommercial.statemachine.StateMachine.State;
import io.openems.edge.battery.protection.BatteryProtection;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.ModbusUtils;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
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
		name = "Byd.BatteryBox.Commercial.C130", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class BatteryBoxC130Impl extends AbstractOpenemsModbusComponent implements BatteryBoxC130, Battery,
		ModbusComponent, OpenemsComponent, EventHandler, ModbusSlave, StartStoppable {

	private static final float CAPACITY_PER_MODULE = 6.9f;
	private static final int MIN_ALLOWED_VOLTAGE_PER_MODULE = 34;
	private static final int MAX_ALLOWED_VOLTAGE_PER_MODULE = 42;

	private static final int OLD_VERSION_DEFAULT_CHARGE_MAX_VOLTAGE = 820;
	private static final int OLD_VERSION_DEFAULT_DISCHARGE_MIN_VOLTAGE = 638;

	private final Logger log = LoggerFactory.getLogger(BatteryBoxC130Impl.class);
	private final StateMachine stateMachine = new StateMachine(State.UNDEFINED);

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	private Config config;
	private BatteryProtection batteryProtection = null;

	public BatteryBoxC130Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				BatteryBoxC130.ChannelId.values(), //
				BatteryProtection.ChannelId.values() //
		);
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}

		// Initialize Battery-Protection
		this.batteryProtection = BatteryProtection.create(this) //
				.applyBatteryProtectionDefinition(new BatteryProtectionDefinitionBydC130(), this.componentManager) //
				.build();

		var maxVoltage = this.config.numberOfSlaves() * MAX_ALLOWED_VOLTAGE_PER_MODULE;
		this._setChargeMaxVoltage(maxVoltage);

		var minVoltage = this.config.numberOfSlaves() * MIN_ALLOWED_VOLTAGE_PER_MODULE;
		this._setDischargeMinVoltage(minVoltage);

		var capacity = (int) (this.config.numberOfSlaves() * CAPACITY_PER_MODULE);
		this._setCapacity(capacity);
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

	private void handleStateMachine() {
		// Store the current State
		this.channel(BatteryBoxC130.ChannelId.STATE_MACHINE).setNextValue(this.stateMachine.getCurrentState());

		// Initialize 'Start-Stop' Channel
		this._setStartStop(StartStop.UNDEFINED);

		// Prepare Context
		var context = new Context(this, this.config);

		// Call the StateMachine
		try {
			this.stateMachine.run(context);

			this.channel(BatteryBoxC130.ChannelId.RUN_FAILED).setNextValue(false);

		} catch (OpenemsNamedException e) {
			this.channel(BatteryBoxC130.ChannelId.RUN_FAILED).setNextValue(true);
			this.logError(this.log, "StateMachine failed: " + e.getMessage());
		}
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc() //
				+ "|Discharge:" + this.getDischargeMinVoltage() + ";" + this.getDischargeMaxCurrent() //
				+ "|Charge:" + this.getChargeMaxVoltage() + ";" + this.getChargeMaxCurrent() //
				+ "|State:" + this.stateMachine.getCurrentState();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(0x2010, Priority.HIGH, //
						m(BatteryBoxC130.ChannelId.POWER_CIRCUIT_CONTROL, new UnsignedWordElement(0x2010)) //
				), //
				new FC6WriteRegisterTask(0x2010, //
						m(BatteryBoxC130.ChannelId.POWER_CIRCUIT_CONTROL, new UnsignedWordElement(0x2010)) //
				), //
				new FC3ReadRegistersTask(0x2100, Priority.HIGH, //
						m(new UnsignedWordElement(0x2100).onUpdateCallback(this.onRegister0x2100Update)) //
								.m(BatteryBoxC130.ChannelId.CLUSTER_1_VOLTAGE, SCALE_FACTOR_2) // [mV]
								.m(Battery.ChannelId.VOLTAGE, SCALE_FACTOR_MINUS_1) // [V]
								.build(), //
						m(new SignedWordElement(0x2101)) //
								.m(BatteryBoxC130.ChannelId.CLUSTER_1_CURRENT, SCALE_FACTOR_2) // [mA]
								.m(Battery.ChannelId.CURRENT, SCALE_FACTOR_MINUS_1) // [A]
								.build(), //
						m(BatteryBoxC130.ChannelId.BATTERY_WORK_STATE, new UnsignedWordElement(0x2102)), //
						m(Battery.ChannelId.SOC, new UnsignedWordElement(0x2103)), //
						m(new UnsignedWordElement(0x2104)) //
								.m(BatteryBoxC130.ChannelId.CLUSTER_1_SOH, DIRECT_1_TO_1) // [%]
								.m(Battery.ChannelId.SOH, DIRECT_1_TO_1) // [%]
								.build(), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_MAX_CELL_VOLTAGE_ID, new UnsignedWordElement(0x2105)), //
						m(new UnsignedWordElement(0x2106)) //
								.m(BatteryBoxC130.ChannelId.CLUSTER_1_MAX_CELL_VOLTAGE, DIRECT_1_TO_1) //
								.m(Battery.ChannelId.MAX_CELL_VOLTAGE, DIRECT_1_TO_1) //
								.build(), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_MIN_CELL_VOLTAGE_ID, new UnsignedWordElement(0x2107)), //
						m(new UnsignedWordElement(0x2108)) //
								.m(BatteryBoxC130.ChannelId.CLUSTER_1_MIN_CELL_VOLTAGE, DIRECT_1_TO_1) //
								.m(Battery.ChannelId.MIN_CELL_VOLTAGE, DIRECT_1_TO_1) //
								.build(), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_MAX_CELL_TEMPERATURE_ID, new UnsignedWordElement(0x2109)), //
						m(new SignedWordElement(0x210A)) //
								.m(BatteryBoxC130.ChannelId.CLUSTER_1_MAX_CELL_TEMPERATURE, DIRECT_1_TO_1) //
								.m(Battery.ChannelId.MAX_CELL_TEMPERATURE, SCALE_FACTOR_MINUS_1) //
								.build(), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_MIN_CELL_TEMPERATURE_ID, new UnsignedWordElement(0x210B)), //
						m(new SignedWordElement(0x210C)) //
								.m(BatteryBoxC130.ChannelId.CLUSTER_1_MIN_CELL_TEMPERATURE, DIRECT_1_TO_1) //
								.m(Battery.ChannelId.MIN_CELL_TEMPERATURE, SCALE_FACTOR_MINUS_1) //
								.build()), //

				new FC3ReadRegistersTask(0x211D, Priority.HIGH, //
						m(new BitsWordElement(0x211D, this) //
								.bit(1, BatteryBoxC130.ChannelId.NEED_CHARGE)) //
				), //
				new FC3ReadRegistersTask(0x2140, Priority.LOW, //
						m(new BitsWordElement(0x2140, this) //
								.bit(0, BatteryBoxC130.ChannelId.PRE_ALARM_CELL_VOLTAGE_HIGH) //
								.bit(1, BatteryBoxC130.ChannelId.PRE_ALARM_CELL_VOLTAGE_LOW) //
								.bit(2, BatteryBoxC130.ChannelId.PRE_ALARM_CELL_VOLTAGE_DIFF_TOO_BIG) //
								.bit(3, BatteryBoxC130.ChannelId.PRE_ALARM_DISCHARGE_TEMP_HIGH) //
								.bit(4, BatteryBoxC130.ChannelId.PRE_ALARM_DISCHARGE_TEMP_LOW) //
								.bit(5, BatteryBoxC130.ChannelId.PRE_ALARM_CHARGE_TEMP_HIGH) //
								.bit(6, BatteryBoxC130.ChannelId.PRE_ALARM_CHARGE_TEMP_LOW) //
								.bit(7, BatteryBoxC130.ChannelId.PRE_ALARM_TEMP_DIFF_TOO_BIG) //
								.bit(9, BatteryBoxC130.ChannelId.PRE_ALARM_POWER_POLE_HIGH) //
								.bit(10, BatteryBoxC130.ChannelId.PRE_ALARM_DISCHARGE_CURRENT_HIGH) //
								.bit(11, BatteryBoxC130.ChannelId.PRE_ALARM_CHARGE_CURRENT_HIGH) //
						), //
						m(new BitsWordElement(0x2141, this) //
								.bit(0, BatteryBoxC130.ChannelId.LEVEL1_CELL_VOLTAGE_HIGH) //
								.bit(1, BatteryBoxC130.ChannelId.LEVEL1_CELL_VOLTAGE_LOW) //
								.bit(2, BatteryBoxC130.ChannelId.LEVEL1_CELL_VOLTAGE_DIFF_TOO_BIG) //
								.bit(3, BatteryBoxC130.ChannelId.LEVEL1_DISCHARGE_TEMP_HIGH) //
								.bit(4, BatteryBoxC130.ChannelId.LEVEL1_DISCHARGE_TEMP_LOW) //
								.bit(5, BatteryBoxC130.ChannelId.LEVEL1_CHARGE_TEMP_HIGH) //
								.bit(6, BatteryBoxC130.ChannelId.LEVEL1_CHARGE_TEMP_LOW) //
								.bit(7, BatteryBoxC130.ChannelId.LEVEL1_TEMP_DIFF_TOO_BIG) //
								.bit(9, BatteryBoxC130.ChannelId.LEVEL1_POWER_POLE_HIGH) //
								.bit(10, BatteryBoxC130.ChannelId.LEVEL1_DISCHARGE_CURRENT_HIGH) //
								.bit(11, BatteryBoxC130.ChannelId.LEVEL1_CHARGE_CURRENT_HIGH) //
						), //
						m(BatteryBoxC130.ChannelId.CLUSTER_RUN_STATE, new UnsignedWordElement(0x2142)), //
						m(new BitsWordElement(0x2143, this) //
								.bit(0, BatteryBoxC130.ChannelId.LEVEL2_CELL_VOLTAGE_HIGH) //
								.bit(1, BatteryBoxC130.ChannelId.LEVEL2_CELL_VOLTAGE_LOW) //
								.bit(2, BatteryBoxC130.ChannelId.LEVEL2_CELL_VOLTAGE_DIFF_TOO_BIG) //
								.bit(3, BatteryBoxC130.ChannelId.LEVEL2_DISCHARGE_TEMP_HIGH) //
								.bit(4, BatteryBoxC130.ChannelId.LEVEL2_DISCHARGE_TEMP_LOW) //
								.bit(5, BatteryBoxC130.ChannelId.LEVEL2_CHARGE_TEMP_HIGH) //
								.bit(6, BatteryBoxC130.ChannelId.LEVEL2_CHARGE_TEMP_LOW) //
								.bit(7, BatteryBoxC130.ChannelId.LEVEL2_TEMP_DIFF_TOO_BIG) //
								.bit(9, BatteryBoxC130.ChannelId.LEVEL2_POWER_POLE_HIGH) //
								.bit(10, BatteryBoxC130.ChannelId.LEVEL2_DISCHARGE_CURRENT_HIGH) //
								.bit(11, BatteryBoxC130.ChannelId.LEVEL2_CHARGE_CURRENT_HIGH) //
						), //
						m(new BitsWordElement(0x2144, this) //
								.bit(0, BatteryBoxC130.ChannelId.ALARM_SLAVE_CONTROL_SUMMARY) //
								.bit(1, BatteryBoxC130.ChannelId.ALARM_BCU_NTC) //
								.bit(2, BatteryBoxC130.ChannelId.ALARM_CONTACTOR_ADHESION) //
								.bit(3, BatteryBoxC130.ChannelId.ALARM_BCU_BMU_COMMUNICATION)//
								.bit(4, BatteryBoxC130.ChannelId.FAILURE_EEPROM2) //
								.bit(5, BatteryBoxC130.ChannelId.ALARM_CURRENT_SENSOR) //
								.bit(6, BatteryBoxC130.ChannelId.ALARM_INSULATION_CHECK) //
								.bit(7, BatteryBoxC130.ChannelId.ALARM_BAU_COMMUNICATION) //
								.bit(8, BatteryBoxC130.ChannelId.SHIELDED_SWITCH_STATE) //
								.bit(9, BatteryBoxC130.ChannelId.ALARM_FUSE) //
						), //
						m(new BitsWordElement(0x2145, this) //
								.bit(0, BatteryBoxC130.ChannelId.PRE_ALARM_SYSTEM_VOLTAGE_HIGH) //
								.bit(1, BatteryBoxC130.ChannelId.PRE_ALARM_SYSTEM_VOLTAGE_LOW) //
								.bit(2, BatteryBoxC130.ChannelId.PRE_ALARM_SYSTEM_VOLTAGE_UNBALANCED) //
								.bit(3, BatteryBoxC130.ChannelId.PRE_ALARM_INSULATION_RESISTANCE_LOWER) //
								.bit(4, BatteryBoxC130.ChannelId.PRE_ALARM_POS_INSULATION_RESISTANCE_LOWER) //
								.bit(5, BatteryBoxC130.ChannelId.PRE_ALARM_NEG_INSULATION_RESISTANCE_LOWER) //
								.bit(6, BatteryBoxC130.ChannelId.PRE_ALARM_SYSTEM_SOC_LOWER) //
								.bit(7, BatteryBoxC130.ChannelId.PRE_ALARM_SYSTEM_SOC_HIGH) //
								.bit(8, BatteryBoxC130.ChannelId.PRE_ALARM_SOH_LOWER) //
								.bit(9, BatteryBoxC130.ChannelId.PRE_ALARM_PACK_TEMP_HIGH) //
						), //

						m(new BitsWordElement(0x2146, this) //
								.bit(0, BatteryBoxC130.ChannelId.LEVEL1_SYSTEM_VOLTAGE_HIGH) //
								.bit(1, BatteryBoxC130.ChannelId.LEVEL1_SYSTEM_VOLTAGE_LOW) //
								.bit(2, BatteryBoxC130.ChannelId.LEVEL1_SYSTEM_VOLTAGE_UNBALANCED) //
								.bit(3, BatteryBoxC130.ChannelId.LEVEL1_INSULATION_RESISTANCE_LOWER) //
								.bit(4, BatteryBoxC130.ChannelId.LEVEL1_POS_INSULATION_RESISTANCE_LOWER) //
								.bit(5, BatteryBoxC130.ChannelId.LEVEL1_NEG_INSULATION_RESISTANCE_LOWER) //
								.bit(6, BatteryBoxC130.ChannelId.LEVEL1_SYSTEM_SOC_LOWER) //
								.bit(7, BatteryBoxC130.ChannelId.LEVEL1_SYSTEM_SOC_HIGH) //
								.bit(8, BatteryBoxC130.ChannelId.LEVEL1_SOH_LOWER) //
								.bit(9, BatteryBoxC130.ChannelId.LEVEL1_PACK_TEMP_HIGH) //
						), //
						m(new BitsWordElement(0x2147, this) //
								.bit(0, BatteryBoxC130.ChannelId.LEVEL2_SYSTEM_VOLTAGE_HIGH) //
								.bit(1, BatteryBoxC130.ChannelId.LEVEL2_SYSTEM_VOLTAGE_LOW) //
								.bit(2, BatteryBoxC130.ChannelId.LEVEL2_SYSTEM_VOLTAGE_UNBALANCED) //
								.bit(3, BatteryBoxC130.ChannelId.LEVEL2_INSULATION_RESISTANCE_LOWER) //
								.bit(4, BatteryBoxC130.ChannelId.LEVEL2_POS_INSULATION_RESISTANCE_LOWER) //
								.bit(5, BatteryBoxC130.ChannelId.LEVEL2_NEG_INSULATION_RESISTANCE_LOWER) //
								.bit(6, BatteryBoxC130.ChannelId.LEVEL2_SYSTEM_SOC_LOWER) //
								.bit(7, BatteryBoxC130.ChannelId.LEVEL2_SYSTEM_SOC_HIGH) //
								.bit(8, BatteryBoxC130.ChannelId.LEVEL2_SOH_LOWER) //
								.bit(9, BatteryBoxC130.ChannelId.LEVEL2_PACK_TEMP_HIGH))//

				), //
				new FC3ReadRegistersTask(0x216C, Priority.HIGH, //
						m(BatteryProtection.ChannelId.BP_CHARGE_BMS, new UnsignedWordElement(0x216C),
								SCALE_FACTOR_MINUS_1), //
						m(BatteryProtection.ChannelId.BP_DISCHARGE_BMS, new UnsignedWordElement(0x216D),
								SCALE_FACTOR_MINUS_1) //
				), //

				new FC3ReadRegistersTask(0x2183, Priority.LOW, //
						m(new BitsWordElement(0x2183, this) //
								.bit(0, BatteryBoxC130.ChannelId.SLAVE_17_COMMUNICATION_ERROR)//
								.bit(1, BatteryBoxC130.ChannelId.SLAVE_18_COMMUNICATION_ERROR)//
								.bit(2, BatteryBoxC130.ChannelId.SLAVE_19_COMMUNICATION_ERROR)//
								.bit(3, BatteryBoxC130.ChannelId.SLAVE_20_COMMUNICATION_ERROR)//
								.bit(4, BatteryBoxC130.ChannelId.SLAVE_21_COMMUNICATION_ERROR)//
								.bit(5, BatteryBoxC130.ChannelId.SLAVE_22_COMMUNICATION_ERROR)//
								.bit(6, BatteryBoxC130.ChannelId.SLAVE_23_COMMUNICATION_ERROR)//
								.bit(7, BatteryBoxC130.ChannelId.SLAVE_24_COMMUNICATION_ERROR)//
								.bit(8, BatteryBoxC130.ChannelId.SLAVE_25_COMMUNICATION_ERROR)//
								.bit(9, BatteryBoxC130.ChannelId.SLAVE_26_COMMUNICATION_ERROR)//
								.bit(10, BatteryBoxC130.ChannelId.SLAVE_27_COMMUNICATION_ERROR)//
								.bit(11, BatteryBoxC130.ChannelId.SLAVE_28_COMMUNICATION_ERROR)//
								.bit(12, BatteryBoxC130.ChannelId.SLAVE_29_COMMUNICATION_ERROR)//
								.bit(13, BatteryBoxC130.ChannelId.SLAVE_30_COMMUNICATION_ERROR)//
								.bit(14, BatteryBoxC130.ChannelId.SLAVE_31_COMMUNICATION_ERROR)//
								.bit(15, BatteryBoxC130.ChannelId.SLAVE_32_COMMUNICATION_ERROR)//
						), //
						m(new BitsWordElement(0x2184, this) //
								.bit(0, BatteryBoxC130.ChannelId.SLAVE_11_COMMUNICATION_ERROR)//
								.bit(1, BatteryBoxC130.ChannelId.SLAVE_12_COMMUNICATION_ERROR)//
								.bit(2, BatteryBoxC130.ChannelId.SLAVE_13_COMMUNICATION_ERROR)//
								.bit(3, BatteryBoxC130.ChannelId.SLAVE_14_COMMUNICATION_ERROR)//
								.bit(4, BatteryBoxC130.ChannelId.SLAVE_15_COMMUNICATION_ERROR)//
								.bit(5, BatteryBoxC130.ChannelId.SLAVE_16_COMMUNICATION_ERROR)//
						)), //
				new FC3ReadRegistersTask(0x2185, Priority.LOW, //
						m(new BitsWordElement(0x2185, this) //
								.bit(0, BatteryBoxC130.ChannelId.FAILURE_SLAVE_UNIT_INITIALIZATION)//
								.bit(1, BatteryBoxC130.ChannelId.FAILURE_VOLTAGE_SAMPLING_LINE)//
								.bit(2, BatteryBoxC130.ChannelId.FAILURE_CONNECTING_LINE)//
								.bit(3, BatteryBoxC130.ChannelId.FAILURE_SAMPLING_CHIP)//
								.bit(4, BatteryBoxC130.ChannelId.FAILURE_VOLTAGE_SAMPLING)//
								.bit(5, BatteryBoxC130.ChannelId.FAILURE_TEMP_SAMPLING)//
								.bit(6, BatteryBoxC130.ChannelId.FAILURE_TEMP_SENSOR)//
								.bit(7, BatteryBoxC130.ChannelId.FAILURE_CONTACTOR)//
								.bit(8, BatteryBoxC130.ChannelId.FAILURE_EEPROM)//
								.bit(9, BatteryBoxC130.ChannelId.FAILURE_PASSIVE_BALANCE)//
								.bit(10, BatteryBoxC130.ChannelId.FAILURE_PASSIVE_BALANCE_TEMP)//
								.bit(11, BatteryBoxC130.ChannelId.FAILURE_ACTIVE_BALANCE)//
						) //
				), //
				new FC3ReadRegistersTask(0x2800, Priority.LOW, //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_001_VOLTAGE, new UnsignedWordElement(0x2800)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_002_VOLTAGE, new UnsignedWordElement(0x2801)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_003_VOLTAGE, new UnsignedWordElement(0x2802)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_004_VOLTAGE, new UnsignedWordElement(0x2803)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_005_VOLTAGE, new UnsignedWordElement(0x2804)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_006_VOLTAGE, new UnsignedWordElement(0x2805)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_007_VOLTAGE, new UnsignedWordElement(0x2806)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_008_VOLTAGE, new UnsignedWordElement(0x2807)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_009_VOLTAGE, new UnsignedWordElement(0x2808)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_010_VOLTAGE, new UnsignedWordElement(0x2809)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_011_VOLTAGE, new UnsignedWordElement(0x280A)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_012_VOLTAGE, new UnsignedWordElement(0x280B)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_013_VOLTAGE, new UnsignedWordElement(0x280C)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_014_VOLTAGE, new UnsignedWordElement(0x280D)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_015_VOLTAGE, new UnsignedWordElement(0x280E)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_016_VOLTAGE, new UnsignedWordElement(0x280F)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_017_VOLTAGE, new UnsignedWordElement(0x2810)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_018_VOLTAGE, new UnsignedWordElement(0x2811)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_019_VOLTAGE, new UnsignedWordElement(0x2812)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_020_VOLTAGE, new UnsignedWordElement(0x2813)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_021_VOLTAGE, new UnsignedWordElement(0x2814)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_022_VOLTAGE, new UnsignedWordElement(0x2815)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_023_VOLTAGE, new UnsignedWordElement(0x2816)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_024_VOLTAGE, new UnsignedWordElement(0x2817)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_025_VOLTAGE, new UnsignedWordElement(0x2818)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_026_VOLTAGE, new UnsignedWordElement(0x2819)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_027_VOLTAGE, new UnsignedWordElement(0x281A)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_028_VOLTAGE, new UnsignedWordElement(0x281B)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_029_VOLTAGE, new UnsignedWordElement(0x281C)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_030_VOLTAGE, new UnsignedWordElement(0x281D)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_031_VOLTAGE, new UnsignedWordElement(0x281E)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_032_VOLTAGE, new UnsignedWordElement(0x281F)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_033_VOLTAGE, new UnsignedWordElement(0x2820)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_034_VOLTAGE, new UnsignedWordElement(0x2821)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_035_VOLTAGE, new UnsignedWordElement(0x2822)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_036_VOLTAGE, new UnsignedWordElement(0x2823)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_037_VOLTAGE, new UnsignedWordElement(0x2824)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_038_VOLTAGE, new UnsignedWordElement(0x2825)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_039_VOLTAGE, new UnsignedWordElement(0x2826)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_040_VOLTAGE, new UnsignedWordElement(0x2827)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_041_VOLTAGE, new UnsignedWordElement(0x2828)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_042_VOLTAGE, new UnsignedWordElement(0x2829)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_043_VOLTAGE, new UnsignedWordElement(0x282A)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_044_VOLTAGE, new UnsignedWordElement(0x282B)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_045_VOLTAGE, new UnsignedWordElement(0x282C)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_046_VOLTAGE, new UnsignedWordElement(0x282D)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_047_VOLTAGE, new UnsignedWordElement(0x282E)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_048_VOLTAGE, new UnsignedWordElement(0x282F)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_049_VOLTAGE, new UnsignedWordElement(0x2830)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_050_VOLTAGE, new UnsignedWordElement(0x2831)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_051_VOLTAGE, new UnsignedWordElement(0x2832)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_052_VOLTAGE, new UnsignedWordElement(0x2833)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_053_VOLTAGE, new UnsignedWordElement(0x2834)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_054_VOLTAGE, new UnsignedWordElement(0x2835)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_055_VOLTAGE, new UnsignedWordElement(0x2836)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_056_VOLTAGE, new UnsignedWordElement(0x2837)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_057_VOLTAGE, new UnsignedWordElement(0x2838)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_058_VOLTAGE, new UnsignedWordElement(0x2839)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_059_VOLTAGE, new UnsignedWordElement(0x283A)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_060_VOLTAGE, new UnsignedWordElement(0x283B)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_061_VOLTAGE, new UnsignedWordElement(0x283C)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_062_VOLTAGE, new UnsignedWordElement(0x283D)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_063_VOLTAGE, new UnsignedWordElement(0x283E)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_064_VOLTAGE, new UnsignedWordElement(0x283F)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_065_VOLTAGE, new UnsignedWordElement(0x2840)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_066_VOLTAGE, new UnsignedWordElement(0x2841)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_067_VOLTAGE, new UnsignedWordElement(0x2842)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_068_VOLTAGE, new UnsignedWordElement(0x2843)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_069_VOLTAGE, new UnsignedWordElement(0x2844)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_070_VOLTAGE, new UnsignedWordElement(0x2845)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_071_VOLTAGE, new UnsignedWordElement(0x2846)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_072_VOLTAGE, new UnsignedWordElement(0x2847)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_073_VOLTAGE, new UnsignedWordElement(0x2848)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_074_VOLTAGE, new UnsignedWordElement(0x2849)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_075_VOLTAGE, new UnsignedWordElement(0x284A)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_076_VOLTAGE, new UnsignedWordElement(0x284B)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_077_VOLTAGE, new UnsignedWordElement(0x284C)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_078_VOLTAGE, new UnsignedWordElement(0x284D)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_079_VOLTAGE, new UnsignedWordElement(0x284E)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_080_VOLTAGE, new UnsignedWordElement(0x284F)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_081_VOLTAGE, new UnsignedWordElement(0x2850)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_082_VOLTAGE, new UnsignedWordElement(0x2851)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_083_VOLTAGE, new UnsignedWordElement(0x2852)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_084_VOLTAGE, new UnsignedWordElement(0x2853)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_085_VOLTAGE, new UnsignedWordElement(0x2854)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_086_VOLTAGE, new UnsignedWordElement(0x2855)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_087_VOLTAGE, new UnsignedWordElement(0x2856)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_088_VOLTAGE, new UnsignedWordElement(0x2857)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_089_VOLTAGE, new UnsignedWordElement(0x2858)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_090_VOLTAGE, new UnsignedWordElement(0x2859)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_091_VOLTAGE, new UnsignedWordElement(0x285A)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_092_VOLTAGE, new UnsignedWordElement(0x285B)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_093_VOLTAGE, new UnsignedWordElement(0x285C)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_094_VOLTAGE, new UnsignedWordElement(0x285D)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_095_VOLTAGE, new UnsignedWordElement(0x285E)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_096_VOLTAGE, new UnsignedWordElement(0x285F)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_097_VOLTAGE, new UnsignedWordElement(0x2860)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_098_VOLTAGE, new UnsignedWordElement(0x2861)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_099_VOLTAGE, new UnsignedWordElement(0x2862)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_100_VOLTAGE, new UnsignedWordElement(0x2863)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_101_VOLTAGE, new UnsignedWordElement(0x2864)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_102_VOLTAGE, new UnsignedWordElement(0x2865)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_103_VOLTAGE, new UnsignedWordElement(0x2866)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_104_VOLTAGE, new UnsignedWordElement(0x2867)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_105_VOLTAGE, new UnsignedWordElement(0x2868)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_106_VOLTAGE, new UnsignedWordElement(0x2869)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_107_VOLTAGE, new UnsignedWordElement(0x286A)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_108_VOLTAGE, new UnsignedWordElement(0x286B)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_109_VOLTAGE, new UnsignedWordElement(0x286C)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_110_VOLTAGE, new UnsignedWordElement(0x286D)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_111_VOLTAGE, new UnsignedWordElement(0x286E)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_112_VOLTAGE, new UnsignedWordElement(0x286F)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_113_VOLTAGE, new UnsignedWordElement(0x2870)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_114_VOLTAGE, new UnsignedWordElement(0x2871)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_115_VOLTAGE, new UnsignedWordElement(0x2872)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_116_VOLTAGE, new UnsignedWordElement(0x2873)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_117_VOLTAGE, new UnsignedWordElement(0x2874)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_118_VOLTAGE, new UnsignedWordElement(0x2875)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_119_VOLTAGE, new UnsignedWordElement(0x2876)) //

				), //
				new FC3ReadRegistersTask(0x2877, Priority.LOW, //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_120_VOLTAGE, new UnsignedWordElement(0x2877)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_121_VOLTAGE, new UnsignedWordElement(0x2878)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_122_VOLTAGE, new UnsignedWordElement(0x2879)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_123_VOLTAGE, new UnsignedWordElement(0x287A)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_124_VOLTAGE, new UnsignedWordElement(0x287B)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_125_VOLTAGE, new UnsignedWordElement(0x287C)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_126_VOLTAGE, new UnsignedWordElement(0x287D)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_127_VOLTAGE, new UnsignedWordElement(0x287E)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_128_VOLTAGE, new UnsignedWordElement(0x287F)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_129_VOLTAGE, new UnsignedWordElement(0x2880)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_130_VOLTAGE, new UnsignedWordElement(0x2881)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_131_VOLTAGE, new UnsignedWordElement(0x2882)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_132_VOLTAGE, new UnsignedWordElement(0x2883)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_133_VOLTAGE, new UnsignedWordElement(0x2884)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_134_VOLTAGE, new UnsignedWordElement(0x2885)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_135_VOLTAGE, new UnsignedWordElement(0x2886)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_136_VOLTAGE, new UnsignedWordElement(0x2887)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_137_VOLTAGE, new UnsignedWordElement(0x2888)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_138_VOLTAGE, new UnsignedWordElement(0x2889)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_139_VOLTAGE, new UnsignedWordElement(0x288A)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_140_VOLTAGE, new UnsignedWordElement(0x288B)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_141_VOLTAGE, new UnsignedWordElement(0x288C)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_142_VOLTAGE, new UnsignedWordElement(0x288D)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_143_VOLTAGE, new UnsignedWordElement(0x288E)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_144_VOLTAGE, new UnsignedWordElement(0x288F)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_145_VOLTAGE, new UnsignedWordElement(0x2890)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_146_VOLTAGE, new UnsignedWordElement(0x2891)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_147_VOLTAGE, new UnsignedWordElement(0x2892)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_148_VOLTAGE, new UnsignedWordElement(0x2893)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_149_VOLTAGE, new UnsignedWordElement(0x2894)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_150_VOLTAGE, new UnsignedWordElement(0x2895)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_151_VOLTAGE, new UnsignedWordElement(0x2896)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_152_VOLTAGE, new UnsignedWordElement(0x2897)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_153_VOLTAGE, new UnsignedWordElement(0x2898)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_154_VOLTAGE, new UnsignedWordElement(0x2899)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_155_VOLTAGE, new UnsignedWordElement(0x289A)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_156_VOLTAGE, new UnsignedWordElement(0x289B)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_157_VOLTAGE, new UnsignedWordElement(0x289C)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_158_VOLTAGE, new UnsignedWordElement(0x289D)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_159_VOLTAGE, new UnsignedWordElement(0x289E)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_160_VOLTAGE, new UnsignedWordElement(0x289F)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_161_VOLTAGE, new UnsignedWordElement(0x28A0)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_162_VOLTAGE, new UnsignedWordElement(0x28A1)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_163_VOLTAGE, new UnsignedWordElement(0x28A2)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_164_VOLTAGE, new UnsignedWordElement(0x28A3)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_165_VOLTAGE, new UnsignedWordElement(0x28A4)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_166_VOLTAGE, new UnsignedWordElement(0x28A5)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_167_VOLTAGE, new UnsignedWordElement(0x28A6)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_168_VOLTAGE, new UnsignedWordElement(0x28A7)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_169_VOLTAGE, new UnsignedWordElement(0x28A8)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_170_VOLTAGE, new UnsignedWordElement(0x28A9)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_171_VOLTAGE, new UnsignedWordElement(0x28AA)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_172_VOLTAGE, new UnsignedWordElement(0x28AB)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_173_VOLTAGE, new UnsignedWordElement(0x28AC)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_174_VOLTAGE, new UnsignedWordElement(0x28AD)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_175_VOLTAGE, new UnsignedWordElement(0x28AE)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_176_VOLTAGE, new UnsignedWordElement(0x28AF)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_177_VOLTAGE, new UnsignedWordElement(0x28B0)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_178_VOLTAGE, new UnsignedWordElement(0x28B1)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_179_VOLTAGE, new UnsignedWordElement(0x28B2)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_180_VOLTAGE, new UnsignedWordElement(0x28B3)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_181_VOLTAGE, new UnsignedWordElement(0x28B4)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_182_VOLTAGE, new UnsignedWordElement(0x28B5)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_183_VOLTAGE, new UnsignedWordElement(0x28B6)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_184_VOLTAGE, new UnsignedWordElement(0x28B7)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_185_VOLTAGE, new UnsignedWordElement(0x28B8)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_186_VOLTAGE, new UnsignedWordElement(0x28B9)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_187_VOLTAGE, new UnsignedWordElement(0x28BA)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_188_VOLTAGE, new UnsignedWordElement(0x28BB)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_189_VOLTAGE, new UnsignedWordElement(0x28BC)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_190_VOLTAGE, new UnsignedWordElement(0x28BD)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_191_VOLTAGE, new UnsignedWordElement(0x28BE)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_192_VOLTAGE, new UnsignedWordElement(0x28BF)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_193_VOLTAGE, new UnsignedWordElement(0x28C0)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_194_VOLTAGE, new UnsignedWordElement(0x28C1)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_195_VOLTAGE, new UnsignedWordElement(0x28C2)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_196_VOLTAGE, new UnsignedWordElement(0x28C3)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_197_VOLTAGE, new UnsignedWordElement(0x28C4)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_198_VOLTAGE, new UnsignedWordElement(0x28C5)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_199_VOLTAGE, new UnsignedWordElement(0x28C6)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_200_VOLTAGE, new UnsignedWordElement(0x28C7)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_201_VOLTAGE, new UnsignedWordElement(0x28C8)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_202_VOLTAGE, new UnsignedWordElement(0x28C9)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_203_VOLTAGE, new UnsignedWordElement(0x28CA)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_204_VOLTAGE, new UnsignedWordElement(0x28CB)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_205_VOLTAGE, new UnsignedWordElement(0x28CC)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_206_VOLTAGE, new UnsignedWordElement(0x28CD)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_207_VOLTAGE, new UnsignedWordElement(0x28CE)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_208_VOLTAGE, new UnsignedWordElement(0x28CF)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_209_VOLTAGE, new UnsignedWordElement(0x28D0)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_210_VOLTAGE, new UnsignedWordElement(0x28D1)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_211_VOLTAGE, new UnsignedWordElement(0x28D2)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_212_VOLTAGE, new UnsignedWordElement(0x28D3)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_213_VOLTAGE, new UnsignedWordElement(0x28D4)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_214_VOLTAGE, new UnsignedWordElement(0x28D5)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_215_VOLTAGE, new UnsignedWordElement(0x28D6)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_216_VOLTAGE, new UnsignedWordElement(0x28D7)) //
				), //
				new FC3ReadRegistersTask(0x2C00, Priority.LOW, //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_00_TEMPERATURE, new UnsignedWordElement(0x2C00)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_01_TEMPERATURE, new UnsignedWordElement(0x2C01)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_02_TEMPERATURE, new UnsignedWordElement(0x2C02)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_03_TEMPERATURE, new UnsignedWordElement(0x2C03)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_04_TEMPERATURE, new UnsignedWordElement(0x2C04)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_05_TEMPERATURE, new UnsignedWordElement(0x2C05)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_06_TEMPERATURE, new UnsignedWordElement(0x2C06)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_07_TEMPERATURE, new UnsignedWordElement(0x2C07)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_08_TEMPERATURE, new UnsignedWordElement(0x2C08)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_09_TEMPERATURE, new UnsignedWordElement(0x2C09)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_10_TEMPERATURE, new UnsignedWordElement(0x2C0A)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_11_TEMPERATURE, new UnsignedWordElement(0x2C0B)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_12_TEMPERATURE, new UnsignedWordElement(0x2C0C)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_13_TEMPERATURE, new UnsignedWordElement(0x2C0D)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_14_TEMPERATURE, new UnsignedWordElement(0x2C0E)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_15_TEMPERATURE, new UnsignedWordElement(0x2C0F)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_16_TEMPERATURE, new UnsignedWordElement(0x2C10)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_17_TEMPERATURE, new UnsignedWordElement(0x2C11)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_18_TEMPERATURE, new UnsignedWordElement(0x2C12)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_19_TEMPERATURE, new UnsignedWordElement(0x2C13)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_20_TEMPERATURE, new UnsignedWordElement(0x2C14)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_21_TEMPERATURE, new UnsignedWordElement(0x2C15)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_22_TEMPERATURE, new UnsignedWordElement(0x2C16)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_23_TEMPERATURE, new UnsignedWordElement(0x2C17)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_24_TEMPERATURE, new UnsignedWordElement(0x2C18)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_25_TEMPERATURE, new UnsignedWordElement(0x2C19)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_26_TEMPERATURE, new UnsignedWordElement(0x2C1A)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_27_TEMPERATURE, new UnsignedWordElement(0x2C1B)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_28_TEMPERATURE, new UnsignedWordElement(0x2C1C)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_29_TEMPERATURE, new UnsignedWordElement(0x2C1D)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_30_TEMPERATURE, new UnsignedWordElement(0x2C1E)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_31_TEMPERATURE, new UnsignedWordElement(0x2C1F)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_32_TEMPERATURE, new UnsignedWordElement(0x2C20)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_33_TEMPERATURE, new UnsignedWordElement(0x2C21)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_34_TEMPERATURE, new UnsignedWordElement(0x2C22)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_35_TEMPERATURE, new UnsignedWordElement(0x2C23)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_36_TEMPERATURE, new UnsignedWordElement(0x2C24)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_37_TEMPERATURE, new UnsignedWordElement(0x2C25)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_38_TEMPERATURE, new UnsignedWordElement(0x2C26)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_39_TEMPERATURE, new UnsignedWordElement(0x2C27)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_40_TEMPERATURE, new UnsignedWordElement(0x2C28)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_41_TEMPERATURE, new UnsignedWordElement(0x2C29)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_42_TEMPERATURE, new UnsignedWordElement(0x2C2A)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_43_TEMPERATURE, new UnsignedWordElement(0x2C2B)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_44_TEMPERATURE, new UnsignedWordElement(0x2C2C)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_45_TEMPERATURE, new UnsignedWordElement(0x2C2D)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_46_TEMPERATURE, new UnsignedWordElement(0x2C2E)), //
						m(BatteryBoxC130.ChannelId.CLUSTER_1_BATTERY_47_TEMPERATURE, new UnsignedWordElement(0x2C2F)) //
				)//
		); //
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				Battery.getModbusSlaveNatureTable(accessMode) //
		);
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

	/*
	 * Handle incompatibility with old hardware protocol.
	 *
	 * 'onRegister0x2100Update()' callback is called when register 0x2100 is read.
	 */

	private boolean isModbusProtocolInitialized = false;
	private final Consumer<Integer> onRegister0x2100Update = value -> {
		if (value == null) {
			// ignore invalid values; modbus bridge has no connection yet
			return;
		}
		if (BatteryBoxC130Impl.this.isModbusProtocolInitialized) {
			// execute only once
			return;
		}
		BatteryBoxC130Impl.this.isModbusProtocolInitialized = true;

		// Try to read MODULE_QTY Register
		try {
			ModbusUtils.readELementOnce(this.getModbusProtocol(), new UnsignedWordElement(0x210D), false)
					.thenAccept(moduleQtyValue -> {
						if (moduleQtyValue != null) {
							// Register is available -> add Registers for current hardware to protocol
							try {
								this.getModbusProtocol().addTasks(//
										new FC3ReadRegistersTask(0x210D, Priority.LOW, //
												m(BatteryBoxC130.ChannelId.MODULE_QTY, new UnsignedWordElement(0x210D)), //
												m(BatteryBoxC130.ChannelId.TOTAL_VOLTAGE_OF_SINGLE_MODULE,
														new UnsignedWordElement(0x210E))), //
										new FC3ReadRegistersTask(0x216E, Priority.LOW, //
												m(Battery.ChannelId.CHARGE_MAX_VOLTAGE, new UnsignedWordElement(0x216E), //
														SCALE_FACTOR_MINUS_1), //
												m(Battery.ChannelId.DISCHARGE_MIN_VOLTAGE,
														new UnsignedWordElement(0x216F), //
														SCALE_FACTOR_MINUS_1) //
								));
							} catch (OpenemsException e) {
								BatteryBoxC130Impl.this.logError(BatteryBoxC130Impl.this.log,
										"Unable to add registers for detected hardware version: " + e.getMessage());
								e.printStackTrace();
							} //
						} else {
							BatteryBoxC130Impl.this.logInfo(BatteryBoxC130Impl.this.log,
									"Detected old hardware version. Registers are not available. Setting default values.");

							this._setChargeMaxVoltage(OLD_VERSION_DEFAULT_CHARGE_MAX_VOLTAGE);
							this._setDischargeMinVoltage(OLD_VERSION_DEFAULT_DISCHARGE_MIN_VOLTAGE);
						}
					});
		} catch (OpenemsException e) {
			BatteryBoxC130Impl.this.logError(BatteryBoxC130Impl.this.log,
					"Unable to detect hardware version: " + e.getMessage());
			e.printStackTrace();
		}
	};

}
