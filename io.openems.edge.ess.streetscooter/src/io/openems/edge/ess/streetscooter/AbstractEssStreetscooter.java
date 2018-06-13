package io.openems.edge.ess.streetscooter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC1ReadCoilsTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC5WriteCoilTask;
import io.openems.edge.bridge.modbus.api.task.Priority;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.Ess;
import io.openems.edge.ess.power.symmetric.PGreaterEqualLimitation;
import io.openems.edge.ess.power.symmetric.PSmallerEqualLimitation;
import io.openems.edge.ess.power.symmetric.SymmetricPower;
import io.openems.edge.ess.symmetric.api.SymmetricEss;

public abstract class AbstractEssStreetscooter extends AbstractOpenemsModbusComponent implements SymmetricEss, Ess, OpenemsComponent {

	protected static final int UNIT_ID = 100;

	static final int MAX_APPARENT_POWER = 2000;
	private static final int POWER_PRECISION = 100;

	private static final int ICU_RUN_ADDRESS = 4002;
	private static final int BATTERY_INFO_START_ADDRESS = 0;
	private static final int INVERTER_INFO_START_ADDRESS = 2000;

	private final Logger log = LoggerFactory.getLogger(AbstractOpenemsModbusComponent.class);

	private SymmetricPower power;
	private PGreaterEqualLimitation allowedChargeLimit;
	private PSmallerEqualLimitation allowedDischargeLimit;

	public AbstractEssStreetscooter() {
		addChannels(); 
		initializePower();
	}

	@Override
	public SymmetricPower getPower() {
		return this.power;
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().value().asString() + 
				", mode:" + this.channel(ChannelId.INVERTER_MODE).value().asOptionString();
	}


	protected void initializePower() {
		this.power = new SymmetricPower(this, AbstractEssStreetscooter.MAX_APPARENT_POWER, AbstractEssStreetscooter.POWER_PRECISION, new PowerHandler(this, log));
		// Allowed Charge
		this.power.addStaticLimitation( //
				this.allowedChargeLimit = new PGreaterEqualLimitation(this.power).setP(0) //
				);
		this.channel(ChannelId.BATTERY_BMS_PWR_CHRG_MAX).onUpdate(value -> { // TODO is this the right field?
			this.allowedChargeLimit.setP(TypeUtils.getAsType(OpenemsType.INTEGER, value));
		});
		// Allowed Discharge
		this.power.addStaticLimitation( //
				this.allowedDischargeLimit = new PSmallerEqualLimitation(this.power).setP(0) //
				);
		this.channel(ChannelId.BATTERY_BMS_PWR_D_CHA_MAX).onUpdate(value -> {// TODO is this the right field?
			this.allowedDischargeLimit.setP(TypeUtils.getAsType(OpenemsType.INTEGER, value));
		});
	}

