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

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
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
import io.openems.edge.common.channel.Doc;
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
		configurationPolicy = ConfigurationPolicy.REQUIRE)
public class EssFeneconBydContainer extends AbstractOpenemsModbusComponent
		implements ManagedSymmetricEss, SymmetricEss, ModbusComponent, OpenemsComponent {

	private static final int MAX_APPARENT_POWER = 480_000;

	private static final int UNIT_ID = 100;
	private boolean readonly = false;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	private Power power;

	public EssFeneconBydContainer() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				ChannelId.values() //
		);
		this._setMaxApparentPower(EssFeneconBydContainer.MAX_APPARENT_POWER);
		this._setGridMode(GridMode.ON_GRID);
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected BridgeModbus modbus1;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected BridgeModbus modbus2;

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
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
		this.channel(ChannelId.READ_ONLY_MODE).setNextValue(config.readonly());
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

		IntegerWriteChannel setActivePowerChannel = this.channel(ChannelId.SET_ACTIVE_POWER);
		IntegerWriteChannel setReactivePowerChannel = this.channel(ChannelId.SET_REACTIVE_POWER);
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
		SystemWorkmode systemWorkmode = this.channel(ChannelId.SYSTEM_WORKMODE).value().asEnum(); //
		SystemWorkstate systemWorkstate = this.channel(ChannelId.SYSTEM_WORKSTATE).value().asEnum();//

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

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		READ_ONLY_MODE(Doc.of(Level.INFO)),
		// RTU registers
		SYSTEM_WORKSTATE(Doc.of(SystemWorkstate.values())), //
		SYSTEM_WORKMODE(Doc.of(SystemWorkmode.values())), //
		LIMIT_INDUCTIVE_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		LIMIT_CAPACITIVE_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		CONTAINER_RUN_NUMBER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)),
		SET_SYSTEM_WORKSTATE(Doc.of(SetSystemWorkstate.values())//
				.accessMode(AccessMode.WRITE_ONLY)),
		SET_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.KILOWATT)//
				.accessMode(AccessMode.WRITE_ONLY)), //
		SET_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)//
				.accessMode(AccessMode.WRITE_ONLY)), //
		// PCS registers
		PCS_SYSTEM_WORKSTATE(Doc.of(SystemWorkstate.values())), //
		PCS_SYSTEM_WORKMODE(Doc.of(SystemWorkmode.values())), //
		PHASE3_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.KILOWATT)), //
		PHASE3_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)), //
		PHASE3_INSPECTING_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.KILOVOLT_AMPERE)), //
		PCS_DISCHARGE_LIMIT_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.KILOWATT)), //
		PCS_CHARGE_LIMIT_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.KILOWATT)), //
		POSITIVE_REACTIVE_POWER_LIMIT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)), //
		NEGATIVE_REACTIVE_POWER_LIMIT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)), //
		CURRENT_L1(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.AMPERE)), //
		CURRENT_L2(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.AMPERE)), //
		CURRENT_L3(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.AMPERE)), //
		VOLTAGE_L1(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT)), //
		VOLTAGE_L2(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT)), //
		VOLTAGE_L3(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT)), //
		VOLTAGE_L12(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT)), //
		VOLTAGE_L23(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT)), //
		VOLTAGE_L31(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT)), //
		SYSTEM_FREQUENCY(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.HERTZ)),
		DC_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT)), //
		DC_CURRENT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.AMPERE)), //
		DC_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.KILOWATT)), //
		IGBT_TEMPERATURE_L1(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)),
		IGBT_TEMPERATURE_L2(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)),
		IGBT_TEMPERATURE_L3(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)),
		// PCS_WARNING_0
		STATE_0(Doc.of(Level.WARNING).text("DC pre-charging contactor checkback abnormal")),
		STATE_1(Doc.of(Level.WARNING).text("AC pre-charging contactor checkback abnormal")),
		STATE_2(Doc.of(Level.WARNING).text("AC main contactor checkback abnormal")),
		STATE_3(Doc.of(Level.WARNING).text("AC circuit breaker checkback abnormal")),
		STATE_4(Doc.of(Level.WARNING).text("Container door open")), //
		STATE_5(Doc.of(Level.WARNING).text("Reserved")),
		STATE_6(Doc.of(Level.WARNING).text("AC circuit breaker is not closed")),
		STATE_7(Doc.of(Level.WARNING).text("Reserved")),
		// PCS_WARNING_1
		STATE_8(Doc.of(Level.WARNING).text("General overload")), //
		STATE_9(Doc.of(Level.WARNING).text("Severe overload")),
		STATE_10(Doc.of(Level.WARNING).text("Over temperature drop power")),
		STATE_11(Doc.of(Level.WARNING).text("AC three-phase current imbalance alarm")),
		STATE_12(Doc.of(Level.WARNING).text("Failed to reset factory settings")),
		STATE_13(Doc.of(Level.WARNING).text("Hardware board invalidation")),
		STATE_14(Doc.of(Level.WARNING).text("Self-test failure alarm")),
		STATE_15(Doc.of(Level.WARNING).text("Receive BMS stop signal")),
		STATE_16(Doc.of(Level.WARNING).text("Air-conditioner")),
		STATE_17(Doc.of(Level.WARNING).text("IGBT Three phase temperature difference is large")),
		STATE_18(Doc.of(Level.WARNING).text("EEPROM Input data overrun")),
		STATE_19(Doc.of(Level.WARNING).text("Back up EEPROM data failure")),
		STATE_20(Doc.of(Level.WARNING).text("DC circuit breaker checkback abnormal")),
		STATE_21(Doc.of(Level.WARNING).text("DC main contactor checkback abnormal")),
		// PCS_WARNING_2
		STATE_22(Doc.of(Level.WARNING).text("Interruption of communication between PCS and Master")),
		STATE_23(Doc.of(Level.WARNING).text("Interruption of communication between PCS and unit controller")),
		STATE_24(Doc.of(Level.WARNING).text("Excessive temperature")),
		STATE_25(Doc.of(Level.WARNING).text("Excessive humidity")),
		STATE_26(Doc.of(Level.WARNING).text("Accept H31 control board signal shutdown")),
		STATE_27(Doc.of(Level.WARNING).text("Radiator A temperature sampling failure")),
		STATE_28(Doc.of(Level.WARNING).text("Radiator B temperature sampling failure")),
		STATE_29(Doc.of(Level.WARNING).text("Radiator C temperature sampling failure")),
		STATE_30(Doc.of(Level.WARNING).text("Reactor temperature sampling failure")),
		STATE_31(Doc.of(Level.WARNING).text("PCS cabinet environmental temperature sampling failure")),
		STATE_32(Doc.of(Level.WARNING).text("DC circuit breaker not engaged")),
		STATE_33(Doc.of(Level.WARNING).text("Controller of receive system shutdown because of abnormal command")),
		// PCS_WARNING_3
		STATE_34(Doc.of(Level.WARNING).text("Interruption of communication between PCS and RTU0 ")),
		STATE_35(Doc.of(Level.WARNING).text("Interruption of communication between PCS and RTU1AN")),
		STATE_36(Doc.of(Level.WARNING).text("Interruption of communication between PCS and MasterCAN")),
		STATE_37(Doc.of(Level.WARNING).text("Short-term access too many times to hot standby status in a short term")),
		STATE_38(Doc.of(Level.WARNING).text("entry and exit dynamic monitoring too many times in a short term")),
		STATE_39(Doc.of(Level.WARNING).text("AC preload contactor delay closure ")),
		// PCS_FAULTS_0
		STATE_40(Doc.of(Level.FAULT).text("DC pre-charge contactor cannot pull in")),
		STATE_41(Doc.of(Level.FAULT).text("AC pre-charge contactor cannot pull in")),
		STATE_42(Doc.of(Level.FAULT).text("AC main contactor cannot pull in")),
		STATE_43(Doc.of(Level.FAULT).text("AC breaker is abnormally disconnected during operation")),
		STATE_44(Doc.of(Level.FAULT).text("AC main contactor disconnected during operation")),
		STATE_45(Doc.of(Level.FAULT).text("AC main contactor cannot be disconnected")),
		STATE_46(Doc.of(Level.FAULT).text("Hardware PDP failure")),
		STATE_47(Doc.of(Level.FAULT).text("DC midpoint 1 high voltage protection")),
		STATE_48(Doc.of(Level.FAULT).text("DC midpoint 2 high voltage protection")),
		// PCS_FAULTS_1
		STATE_49(Doc.of(Level.FAULT).text("Radiator A over-temperature protection")),
		STATE_50(Doc.of(Level.FAULT).text("Radiator B over-temperature protection")),
		STATE_51(Doc.of(Level.FAULT).text("Radiator C over-temperature protection")),
		STATE_52(Doc.of(Level.FAULT).text("Electric reactor core over temperature protection")),
		STATE_53(Doc.of(Level.FAULT).text("DC breaker disconnected abnormally in operation")),
		STATE_54(Doc.of(Level.FAULT).text("DC main contactor disconnected abnormally in operation")),
		// PCS_FAULTS_2
		STATE_55(Doc.of(Level.FAULT).text("DC short-circuit protection")),
		STATE_56(Doc.of(Level.FAULT).text("DC overvoltage protection")),
		STATE_57(Doc.of(Level.FAULT).text("DC undervoltage protection")),
		STATE_58(Doc.of(Level.FAULT).text("DC reverse or missed connection protection")),
		STATE_59(Doc.of(Level.FAULT).text("DC disconnection protection")),
		STATE_60(Doc.of(Level.FAULT).text("DC overcurrent protection")),
		STATE_61(Doc.of(Level.FAULT).text("AC Phase A Peak Protection")),
		STATE_62(Doc.of(Level.FAULT).text("AC Phase B Peak Protection")),
		STATE_63(Doc.of(Level.FAULT).text("AC Phase C Peak Protection")),
		STATE_64(Doc.of(Level.FAULT).text("AC phase A effective value high protection")),
		STATE_65(Doc.of(Level.FAULT).text("AC phase B effective value high protection")),
		STATE_66(Doc.of(Level.FAULT).text("AC phase C effective value high protection")),
		STATE_67(Doc.of(Level.FAULT).text("A-phase voltage sampling Failure")),
		STATE_68(Doc.of(Level.FAULT).text("B-phase voltage sampling Failure")),
		STATE_69(Doc.of(Level.FAULT).text("C-phase voltage sampling Failure")),
		// PCS_FAULTS_3
		STATE_70(Doc.of(Level.FAULT).text("Inverted Phase A Voltage Sampling Failure")),
		STATE_71(Doc.of(Level.FAULT).text("Inverted Phase B Voltage Sampling Failure")),
		STATE_72(Doc.of(Level.FAULT).text("Inverted Phase C Voltage Sampling Failure")),
		STATE_73(Doc.of(Level.FAULT).text("AC current sampling failure")),
		STATE_74(Doc.of(Level.FAULT).text("DC current sampling failure")),
		STATE_75(Doc.of(Level.FAULT).text("Phase A over-temperature protection")),
		STATE_76(Doc.of(Level.FAULT).text("Phase B over-temperature protection")),
		STATE_77(Doc.of(Level.FAULT).text("Phase C over-temperature protection")),
		STATE_78(Doc.of(Level.FAULT).text("A phase temperature sampling failure")),
		STATE_79(Doc.of(Level.FAULT).text("B phase temperature sampling failure")),
		STATE_80(Doc.of(Level.FAULT).text("C phase temperature sampling failure")),
		STATE_81(Doc.of(Level.FAULT).text("AC Phase A not fully pre-charged under-protection")),
		STATE_82(Doc.of(Level.FAULT).text("AC Phase B not fully pre-charged under-protection")),
		STATE_83(Doc.of(Level.FAULT).text("AC Phase C not fully pre-charged under-protection")),
		STATE_84(Doc.of(Level.FAULT).text("Non-adaptable phase sequence error protection")),
		STATE_85(Doc.of(Level.FAULT).text("DSP protection")),
		// PCS_FAULTS_4
		STATE_86(Doc.of(Level.FAULT).text("A-phase grid voltage serious high protection")),
		STATE_87(Doc.of(Level.FAULT).text("A-phase grid voltage general high protection")),
		STATE_88(Doc.of(Level.FAULT).text("B-phase grid voltage serious high protection")),
		STATE_89(Doc.of(Level.FAULT).text("B-phase grid voltage general high protection")),
		STATE_90(Doc.of(Level.FAULT).text("C-phase grid voltage serious high protection")),
		STATE_91(Doc.of(Level.FAULT).text("C-phase grid voltage general high protection")),
		STATE_92(Doc.of(Level.FAULT).text("A-phase grid voltage serious low  protection")),
		STATE_93(Doc.of(Level.FAULT).text("A-phase grid voltage general low protection")),
		STATE_94(Doc.of(Level.FAULT).text("B-phase grid voltage serious low  protection")),
		STATE_95(Doc.of(Level.FAULT).text("B-phase grid voltage general low protection")),
		STATE_96(Doc.of(Level.FAULT).text("C-phase grid voltage serious low  protection")),
		STATE_97(Doc.of(Level.FAULT).text("C-phase grid voltage general low protection")),
		STATE_98(Doc.of(Level.FAULT).text("serious high frequency")),
		STATE_99(Doc.of(Level.FAULT).text("general high frequency")),
		STATE_100(Doc.of(Level.FAULT).text("serious low frequency")),
		STATE_101(Doc.of(Level.FAULT).text("general low frequency")),
		// PCS_FAULTS_5
		STATE_102(Doc.of(Level.FAULT).text("Grid A phase loss")),
		STATE_103(Doc.of(Level.FAULT).text("Grid B phase loss")),
		STATE_104(Doc.of(Level.FAULT).text("Grid C phase loss")),
		STATE_105(Doc.of(Level.FAULT).text("Island protection")),
		STATE_106(Doc.of(Level.FAULT).text("A-phase low voltage ride through")),
		STATE_107(Doc.of(Level.FAULT).text("B-phase low voltage ride through")),
		STATE_108(Doc.of(Level.FAULT).text("C-phase low voltage ride through")),
		STATE_109(Doc.of(Level.FAULT).text("A phase inverter voltage serious high protection")),
		STATE_110(Doc.of(Level.FAULT).text("A phase inverter voltage general high protection")),
		STATE_111(Doc.of(Level.FAULT).text("B phase inverter voltage serious high protection")),
		STATE_112(Doc.of(Level.FAULT).text("B phase inverter voltage general high protection")),
		STATE_113(Doc.of(Level.FAULT).text("C phase inverter voltage serious high protection")),
		STATE_114(Doc.of(Level.FAULT).text("C phase inverter voltage general high protection")),
		STATE_115(Doc.of(Level.FAULT).text("Inverter peak voltage high protection cause by AC disconnection")),
		// BECU registers
		BATTERY_STRING_WORK_STATE(Doc.of(BatteryStringWorkState.values())),
		BATTERY_STRING_TOTAL_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT)), //
		BATTERY_STRING_CURRENT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.AMPERE)),
		BATTERY_STRING_SOC(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)),
		BATTERY_STRING_AVERAGE_TEMPERATURE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)),
		BATTERY_NUMBER_MAX_STRING_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)),
		BATTERY_STRING_MAX_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT)),
		BATTERY_STRING_MAX_VOLTAGE_TEMPERATURE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)),
		BATTERY_NUMBER_MIN_STRING_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)),
		BATTERY_STRING_MIN_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT)),
		BATTERY_STRING_MIN_VOLTAGE_TEMPERATURE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)),
		BATTERY_NUMBER_MAX_STRING_TEMPERATURE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)),
		BATTERY_STRING_MAX_TEMPERATURE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)),
		BATTERY_STRING_MAX_TEMPERATURE_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT)),
		BATTERY_NUMBER_MIN_STRING_TEMPERATURE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)),
		BATTERY_STRING_MIN_TEMPERATURE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)),
		BATTERY_STRING_MIN_TEMPERATURE_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT)),
		BATTERY_STRING_CHARGE_CURRENT_LIMIT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.AMPERE)),
		BATTERY_STRING_DISCHARGE_CURRENT_LIMIT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.AMPERE)),
		// BATTERY_STRING_WARNING_0_0
		STATE_116(Doc.of(Level.WARNING).text("Charging overcurrent general alarm")), //
		STATE_117(Doc.of(Level.WARNING).text("discharging overcurrent general alarm")), //
		STATE_118(Doc.of(Level.WARNING).text("Charge current over-limit alarm")), //
		STATE_119(Doc.of(Level.WARNING).text("discharge current over-limit alarm")), //
		STATE_120(Doc.of(Level.WARNING).text("General high voltage alarm")), //
		STATE_121(Doc.of(Level.WARNING).text("General low voltage alarm")), //
		STATE_122(Doc.of(Level.WARNING).text("Abnormal voltage change alarm")), //
		STATE_123(Doc.of(Level.WARNING).text("General high temperature alarm")), //
		STATE_124(Doc.of(Level.WARNING).text("General low temperature alarm")), //
		STATE_125(Doc.of(Level.WARNING).text("Abnormal temperature change alarm")), //
		STATE_126(Doc.of(Level.WARNING).text("Severe high voltage alarm")), //
		STATE_127(Doc.of(Level.WARNING).text("Severe low voltage alarm")), //
		STATE_128(Doc.of(Level.WARNING).text("Severe low temperature alarm")), //
		STATE_129(Doc.of(Level.WARNING).text("Charge current severe over-limit alarm")), //
		STATE_130(Doc.of(Level.WARNING).text("Discharge current severe over-limit alarm")), //
		STATE_131(Doc.of(Level.WARNING).text("Total voltage over limit alarm")), //
		// BATTERY_STRING_WARNING_0_1
		STATE_132(Doc.of(Level.WARNING).text("Balanced sampling abnormal alarm")), //
		STATE_133(Doc.of(Level.WARNING).text("Balanced control abnormal alarm")), //
		STATE_134(Doc.of(Level.WARNING).text("Isolation switch is not closed")), //
		STATE_135(Doc.of(Level.WARNING).text("Pre-charge current abnormal")), //
		STATE_136(Doc.of(Level.WARNING).text("Disconnected contactor current is not safe")), //
		STATE_137(Doc.of(Level.WARNING).text("Value of the current limit reduce")), //
		STATE_138(Doc.of(Level.WARNING).text("Isolation Switch Checkback Abnormal")), //
		STATE_139(Doc.of(Level.WARNING).text("Over temperature drop power")), //
		STATE_140(Doc.of(Level.WARNING).text("Pulse charge approaching maximum load time")), //
		STATE_141(Doc.of(Level.WARNING).text("Pulse charge timeout alarm")), //
		STATE_142(Doc.of(Level.WARNING).text("Pulse discharge approaching maximum load time")), //
		STATE_143(Doc.of(Level.WARNING).text("Pulse discharge timeout alarm")), //
		STATE_144(Doc.of(Level.WARNING).text("Battery string undervoltage")), //
		STATE_145(Doc.of(Level.WARNING).text("High voltage offset")), //
		STATE_146(Doc.of(Level.WARNING).text("Low pressure offset")), //
		STATE_147(Doc.of(Level.WARNING).text("High temperature offset")), //
		// BATTERY_STRING_WARNING_1_0
		STATE_148(Doc.of(Level.FAULT).text("Start timeout")), //
		STATE_149(Doc.of(Level.FAULT).text("Total operating voltage sampling abnormal")), //
		STATE_150(Doc.of(Level.FAULT).text("BMU Sampling circuit abnormal")), //
		STATE_151(Doc.of(Level.FAULT).text("Stop total voltage sampling abnormal")), //
		STATE_152(Doc.of(Level.FAULT).text("voltage sampling line open")), //
		STATE_153(Doc.of(Level.FAULT).text("Temperature sample line open")), //
		STATE_154(Doc.of(Level.FAULT).text("Main-auxiliary internal CAN open")), //
		STATE_155(Doc.of(Level.FAULT).text("Interruption with system controller communication")), //
		// BATTERY_STRING_WARNING_1_1
		STATE_156(Doc.of(Level.FAULT).text("Severe high temperature failure")), //
		STATE_157(Doc.of(Level.FAULT).text("Smoke alarm")), //
		STATE_158(Doc.of(Level.FAULT).text("Fuse failure")), //
		STATE_159(Doc.of(Level.FAULT).text("General leakage")), //
		STATE_160(Doc.of(Level.FAULT).text("Severe leakage")), //
		STATE_161(Doc.of(Level.FAULT).text("Repair switch disconnected")), //
		STATE_162(Doc.of(Level.FAULT).text("Emergency stop pressed down")), //
		// ADAS register addresses
		CONTAINER_IMMERSION_STATE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)), //
		CONTAINER_FIRE_STATUS(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)), //
		CONTROL_CABINET_STATE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)), //
		CONTAINER_GROUNDING_FAULT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)),
		CONTAINER_DOOR_STATUS_0(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)), //
		CONTAINER_DOOR_STATUS_1(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)),
		CONTAINER_AIRCONDITION_POWER_SUPPLY_STATE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)),
		// ADAS_WARNING_0_0
		STATE_163(Doc.of(Level.WARNING).text("ups1 Power down")), //
		STATE_164(Doc.of(Level.WARNING).text("Immersion sensor abnormal")), //
		STATE_165(Doc.of(Level.WARNING).text("switch 2 (battery room door) abnormal")), //
		STATE_166(Doc.of(Level.WARNING).text("switch 1 (PCS room door) abnormal")), //
		STATE_167(Doc.of(Level.WARNING).text("Firefighting fault")), //
		STATE_168(Doc.of(Level.WARNING).text("Lightning arrester abnormal")), //
		STATE_169(Doc.of(Level.WARNING).text("Fire alarm")), //
		STATE_170(Doc.of(Level.WARNING).text("Fire detector works")), //
		STATE_171(Doc.of(Level.WARNING).text("pcs1 Ground fault alarm signal")), //
		STATE_172(Doc.of(Level.WARNING).text("Integrated fire extinguishing")), //
		STATE_173(Doc.of(Level.WARNING).text("Emergency stop signal")), //
		STATE_174(Doc.of(Level.WARNING).text("Air conditioning fault contactor signal")), //
		STATE_175(Doc.of(Level.WARNING).text("pcs1 Ground fault shutdown signal")), //
		// ADAS_WARNING_0_1
		STATE_176(Doc.of(Level.WARNING).text("PCS room ambient temperature sensor failure")), //
		STATE_177(Doc.of(Level.WARNING).text("Battery room ambient temperature sensor failure")), //
		STATE_178(Doc.of(Level.WARNING).text("container external ambient temperature sensor failure")), //
		STATE_179(Doc.of(Level.WARNING).text("The temperature sensor on the top of the control cabinet failed")), //
		STATE_180(Doc.of(Level.WARNING).text("PCS room ambient humidity sensor failure")), //
		STATE_181(Doc.of(Level.WARNING).text("Battery room ambient humidity sensor failure")), //
		STATE_182(Doc.of(Level.WARNING).text("container external humidity sensor failure")), //
		STATE_183(Doc.of(Level.WARNING).text("SD card failure")), //
		STATE_184(Doc.of(Level.WARNING).text("PCS room ambient humidity alarm")), //
		STATE_185(Doc.of(Level.WARNING).text("battery room ambient humidity alarm")), //
		STATE_186(Doc.of(Level.WARNING).text("container external humidity alarm")), //
		// ADAS_WARNING_0_2
		STATE_187(Doc.of(Level.WARNING).text("Master Firefighting fault")), //
		STATE_188(Doc.of(Level.WARNING).text("Harmonic protection")), //
		STATE_189(Doc.of(Level.WARNING).text("Battery emergency stop")), //
		// ADAS_WARNING_1_0
		STATE_190(Doc.of(Level.FAULT).text("Reserved")), //
		STATE_203(Doc.of(Level.FAULT).text("Reserved")), //
		// ADAS_WARNING_1_1
		STATE_191(Doc.of(Level.FAULT).text("Serious overheating of ambient temperature in PCS room")), //
		STATE_192(Doc.of(Level.FAULT).text("Serious overheating of ambient temperature in Battery room")), //
		STATE_193(Doc.of(Level.FAULT).text("Serious overheating of ambient temperature outside the container")), //
		STATE_194(Doc.of(Level.FAULT).text("Serious overheating on the top of the electric control cabinet ")), //
		STATE_195(Doc.of(Level.FAULT).text("Serious overheating of transformer")), //
		// ADAS_WARNING_1_2
		STATE_196(Doc.of(Level.FAULT).text("DCAC module 1 Communication disconnected")), //
		STATE_197(Doc.of(Level.FAULT).text("DCAC module 2 Communication disconnected")), //
		STATE_198(Doc.of(Level.FAULT).text("DCAC module 3 Communication disconnected")), //
		STATE_199(Doc.of(Level.FAULT).text("DCAC module 4 Communication disconnected")), //
		STATE_200(Doc.of(Level.FAULT).text("BECU1 Communication disconnected")), //
		STATE_201(Doc.of(Level.FAULT).text("BECU2 Communication disconnected")), //
		STATE_202(Doc.of(Level.FAULT).text("BECU3 Communication disconnected")); //

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
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
