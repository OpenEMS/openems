package io.openems.edge.goodwe.et.ess;

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

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
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
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.goodwe.et.charger.AbstractGoodWeEtCharger;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "GoodWe.ET.Battery-Inverter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
) //
public class GoodWeEtBatteryInverterImpl extends AbstractOpenemsModbusComponent
		implements GoodWeEtBatteryInverter, ManagedSymmetricEss, SymmetricEss, OpenemsComponent, EventHandler {

	protected EnumWriteChannel setEmsPowerMode;

	private Config config;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	private Power power;

	private final Set<AbstractGoodWeEtCharger> chargers = new HashSet<>();

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus); // Bridge Modbus
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

	public GoodWeEtBatteryInverterImpl() throws OpenemsNamedException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				EssChannelId.values() //
		);
	}

	public String getModbusBridgeId() {
		return this.config.modbus_id();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //

				new FC3ReadRegistersTask(35001, Priority.ONCE, //
						m(SymmetricEss.ChannelId.MAX_APPARENT_POWER, new UnsignedWordElement(35001))), //

				new FC3ReadRegistersTask(35111, Priority.LOW, //
						m(EssChannelId.V_PV3, new UnsignedWordElement(35111),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(EssChannelId.I_PV3, new UnsignedWordElement(35112),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						new DummyRegisterElement(35113, 35114), //
						m(EssChannelId.V_PV4, new UnsignedWordElement(35115),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(EssChannelId.I_PV4, new UnsignedWordElement(35116),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						new DummyRegisterElement(35117, 35118), //
						m(EssChannelId.PV_MODE, new UnsignedDoublewordElement(35119))), //

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
						m(EssChannelId.TOTAL_INV_POWER, new SignedWordElement(35138)), //
						new DummyRegisterElement(35139), //
						m(EssChannelId.AC_ACTIVE_POWER, new SignedWordElement(35140), //
								ElementToChannelConverter.INVERT), //
						new DummyRegisterElement(35141), //
						m(EssChannelId.AC_REACTIVE_POWER, new SignedWordElement(35142), //
								ElementToChannelConverter.INVERT), //
						new DummyRegisterElement(35143), //
						m(EssChannelId.AC_APPARENT_POWER, new SignedWordElement(35144), //
								ElementToChannelConverter.INVERT), //
						m(EssChannelId.BACK_UP_V_LOAD_R, new UnsignedWordElement(35145), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(EssChannelId.BACK_UP_I_LOAD_R, new UnsignedWordElement(35146),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(EssChannelId.BACK_UP_F_LOAD_R, new UnsignedWordElement(35147),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(EssChannelId.LOAD_MODE_R, new UnsignedWordElement(35148)), //
						new DummyRegisterElement(35149), //
						m(EssChannelId.BACK_UP_P_LOAD_R, new SignedWordElement(35150)), //
						m(EssChannelId.BACK_UP_V_LOAD_S, new UnsignedWordElement(35151),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(EssChannelId.BACK_UP_I_LOAD_S, new UnsignedWordElement(35152),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(EssChannelId.BACK_UP_F_LOAD_S, new UnsignedWordElement(35153),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(EssChannelId.LOAD_MODE_S, new UnsignedWordElement(35154)), //
						new DummyRegisterElement(35155), //
						m(EssChannelId.BACK_UP_P_LOAD_S, new SignedWordElement(35156)), //
						m(EssChannelId.BACK_UP_V_LOAD_T, new UnsignedWordElement(35157),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(EssChannelId.BACK_UP_I_LOAD_T, new UnsignedWordElement(35158),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(EssChannelId.BACK_UP_F_LOAD_T, new UnsignedWordElement(35159),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(EssChannelId.LOAD_MODE_T, new UnsignedWordElement(35160)), //
						new DummyRegisterElement(35161), //
						m(EssChannelId.BACK_UP_P_LOAD_T, new SignedWordElement(35162)), //
						new DummyRegisterElement(35163), //
						m(EssChannelId.P_LOAD_R, new SignedWordElement(35164)), //
						new DummyRegisterElement(35165), //
						m(EssChannelId.P_LOAD_S, new SignedWordElement(35166)), //
						new DummyRegisterElement(35167), //
						m(EssChannelId.P_LOAD_T, new SignedWordElement(35168)), //
						new DummyRegisterElement(35169), //
						m(EssChannelId.TOTAL_BACK_UP_LOAD, new SignedWordElement(35170)), //
						new DummyRegisterElement(35171), //
						m(EssChannelId.TOTAL_LOAD_POWER, new SignedWordElement(35172)), //
						m(EssChannelId.UPS_LOAD_PERCENT, new UnsignedWordElement(35173),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2)), //

				new FC3ReadRegistersTask(35180, Priority.HIGH, //
						m(EssChannelId.V_BATTERY1, new UnsignedWordElement(35180),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(EssChannelId.I_BATTERY1, new SignedWordElement(35181),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						new DummyRegisterElement(35182), //
						m(EssChannelId.P_BATTERY1, new SignedWordElement(35183)), //
						m(EssChannelId.BATTERY_MODE, new UnsignedWordElement(35184))), //

				new FC3ReadRegistersTask(35185, Priority.LOW, //
						m(EssChannelId.WARNING_CODE, new UnsignedWordElement(35185)), //
						m(EssChannelId.SAFETY_COUNTRY, new UnsignedWordElement(35186)), //
						m(EssChannelId.WORK_MODE, new UnsignedWordElement(35187)), //
						m(EssChannelId.OPERATION_MODE, new UnsignedDoublewordElement(35188))), //

				// Ess Active charge energy
				new FC3ReadRegistersTask(35206, Priority.LOW, //
						m(SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY, new UnsignedDoublewordElement(35206), //
								ElementToChannelConverter.SCALE_FACTOR_2), //
						new DummyRegisterElement(35208), //
						// Ess Active Discharge energy
						m(SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY, new UnsignedDoublewordElement(35209),
								ElementToChannelConverter.SCALE_FACTOR_2)), //

				new FC3ReadRegistersTask(36003, Priority.LOW, //
						m(EssChannelId.B_METER_COMMUNICATE_STATUS, new UnsignedWordElement(36003)), //
						m(EssChannelId.METER_COMMUNICATE_STATUS, new UnsignedWordElement(36004))), //

				new FC3ReadRegistersTask(37001, Priority.LOW,
						m(EssChannelId.BATTERY_TYPE_INDEX, new UnsignedWordElement(37001)), //
						m(EssChannelId.BMS_STATUS, new UnsignedWordElement(37002)), //
						m(EssChannelId.BMS_PACK_TEMPERATURE, new UnsignedWordElement(37003),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(EssChannelId.BMS_CHARGE_IMAX, new UnsignedWordElement(37004)), //
						m(EssChannelId.BMS_DISCHARGE_IMAX, new UnsignedWordElement(37005))), //

				new FC3ReadRegistersTask(37007, Priority.HIGH, //
						m(SymmetricEss.ChannelId.SOC, new UnsignedWordElement(37007), new ElementToChannelConverter(
								// element -> channel
								value -> {
									// Set SoC to undefined if there is No Battery
									EnumReadChannel batteryModeChannel = this.channel(EssChannelId.BATTERY_MODE);
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
						m(EssChannelId.BMS_SOH, new UnsignedWordElement(37008)), //
						m(EssChannelId.BMS_BATTERY_STRINGS, new UnsignedWordElement(37009))), //

				new FC16WriteRegistersTask(47000, //
						m(EssChannelId.APP_MODE_INDEX, new UnsignedWordElement(47000)), //
						m(EssChannelId.METER_CHECK_VALUE, new UnsignedWordElement(47001)), //
						m(EssChannelId.WMETER_CONNECT_CHECK_FLAG, new UnsignedWordElement(47002))), //

				new FC3ReadRegistersTask(47000, Priority.LOW, //
						m(EssChannelId.APP_MODE_INDEX, new UnsignedWordElement(47000)), //
						m(EssChannelId.METER_CHECK_VALUE, new UnsignedWordElement(47001)), //
						m(EssChannelId.WMETER_CONNECT_CHECK_FLAG, new UnsignedWordElement(47002))), //

				new FC16WriteRegistersTask(47500, //
						m(EssChannelId.STOP_SOC_PROTECT, new UnsignedWordElement(47500)), //
						new DummyRegisterElement(47501, 47508), //
						m(EssChannelId.FEED_POWER_ENABLE, new UnsignedWordElement(47509)), //
						m(EssChannelId.FEED_POWER_PARA, new UnsignedWordElement(47510)), //
						m(EssChannelId.EMS_POWER_MODE, new UnsignedWordElement(47511)), //
						m(EssChannelId.EMS_POWER_SET, new UnsignedWordElement(47512))), //

				new FC16WriteRegistersTask(47531, //
						m(EssChannelId.SOC_START_TO_FORCE_CHARGE, new UnsignedWordElement(47531)), //
						m(EssChannelId.SOC_STOP_TO_FORCE_CHARGE, new UnsignedWordElement(47532)), //
						m(EssChannelId.CLEAR_ALL_ECONOMIC_MODE, new UnsignedWordElement(47533))), //

				new FC3ReadRegistersTask(47500, Priority.LOW,
						m(EssChannelId.STOP_SOC_PROTECT, new UnsignedWordElement(47500)), //
						new DummyRegisterElement(47501, 47508), //
						m(EssChannelId.FEED_POWER_ENABLE, new UnsignedWordElement(47509)), //
						m(EssChannelId.FEED_POWER_PARA, new UnsignedWordElement(47510))), //

				new FC3ReadRegistersTask(47511, Priority.HIGH,
						m(EssChannelId.EMS_POWER_MODE, new UnsignedWordElement(47511)), //
						m(EssChannelId.EMS_POWER_SET, new UnsignedWordElement(47512))), //

				new FC3ReadRegistersTask(47531, Priority.LOW,
						m(EssChannelId.SOC_START_TO_FORCE_CHARGE, new UnsignedWordElement(47531)), //
						m(EssChannelId.SOC_STOP_TO_FORCE_CHARGE, new UnsignedWordElement(47532))), //

				new FC16WriteRegistersTask(47900, //
						m(EssChannelId.BMS_VERSION, new UnsignedWordElement(47900)), //
						m(EssChannelId.BATT_STRINGS_RS485, new UnsignedWordElement(47901)), //
						m(EssChannelId.WBMS_BAT_CHARGE_VMAX, new UnsignedWordElement(47902)), //
						m(EssChannelId.WBMS_BAT_CHARGE_IMAX, new UnsignedWordElement(47903)), //
						m(EssChannelId.WBMS_BAT_DISCHARGE_VMIN, new UnsignedWordElement(47904)), //
						m(EssChannelId.WBMS_BAT_DISCHARGE_IMAX, new UnsignedWordElement(47905)), //
						m(EssChannelId.WBMS_BAT_VOLTAGE, new UnsignedWordElement(47906)), //
						m(EssChannelId.WBMS_BAT_CURRENT, new UnsignedWordElement(47907)), //
						m(EssChannelId.WBMS_BAT_SOC, new UnsignedWordElement(47908)), //
						m(EssChannelId.WBMS_BAT_SOH, new UnsignedWordElement(47909)), //
						m(EssChannelId.WBMS_BAT_TEMPERATURE, new UnsignedWordElement(47910)), //
						m(new BitsWordElement(47911, this) //
								.bit(0, EssChannelId.STATE_58) //
								.bit(1, EssChannelId.STATE_59) //
								.bit(2, EssChannelId.STATE_60) //
								.bit(3, EssChannelId.STATE_61) //
								.bit(4, EssChannelId.STATE_62) //
								.bit(5, EssChannelId.STATE_63) //
								.bit(6, EssChannelId.STATE_64) //
								.bit(7, EssChannelId.STATE_65) //
								.bit(8, EssChannelId.STATE_66) //
								.bit(9, EssChannelId.STATE_67) //
								.bit(10, EssChannelId.STATE_68) //
								.bit(11, EssChannelId.STATE_69)), //
						new DummyRegisterElement(47912), //
						m(new BitsWordElement(47913, this) //
								.bit(0, EssChannelId.STATE_42) //
								.bit(1, EssChannelId.STATE_43) //
								.bit(2, EssChannelId.STATE_44) //
								.bit(3, EssChannelId.STATE_45) //
								.bit(4, EssChannelId.STATE_46) //
								.bit(5, EssChannelId.STATE_47) //
								.bit(6, EssChannelId.STATE_48) //
								.bit(7, EssChannelId.STATE_49) //
								.bit(8, EssChannelId.STATE_50) //
								.bit(9, EssChannelId.STATE_51) //
								.bit(10, EssChannelId.STATE_52) //
								.bit(11, EssChannelId.STATE_53) //
								.bit(12, EssChannelId.STATE_54) //
								.bit(13, EssChannelId.STATE_55) //
								.bit(14, EssChannelId.STATE_56) //
								.bit(15, EssChannelId.STATE_57)), //
						new DummyRegisterElement(47914), //
						m(new BitsWordElement(47915, this) //
								.bit(0, EssChannelId.STATE_79) //
								.bit(1, EssChannelId.STATE_80) //
								.bit(2, EssChannelId.STATE_81))), //

				new FC3ReadRegistersTask(47902, Priority.LOW, //
						m(EssChannelId.WBMS_BAT_CHARGE_VMAX, new UnsignedWordElement(47902)), //
						m(EssChannelId.WBMS_BAT_CHARGE_IMAX, new UnsignedWordElement(47903)), //
						m(EssChannelId.WBMS_BAT_DISCHARGE_VMIN, new UnsignedWordElement(47904)), //
						m(EssChannelId.WBMS_BAT_DISCHARGE_IMAX, new UnsignedWordElement(47905)), //
						m(EssChannelId.WBMS_BAT_VOLTAGE, new UnsignedWordElement(47906)), //
						m(EssChannelId.WBMS_BAT_CURRENT, new UnsignedWordElement(47907)), //
						m(EssChannelId.WBMS_BAT_SOC, new UnsignedWordElement(47908)), //
						m(EssChannelId.WBMS_BAT_SOH, new UnsignedWordElement(47909)), //
						m(EssChannelId.WBMS_BAT_TEMPERATURE, new UnsignedWordElement(47910)), //
						m(new BitsWordElement(47911, this) //
								.bit(0, EssChannelId.STATE_58) //
								.bit(1, EssChannelId.STATE_59) //
								.bit(2, EssChannelId.STATE_60) //
								.bit(3, EssChannelId.STATE_61) //
								.bit(4, EssChannelId.STATE_62) //
								.bit(5, EssChannelId.STATE_63) //
								.bit(6, EssChannelId.STATE_64) //
								.bit(7, EssChannelId.STATE_65) //
								.bit(8, EssChannelId.STATE_66) //
								.bit(9, EssChannelId.STATE_67) //
								.bit(10, EssChannelId.STATE_68) //
								.bit(11, EssChannelId.STATE_69)), //
						new DummyRegisterElement(47912), //
						m(new BitsWordElement(47913, this) //
								.bit(0, EssChannelId.STATE_42) //
								.bit(1, EssChannelId.STATE_43) //
								.bit(2, EssChannelId.STATE_44) //
								.bit(3, EssChannelId.STATE_45) //
								.bit(4, EssChannelId.STATE_46) //
								.bit(5, EssChannelId.STATE_47) //
								.bit(6, EssChannelId.STATE_48) //
								.bit(7, EssChannelId.STATE_49) //
								.bit(8, EssChannelId.STATE_50) //
								.bit(9, EssChannelId.STATE_51) //
								.bit(10, EssChannelId.STATE_52) //
								.bit(11, EssChannelId.STATE_53) //
								.bit(12, EssChannelId.STATE_54) //
								.bit(13, EssChannelId.STATE_55) //
								.bit(14, EssChannelId.STATE_56) //
								.bit(15, EssChannelId.STATE_57)), //
						new DummyRegisterElement(47914), //
						m(new BitsWordElement(47915, this) //
								.bit(0, EssChannelId.STATE_79) //
								.bit(1, EssChannelId.STATE_80) //
								.bit(2, EssChannelId.STATE_81))));
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {
		final PowerModeEms nextPowerMode;

		if (this.config.readOnlyMode()) {
			// Read-Only-Mode: fall-back to internal self-consumption optimization
			nextPowerMode = PowerModeEms.AUTO;
			activePower = 0;
		} else {
			if (activePower <= 0) {
				// ActivePower is negative or zero -> CHARGE
				nextPowerMode = PowerModeEms.CHARGE_BAT;

			} else {
				// ActivePower is positive -> DISCHARGE

				/*
				 * Check if PV is available. Discharge mode changes according to availability of
				 * PV
				 * 
				 * TODO PV mode is not working, need an update from GoodWe for this.
				 */
				Integer productionPower = null;
				for (AbstractGoodWeEtCharger charger : this.chargers) {
					productionPower = TypeUtils.sum(productionPower, charger.getActualPower().value().get());
				}
				if (productionPower == null) {
					// No PV-Power -> required to put on SELL_POWER
					nextPowerMode = PowerModeEms.SELL_POWER;
				} else {
					// PV-Power exists -> set DISCHARGE_BAT
					nextPowerMode = PowerModeEms.DISCHARGE_BAT;
				}
			}
		}

		// Set the PowerMode and PowerSet
		IntegerWriteChannel emsPowerSetChannel = this.channel(EssChannelId.EMS_POWER_SET);

		Integer essPowerSet = emsPowerSetChannel.value().get();
		if (essPowerSet == null || activePower != essPowerSet) {
			// Set to new power mode only if the previous activePower is different or
			// undefined
			emsPowerSetChannel.setNextWriteValue(Math.abs(activePower));
		}

		EnumWriteChannel emsPowerModeChannel = this.channel(EssChannelId.EMS_POWER_MODE);
		PowerModeEms emsPowerMode = emsPowerModeChannel.value().asEnum();
		if (emsPowerMode != nextPowerMode) {
			// Set to new power mode only if the previous mode is different
			emsPowerModeChannel.setNextWriteValue(nextPowerMode);
		}
		// TODO : Add Reactive Power Register
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
		final Channel<Integer> batteryPower = this.channel(EssChannelId.P_BATTERY1);
		Integer activePower = batteryPower.getNextValue().get();
		for (AbstractGoodWeEtCharger charger : this.chargers) {
			activePower = TypeUtils.sum(activePower, charger.getActualPower().getNextValue().get());
		}
		this._setActivePower(activePower);

		/*
		 * Update Allowed charge and Allowed discharge
		 */

		Integer soc = this.getSoc().get();
		Integer maxApparentPower = this.getMaxApparentPower().get();

		if (soc == null || soc >= 99) {
			this._setAllowedChargePower(0);
		} else {
			this._setAllowedChargePower(TypeUtils.multiply(maxApparentPower, -1));
		}
		if (soc == null || soc <= 0) {
			this._setAllowedDischargePower(0);
		} else {
			this._setAllowedDischargePower(maxApparentPower);
		}
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
}
