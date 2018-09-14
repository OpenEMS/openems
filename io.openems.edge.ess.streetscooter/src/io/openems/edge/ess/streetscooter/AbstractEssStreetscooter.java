package io.openems.edge.ess.streetscooter;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC1ReadCoilsTask;
import io.openems.edge.bridge.modbus.api.task.FC2ReadInputsTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC5WriteCoilTask;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.OptionsEnum;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;

public abstract class AbstractEssStreetscooter extends AbstractOpenemsModbusComponent
		implements ManagedSymmetricEss, SymmetricEss, OpenemsComponent {

	protected static final int UNIT_ID = 100;
	protected static final int MAX_APPARENT_POWER = 12000;

	private static final int POWER_PRECISION = 100;
	private static final int ICU_RUN_ADDRESS = 4002;
	private static final int BATTERY_INFO_START_ADDRESS = 0;
	private static final int INVERTER_INFO_START_ADDRESS = 2000;

//	private final Logger log = LoggerFactory.getLogger(AbstractEssStreetscooter.class);

	private final PowerHandler powerHandler;

	private boolean readonly = false;

	public AbstractEssStreetscooter() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel((Channel<?>) channel));
		this.powerHandler = new PowerHandler(this);
	}

	protected void activate(ComponentContext context, String servicePid, String id, boolean enabled, boolean readonly,
			int unitId, ConfigurationAdmin cm, String modbusReference, String modbusId) {
		this.readonly = readonly;

		if (readonly) {
			// Do not allow Power in read-only mode
			this.getMaxApparentPower().setNextValue(0);
		}

		super.activate(context, servicePid, id, enabled, unitId, cm, modbusReference, modbusId);
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().value().asString() + ", mode:"
				+ this.channel(ChannelId.INVERTER_MODE).value().asOptionString() + "|L:"
				+ this.getActivePower().value().asString() //
		;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) {
		if (!this.readonly) {
			this.powerHandler.accept(activePower, reactivePower);
		}
	}

	@Override
	public int getPowerPrecision() {
		return AbstractEssStreetscooter.POWER_PRECISION;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		int batteryInfoStartAddress = BATTERY_INFO_START_ADDRESS + getAdressOffsetForBattery();
		int inverterInfoStartAddress = INVERTER_INFO_START_ADDRESS + getAdressOffsetForInverter();

		return new ModbusProtocol(this, //
				new FC1ReadCoilsTask(getIcuRunAddress(), Priority.HIGH,
						m(ChannelId.ICU_RUN, new CoilElement(getIcuRunAddress()))),
				new FC5WriteCoilTask(getIcuRunAddress(), m(ChannelId.ICU_RUN, new CoilElement(getIcuRunAddress()))),
				new FC1ReadCoilsTask(getIcuEnabledAddress(), Priority.HIGH,
						m(ChannelId.ICU_ENABLED, new CoilElement(getIcuEnabledAddress()))),
				new FC5WriteCoilTask(getIcuEnabledAddress(),
						m(ChannelId.ICU_ENABLED, new CoilElement(getIcuEnabledAddress()))),
				new FC4ReadInputRegistersTask(getInverterModeAddress(), Priority.HIGH,
						m(ChannelId.INVERTER_MODE,
								new FloatDoublewordElement(getInverterModeAddress()).wordOrder(WordOrder.LSWMSW))),
				new FC16WriteRegistersTask(getIcuSetPowerAddress(),
						m(ChannelId.INVERTER_SET_ACTIVE_POWER,
								new FloatDoublewordElement(getIcuSetPowerAddress()).wordOrder(WordOrder.LSWMSW))),
				new FC4ReadInputRegistersTask(batteryInfoStartAddress, Priority.HIGH,
						m(ChannelId.BATTERY_BMS_ERR,
								new FloatDoublewordElement(batteryInfoStartAddress).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.BATTERY_BMS_I_ACT,
								new FloatDoublewordElement(batteryInfoStartAddress + 2).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.BATTERY_BMS_PWR_CHRG_MAX,
								new FloatDoublewordElement(batteryInfoStartAddress + 4).wordOrder(WordOrder.LSWMSW)),
						m(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER,
								new FloatDoublewordElement(batteryInfoStartAddress + 6).wordOrder(WordOrder.LSWMSW)),
						m(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER,
								new FloatDoublewordElement(batteryInfoStartAddress + 8).wordOrder(WordOrder.LSWMSW), ElementToChannelConverter.INVERT),
						m(SymmetricEss.ChannelId.SOC,
								new FloatDoublewordElement(batteryInfoStartAddress + 10).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.BATTERY_BMS_SOH,
								new FloatDoublewordElement(batteryInfoStartAddress + 12).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.BATTERY_BMS_ST_BAT,
								new FloatDoublewordElement(batteryInfoStartAddress + 14).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.BATTERY_BMS_T_MAX_PACK,
								new FloatDoublewordElement(batteryInfoStartAddress + 16).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.BATTERY_BMS_T_MIN_PACK,
								new FloatDoublewordElement(batteryInfoStartAddress + 18).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.BATTERY_BMS_U_PACK,
								new FloatDoublewordElement(batteryInfoStartAddress + 20).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.BATTERY_BMS_WRN,
								new FloatDoublewordElement(batteryInfoStartAddress + 22).wordOrder(WordOrder.LSWMSW))),
				new FC4ReadInputRegistersTask(inverterInfoStartAddress, Priority.HIGH,
						m(ChannelId.INVERTER_ACTIVE_POWER,
								new FloatDoublewordElement(inverterInfoStartAddress).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_DC1_FAULT_VALUE,
								new FloatDoublewordElement(inverterInfoStartAddress + 2).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_DC2_FAULT_VALUE,
								new FloatDoublewordElement(inverterInfoStartAddress + 4).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_ERROR_MESSAGE_1H,
								new FloatDoublewordElement(inverterInfoStartAddress + 6).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_ERROR_MESSAGE_1L,
								new FloatDoublewordElement(inverterInfoStartAddress + 8).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_ERROR_MESSAGE_2H,
								new FloatDoublewordElement(inverterInfoStartAddress + 10).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_ERROR_MESSAGE_2L,
								new FloatDoublewordElement(inverterInfoStartAddress + 12).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_F_AC_1,
								new FloatDoublewordElement(inverterInfoStartAddress + 14).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_F_AC_2,
								new FloatDoublewordElement(inverterInfoStartAddress + 16).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_F_AC_3,
								new FloatDoublewordElement(inverterInfoStartAddress + 18).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_GF1_FAULT_VALUE,
								new FloatDoublewordElement(inverterInfoStartAddress + 20).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_GF2_FAULT_VALUE,
								new FloatDoublewordElement(inverterInfoStartAddress + 22).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_GF3_FAULT_VALUE,
								new FloatDoublewordElement(inverterInfoStartAddress + 24).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_GFCI_FAULT_VALUE,
								new FloatDoublewordElement(inverterInfoStartAddress + 26).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_GV1_FAULT_VALUE,
								new FloatDoublewordElement(inverterInfoStartAddress + 28).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_GV2_FAULT_VALUE,
								new FloatDoublewordElement(inverterInfoStartAddress + 30).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_GV3_FAULT_VALUE,
								new FloatDoublewordElement(inverterInfoStartAddress + 32).wordOrder(WordOrder.LSWMSW)),
						m(SymmetricEss.ChannelId.ACTIVE_POWER,
								new FloatDoublewordElement(inverterInfoStartAddress + 34).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_P_AC_1,
								new FloatDoublewordElement(inverterInfoStartAddress + 36).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_P_AC_2,
								new FloatDoublewordElement(inverterInfoStartAddress + 38).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_P_AC_3,
								new FloatDoublewordElement(inverterInfoStartAddress + 40).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_TEMPERATURE,
								new FloatDoublewordElement(inverterInfoStartAddress + 42).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_TEMPERATURE_FAULT_VALUE,
								new FloatDoublewordElement(inverterInfoStartAddress + 44).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_V_AC_1,
								new FloatDoublewordElement(inverterInfoStartAddress + 46).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_V_AC_2,
								new FloatDoublewordElement(inverterInfoStartAddress + 48).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_V_AC_3,
								new FloatDoublewordElement(inverterInfoStartAddress + 50).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_V_DC_1,
								new FloatDoublewordElement(inverterInfoStartAddress + 52).wordOrder(WordOrder.LSWMSW)),
						m(ChannelId.INVERTER_V_DC_2,
								new FloatDoublewordElement(inverterInfoStartAddress + 54).wordOrder(WordOrder.LSWMSW))),
				new FC2ReadInputsTask(getIcuRunstateAddress(), Priority.HIGH,
						m(ChannelId.ICU_RUNSTATE, new CoilElement(getIcuRunstateAddress()))),
				new FC2ReadInputsTask(getInverterConnectedAddress(), Priority.HIGH,
						m(ChannelId.INVERTER_CONNECTED, new CoilElement(getInverterConnectedAddress()))),
				new FC2ReadInputsTask(getBatteryConnectedAddress(), Priority.HIGH,
						m(ChannelId.BATTERY_CONNECTED, new CoilElement(getBatteryConnectedAddress()))),
				new FC2ReadInputsTask(getBatteryOverloadAddress(), Priority.HIGH,
						m(ChannelId.BATTERY_OVERLOAD, new CoilElement(getBatteryOverloadAddress()))));
	}

	public enum InverterMode implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		INITIAL(0, "Initial"), //
		WAIT(1, "Wait"), //
		START_UP(2, "Start up"), //
		NORMAL(3, "Normal"), //
		OFF_GRID(4, "Off grid"), //
		FAULT(5, "Fault"), //
		PERMANENT_FAULT(6, "Permanent fault"), //
		UPDATE_MASTER(7, "Program update of master controller"), //
		UPDATE_SLAVE(8, "Program update of slave controller");

		private final int value;
		private final String option;

		private InverterMode(int value, String option) {
			this.value = value;
			this.option = option;
		}

		@Override
		public int getValue() {
			return value;
		}

		@Override
		public String getOption() {
			return option;
		}
	}

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		DEBUG_ICU_RUN(new Doc()), //
		ICU_RUN(new Doc().unit(Unit.ON_OFF) //
				// on each setNextWrite to the channel -> store the value in the DEBUG-channel
				.onInit(channel -> { //
					((BooleanWriteChannel) channel).onSetNextWrite(value -> {
						channel.getComponent().channel(ChannelId.DEBUG_ICU_RUN).setNextValue(value);
					});
				})), //
		DEBUG_ICU_ENABLED(new Doc()), //
		ICU_ENABLED(new Doc().unit(Unit.ON_OFF) //
				// on each setNextWrite to the channel -> store the value in the DEBUG-channel
				.onInit(channel -> { //
					((BooleanWriteChannel) channel).onSetNextWrite(value -> {
						channel.getComponent().channel(ChannelId.DEBUG_ICU_ENABLED).setNextValue(value);
					});
				})), //
		ICU_RUNSTATE(new Doc().unit(Unit.ON_OFF)), //
		BATTERY_BMS_ERR(new Doc().unit(Unit.NONE)), //
		BATTERY_BMS_I_ACT(new Doc().unit(Unit.AMPERE)), //
		BATTERY_BMS_PWR_CHRG_MAX(new Doc().unit(Unit.WATT)), //
		BATTERY_BMS_SOH(new Doc().unit(Unit.PERCENT)), //
		BATTERY_BMS_ST_BAT(new Doc().unit(Unit.NONE)), //
		BATTERY_BMS_T_MAX_PACK(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_BMS_T_MIN_PACK(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_BMS_U_PACK(new Doc().unit(Unit.VOLT)), //
		BATTERY_BMS_WRN(new Doc().unit(Unit.NONE)), //
		BATTERY_CONNECTED(new Doc().unit(Unit.ON_OFF)), //
		BATTERY_OVERLOAD(new Doc().unit(Unit.ON_OFF)), //

		INVERTER_MODE(new Doc().options(InverterMode.values())), //
		DEBUG_INVERTER_SET_ACTIVE_POWER(new Doc()), //
		INVERTER_SET_ACTIVE_POWER(new Doc().unit(Unit.WATT) //
				// on each setNextWrite to the channel -> store the value in the DEBUG-channel
				.onInit(channel -> { //
					((IntegerWriteChannel) channel).onSetNextWrite(value -> {
						channel.getComponent().channel(ChannelId.DEBUG_INVERTER_SET_ACTIVE_POWER).setNextValue(value);
					});
				})), //
		INVERTER_ACTIVE_POWER(new Doc().unit(Unit.WATT)), //
		INVERTER_DC1_FAULT_VALUE(new Doc().unit(Unit.NONE)), //
		INVERTER_DC2_FAULT_VALUE(new Doc().unit(Unit.NONE)), //
		INVERTER_ERROR_MESSAGE_1H(new Doc().unit(Unit.NONE)), //
		INVERTER_ERROR_MESSAGE_1L(new Doc().unit(Unit.NONE)), //
		INVERTER_ERROR_MESSAGE_2H(new Doc().unit(Unit.NONE)), //
		INVERTER_ERROR_MESSAGE_2L(new Doc().unit(Unit.NONE)), //
		INVERTER_F_AC_1(new Doc().unit(Unit.NONE)), //
		INVERTER_F_AC_2(new Doc().unit(Unit.NONE)), //
		INVERTER_F_AC_3(new Doc().unit(Unit.NONE)), //
		INVERTER_GF1_FAULT_VALUE(new Doc().unit(Unit.NONE)), //
		INVERTER_GF2_FAULT_VALUE(new Doc().unit(Unit.NONE)), //
		INVERTER_GF3_FAULT_VALUE(new Doc().unit(Unit.NONE)), //
		INVERTER_GFCI_FAULT_VALUE(new Doc().unit(Unit.NONE)), //
		INVERTER_GV1_FAULT_VALUE(new Doc().unit(Unit.NONE)), //
		INVERTER_GV2_FAULT_VALUE(new Doc().unit(Unit.NONE)), //
		INVERTER_GV3_FAULT_VALUE(new Doc().unit(Unit.NONE)), //
		INVERTER_P_AC(new Doc().unit(Unit.WATT)), //
		INVERTER_P_AC_1(new Doc().unit(Unit.WATT)), //
		INVERTER_P_AC_2(new Doc().unit(Unit.WATT)), //
		INVERTER_P_AC_3(new Doc().unit(Unit.WATT)), //
		INVERTER_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELSIUS)),
		INVERTER_TEMPERATURE_FAULT_VALUE(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		INVERTER_V_AC_1(new Doc().unit(Unit.VOLT)), //
		INVERTER_V_AC_2(new Doc().unit(Unit.VOLT)), //
		INVERTER_V_AC_3(new Doc().unit(Unit.VOLT)), //
		INVERTER_V_DC_1(new Doc().unit(Unit.VOLT)), //
		INVERTER_V_DC_2(new Doc().unit(Unit.VOLT)), //
		INVERTER_CONNECTED(new Doc().unit(Unit.ON_OFF)),
		SYSTEM_STATE_INFORMATION(new Doc().setWritable().unit(Unit.NONE));

		private final Doc doc;

		@Override
		public Doc doc() {
			return this.doc;
		}

		private ChannelId(Doc doc) {
			this.doc = doc;
		}
	}

	protected abstract int getAdressOffsetForBattery();

	protected abstract int getAdressOffsetForInverter();

	protected int getIcuRunAddress() {
		return ICU_RUN_ADDRESS;
	}

	protected abstract int getIcuEnabledAddress();

	protected abstract int getIcuSetPowerAddress();

	protected abstract int getBatteryOverloadAddress();

	protected abstract int getBatteryConnectedAddress();

	protected abstract int getInverterConnectedAddress();

	protected abstract int getIcuRunstateAddress();

	protected abstract int getInverterModeAddress();
	
	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}
	
	@Override
	protected void logError(Logger log, String message) {
		super.logError(log, message);
	}
}