	@Override
	protected ModbusProtocol defineModbusProtocol(int unitId) {
		int batteryInfoStartAddress = getBatteryInfoStartAddress() + getAdressOffsetForBattery();
		int inverterInfoStartAddress = getInverterInfoStartAddress() + getAdressOffsetForInverter();

		return new ModbusProtocol(unitId, //
				new FC1ReadCoilsTask(getIcuRunAddress(), Priority.HIGH, m(ChannelId.ICU_RUN, new CoilElement(getIcuRunAddress()))),
				new FC5WriteCoilTask(getIcuRunAddress(), m(ChannelId.ICU_RUN, new CoilElement(getIcuRunAddress()))),
				new FC1ReadCoilsTask(getIcuEnabledAddress(), Priority.HIGH, m(ChannelId.ICU_ENABLED, new CoilElement(getIcuEnabledAddress()))),
				new FC5WriteCoilTask(getIcuEnabledAddress(), m(ChannelId.ICU_ENABLED, new CoilElement(getIcuEnabledAddress()))),
				new FC4ReadInputRegistersTask(getInverterModeAddress(), Priority.HIGH,  
						m(ChannelId.INVERTER_MODE, new FloatDoublewordElement(getInverterModeAddress()).wordOrder(WordOrder.LSWMSW))
						),
				new FC16WriteRegistersTask(getIcuSetPowerAddress(),  
						m(ChannelId.SET_ACTIVE_POWER, new FloatDoublewordElement(getIcuSetPowerAddress()).wordOrder(WordOrder.LSWMSW)) 
						),
				new FC4ReadInputRegistersTask(batteryInfoStartAddress, Priority.HIGH, 
						/*Float gets the right result*/ m(ChannelId.BATTERY_BMS_ERR, new FloatDoublewordElement(batteryInfoStartAddress).wordOrder(WordOrder.LSWMSW)),
						/*double word seems to be wrong?*/	//m(ChannelId.BATTERY_BMS_ERR, new UnsignedDoublewordElement(getBatteryBmsErrAddress()).wordOrder(WordOrder.LSWMSW))
						m(ChannelId.BATTERY_BMS_I_ACT, new FloatDoublewordElement(batteryInfoStartAddress + 2).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.BATTERY_BMS_PWR_CHRG_MAX, new FloatDoublewordElement(batteryInfoStartAddress + 4).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.BATTERY_BMS_PWR_D_CHA_MAX, new FloatDoublewordElement(batteryInfoStartAddress + 6).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.BATTERY_BMS_PWR_RGN_MAX, new FloatDoublewordElement(batteryInfoStartAddress + 8).wordOrder(WordOrder.LSWMSW)),
						m(Ess.ChannelId.SOC, new FloatDoublewordElement(batteryInfoStartAddress + 10).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.BATTERY_BMS_SOH, new FloatDoublewordElement(batteryInfoStartAddress + 12).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.BATTERY_BMS_ST_BAT, new FloatDoublewordElement(batteryInfoStartAddress + 14).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.BATTERY_BMS_T_MAX_PACK, new FloatDoublewordElement(batteryInfoStartAddress + 16).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.BATTERY_BMS_T_MIN_PACK, new FloatDoublewordElement(batteryInfoStartAddress + 18).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.BATTERY_BMS_U_PACK, new FloatDoublewordElement(batteryInfoStartAddress + 20).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.BATTERY_BMS_WRN, new FloatDoublewordElement(batteryInfoStartAddress + 22).wordOrder(WordOrder.LSWMSW))
						),
				new FC4ReadInputRegistersTask(inverterInfoStartAddress, Priority.HIGH,  
						m(ChannelId.INVERTER_ACTIVE_POWER, new FloatDoublewordElement(inverterInfoStartAddress).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_DC1_FAULT_VALUE, new FloatDoublewordElement(inverterInfoStartAddress + 2).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_DC2_FAULT_VALUE, new FloatDoublewordElement(inverterInfoStartAddress + 4).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_ERROR_MESSAGE_1H, new FloatDoublewordElement(inverterInfoStartAddress + 6).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_ERROR_MESSAGE_1L, new FloatDoublewordElement(inverterInfoStartAddress + 8).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_ERROR_MESSAGE_2H, new FloatDoublewordElement(inverterInfoStartAddress + 10).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_ERROR_MESSAGE_2L, new FloatDoublewordElement(inverterInfoStartAddress + 12).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_F_AC_1, new FloatDoublewordElement(inverterInfoStartAddress + 14).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_F_AC_2, new FloatDoublewordElement(inverterInfoStartAddress + 16).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_F_AC_3, new FloatDoublewordElement(inverterInfoStartAddress + 18).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_GF1_FAULT_VALUE, new FloatDoublewordElement(inverterInfoStartAddress + 20).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_GF2_FAULT_VALUE, new FloatDoublewordElement(inverterInfoStartAddress + 22).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_GF3_FAULT_VALUE, new FloatDoublewordElement(inverterInfoStartAddress + 24).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_GFCI_FAULT_VALUE, new FloatDoublewordElement(inverterInfoStartAddress + 26).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_GV1_FAULT_VALUE, new FloatDoublewordElement(inverterInfoStartAddress + 28).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_GV2_FAULT_VALUE, new FloatDoublewordElement(inverterInfoStartAddress + 30).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_GV3_FAULT_VALUE, new FloatDoublewordElement(inverterInfoStartAddress + 32).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_P_AC, new FloatDoublewordElement(inverterInfoStartAddress + 34).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_P_AC_1, new FloatDoublewordElement(inverterInfoStartAddress + 36).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_P_AC_2, new FloatDoublewordElement(inverterInfoStartAddress + 38).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_P_AC_3, new FloatDoublewordElement(inverterInfoStartAddress + 40).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_TEMPERATURE, new FloatDoublewordElement(inverterInfoStartAddress + 42).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_TEMPERATURE_FAULT_VALUE, new FloatDoublewordElement(inverterInfoStartAddress + 44).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_V_AC_1, new FloatDoublewordElement(inverterInfoStartAddress + 46).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_V_AC_2, new FloatDoublewordElement(inverterInfoStartAddress + 48).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_V_AC_3, new FloatDoublewordElement(inverterInfoStartAddress + 50).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_V_DC_1, new FloatDoublewordElement(inverterInfoStartAddress + 52).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_V_DC_2, new FloatDoublewordElement(inverterInfoStartAddress + 54).wordOrder(WordOrder.LSWMSW))						
						)
				);
	}
	
	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		INVERTER_MODE(new Doc(). //
				option(getInverterModeInitial(), "Initial"). //
				option(getInverterModeWait(), "Wait"). //
				option(getInverterModeStartUp(), "Start up"). //
				option(getInverterModeNormal(), "Normal"). //
				option(getInverterModeOffGrid(), "Off grid"). //
				option(getInverterModeFault(), "Fault"). //
				option(getInverterModePermanentFault(), "Permanent fault"). //
				option(getInverterModeUpdateMaster(), "Program update of master controller"). //
				option(getInverterModeUpdateSlave(), "Program update of slave controller")), //
		SET_ACTIVE_POWER(new Doc().unit(Unit.WATT)), //
		ICU_RUN(new Doc().unit(Unit.ON_OFF)), //
		ICU_ENABLED(new Doc().unit(Unit.ON_OFF)), //	
		BATTERY_BMS_ERR(new Doc().unit(Unit.NONE)),
		BATTERY_BMS_I_ACT(new Doc().unit(Unit.AMPERE)),
		BATTERY_BMS_PWR_CHRG_MAX(new Doc().unit(Unit.WATT)),
		BATTERY_BMS_PWR_D_CHA_MAX(new Doc().unit(Unit.WATT)),
		BATTERY_BMS_PWR_RGN_MAX(new Doc().unit(Unit.WATT)),
		BATTERY_BMS_SOH(new Doc().unit(Unit.PERCENT)),
		BATTERY_BMS_ST_BAT(new Doc().unit(Unit.NONE)),
		BATTERY_BMS_T_MAX_PACK(new Doc().unit(Unit.DEGREE_CELCIUS)),
		BATTERY_BMS_T_MIN_PACK(new Doc().unit(Unit.DEGREE_CELCIUS)),
		BATTERY_BMS_U_PACK(new Doc().unit(Unit.VOLT)),
		BATTERY_BMS_WRN(new Doc().unit(Unit.NONE)),
		
