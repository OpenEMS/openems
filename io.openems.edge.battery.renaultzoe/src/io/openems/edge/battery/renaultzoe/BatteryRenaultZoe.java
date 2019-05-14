package io.openems.edge.battery.renaultzoe;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.AccessMode;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.Level;
import io.openems.edge.common.channel.Unit;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;

import org.osgi.service.metatype.annotations.Designate;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Bms.RenaultZoe", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
)
public class BatteryRenaultZoe extends AbstractOpenemsModbusComponent
		implements Battery, OpenemsComponent, EventHandler, ModbusSlave {

	// Default values for the battery ranges
	public static final int DISCHARGE_MIN_V = 288;
	public static final int CHARGE_MAX_V = 400;
	public static final int DISCHARGE_MAX_A = 300;
	public static final int CHARGE_MAX_A = 300;

	public BatteryRenaultZoe() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				BatteryRenaultZoe.ChannelId.values() //
		);
		this.channel(Battery.ChannelId.CHARGE_MAX_CURRENT).setNextValue(BatteryRenaultZoe.CHARGE_MAX_A);
		this.channel(Battery.ChannelId.CHARGE_MAX_VOLTAGE).setNextValue(BatteryRenaultZoe.CHARGE_MAX_V);
		this.channel(Battery.ChannelId.DISCHARGE_MAX_CURRENT).setNextValue(BatteryRenaultZoe.DISCHARGE_MAX_A);
		this.channel(Battery.ChannelId.DISCHARGE_MIN_VOLTAGE).setNextValue(BatteryRenaultZoe.DISCHARGE_MIN_V);
	}

	@ObjectClassDefinition
	@interface Config {
		String name() default "World";
	}

	private String name;

	@Activate
	void activate(Config config) {
		this.name = config.name();
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
//			handleBatteryState();
			break;
		}
	}

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/*
		 * Battery String1
		 */

		// IntegerReadChannels
		END_OF_CHARGE_REQUEST(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		AVAILABLE_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT).accessMode(AccessMode.READ_ONLY)),
		HV_BATTERY_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		HV_BAT_HEALTH(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)),
		LBCPRUN_ANSWER(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		HV_BATTERY_MAX_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		HV_BAT_STATE(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		LBC_REFUSE_TO_SLEEP(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		AVAILABLE_ENERGY(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_ONLY)),
		ISOL_DIAG_AUTHORISATION(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		SAFETY_MODE_1_FLAG(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		HV_ISOLATON_IMPEDANCE(Doc.of(OpenemsType.INTEGER).unit(Unit.OHM).accessMode(AccessMode.READ_ONLY)),
		CELL_HIGHEST_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT).accessMode(AccessMode.READ_ONLY)),
		CELL_LOWEST_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT).accessMode(AccessMode.READ_ONLY)),
		CHARGING_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT).accessMode(AccessMode.READ_ONLY)),
		HV_BAT_INSTANT_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
		HV_POWER_CONNECTION(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		HV_BAT_LEVEL2_FAILURE(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		HV_BAT_LEVEL1_FAILURE(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		USER_SOC(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)),
		HV_NETWORK_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
		HV_BAT_SERIAL_NUMBER(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		CELL_LOWEST_VOLTAGE_RCY(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		CELL_HIGHEST_VOLTAGE_RCY(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		HV_BATTERY_MAX_TEMP_RCY(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		LBCRUN_ANSWER_RCY(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		HV_POWER_CONNECTION_RCY(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		HV_BAT_LEVEL2_FAILURE_RCY(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		SAFETY_MODE_1_FLAG_RCY(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		LBC2_REFUSE_TO_SLEEP(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		ELEC_MASCHINE_SPEED(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		ETS_SLEEP_MODE(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		SCH_WAKE_UP_SLEEP_COMMAND(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		WAKE_UP_TYPE(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		LBCPRUN_KEY(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		LBCPRUNKEY_RCY(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		OPERATING_TYPE(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		POWER_RELAY_STATE(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		DISTANCE_TOTALIZER_COPY(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		ABSOLUTE_TIME_SINCE_1RST_IGNITION(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		VEHICLE_ID(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),

		// EnumReadChannels
		STR_ST(Doc.of(StringStatus.values()).accessMode(AccessMode.READ_ONLY)), //

		// EnumWriteChannels
		EN_STRING(Doc.of(EnableString.values()).accessMode(AccessMode.READ_WRITE)), //
		CON_STRING(Doc.of(StartStopString.values()).accessMode(AccessMode.READ_WRITE)), //

		// StateChannels
		ALARM_STRING_1(Doc.of(Level.WARNING).text("Alarm Ids specific")), //
		FAULT_STRING_1(Doc.of(Level.FAULT).text("Fault Ids specific")), //

		/*
		 * Battery Stack
		 */

		// IntegerReadChannels
		SERIAL_NO(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		MOD_V(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		STATE(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		WARR_DT(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		INST_DT(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		AH_RTG(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE_HOURS).accessMode(AccessMode.READ_ONLY)),
		WH_RTG(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)),
		W_CHA_RTE_MAX(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)),
		W_DIS_CHA_RTE_MAX(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)),
		V_MAX(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
		V_MIN(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
		CELL_V_MAX(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT).accessMode(AccessMode.READ_ONLY)),
		CELL_V_MAX_STR(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		CELL_V_MIN(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT).accessMode(AccessMode.READ_ONLY)),
		CELL_V_MIN_STR(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		CHARGE_WH(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)),
		DISCHARGE_WH(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)),
		TOTAL_CAP(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)),
		REST_CAP(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)),
		A(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
		V(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
		A_CHA_MAX(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
		A_DIS_CHA_MAX(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
		W(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)),
		N_STR(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		N_STR_CON(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		STR_V_MAX(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
		STR_V_MAX_STR(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		STR_V_MIN(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
		STR_V_MIN_STR(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		STR_V_AVG(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
		STR_A_MAX(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
		STR_A_MAX_STR(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
		STR_A_MIN(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
		STR_A_MIN_STR(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		STR_A_AVG(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),

		// IntegerWriteChannels
		REQ_W(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).accessMode(AccessMode.WRITE_ONLY)),

		// EnumReadChannels
		TYP(Doc.of(BatteryTyp.values())), //
		BAT_MAN(Doc.of(BatteryManufacturer.values())), //
		BAT_MODEL(Doc.of(BatteryModel.values())), //
		REQ_INV_STATE(Doc.of(InverterStateRequest.values())), //
		CTL_MODE(Doc.of(ControlMode.values())), //

		// EnumWriteChannels
		REQ_MODE(Doc.of(BatteryChargeDischargeRequest.values())), ON_OFF(Doc.of(StartStopBatteryStack.values())),

		// StateChannels
		ALARM_STACK(Doc.of(Level.WARNING).text("Alarm Stack")), //
		FAULT_STACK(Doc.of(Level.FAULT).text("Fault Stack")); //

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //

				/*
				 * Battery String1
				 */

				new FC3ReadRegistersTask(0x100, Priority.HIGH, //
						m(BatteryRenaultZoe.ChannelId.END_OF_CHARGE_REQUEST, new UnsignedWordElement(0x100), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.AVAILABLE_POWER, new UnsignedWordElement(0x101), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.HV_BATTERY_TEMP, new UnsignedWordElement(0x102), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.HV_BAT_HEALTH, new UnsignedWordElement(0x103), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.LBCPRUN_ANSWER, new UnsignedWordElement(0x104), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.HV_BATTERY_MAX_TEMP, new UnsignedWordElement(0x105), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.HV_BAT_STATE, new UnsignedWordElement(0x106), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.LBC_REFUSE_TO_SLEEP, new UnsignedWordElement(0x107), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.AVAILABLE_ENERGY, new UnsignedWordElement(0x108), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(BatteryRenaultZoe.ChannelId.ISOL_DIAG_AUTHORISATION, new UnsignedWordElement(0x109), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(BatteryRenaultZoe.ChannelId.SAFETY_MODE_1_FLAG, new UnsignedWordElement(0x110), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(BatteryRenaultZoe.ChannelId.HV_ISOLATON_IMPEDANCE, new UnsignedWordElement(0x111), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(BatteryRenaultZoe.ChannelId.CELL_HIGHEST_VOLTAGE, new UnsignedWordElement(0x112), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(BatteryRenaultZoe.ChannelId.CELL_LOWEST_VOLTAGE, new UnsignedWordElement(0x113), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(BatteryRenaultZoe.ChannelId.CHARGING_POWER, new UnsignedWordElement(0x114), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(BatteryRenaultZoe.ChannelId.HV_BAT_INSTANT_CURRENT, new UnsignedWordElement(0x115), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(BatteryRenaultZoe.ChannelId.HV_POWER_CONNECTION, new UnsignedWordElement(0x116), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(BatteryRenaultZoe.ChannelId.HV_BAT_LEVEL2_FAILURE, new UnsignedWordElement(0x117), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(BatteryRenaultZoe.ChannelId.HV_BAT_LEVEL1_FAILURE, new UnsignedWordElement(0x118), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(BatteryRenaultZoe.ChannelId.USER_SOC, new UnsignedWordElement(0x119), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(BatteryRenaultZoe.ChannelId.HV_NETWORK_VOLTAGE, new UnsignedWordElement(0x120), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(BatteryRenaultZoe.ChannelId.HV_BAT_SERIAL_NUMBER, new UnsignedWordElement(0x121), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(BatteryRenaultZoe.ChannelId.CELL_LOWEST_VOLTAGE_RCY, new UnsignedWordElement(0x122), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(BatteryRenaultZoe.ChannelId.CELL_HIGHEST_VOLTAGE_RCY, new UnsignedWordElement(0x123), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(BatteryRenaultZoe.ChannelId.HV_BATTERY_MAX_TEMP_RCY, new UnsignedWordElement(0x124), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(BatteryRenaultZoe.ChannelId.LBCRUN_ANSWER_RCY, new UnsignedWordElement(0x125), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(BatteryRenaultZoe.ChannelId.HV_POWER_CONNECTION_RCY, new UnsignedWordElement(0x126), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(BatteryRenaultZoe.ChannelId.HV_BAT_LEVEL2_FAILURE_RCY, new UnsignedWordElement(0x127), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(BatteryRenaultZoe.ChannelId.SAFETY_MODE_1_FLAG_RCY, new UnsignedWordElement(0x128), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(BatteryRenaultZoe.ChannelId.LBC2_REFUSE_TO_SLEEP, new UnsignedWordElement(0x129), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(BatteryRenaultZoe.ChannelId.ELEC_MASCHINE_SPEED, new UnsignedWordElement(0x130), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(BatteryRenaultZoe.ChannelId.ETS_SLEEP_MODE, new UnsignedWordElement(0x131), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(BatteryRenaultZoe.ChannelId.SCH_WAKE_UP_SLEEP_COMMAND, new UnsignedWordElement(0x132), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(BatteryRenaultZoe.ChannelId.WAKE_UP_TYPE, new UnsignedWordElement(0x133), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(BatteryRenaultZoe.ChannelId.LBCPRUN_KEY, new UnsignedWordElement(0x134), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(BatteryRenaultZoe.ChannelId.LBCPRUNKEY_RCY, new UnsignedWordElement(0x135), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(BatteryRenaultZoe.ChannelId.OPERATING_TYPE, new UnsignedWordElement(0x136), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(BatteryRenaultZoe.ChannelId.POWER_RELAY_STATE, new UnsignedWordElement(0x137), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(BatteryRenaultZoe.ChannelId.DISTANCE_TOTALIZER_COPY, new UnsignedWordElement(0x138), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(BatteryRenaultZoe.ChannelId.ABSOLUTE_TIME_SINCE_1RST_IGNITION, new UnsignedWordElement(0x139), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(BatteryRenaultZoe.ChannelId.VEHICLE_ID, new UnsignedWordElement(0x140), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),//
						new DummyRegisterElement(0x141, 0x159),
						m(BatteryRenaultZoe.ChannelId.STR_ST, new UnsignedWordElement(0x160), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)), //
				
				new FC6WriteRegisterTask(0x161, //
						m(BatteryRenaultZoe.ChannelId.EN_STRING, new UnsignedWordElement(0x161)) //	
				),
				new FC6WriteRegisterTask(0x162, //
						m(BatteryRenaultZoe.ChannelId.CON_STRING, new UnsignedWordElement(0x162)) //	
				),
				
				
				/*
				 * Battery Stack
				 */

				new FC3ReadRegistersTask(0x500, Priority.HIGH, //
						m(BatteryRenaultZoe.ChannelId.SERIAL_NO, new UnsignedWordElement(0x500), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.MOD_V, new UnsignedWordElement(0x501), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.TYP, new UnsignedWordElement(0x502), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.BAT_MAN, new UnsignedWordElement(0x503), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(BatteryRenaultZoe.ChannelId.BAT_MODEL, new UnsignedWordElement(0x504), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.STATE, new UnsignedWordElement(0x505), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.WARR_DT, new UnsignedWordElement(0x506), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.INST_DT, new UnsignedWordElement(0x507), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.AH_RTG, new UnsignedWordElement(0x508), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.WH_RTG, new UnsignedWordElement(0x509), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.W_CHA_RTE_MAX, new UnsignedWordElement(0x510), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.W_DIS_CHA_RTE_MAX, new UnsignedWordElement(0x511), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.V_MAX, new UnsignedWordElement(0x512), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.V_MIN, new UnsignedWordElement(0x513), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.CELL_V_MAX, new UnsignedWordElement(0x514), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.CELL_V_MAX_STR, new UnsignedWordElement(0x515), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.CELL_V_MIN, new UnsignedWordElement(0x516), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.CELL_V_MIN_STR, new UnsignedWordElement(0x517), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.CHARGE_WH, new UnsignedWordElement(0x518), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.DISCHARGE_WH, new UnsignedWordElement(0x519), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.TOTAL_CAP, new UnsignedWordElement(0x520), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.REST_CAP, new UnsignedWordElement(0x521), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.A, new UnsignedWordElement(0x522), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.V, new UnsignedWordElement(0x523), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.A_CHA_MAX, new UnsignedWordElement(0x524), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.A_DIS_CHA_MAX, new UnsignedWordElement(0x525), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.W, new UnsignedWordElement(0x526), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.REQ_INV_STATE, new UnsignedWordElement(0x527), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)), //	
				
				new FC6WriteRegisterTask(0x528, //
						m(BatteryRenaultZoe.ChannelId.REQ_W, new UnsignedWordElement(0x528)) //
				),
				new FC6WriteRegisterTask(0x529, //
						m(BatteryRenaultZoe.ChannelId.REQ_MODE, new UnsignedWordElement(0x529)) //
				),
				new FC3ReadRegistersTask(0x530, Priority.HIGH, //
						m(BatteryRenaultZoe.ChannelId.CTL_MODE, new UnsignedWordElement(0x530), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)), //
				new FC6WriteRegisterTask(0x531, //
						m(BatteryRenaultZoe.ChannelId.ON_OFF, new UnsignedWordElement(0x531)) //
				),		
				new FC3ReadRegistersTask(0x532, Priority.HIGH, //			
						m(BatteryRenaultZoe.ChannelId.N_STR, new UnsignedWordElement(0x532), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.N_STR_CON, new UnsignedWordElement(0x533), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.STR_V_MAX, new UnsignedWordElement(0x534), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.STR_V_MAX_STR, new UnsignedWordElement(0x535), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.STR_V_MIN, new UnsignedWordElement(0x536), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.STR_V_MIN_STR, new UnsignedWordElement(0x537), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.STR_V_AVG, new UnsignedWordElement(0x538), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.STR_A_MAX, new UnsignedWordElement(0x539), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.STR_A_MAX_STR, new UnsignedWordElement(0x540), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.STR_A_MIN, new UnsignedWordElement(0x541), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.STR_A_MIN_STR, new UnsignedWordElement(0x542), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.STR_A_AVG, new UnsignedWordElement(0x543), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.ALARM_STACK, new UnsignedWordElement(0x544), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BatteryRenaultZoe.ChannelId.FAULT_STACK, new UnsignedWordElement(0x545), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)) //
				);//


	}



	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable( //
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				Battery.getModbusSlaveNatureTable(accessMode));
	}

}
