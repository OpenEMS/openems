package io.openems.edge.goodwe.ess;

import java.util.HashSet;
import java.util.Set;

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

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.goodwe.charger.AbstractGoodWeEtCharger;
import io.openems.edge.goodwe.ess.applypower.ApplyPowerStateMachine;
import io.openems.edge.goodwe.ess.applypower.Context;
import io.openems.edge.goodwe.ess.enums.BatteryMode;
import io.openems.edge.goodwe.ess.enums.GoodweType;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "GoodWe.Ess", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
) //
public class GoodWeEssImpl extends AbstractOpenemsModbusComponent implements GoodWeEss, HybridEss, ManagedSymmetricEss,
		SymmetricEss, OpenemsComponent, TimedataProvider, EventHandler {

	private final Logger log = LoggerFactory.getLogger(GoodWeEssImpl.class);

	private Config config;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	private Power power;

	private final CalculateEnergyFromPower calculateAcChargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY);
	private final CalculateEnergyFromPower calculateAcDischargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY);

	private final Set<AbstractGoodWeEtCharger> chargers = new HashSet<>();

	private final ApplyPowerStateMachine applyPowerStateMachine = new ApplyPowerStateMachine(
			ApplyPowerStateMachine.State.UNDEFINED);

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.unit_id(), this.cm, "Modbus",
				config.modbus_id());
		this.config = config;
		this._setCapacity(this.config.capacity());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public GoodWeEssImpl() throws OpenemsNamedException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				HybridEss.ChannelId.values(), //
				GoodWeEss.ChannelId.values() //
		);
	}

	public String getModbusBridgeId() {
		return this.config.modbus_id();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this, //

				new FC3ReadRegistersTask(35001, Priority.ONCE, //
						m(SymmetricEss.ChannelId.MAX_APPARENT_POWER, new UnsignedWordElement(35001)), //
						new DummyRegisterElement(35002), //
						m(GoodWeEss.ChannelId.SERIAL_NUMBER, new StringWordElement(35003, 8)), //
						m(GoodWeEss.ChannelId.GOODWE_TYPE, new StringWordElement(35011, 5),
								new ElementToChannelConverter((value) -> {
									String stringValue = TypeUtils.<String>getAsType(OpenemsType.STRING, value);
									switch (stringValue) {
									case "GW10K-BT":
										this.logInfo(this.log, "Identified GoodWe GW10K-BT");
										return GoodweType.GOODWE_10K_BT;
									case "GW10K-ET":
										this.logInfo(this.log, "Identified GoodWe GW10K-ET");
										return GoodweType.GOODWE_10K_ET;
									default:
										this.logError(this.log, "Unable to identify GoodWe [" + stringValue + "]");
										return GoodweType.UNDEFINED;
									}
								}))), //

				new FC3ReadRegistersTask(35111, Priority.LOW, //
						m(GoodWeEss.ChannelId.V_PV3, new UnsignedWordElement(35111),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWeEss.ChannelId.I_PV3, new UnsignedWordElement(35112),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						new DummyRegisterElement(35113, 35114), //
						m(GoodWeEss.ChannelId.V_PV4, new UnsignedWordElement(35115),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWeEss.ChannelId.I_PV4, new UnsignedWordElement(35116),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						new DummyRegisterElement(35117, 35118), //
						m(GoodWeEss.ChannelId.PV_MODE, new UnsignedDoublewordElement(35119))), //

				new FC3ReadRegistersTask(35136, Priority.HIGH, //
						m(SymmetricEss.ChannelId.GRID_MODE, new UnsignedWordElement(35136), //
								new ElementToChannelConverter((value) -> {
									Integer intValue = TypeUtils.<Integer>getAsType(OpenemsType.INTEGER, value);
									if (intValue != null) {
										switch (intValue) {
										case 0:
											return GridMode.OFF_GRID;
										case 1:
											return GridMode.ON_GRID;
										case 2:
											return GridMode.UNDEFINED;
										}
									}
									return GridMode.UNDEFINED;
								}))), //
				new FC3ReadRegistersTask(35138, Priority.LOW, //
						m(GoodWeEss.ChannelId.TOTAL_INV_POWER, new SignedWordElement(35138)), //
						new DummyRegisterElement(35139), //
						m(GoodWeEss.ChannelId.AC_ACTIVE_POWER, new SignedWordElement(35140), //
								ElementToChannelConverter.INVERT), //
						new DummyRegisterElement(35141), //
						m(GoodWeEss.ChannelId.AC_REACTIVE_POWER, new SignedWordElement(35142), //
								ElementToChannelConverter.INVERT), //
						new DummyRegisterElement(35143), //
						m(GoodWeEss.ChannelId.AC_APPARENT_POWER, new SignedWordElement(35144), //
								ElementToChannelConverter.INVERT), //
						m(GoodWeEss.ChannelId.BACK_UP_V_LOAD_R, new UnsignedWordElement(35145), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWeEss.ChannelId.BACK_UP_I_LOAD_R, new UnsignedWordElement(35146),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWeEss.ChannelId.BACK_UP_F_LOAD_R, new UnsignedWordElement(35147),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWeEss.ChannelId.LOAD_MODE_R, new UnsignedWordElement(35148)), //
						new DummyRegisterElement(35149), //
						m(GoodWeEss.ChannelId.BACK_UP_P_LOAD_R, new SignedWordElement(35150)), //
						m(GoodWeEss.ChannelId.BACK_UP_V_LOAD_S, new UnsignedWordElement(35151),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWeEss.ChannelId.BACK_UP_I_LOAD_S, new UnsignedWordElement(35152),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWeEss.ChannelId.BACK_UP_F_LOAD_S, new UnsignedWordElement(35153),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWeEss.ChannelId.LOAD_MODE_S, new UnsignedWordElement(35154)), //
						new DummyRegisterElement(35155), //
						m(GoodWeEss.ChannelId.BACK_UP_P_LOAD_S, new SignedWordElement(35156)), //
						m(GoodWeEss.ChannelId.BACK_UP_V_LOAD_T, new UnsignedWordElement(35157),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWeEss.ChannelId.BACK_UP_I_LOAD_T, new UnsignedWordElement(35158),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWeEss.ChannelId.BACK_UP_F_LOAD_T, new UnsignedWordElement(35159),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWeEss.ChannelId.LOAD_MODE_T, new UnsignedWordElement(35160)), //
						new DummyRegisterElement(35161), //
						m(GoodWeEss.ChannelId.BACK_UP_P_LOAD_T, new SignedWordElement(35162)), //
						new DummyRegisterElement(35163), //
						m(GoodWeEss.ChannelId.P_LOAD_R, new SignedWordElement(35164)), //
						new DummyRegisterElement(35165), //
						m(GoodWeEss.ChannelId.P_LOAD_S, new SignedWordElement(35166)), //
						new DummyRegisterElement(35167), //
						m(GoodWeEss.ChannelId.P_LOAD_T, new SignedWordElement(35168)), //
						new DummyRegisterElement(35169), //
						m(GoodWeEss.ChannelId.TOTAL_BACK_UP_LOAD, new SignedWordElement(35170)), //
						new DummyRegisterElement(35171), //
						m(GoodWeEss.ChannelId.TOTAL_LOAD_POWER, new SignedWordElement(35172)), //
						m(GoodWeEss.ChannelId.UPS_LOAD_PERCENT, new UnsignedWordElement(35173),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2)), //

				new FC3ReadRegistersTask(35180, Priority.HIGH, //
						m(GoodWeEss.ChannelId.V_BATTERY1, new UnsignedWordElement(35180),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWeEss.ChannelId.I_BATTERY1, new SignedWordElement(35181),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						new DummyRegisterElement(35182), //
						m(GoodWeEss.ChannelId.P_BATTERY1, new SignedWordElement(35183)), //
						m(GoodWeEss.ChannelId.BATTERY_MODE, new UnsignedWordElement(35184))), //

				new FC3ReadRegistersTask(35185, Priority.LOW, //
						m(GoodWeEss.ChannelId.WARNING_CODE, new UnsignedWordElement(35185)), //
						m(GoodWeEss.ChannelId.SAFETY_COUNTRY, new UnsignedWordElement(35186)), //
						m(GoodWeEss.ChannelId.WORK_MODE, new UnsignedWordElement(35187)), //
						m(GoodWeEss.ChannelId.OPERATION_MODE, new UnsignedDoublewordElement(35188))), //

				new FC3ReadRegistersTask(35206, Priority.LOW, //
						m(HybridEss.ChannelId.DC_CHARGE_ENERGY, new UnsignedDoublewordElement(35206), //
								ElementToChannelConverter.SCALE_FACTOR_2), //
						new DummyRegisterElement(35208), //
						m(HybridEss.ChannelId.DC_DISCHARGE_ENERGY, new UnsignedDoublewordElement(35209),
								ElementToChannelConverter.SCALE_FACTOR_2)), //

				new FC3ReadRegistersTask(36003, Priority.LOW, //
						m(GoodWeEss.ChannelId.B_METER_COMMUNICATE_STATUS, new UnsignedWordElement(36003)), //
						m(GoodWeEss.ChannelId.METER_COMMUNICATE_STATUS, new UnsignedWordElement(36004))), //

				new FC3ReadRegistersTask(37001, Priority.LOW,
						m(GoodWeEss.ChannelId.BATTERY_TYPE_INDEX, new UnsignedWordElement(37001)), //
						m(GoodWeEss.ChannelId.BMS_STATUS, new UnsignedWordElement(37002)), //
						m(GoodWeEss.ChannelId.BMS_PACK_TEMPERATURE, new UnsignedWordElement(37003),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWeEss.ChannelId.BMS_CHARGE_IMAX, new UnsignedWordElement(37004)), //
						m(GoodWeEss.ChannelId.BMS_DISCHARGE_IMAX, new UnsignedWordElement(37005))), //

				new FC3ReadRegistersTask(37007, Priority.HIGH, //
						m(SymmetricEss.ChannelId.SOC, new UnsignedWordElement(37007), new ElementToChannelConverter(
								// element -> channel
								value -> {
									// Set SoC to undefined if there is No Battery
									EnumReadChannel batteryModeChannel = this.channel(GoodWeEss.ChannelId.BATTERY_MODE);
									BatteryMode batteryMode = batteryModeChannel.value().asEnum();
									if (batteryMode == BatteryMode.NO_BATTERY || batteryMode == BatteryMode.UNDEFINED) {
										return null;
									} else {
										return value;
									}
								},
								// channel -> element
								value -> value))), //
				new FC3ReadRegistersTask(37008, Priority.LOW, //
						m(GoodWeEss.ChannelId.BMS_SOH, new UnsignedWordElement(37008)), //
						m(GoodWeEss.ChannelId.BMS_BATTERY_STRINGS, new UnsignedWordElement(37009))), //

				new FC16WriteRegistersTask(47000, //
						m(GoodWeEss.ChannelId.APP_MODE_INDEX, new UnsignedWordElement(47000)), //
						m(GoodWeEss.ChannelId.METER_CHECK_VALUE, new UnsignedWordElement(47001)), //
						m(GoodWeEss.ChannelId.WMETER_CONNECT_CHECK_FLAG, new UnsignedWordElement(47002))), //

				new FC3ReadRegistersTask(47000, Priority.LOW, //
						m(GoodWeEss.ChannelId.APP_MODE_INDEX, new UnsignedWordElement(47000)), //
						m(GoodWeEss.ChannelId.METER_CHECK_VALUE, new UnsignedWordElement(47001)), //
						m(GoodWeEss.ChannelId.WMETER_CONNECT_CHECK_FLAG, new UnsignedWordElement(47002))), //

				new FC16WriteRegistersTask(47500, //
						m(GoodWeEss.ChannelId.STOP_SOC_PROTECT, new UnsignedWordElement(47500)), //
						new DummyRegisterElement(47501, 47508), //
						m(GoodWeEss.ChannelId.FEED_POWER_ENABLE, new UnsignedWordElement(47509)), //
						m(GoodWeEss.ChannelId.FEED_POWER_PARA, new UnsignedWordElement(47510)), //
						m(GoodWeEss.ChannelId.EMS_POWER_MODE, new UnsignedWordElement(47511)), //
						m(GoodWeEss.ChannelId.EMS_POWER_SET, new UnsignedWordElement(47512))), //

				new FC16WriteRegistersTask(47531, //
						m(GoodWeEss.ChannelId.SOC_START_TO_FORCE_CHARGE, new UnsignedWordElement(47531)), //
						m(GoodWeEss.ChannelId.SOC_STOP_TO_FORCE_CHARGE, new UnsignedWordElement(47532)), //
						m(GoodWeEss.ChannelId.CLEAR_ALL_ECONOMIC_MODE, new UnsignedWordElement(47533))), //

				new FC3ReadRegistersTask(47500, Priority.LOW,
						m(GoodWeEss.ChannelId.STOP_SOC_PROTECT, new UnsignedWordElement(47500)), //
						new DummyRegisterElement(47501, 47508), //
						m(GoodWeEss.ChannelId.FEED_POWER_ENABLE, new UnsignedWordElement(47509)), //
						m(GoodWeEss.ChannelId.FEED_POWER_PARA, new UnsignedWordElement(47510))), //

				new FC3ReadRegistersTask(47511, Priority.HIGH,
						m(GoodWeEss.ChannelId.EMS_POWER_MODE, new UnsignedWordElement(47511)), //
						m(GoodWeEss.ChannelId.EMS_POWER_SET, new UnsignedWordElement(47512))), //

				new FC3ReadRegistersTask(47531, Priority.LOW,
						m(GoodWeEss.ChannelId.SOC_START_TO_FORCE_CHARGE, new UnsignedWordElement(47531)), //
						m(GoodWeEss.ChannelId.SOC_STOP_TO_FORCE_CHARGE, new UnsignedWordElement(47532))), //

				new FC16WriteRegistersTask(47900, //
						m(GoodWeEss.ChannelId.BMS_VERSION, new UnsignedWordElement(47900)), //
						m(GoodWeEss.ChannelId.BATT_STRINGS_RS485, new UnsignedWordElement(47901)), //
						m(GoodWeEss.ChannelId.WBMS_BAT_CHARGE_VMAX, new UnsignedWordElement(47902)), //
						m(GoodWeEss.ChannelId.WBMS_BAT_CHARGE_IMAX, new UnsignedWordElement(47903)), //
						m(GoodWeEss.ChannelId.WBMS_BAT_DISCHARGE_VMIN, new UnsignedWordElement(47904)), //
						m(GoodWeEss.ChannelId.WBMS_BAT_DISCHARGE_IMAX, new UnsignedWordElement(47905)), //
						m(GoodWeEss.ChannelId.WBMS_BAT_VOLTAGE, new UnsignedWordElement(47906)), //
						m(GoodWeEss.ChannelId.WBMS_BAT_CURRENT, new UnsignedWordElement(47907)), //
						m(GoodWeEss.ChannelId.WBMS_BAT_SOC, new UnsignedWordElement(47908)), //
						m(GoodWeEss.ChannelId.WBMS_BAT_SOH, new UnsignedWordElement(47909)), //
						m(GoodWeEss.ChannelId.WBMS_BAT_TEMPERATURE, new UnsignedWordElement(47910)), //
						m(new BitsWordElement(47911, this) //
								.bit(0, GoodWeEss.ChannelId.STATE_58) //
								.bit(1, GoodWeEss.ChannelId.STATE_59) //
								.bit(2, GoodWeEss.ChannelId.STATE_60) //
								.bit(3, GoodWeEss.ChannelId.STATE_61) //
								.bit(4, GoodWeEss.ChannelId.STATE_62) //
								.bit(5, GoodWeEss.ChannelId.STATE_63) //
								.bit(6, GoodWeEss.ChannelId.STATE_64) //
								.bit(7, GoodWeEss.ChannelId.STATE_65) //
								.bit(8, GoodWeEss.ChannelId.STATE_66) //
								.bit(9, GoodWeEss.ChannelId.STATE_67) //
								.bit(10, GoodWeEss.ChannelId.STATE_68) //
								.bit(11, GoodWeEss.ChannelId.STATE_69)), //
						new DummyRegisterElement(47912), //
						m(new BitsWordElement(47913, this) //
								.bit(0, GoodWeEss.ChannelId.STATE_42) //
								.bit(1, GoodWeEss.ChannelId.STATE_43) //
								.bit(2, GoodWeEss.ChannelId.STATE_44) //
								.bit(3, GoodWeEss.ChannelId.STATE_45) //
								.bit(4, GoodWeEss.ChannelId.STATE_46) //
								.bit(5, GoodWeEss.ChannelId.STATE_47) //
								.bit(6, GoodWeEss.ChannelId.STATE_48) //
								.bit(7, GoodWeEss.ChannelId.STATE_49) //
								.bit(8, GoodWeEss.ChannelId.STATE_50) //
								.bit(9, GoodWeEss.ChannelId.STATE_51) //
								.bit(10, GoodWeEss.ChannelId.STATE_52) //
								.bit(11, GoodWeEss.ChannelId.STATE_53) //
								.bit(12, GoodWeEss.ChannelId.STATE_54) //
								.bit(13, GoodWeEss.ChannelId.STATE_55) //
								.bit(14, GoodWeEss.ChannelId.STATE_56) //
								.bit(15, GoodWeEss.ChannelId.STATE_57)), //
						new DummyRegisterElement(47914), //
						m(new BitsWordElement(47915, this) //
								.bit(0, GoodWeEss.ChannelId.STATE_79) //
								.bit(1, GoodWeEss.ChannelId.STATE_80) //
								.bit(2, GoodWeEss.ChannelId.STATE_81))), //

				new FC3ReadRegistersTask(47902, Priority.LOW, //
						m(GoodWeEss.ChannelId.WBMS_BAT_CHARGE_VMAX, new UnsignedWordElement(47902)), //
						m(GoodWeEss.ChannelId.WBMS_BAT_CHARGE_IMAX, new UnsignedWordElement(47903)), //
						m(GoodWeEss.ChannelId.WBMS_BAT_DISCHARGE_VMIN, new UnsignedWordElement(47904)), //
						m(GoodWeEss.ChannelId.WBMS_BAT_DISCHARGE_IMAX, new UnsignedWordElement(47905)), //
						m(GoodWeEss.ChannelId.WBMS_BAT_VOLTAGE, new UnsignedWordElement(47906)), //
						m(GoodWeEss.ChannelId.WBMS_BAT_CURRENT, new UnsignedWordElement(47907)), //
						m(GoodWeEss.ChannelId.WBMS_BAT_SOC, new UnsignedWordElement(47908)), //
						m(GoodWeEss.ChannelId.WBMS_BAT_SOH, new UnsignedWordElement(47909)), //
						m(GoodWeEss.ChannelId.WBMS_BAT_TEMPERATURE, new UnsignedWordElement(47910)), //
						m(new BitsWordElement(47911, this) //
								.bit(0, GoodWeEss.ChannelId.STATE_58) //
								.bit(1, GoodWeEss.ChannelId.STATE_59) //
								.bit(2, GoodWeEss.ChannelId.STATE_60) //
								.bit(3, GoodWeEss.ChannelId.STATE_61) //
								.bit(4, GoodWeEss.ChannelId.STATE_62) //
								.bit(5, GoodWeEss.ChannelId.STATE_63) //
								.bit(6, GoodWeEss.ChannelId.STATE_64) //
								.bit(7, GoodWeEss.ChannelId.STATE_65) //
								.bit(8, GoodWeEss.ChannelId.STATE_66) //
								.bit(9, GoodWeEss.ChannelId.STATE_67) //
								.bit(10, GoodWeEss.ChannelId.STATE_68) //
								.bit(11, GoodWeEss.ChannelId.STATE_69)), //
						new DummyRegisterElement(47912), //
						m(new BitsWordElement(47913, this) //
								.bit(0, GoodWeEss.ChannelId.STATE_42) //
								.bit(1, GoodWeEss.ChannelId.STATE_43) //
								.bit(2, GoodWeEss.ChannelId.STATE_44) //
								.bit(3, GoodWeEss.ChannelId.STATE_45) //
								.bit(4, GoodWeEss.ChannelId.STATE_46) //
								.bit(5, GoodWeEss.ChannelId.STATE_47) //
								.bit(6, GoodWeEss.ChannelId.STATE_48) //
								.bit(7, GoodWeEss.ChannelId.STATE_49) //
								.bit(8, GoodWeEss.ChannelId.STATE_50) //
								.bit(9, GoodWeEss.ChannelId.STATE_51) //
								.bit(10, GoodWeEss.ChannelId.STATE_52) //
								.bit(11, GoodWeEss.ChannelId.STATE_53) //
								.bit(12, GoodWeEss.ChannelId.STATE_54) //
								.bit(13, GoodWeEss.ChannelId.STATE_55) //
								.bit(14, GoodWeEss.ChannelId.STATE_56) //
								.bit(15, GoodWeEss.ChannelId.STATE_57)), //
						new DummyRegisterElement(47914), //
						m(new BitsWordElement(47915, this) //
								.bit(0, GoodWeEss.ChannelId.STATE_79) //
								.bit(1, GoodWeEss.ChannelId.STATE_80) //
								.bit(2, GoodWeEss.ChannelId.STATE_81))));
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {
		int pvProduction = getPvProduction();
		int soc = this.getSoc().orElse(0);
		ApplyPowerStateMachine.State state = ApplyPowerStateMachine.evaluateState(this.getGoodweType(),
				config.readOnlyMode(), pvProduction, soc, activePower);

		// Store the current State
		this.channel(GoodWeEss.ChannelId.APPLY_POWER_STATE_MACHINE).setNextValue(state);

		// Prepare Context
		Context context = new Context(this, pvProduction, activePower);

		this.applyPowerStateMachine.forceNextState(state);
		this.applyPowerStateMachine.run(context); // apply the force next state
		this.applyPowerStateMachine.run(context); // execute correct handler

		IntegerWriteChannel emsPowerSetChannel = this.channel(GoodWeEss.ChannelId.EMS_POWER_SET);
		emsPowerSetChannel.setNextWriteValue(context.getEssPowerSet());

		EnumWriteChannel emsPowerModeChannel = this.channel(GoodWeEss.ChannelId.EMS_POWER_MODE);
		emsPowerModeChannel.setNextWriteValue(context.getNextPowerMode());
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().asString() //
				+ "|L:" + this.getActivePower().asString() //
				+ "|" + this.getGridModeChannel().value().asOptionString()//
				+ "|Allowed:" + this.getAllowedChargePower().asStringWithoutUnit() + ";"
				+ this.getAllowedDischargePower().asString();
	}

	@Override
	public void addCharger(AbstractGoodWeEtCharger charger) {
		this.chargers.add(charger);
	}

	@Override
	public void removeCharger(AbstractGoodWeEtCharger charger) {
		this.chargers.remove(charger);
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}

		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.updatechannels();
			break;
		}
	}

	private void updatechannels() {
		/*
		 * Update ActivePower from P_BATTERY1 and chargers ACTUAL_POWER
		 */
		final Channel<Integer> batteryPower = this.channel(GoodWeEss.ChannelId.P_BATTERY1);
		Integer activePower = batteryPower.getNextValue().get();
		Integer productionPower = null;
		for (AbstractGoodWeEtCharger charger : this.chargers) {
			productionPower = TypeUtils.sum(productionPower, charger.getActualPower().get());
			activePower = TypeUtils.sum(activePower, charger.getActualPowerChannel().getNextValue().get());
		}
		this._setActivePower(activePower);

		/*
		 * Calculate AC Energy
		 */
		if (activePower == null) {
			// Not available
			this.calculateAcChargeEnergy.update(null);
			this.calculateAcDischargeEnergy.update(null);
		} else if (activePower > 0) {
			// Discharge
			this.calculateAcChargeEnergy.update(0);
			this.calculateAcDischargeEnergy.update(activePower);
		} else {
			// Charge
			this.calculateAcChargeEnergy.update(activePower * -1);
			this.calculateAcDischargeEnergy.update(0);
		}

		/*
		 * Update Allowed charge and Allowed discharge
		 */

//		System.out.println(
//				"Allowed Charge power: " + allowedChargePower + " Allowed Discharge Power: " + allowedDischargePower);

		Integer soc = this.getSoc().get();
		Integer maxBatteryPower = this.config.maxBatteryPower();

		Integer allowedCharge = null;
		Integer allowedDischarge = null;

		if (productionPower == null) {
			productionPower = 0;
		}

		if (soc == null) {

			allowedCharge = 0;
			allowedDischarge = 0;

		} else if (soc == 100) {

			allowedDischarge = maxBatteryPower + productionPower;
			allowedCharge = 0;

		} else if (soc > 0) {

			allowedDischarge = maxBatteryPower + productionPower;
			allowedCharge = maxBatteryPower;

		} else if (soc == 0) {

			allowedDischarge = productionPower;
			allowedCharge = maxBatteryPower;

		}

		// to avoid charging when production is greater than maximum battery power.
		if (allowedCharge < 0) {
			allowedCharge = 0;
		}

		this._setAllowedChargePower(TypeUtils.multiply(allowedCharge * -1));
		this._setAllowedDischargePower(allowedDischarge);
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}

	@Override
	public Constraint[] getStaticConstraints() throws OpenemsNamedException {
		// Handle Read-Only mode -> no charge/discharge
		if (this.config.readOnlyMode()) {
			return new Constraint[] { //
					this.createPowerConstraint("Read-Only-Mode", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 0), //
					this.createPowerConstraint("Read-Only-Mode", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 0) //
			};
		}

		return Power.NO_CONSTRAINTS;
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public Integer getSurplusPower() {
		if (this.getSoc().orElse(0) < 99) {
			return null;
		}
		Integer productionPower = null;
		for (AbstractGoodWeEtCharger charger : this.chargers) {
			productionPower = TypeUtils.sum(productionPower, charger.getActualPower().get());
		}
		if (productionPower == null || productionPower < 100) {
			return null;
		}
		return productionPower;
	}

	/**
	 * Gets the PV production. Returns 0 if the PV production is not available.
	 * 
	 * @return production power
	 */
	private int getPvProduction() {
		int productionPower = 0;
		for (AbstractGoodWeEtCharger charger : this.chargers) {
			productionPower = TypeUtils.sum(productionPower, charger.getActualPower().get());
		}

		return productionPower;
	}

}