		INVERTER_ACTIVE_POWER(new Doc().unit(Unit.WATT)),
		INVERTER_DC1_FAULT_VALUE(new Doc().unit(Unit.NONE)),
		INVERTER_DC2_FAULT_VALUE(new Doc().unit(Unit.NONE)),
		INVERTER_ERROR_MESSAGE_1H(new Doc().unit(Unit.NONE)),
		INVERTER_ERROR_MESSAGE_1L(new Doc().unit(Unit.NONE)),
		INVERTER_ERROR_MESSAGE_2H(new Doc().unit(Unit.NONE)),
		INVERTER_ERROR_MESSAGE_2L(new Doc().unit(Unit.NONE)),
		INVERTER_F_AC_1(new Doc().unit(Unit.NONE)), 
		INVERTER_F_AC_2(new Doc().unit(Unit.NONE)),
		INVERTER_F_AC_3(new Doc().unit(Unit.NONE)),
		INVERTER_GF1_FAULT_VALUE(new Doc().unit(Unit.NONE)),
		INVERTER_GF2_FAULT_VALUE(new Doc().unit(Unit.NONE)),
		INVERTER_GF3_FAULT_VALUE(new Doc().unit(Unit.NONE)),
		INVERTER_GFCI_FAULT_VALUE(new Doc().unit(Unit.NONE)),
		INVERTER_GV1_FAULT_VALUE(new Doc().unit(Unit.NONE)),
		INVERTER_GV2_FAULT_VALUE(new Doc().unit(Unit.NONE)),
		INVERTER_GV3_FAULT_VALUE(new Doc().unit(Unit.NONE)),
		INVERTER_P_AC(new Doc().unit(Unit.WATT)),
		INVERTER_P_AC_1(new Doc().unit(Unit.WATT)),
		INVERTER_P_AC_2(new Doc().unit(Unit.WATT)),
		INVERTER_P_AC_3(new Doc().unit(Unit.WATT)),
		INVERTER_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		INVERTER_TEMPERATURE_FAULT_VALUE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		INVERTER_V_AC_1(new Doc().unit(Unit.VOLT)),
		INVERTER_V_AC_2(new Doc().unit(Unit.VOLT)),
		INVERTER_V_AC_3(new Doc().unit(Unit.VOLT)),
		INVERTER_V_DC_1(new Doc().unit(Unit.VOLT)),
		INVERTER_V_DC_2(new Doc().unit(Unit.VOLT));

		



