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

	private final static int UNIT_ID = 1310;
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
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.enabled(), UNIT_ID, this.cm, "Modbus", config.modbus_id());

		this.modbusBridgeId = config.modbus_id();
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
		LIMIT_INDUCTIVE_REACTIVE_POWER(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)),
		LIMIT_CAPACITIVE_REACTIVE_POWER(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)),
		CONTAINER_RUN_NUMBER(new Doc().unit(Unit.NONE)),
		SET_SYSTEM_WORKSTATE(new Doc().options(SetSystemWorkstate.values())),
		SET_ACTIVE_POWER(new Doc().unit(Unit.KILOWATT)),
		// TODO SET_ACTIVE_POWER -> kW!
		SET_REACTIVE_POWER(new Doc().unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		// TODO SET_REACTIVE_POWER -> kvar!
		// PCS registers
		PCS_SYSTEM_WORKSTATE(new Doc().options(SystemWorkstate.values())),
		PCS_SYSTEM_WORKMODE(new Doc().options(SystemWorkmode.values())),
		PHASE3_ACTIVE_POWER(new Doc().unit(Unit.KILOWATT)),
		PHASE3_REACTIVE_POWER(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)),
		PHASE3_INSPECTING_POWER(new Doc().unit(Unit.VOLT_AMPERE)),
		// Conversion required to from VA to kVA
		PCS_DISCHARGE_LIMIT_ACTIVE_POWER(new Doc().unit(Unit.KILOWATT)),
		PCS_CHARGE_LIMIT_ACTIVE_POWER(new Doc().unit(Unit.KILOWATT)),
		POSITIVE_REACTIVE_POWER_LIMIT(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)),
		// TODO -> whats this?
		// Conversion required to from var to kvar
		NEGATIVE_REACTIVE_POWER_LIMIT(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)),
		CURRENT_L1(new Doc().unit(Unit.AMPERE)), CURRENT_L2(new Doc().unit(Unit.AMPERE)),
		CURRENT_L3(new Doc().unit(Unit.AMPERE)), VOLTAGE_L1(new Doc().unit(Unit.VOLT)),
		VOLTAGE_L2(new Doc().unit(Unit.VOLT)), VOLTAGE_L3(new Doc().unit(Unit.VOLT)),
		VOLTAGE_L12(new Doc().unit(Unit.VOLT)), VOLTAGE_L23(new Doc().unit(Unit.VOLT)),
		VOLTAGE_L31(new Doc().unit(Unit.VOLT)), SYSTEM_FREQUENCY(new Doc().unit(Unit.HERTZ)),
		DC_VOLTAGE(new Doc().unit(Unit.VOLT)), DC_CURRENT(new Doc().unit(Unit.AMPERE)),
		DC_POWER(new Doc().unit(Unit.KILOWATT)), IGBT_TEMPERATURE_L1(new Doc().unit(Unit.DEGREE_CELSIUS)),
		IGBT_TEMPERATURE_L2(new Doc().unit(Unit.DEGREE_CELSIUS)),
		IGBT_TEMPERATURE_L3(new Doc().unit(Unit.DEGREE_CELSIUS)), PCS_WARNING_0(new Doc().unit(Unit.NONE)),
		PCS_WARNING_1(new Doc().unit(Unit.NONE)), PCS_WARNING_2(new Doc().unit(Unit.NONE)),
		PCS_WARNING_3(new Doc().unit(Unit.NONE)), PCS_FAULTS_0(new Doc().unit(Unit.NONE)),
		PCS_FAULTS_1(new Doc().unit(Unit.NONE)), PCS_FAULTS_2(new Doc().unit(Unit.NONE)),
		PCS_FAULTS_3(new Doc().unit(Unit.NONE)), PCS_FAULTS_4(new Doc().unit(Unit.NONE)),
		PCS_FAULTS_5(new Doc().unit(Unit.NONE)),
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
		BATTERY_STRING_WARNING_0_0(new Doc().unit(Unit.NONE)), BATTERY_STRING_WARNING_0_1(new Doc().unit(Unit.NONE)),
		BATTERY_STRING_WARNING_1_0(new Doc().unit(Unit.NONE)), BATTERY_STRING_WARNING_1_1(new Doc().unit(Unit.NONE)),
		// ADAS register addresses
		CONTAINER_IMMERSION_STATE_1(new Doc().level(Level.WARNING).text("Immersion happens")),
		CONTAINER_IMMERSION_STATE_0(new Doc().level(Level.WARNING).text("Immersion vanishes")),
		CONTAINER_FIRE_STATUS_1(new Doc().level(Level.WARNING).text("Fire alarm")),
		CONTAINER_FIRE_STATUS_0(new Doc().level(Level.WARNING).text("Fire alarm vanishes")),
		CONTROL_CABINET_STATE_1(new Doc().level(Level.WARNING).text("Sudden stop press down")),
		CONTROL_CABINET_STATE_0(new Doc().level(Level.WARNING).text("Sudden stop spin up")),
		CONTAINER_GROUNDING_FAULT_1(new Doc().level(Level.WARNING).text("Grounding fault happens")),
		CONTAINER_GROUNDING_FAULT_0(new Doc().level(Level.WARNING).text("Grounding fault vanishes")),
		CONTAINER_DOOR_STATUS_0_1(new Doc().level(Level.WARNING).text("Door opens")),
		CONTAINER_DOOR_STATUS_0_0(new Doc().level(Level.WARNING).text("Door closes")),
		CONTAINER_DOOR_STATUS_1_1(new Doc().level(Level.WARNING).text("Door opens")),
		CONTAINER_DOOR_STATUS_1_0(new Doc().level(Level.WARNING).text("Door closes")),
		CONTAINER_AIRCONDITION_POWER_SUPPLY_STATE_1(
				new Doc().level(Level.WARNING).text("Air conditioning power supply")),
		CONTAINER_AIRCONDITION_POWER_SUPPLY_STATE_0(new Doc().level(Level.WARNING).text("Air conditioning shuts down")),
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
	protected ModbusProtocol defineModbusProtocol() { // Double of Not ???
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(0x38A0, Priority.LOW, //
						// RTU registers
						m(EssFeneconBydContainer.ChannelId.SYSTEM_WORKSTATE, new UnsignedWordElement(0x38A0)),
						m(EssFeneconBydContainer.ChannelId.SYSTEM_WORKMODE, new UnsignedWordElement(0x38A1)),
						m(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER, new SignedWordElement(0x38A2),
								ElementToChannelConverter.SCALE_FACTOR_3),
						m(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER, new SignedWordElement(0x38A3),
								ElementToChannelConverter.SCALE_FACTOR_3),
						m(EssFeneconBydContainer.ChannelId.LIMIT_INDUCTIVE_REACTIVE_POWER,
								new SignedWordElement(0x38A4)),
						m(EssFeneconBydContainer.ChannelId.LIMIT_CAPACITIVE_REACTIVE_POWER,
								new SignedWordElement(0x38A5)),
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
						m(EssFeneconBydContainer.ChannelId.SYSTEM_WORKMODE, new UnsignedWordElement(0x0601))),
				// PCS registers
				// TODO multiple PCS!
				// PCS information list 0x1000-0x2FFF offset 0x200 up to 4 PCSs information
				// 0x1000~0x11F is for 1#PCS
				// 0x1200~0x13FF is for 2#PCS
				// 0x1400~0x15FFis for 3#PCS
				// 0x1600~0x17FFis for 4#PCS
				new FC3ReadRegistersTask(0x1001, Priority.LOW,
						m(EssFeneconBydContainer.ChannelId.PCS_SYSTEM_WORKSTATE, new UnsignedWordElement(0x1001)),
						m(EssFeneconBydContainer.ChannelId.PCS_SYSTEM_WORKMODE, new UnsignedWordElement(0x1002)),
						m(EssFeneconBydContainer.ChannelId.PHASE3_ACTIVE_POWER, new SignedWordElement(0x1003)),
						m(EssFeneconBydContainer.ChannelId.PHASE3_REACTIVE_POWER, new SignedWordElement(0x1004)),
						m(EssFeneconBydContainer.ChannelId.PHASE3_INSPECTING_POWER, new UnsignedWordElement(0x1005)),
						m(EssFeneconBydContainer.ChannelId.PCS_DISCHARGE_LIMIT_ACTIVE_POWER,
								new UnsignedWordElement(0x1006)),
						m(EssFeneconBydContainer.ChannelId.PCS_CHARGE_LIMIT_ACTIVE_POWER,
								new SignedWordElement(0x1007)),
						m(EssFeneconBydContainer.ChannelId.POSITIVE_REACTIVE_POWER_LIMIT,
								new UnsignedWordElement(0x1008)),
						m(EssFeneconBydContainer.ChannelId.NEGATIVE_REACTIVE_POWER_LIMIT,
								new SignedWordElement(0x1009)),
						m(EssFeneconBydContainer.ChannelId.CURRENT_L1, new SignedWordElement(0x100A)),
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
						new DummyRegisterElement(0x101A, 0X103F),
						m(EssFeneconBydContainer.ChannelId.PCS_WARNING_0, new UnsignedWordElement(0x1040)),
						m(EssFeneconBydContainer.ChannelId.PCS_WARNING_1, new UnsignedWordElement(0x1041)),
						m(EssFeneconBydContainer.ChannelId.PCS_WARNING_2, new UnsignedWordElement(0x1042)),
						m(EssFeneconBydContainer.ChannelId.PCS_WARNING_3, new UnsignedWordElement(0x1043)),
						new DummyRegisterElement(0x1044, 0X104F),
						m(EssFeneconBydContainer.ChannelId.PCS_FAULTS_0, new UnsignedWordElement(0x1050)),
						m(EssFeneconBydContainer.ChannelId.PCS_FAULTS_1, new UnsignedWordElement(0x1051)),
						m(EssFeneconBydContainer.ChannelId.PCS_FAULTS_2, new UnsignedWordElement(0x1052)),
						m(EssFeneconBydContainer.ChannelId.PCS_FAULTS_3, new UnsignedWordElement(0x1053)),
						m(EssFeneconBydContainer.ChannelId.PCS_FAULTS_4, new UnsignedWordElement(0x1054)),
						m(EssFeneconBydContainer.ChannelId.PCS_FAULTS_5, new UnsignedWordElement(0x1055))),
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
								new UnsignedWordElement(0x6043))),
				// ADAS address list
				new FC3ReadRegistersTask(0x3410, Priority.LOW,
						bm(new UnsignedWordElement(0x3410))
								.m(EssFeneconBydContainer.ChannelId.CONTAINER_IMMERSION_STATE_1, 1)
								.m(EssFeneconBydContainer.ChannelId.CONTAINER_IMMERSION_STATE_0, 0).build(),
						bm(new UnsignedWordElement(0x3411))
								.m(EssFeneconBydContainer.ChannelId.CONTAINER_FIRE_STATUS_1, 1)
								.m(EssFeneconBydContainer.ChannelId.CONTAINER_FIRE_STATUS_0, 0).build(),
						bm(new UnsignedWordElement(0x3412))
								.m(EssFeneconBydContainer.ChannelId.CONTROL_CABINET_STATE_1, 1)
								.m(EssFeneconBydContainer.ChannelId.CONTROL_CABINET_STATE_0, 0).build(),
						bm(new UnsignedWordElement(0x3413))
								.m(EssFeneconBydContainer.ChannelId.CONTAINER_GROUNDING_FAULT_1, 1)
								.m(EssFeneconBydContainer.ChannelId.CONTAINER_GROUNDING_FAULT_0, 0).build(),
						bm(new UnsignedWordElement(0x3414))
								.m(EssFeneconBydContainer.ChannelId.CONTAINER_DOOR_STATUS_0_1, 1)
								.m(EssFeneconBydContainer.ChannelId.CONTAINER_DOOR_STATUS_0_0, 0).build(),
						bm(new UnsignedWordElement(0x3415))
								.m(EssFeneconBydContainer.ChannelId.CONTAINER_DOOR_STATUS_1_1, 1)
								.m(EssFeneconBydContainer.ChannelId.CONTAINER_DOOR_STATUS_1_0, 0).build(),
						bm(new UnsignedWordElement(0x3416))
								.m(EssFeneconBydContainer.ChannelId.CONTAINER_AIRCONDITION_POWER_SUPPLY_STATE_1, 1)
								.m(EssFeneconBydContainer.ChannelId.CONTAINER_AIRCONDITION_POWER_SUPPLY_STATE_0, 0)
								.build(),
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
