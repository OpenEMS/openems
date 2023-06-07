package io.openems.edge.ess.refubeckhoff;

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
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
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
import io.openems.edge.common.channel.EnumWriteChannel;
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
import io.openems.edge.ess.refubeckhoff.enums.SetOperationMode;
import io.openems.edge.ess.refubeckhoff.enums.StopStart;
import io.openems.edge.ess.refubeckhoff.enums.SystemState;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Ess.Refubeckhoff", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
		} //
)
public class RefuBeckhoffEssImpl extends AbstractOpenemsModbusComponent implements RefuBeckhoffEss, AsymmetricEss,
		ManagedAsymmetricEss, OpenemsComponent, EventHandler, ModbusComponent, ModbusSlave {

	private final Logger log = LoggerFactory.getLogger(RefuBeckhoffEssImpl.class);

	protected static final int MAX_APPARENT_POWER = 100_000;
	private static final int UNIT_ID = 1;
	// Resetting the Error errorTrail times
	private int errorTrail;
	private boolean chkError = false;

	@Reference
	private Power power;

	@Reference
	protected ConfigurationAdmin cm;

	private Config config = null;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public RefuBeckhoffEssImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				AsymmetricEss.ChannelId.values(), //
				ManagedAsymmetricEss.ChannelId.values(), //
				RefuBeckhoffEss.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), UNIT_ID, this.cm, "Modbus",
				config.modbus_id())) {
			return;
		}
		this.operationalMode(this.config.operationState());
		this._setGridMode(GridMode.ON_GRID);
		this._setMaxApparentPower(RefuBeckhoffEssImpl.MAX_APPARENT_POWER);
		this.errorTrail = this.config.acknowledgeError();

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
			this.handleEssState();
			break;
		}
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().asString() //
				+ "|L:" + this.getActivePower().asString() //
				+ "|Allowed:" + this.getAllowedChargePower().asStringWithoutUnit() + ";"
				+ this.getAllowedDischargePower().asString(); //
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		ModbusProtocol protocol = new ModbusProtocol(this, //
				new FC4ReadInputRegistersTask(0x100, Priority.HIGH, //
						m(RefuBeckhoffEss.ChannelId.SYSTEM_STATE, new UnsignedWordElement(0x100)), //
						m(new BitsWordElement(0x101, this) //
								.bit(0, RefuBeckhoffEss.ChannelId.STATE_BMS_ERROR) //
								.bit(1, RefuBeckhoffEss.ChannelId.STATE_BMS_OVERVOLTAGE) //
								.bit(2, RefuBeckhoffEss.ChannelId.STATE_BMS_UNDERVOLTAGE) //
								.bit(3, RefuBeckhoffEss.ChannelId.STATE_BMS_OVERCURRENT) //
								.bit(4, RefuBeckhoffEss.ChannelId.STATE_BMS_LIMIT_NOT_INITIALIZED) //
								.bit(5, RefuBeckhoffEss.ChannelId.STATE_CONNECT_ERROR) //
								.bit(6, RefuBeckhoffEss.ChannelId.STATE_OVERVOLTAGE_WARNING) //
								.bit(7, RefuBeckhoffEss.ChannelId.STATE_UNDERVOLTAGE_WARNING) //
								.bit(8, RefuBeckhoffEss.ChannelId.STATE_OVERCURRENT_WARNING) //
								.bit(9, RefuBeckhoffEss.ChannelId.STATE_BMS_READY, BitConverter.INVERT) //
								.bit(10, RefuBeckhoffEss.ChannelId.STATE_TREX_READY, BitConverter.INVERT)), //
						m(new BitsWordElement(0x102, this) //
								.bit(0, RefuBeckhoffEss.ChannelId.STATE_GATEWAY_INITIALIZED, BitConverter.INVERT) //
								.bit(1, RefuBeckhoffEss.ChannelId.STATE_MODBUS_SLAVE_STATUS, BitConverter.INVERT) //
								.bit(2, RefuBeckhoffEss.ChannelId.STATE_MODBUS_MASTER_STATUS, BitConverter.INVERT) //
								.bit(3, RefuBeckhoffEss.ChannelId.STATE_CAN_TIMEOUT) //
								.bit(4, RefuBeckhoffEss.ChannelId.STATE_FIRST_COMMNUICATION_OK, BitConverter.INVERT)), //
						m(new BitsWordElement(0x103, this) //
								.bit(0, RefuBeckhoffEss.ChannelId.INVERTER_STATE_READYTOPOWERON) //
								.bit(1, RefuBeckhoffEss.ChannelId.INVERTER_STATE_READYFOROPERATING) //
								.bit(2, RefuBeckhoffEss.ChannelId.INVERTER_STATE_ENABLED) //
								.bit(3, RefuBeckhoffEss.ChannelId.INVERTER_STATE_FAULT) //
								.bit(7, RefuBeckhoffEss.ChannelId.INVERTER_STATE_WARNING) //
								.bit(8, RefuBeckhoffEss.ChannelId.INVERTER_STATE_VOLTAGE_CURRENT_MODE) //
								.bit(9, RefuBeckhoffEss.ChannelId.INVERTER_STATE_POWER_MODE) //
								.bit(10, RefuBeckhoffEss.ChannelId.INVERTER_STATE_STATUS_AC_RELAYS) //
								.bit(11, RefuBeckhoffEss.ChannelId.INVERTER_STATE_STATUS_DC_1_RELAYS) //
								.bit(12, RefuBeckhoffEss.ChannelId.INVERTER_STATE__STATUS_DC_2_RELAYS) //
								.bit(13, RefuBeckhoffEss.ChannelId.INVERTER_STATE_MAINS_OK)),
						m(RefuBeckhoffEss.ChannelId.INVERTER_ERROR_CODE, new UnsignedWordElement(0x104)), //
						m(new BitsWordElement(0x105, this) //
								.bit(0, RefuBeckhoffEss.ChannelId.DCDC_STATE_READYTOPOWERON) //
								.bit(1, RefuBeckhoffEss.ChannelId.DCDC_STATE_READYFOROPERATING) //
								.bit(2, RefuBeckhoffEss.ChannelId.DCDC_STATE_ENABLED) //
								.bit(3, RefuBeckhoffEss.ChannelId.DCDC_STATE_DCDC_FAULT) //
								.bit(7, RefuBeckhoffEss.ChannelId.DCDC_STATE_DCDC_WARNING) //
								.bit(8, RefuBeckhoffEss.ChannelId.DCDC_STATE_VOLTAGE_CURRENT_MODE) //
								.bit(9, RefuBeckhoffEss.ChannelId.DCDC_STATE_POWER_MODE)),
						m(RefuBeckhoffEss.ChannelId.DCDC_ERROR_CODE, new UnsignedWordElement(0x106)), //
						m(RefuBeckhoffEss.ChannelId.BATTERY_CURRENT_PCS, new SignedWordElement(0x107),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(RefuBeckhoffEss.ChannelId.BATTERY_VOLTAGE_PCS, new SignedWordElement(0x108),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(RefuBeckhoffEss.ChannelId.CURRENT, new SignedWordElement(0x109),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(RefuBeckhoffEss.ChannelId.CURRENT_L1, new SignedWordElement(0x10A),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(RefuBeckhoffEss.ChannelId.CURRENT_L2, new SignedWordElement(0x10B),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(RefuBeckhoffEss.ChannelId.CURRENT_L3, new SignedWordElement(0x10C),
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
						m(RefuBeckhoffEss.ChannelId.COS_PHI_3P, new SignedWordElement(0x115)),
						m(RefuBeckhoffEss.ChannelId.COS_PHI_L1, new SignedWordElement(0x116)),
						m(RefuBeckhoffEss.ChannelId.COS_PHI_L2, new SignedWordElement(0x117)),
						m(RefuBeckhoffEss.ChannelId.COS_PHI_L3, new SignedWordElement(0x118))),

				new FC4ReadInputRegistersTask(0x11A, Priority.LOW, //
						m(RefuBeckhoffEss.ChannelId.PCS_ALLOWED_CHARGE, new SignedWordElement(0x11A),
								ElementToChannelConverter.SCALE_FACTOR_2_AND_INVERT),
						m(RefuBeckhoffEss.ChannelId.PCS_ALLOWED_DISCHARGE, new SignedWordElement(0x11B),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(RefuBeckhoffEss.ChannelId.BATTERY_STATE, new UnsignedWordElement(0x11C)), //
						m(RefuBeckhoffEss.ChannelId.BATTERY_MODE, new UnsignedWordElement(0x11D)), //
						m(RefuBeckhoffEss.ChannelId.BATTERY_VOLTAGE, new UnsignedWordElement(0x11E)), //
						m(RefuBeckhoffEss.ChannelId.BATTERY_CURRENT, new SignedWordElement(0x11F)), //
						m(RefuBeckhoffEss.ChannelId.BATTERY_POWER, new SignedWordElement(0x120)), //
						m(SymmetricEss.ChannelId.SOC, new UnsignedWordElement(0x121)), //
						m(RefuBeckhoffEss.ChannelId.CHARGE_MAX_CURRENT, new UnsignedWordElement(0x122),
								ElementToChannelConverter.SCALE_FACTOR_2_AND_INVERT),
						m(RefuBeckhoffEss.ChannelId.DISCHARGE_MAX_CURRENT, new UnsignedWordElement(0x123),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER, new UnsignedWordElement(0x124),
								ElementToChannelConverter.SCALE_FACTOR_2_AND_INVERT), //
						m(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER, new UnsignedWordElement(0x125),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(RefuBeckhoffEss.ChannelId.BATTERY_CHARGE_ENERGY,
								new SignedDoublewordElement(0x126).wordOrder(WordOrder.LSWMSW)), //
						m(RefuBeckhoffEss.ChannelId.BATTERY_DISCHARGE_ENERGY,
								new SignedDoublewordElement(0x128).wordOrder(WordOrder.LSWMSW)), //
						m(new BitsWordElement(0x12A, this) //
								.bit(0, RefuBeckhoffEss.ChannelId.BATTERY_ON_GRID_STATE_0) //
								.bit(1, RefuBeckhoffEss.ChannelId.BATTERY_ON_GRID_STATE_1) //
								.bit(2, RefuBeckhoffEss.ChannelId.BATTERY_ON_GRID_STATE_2) //
								.bit(3, RefuBeckhoffEss.ChannelId.BATTERY_ON_GRID_STATE_3) //
								.bit(4, RefuBeckhoffEss.ChannelId.BATTERY_ON_GRID_STATE_4) //
								.bit(5, RefuBeckhoffEss.ChannelId.BATTERY_ON_GRID_STATE_5) //
								.bit(6, RefuBeckhoffEss.ChannelId.BATTERY_ON_GRID_STATE_6) //
								.bit(7, RefuBeckhoffEss.ChannelId.BATTERY_ON_GRID_STATE_7) //
								.bit(8, RefuBeckhoffEss.ChannelId.BATTERY_ON_GRID_STATE_8) //
								.bit(9, RefuBeckhoffEss.ChannelId.BATTERY_ON_GRID_STATE_9) //
								.bit(10, RefuBeckhoffEss.ChannelId.BATTERY_ON_GRID_STATE_10) //
								.bit(11, RefuBeckhoffEss.ChannelId.BATTERY_ON_GRID_STATE_11) //
								.bit(12, RefuBeckhoffEss.ChannelId.BATTERY_ON_GRID_STATE_12) //
								.bit(13, RefuBeckhoffEss.ChannelId.BATTERY_ON_GRID_STATE_13) //
								.bit(14, RefuBeckhoffEss.ChannelId.BATTERY_ON_GRID_STATE_14) //
								.bit(15, RefuBeckhoffEss.ChannelId.BATTERY_ON_GRID_STATE_15)),
						m(RefuBeckhoffEss.ChannelId.BATTERY_HIGHEST_VOLTAGE, new UnsignedWordElement(0x12B)), //
						m(RefuBeckhoffEss.ChannelId.BATTERY_LOWEST_VOLTAGE, new UnsignedWordElement(0x12C)), //
						m(RefuBeckhoffEss.ChannelId.BATTERY_HIGHEST_TEMPERATURE, new SignedWordElement(0x12D)), //
						m(RefuBeckhoffEss.ChannelId.BATTERY_LOWEST_TEMPERATURE, new SignedWordElement(0x12E)), //
						m(RefuBeckhoffEss.ChannelId.BATTERY_STOP_REQUEST, new UnsignedWordElement(0x12F)), //
						m(new BitsWordElement(0x130, this) //
								.bit(0, RefuBeckhoffEss.ChannelId.STATE_16) //
								.bit(1, RefuBeckhoffEss.ChannelId.STATE_17) //
								.bit(2, RefuBeckhoffEss.ChannelId.STATE_18) //
								.bit(3, RefuBeckhoffEss.ChannelId.STATE_19) //
								.bit(4, RefuBeckhoffEss.ChannelId.STATE_20) //
								.bit(5, RefuBeckhoffEss.ChannelId.STATE_21) //
								.bit(6, RefuBeckhoffEss.ChannelId.STATE_22) //
								.bit(7, RefuBeckhoffEss.ChannelId.STATE_23) //
								.bit(8, RefuBeckhoffEss.ChannelId.STATE_24) //
								.bit(9, RefuBeckhoffEss.ChannelId.STATE_25) //
								.bit(10, RefuBeckhoffEss.ChannelId.STATE_26) //
								.bit(11, RefuBeckhoffEss.ChannelId.STATE_27) //
								.bit(12, RefuBeckhoffEss.ChannelId.STATE_28) //
								.bit(13, RefuBeckhoffEss.ChannelId.STATE_29) //
								.bit(14, RefuBeckhoffEss.ChannelId.STATE_30)),
						m(new BitsWordElement(0x131, this) //
								.bit(0, RefuBeckhoffEss.ChannelId.STATE_31) //
								.bit(1, RefuBeckhoffEss.ChannelId.STATE_32) //
								.bit(5, RefuBeckhoffEss.ChannelId.STATE_33) //
								.bit(7, RefuBeckhoffEss.ChannelId.STATE_34)),
						m(new BitsWordElement(0x132, this) //
								.bit(0, RefuBeckhoffEss.ChannelId.STATE_35) //
								.bit(1, RefuBeckhoffEss.ChannelId.STATE_36) //
								.bit(2, RefuBeckhoffEss.ChannelId.STATE_37) //
								.bit(3, RefuBeckhoffEss.ChannelId.STATE_38)),
						m(new BitsWordElement(0x133, this) //
								.bit(0, RefuBeckhoffEss.ChannelId.STATE_39) //
								.bit(1, RefuBeckhoffEss.ChannelId.STATE_40) //
								.bit(2, RefuBeckhoffEss.ChannelId.STATE_41) //
								.bit(3, RefuBeckhoffEss.ChannelId.STATE_42)),
						new DummyRegisterElement(0x134), //
						m(new BitsWordElement(0x135, this) //
								.bit(0, RefuBeckhoffEss.ChannelId.STATE_43) //
								.bit(1, RefuBeckhoffEss.ChannelId.STATE_44) //
								.bit(2, RefuBeckhoffEss.ChannelId.STATE_45) //
								.bit(3, RefuBeckhoffEss.ChannelId.STATE_46)),
						m(new BitsWordElement(0x136, this) //
								.bit(0, RefuBeckhoffEss.ChannelId.STATE_47) //
								.bit(1, RefuBeckhoffEss.ChannelId.STATE_48) //
								.bit(2, RefuBeckhoffEss.ChannelId.STATE_49) //
								.bit(3, RefuBeckhoffEss.ChannelId.STATE_50)),
						m(new BitsWordElement(0x137, this) //
								.bit(0, RefuBeckhoffEss.ChannelId.STATE_51) //
								.bit(1, RefuBeckhoffEss.ChannelId.STATE_52) //
								.bit(2, RefuBeckhoffEss.ChannelId.STATE_53) //
								.bit(3, RefuBeckhoffEss.ChannelId.STATE_54) //
								.bit(4, RefuBeckhoffEss.ChannelId.STATE_55) //
								.bit(5, RefuBeckhoffEss.ChannelId.STATE_56) //
								.bit(10, RefuBeckhoffEss.ChannelId.STATE_57) //
								.bit(11, RefuBeckhoffEss.ChannelId.STATE_58) //
								.bit(12, RefuBeckhoffEss.ChannelId.STATE_59) //
								.bit(13, RefuBeckhoffEss.ChannelId.STATE_60)),
						m(new BitsWordElement(0x138, this) //
								.bit(0, RefuBeckhoffEss.ChannelId.STATE_61) //
								.bit(1, RefuBeckhoffEss.ChannelId.STATE_62) //
								.bit(2, RefuBeckhoffEss.ChannelId.STATE_63) //
								.bit(3, RefuBeckhoffEss.ChannelId.STATE_64)),
						m(new BitsWordElement(0x139, this) //
								.bit(0, RefuBeckhoffEss.ChannelId.STATE_65) //
								.bit(1, RefuBeckhoffEss.ChannelId.STATE_66) //
								.bit(2, RefuBeckhoffEss.ChannelId.STATE_67) //
								.bit(3, RefuBeckhoffEss.ChannelId.STATE_68)),
						m(new BitsWordElement(0x13A, this) //
								.bit(0, RefuBeckhoffEss.ChannelId.STATE_69) //
								.bit(1, RefuBeckhoffEss.ChannelId.STATE_70) //
								.bit(2, RefuBeckhoffEss.ChannelId.STATE_71) //
								.bit(3, RefuBeckhoffEss.ChannelId.STATE_72)),
						m(new BitsWordElement(0x13B, this) //
								.bit(0, RefuBeckhoffEss.ChannelId.STATE_73) //
								.bit(1, RefuBeckhoffEss.ChannelId.STATE_74) //
								.bit(2, RefuBeckhoffEss.ChannelId.STATE_75) //
								.bit(3, RefuBeckhoffEss.ChannelId.STATE_76)),
						m(new BitsWordElement(0x13C, this) //
								.bit(0, RefuBeckhoffEss.ChannelId.STATE_77) //
								.bit(1, RefuBeckhoffEss.ChannelId.STATE_78) //
								.bit(2, RefuBeckhoffEss.ChannelId.STATE_79) //
								.bit(3, RefuBeckhoffEss.ChannelId.STATE_80)),
						new DummyRegisterElement(0x13D), //
						new DummyRegisterElement(0x13E), //
						m(new BitsWordElement(0x13F, this) //
								.bit(2, RefuBeckhoffEss.ChannelId.STATE_81) //
								.bit(3, RefuBeckhoffEss.ChannelId.STATE_82) //
								.bit(4, RefuBeckhoffEss.ChannelId.STATE_83) //
								.bit(6, RefuBeckhoffEss.ChannelId.STATE_84) //
								.bit(9, RefuBeckhoffEss.ChannelId.STATE_85) //
								.bit(10, RefuBeckhoffEss.ChannelId.STATE_86) //
								.bit(11, RefuBeckhoffEss.ChannelId.STATE_87) //
								.bit(12, RefuBeckhoffEss.ChannelId.STATE_88) //
								.bit(13, RefuBeckhoffEss.ChannelId.STATE_89) //
								.bit(14, RefuBeckhoffEss.ChannelId.STATE_90) //
								.bit(15, RefuBeckhoffEss.ChannelId.STATE_91)),
						m(new BitsWordElement(0x140, this) //
								.bit(2, RefuBeckhoffEss.ChannelId.STATE_92) //
								.bit(3, RefuBeckhoffEss.ChannelId.STATE_93) //
								.bit(7, RefuBeckhoffEss.ChannelId.STATE_94) //
								.bit(8, RefuBeckhoffEss.ChannelId.STATE_95) //
								.bit(10, RefuBeckhoffEss.ChannelId.STATE_96) //
								.bit(11, RefuBeckhoffEss.ChannelId.STATE_97) //
								.bit(12, RefuBeckhoffEss.ChannelId.STATE_98) //
								.bit(13, RefuBeckhoffEss.ChannelId.STATE_99) //
								.bit(14, RefuBeckhoffEss.ChannelId.STATE_100)),
						new DummyRegisterElement(0x141), //
						new DummyRegisterElement(0x142), //
						new DummyRegisterElement(0x143), //
						new DummyRegisterElement(0x144), //
						m(new BitsWordElement(0x145, this) //
								.bit(0, RefuBeckhoffEss.ChannelId.BATTERY_CONTROL_STATE_0) //
								.bit(1, RefuBeckhoffEss.ChannelId.BATTERY_CONTROL_STATE_1) //
								.bit(2, RefuBeckhoffEss.ChannelId.BATTERY_CONTROL_STATE_2) //
								.bit(3, RefuBeckhoffEss.ChannelId.BATTERY_CONTROL_STATE_3) //
								.bit(4, RefuBeckhoffEss.ChannelId.BATTERY_CONTROL_STATE_4) //
								.bit(5, RefuBeckhoffEss.ChannelId.BATTERY_CONTROL_STATE_5) //
								.bit(6, RefuBeckhoffEss.ChannelId.BATTERY_CONTROL_STATE_6) //
								.bit(7, RefuBeckhoffEss.ChannelId.BATTERY_CONTROL_STATE_7) //
								.bit(8, RefuBeckhoffEss.ChannelId.BATTERY_CONTROL_STATE_8) //
								.bit(9, RefuBeckhoffEss.ChannelId.BATTERY_CONTROL_STATE_9) //
								.bit(10, RefuBeckhoffEss.ChannelId.BATTERY_CONTROL_STATE_10) //
								.bit(11, RefuBeckhoffEss.ChannelId.BATTERY_CONTROL_STATE_11) //
								.bit(12, RefuBeckhoffEss.ChannelId.BATTERY_CONTROL_STATE_12) //
								.bit(13, RefuBeckhoffEss.ChannelId.BATTERY_CONTROL_STATE_13) //
								.bit(14, RefuBeckhoffEss.ChannelId.BATTERY_CONTROL_STATE_14) //
								.bit(15, RefuBeckhoffEss.ChannelId.BATTERY_CONTROL_STATE_15)),
						m(RefuBeckhoffEss.ChannelId.ERROR_LOG_0, new UnsignedWordElement(0x146)), //
						m(RefuBeckhoffEss.ChannelId.ERROR_LOG_1, new UnsignedWordElement(0x147)), //
						m(RefuBeckhoffEss.ChannelId.ERROR_LOG_2, new UnsignedWordElement(0x148)), //
						m(RefuBeckhoffEss.ChannelId.ERROR_LOG_3, new UnsignedWordElement(0x149)), //
						m(RefuBeckhoffEss.ChannelId.ERROR_LOG_4, new UnsignedWordElement(0x14A)), //
						m(RefuBeckhoffEss.ChannelId.ERROR_LOG_5, new UnsignedWordElement(0x14B)), //
						m(RefuBeckhoffEss.ChannelId.ERROR_LOG_6, new UnsignedWordElement(0x14C)), //
						m(RefuBeckhoffEss.ChannelId.ERROR_LOG_7, new UnsignedWordElement(0x14D)), //
						m(RefuBeckhoffEss.ChannelId.ERROR_LOG_8, new UnsignedWordElement(0x14E)), //
						m(RefuBeckhoffEss.ChannelId.ERROR_LOG_9, new UnsignedWordElement(0x14F)), //
						m(RefuBeckhoffEss.ChannelId.ERROR_LOG_10, new UnsignedWordElement(0x150)), //
						m(RefuBeckhoffEss.ChannelId.ERROR_LOG_11, new UnsignedWordElement(0x151)), //
						m(RefuBeckhoffEss.ChannelId.ERROR_LOG_12, new UnsignedWordElement(0x152)), //
						m(RefuBeckhoffEss.ChannelId.ERROR_LOG_13, new UnsignedWordElement(0x153)), //
						m(RefuBeckhoffEss.ChannelId.ERROR_LOG_14, new UnsignedWordElement(0x154)), //
						m(RefuBeckhoffEss.ChannelId.ERROR_LOG_15, new UnsignedWordElement(0x155)) //
				),

				new FC3ReadRegistersTask(0x200, Priority.LOW, //
						m(RefuBeckhoffEss.ChannelId.WORK_STATE, new UnsignedWordElement(0x200)),
						m(RefuBeckhoffEss.ChannelId.SYSTEM_ERROR_RESET, new UnsignedWordElement(0x201)),
						m(RefuBeckhoffEss.ChannelId.OPERATION_MODE, new UnsignedWordElement(0x202))),

				new FC16WriteRegistersTask(0x200, //
						m(RefuBeckhoffEss.ChannelId.SET_WORK_STATE, new UnsignedWordElement(0x200))), //
				new FC16WriteRegistersTask(0x201, //
						m(RefuBeckhoffEss.ChannelId.SET_SYSTEM_ERROR_RESET, new UnsignedWordElement(0x201))),
				new FC16WriteRegistersTask(0x202, //
						m(RefuBeckhoffEss.ChannelId.SET_OPERATION_MODE, new UnsignedWordElement(0x202))), //
				new FC16WriteRegistersTask(0x203, //
						m(ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS, new SignedWordElement(0x203),
								ElementToChannelConverter.SCALE_FACTOR_2)),
				new FC16WriteRegistersTask(0x204, //
						m(ManagedAsymmetricEss.ChannelId.SET_ACTIVE_POWER_L1_EQUALS, new SignedWordElement(0x204),
								ElementToChannelConverter.SCALE_FACTOR_2)), //
				new FC16WriteRegistersTask(0x205, //
						m(ManagedAsymmetricEss.ChannelId.SET_ACTIVE_POWER_L2_EQUALS, new SignedWordElement(0x205),
								ElementToChannelConverter.SCALE_FACTOR_2)),
				new FC16WriteRegistersTask(0x206, //
						m(ManagedAsymmetricEss.ChannelId.SET_ACTIVE_POWER_L3_EQUALS, new SignedWordElement(0x206),
								ElementToChannelConverter.SCALE_FACTOR_2)), //
				new FC16WriteRegistersTask(0x207, //
						m(ManagedSymmetricEss.ChannelId.SET_REACTIVE_POWER_EQUALS, new SignedWordElement(0x207),
								ElementToChannelConverter.SCALE_FACTOR_2)),
				new FC16WriteRegistersTask(0x208, //
						m(ManagedAsymmetricEss.ChannelId.SET_REACTIVE_POWER_L1_EQUALS, new SignedWordElement(0x208),
								ElementToChannelConverter.SCALE_FACTOR_2)),
				new FC16WriteRegistersTask(0x209, //
						m(ManagedAsymmetricEss.ChannelId.SET_REACTIVE_POWER_L2_EQUALS, new SignedWordElement(0x209),
								ElementToChannelConverter.SCALE_FACTOR_2)),
				new FC16WriteRegistersTask(0x20A, //
						m(ManagedAsymmetricEss.ChannelId.SET_REACTIVE_POWER_L3_EQUALS, new SignedWordElement(0x20A),
								ElementToChannelConverter.SCALE_FACTOR_2)));//

		return protocol;
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricEss.getModbusSlaveNatureTable(accessMode), //
				AsymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ManagedAsymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(RefuBeckhoffEss.class, accessMode, 300) //
						.build());
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
	public void applyPower(int activePowerL1, int reactivePowerL1, int activePowerL2, int reactivePowerL2,
			int activePowerL3, int reactivePowerL3) throws OpenemsNamedException {
		int activePower = activePowerL1 + activePowerL2 + activePowerL3;
		int allowedCharge = this.getAllowedChargePower().orElse(0);
		int allowedDischarge = this.getAllowedDischargePower().orElse(0);

		/*
		 * Specific handling for REFU to never be at -5000 < power < 5000 if battery is
		 * full/empty
		 */
		if (// Battery is full and discharge power < 5000
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

		setSetActivePowerL1Equals(activePowerL1);
		setSetActivePowerL2Equals(activePowerL2);
		setSetActivePowerL3Equals(activePowerL3);
		setSetReactivePowerL1Equals(reactivePowerL1);
		setSetReactivePowerL2Equals(reactivePowerL2);
		setSetReactivePowerL3Equals(reactivePowerL3);
	}

	@Override
	public Constraint[] getStaticConstraints() throws OpenemsException {
		SystemState systemState = this.channel(RefuBeckhoffEss.ChannelId.SYSTEM_STATE).value().asEnum();
		switch (systemState) {
		case ERROR:
		case INIT:
		case OFF:
		case PRE_OPERATION:
		case UNDEFINED:
			if (this.config.symmetricMode()) {
				return new Constraint[] {
						this.power.createSimpleConstraint("Refu State: " + systemState, this, Phase.ALL, Pwr.ACTIVE,
								Relationship.EQUALS, 0),
						this.power.createSimpleConstraint("Refu State: " + systemState, this, Phase.ALL, Pwr.REACTIVE,
								Relationship.EQUALS, 0) };
			} else {
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
			}
		case OPERATION:
			break;
		case STANDBY:
			break;
		}
		return Power.NO_CONSTRAINTS;
	}

	/**
	 * This Method set the operational mode from the {@link Config}.
	 * 
	 * @param setOperationMode {@link setOperationMode} the operational state of the
	 *                         system.
	 */
	private void operationalMode(SetOperationMode setOperationMode) {
		EnumWriteChannel currentState = this.channel(RefuBeckhoffEss.ChannelId.SET_OPERATION_MODE);
		try {
			currentState.setNextWriteValue(setOperationMode);
		} catch (OpenemsNamedException e) {
			this.log.error("Enum write channel (SET_OPERATION_MODE) was not able to set");
		}
	}

	/**
	 * Handle state machine based on the input from the {@link Config}.
	 */
	private void handleEssState() {
		switch (this.config.essState()) {
		case AUTO:
			this.handleStateMachine();
			break;
		case FORCE_TO_STOP:
			this.setWorkingstate(StopStart.STOP);
			break;
		case FORCE_TO_START:
			this.setWorkingstate(StopStart.START);
			break;
		}
	}

	/**
	 * This method sets the work state to start or stop.
	 * 
	 * @param start {@link StopStart} stop start enum.
	 */
	private void setWorkingstate(StopStart start) {

		EnumWriteChannel currentState = this.channel(RefuBeckhoffEss.ChannelId.SET_WORK_STATE);
		try {
			currentState.setNextWriteValue(start);
		} catch (OpenemsNamedException e) {
			this.log.error("Enum write channel (SET_WORK_STATE) was not able to set");
		}
	}

	/**
	 * Method to handle the state machine of the ESS inverter.
	 */
	private void handleStateMachine() {

		switch (this.getSystemState()) {
		case ERROR:
			this.log.info("Error state");
			if (this.handleError()) {
				this.log.info("Tried starting for " + this.errorTrail + " times, now stopping");
			}
			break;
		case OFF:
			this.log.info("Off state");
			this.setWorkingstate(StopStart.STOP);
			break;
		case INIT:
			this.log.info("Init State");
			this.setWorkingstate(StopStart.START);
			break;
		case OPERATION:
			this.log.info("Operational state");
			this.setWorkingstate(StopStart.START);
			break;
		case PRE_OPERATION:
			this.log.info("Pre_Operational state");
			this.setWorkingstate(StopStart.START);
			break;
		case STANDBY:
			this.log.info("Stand-by state");
			this.setWorkingstate(StopStart.START);
			break;
		case UNDEFINED:
			this.log.info("Undefined state");
			break;
		}
	}

	/**
	 * This method resets the error, or acknowledges the error for specific time.
	 * 
	 * @return boolean error checked or not.
	 */
	private boolean handleError() {
		if (this.errorTrail > 0 && !this.chkError) {
			// Reset Error
			this.setErrorReset();
			this.log.info("Acknowledges the Error for " //
					+ (this.config.acknowledgeError() - this.errorTrail) + " times");
			this.errorTrail = this.errorTrail - 1;
			this.setWorkingstate(StopStart.START);
			return this.chkError = false;
		} else {
			this.setWorkingstate(StopStart.STOP);
			this.errorTrail = this.config.acknowledgeError();
			return this.chkError = true;

		}

	}

	private void setErrorReset() {
		EnumWriteChannel currentState = this.channel(RefuBeckhoffEss.ChannelId.SET_SYSTEM_ERROR_RESET);
		try {
			currentState.setNextWriteValue(StopStart.START);
		} catch (OpenemsNamedException e) {
			this.log.error("Enum write channel (SET_SYSTEM_ERROR_RESET) was not able to set");
		}

	}

	/**
	 * This method gets the state of the system.
	 * 
	 * @return SystemState {@link SystemState} state of the system
	 */
	private SystemState getSystemState() {
		return this.channel(RefuBeckhoffEss.ChannelId.SYSTEM_STATE).value().asEnum();
	}
}