		public static final int INVERTER_MODE_INITIAL = 0;
		public static final int INVERTER_MODE_WAIT = 1;
		public static final int INVERTER_MODE_START_UP = 2;
		public static final int INVERTER_MODE_NORMAL = 3;	
		public static final int INVERTER_MODE_OFF_GRID = 4;
		public static final int INVERTER_MODE_FAULT = 5;
		public static final int INVERTER_MODE_PERMANENT_FAULT = 6;
		public static final int INVERTER_MODE_UPDATE_MASTER = 7;	
		public static final int INVERTER_MODE_UPDATE_SLAVE = 8;

		private final Doc doc;

		@Override
		public Doc doc() {
			return this.doc;
		}

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		private static int getInverterModeUpdateSlave() {
			return INVERTER_MODE_UPDATE_SLAVE;
		}

		private static int getInverterModeUpdateMaster() {
			return INVERTER_MODE_UPDATE_MASTER;
		}

		private static int getInverterModePermanentFault() {
			return INVERTER_MODE_PERMANENT_FAULT;
		}

		private static int getInverterModeFault() {
			return INVERTER_MODE_FAULT;
		}

		private static int getInverterModeOffGrid() {
			return INVERTER_MODE_OFF_GRID;
		}

		private static int getInverterModeNormal() {
			return INVERTER_MODE_NORMAL;
		}

		private static int getInverterModeStartUp() {
			return INVERTER_MODE_START_UP;
		}

		private static int getInverterModeWait() {
			return INVERTER_MODE_WAIT;
		}

		private static int getInverterModeInitial() {
			return INVERTER_MODE_INITIAL;
		}
	}

	private int getBatteryInfoStartAddress() {return BATTERY_INFO_START_ADDRESS; }
	protected abstract int getAdressOffsetForBattery();
	private int getInverterInfoStartAddress() { return INVERTER_INFO_START_ADDRESS; }
	protected abstract int getAdressOffsetForInverter();

	protected int getIcuRunAddress() { return ICU_RUN_ADDRESS; }
	protected abstract int getIcuEnabledAddress();
	protected abstract int getIcuSetPowerAddress();
	protected abstract int getInverterModeAddress();


	private void addChannels() {
		Utils.initializeChannels(this).
		forEach(channel -> this.addChannel((Channel<?>) channel));;	
	}
}