package io.openems.edge.ess.byd.container;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.doc.AccessMode;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Level;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.OpenemsComponent;
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
		implements ManagedSymmetricEss, SymmetricEss, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(EssFeneconBydContainer.class);

	private final static int UNIT_ID = 100;
	private String modbusBridgeId;
	private boolean readonly = false;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	private Power power;

	public EssFeneconBydContainer() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		// log.info("Received via Reference Modbus: " + modbus.id());
		super.setModbus(modbus);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected BridgeModbus modbus1;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected BridgeModbus modbus2;

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.enabled(), UNIT_ID, this.cm, "Modbus", config.modbus_id0());

		// Configure Modbus 1
		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "modbus1", config.modbus_id1())) {
			return;
		}

		// Configure Modbus 2
		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "modbus2", config.modbus_id2())) {
			return;
		}

		if (this.isEnabled() && this.modbus1 != null) {
			this.modbus1.addProtocol(this.id(), this.defineModbus1Protocol());
		}

		if (this.isEnabled() && this.modbus2 != null) {
			this.modbus2.addProtocol(this.getModbusBridgeId(), this.defineModbus2Protocol());
		}

		this.modbusBridgeId = config.modbus_id1();
		this.readonly = config.readonly();

		if (config.readonly()) {
			// Do not allow Power in read-only mode
			this.getMaxApparentPower().setNextValue(0);
		}
		this.channel(ChannelId.READ_ONLY_MODE).setNextValue(config.readonly());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void applyPower(int activePower, int reactivePower) {
		if (this.readonly) {
			return;
		}

		this.logInfo(this.log, "Would apply " + activePower + ", " + reactivePower);

		IntegerWriteChannel systemWorkmodeChannel = this.channel(ChannelId.SYSTEM_WORKMODE);
		SystemWorkmode systemWorkmode = systemWorkmodeChannel.value().asEnum();
		if (systemWorkmode != SystemWorkmode.PQ_MODE) {
			this.logWarn(this.log, "System Work-Mode is not P/Q");
			// TODO systemWorkmodeChannel.setNextWriteValue(SystemWorkmode.PQ_MODE);
			return;
		}

		IntegerReadChannel systemWorkstateChannel = this.channel(ChannelId.SYSTEM_WORKSTATE);
		SystemWorkstate systemWorkstate = systemWorkstateChannel.value().asEnum();
		if (systemWorkstate != SystemWorkstate.RUNNING) {
			this.logWarn(this.log, "System Work-State is not RUNNING");
			IntegerWriteChannel setSystemWorkstateChannel = this.channel(ChannelId.SET_SYSTEM_WORKSTATE);
			// TODO setSystemWorkstateChannel.setNextWriteValue(SetSystemWorkstate.RUN);
			return;
		}

		// TODO
		// IntegerWriteChannel setActivePowerChannel =
		// this.channel(ChannelId.SET_ACTIVE_POWER);
		// IntegerWriteChannel setReactivePowerChannel =
		// this.channel(ChannelId.SET_REACTIVE_POWER);
		// try {
		// setActivePowerChannel.setNextWriteValue(activePower);
		// } catch (OpenemsException e) {
		// log.error("Unable to set ActivePower: " + e.getMessage());
		// }
		// try {
		// setReactivePowerChannel.setNextWriteValue(reactivePower);
		// } catch (OpenemsException e) {
		// log.error("Unable to set ReactivePower: " + e.getMessage());
		// }
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public int getPowerPrecision() {
		return 1000;
	}

	@Override
	public Constraint[] getStaticConstraints() {
		IntegerReadChannel systemWorkmodeChannel = this.channel(ChannelId.SYSTEM_WORKMODE);
		IntegerReadChannel systemWorkstateChannel = this.channel(ChannelId.SYSTEM_WORKSTATE);
		SystemWorkmode systemWorkmode = systemWorkmodeChannel.value().asEnum();
		SystemWorkstate systemWorkstate = systemWorkstateChannel.value().asEnum();
		if (systemWorkmode != SystemWorkmode.PQ_MODE || systemWorkstate != SystemWorkstate.RUNNING) {
			return new Constraint[] { //
					this.createPowerConstraint("WorkMode+State invalid", //
							Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 0),
					this.createPowerConstraint("WorkMode+State invalid", //
							Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 0) };
		}

		IntegerReadChannel limitInductiveReactivePower = this.channel(ChannelId.LIMIT_INDUCTIVE_REACTIVE_POWER);
		IntegerReadChannel limitCapacitiveReactivePower = this.channel(ChannelId.LIMIT_CAPACITIVE_REACTIVE_POWER);

		// TODO set reactive power limit from limitInductiveReactivePower +
		// limitCapacitiveReactivePower
		return Power.NO_CONSTRAINTS;
	}

	public String getModbusBridgeId() {
		return modbusBridgeId;
	}

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		READ_ONLY_MODE(new Doc().level(Level.INFO)),
		// RTU registers
		SYSTEM_WORKSTATE(new Doc().options(SystemWorkstate.values())),
		SYSTEM_WORKMODE(new Doc().accessMode(AccessMode.READ_WRITE).options(SystemWorkmode.values())),
		LIMIT_INDUCTIVE_REACTIVE_POWER(new Doc().unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		LIMIT_CAPACITIVE_REACTIVE_POWER(new Doc().unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		CONTAINER_RUN_NUMBER(new Doc().unit(Unit.NONE)),
		SET_SYSTEM_WORKSTATE(new Doc().options(SetSystemWorkstate.values())),
		SET_ACTIVE_POWER(new Doc().unit(Unit.KILOWATT)),
		SET_REACTIVE_POWER(new Doc().unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		// PCS registers
		PCS_SYSTEM_WORKSTATE(new Doc().options(SystemWorkstate.values())),
		PCS_SYSTEM_WORKMODE(new Doc().options(SystemWorkmode.values())),
		PHASE3_ACTIVE_POWER(new Doc().unit(Unit.KILOWATT)),
		PHASE3_REACTIVE_POWER(new Doc().unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		PHASE3_INSPECTING_POWER(new Doc().unit(Unit.KILOVOLT_AMPERE)),
		PCS_DISCHARGE_LIMIT_ACTIVE_POWER(new Doc().unit(Unit.KILOWATT)),
		PCS_CHARGE_LIMIT_ACTIVE_POWER(new Doc().unit(Unit.KILOWATT)),
		POSITIVE_REACTIVE_POWER_LIMIT(new Doc().unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		NEGATIVE_REACTIVE_POWER_LIMIT(new Doc().unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		CURRENT_L1(new Doc().unit(Unit.AMPERE)), CURRENT_L2(new Doc().unit(Unit.AMPERE)),
		CURRENT_L3(new Doc().unit(Unit.AMPERE)), VOLTAGE_L1(new Doc().unit(Unit.VOLT)),
		VOLTAGE_L2(new Doc().unit(Unit.VOLT)), VOLTAGE_L3(new Doc().unit(Unit.VOLT)),
		VOLTAGE_L12(new Doc().unit(Unit.VOLT)), VOLTAGE_L23(new Doc().unit(Unit.VOLT)),
		VOLTAGE_L31(new Doc().unit(Unit.VOLT)), SYSTEM_FREQUENCY(new Doc().unit(Unit.HERTZ)),
		DC_VOLTAGE(new Doc().unit(Unit.VOLT)), DC_CURRENT(new Doc().unit(Unit.AMPERE)),
		DC_POWER(new Doc().unit(Unit.KILOWATT)), IGBT_TEMPERATURE_L1(new Doc().unit(Unit.DEGREE_CELSIUS)),
		IGBT_TEMPERATURE_L2(new Doc().unit(Unit.DEGREE_CELSIUS)),
		IGBT_TEMPERATURE_L3(new Doc().unit(Unit.DEGREE_CELSIUS)),

		STATE_0(new Doc().level(Level.WARNING).text("DC pre-charging contactor checkback abnormal")),
		STATE_1(new Doc().level(Level.WARNING).text("AC pre-charging contactor checkback abnormal")),
		STATE_2(new Doc().level(Level.WARNING).text("AC main contactor checkback abnormal")),
		STATE_3(new Doc().level(Level.WARNING).text("AC circuit breaker checkback abnormal")),
		STATE_4(new Doc().level(Level.WARNING).text("Container door open")),
		STATE_5(new Doc().level(Level.WARNING).text("Reserved")),
		STATE_6(new Doc().level(Level.WARNING).text("AC circuit breaker is not closed")),
		STATE_7(new Doc().level(Level.WARNING).text("Reserved")),

		STATE_8(new Doc().level(Level.WARNING).text("General overload")),
		STATE_9(new Doc().level(Level.WARNING).text("Severe overload")),
		STATE_10(new Doc().level(Level.WARNING).text("Over temperature drop power")),
		STATE_11(new Doc().level(Level.WARNING).text("AC three-phase current imbalance alarm")),
		STATE_12(new Doc().level(Level.WARNING).text("Failed to reset factory settings")),
		STATE_13(new Doc().level(Level.WARNING).text("Hardware board invalidation")),
		STATE_14(new Doc().level(Level.WARNING).text("Self-test failure alarm")),
		STATE_15(new Doc().level(Level.WARNING).text("Receive BMS stop signal")),
		STATE_16(new Doc().level(Level.WARNING).text("Air-conditioner")),
		STATE_17(new Doc().level(Level.WARNING).text("IGBT Three phase temperature difference is large")),
		STATE_18(new Doc().level(Level.WARNING).text("EEPROM Input data overrun")),
		STATE_19(new Doc().level(Level.WARNING).text("Back up EEPROM data failure")),
		STATE_20(new Doc().level(Level.WARNING).text("DC circuit breaker checkback abnormal")),
		STATE_21(new Doc().level(Level.WARNING).text("DC main contactor checkback abnormal")),

		STATE_22(new Doc().level(Level.WARNING).text("Interruption of communication between PCS and Master")),
		STATE_23(new Doc().level(Level.WARNING).text("Interruption of communication between PCS and unit controller")),
		STATE_24(new Doc().level(Level.WARNING).text("Excessive temperature")),
		STATE_25(new Doc().level(Level.WARNING).text("Excessive humidity")),
		STATE_26(new Doc().level(Level.WARNING).text("Accept H31 control board signal shutdown")),
		STATE_27(new Doc().level(Level.WARNING).text("Radiator A temperature sampling failure")),
		STATE_28(new Doc().level(Level.WARNING).text("Radiator B temperature sampling failure")),
		STATE_29(new Doc().level(Level.WARNING).text("Radiator C temperature sampling failure")),
		STATE_30(new Doc().level(Level.WARNING).text("Reactor temperature sampling failure")),
		STATE_31(new Doc().level(Level.WARNING).text("PCS cabinet environmental temperature sampling failure")),
		STATE_32(new Doc().level(Level.WARNING).text("DC circuit breaker not engaged")),
		STATE_33(new Doc().level(Level.WARNING)
				.text("Controller of receive system shutdown because of abnormal command")),

		STATE_34(new Doc().level(Level.WARNING).text("Interruption of communication between PCS and RTU0 ")),
		STATE_35(new Doc().level(Level.WARNING).text("Interruption of communication between PCS and RTU1AN")),
		STATE_36(new Doc().level(Level.WARNING).text("Interruption of communication between PCS and MasterCAN")),
		STATE_37(new Doc().level(Level.WARNING).text("Short-term access too many times to hot standby status in a short term")),
		STATE_38(new Doc().level(Level.WARNING).text("entry and exit dynamic monitoring too many times in a short term")),
		STATE_39(new Doc().level(Level.WARNING).text("AC preload contactor delay closure ")),

		/*
		 * 
		 * 
		 * 
		 *  
		 */
		//PCS_WARNING_0(new Doc().unit(Unit.NONE)), 
		STATE_40(new Doc().level(Level.WARNING).text("DC pre-charge contactor cannot pull in")),
		STATE_41(new Doc().level(Level.WARNING).text("AC pre-charge contactor cannot pull in")),
		STATE_42(new Doc().level(Level.WARNING).text("AC main contactor cannot pull in")),
		STATE_43(new Doc().level(Level.WARNING).text("AC breaker is abnormally disconnected during operation")),
		STATE_44(new Doc().level(Level.WARNING).text("AC main contactor disconnected during operation")),
		STATE_45(new Doc().level(Level.WARNING).text("AC main contactor cannot be disconnected")),
		STATE_46(new Doc().level(Level.WARNING).text("Hardware PDP failure")),
		STATE_47(new Doc().level(Level.WARNING).text("DC midpoint 1 high voltage protection")),
		STATE_48(new Doc().level(Level.WARNING).text("DC midpoint 2 high voltage protection")),
		//PCS_WARNING_1(new Doc().unit(Unit.NONE)),
		STATE_49(new Doc().level(Level.WARNING).text("Radiator A over-temperature protection")),
		STATE_50(new Doc().level(Level.WARNING).text("Radiator B over-temperature protection")),
		STATE_51(new Doc().level(Level.WARNING).text("Radiator C over-temperature protection")),
		STATE_52(new Doc().level(Level.WARNING).text("Electric reactor core over temperature protection")),
		STATE_53(new Doc().level(Level.WARNING).text("DC breaker disconnected abnormally in operation")),
		STATE_54(new Doc().level(Level.WARNING).text("DC main contactor disconnected abnormally in operation")),
		//PCS_WARNING_2(new Doc().unit(Unit.NONE)),
		STATE_55(new Doc().level(Level.WARNING).text("DC short-circuit protection")),
		STATE_56(new Doc().level(Level.WARNING).text("DC overvoltage protection")),
		STATE_57(new Doc().level(Level.WARNING).text("DC undervoltage protection")),
		STATE_58(new Doc().level(Level.WARNING).text("DC reverse or missed connection protection")),
		STATE_59(new Doc().level(Level.WARNING).text("DC disconnection protection")),
		STATE_60(new Doc().level(Level.WARNING).text("DC overcurrent protection")),
		STATE_61(new Doc().level(Level.WARNING).text("AC Phase A Peak Protection")),
		STATE_62(new Doc().level(Level.WARNING).text("AC Phase B Peak Protection")),
		STATE_63(new Doc().level(Level.WARNING).text("AC Phase C Peak Protection")),
		STATE_64(new Doc().level(Level.WARNING).text("AC phase A effective value high protection")),
		STATE_65(new Doc().level(Level.WARNING).text("AC phase B effective value high protection")),
		STATE_66(new Doc().level(Level.WARNING).text("AC phase C effective value high protection")),
		STATE_67(new Doc().level(Level.WARNING).text("A-phase voltage sampling Failure")),
		STATE_68(new Doc().level(Level.WARNING).text("B-phase voltage sampling Failure")),
		STATE_69(new Doc().level(Level.WARNING).text("C-phase voltage sampling Failure")),
		//PCS_WARNING_3(new Doc().unit(Unit.NONE)),
		STATE_70(new Doc().level(Level.WARNING).text("Inverted Phase A Voltage Sampling Failure")),
		STATE_71(new Doc().level(Level.WARNING).text("Inverted Phase B Voltage Sampling Failure")),
		STATE_72(new Doc().level(Level.WARNING).text("Inverted Phase C Voltage Sampling Failure")),
		STATE_73(new Doc().level(Level.WARNING).text("AC current sampling failure")),
		STATE_74(new Doc().level(Level.WARNING).text("DC current sampling failure")),
		STATE_75(new Doc().level(Level.WARNING).text("Phase A over-temperature protection")),
		STATE_76(new Doc().level(Level.WARNING).text("Phase B over-temperature protection")),
		STATE_77(new Doc().level(Level.WARNING).text("Phase C over-temperature protection")),
		STATE_78(new Doc().level(Level.WARNING).text("A phase temperature sampling failure")),
		STATE_79(new Doc().level(Level.WARNING).text("B phase temperature sampling failure")),
		STATE_80(new Doc().level(Level.WARNING).text("C phase temperature sampling failure")),
		STATE_81(new Doc().level(Level.WARNING).text("AC Phase A not fully pre-charged under-protection")),
		STATE_82(new Doc().level(Level.WARNING).text("AC Phase B not fully pre-charged under-protection")),
		STATE_83(new Doc().level(Level.WARNING).text("AC Phase C not fully pre-charged under-protection")),
		STATE_84(new Doc().level(Level.WARNING).text("Non-adaptable phase sequence error protection")),
		STATE_85(new Doc().level(Level.WARNING).text("DSP protection")),

		STATE_86(new Doc().level(Level.WARNING).text("A-phase grid voltage serious high protection")),
		STATE_87(new Doc().level(Level.WARNING).text("A-phase grid voltage general high protection")),
		STATE_88(new Doc().level(Level.WARNING).text("B-phase grid voltage serious high protection")),
		STATE_89(new Doc().level(Level.WARNING).text("B-phase grid voltage general high protection")),
		STATE_90(new Doc().level(Level.WARNING).text("C-phase grid voltage serious high protection")),
		STATE_91(new Doc().level(Level.WARNING).text("C-phase grid voltage general high protection")),
		STATE_92(new Doc().level(Level.WARNING).text("A-phase grid voltage serious low  protection")),
		STATE_93(new Doc().level(Level.WARNING).text("A-phase grid voltage general low protection")),
		STATE_94(new Doc().level(Level.WARNING).text("B-phase grid voltage serious low  protection")),
		STATE_95(new Doc().level(Level.WARNING).text("B-phase grid voltage general low protection")),
		STATE_96(new Doc().level(Level.WARNING).text("C-phase grid voltage serious low  protection")),
		STATE_97(new Doc().level(Level.WARNING).text("C-phase grid voltage general low protection")),
		STATE_98(new Doc().level(Level.WARNING).text("serious high frequency")),
		STATE_99(new Doc().level(Level.WARNING).text("general high frequency")),
		STATE_100(new Doc().level(Level.WARNING).text("serious low frequency")),
		STATE_101(new Doc().level(Level.WARNING).text("general low frequency")),

		STATE_102(new Doc().level(Level.WARNING).text("Grid A phase loss")),
		STATE_103(new Doc().level(Level.WARNING).text("Grid B phase loss")),
		STATE_104(new Doc().level(Level.WARNING).text("Grid C phase loss")),
		STATE_105(new Doc().level(Level.WARNING).text("Island protection")),
		STATE_106(new Doc().level(Level.WARNING).text("A-phase low voltage ride through")),
		STATE_107(new Doc().level(Level.WARNING).text("B-phase low voltage ride through")),
		STATE_108(new Doc().level(Level.WARNING).text("C-phase low voltage ride through")),
		STATE_109(new Doc().level(Level.WARNING).text("A phase inverter voltage serious high protection")),
		STATE_110(new Doc().level(Level.WARNING).text("A phase inverter voltage general high protection")),
		STATE_111(new Doc().level(Level.WARNING).text("B phase inverter voltage serious high protection")),
		STATE_112(new Doc().level(Level.WARNING).text("B phase inverter voltage general high protection")),
		STATE_113(new Doc().level(Level.WARNING).text("C phase inverter voltage serious high protection")),
		STATE_114(new Doc().level(Level.WARNING).text("C phase inverter voltage general high protection")),
		STATE_115(
				new Doc().level(Level.WARNING).text("Inverter peak voltage high protection cause by AC disconnection")),

		PCS_FAULTS_0(new Doc().unit(Unit.NONE)), //
		PCS_FAULTS_1(new Doc().unit(Unit.NONE)), //
		PCS_FAULTS_2(new Doc().unit(Unit.NONE)), //
		PCS_FAULTS_3(new Doc().unit(Unit.NONE)), //
		PCS_FAULTS_4(new Doc().unit(Unit.NONE)), //
		PCS_FAULTS_5(new Doc().unit(Unit.NONE)), //
		// BECU registers
		BATTERY_STRING_WORK_STATE(new Doc().options(BatteryStringWorkState.values())),
		BATTERY_STRING_TOTAL_VOLTAGE(new Doc().unit(Unit.VOLT)), BATTERY_STRING_CURRENT(new Doc().unit(Unit.AMPERE)),
		BATTERY_STRING_SOC(new Doc().unit(Unit.NONE)),
		BATTERY_STRING_AVERAGE_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELSIUS)),
		BATTERY_NUMBER_MAX_STRING_VOLTAGE(new Doc().unit(Unit.NONE)),
		BATTERY_STRING_MAX_VOLTAGE(new Doc().unit(Unit.VOLT)),
		BATTERY_STRING_MAX_VOLTAGE_TEMPARATURE(new Doc().unit(Unit.DEGREE_CELSIUS)),
		BATTERY_NUMBER_MIN_STRING_VOLTAGE(new Doc().unit(Unit.NONE)),
		BATTERY_STRING_MIN_VOLTAGE(new Doc().unit(Unit.VOLT)),
		BATTERY_STRING_MIN_VOLTAGE_TEMPARATURE(new Doc().unit(Unit.DEGREE_CELSIUS)),
		BATTERY_NUMBER_MAX_STRING_TEMPERATURE(new Doc().unit(Unit.NONE)),
		BATTERY_STRING_MAX_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELSIUS)),
		BATTERY_STRING_MAX_TEMPARATURE_VOLTAGE(new Doc().unit(Unit.VOLT)),
		BATTERY_NUMBER_MIN_STRING_TEMPERATURE(new Doc().unit(Unit.NONE)),
		BATTERY_STRING_MIN_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELSIUS)),
		BATTERY_STRING_MIN_TEMPARATURE_VOLTAGE(new Doc().unit(Unit.VOLT)),
		BATTERY_STRING_CHARGE_CURRENT_LIMIT(new Doc().unit(Unit.AMPERE)),
		BATTERY_STRING_DISCHARGE_CURRENT_LIMIT(new Doc().unit(Unit.AMPERE)),
		
		
		BATTERY_STRING_WARNING_0_0(new Doc().unit(Unit.NONE)), //
		BATTERY_STRING_WARNING_0_1(new Doc().unit(Unit.NONE)), //
		BATTERY_STRING_WARNING_1_0(new Doc().unit(Unit.NONE)), //
		BATTERY_STRING_WARNING_1_1(new Doc().unit(Unit.NONE)), //
		// ADAS register addresses
		CONTAINER_IMMERSION_STATE(new Doc().unit(Unit.NONE)), CONTAINER_FIRE_STATUS(new Doc().unit(Unit.NONE)),
		CONTROL_CABINET_STATE(new Doc().unit(Unit.NONE)), CONTAINER_GROUNDING_FAULT(new Doc().unit(Unit.NONE)),
		CONTAINER_DOOR_STATUS_0(new Doc().unit(Unit.NONE)), CONTAINER_DOOR_STATUS_1(new Doc().unit(Unit.NONE)),
		CONTAINER_AIRCONDITION_POWER_SUPPLY_STATE(new Doc().unit(Unit.NONE)),
		/*
		 * CONTAINER_IMMERSION_STATE_1(new
		 * Doc().level(Level.WARNING).text("Immersion happens")),
		 * CONTAINER_IMMERSION_STATE_0(new
		 * Doc().level(Level.WARNING).text("Immersion vanishes")),
		 * CONTAINER_FIRE_STATUS_1(new Doc().level(Level.WARNING).text("Fire alarm")),
		 * CONTAINER_FIRE_STATUS_0(new
		 * Doc().level(Level.WARNING).text("Fire alarm vanishes")),
		 * CONTROL_CABINET_STATE_1(new
		 * Doc().level(Level.WARNING).text("Sudden stop press down")),
		 * CONTROL_CABINET_STATE_0(new
		 * Doc().level(Level.WARNING).text("Sudden stop spin up")),
		 * CONTAINER_GROUNDING_FAULT_1(new
		 * Doc().level(Level.WARNING).text("Grounding fault happens")),
		 * CONTAINER_GROUNDING_FAULT_0(new
		 * Doc().level(Level.WARNING).text("Grounding fault vanishes")),
		 * CONTAINER_DOOR_STATUS_0_1(new Doc().level(Level.WARNING).text("Door opens")),
		 * CONTAINER_DOOR_STATUS_0_0(new
		 * Doc().level(Level.WARNING).text("Door closes")),
		 * CONTAINER_DOOR_STATUS_1_1(new Doc().level(Level.WARNING).text("Door opens")),
		 * CONTAINER_DOOR_STATUS_1_0(new
		 * Doc().level(Level.WARNING).text("Door closes")),
		 * CONTAINER_AIRCONDITION_POWER_SUPPLY_STATE_1( new
		 * Doc().level(Level.WARNING).text("Air conditioning power supply")),
		 * CONTAINER_AIRCONDITION_POWER_SUPPLY_STATE_0(new
		 * Doc().level(Level.WARNING).text("Air conditioning shuts down")),
		 */
		ADAS_WARNING_0_0(new Doc().unit(Unit.NONE)), ADAS_WARNING_0_1(new Doc().unit(Unit.NONE)),
		ADAS_WARNING_0_2(new Doc().unit(Unit.NONE)), ADAS_WARNING_1_0(new Doc().unit(Unit.NONE)),
		ADAS_WARNING_1_1(new Doc().unit(Unit.NONE)), ADAS_WARNING_1_2(new Doc().unit(Unit.NONE)),
		ADAS_WARNING_1_3(new Doc().unit(Unit.NONE)),;

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
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, new FC3ReadRegistersTask(0x1001, Priority.LOW,
				m(EssFeneconBydContainer.ChannelId.PCS_SYSTEM_WORKSTATE, new UnsignedWordElement(0x1001)),
				m(EssFeneconBydContainer.ChannelId.PCS_SYSTEM_WORKMODE, new UnsignedWordElement(0x1002)),
				m(EssFeneconBydContainer.ChannelId.PHASE3_ACTIVE_POWER, new SignedWordElement(0x1003)),
				m(EssFeneconBydContainer.ChannelId.PHASE3_REACTIVE_POWER, new SignedWordElement(0x1004)),
				m(EssFeneconBydContainer.ChannelId.PHASE3_INSPECTING_POWER, new UnsignedWordElement(0x1005)),
				m(EssFeneconBydContainer.ChannelId.PCS_DISCHARGE_LIMIT_ACTIVE_POWER, new UnsignedWordElement(0x1006)),
				m(EssFeneconBydContainer.ChannelId.PCS_CHARGE_LIMIT_ACTIVE_POWER, new SignedWordElement(0x1007)),
				m(EssFeneconBydContainer.ChannelId.POSITIVE_REACTIVE_POWER_LIMIT, new UnsignedWordElement(0x1008)),
				m(EssFeneconBydContainer.ChannelId.NEGATIVE_REACTIVE_POWER_LIMIT, new SignedWordElement(0x1009)),
				m(EssFeneconBydContainer.ChannelId.CURRENT_L1, new SignedWordElement(0x100A),
						ElementToChannelConverter.SCALE_FACTOR_2),
				m(EssFeneconBydContainer.ChannelId.CURRENT_L2, new SignedWordElement(0x100B)),
				m(EssFeneconBydContainer.ChannelId.CURRENT_L3, new SignedWordElement(0x100C)),
				m(EssFeneconBydContainer.ChannelId.VOLTAGE_L1, new UnsignedWordElement(0x100D)),
				m(EssFeneconBydContainer.ChannelId.VOLTAGE_L2, new UnsignedWordElement(0x100E)),
				m(EssFeneconBydContainer.ChannelId.VOLTAGE_L3, new UnsignedWordElement(0x100F)),
				m(EssFeneconBydContainer.ChannelId.VOLTAGE_L12, new UnsignedWordElement(0x1010)),
				m(EssFeneconBydContainer.ChannelId.VOLTAGE_L23, new UnsignedWordElement(0x1011)),
				m(EssFeneconBydContainer.ChannelId.VOLTAGE_L31, new UnsignedWordElement(0x1012)),
				m(EssFeneconBydContainer.ChannelId.SYSTEM_FREQUENCY, new UnsignedWordElement(0x1013)),
				m(EssFeneconBydContainer.ChannelId.DC_VOLTAGE, new SignedWordElement(0x1014)),
				m(EssFeneconBydContainer.ChannelId.DC_CURRENT, new SignedWordElement(0x1015)),
				m(EssFeneconBydContainer.ChannelId.DC_POWER, new SignedWordElement(0x1016)),
				m(EssFeneconBydContainer.ChannelId.IGBT_TEMPERATURE_L1, new SignedWordElement(0x1017)),
				m(EssFeneconBydContainer.ChannelId.IGBT_TEMPERATURE_L2, new SignedWordElement(0x1018)),
				m(EssFeneconBydContainer.ChannelId.IGBT_TEMPERATURE_L3, new SignedWordElement(0x1019)),

				// m(EssFeneconBydContainer.ChannelId.PCS_WARNING_0, new
				// UnsignedWordElement(0x1040)),

				bm(new UnsignedWordElement(0x1040)) //
						.m(EssFeneconBydContainer.ChannelId.STATE_0, 0) //
						.m(EssFeneconBydContainer.ChannelId.STATE_1, 3) //
						.m(EssFeneconBydContainer.ChannelId.STATE_2, 4) //
						.m(EssFeneconBydContainer.ChannelId.STATE_3, 5)//
						.m(EssFeneconBydContainer.ChannelId.STATE_4, 12)//
						.m(EssFeneconBydContainer.ChannelId.STATE_5, 13)//
						.m(EssFeneconBydContainer.ChannelId.STATE_6, 14)//
						.m(EssFeneconBydContainer.ChannelId.STATE_7, 15)//
						.build(),

				// (EssFeneconBydContainer.ChannelId.PCS_WARNING_1, new
				// UnsignedWordElement(0x1041)),
				bm(new UnsignedWordElement(0x1041))//
						.m(EssFeneconBydContainer.ChannelId.STATE_8, 0)//
						.m(EssFeneconBydContainer.ChannelId.STATE_9, 1)//
						.m(EssFeneconBydContainer.ChannelId.STATE_10, 3)//
						.m(EssFeneconBydContainer.ChannelId.STATE_11, 5)//
						.m(EssFeneconBydContainer.ChannelId.STATE_12, 6)//
						.m(EssFeneconBydContainer.ChannelId.STATE_13, 7)//
						.m(EssFeneconBydContainer.ChannelId.STATE_14, 8)//
						.m(EssFeneconBydContainer.ChannelId.STATE_15, 9)//
						.m(EssFeneconBydContainer.ChannelId.STATE_16, 10)//
						.m(EssFeneconBydContainer.ChannelId.STATE_17, 11)//
						.m(EssFeneconBydContainer.ChannelId.STATE_18, 12)//
						.m(EssFeneconBydContainer.ChannelId.STATE_19, 13)//
						.m(EssFeneconBydContainer.ChannelId.STATE_20, 14)//
						.m(EssFeneconBydContainer.ChannelId.STATE_21, 15)//
						.build(),

				// m(EssFeneconBydContainer.ChannelId.PCS_WARNING_2, new
				// UnsignedWordElement(0x1042)),

				bm(new UnsignedWordElement(0x1041)) //
						.m(EssFeneconBydContainer.ChannelId.STATE_22, 1)//
						.m(EssFeneconBydContainer.ChannelId.STATE_23, 2)//
						.m(EssFeneconBydContainer.ChannelId.STATE_24, 5)//
						.m(EssFeneconBydContainer.ChannelId.STATE_25, 6)//
						.m(EssFeneconBydContainer.ChannelId.STATE_26, 7)//
						.m(EssFeneconBydContainer.ChannelId.STATE_27, 8)//
						.m(EssFeneconBydContainer.ChannelId.STATE_28, 9)//
						.m(EssFeneconBydContainer.ChannelId.STATE_29, 10)//
						.m(EssFeneconBydContainer.ChannelId.STATE_30, 11)//
						.m(EssFeneconBydContainer.ChannelId.STATE_31, 12)//
						.m(EssFeneconBydContainer.ChannelId.STATE_32, 13)//
						.m(EssFeneconBydContainer.ChannelId.STATE_33, 14)//
						.build(),

				// m(EssFeneconBydContainer.ChannelId.PCS_WARNING_3, new
				// UnsignedWordElement(0x1043)),

				bm(new UnsignedWordElement(0x1043)) //
						.m(EssFeneconBydContainer.ChannelId.STATE_34, 1)//
						.m(EssFeneconBydContainer.ChannelId.STATE_35, 2)//
						.m(EssFeneconBydContainer.ChannelId.STATE_36, 3)//
						.m(EssFeneconBydContainer.ChannelId.STATE_37, 4)//
						.m(EssFeneconBydContainer.ChannelId.STATE_38, 5)//
						.m(EssFeneconBydContainer.ChannelId.STATE_39, 6)//
						.build(),

				new DummyRegisterElement(0x1044, 0X104F), //
				// m(EssFeneconBydContainer.ChannelId.PCS_FAULTS_0, new
				// UnsignedWordElement(0x1050)),
				bm(new UnsignedWordElement(0x1050)) //
						.m(EssFeneconBydContainer.ChannelId.STATE_40, 0) //
						.m(EssFeneconBydContainer.ChannelId.STATE_41, 1) //
						.m(EssFeneconBydContainer.ChannelId.STATE_42, 2) //
						.m(EssFeneconBydContainer.ChannelId.STATE_43, 5) //
						.m(EssFeneconBydContainer.ChannelId.STATE_44, 6) //
						.m(EssFeneconBydContainer.ChannelId.STATE_45, 8) //
						.m(EssFeneconBydContainer.ChannelId.STATE_46, 11) //
						.m(EssFeneconBydContainer.ChannelId.STATE_47, 12) //
						.m(EssFeneconBydContainer.ChannelId.STATE_48, 13) //
						.build(),
				// m(EssFeneconBydContainer.ChannelId.PCS_FAULTS_1, new
				// UnsignedWordElement(0x1051)),
				bm(new UnsignedWordElement(0x1051)) //
						.m(EssFeneconBydContainer.ChannelId.STATE_49, 1) //
						.m(EssFeneconBydContainer.ChannelId.STATE_50, 2) //
						.m(EssFeneconBydContainer.ChannelId.STATE_51, 3) //
						.m(EssFeneconBydContainer.ChannelId.STATE_52, 4) //
						.m(EssFeneconBydContainer.ChannelId.STATE_53, 7) //
						.m(EssFeneconBydContainer.ChannelId.STATE_54, 8) //
						.build(),

				bm(new UnsignedWordElement(0x1052)) //
						.m(EssFeneconBydContainer.ChannelId.STATE_55, 0) //
						.m(EssFeneconBydContainer.ChannelId.STATE_56, 1) //
						.m(EssFeneconBydContainer.ChannelId.STATE_57, 2) //
						.m(EssFeneconBydContainer.ChannelId.STATE_58, 3) //
						.m(EssFeneconBydContainer.ChannelId.STATE_59, 4) //
						.m(EssFeneconBydContainer.ChannelId.STATE_60, 6) //
						.m(EssFeneconBydContainer.ChannelId.STATE_61, 7) //
						.m(EssFeneconBydContainer.ChannelId.STATE_62, 8) //
						.m(EssFeneconBydContainer.ChannelId.STATE_63, 9) //
						.m(EssFeneconBydContainer.ChannelId.STATE_64, 10) //
						.m(EssFeneconBydContainer.ChannelId.STATE_65, 11) //
						.m(EssFeneconBydContainer.ChannelId.STATE_66, 12) //
						.m(EssFeneconBydContainer.ChannelId.STATE_67, 13) //
						.m(EssFeneconBydContainer.ChannelId.STATE_68, 14) //
						.m(EssFeneconBydContainer.ChannelId.STATE_69, 15) //
						.build(),

				// m(EssFeneconBydContainer.ChannelId.PCS_FAULTS_3, new
				// UnsignedWordElement(0x1053)),
				bm(new UnsignedWordElement(0x1053)) //
						.m(EssFeneconBydContainer.ChannelId.STATE_70, 0) //
						.m(EssFeneconBydContainer.ChannelId.STATE_71, 1) //
						.m(EssFeneconBydContainer.ChannelId.STATE_72, 2) //
						.m(EssFeneconBydContainer.ChannelId.STATE_73, 3) //
						.m(EssFeneconBydContainer.ChannelId.STATE_74, 4) //
						.m(EssFeneconBydContainer.ChannelId.STATE_75, 5) //
						.m(EssFeneconBydContainer.ChannelId.STATE_76, 6) //
						.m(EssFeneconBydContainer.ChannelId.STATE_77, 7) //
						.m(EssFeneconBydContainer.ChannelId.STATE_78, 8) //
						.m(EssFeneconBydContainer.ChannelId.STATE_79, 9) //
						.m(EssFeneconBydContainer.ChannelId.STATE_80, 10) //
						.m(EssFeneconBydContainer.ChannelId.STATE_81, 11) //
						.m(EssFeneconBydContainer.ChannelId.STATE_82, 12) //
						.m(EssFeneconBydContainer.ChannelId.STATE_83, 13) //
						.m(EssFeneconBydContainer.ChannelId.STATE_84, 14) //
						.m(EssFeneconBydContainer.ChannelId.STATE_85, 15) //
						.build(),

				// m(EssFeneconBydContainer.ChannelId.PCS_FAULTS_4, new
				// UnsignedWordElement(0x1054)),

				bm(new UnsignedWordElement(0x1054)) //
						.m(EssFeneconBydContainer.ChannelId.STATE_86, 0) //
						.m(EssFeneconBydContainer.ChannelId.STATE_87, 1) //
						.m(EssFeneconBydContainer.ChannelId.STATE_88, 2) //
						.m(EssFeneconBydContainer.ChannelId.STATE_89, 3) //
						.m(EssFeneconBydContainer.ChannelId.STATE_90, 4) //
						.m(EssFeneconBydContainer.ChannelId.STATE_91, 5) //
						.m(EssFeneconBydContainer.ChannelId.STATE_92, 6) //
						.m(EssFeneconBydContainer.ChannelId.STATE_93, 7) //
						.m(EssFeneconBydContainer.ChannelId.STATE_94, 8) //
						.m(EssFeneconBydContainer.ChannelId.STATE_95, 9) //
						.m(EssFeneconBydContainer.ChannelId.STATE_96, 10) //
						.m(EssFeneconBydContainer.ChannelId.STATE_97, 11) //
						.m(EssFeneconBydContainer.ChannelId.STATE_98, 12) //
						.m(EssFeneconBydContainer.ChannelId.STATE_99, 13) //
						.m(EssFeneconBydContainer.ChannelId.STATE_100, 14) //
						.m(EssFeneconBydContainer.ChannelId.STATE_101, 15) //
						.build(),

				// m(EssFeneconBydContainer.ChannelId.PCS_FAULTS_5, new
				// UnsignedWordElement(0x1055))),

				bm(new UnsignedWordElement(0x1055)) //
						.m(EssFeneconBydContainer.ChannelId.STATE_102, 0) //
						.m(EssFeneconBydContainer.ChannelId.STATE_103, 1) //
						.m(EssFeneconBydContainer.ChannelId.STATE_104, 2) //
						.m(EssFeneconBydContainer.ChannelId.STATE_105, 3) //
						.m(EssFeneconBydContainer.ChannelId.STATE_106, 4) //
						.m(EssFeneconBydContainer.ChannelId.STATE_107, 5) //
						.m(EssFeneconBydContainer.ChannelId.STATE_108, 6) //
						.m(EssFeneconBydContainer.ChannelId.STATE_109, 7) //
						.m(EssFeneconBydContainer.ChannelId.STATE_110, 8) //
						.m(EssFeneconBydContainer.ChannelId.STATE_111, 9) //
						.m(EssFeneconBydContainer.ChannelId.STATE_112, 10) //
						.m(EssFeneconBydContainer.ChannelId.STATE_113, 11) //
						.m(EssFeneconBydContainer.ChannelId.STATE_114, 12) //
						.m(EssFeneconBydContainer.ChannelId.STATE_115, 13) //
						.build()),

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
						m(EssFeneconBydContainer.ChannelId.BATTERY_STRING_MAX_VOLTAGE_TEMPARATURE,
								new SignedWordElement(0x6009)),
						m(EssFeneconBydContainer.ChannelId.BATTERY_NUMBER_MIN_STRING_VOLTAGE,
								new UnsignedWordElement(0x600A)),
						m(EssFeneconBydContainer.ChannelId.BATTERY_STRING_MIN_VOLTAGE, new UnsignedWordElement(0x600B)),
						m(EssFeneconBydContainer.ChannelId.BATTERY_STRING_MIN_VOLTAGE_TEMPARATURE,
								new SignedWordElement(0x600C)),
						m(EssFeneconBydContainer.ChannelId.BATTERY_NUMBER_MAX_STRING_TEMPERATURE,
								new UnsignedWordElement(0x600D)),
						m(EssFeneconBydContainer.ChannelId.BATTERY_STRING_MAX_TEMPERATURE,
								new SignedWordElement(0x600E)),
						m(EssFeneconBydContainer.ChannelId.BATTERY_STRING_MAX_TEMPARATURE_VOLTAGE,
								new UnsignedWordElement(0x600F)),
						m(EssFeneconBydContainer.ChannelId.BATTERY_NUMBER_MIN_STRING_TEMPERATURE,
								new UnsignedWordElement(0x6010)),
						m(EssFeneconBydContainer.ChannelId.BATTERY_STRING_MIN_TEMPERATURE,
								new SignedWordElement(0x6011)),
						m(EssFeneconBydContainer.ChannelId.BATTERY_STRING_MIN_TEMPARATURE_VOLTAGE,
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
						m(EssFeneconBydContainer.ChannelId.BATTERY_STRING_WARNING_0_0, new UnsignedWordElement(0x6040)),
						m(EssFeneconBydContainer.ChannelId.BATTERY_STRING_WARNING_0_1, new UnsignedWordElement(0x6041)),
						m(EssFeneconBydContainer.ChannelId.BATTERY_STRING_WARNING_1_0, new UnsignedWordElement(0x6042)),
						m(EssFeneconBydContainer.ChannelId.BATTERY_STRING_WARNING_1_1,
								new UnsignedWordElement(0x6043))));
	}

	private ModbusProtocol defineModbus1Protocol() {
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
						m(EssFeneconBydContainer.ChannelId.ADAS_WARNING_0_0, new UnsignedWordElement(0x3440)),
						m(EssFeneconBydContainer.ChannelId.ADAS_WARNING_0_1, new UnsignedWordElement(0x3441)),
						m(EssFeneconBydContainer.ChannelId.ADAS_WARNING_0_2, new UnsignedWordElement(0x3442)),
						new DummyRegisterElement(0X3443, 0X344F),
						m(EssFeneconBydContainer.ChannelId.ADAS_WARNING_1_0, new UnsignedWordElement(0x3450)),
						m(EssFeneconBydContainer.ChannelId.ADAS_WARNING_1_1, new UnsignedWordElement(0x3451)),
						m(EssFeneconBydContainer.ChannelId.ADAS_WARNING_1_2, new UnsignedWordElement(0x3452)),
						m(EssFeneconBydContainer.ChannelId.ADAS_WARNING_1_3, new UnsignedWordElement(0x3453))));
	}

	protected ModbusProtocol defineModbus2Protocol() {
		return new ModbusProtocol(this, new FC3ReadRegistersTask(0x38A0, Priority.LOW, //
				// RTU registers
				m(EssFeneconBydContainer.ChannelId.SYSTEM_WORKSTATE, new UnsignedWordElement(0x38A0)),
				m(EssFeneconBydContainer.ChannelId.SYSTEM_WORKMODE, new UnsignedWordElement(0x38A1)),
				m(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER, new SignedWordElement(0x38A2),
						ElementToChannelConverter.SCALE_FACTOR_3),
				m(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER, new SignedWordElement(0x38A3),
						ElementToChannelConverter.SCALE_FACTOR_3),
				m(EssFeneconBydContainer.ChannelId.LIMIT_INDUCTIVE_REACTIVE_POWER, new SignedWordElement(0x38A4)),
				m(EssFeneconBydContainer.ChannelId.LIMIT_CAPACITIVE_REACTIVE_POWER, new SignedWordElement(0x38A5)),
				m(SymmetricEss.ChannelId.ACTIVE_POWER, new SignedWordElement(0x38A6),
						ElementToChannelConverter.SCALE_FACTOR_2),
				m(SymmetricEss.ChannelId.REACTIVE_POWER, new SignedWordElement(0x38A7),
						ElementToChannelConverter.SCALE_FACTOR_2),
				m(SymmetricEss.ChannelId.SOC, new UnsignedWordElement(0x38A8)),
				m(EssFeneconBydContainer.ChannelId.CONTAINER_RUN_NUMBER, new UnsignedWordElement(0x38A9))),
				new FC16WriteRegistersTask(0x0500,
						m(EssFeneconBydContainer.ChannelId.SET_SYSTEM_WORKSTATE, new UnsignedWordElement(0x0500)),
						m(EssFeneconBydContainer.ChannelId.SET_ACTIVE_POWER, new SignedWordElement(0x0501),
								ElementToChannelConverter.SCALE_FACTOR_3),
						m(EssFeneconBydContainer.ChannelId.SET_REACTIVE_POWER, new SignedWordElement(0x0502),
								ElementToChannelConverter.SCALE_FACTOR_3)),
				new FC16WriteRegistersTask(0x0601, //
						m(EssFeneconBydContainer.ChannelId.SYSTEM_WORKMODE, new UnsignedWordElement(0x0601))));
	}

//	@Override
//	public String debugLog() {
//		return "SoC:" + this.getSoc().value().asString() //
//				+ "|RTU_WS" + this.channel(EssFeneconBydContainer.ChannelId.SYSTEM_WORKSTATE).value().asString()
//				+ "|PCS_WS" + this.channel(EssFeneconBydContainer.ChannelId.PCS_SYSTEM_WORKSTATE).value().asString()
//				+ "|BECU_WS"
//				+ this.channel(EssFeneconBydContainer.ChannelId.BATTERY_STRING_WORK_STATE).value().asString();
//
//	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().value().asString() //
				+ "|L:" + this.getActivePower().value().asString() //
				+ "|Allowed:"
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER).value().asStringWithoutUnit() + ";"
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER).value().asString() //
				+ "|" + this.getGridMode().value().asOptionString();
	}

}
