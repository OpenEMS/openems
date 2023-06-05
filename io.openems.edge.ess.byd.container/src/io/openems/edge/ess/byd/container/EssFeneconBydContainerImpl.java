package io.openems.edge.ess.byd.container;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3;

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
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Ess.Fenecon.BydContainer", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class EssFeneconBydContainerImpl extends AbstractOpenemsModbusComponent
		implements EssFeneconBydContainer, ManagedSymmetricEss, SymmetricEss, ModbusComponent, OpenemsComponent {

	private static final int MAX_APPARENT_POWER = 480_000;
	private static final int UNIT_ID = 100;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private Power power;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private BridgeModbus modbus1;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private BridgeModbus modbus2;

	private boolean readonly = false;

	public EssFeneconBydContainerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				EssFeneconBydContainer.ChannelId.values() //
		);
		this._setMaxApparentPower(EssFeneconBydContainerImpl.MAX_APPARENT_POWER);
		this._setGridMode(GridMode.ON_GRID);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), UNIT_ID, this.cm, "Modbus",
				config.modbus_id0())) {
			return;
		}

		// Configure Modbus 1
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "modbus1", config.modbus_id1())) {
			return;
		}
		if (this.isEnabled() && this.modbus1 != null) {
			this.modbus1.addProtocol(this.id(), this.defineModbus1Protocol());
		}

		// Configure Modbus 2
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "modbus2", config.modbus_id2())) {
			return;
		}
		if (this.isEnabled() && this.modbus2 != null) {
			this.modbus2.addProtocol(this.id(), this.defineModbus2Protocol());
		}

		// Handle Read-Only mode
		this.readonly = config.readonly();
		this.channel(EssFeneconBydContainer.ChannelId.READ_ONLY_MODE).setNextValue(config.readonly());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		if (this.modbus1 != null) {
			this.modbus1.removeProtocol(this.id());
		}
		if (this.modbus2 != null) {
			this.modbus2.removeProtocol(this.id());
		}
		super.deactivate();
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {
		if (this.readonly) {
			return;
		}

		IntegerWriteChannel setActivePowerChannel = this.channel(EssFeneconBydContainer.ChannelId.SET_ACTIVE_POWER);
		IntegerWriteChannel setReactivePowerChannel = this.channel(EssFeneconBydContainer.ChannelId.SET_REACTIVE_POWER);
		setActivePowerChannel.setNextWriteValue(activePower / 1000);
		setReactivePowerChannel.setNextWriteValue(reactivePower / 1000);
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public boolean isManaged() {
		return !this.readonly;
	}

	@Override
	public int getPowerPrecision() {
		return 1000;
	}

	@Override
	public Constraint[] getStaticConstraints() throws OpenemsNamedException {
		// Handle Read-Only mode -> no charge/discharge
		if (this.readonly) {
			return new Constraint[] { //
					this.createPowerConstraint("Read-Only-Mode", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 0), //
					this.createPowerConstraint("Read-Only-Mode", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 0) //
			};
		}

		// System is not running -> no charge/discharge
		SystemWorkmode systemWorkmode = this.channel(EssFeneconBydContainer.ChannelId.SYSTEM_WORKMODE).value().asEnum(); //
		SystemWorkstate systemWorkstate = this.channel(EssFeneconBydContainer.ChannelId.SYSTEM_WORKSTATE).value()
				.asEnum();//

		if (systemWorkmode != SystemWorkmode.PQ_MODE) {
			return new Constraint[] { //
					this.createPowerConstraint("WorkMode invalid", //
							Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 0),
					this.createPowerConstraint("WorkMode invalid", //
							Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 0) };
		}

		switch (systemWorkstate) {
		case FAULT:
		case STOP:
			return new Constraint[] { //
					this.createPowerConstraint("WorkState invalid", //
							Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 0),
					this.createPowerConstraint("WorkState invalid", //
							Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 0) };
		case DEBUG:
		case RUNNING:
		case UNDEFINED:
		case INITIAL:
		case STANDBY:
		case GRID_MONITORING:
		case READY:
			break;
		}

		// TODO set the positive and negative power limit in Constraints
		// IntegerReadChannel posReactivePowerLimit =
		// this.channel(ChannelId.POSITIVE_REACTIVE_POWER_LIMIT);//
		// int positiveReactivePowerLimit = TypeUtils.getAsType(OpenemsType.INTEGER,
		// posReactivePowerLimit);
		// IntegerReadChannel negReactivePowerLimit =
		// this.channel(ChannelId.NEGATIVE_REACTIVE_POWER_LIMIT);//
		// int negativeReactivePowerLimit = TypeUtils.getAsType(OpenemsType.INTEGER,
		// posReactivePowerLimit);
		// return new Constraint[] { //
		// this.createPowerConstraint("Positive Reactive Power Limit", Phase.ALL,
		// Pwr.REACTIVE,//
		// Relationship.LESS_OR_EQUALS, positiveReactivePowerLimit), //
		// this.createPowerConstraint("Negative Reactive Power Limit", Phase.ALL,
		// Pwr.REACTIVE, //
		// Relationship.GREATER_OR_EQUALS, negativeReactivePowerLimit) //
		// };
		// TODO set reactive power limit from limitInductiveReactivePower +
		// limitCapacitiveReactivePower
		// IntegerReadChannel limitInductiveReactivePower =
		// this.channel(ChannelId.LIMIT_INDUCTIVE_REACTIVE_POWER);
		// IntegerReadChannel limitCapacitiveReactivePower =
		// this.channel(ChannelId.LIMIT_CAPACITIVE_REACTIVE_POWER);
		return Power.NO_CONSTRAINTS;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this, new FC3ReadRegistersTask(0x1001, Priority.LOW,
				// TODO check each channels id's for scaling factor
				m(EssFeneconBydContainer.ChannelId.PCS_SYSTEM_WORKSTATE, new UnsignedWordElement(0x1001)),
				m(EssFeneconBydContainer.ChannelId.PCS_SYSTEM_WORKMODE, new UnsignedWordElement(0x1002)),
				m(EssFeneconBydContainer.ChannelId.PHASE3_ACTIVE_POWER, new SignedWordElement(0x1003)),
				m(EssFeneconBydContainer.ChannelId.PHASE3_REACTIVE_POWER, new SignedWordElement(0x1004)),
				m(EssFeneconBydContainer.ChannelId.PHASE3_INSPECTING_POWER, new UnsignedWordElement(0x1005)),
				m(EssFeneconBydContainer.ChannelId.PCS_DISCHARGE_LIMIT_ACTIVE_POWER, new UnsignedWordElement(0x1006)),
				m(EssFeneconBydContainer.ChannelId.PCS_CHARGE_LIMIT_ACTIVE_POWER, new SignedWordElement(0x1007)),
				m(EssFeneconBydContainer.ChannelId.POSITIVE_REACTIVE_POWER_LIMIT, new UnsignedWordElement(0x1008)),
				m(EssFeneconBydContainer.ChannelId.NEGATIVE_REACTIVE_POWER_LIMIT, new SignedWordElement(0x1009)),
				m(EssFeneconBydContainer.ChannelId.CURRENT_L1, new SignedWordElement(0x100A), SCALE_FACTOR_2),
				m(EssFeneconBydContainer.ChannelId.CURRENT_L2, new SignedWordElement(0x100B), SCALE_FACTOR_2),
				m(EssFeneconBydContainer.ChannelId.CURRENT_L3, new SignedWordElement(0x100C), SCALE_FACTOR_2),
				m(EssFeneconBydContainer.ChannelId.VOLTAGE_L1, new UnsignedWordElement(0x100D), SCALE_FACTOR_2),
				m(EssFeneconBydContainer.ChannelId.VOLTAGE_L2, new UnsignedWordElement(0x100E), SCALE_FACTOR_2),
				m(EssFeneconBydContainer.ChannelId.VOLTAGE_L3, new UnsignedWordElement(0x100F), SCALE_FACTOR_2),
				m(EssFeneconBydContainer.ChannelId.VOLTAGE_L12, new UnsignedWordElement(0x1010), SCALE_FACTOR_2),
				m(EssFeneconBydContainer.ChannelId.VOLTAGE_L23, new UnsignedWordElement(0x1011), SCALE_FACTOR_2),
				m(EssFeneconBydContainer.ChannelId.VOLTAGE_L31, new UnsignedWordElement(0x1012), SCALE_FACTOR_2),
				m(EssFeneconBydContainer.ChannelId.SYSTEM_FREQUENCY, new UnsignedWordElement(0x1013)),
				m(EssFeneconBydContainer.ChannelId.DC_VOLTAGE, new SignedWordElement(0x1014)),
				m(EssFeneconBydContainer.ChannelId.DC_CURRENT, new SignedWordElement(0x1015)),
				m(EssFeneconBydContainer.ChannelId.DC_POWER, new SignedWordElement(0x1016)),
				m(EssFeneconBydContainer.ChannelId.IGBT_TEMPERATURE_L1, new SignedWordElement(0x1017)),
				m(EssFeneconBydContainer.ChannelId.IGBT_TEMPERATURE_L2, new SignedWordElement(0x1018)),
				m(EssFeneconBydContainer.ChannelId.IGBT_TEMPERATURE_L3, new SignedWordElement(0x1019)),
				new DummyRegisterElement(0x101A, 0x103F), //
				// PCS_WARNING_0
				m(new BitsWordElement(0x1040, this) //
						.bit(0, EssFeneconBydContainer.ChannelId.STATE_0) //
						.bit(3, EssFeneconBydContainer.ChannelId.STATE_1) //
						.bit(4, EssFeneconBydContainer.ChannelId.STATE_2) //
						.bit(5, EssFeneconBydContainer.ChannelId.STATE_3) //
						.bit(12, EssFeneconBydContainer.ChannelId.STATE_4) //
						.bit(13, EssFeneconBydContainer.ChannelId.STATE_5) //
						.bit(14, EssFeneconBydContainer.ChannelId.STATE_6) //
						.bit(15, EssFeneconBydContainer.ChannelId.STATE_7) //
				),
				// PCS_WARNING_1
				m(new BitsWordElement(0x1041, this) //
						.bit(0, EssFeneconBydContainer.ChannelId.STATE_8) //
						.bit(1, EssFeneconBydContainer.ChannelId.STATE_9) //
						.bit(3, EssFeneconBydContainer.ChannelId.STATE_10) //
						.bit(5, EssFeneconBydContainer.ChannelId.STATE_11) //
						.bit(6, EssFeneconBydContainer.ChannelId.STATE_12) //
						.bit(7, EssFeneconBydContainer.ChannelId.STATE_13) //
						.bit(8, EssFeneconBydContainer.ChannelId.STATE_14) //
						.bit(9, EssFeneconBydContainer.ChannelId.STATE_15) //
						.bit(10, EssFeneconBydContainer.ChannelId.STATE_16) //
						.bit(11, EssFeneconBydContainer.ChannelId.STATE_17) //
						.bit(12, EssFeneconBydContainer.ChannelId.STATE_18) //
						.bit(13, EssFeneconBydContainer.ChannelId.STATE_19) //
						.bit(14, EssFeneconBydContainer.ChannelId.STATE_20) //
						.bit(15, EssFeneconBydContainer.ChannelId.STATE_21) //
				),
				// PCS_WARNING_2
				m(new BitsWordElement(0x1042, this) //
						.bit(1, EssFeneconBydContainer.ChannelId.STATE_22) //
						.bit(2, EssFeneconBydContainer.ChannelId.STATE_23) //
						.bit(5, EssFeneconBydContainer.ChannelId.STATE_24) //
						.bit(6, EssFeneconBydContainer.ChannelId.STATE_25) //
						.bit(7, EssFeneconBydContainer.ChannelId.STATE_26) //
						.bit(8, EssFeneconBydContainer.ChannelId.STATE_27) //
						.bit(9, EssFeneconBydContainer.ChannelId.STATE_28) //
						.bit(10, EssFeneconBydContainer.ChannelId.STATE_29) //
						.bit(11, EssFeneconBydContainer.ChannelId.STATE_30) //
						.bit(12, EssFeneconBydContainer.ChannelId.STATE_31) //
						.bit(13, EssFeneconBydContainer.ChannelId.STATE_32) //
						.bit(14, EssFeneconBydContainer.ChannelId.STATE_33) //
				),
				// PCS_WARNING_3
				m(new BitsWordElement(0x1043, this) //
						.bit(1, EssFeneconBydContainer.ChannelId.STATE_34) //
						.bit(2, EssFeneconBydContainer.ChannelId.STATE_35) //
						.bit(3, EssFeneconBydContainer.ChannelId.STATE_36) //
						.bit(4, EssFeneconBydContainer.ChannelId.STATE_37) //
						.bit(5, EssFeneconBydContainer.ChannelId.STATE_38) //
						.bit(6, EssFeneconBydContainer.ChannelId.STATE_39) //
				), //
				new DummyRegisterElement(0x1044, 0X104F), //
				// PCS_FAULTS_0
				m(new BitsWordElement(0x1050, this) //
						.bit(0, EssFeneconBydContainer.ChannelId.STATE_40) //
						.bit(1, EssFeneconBydContainer.ChannelId.STATE_41) //
						.bit(2, EssFeneconBydContainer.ChannelId.STATE_42) //
						.bit(5, EssFeneconBydContainer.ChannelId.STATE_43) //
						.bit(6, EssFeneconBydContainer.ChannelId.STATE_44) //
						.bit(8, EssFeneconBydContainer.ChannelId.STATE_45) //
						.bit(11, EssFeneconBydContainer.ChannelId.STATE_46) //
						.bit(12, EssFeneconBydContainer.ChannelId.STATE_47) //
						.bit(13, EssFeneconBydContainer.ChannelId.STATE_48) //
				),
				// PCS_FAULTS_1
				m(new BitsWordElement(0x1051, this) //
						.bit(1, EssFeneconBydContainer.ChannelId.STATE_49) //
						.bit(2, EssFeneconBydContainer.ChannelId.STATE_50) //
						.bit(3, EssFeneconBydContainer.ChannelId.STATE_51) //
						.bit(4, EssFeneconBydContainer.ChannelId.STATE_52) //
						.bit(7, EssFeneconBydContainer.ChannelId.STATE_53) //
						.bit(8, EssFeneconBydContainer.ChannelId.STATE_54) //
				), m(new BitsWordElement(0x1052, this) //
						.bit(0, EssFeneconBydContainer.ChannelId.STATE_55) //
						.bit(1, EssFeneconBydContainer.ChannelId.STATE_56) //
						.bit(2, EssFeneconBydContainer.ChannelId.STATE_57) //
						.bit(3, EssFeneconBydContainer.ChannelId.STATE_58) //
						.bit(4, EssFeneconBydContainer.ChannelId.STATE_59) //
						.bit(6, EssFeneconBydContainer.ChannelId.STATE_60) //
						.bit(7, EssFeneconBydContainer.ChannelId.STATE_61) //
						.bit(8, EssFeneconBydContainer.ChannelId.STATE_62) //
						.bit(9, EssFeneconBydContainer.ChannelId.STATE_63) //
						.bit(10, EssFeneconBydContainer.ChannelId.STATE_64) //
						.bit(11, EssFeneconBydContainer.ChannelId.STATE_65) //
						.bit(12, EssFeneconBydContainer.ChannelId.STATE_66) //
						.bit(13, EssFeneconBydContainer.ChannelId.STATE_67) //
						.bit(14, EssFeneconBydContainer.ChannelId.STATE_68) //
						.bit(15, EssFeneconBydContainer.ChannelId.STATE_69) //
				),
				// PCS_FAULTS_3
				m(new BitsWordElement(0x1053, this) //
						.bit(0, EssFeneconBydContainer.ChannelId.STATE_70) //
						.bit(1, EssFeneconBydContainer.ChannelId.STATE_71) //
						.bit(2, EssFeneconBydContainer.ChannelId.STATE_72) //
						.bit(3, EssFeneconBydContainer.ChannelId.STATE_73) //
						.bit(4, EssFeneconBydContainer.ChannelId.STATE_74) //
						.bit(5, EssFeneconBydContainer.ChannelId.STATE_75) //
						.bit(6, EssFeneconBydContainer.ChannelId.STATE_76) //
						.bit(7, EssFeneconBydContainer.ChannelId.STATE_77) //
						.bit(8, EssFeneconBydContainer.ChannelId.STATE_78) //
						.bit(9, EssFeneconBydContainer.ChannelId.STATE_79) //
						.bit(10, EssFeneconBydContainer.ChannelId.STATE_80) //
						.bit(11, EssFeneconBydContainer.ChannelId.STATE_81) //
						.bit(12, EssFeneconBydContainer.ChannelId.STATE_82) //
						.bit(13, EssFeneconBydContainer.ChannelId.STATE_83) //
						.bit(14, EssFeneconBydContainer.ChannelId.STATE_84) //
						.bit(15, EssFeneconBydContainer.ChannelId.STATE_85) //
				),
				// PCS_FAULTS_4
				m(new BitsWordElement(0x1054, this) //
						.bit(0, EssFeneconBydContainer.ChannelId.STATE_86) //
						.bit(1, EssFeneconBydContainer.ChannelId.STATE_87) //
						.bit(2, EssFeneconBydContainer.ChannelId.STATE_88) //
						.bit(3, EssFeneconBydContainer.ChannelId.STATE_89) //
						.bit(4, EssFeneconBydContainer.ChannelId.STATE_90) //
						.bit(5, EssFeneconBydContainer.ChannelId.STATE_91) //
						.bit(6, EssFeneconBydContainer.ChannelId.STATE_92) //
						.bit(7, EssFeneconBydContainer.ChannelId.STATE_93) //
						.bit(8, EssFeneconBydContainer.ChannelId.STATE_94) //
						.bit(9, EssFeneconBydContainer.ChannelId.STATE_95) //
						.bit(10, EssFeneconBydContainer.ChannelId.STATE_96) //
						.bit(11, EssFeneconBydContainer.ChannelId.STATE_97) //
						.bit(12, EssFeneconBydContainer.ChannelId.STATE_98) //
						.bit(13, EssFeneconBydContainer.ChannelId.STATE_99) //
						.bit(14, EssFeneconBydContainer.ChannelId.STATE_100) //
						.bit(15, EssFeneconBydContainer.ChannelId.STATE_101) //
				),
				// PCS_FAULTS_5
				m(new BitsWordElement(0x1055, this) //
						.bit(0, EssFeneconBydContainer.ChannelId.STATE_102) //
						.bit(1, EssFeneconBydContainer.ChannelId.STATE_103) //
						.bit(2, EssFeneconBydContainer.ChannelId.STATE_104) //
						.bit(3, EssFeneconBydContainer.ChannelId.STATE_105) //
						.bit(4, EssFeneconBydContainer.ChannelId.STATE_106) //
						.bit(5, EssFeneconBydContainer.ChannelId.STATE_107) //
						.bit(6, EssFeneconBydContainer.ChannelId.STATE_108) //
						.bit(7, EssFeneconBydContainer.ChannelId.STATE_109) //
						.bit(8, EssFeneconBydContainer.ChannelId.STATE_110) //
						.bit(9, EssFeneconBydContainer.ChannelId.STATE_111) //
						.bit(10, EssFeneconBydContainer.ChannelId.STATE_112) //
						.bit(11, EssFeneconBydContainer.ChannelId.STATE_113) //
						.bit(12, EssFeneconBydContainer.ChannelId.STATE_114) //
						.bit(13, EssFeneconBydContainer.ChannelId.STATE_115) //
				)),
				// BECU registers
				new FC3ReadRegistersTask(0x6001, Priority.LOW,
						m(EssFeneconBydContainer.ChannelId.BATTERY_STRING_WORK_STATE, new UnsignedWordElement(0x6001)),
						m(EssFeneconBydContainer.ChannelId.BATTERY_STRING_TOTAL_VOLTAGE,
								new UnsignedWordElement(0x6002)),
						m(EssFeneconBydContainer.ChannelId.BATTERY_STRING_CURRENT, new SignedWordElement(0x6003)),
						m(EssFeneconBydContainer.ChannelId.BATTERY_STRING_SOC, new UnsignedWordElement(0x6004)),
						new DummyRegisterElement(0x6005),
						m(EssFeneconBydContainer.ChannelId.BATTERY_STRING_AVERAGE_TEMPERATURE,
								new SignedWordElement(0x6006)),
						m(EssFeneconBydContainer.ChannelId.BATTERY_NUMBER_MAX_STRING_VOLTAGE,
								new UnsignedWordElement(0x6007)),
						m(EssFeneconBydContainer.ChannelId.BATTERY_STRING_MAX_VOLTAGE, new UnsignedWordElement(0x6008)),
						m(EssFeneconBydContainer.ChannelId.BATTERY_STRING_MAX_VOLTAGE_TEMPERATURE,
								new SignedWordElement(0x6009)),
						m(EssFeneconBydContainer.ChannelId.BATTERY_NUMBER_MIN_STRING_VOLTAGE,
								new UnsignedWordElement(0x600A)),
						m(EssFeneconBydContainer.ChannelId.BATTERY_STRING_MIN_VOLTAGE, new UnsignedWordElement(0x600B)),
						m(EssFeneconBydContainer.ChannelId.BATTERY_STRING_MIN_VOLTAGE_TEMPERATURE,
								new SignedWordElement(0x600C)),
						m(EssFeneconBydContainer.ChannelId.BATTERY_NUMBER_MAX_STRING_TEMPERATURE,
								new UnsignedWordElement(0x600D)),
						m(EssFeneconBydContainer.ChannelId.BATTERY_STRING_MAX_TEMPERATURE,
								new SignedWordElement(0x600E)),
						m(EssFeneconBydContainer.ChannelId.BATTERY_STRING_MAX_TEMPERATURE_VOLTAGE,
								new UnsignedWordElement(0x600F)),
						m(EssFeneconBydContainer.ChannelId.BATTERY_NUMBER_MIN_STRING_TEMPERATURE,
								new UnsignedWordElement(0x6010)),
						m(EssFeneconBydContainer.ChannelId.BATTERY_STRING_MIN_TEMPERATURE,
								new SignedWordElement(0x6011)),
						m(EssFeneconBydContainer.ChannelId.BATTERY_STRING_MIN_TEMPERATURE_VOLTAGE,
								new UnsignedWordElement(0x6012)),
						m(EssFeneconBydContainer.ChannelId.BATTERY_STRING_CHARGE_CURRENT_LIMIT,
								new UnsignedWordElement(0x6013)),
						m(EssFeneconBydContainer.ChannelId.BATTERY_STRING_DISCHARGE_CURRENT_LIMIT,
								new UnsignedWordElement(0x6014)),
						m(SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY,
								new UnsignedDoublewordElement(0x6015).wordOrder(WordOrder.LSWMSW)),
						m(SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY,
								new UnsignedDoublewordElement(0x6017).wordOrder(WordOrder.LSWMSW)),
						new DummyRegisterElement(0X6019, 0X603F),
						// BATTERY_STRING_WARNING_0_0
						m(new BitsWordElement(0x6040, this) //
								.bit(0, EssFeneconBydContainer.ChannelId.STATE_116) //
								.bit(1, EssFeneconBydContainer.ChannelId.STATE_117) //
								.bit(2, EssFeneconBydContainer.ChannelId.STATE_118) //
								.bit(3, EssFeneconBydContainer.ChannelId.STATE_119) //
								.bit(4, EssFeneconBydContainer.ChannelId.STATE_120) //
								.bit(5, EssFeneconBydContainer.ChannelId.STATE_121) //
								.bit(6, EssFeneconBydContainer.ChannelId.STATE_122) //
								.bit(7, EssFeneconBydContainer.ChannelId.STATE_123) //
								.bit(8, EssFeneconBydContainer.ChannelId.STATE_124) //
								.bit(9, EssFeneconBydContainer.ChannelId.STATE_125) //
								.bit(10, EssFeneconBydContainer.ChannelId.STATE_126) //
								.bit(11, EssFeneconBydContainer.ChannelId.STATE_127) //
								.bit(12, EssFeneconBydContainer.ChannelId.STATE_128) //
								.bit(13, EssFeneconBydContainer.ChannelId.STATE_129) //
								.bit(14, EssFeneconBydContainer.ChannelId.STATE_130) //
								.bit(15, EssFeneconBydContainer.ChannelId.STATE_131) //
						),
						// BATTERY_STRING_WARNING_0_1
						m(new BitsWordElement(0x6041, this) //
								.bit(0, EssFeneconBydContainer.ChannelId.STATE_132) //
								.bit(1, EssFeneconBydContainer.ChannelId.STATE_133) //
								.bit(2, EssFeneconBydContainer.ChannelId.STATE_134) //
								.bit(3, EssFeneconBydContainer.ChannelId.STATE_135) //
								.bit(4, EssFeneconBydContainer.ChannelId.STATE_136) //
								.bit(5, EssFeneconBydContainer.ChannelId.STATE_137) //
								.bit(6, EssFeneconBydContainer.ChannelId.STATE_138) //
								.bit(7, EssFeneconBydContainer.ChannelId.STATE_139) //
								.bit(8, EssFeneconBydContainer.ChannelId.STATE_140) //
								.bit(9, EssFeneconBydContainer.ChannelId.STATE_141) //
								.bit(10, EssFeneconBydContainer.ChannelId.STATE_142) //
								.bit(11, EssFeneconBydContainer.ChannelId.STATE_143) //
								.bit(12, EssFeneconBydContainer.ChannelId.STATE_144) //
								.bit(13, EssFeneconBydContainer.ChannelId.STATE_145) //
								.bit(14, EssFeneconBydContainer.ChannelId.STATE_146) //
								.bit(15, EssFeneconBydContainer.ChannelId.STATE_147) //
						),
						// BATTERY_STRING_WARNING_1_0
						m(new BitsWordElement(0x6042, this) //
								.bit(0, EssFeneconBydContainer.ChannelId.STATE_148) //
								.bit(1, EssFeneconBydContainer.ChannelId.STATE_149) //
								.bit(2, EssFeneconBydContainer.ChannelId.STATE_150) //
								.bit(3, EssFeneconBydContainer.ChannelId.STATE_151) //
								.bit(4, EssFeneconBydContainer.ChannelId.STATE_152) //
								.bit(5, EssFeneconBydContainer.ChannelId.STATE_153) //
								.bit(6, EssFeneconBydContainer.ChannelId.STATE_154) //
								.bit(7, EssFeneconBydContainer.ChannelId.STATE_155) //
						),
						// BATTERY_STRING_WARNING_1_1
						m(new BitsWordElement(0x6043, this) //
								.bit(2, EssFeneconBydContainer.ChannelId.STATE_156) //
								.bit(7, EssFeneconBydContainer.ChannelId.STATE_157) //
								.bit(8, EssFeneconBydContainer.ChannelId.STATE_158) //
								.bit(10, EssFeneconBydContainer.ChannelId.STATE_159) //
								.bit(11, EssFeneconBydContainer.ChannelId.STATE_160) //
								.bit(13, EssFeneconBydContainer.ChannelId.STATE_161) //
								.bit(14, EssFeneconBydContainer.ChannelId.STATE_162) //
						)));
	}

	private ModbusProtocol defineModbus1Protocol() throws OpenemsException {
		return new ModbusProtocol(this,
				new FC3ReadRegistersTask(0x3410, Priority.LOW,
						m(EssFeneconBydContainer.ChannelId.CONTAINER_IMMERSION_STATE, new UnsignedWordElement(0x3410)),
						m(EssFeneconBydContainer.ChannelId.CONTAINER_FIRE_STATUS, new UnsignedWordElement(0x3411)),
						m(EssFeneconBydContainer.ChannelId.CONTROL_CABINET_STATE, new UnsignedWordElement(0x3412)),
						m(EssFeneconBydContainer.ChannelId.CONTAINER_GROUNDING_FAULT, new UnsignedWordElement(0x3413)),
						m(EssFeneconBydContainer.ChannelId.CONTAINER_DOOR_STATUS_0, new UnsignedWordElement(0x3414)),
						m(EssFeneconBydContainer.ChannelId.CONTAINER_DOOR_STATUS_1, new UnsignedWordElement(0x3415)),
						m(EssFeneconBydContainer.ChannelId.CONTAINER_AIRCONDITION_POWER_SUPPLY_STATE,
								new UnsignedWordElement(0x3416)),
						new DummyRegisterElement(0X3417, 0X343F),
						// ADAS_WARNING_0_0
						m(new BitsWordElement(0x3440, this) //
								.bit(0, EssFeneconBydContainer.ChannelId.STATE_163) //
								.bit(2, EssFeneconBydContainer.ChannelId.STATE_164) //
								.bit(4, EssFeneconBydContainer.ChannelId.STATE_165) //
								.bit(5, EssFeneconBydContainer.ChannelId.STATE_166) //
								.bit(6, EssFeneconBydContainer.ChannelId.STATE_167) //
								.bit(7, EssFeneconBydContainer.ChannelId.STATE_168) //
								.bit(8, EssFeneconBydContainer.ChannelId.STATE_169) //
								.bit(9, EssFeneconBydContainer.ChannelId.STATE_170) //
								.bit(10, EssFeneconBydContainer.ChannelId.STATE_171) //
								.bit(11, EssFeneconBydContainer.ChannelId.STATE_172) //
								.bit(12, EssFeneconBydContainer.ChannelId.STATE_173) //
								.bit(13, EssFeneconBydContainer.ChannelId.STATE_174) //
								.bit(14, EssFeneconBydContainer.ChannelId.STATE_175) //
						),
						// ADAS_WARNING_0_1
						m(new BitsWordElement(0x3441, this) //
								.bit(3, EssFeneconBydContainer.ChannelId.STATE_176) //
								.bit(4, EssFeneconBydContainer.ChannelId.STATE_177) //
								.bit(5, EssFeneconBydContainer.ChannelId.STATE_178) //
								.bit(6, EssFeneconBydContainer.ChannelId.STATE_179) //
								.bit(9, EssFeneconBydContainer.ChannelId.STATE_180) //
								.bit(10, EssFeneconBydContainer.ChannelId.STATE_181) //
								.bit(11, EssFeneconBydContainer.ChannelId.STATE_182) //
								.bit(12, EssFeneconBydContainer.ChannelId.STATE_183) //
								.bit(13, EssFeneconBydContainer.ChannelId.STATE_184) //
								.bit(14, EssFeneconBydContainer.ChannelId.STATE_185) //
								.bit(15, EssFeneconBydContainer.ChannelId.STATE_186) //
						),
						// ADAS_WARNING_0_2
						m(new BitsWordElement(0x3442, this) //
								.bit(0, EssFeneconBydContainer.ChannelId.STATE_187) //
								.bit(2, EssFeneconBydContainer.ChannelId.STATE_188) //
								.bit(3, EssFeneconBydContainer.ChannelId.STATE_189) //
						), //
						new DummyRegisterElement(0X3443, 0X344F),
						// ADAS_WARNING_1_0
						m(new BitsWordElement(0x3450, this) //
								.bit(0, EssFeneconBydContainer.ChannelId.STATE_190) //
						),
						// ADAS_WARNING_1_1
						m(new BitsWordElement(0x3451, this) //
								.bit(3, EssFeneconBydContainer.ChannelId.STATE_203) //
								.bit(4, EssFeneconBydContainer.ChannelId.STATE_191) //
								.bit(5, EssFeneconBydContainer.ChannelId.STATE_192) //
								.bit(6, EssFeneconBydContainer.ChannelId.STATE_193) //
								.bit(9, EssFeneconBydContainer.ChannelId.STATE_194) //
						),
						// ADAS_WARNING_1_2
						m(new BitsWordElement(0x3452, this) //
								.bit(0, EssFeneconBydContainer.ChannelId.STATE_195) //
								.bit(1, EssFeneconBydContainer.ChannelId.STATE_196) //
								.bit(2, EssFeneconBydContainer.ChannelId.STATE_197) //
								.bit(3, EssFeneconBydContainer.ChannelId.STATE_198) //
								.bit(8, EssFeneconBydContainer.ChannelId.STATE_199) //
								.bit(9, EssFeneconBydContainer.ChannelId.STATE_200) //
								.bit(10, EssFeneconBydContainer.ChannelId.STATE_201) //
								.bit(11, EssFeneconBydContainer.ChannelId.STATE_202) //
						)));
	}

	protected ModbusProtocol defineModbus2Protocol() throws OpenemsException {
		return new ModbusProtocol(this, new FC3ReadRegistersTask(0x38A0, Priority.LOW, //
				// RTU registers
				m(EssFeneconBydContainer.ChannelId.SYSTEM_WORKSTATE, new UnsignedWordElement(0x38A0)),
				m(EssFeneconBydContainer.ChannelId.SYSTEM_WORKMODE, new UnsignedWordElement(0x38A1)),
				m(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER, new SignedWordElement(0x38A2), SCALE_FACTOR_3),
				m(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER, new SignedWordElement(0x38A3), SCALE_FACTOR_3),
				m(EssFeneconBydContainer.ChannelId.LIMIT_INDUCTIVE_REACTIVE_POWER, new SignedWordElement(0x38A4)),
				m(EssFeneconBydContainer.ChannelId.LIMIT_CAPACITIVE_REACTIVE_POWER, new SignedWordElement(0x38A5)),
				m(SymmetricEss.ChannelId.ACTIVE_POWER, new SignedWordElement(0x38A6), SCALE_FACTOR_2),
				m(SymmetricEss.ChannelId.REACTIVE_POWER, new SignedWordElement(0x38A7), SCALE_FACTOR_2),
				m(SymmetricEss.ChannelId.SOC, new UnsignedWordElement(0x38A8)),
				m(EssFeneconBydContainer.ChannelId.CONTAINER_RUN_NUMBER, new UnsignedWordElement(0x38A9))),
				new FC16WriteRegistersTask(0x0500,
						m(EssFeneconBydContainer.ChannelId.SET_SYSTEM_WORKSTATE, new UnsignedWordElement(0x0500)),
						m(EssFeneconBydContainer.ChannelId.SET_ACTIVE_POWER, new SignedWordElement(0x0501)),
						m(EssFeneconBydContainer.ChannelId.SET_REACTIVE_POWER, new SignedWordElement(0x0502))));
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().asString() //
				+ "|L:" + this.getActivePower().asString() //
				+ "|Allowed:"
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER).value().asStringWithoutUnit() + ";"
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER).value().asString() //
				+ "|" + this.getGridModeChannel().value().asOptionString();
	}

}
