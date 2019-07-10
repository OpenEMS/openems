package io.openems.edge.ess.refu;

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

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Refu.Ess", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)

public class RefuEss extends AbstractOpenemsModbusComponent implements SymmetricEss, AsymmetricEss,
		ManagedAsymmetricEss, ManagedSymmetricEss, OpenemsComponent, EventHandler, ModbusSlave {

	protected final static int MAX_APPARENT_POWER = 100_000;
	private final static int UNIT_ID = 1;

	@Reference
	private Power power;

	@Reference
	protected ConfigurationAdmin cm;

	private final ErrorHandler errorHandler;

	public RefuEss() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				AsymmetricEss.ChannelId.values(), //
				ManagedAsymmetricEss.ChannelId.values(), //
				ChannelId.values() //
		);
		this.channel(SymmetricEss.ChannelId.GRID_MODE).setNextValue(GridMode.ON_GRID);
		this.channel(SymmetricEss.ChannelId.MAX_APPARENT_POWER).setNextValue(RefuEss.MAX_APPARENT_POWER);
		this.errorHandler = new ErrorHandler(this);
	}

	@Override
	public void applyPower(int activePowerL1, int reactivePowerL1, int activePowerL2, int reactivePowerL2,
			int activePowerL3, int reactivePowerL3) throws OpenemsNamedException {
		int activePower = activePowerL1 + activePowerL2 + activePowerL3;
		int allowedCharge = this.getAllowedCharge().value().orElse(0);
		int allowedDischarge = this.getAllowedDischarge().value().orElse(0);

		/*
		 * Specific handling for REFU to never be at -5000 < power < 5000 if battery is
		 * full/empty
		 */
		if ( // Battery is full and discharge power < 5000
		(allowedCharge > -100 && activePower > 0 && activePower < 5000)
				// Battery is empty and charge power > -5000
				|| (allowedDischarge < 100 && activePower < 0 && activePower > -5000)) {
			activePowerL1 = 0;
			activePowerL2 = 0;
			activePowerL3 = 0;
			reactivePowerL1 = 0;
			reactivePowerL2 = 0;
			reactivePowerL3 = 0;
		}
		this.getSetActivePowerL1Channel().setNextWriteValue(activePowerL1);
		this.getSetActivePowerL2Channel().setNextWriteValue(activePowerL2);
		this.getSetActivePowerL3Channel().setNextWriteValue(activePowerL3);
		this.getSetReactivePowerL1Channel().setNextWriteValue(reactivePowerL1);
		this.getSetReactivePowerL2Channel().setNextWriteValue(reactivePowerL2);
		this.getSetReactivePowerL3Channel().setNextWriteValue(reactivePowerL3);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled(), UNIT_ID, this.cm, "Modbus",
				config.modbus_id());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC4ReadInputRegistersTask(0x100, Priority.HIGH, //
						m(RefuEss.ChannelId.SYSTEM_STATE, new UnsignedWordElement(0x100)), //
						m(new BitsWordElement(0x101, this) //
								.bit(0, RefuEss.ChannelId.STATE_0) //
								.bit(1, RefuEss.ChannelId.STATE_1) //
								.bit(2, RefuEss.ChannelId.STATE_2) //
								.bit(3, RefuEss.ChannelId.STATE_3) //
								.bit(4, RefuEss.ChannelId.STATE_4) //
								.bit(5, RefuEss.ChannelId.STATE_5) //
								.bit(6, RefuEss.ChannelId.STATE_6) //
								.bit(7, RefuEss.ChannelId.STATE_7) //
								.bit(8, RefuEss.ChannelId.STATE_8) //
								.bit(9, RefuEss.ChannelId.STATE_9, BitConverter.INVERT) //
								.bit(10, RefuEss.ChannelId.STATE_10, BitConverter.INVERT)), //
						m(new BitsWordElement(0x102, this) //
								.bit(0, RefuEss.ChannelId.STATE_11, BitConverter.INVERT) //
								.bit(1, RefuEss.ChannelId.STATE_12, BitConverter.INVERT) //
								.bit(2, RefuEss.ChannelId.STATE_13, BitConverter.INVERT) //
								.bit(3, RefuEss.ChannelId.STATE_14) //
								.bit(4, RefuEss.ChannelId.STATE_15, BitConverter.INVERT)), //
						m(new BitsWordElement(0x103, this) //
								.bit(0, RefuEss.ChannelId.INVERTER_STATE_0) //
								.bit(1, RefuEss.ChannelId.INVERTER_STATE_1) //
								.bit(2, RefuEss.ChannelId.INVERTER_STATE_2) //
								.bit(3, RefuEss.ChannelId.INVERTER_STATE_3) //
								.bit(7, RefuEss.ChannelId.INVERTER_STATE_7) //
								.bit(8, RefuEss.ChannelId.INVERTER_STATE_8) //
								.bit(9, RefuEss.ChannelId.INVERTER_STATE_9) //
								.bit(10, RefuEss.ChannelId.INVERTER_STATE_10) //
								.bit(11, RefuEss.ChannelId.INVERTER_STATE_11) //
								.bit(12, RefuEss.ChannelId.INVERTER_STATE_12) //
								.bit(13, RefuEss.ChannelId.INVERTER_STATE_13)),
						m(RefuEss.ChannelId.INVERTER_ERROR_CODE, new UnsignedWordElement(0x104)), //
						m(new BitsWordElement(0x105, this) //
								.bit(0, RefuEss.ChannelId.DCDC_STATE_0) //
								.bit(1, RefuEss.ChannelId.DCDC_STATE_1) //
								.bit(2, RefuEss.ChannelId.DCDC_STATE_2) //
								.bit(3, RefuEss.ChannelId.DCDC_STATE_3) //
								.bit(7, RefuEss.ChannelId.DCDC_STATE_7) //
								.bit(8, RefuEss.ChannelId.DCDC_STATE_8) //
								.bit(9, RefuEss.ChannelId.DCDC_STATE_9)),
						m(RefuEss.ChannelId.DCDC_ERROR_CODE, new UnsignedWordElement(0x106)), //
						m(RefuEss.ChannelId.BATTERY_CURRENT_PCS, new SignedWordElement(0x107),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(RefuEss.ChannelId.BATTERY_VOLTAGE_PCS, new SignedWordElement(0x108),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(RefuEss.ChannelId.CURRENT, new SignedWordElement(0x109),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(RefuEss.ChannelId.CURRENT_L1, new SignedWordElement(0x10A),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(RefuEss.ChannelId.CURRENT_L2, new SignedWordElement(0x10B),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(RefuEss.ChannelId.CURRENT_L3, new SignedWordElement(0x10C),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(SymmetricEss.ChannelId.ACTIVE_POWER, new SignedWordElement(0x10D),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(AsymmetricEss.ChannelId.ACTIVE_POWER_L1, new SignedWordElement(0x10E),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(AsymmetricEss.ChannelId.ACTIVE_POWER_L2, new SignedWordElement(0x10F),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(AsymmetricEss.ChannelId.ACTIVE_POWER_L3, new SignedWordElement(0x110),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(SymmetricEss.ChannelId.REACTIVE_POWER, new SignedWordElement(0x111),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(AsymmetricEss.ChannelId.REACTIVE_POWER_L1, new SignedWordElement(0x112),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(AsymmetricEss.ChannelId.REACTIVE_POWER_L2, new SignedWordElement(0x113),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(AsymmetricEss.ChannelId.REACTIVE_POWER_L3, new SignedWordElement(0x114),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(RefuEss.ChannelId.COS_PHI_3P, new SignedWordElement(0x115)),
						m(RefuEss.ChannelId.COS_PHI_L1, new SignedWordElement(0x116)),
						m(RefuEss.ChannelId.COS_PHI_L2, new SignedWordElement(0x117)),
						m(RefuEss.ChannelId.COS_PHI_L3, new SignedWordElement(0x118))),

				new FC4ReadInputRegistersTask(0x11A, Priority.LOW, //
						m(RefuEss.ChannelId.PCS_ALLOWED_CHARGE, new SignedWordElement(0x11A),
								ElementToChannelConverter.SCALE_FACTOR_2_AND_INVERT),
						m(RefuEss.ChannelId.PCS_ALLOWED_DISCHARGE, new SignedWordElement(0x11B),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(RefuEss.ChannelId.BATTERY_STATE, new UnsignedWordElement(0x11C)), //
						m(RefuEss.ChannelId.BATTERY_MODE, new UnsignedWordElement(0x11D)), //
						m(RefuEss.ChannelId.BATTERY_VOLTAGE, new UnsignedWordElement(0x11E)), //
						m(RefuEss.ChannelId.BATTERY_CURRENT, new SignedWordElement(0x11F)), //
						m(RefuEss.ChannelId.BATTERY_POWER, new SignedWordElement(0x120)), //
						m(SymmetricEss.ChannelId.SOC, new UnsignedWordElement(0x121)), //
						m(RefuEss.ChannelId.ALLOWED_CHARGE_CURRENT, new UnsignedWordElement(0x122),
								ElementToChannelConverter.SCALE_FACTOR_2_AND_INVERT),
						m(RefuEss.ChannelId.ALLOWED_DISCHARGE_CURRENT, new UnsignedWordElement(0x123),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER, new UnsignedWordElement(0x124),
								ElementToChannelConverter.SCALE_FACTOR_2_AND_INVERT), //
						m(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER, new UnsignedWordElement(0x125),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(RefuEss.ChannelId.BATTERY_CHARGE_ENERGY,
								new SignedDoublewordElement(0x126).wordOrder(WordOrder.LSWMSW)), //
						m(RefuEss.ChannelId.BATTERY_DISCHARGE_ENERGY,
								new SignedDoublewordElement(0x128).wordOrder(WordOrder.LSWMSW)), //
						m(new BitsWordElement(0x12A, this) //
								.bit(0, RefuEss.ChannelId.BATTERY_ON_GRID_STATE_0) //
								.bit(1, RefuEss.ChannelId.BATTERY_ON_GRID_STATE_1) //
								.bit(2, RefuEss.ChannelId.BATTERY_ON_GRID_STATE_2) //
								.bit(3, RefuEss.ChannelId.BATTERY_ON_GRID_STATE_3) //
								.bit(4, RefuEss.ChannelId.BATTERY_ON_GRID_STATE_4) //
								.bit(5, RefuEss.ChannelId.BATTERY_ON_GRID_STATE_5) //
								.bit(6, RefuEss.ChannelId.BATTERY_ON_GRID_STATE_6) //
								.bit(7, RefuEss.ChannelId.BATTERY_ON_GRID_STATE_7) //
								.bit(8, RefuEss.ChannelId.BATTERY_ON_GRID_STATE_8) //
								.bit(9, RefuEss.ChannelId.BATTERY_ON_GRID_STATE_9) //
								.bit(10, RefuEss.ChannelId.BATTERY_ON_GRID_STATE_10) //
								.bit(11, RefuEss.ChannelId.BATTERY_ON_GRID_STATE_11) //
								.bit(12, RefuEss.ChannelId.BATTERY_ON_GRID_STATE_12) //
								.bit(13, RefuEss.ChannelId.BATTERY_ON_GRID_STATE_13) //
								.bit(14, RefuEss.ChannelId.BATTERY_ON_GRID_STATE_14) //
								.bit(15, RefuEss.ChannelId.BATTERY_ON_GRID_STATE_15)),
						m(RefuEss.ChannelId.BATTERY_HIGHEST_VOLTAGE, new UnsignedWordElement(0x12B)), //
						m(RefuEss.ChannelId.BATTERY_LOWEST_VOLTAGE, new UnsignedWordElement(0x12C)), //
						m(RefuEss.ChannelId.BATTERY_HIGHEST_TEMPERATURE, new SignedWordElement(0x12D)), //
						m(RefuEss.ChannelId.BATTERY_LOWEST_TEMPERATURE, new SignedWordElement(0x12E)), //
						m(RefuEss.ChannelId.BATTERY_STOP_REQUEST, new UnsignedWordElement(0x12F)), //
						m(new BitsWordElement(0x130, this) //
								.bit(0, RefuEss.ChannelId.STATE_16) //
								.bit(1, RefuEss.ChannelId.STATE_17) //
								.bit(2, RefuEss.ChannelId.STATE_18) //
								.bit(3, RefuEss.ChannelId.STATE_19) //
								.bit(4, RefuEss.ChannelId.STATE_20) //
								.bit(5, RefuEss.ChannelId.STATE_21) //
								.bit(6, RefuEss.ChannelId.STATE_22) //
								.bit(7, RefuEss.ChannelId.STATE_23) //
								.bit(8, RefuEss.ChannelId.STATE_24) //
								.bit(9, RefuEss.ChannelId.STATE_25) //
								.bit(10, RefuEss.ChannelId.STATE_26) //
								.bit(11, RefuEss.ChannelId.STATE_27) //
								.bit(12, RefuEss.ChannelId.STATE_28) //
								.bit(13, RefuEss.ChannelId.STATE_29) //
								.bit(14, RefuEss.ChannelId.STATE_30)),
						m(new BitsWordElement(0x131, this) //
								.bit(0, RefuEss.ChannelId.STATE_31) //
								.bit(1, RefuEss.ChannelId.STATE_32) //
								.bit(5, RefuEss.ChannelId.STATE_33) //
								.bit(7, RefuEss.ChannelId.STATE_34)),
						m(new BitsWordElement(0x132, this) //
								.bit(0, RefuEss.ChannelId.STATE_35) //
								.bit(1, RefuEss.ChannelId.STATE_36) //
								.bit(2, RefuEss.ChannelId.STATE_37) //
								.bit(3, RefuEss.ChannelId.STATE_38)),
						m(new BitsWordElement(0x133, this) //
								.bit(0, RefuEss.ChannelId.STATE_39) //
								.bit(1, RefuEss.ChannelId.STATE_40) //
								.bit(2, RefuEss.ChannelId.STATE_41) //
								.bit(3, RefuEss.ChannelId.STATE_42)),
						new DummyRegisterElement(0x134), //
						m(new BitsWordElement(0x135, this) //
								.bit(0, RefuEss.ChannelId.STATE_43) //
								.bit(1, RefuEss.ChannelId.STATE_44) //
								.bit(2, RefuEss.ChannelId.STATE_45) //
								.bit(3, RefuEss.ChannelId.STATE_46)),
						m(new BitsWordElement(0x136, this) //
								.bit(0, RefuEss.ChannelId.STATE_47) //
								.bit(1, RefuEss.ChannelId.STATE_48) //
								.bit(2, RefuEss.ChannelId.STATE_49) //
								.bit(3, RefuEss.ChannelId.STATE_50)),
						m(new BitsWordElement(0x137, this) //
								.bit(0, RefuEss.ChannelId.STATE_51) //
								.bit(1, RefuEss.ChannelId.STATE_52) //
								.bit(2, RefuEss.ChannelId.STATE_53) //
								.bit(3, RefuEss.ChannelId.STATE_54) //
								.bit(4, RefuEss.ChannelId.STATE_55) //
								.bit(5, RefuEss.ChannelId.STATE_56) //
								.bit(10, RefuEss.ChannelId.STATE_57) //
								.bit(11, RefuEss.ChannelId.STATE_58) //
								.bit(12, RefuEss.ChannelId.STATE_59) //
								.bit(13, RefuEss.ChannelId.STATE_60)),
						m(new BitsWordElement(0x138, this) //
								.bit(0, RefuEss.ChannelId.STATE_61) //
								.bit(1, RefuEss.ChannelId.STATE_62) //
								.bit(2, RefuEss.ChannelId.STATE_63) //
								.bit(3, RefuEss.ChannelId.STATE_64)),
						m(new BitsWordElement(0x139, this) //
								.bit(0, RefuEss.ChannelId.STATE_65) //
								.bit(1, RefuEss.ChannelId.STATE_66) //
								.bit(2, RefuEss.ChannelId.STATE_67) //
								.bit(3, RefuEss.ChannelId.STATE_68)),
						m(new BitsWordElement(0x13A, this) //
								.bit(0, RefuEss.ChannelId.STATE_69) //
								.bit(1, RefuEss.ChannelId.STATE_70) //
								.bit(2, RefuEss.ChannelId.STATE_71) //
								.bit(3, RefuEss.ChannelId.STATE_72)),
						m(new BitsWordElement(0x13B, this) //
								.bit(0, RefuEss.ChannelId.STATE_73) //
								.bit(1, RefuEss.ChannelId.STATE_74) //
								.bit(2, RefuEss.ChannelId.STATE_75) //
								.bit(3, RefuEss.ChannelId.STATE_76)),
						m(new BitsWordElement(0x13C, this) //
								.bit(0, RefuEss.ChannelId.STATE_77) //
								.bit(1, RefuEss.ChannelId.STATE_78) //
								.bit(2, RefuEss.ChannelId.STATE_79) //
								.bit(3, RefuEss.ChannelId.STATE_80)),
						new DummyRegisterElement(0x13D), //
						new DummyRegisterElement(0x13E), //
						m(new BitsWordElement(0x13F, this) //
								.bit(2, RefuEss.ChannelId.STATE_81) //
								.bit(3, RefuEss.ChannelId.STATE_82) //
								.bit(4, RefuEss.ChannelId.STATE_83) //
								.bit(6, RefuEss.ChannelId.STATE_84) //
								.bit(9, RefuEss.ChannelId.STATE_85) //
								.bit(10, RefuEss.ChannelId.STATE_86) //
								.bit(11, RefuEss.ChannelId.STATE_87) //
								.bit(12, RefuEss.ChannelId.STATE_88) //
								.bit(13, RefuEss.ChannelId.STATE_89) //
								.bit(14, RefuEss.ChannelId.STATE_90) //
								.bit(15, RefuEss.ChannelId.STATE_91)),
						m(new BitsWordElement(0x140, this) //
								.bit(2, RefuEss.ChannelId.STATE_92) //
								.bit(3, RefuEss.ChannelId.STATE_93) //
								.bit(7, RefuEss.ChannelId.STATE_94) //
								.bit(8, RefuEss.ChannelId.STATE_95) //
								.bit(10, RefuEss.ChannelId.STATE_96) //
								.bit(11, RefuEss.ChannelId.STATE_97) //
								.bit(12, RefuEss.ChannelId.STATE_98) //
								.bit(13, RefuEss.ChannelId.STATE_99) //
								.bit(14, RefuEss.ChannelId.STATE_100)),
						new DummyRegisterElement(0x141), //
						new DummyRegisterElement(0x142), //
						new DummyRegisterElement(0x143), //
						new DummyRegisterElement(0x144), //
						m(new BitsWordElement(0x145, this) //
								.bit(0, RefuEss.ChannelId.BATTERY_CONTROL_STATE_0) //
								.bit(1, RefuEss.ChannelId.BATTERY_CONTROL_STATE_1) //
								.bit(2, RefuEss.ChannelId.BATTERY_CONTROL_STATE_2) //
								.bit(3, RefuEss.ChannelId.BATTERY_CONTROL_STATE_3) //
								.bit(4, RefuEss.ChannelId.BATTERY_CONTROL_STATE_4) //
								.bit(5, RefuEss.ChannelId.BATTERY_CONTROL_STATE_5) //
								.bit(6, RefuEss.ChannelId.BATTERY_CONTROL_STATE_6) //
								.bit(7, RefuEss.ChannelId.BATTERY_CONTROL_STATE_7) //
								.bit(8, RefuEss.ChannelId.BATTERY_CONTROL_STATE_8) //
								.bit(9, RefuEss.ChannelId.BATTERY_CONTROL_STATE_9) //
								.bit(10, RefuEss.ChannelId.BATTERY_CONTROL_STATE_10) //
								.bit(11, RefuEss.ChannelId.BATTERY_CONTROL_STATE_11) //
								.bit(12, RefuEss.ChannelId.BATTERY_CONTROL_STATE_12) //
								.bit(13, RefuEss.ChannelId.BATTERY_CONTROL_STATE_13) //
								.bit(14, RefuEss.ChannelId.BATTERY_CONTROL_STATE_14) //
								.bit(15, RefuEss.ChannelId.BATTERY_CONTROL_STATE_15)),
						m(RefuEss.ChannelId.ERROR_LOG_0, new UnsignedWordElement(0x146)), //
						m(RefuEss.ChannelId.ERROR_LOG_1, new UnsignedWordElement(0x147)), //
						m(RefuEss.ChannelId.ERROR_LOG_2, new UnsignedWordElement(0x148)), //
						m(RefuEss.ChannelId.ERROR_LOG_3, new UnsignedWordElement(0x149)), //
						m(RefuEss.ChannelId.ERROR_LOG_4, new UnsignedWordElement(0x14A)), //
						m(RefuEss.ChannelId.ERROR_LOG_5, new UnsignedWordElement(0x14B)), //
						m(RefuEss.ChannelId.ERROR_LOG_6, new UnsignedWordElement(0x14C)), //
						m(RefuEss.ChannelId.ERROR_LOG_7, new UnsignedWordElement(0x14D)), //
						m(RefuEss.ChannelId.ERROR_LOG_8, new UnsignedWordElement(0x14E)), //
						m(RefuEss.ChannelId.ERROR_LOG_9, new UnsignedWordElement(0x14F)), //
						m(RefuEss.ChannelId.ERROR_LOG_10, new UnsignedWordElement(0x150)), //
						m(RefuEss.ChannelId.ERROR_LOG_11, new UnsignedWordElement(0x151)), //
						m(RefuEss.ChannelId.ERROR_LOG_12, new UnsignedWordElement(0x152)), //
						m(RefuEss.ChannelId.ERROR_LOG_13, new UnsignedWordElement(0x153)), //
						m(RefuEss.ChannelId.ERROR_LOG_14, new UnsignedWordElement(0x154)), //
						m(RefuEss.ChannelId.ERROR_LOG_15, new UnsignedWordElement(0x155)) //
				),

				new FC3ReadRegistersTask(0x200, Priority.LOW, //
						m(RefuEss.ChannelId.SET_WORK_STATE, new UnsignedWordElement(0x200)),
						m(RefuEss.ChannelId.SET_SYSTEM_ERROR_RESET, new UnsignedWordElement(0x201)),
						m(RefuEss.ChannelId.SET_OPERATION_MODE, new UnsignedWordElement(0x202))),

				new FC16WriteRegistersTask(0x200, //
						m(RefuEss.ChannelId.SET_WORK_STATE, new UnsignedWordElement(0x200)),
						m(RefuEss.ChannelId.SET_SYSTEM_ERROR_RESET, new UnsignedWordElement(0x201)),
						m(RefuEss.ChannelId.SET_OPERATION_MODE, new UnsignedWordElement(0x202))), //
				new FC16WriteRegistersTask(0x203, //
						m(RefuEss.ChannelId.SET_ACTIVE_POWER, new SignedWordElement(0x203),
								ElementToChannelConverter.SCALE_FACTOR_2)),
				new FC16WriteRegistersTask(0x204, //
						m(RefuEss.ChannelId.SET_ACTIVE_POWER_L1, new SignedWordElement(0x204),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(RefuEss.ChannelId.SET_ACTIVE_POWER_L2, new SignedWordElement(0x205),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(RefuEss.ChannelId.SET_ACTIVE_POWER_L3, new SignedWordElement(0x206),
								ElementToChannelConverter.SCALE_FACTOR_2)), //
				new FC16WriteRegistersTask(0x207, //
						m(RefuEss.ChannelId.SET_REACTIVE_POWER, new SignedWordElement(0x207),
								ElementToChannelConverter.SCALE_FACTOR_2)),
				new FC16WriteRegistersTask(0x208, //
						m(RefuEss.ChannelId.SET_REACTIVE_POWER_L1, new SignedWordElement(0x208),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(RefuEss.ChannelId.SET_REACTIVE_POWER_L2, new SignedWordElement(0x209),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(RefuEss.ChannelId.SET_REACTIVE_POWER_L3, new SignedWordElement(0x20A),
								ElementToChannelConverter.SCALE_FACTOR_2)));//
	}

	@Override
	public String debugLog() {
//		String state = this.getState().listStates(Level.WARNING);
		return "SoC:" + this.getSoc().value().asString() //
				+ "|L:" + this.getActivePower().value().asString() //
				+ "|Allowed:" + this.getAllowedCharge().value().asStringWithoutUnit() + ";"
				+ this.getAllowedDischarge().value().asString(); //
//				+ (state.isEmpty() ? "" : ";" + this.getState().listStates());
	}

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		// IntegerWriteChannel
		SET_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.unit(Unit.WATT)), //
		SET_ACTIVE_POWER_L1(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.unit(Unit.WATT)), //
		SET_ACTIVE_POWER_L2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.unit(Unit.WATT)), //
		SET_ACTIVE_POWER_L3(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.unit(Unit.WATT)), //
		SET_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.unit(Unit.WATT)), //
		SET_REACTIVE_POWER_L1(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.unit(Unit.VOLT_AMPERE)), //
		SET_REACTIVE_POWER_L2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.unit(Unit.VOLT_AMPERE)), //
		SET_REACTIVE_POWER_L3(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.unit(Unit.VOLT_AMPERE)), //

		// EnumWriteChannel
		SET_SYSTEM_ERROR_RESET(Doc.of(StopStart.values()) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		SET_OPERATION_MODE(Doc.of(SetOperationMode.values()) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		SET_WORK_STATE(Doc.of(StopStart.values()) //
				.accessMode(AccessMode.WRITE_ONLY)), //

		// EnumReadChannel
		BATTERY_STATE(Doc.of(BatteryState.values())), //
		BATTERY_MODE(Doc.of(BatteryMode.values())), //
		SYSTEM_STATE(Doc.of(SystemState.values())), //
		DCDC_STATUS(Doc.of(DcdcStatus.values())), //

		// IntegerReadChannel
		ERROR_HANDLER_STATE(Doc.of(OpenemsType.INTEGER)), //
		INVERTER_ERROR_CODE(Doc.of(OpenemsType.INTEGER)), //
		DCDC_ERROR_CODE(Doc.of(OpenemsType.INTEGER)), //
		BATTERY_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		BATTERY_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		BATTERY_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		BATTERY_VOLTAGE_PCS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		BATTERY_CURRENT_PCS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		PCS_ALLOWED_CHARGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		PCS_ALLOWED_DISCHARGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		ALLOWED_CHARGE_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		ALLOWED_DISCHARGE_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //

		BATTERY_CHARGE_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS)), //
		BATTERY_DISCHARGE_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS)), //
		BATTERY_HIGHEST_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		BATTERY_LOWEST_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		BATTERY_HIGHEST_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_LOWEST_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_STOP_REQUEST(Doc.of(OpenemsType.INTEGER)), //
		CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		CURRENT_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		CURRENT_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		CURRENT_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //

		COS_PHI_3P(Doc.of(OpenemsType.INTEGER)), //
		COS_PHI_L1(Doc.of(OpenemsType.INTEGER)), //
		COS_PHI_L2(Doc.of(OpenemsType.INTEGER)), //
		COS_PHI_L3(Doc.of(OpenemsType.INTEGER)), //

		STATE_0(Doc.of(Level.FAULT) //
				.text("BMS In Error")), //
		STATE_1(Doc.of(Level.FAULT) //
				.text("BMS Overvoltage")), //
		STATE_2(Doc.of(Level.FAULT) //
				.text("BMS Undervoltage")), //
		STATE_3(Doc.of(Level.FAULT) //
				.text("BMS Overcurrent")), //
		STATE_4(Doc.of(Level.FAULT) //
				.text("Error BMS Limits Not Initialized")), //
		STATE_5(Doc.of(Level.FAULT) //
				.text("Connect Error")), //
		STATE_6(Doc.of(Level.FAULT) //
				.text("Overvoltage Warning")), //
		STATE_7(Doc.of(Level.FAULT) //
				.text("Undervoltage Warning")), //
		STATE_8(Doc.of(Level.FAULT) //
				.text("Overcurrent Warning")), //
		STATE_9(Doc.of(Level.FAULT) //
				.text("BMS Ready")), //
		STATE_10(Doc.of(Level.FAULT) //
				.text("TREX Ready")), //

		STATE_11(Doc.of(Level.FAULT) //
				.text("Gateway Initialized")), //
		STATE_12(Doc.of(Level.FAULT) //
				.text("Modbus Slave Status")), //
		STATE_13(Doc.of(Level.FAULT) //
				.text("Modbus Master Status")), //
		STATE_14(Doc.of(Level.FAULT) //
				.text("CAN Timeout")), //
		STATE_15(Doc.of(Level.FAULT) //
				.text("First Communication Ok")), //

		INVERTER_STATE_0(Doc.of(Level.INFO) //
				.text("Ready to Power on")), //
		INVERTER_STATE_1(Doc.of(Level.INFO) //
				.text("Ready for Operating")), //
		INVERTER_STATE_2(Doc.of(Level.INFO) //
				.text("Enabled")), //
		INVERTER_STATE_3(Doc.of(Level.FAULT) //
				.text("Inverter Fault")), //
		INVERTER_STATE_7(Doc.of(Level.WARNING) //
				.text("Inverter Warning")), //
		INVERTER_STATE_8(Doc.of(Level.INFO) //
				.text("Voltage/Current mode")), //
		INVERTER_STATE_9(Doc.of(Level.INFO) //
				.text("Power mode")), //
		INVERTER_STATE_10(Doc.of(Level.INFO) //
				.text("Status AC relays (1:Close, 0:Open)")), //
		INVERTER_STATE_11(Doc.of(Level.INFO) //
				.text("Status DC relay 1 (1:Close, 0:Open)")), //
		INVERTER_STATE_12(Doc.of(Level.INFO) //
				.text("Status DC relay 2 (1:Close, 0:Open)")), //
		INVERTER_STATE_13(Doc.of(Level.INFO) //
				.text("Mains OK")), //

		DCDC_STATE_0(Doc.of(Level.INFO) //
				.text("Ready to Power on")), //
		DCDC_STATE_1(Doc.of(Level.INFO) //
				.text("Ready for Operating")), //
		DCDC_STATE_2(Doc.of(Level.INFO) //
				.text("Enabled")), //
		DCDC_STATE_3(Doc.of(Level.FAULT) //
				.text("DCDC Fault")), //
		DCDC_STATE_7(Doc.of(Level.WARNING) //
				.text("DCDC Warning")), //
		DCDC_STATE_8(Doc.of(Level.INFO) //
				.text("Voltage/Current mode")), //
		DCDC_STATE_9(Doc.of(Level.INFO) //
				.text("Power mode")), //

		BATTERY_ON_GRID_STATE_0(Doc.of(Level.INFO) //
				.text("On-grid status of battery group 0")), //
		BATTERY_ON_GRID_STATE_1(Doc.of(Level.INFO) //
				.text("On-grid status of battery group 1")), //
		BATTERY_ON_GRID_STATE_2(Doc.of(Level.INFO) //
				.text("On-grid status of battery group 2")), //
		BATTERY_ON_GRID_STATE_3(Doc.of(Level.INFO) //
				.text("On-grid status of battery group 3")), //
		BATTERY_ON_GRID_STATE_4(Doc.of(Level.INFO) //
				.text("On-grid status of battery group 4")), //
		BATTERY_ON_GRID_STATE_5(Doc.of(Level.INFO) //
				.text("On-grid status of battery group 5")), //
		BATTERY_ON_GRID_STATE_6(Doc.of(Level.INFO) //
				.text("On-grid status of battery group 6")), //
		BATTERY_ON_GRID_STATE_7(Doc.of(Level.INFO) //
				.text("On-grid status of battery group 7")), //
		BATTERY_ON_GRID_STATE_8(Doc.of(Level.INFO) //
				.text("On-grid status of battery group 8")), //
		BATTERY_ON_GRID_STATE_9(Doc.of(Level.INFO) //
				.text("On-grid status of battery group 9")), //
		BATTERY_ON_GRID_STATE_10(Doc.of(Level.INFO) //
				.text("On-grid status of battery group 10")), //
		BATTERY_ON_GRID_STATE_11(Doc.of(Level.INFO) //
				.text("On-grid status of battery group 11")), //
		BATTERY_ON_GRID_STATE_12(Doc.of(Level.INFO) //
				.text("On-grid status of battery group 12")), //
		BATTERY_ON_GRID_STATE_13(Doc.of(Level.INFO) //
				.text("On-grid status of battery group 13")), //
		BATTERY_ON_GRID_STATE_14(Doc.of(Level.INFO) //
				.text("On-grid status of battery group 14")), //
		BATTERY_ON_GRID_STATE_15(Doc.of(Level.INFO) //
				.text("On-grid status of battery group 15")), //

		STATE_16(Doc.of(Level.WARNING) //
				.text("Normal Charging Over Current")), //
		STATE_17(Doc.of(Level.WARNING) //
				.text("Charging Current Over Limit")), //
		STATE_18(Doc.of(Level.WARNING) //
				.text("Discharging Current Over Limit")), //
		STATE_19(Doc.of(Level.WARNING) //
				.text("Normal High Voltage")), //
		STATE_20(Doc.of(Level.WARNING) //
				.text("Normal Low Voltage")), //
		STATE_21(Doc.of(Level.WARNING) //
				.text("Abnormal Voltage Variation")), //
		STATE_22(Doc.of(Level.WARNING) //
				.text("Normal High Temperature")), //
		STATE_23(Doc.of(Level.WARNING) //
				.text("Normal Low Temperature")), //
		STATE_24(Doc.of(Level.WARNING) //
				.text("Abnormal Temperature Variation")), //
		STATE_25(Doc.of(Level.WARNING) //
				.text("Serious High Voltage")), //
		STATE_26(Doc.of(Level.WARNING) //
				.text("Serious Low Voltage")), //
		STATE_27(Doc.of(Level.WARNING) //
				.text("Serious Low Temperature")), //
		STATE_28(Doc.of(Level.WARNING) //
				.text("Charging Serious Over Current")), //
		STATE_29(Doc.of(Level.WARNING) //
				.text("Discharging Serious Over Current")), //
		STATE_30(Doc.of(Level.WARNING) //
				.text("Abnormal Capacity Alarm")), //

		STATE_31(Doc.of(Level.WARNING) //
				.text("EEPROM Parameter Failure")), //
		STATE_32(Doc.of(Level.WARNING) //
				.text("Switch Of Inside Combined Cabinet")), //
		STATE_33(Doc.of(Level.WARNING) //
				.text("Should Not Be Connected To Grid Due To The DC Side Condition")), //
		STATE_34(Doc.of(Level.WARNING) //
				.text("Emergency Stop Require From System Controller")), //

		STATE_35(Doc.of(Level.WARNING) //
				.text("Battery Group 1 Enable And Not Connected To Grid")), //
		STATE_36(Doc.of(Level.WARNING) //
				.text("Battery Group 2 Enable And Not Connected To Grid")), //
		STATE_37(Doc.of(Level.WARNING) //
				.text("Battery Group 3 Enable And Not Connected To Grid")), //
		STATE_38(Doc.of(Level.WARNING) //
				.text("Battery Group 4 Enable And Not Connected To Grid")), //

		STATE_39(Doc.of(Level.WARNING) //
				.text("TheIsolationSwitchOfBatteryGroup1Open")), //
		STATE_40(Doc.of(Level.WARNING) //
				.text("TheIsolationSwitchOfBatteryGroup2Open")), //
		STATE_41(Doc.of(Level.WARNING) //
				.text("TheIsolationSwitchOfBatteryGroup3Open")), //
		STATE_42(Doc.of(Level.WARNING) //
				.text("TheIsolationSwitchOfBatteryGroup4Open")), //

		STATE_43(Doc.of(Level.WARNING) //
				.text("BalancingSamplingFailureOfBatteryGroup1")), //
		STATE_44(Doc.of(Level.WARNING) //
				.text("BalancingSamplingFailureOfBatteryGroup2")), //
		STATE_45(Doc.of(Level.WARNING) //
				.text("BalancingSamplingFailureOfBatteryGroup3")), //
		STATE_46(Doc.of(Level.WARNING) //
				.text("BalancingSamplingFailureOfBatteryGroup4")), //

		STATE_47(Doc.of(Level.WARNING) //
				.text("BalancingControlFailureOfBatteryGroup1")), //
		STATE_48(Doc.of(Level.WARNING) //
				.text("BalancingControlFailureOfBatteryGroup2")), //
		STATE_49(Doc.of(Level.WARNING) //
				.text("BalancingControlFailureOfBatteryGroup3")), //
		STATE_50(Doc.of(Level.WARNING) //
				.text("BalancingControlFailureOfBatteryGroup4")), //

		STATE_51(Doc.of(Level.FAULT) //
				.text("NoEnableBateryGroupOrUsableBatteryGroup")), //
		STATE_52(Doc.of(Level.FAULT) //
				.text("NormalLeakageOfBatteryGroup")), //
		STATE_53(Doc.of(Level.FAULT) //
				.text("SeriousLeakageOfBatteryGroup")), //
		STATE_54(Doc.of(Level.FAULT) //
				.text("BatteryStartFailure")), //
		STATE_55(Doc.of(Level.FAULT) //
				.text("BatteryStopFailure")), //
		STATE_56(Doc.of(Level.FAULT) //
				.text("InterruptionOfCANCommunicationBetweenBatteryGroupAndController")), //
		STATE_57(Doc.of(Level.FAULT) //
				.text("EmergencyStopAbnormalOfAuxiliaryCollector")), //
		STATE_58(Doc.of(Level.FAULT) //
				.text("LeakageSelfDetectionOnNegative")), //
		STATE_59(Doc.of(Level.FAULT) //
				.text("LeakageSelfDetectionOnPositive")), //
		STATE_60(Doc.of(Level.FAULT) //
				.text("SelfDetectionFailureOnBattery")), //

		STATE_61(Doc.of(Level.FAULT) //
				.text("CANCommunicationInterruptionBetweenBatteryGroupAndGroup1")), //
		STATE_62(Doc.of(Level.FAULT) //
				.text("CANCommunicationInterruptionBetweenBatteryGroupAndGroup2")), //
		STATE_63(Doc.of(Level.FAULT) //
				.text("CANCommunicationInterruptionBetweenBatteryGroupAndGroup3")), //
		STATE_64(Doc.of(Level.FAULT) //
				.text("CANCommunicationInterruptionBetweenBatteryGroupAndGroup4")), //

		STATE_65(Doc.of(Level.FAULT) //
				.text("MainContractorAbnormalInBatterySelfDetectGroup1")), //
		STATE_66(Doc.of(Level.FAULT) //
				.text("MainContractorAbnormalInBatterySelfDetectGroup2")), //
		STATE_67(Doc.of(Level.FAULT) //
				.text("MainContractorAbnormalInBatterySelfDetectGroup3")), //
		STATE_68(Doc.of(Level.FAULT) //
				.text("MainContractorAbnormalInBatterySelfDetectGroup4")), //

		STATE_69(Doc.of(Level.FAULT) //
				.text("PreChargeContractorAbnormalOnBatterySelfDetectGroup1")), //
		STATE_70(Doc.of(Level.FAULT) //
				.text("PreChargeContractorAbnormalOnBatterySelfDetectGroup2")), //
		STATE_71(Doc.of(Level.FAULT) //
				.text("PreChargeContractorAbnormalOnBatterySelfDetectGroup3")), //
		STATE_72(Doc.of(Level.FAULT) //
				.text("PreChargeContractorAbnormalOnBatterySelfDetectGroup4")), //

		STATE_73(Doc.of(Level.FAULT) //
				.text("MainContactFailureOnBatteryControlGroup1")), //
		STATE_74(Doc.of(Level.FAULT) //
				.text("MainContactFailureOnBatteryControlGroup2")), //
		STATE_75(Doc.of(Level.FAULT) //
				.text("MainContactFailureOnBatteryControlGroup3")), //
		STATE_76(Doc.of(Level.FAULT) //
				.text("MainContactFailureOnBatteryControlGroup4")), //

		STATE_77(Doc.of(Level.FAULT) //
				.text("PreChargeFailureOnBatteryControlGroup1")), //
		STATE_78(Doc.of(Level.FAULT) //
				.text("PreChargeFailureOnBatteryControlGroup2")), //
		STATE_79(Doc.of(Level.FAULT) //
				.text("PreChargeFailureOnBatteryControlGroup3")), //
		STATE_80(Doc.of(Level.FAULT) //
				.text("PreChargeFailureOnBatteryControlGroup4")), //

		STATE_81(Doc.of(Level.FAULT) //
				.text("SamplingCircuitAbnormalForBMU")), //
		STATE_82(Doc.of(Level.FAULT) //
				.text("PowerCableDisconnectFailure")), //
		STATE_83(Doc.of(Level.FAULT) //
				.text("SamplingCircuitDisconnectFailure")), //
		STATE_84(Doc.of(Level.FAULT) //
				.text("CANDisconnectForMasterAndSlave")), //
		STATE_85(Doc.of(Level.FAULT) //
				.text("SammplingCircuitFailure")), //
		STATE_86(Doc.of(Level.FAULT) //
				.text("SingleBatteryFailure")), //
		STATE_87(Doc.of(Level.FAULT) //
				.text("CircuitDetectionAbnormalForMainContactor")), //
		STATE_88(Doc.of(Level.FAULT) //
				.text("CircuitDetectionAbnormalForMainContactorSecond")), //
		STATE_89(Doc.of(Level.FAULT) //
				.text("CircuitDetectionAbnormalForFancontactor")), //
		STATE_90(Doc.of(Level.FAULT) //
				.text("BMUPowerContactorCircuitDetectionAbnormal")), //
		STATE_91(Doc.of(Level.FAULT) //
				.text("CentralContactorCircuitDetectionAbnormal")), // 3

		STATE_92(Doc.of(Level.FAULT) //
				.text("SeriousTemperatureFault")), //
		STATE_93(Doc.of(Level.FAULT) //
				.text("CommunicationFaultForSystemController")), //
		STATE_94(Doc.of(Level.FAULT) //
				.text("FrogAlarm")), //
		STATE_95(Doc.of(Level.FAULT) //
				.text("FuseFault")), //
		STATE_96(Doc.of(Level.FAULT) //
				.text("NormalLeakage")), //
		STATE_97(Doc.of(Level.FAULT) //
				.text("SeriousLeakage")), //
		STATE_98(Doc.of(Level.FAULT) //
				.text("CANDisconnectionBetweenBatteryGroupAndBatteryStack")), //
		STATE_99(Doc.of(Level.FAULT) //
				.text("CentralContactorCircuitOpen")), //
		STATE_100(Doc.of(Level.FAULT) //
				.text("BMUPowerContactorOpen")), //

		BATTERY_CONTROL_STATE_0(Doc.of(Level.INFO) //
				.text("Control Status Battery Group 0")), //
		BATTERY_CONTROL_STATE_1(Doc.of(Level.INFO) //
				.text("Control Status Battery Group 1")), //
		BATTERY_CONTROL_STATE_2(Doc.of(Level.INFO) //
				.text("Control Status Battery Group 2")), //
		BATTERY_CONTROL_STATE_3(Doc.of(Level.INFO) //
				.text("Control Status Battery Group 3")), //
		BATTERY_CONTROL_STATE_4(Doc.of(Level.INFO) //
				.text("Control Status Battery Group 4")), //
		BATTERY_CONTROL_STATE_5(Doc.of(Level.INFO) //
				.text("Control Status Battery Group 5")), //
		BATTERY_CONTROL_STATE_6(Doc.of(Level.INFO) //
				.text("Control Status Battery Group 6")), //
		BATTERY_CONTROL_STATE_7(Doc.of(Level.INFO) //
				.text("Control Status Battery Group 7")), //
		BATTERY_CONTROL_STATE_8(Doc.of(Level.INFO) //
				.text("Control Status Battery Group 8")), //
		BATTERY_CONTROL_STATE_9(Doc.of(Level.INFO) //
				.text("Control Status Battery Group 9")), //
		BATTERY_CONTROL_STATE_10(Doc.of(Level.INFO) //
				.text("Control Status Battery Group 10")), //
		BATTERY_CONTROL_STATE_11(Doc.of(Level.INFO) //
				.text("Control Status Battery Group 11")), //
		BATTERY_CONTROL_STATE_12(Doc.of(Level.INFO) //
				.text("Control Status Battery Group 12")), //
		BATTERY_CONTROL_STATE_13(Doc.of(Level.INFO) //
				.text("Control Status Battery Group 13")), //
		BATTERY_CONTROL_STATE_14(Doc.of(Level.INFO) //
				.text("Control Status Battery Group 14")), //
		BATTERY_CONTROL_STATE_15(Doc.of(Level.INFO) //
				.text("Control Status Battery Group 15")), //

		ERROR_LOG_0(Doc.of(OpenemsType.INTEGER)), //
		ERROR_LOG_1(Doc.of(OpenemsType.INTEGER)), //
		ERROR_LOG_2(Doc.of(OpenemsType.INTEGER)), //
		ERROR_LOG_3(Doc.of(OpenemsType.INTEGER)), //
		ERROR_LOG_4(Doc.of(OpenemsType.INTEGER)), //
		ERROR_LOG_5(Doc.of(OpenemsType.INTEGER)), //
		ERROR_LOG_6(Doc.of(OpenemsType.INTEGER)), //
		ERROR_LOG_7(Doc.of(OpenemsType.INTEGER)), //
		ERROR_LOG_8(Doc.of(OpenemsType.INTEGER)), //
		ERROR_LOG_9(Doc.of(OpenemsType.INTEGER)), //
		ERROR_LOG_10(Doc.of(OpenemsType.INTEGER)), //
		ERROR_LOG_11(Doc.of(OpenemsType.INTEGER)), //
		ERROR_LOG_12(Doc.of(OpenemsType.INTEGER)), //
		ERROR_LOG_13(Doc.of(OpenemsType.INTEGER)), //
		ERROR_LOG_14(Doc.of(OpenemsType.INTEGER)), //
		ERROR_LOG_15(Doc.of(OpenemsType.INTEGER));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	private IntegerWriteChannel getSetActivePowerL1Channel() {
		return this.channel(RefuEss.ChannelId.SET_ACTIVE_POWER_L1);
	}

	private IntegerWriteChannel getSetActivePowerL2Channel() {
		return this.channel(RefuEss.ChannelId.SET_ACTIVE_POWER_L2);
	}

	private IntegerWriteChannel getSetActivePowerL3Channel() {
		return this.channel(RefuEss.ChannelId.SET_ACTIVE_POWER_L3);
	}

	private IntegerWriteChannel getSetReactivePowerL1Channel() {
		return this.channel(RefuEss.ChannelId.SET_REACTIVE_POWER_L1);
	}

	private IntegerWriteChannel getSetReactivePowerL2Channel() {
		return this.channel(RefuEss.ChannelId.SET_REACTIVE_POWER_L2);
	}

	private IntegerWriteChannel getSetReactivePowerL3Channel() {
		return this.channel(RefuEss.ChannelId.SET_REACTIVE_POWER_L3);
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public int getPowerPrecision() {
		return 100;
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.errorHandler.run();
			break;
		}
	}

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	@Override
	protected void logError(Logger log, String message) {
		super.logError(log, message);
	}

	public Constraint[] getStaticConstraints() throws OpenemsException {
		SystemState systemState = this.channel(ChannelId.SYSTEM_STATE).value().asEnum();
		switch (systemState) {
		case ERROR:
		case INIT:
		case OFF:
		case PRE_OPERATION:
		case STANDBY:
		case UNDEFINED:
			return new Constraint[] {
					this.power.createSimpleConstraint("Refu State: " + systemState, this, Phase.L1, Pwr.ACTIVE,
							Relationship.EQUALS, 0),
					this.power.createSimpleConstraint("Refu State: " + systemState, this, Phase.L2, Pwr.ACTIVE,
							Relationship.EQUALS, 0),
					this.power.createSimpleConstraint("Refu State: " + systemState, this, Phase.L3, Pwr.ACTIVE,
							Relationship.EQUALS, 0),
					this.power.createSimpleConstraint("Refu State: " + systemState, this, Phase.L3, Pwr.REACTIVE,
							Relationship.EQUALS, 0),
					this.power.createSimpleConstraint("Refu State: " + systemState, this, Phase.L3, Pwr.REACTIVE,
							Relationship.EQUALS, 0),
					this.power.createSimpleConstraint("Refu State: " + systemState, this, Phase.L3, Pwr.REACTIVE,
							Relationship.EQUALS, 0) };

		case OPERATION:
			break;
		}
		return Power.NO_CONSTRAINTS;
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable( //
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricEss.getModbusSlaveNatureTable(accessMode), //
				AsymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ManagedAsymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(RefuEss.class, accessMode, 300) //
						.build());
	}
}
