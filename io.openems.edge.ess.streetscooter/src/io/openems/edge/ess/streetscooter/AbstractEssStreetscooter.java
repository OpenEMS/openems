package io.openems.edge.ess.streetscooter;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
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
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

public abstract class AbstractEssStreetscooter extends AbstractOpenemsModbusComponent
		implements ManagedSymmetricEss, SymmetricEss, OpenemsComponent, ModbusSlave {

	protected static final int UNIT_ID = 100;
	protected static final int MAX_APPARENT_POWER = 11600;

	private static final int POWER_PRECISION = 1;
	private static final int ICU_RUN_ADDRESS = 4002;
	private static final int BATTERY_INFO_START_ADDRESS = 0;
	private static final int INVERTER_INFO_START_ADDRESS = 2000;

	// private final Logger log =
	// LoggerFactory.getLogger(AbstractEssStreetscooter.class);

	private final PowerHandler powerHandler;

	private boolean readonly = false;

	public AbstractEssStreetscooter() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				StrtsctrChannelId.values() //
		);
		this.channel(SymmetricEss.ChannelId.MAX_APPARENT_POWER)
				.setNextValue(AbstractEssStreetscooter.MAX_APPARENT_POWER);
		this.channel(SymmetricEss.ChannelId.GRID_MODE).setNextValue(GridMode.ON_GRID);

		this.powerHandler = new PowerHandler(this);
	}

	protected void activate(ComponentContext context, String id, String alias, boolean enabled, boolean readonly,
			int unitId, ConfigurationAdmin cm, String modbusReference, String modbusId) {
		this.readonly = readonly;

		if (readonly) {
			// Do not allow Power in read-only mode
			this._setMaxApparentPower(0);
		}

		super.activate(context, id, alias, enabled, unitId, cm, modbusReference, modbusId);
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().asString() + ", mode:"
				+ this.channel(StrtsctrChannelId.INVERTER_MODE).value().asOptionString() + "|L:"
				+ this.getActivePower().asString() //
		;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsException {
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
						m(StrtsctrChannelId.ICU_RUN, new CoilElement(getIcuRunAddress()))),
				new FC5WriteCoilTask(getIcuRunAddress(),
						m(StrtsctrChannelId.ICU_RUN, new CoilElement(getIcuRunAddress()))),
				new FC1ReadCoilsTask(getIcuEnabledAddress(), Priority.HIGH,
						m(StrtsctrChannelId.ICU_ENABLED, new CoilElement(getIcuEnabledAddress()))),
				new FC5WriteCoilTask(getIcuEnabledAddress(),
						m(StrtsctrChannelId.ICU_ENABLED, new CoilElement(getIcuEnabledAddress()))),
				new FC16WriteRegistersTask(getIcuSetPowerAddress(),
						m(StrtsctrChannelId.INVERTER_SET_ACTIVE_POWER,
								new FloatDoublewordElement(getIcuSetPowerAddress()).wordOrder(WordOrder.LSWMSW))),
				new FC4ReadInputRegistersTask(batteryInfoStartAddress, Priority.LOW,
						m(StrtsctrChannelId.BATTERY_BMS_ERR,
								new FloatDoublewordElement(batteryInfoStartAddress).wordOrder(WordOrder.LSWMSW)),
						m(StrtsctrChannelId.BATTERY_BMS_I_ACT,
								new FloatDoublewordElement(batteryInfoStartAddress + 2).wordOrder(WordOrder.LSWMSW)),
						m(StrtsctrChannelId.BATTERY_BMS_PWR_CHRG_MAX,
								new FloatDoublewordElement(batteryInfoStartAddress + 4).wordOrder(WordOrder.LSWMSW)),
						m(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER,
								new FloatDoublewordElement(batteryInfoStartAddress + 6).wordOrder(WordOrder.LSWMSW)),
						m(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER,
								new FloatDoublewordElement(batteryInfoStartAddress + 8).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.INVERT),
						m(SymmetricEss.ChannelId.SOC,
								new FloatDoublewordElement(batteryInfoStartAddress + 10).wordOrder(WordOrder.LSWMSW)),
						m(StrtsctrChannelId.BATTERY_BMS_SOH,
								new FloatDoublewordElement(batteryInfoStartAddress + 12).wordOrder(WordOrder.LSWMSW)),
						m(StrtsctrChannelId.BATTERY_BMS_ST_BAT,
								new FloatDoublewordElement(batteryInfoStartAddress + 14).wordOrder(WordOrder.LSWMSW)),
						m(StrtsctrChannelId.BATTERY_BMS_T_MAX_PACK,
								new FloatDoublewordElement(batteryInfoStartAddress + 16).wordOrder(WordOrder.LSWMSW)),
						m(StrtsctrChannelId.BATTERY_BMS_T_MIN_PACK,
								new FloatDoublewordElement(batteryInfoStartAddress + 18).wordOrder(WordOrder.LSWMSW)),
						m(StrtsctrChannelId.BATTERY_BMS_U_PACK,
								new FloatDoublewordElement(batteryInfoStartAddress + 20).wordOrder(WordOrder.LSWMSW)),
						m(StrtsctrChannelId.BATTERY_BMS_WRN,
								new FloatDoublewordElement(batteryInfoStartAddress + 22).wordOrder(WordOrder.LSWMSW))),
				new FC4ReadInputRegistersTask(inverterInfoStartAddress, Priority.LOW,
						m(StrtsctrChannelId.INVERTER_ACTIVE_POWER,
								new FloatDoublewordElement(inverterInfoStartAddress).wordOrder(WordOrder.LSWMSW)),
						m(StrtsctrChannelId.INVERTER_DC1_FAULT_VALUE,
								new FloatDoublewordElement(inverterInfoStartAddress + 2).wordOrder(WordOrder.LSWMSW)),
						m(StrtsctrChannelId.INVERTER_DC2_FAULT_VALUE,
								new FloatDoublewordElement(inverterInfoStartAddress + 4).wordOrder(WordOrder.LSWMSW)),
						m(StrtsctrChannelId.INVERTER_ERROR_MESSAGE_1H,
								new FloatDoublewordElement(inverterInfoStartAddress + 6).wordOrder(WordOrder.LSWMSW)),
						m(StrtsctrChannelId.INVERTER_ERROR_MESSAGE_1L,
								new FloatDoublewordElement(inverterInfoStartAddress + 8).wordOrder(WordOrder.LSWMSW)),
						m(StrtsctrChannelId.INVERTER_ERROR_MESSAGE_2H,
								new FloatDoublewordElement(inverterInfoStartAddress + 10).wordOrder(WordOrder.LSWMSW)),
						m(StrtsctrChannelId.INVERTER_ERROR_MESSAGE_2L,
								new FloatDoublewordElement(inverterInfoStartAddress + 12).wordOrder(WordOrder.LSWMSW)),
						m(StrtsctrChannelId.INVERTER_F_AC_1,
								new FloatDoublewordElement(inverterInfoStartAddress + 14).wordOrder(WordOrder.LSWMSW)),
						m(StrtsctrChannelId.INVERTER_F_AC_2,
								new FloatDoublewordElement(inverterInfoStartAddress + 16).wordOrder(WordOrder.LSWMSW)),
						m(StrtsctrChannelId.INVERTER_F_AC_3,
								new FloatDoublewordElement(inverterInfoStartAddress + 18).wordOrder(WordOrder.LSWMSW)),
						m(StrtsctrChannelId.INVERTER_GF1_FAULT_VALUE,
								new FloatDoublewordElement(inverterInfoStartAddress + 20).wordOrder(WordOrder.LSWMSW)),
						m(StrtsctrChannelId.INVERTER_GF2_FAULT_VALUE,
								new FloatDoublewordElement(inverterInfoStartAddress + 22).wordOrder(WordOrder.LSWMSW)),
						m(StrtsctrChannelId.INVERTER_GF3_FAULT_VALUE,
								new FloatDoublewordElement(inverterInfoStartAddress + 24).wordOrder(WordOrder.LSWMSW)),
						m(StrtsctrChannelId.INVERTER_GFCI_FAULT_VALUE,
								new FloatDoublewordElement(inverterInfoStartAddress + 26).wordOrder(WordOrder.LSWMSW)),
						m(StrtsctrChannelId.INVERTER_GV1_FAULT_VALUE,
								new FloatDoublewordElement(inverterInfoStartAddress + 28).wordOrder(WordOrder.LSWMSW)),
						m(StrtsctrChannelId.INVERTER_GV2_FAULT_VALUE,
								new FloatDoublewordElement(inverterInfoStartAddress + 30).wordOrder(WordOrder.LSWMSW)),
						m(StrtsctrChannelId.INVERTER_GV3_FAULT_VALUE,
								new FloatDoublewordElement(inverterInfoStartAddress + 32).wordOrder(WordOrder.LSWMSW)),
						m(SymmetricEss.ChannelId.ACTIVE_POWER,
								new FloatDoublewordElement(inverterInfoStartAddress + 34).wordOrder(WordOrder.LSWMSW)),
						m(StrtsctrChannelId.INVERTER_P_AC_1,
								new FloatDoublewordElement(inverterInfoStartAddress + 36).wordOrder(WordOrder.LSWMSW)),
						m(StrtsctrChannelId.INVERTER_P_AC_2,
								new FloatDoublewordElement(inverterInfoStartAddress + 38).wordOrder(WordOrder.LSWMSW)),
						m(StrtsctrChannelId.INVERTER_P_AC_3,
								new FloatDoublewordElement(inverterInfoStartAddress + 40).wordOrder(WordOrder.LSWMSW)),
						m(StrtsctrChannelId.INVERTER_TEMPERATURE,
								new FloatDoublewordElement(inverterInfoStartAddress + 42).wordOrder(WordOrder.LSWMSW)),
						m(StrtsctrChannelId.INVERTER_TEMPERATURE_FAULT_VALUE,
								new FloatDoublewordElement(inverterInfoStartAddress + 44).wordOrder(WordOrder.LSWMSW)),
						m(StrtsctrChannelId.INVERTER_V_AC_1,
								new FloatDoublewordElement(inverterInfoStartAddress + 46).wordOrder(WordOrder.LSWMSW)),
						m(StrtsctrChannelId.INVERTER_V_AC_2,
								new FloatDoublewordElement(inverterInfoStartAddress + 48).wordOrder(WordOrder.LSWMSW)),
						m(StrtsctrChannelId.INVERTER_V_AC_3,
								new FloatDoublewordElement(inverterInfoStartAddress + 50).wordOrder(WordOrder.LSWMSW)),
						m(StrtsctrChannelId.INVERTER_V_DC_1,
								new FloatDoublewordElement(inverterInfoStartAddress + 52).wordOrder(WordOrder.LSWMSW)),
						// Ewon: 2055 / 3055
						m(StrtsctrChannelId.INVERTER_V_DC_2,
								new FloatDoublewordElement(inverterInfoStartAddress + 54).wordOrder(WordOrder.LSWMSW)),
						// Ewon: 2057 / 3057
						m(StrtsctrChannelId.INVERTER_MODE,
								new FloatDoublewordElement(inverterInfoStartAddress + 56).wordOrder(WordOrder.LSWMSW)),
						// Ewon: 2059 / 3059
						m(StrtsctrChannelId.ICU_STATUS,
								new FloatDoublewordElement(inverterInfoStartAddress + 58).wordOrder(WordOrder.LSWMSW))),
				new FC2ReadInputsTask(getIcuRunstateAddress(), Priority.LOW,
						m(StrtsctrChannelId.ICU_RUNSTATE, new CoilElement(getIcuRunstateAddress()))),
				new FC2ReadInputsTask(getInverterConnectedAddress(), Priority.LOW,
						m(StrtsctrChannelId.INVERTER_CONNECTED, new CoilElement(getInverterConnectedAddress()))),
				new FC2ReadInputsTask(getBatteryConnectedAddress(), Priority.LOW,
						m(StrtsctrChannelId.BATTERY_CONNECTED, new CoilElement(getBatteryConnectedAddress()))),
				new FC2ReadInputsTask(getBatteryOverloadAddress(), Priority.LOW,
						m(StrtsctrChannelId.BATTERY_OVERLOAD, new CoilElement(getBatteryOverloadAddress()))));
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

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	@Override
	protected void logError(Logger log, String message) {
		super.logError(log, message);
	}

	private int invalidIcuStatusCounter = 0;

	@Override
	public Constraint[] getStaticConstraints() throws OpenemsException {
		/*
		 * Increase the invalidIcuStatusCounter if the ICU_STATUS is not ok.
		 */
		IntegerReadChannel icuStatusChannel = this.channel(StrtsctrChannelId.ICU_STATUS);
		int icuStatus = icuStatusChannel.value().orElse(0);
		switch (icuStatus) {
		case 20:
		case 21:
		case 120:
		case 122:
		case 200:
		case 201:
			invalidIcuStatusCounter = 0;

		case 0:
		case 101:
		default:
			invalidIcuStatusCounter++;
		}

		/*
		 * If device is in read-only mode or the ICU_STATUS was repeatedly not ok ->
		 * block any charging/discharging
		 */
		if (this.readonly || invalidIcuStatusCounter > 10) {
			return new Constraint[] { //
					this.createPowerConstraint("Wrong ICU_STATUS or disabled: " + icuStatus, Phase.ALL, Pwr.ACTIVE,
							Relationship.EQUALS, 0), //
					this.createPowerConstraint("Wrong ICU_STATUS or disabled: " + icuStatus, Phase.ALL, Pwr.REACTIVE,
							Relationship.EQUALS, 0) //
			};

		} else {
			return Power.NO_CONSTRAINTS;
		}
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable( //
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(AbstractEssStreetscooter.class, accessMode, 300) //
						.build());
	}
}
