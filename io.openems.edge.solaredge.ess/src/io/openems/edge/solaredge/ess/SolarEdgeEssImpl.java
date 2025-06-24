package io.openems.edge.solaredge.ess;

import static io.openems.edge.common.cycle.Cycle.DEFAULT_CYCLE_TIME;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.DIRECT_1_TO_1;
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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import io.openems.edge.common.event.EdgeEventConstants;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel.S1;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel.S101;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel.S102;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel.S103;
import io.openems.edge.common.channel.FloatReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.cycle.Cycle;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSinglePhaseEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SinglePhase;
import io.openems.edge.ess.api.SinglePhaseEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.generic.common.CycleProvider;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.solaredge.ess.charger.SolarEdgeCharger;
import io.openems.edge.solaredge.ess.common.AbstractSunSpecEss;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;


@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "SolarEdge.ESS", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
	EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
	EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
	EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class SolarEdgeEssImpl extends AbstractSunSpecEss implements SolarEdgeEss, ManagedSinglePhaseEss, SinglePhaseEss,
				ManagedAsymmetricEss, AsymmetricEss, ManagedSymmetricEss, SymmetricEss, HybridEss, ModbusComponent,
				OpenemsComponent, EventHandler, ModbusSlave, TimedataProvider, CycleProvider {

	private static enum InverterType {
		SINGLE_PHASE(DefaultSunSpecModel.S_101), //
		SPLIT_PHASE(DefaultSunSpecModel.S_102), //
		THREE_PHASE(DefaultSunSpecModel.S_103);

		private final List<DefaultSunSpecModel> blocks;

		private InverterType(DefaultSunSpecModel... blocks) {
			this.blocks = Lists.newArrayList(blocks);
		}
	}	
	
	private final AllowedChargeDischargeHandler allowedChargeDischargeHandler = new AllowedChargeDischargeHandler(this);	
	private final ApplyPowerHandler applyPowerHandler = new ApplyPowerHandler();
	private final SetPvExportLimitHandler setPvExportLimitHandler = new SetPvExportLimitHandler(this);
	
	private static final int READ_FROM_MODBUS_BLOCK = 1;
	protected final Set<SolarEdgeCharger> chargers = new HashSet<>();
	
	private final Logger log = LoggerFactory.getLogger(SolarEdgeEss.class);
	
	private Config config;
	private SinglePhase singlePhase = null;
	private InverterType inverterType = null;
	
	private final CalculateEnergyFromPower calculateAcChargeEnergy = new CalculateEnergyFromPower(this, SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY);
	private final CalculateEnergyFromPower calculateAcDischargeEnergy = new CalculateEnergyFromPower(this, SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY);
	private final CalculateEnergyFromPower calculateDcChargeEnergy = new CalculateEnergyFromPower(this, HybridEss.ChannelId.DC_CHARGE_ENERGY);
	private final CalculateEnergyFromPower calculateDcDischargeEnergy = new CalculateEnergyFromPower(this, HybridEss.ChannelId.DC_DISCHARGE_ENERGY);

	private static final Map<SunSpecModel, Priority> ACTIVE_MODELS = ImmutableMap.<SunSpecModel, Priority>builder()
			.put(DefaultSunSpecModel.S_1, Priority.LOW) //
			.put(DefaultSunSpecModel.S_101, Priority.LOW) //
			.put(DefaultSunSpecModel.S_102, Priority.LOW) //
			.put(DefaultSunSpecModel.S_103, Priority.LOW) //
			.build();
	
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;	
	
	@Reference
	private Cycle cycle;
	
	@Reference
	private ConfigurationAdmin cm;
	
	@Reference
	private Power power;	
	
	@Reference
	private Sum sum;
	
	@Reference
	private ComponentManager componentManager;	

	public SolarEdgeEssImpl() throws OpenemsNamedException {
		super(//
				ACTIVE_MODELS, //
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				HybridEss.ChannelId.values(), //				
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				AsymmetricEss.ChannelId.values(), //
				ManagedAsymmetricEss.ChannelId.values(), //
				SinglePhaseEss.ChannelId.values(), //
				ManagedSinglePhaseEss.ChannelId.values(), //
				SolarEdgeEss.ChannelId.values() //
		);	
	}
	
	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id(), READ_FROM_MODBUS_BLOCK)) {
			return;
		}		

		// Evaluate 'SinglePhase'
		switch (config.phase()) {
		case ALL:
			this.singlePhase = null;
			break;
		case L1:
			this.singlePhase = SinglePhase.L1;
			break;
		case L2:
			this.singlePhase = SinglePhase.L2;
			break;
		case L3:
			this.singlePhase = SinglePhase.L3;
			break;
		}		
		
		// update filter for 'Controllers'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "Controllers", config.id())) {
			return;
		}			

		this._setGridMode(GridMode.ON_GRID);
		this.addStaticModbusTasks(this.getModbusProtocol());
	}
	
	//@Override
	protected void onSunSpecInitializationCompleted() {
		this.logInfo(this.log, "SunSpec initialization finished. " + this.channels().size() + " Channels available.");	

		this.channel(SolarEdgeEss.ChannelId.WRONG_PHASE_CONFIGURED).setNextValue(
				this.inverterType == InverterType.SINGLE_PHASE ? this.config.phase() == Phase.ALL : this.config.phase() != Phase.ALL);
		
		this.mapFirstPointToChannel(//
				SolarEdgeEss.ChannelId.SERIAL_NUMBER, //
				DIRECT_1_TO_1, //
				S1.SN);
		
		this.mapFirstPointToChannel(//
				SymmetricEss.ChannelId.ACTIVE_POWER, //
				DIRECT_1_TO_1, //
				S101.W, S102.W, S103.W);
		this.mapFirstPointToChannel(//
				SymmetricEss.ChannelId.REACTIVE_POWER, //
				DIRECT_1_TO_1, //
				S101.V_AR, S102.V_AR, S103.V_AR);
		
		// Individual Phases Power
		switch (this.inverterType) {
			case SINGLE_PHASE -> {
				SolarEdgeEssImpl.calculateSinglePhaseFromActivePower(this, this.config.phase());
			}
			case SPLIT_PHASE, THREE_PHASE -> {
				SolarEdgeEssImpl.calculatePhasesFromActivePower(this);
				SolarEdgeEssImpl.calculatePhasesFromReactivePower(this);
			}
		}		
		
		// Energy
		this.mapFirstPointToChannel(//
				SolarEdgeEss.ChannelId.ACTIVE_PRODUCTION_ENERGY, //
				DIRECT_1_TO_1, //
				S101.WH, S102.WH, S103.WH);
		
		// Voltage
		this.mapFirstPointToChannel(//
				SolarEdgeEss.ChannelId.VOLTAGE_DC, //
				SCALE_FACTOR_3, // Convert V to mV
				S101.DCV, S102.DCV, S103.DCV);
		/*
		 * TODO: Do we need these channels?
		 * 
		this.mapFirstPointToChannel(SolarEdgeEss.ChannelId.VOLTAGE_L1, //
				SCALE_FACTOR_3, // Convert V to mV
				S101.PH_VPH_A, S102.PH_VPH_A, S103.PH_VPH_A);
		this.mapFirstPointToChannel(SolarEdgeEss.ChannelId.VOLTAGE_L2, //
				SCALE_FACTOR_3, // Convert V to mV
				S101.PH_VPH_B, S102.PH_VPH_B, S103.PH_VPH_B);
		this.mapFirstPointToChannel(SolarEdgeEss.ChannelId.VOLTAGE_L3, //
				SCALE_FACTOR_3, // Convert V to mV
				S101.PH_VPH_C, S102.PH_VPH_C, S103.PH_VPH_C);
		*/
		
		// Current
		this.mapFirstPointToChannel(//
				SolarEdgeEss.ChannelId.CURRENT_DC, //
				SCALE_FACTOR_3, // Convert A to mA
				S101.DCA, S102.DCA, S103.DCA);
		/*
		 * 	TODO: Do we need these channels?
		 * 
		this.mapFirstPointToChannel(SolarEdgeEss.ChannelId.CURRENT_L1, //
				SCALE_FACTOR_3, // Convert A to mA
				S111.APH_A, S112.APH_A, S113.APH_A, S101.APH_A, S102.APH_A, S103.APH_A);
		this.mapFirstPointToChannel(SolarEdgeEss.ChannelId.CURRENT_L2, //
				SCALE_FACTOR_3, // Convert A to mA
				S111.APH_B, S112.APH_B, S113.APH_B, S101.APH_B, S102.APH_B, S103.APH_B);
		this.mapFirstPointToChannel(SolarEdgeEss.ChannelId.CURRENT_L3, //
				SCALE_FACTOR_3, // Convert A to mA
				S111.APH_C, S112.APH_C, S113.APH_C, S101.APH_C, S102.APH_C, S103.APH_C);		
		*/
	}
		
	@Override
	protected void addBlock(int startAddress, SunSpecModel model, Priority priority) {
		super.addBlock(startAddress, model, priority);

		// Can we evaluate the InverterType from this Block?
		Stream.of(InverterType.values()) //
				.filter(type -> type.blocks.stream().anyMatch(t -> t.equals(model))) //
				.findFirst() //
				.ifPresent(type -> this.inverterType = type);
	}		

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	/**
	 * Adds static modbus tasks.
	 * 
	 * @param protocol the {@link ModbusProtocol}
	 * @throws OpenemsException on error
	 */
	private void addStaticModbusTasks(ModbusProtocol protocol) throws OpenemsException {
			
		// StorEdge Control Block register
		protocol.addTask(//
				new FC3ReadRegistersTask(0xE004, Priority.LOW, //
						m(SolarEdgeEss.ChannelId.STORAGE_CONTROL_MODE, new UnsignedWordElement(0xE004)),
						m(SolarEdgeEss.ChannelId.STORAGE_AC_CHARGE_POLICY, new UnsignedWordElement(0xE005)),
						m(SolarEdgeEss.ChannelId.STORAGE_AC_CHARGE_LIMIT,
								new FloatDoublewordElement(0xE006).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeEss.ChannelId.STORAGE_BACKUP_RESERVED_SETTING,
								new FloatDoublewordElement(0xE008).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeEss.ChannelId.STORAGE_CHARGE_DISCHARGE_DEFAULT_MODE, new UnsignedWordElement(0xE00A)),
						m(SolarEdgeEss.ChannelId.REMOTE_CONTROL_COMMAND_TIMEOUT,
								new UnsignedDoublewordElement(0xE00B).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeEss.ChannelId.REMOTE_CONTROL_COMMAND_MODE, new UnsignedWordElement(0xE00D)),
						m(SolarEdgeEss.ChannelId.REMOTE_CONTROL_COMMAND_CHARGE_LIMIT,
								new FloatDoublewordElement(0xE00E).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeEss.ChannelId.REMOTE_CONTROL_COMMAND_DISCHARGE_LIMIT,
								new FloatDoublewordElement(0xE010).wordOrder(WordOrder.LSWMSW))));		
		
		// StorEdge Battery 1 Status and Information Block register (Task 1)
		protocol.addTask(//
				new FC3ReadRegistersTask(0xE144, Priority.HIGH, //
						m(SolarEdgeEss.ChannelId.BATTERY1_MAX_CHARGE_CONTINUES_POWER, // feeds AllowedChargeDischargeHandler
								new FloatDoublewordElement(0xE144).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeEss.ChannelId.BATTERY1_MAX_DISCHARGE_CONTINUES_POWER, // feeds AllowedChargeDischargeHandler
								new FloatDoublewordElement(0xE146).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeEss.ChannelId.BATTERY1_MAX_CHARGE_PEAK_POWER, //
								new FloatDoublewordElement(0xE148).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeEss.ChannelId.BATTERY1_MAX_DISCHARGE_PEAK_POWER, //
								new FloatDoublewordElement(0xE14A).wordOrder(WordOrder.LSWMSW)),
						new DummyRegisterElement(0xE14C, 0xE16B), // Reserved
						m(SolarEdgeEss.ChannelId.BATTERY1_AVG_TEMPERATURE, //
								new FloatDoublewordElement(0xE16C).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeEss.ChannelId.BATTERY1_MAX_TEMPERATURE, //
								new FloatDoublewordElement(0xE16E).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeEss.ChannelId.BATTERY1_ACTUAL_VOLTAGE, //
								new FloatDoublewordElement(0xE170).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeEss.ChannelId.BATTERY1_ACTUAL_CURRENT, // Instantaneous Battery Current (charge / discharge) 
								new FloatDoublewordElement(0xE172).wordOrder(WordOrder.LSWMSW), SCALE_FACTOR_3), // Convert A to mA
						m(SolarEdgeEss.ChannelId.BATTERY1_ACTUAL_POWER, // Instantaneous Battery Power (charge / discharge) 
								new FloatDoublewordElement(0xE174).wordOrder(WordOrder.LSWMSW)),

						// Active Charge / Discharge energy are only valid until the next day/loading
						// cycle (not clear or verified)
						m(SolarEdgeEss.ChannelId.BATTERY1_LIFETIME_EXPORT_ENERGY, //
								new UnsignedQuadruplewordElement(0xE176).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeEss.ChannelId.BATTERY1_LIFETIME_IMPORT_ENERGY, //
								new UnsignedQuadruplewordElement(0xE17A).wordOrder(WordOrder.LSWMSW))));
		
		// StorEdge Battery 1 Status and Information Block register (Task 2)
		protocol.addTask(//
				new FC3ReadRegistersTask(0xE17E, Priority.LOW,
						m(SolarEdgeEss.ChannelId.BATTERY1_MAX_CAPACITY, //
								new FloatDoublewordElement(0xE17E).wordOrder(WordOrder.LSWMSW)),
						m(SymmetricEss.ChannelId.CAPACITY, // Available capacity or "real" capacity
								new FloatDoublewordElement(0xE180).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeEss.ChannelId.SOH, //
								new FloatDoublewordElement(0xE182).wordOrder(WordOrder.LSWMSW)),
						m(SymmetricEss.ChannelId.SOC, //
								new FloatDoublewordElement(0xE184).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeEss.ChannelId.BATTERY1_STATUS, //
								new UnsignedDoublewordElement(0xE186).wordOrder(WordOrder.LSWMSW))));		
		
		// Smart Meter register
		protocol.addTask(//
				new FC3ReadRegistersTask(40121, Priority.LOW, //
						m(SolarEdgeEss.ChannelId.METER_COMMUNICATE_STATUS, new UnsignedWordElement(40121))));
		
		// Power Control Block Register
		protocol.addTask(//
				new FC3ReadRegistersTask(0xF140, Priority.LOW, //
						m(SolarEdgeEss.ChannelId.INVERTER_POWER_LIMIT, // Power Reduction (e.g. 70%)
								new FloatDoublewordElement(0xF140).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeEss.ChannelId.ADVANCED_PWR_CONTROL_EN, // AdvacedPwrControlEn
								new SignedDoublewordElement(0xF142).wordOrder(WordOrder.LSWMSW))));
	
		// Enhanced Power Control Block register
		protocol.addTask(//
				new FC3ReadRegistersTask(0xF304, Priority.LOW, //
						m(SolarEdgeEss.ChannelId.INVERTER_MAX_APPARENT_POWER,
								new FloatDoublewordElement(0xF304).wordOrder(WordOrder.LSWMSW))));

		// Export Limit Control Block		
		protocol.addTask(//
				new FC3ReadRegistersTask(0xE000, Priority.LOW, //
						m(SolarEdgeEss.ChannelId.EXPORT_CONTROL_MODE, new UnsignedWordElement(0xE000)),
						m(SolarEdgeEss.ChannelId.EXPORT_CONTROL_LIMIT_MODE, new UnsignedWordElement(0xE001)),
						m(SolarEdgeEss.ChannelId.EXPORT_CONTROL_SITE_LIMIT, new FloatDoublewordElement(0xE002).wordOrder(WordOrder.LSWMSW))));		

		// StorEdge Control Block register
		protocol.addTask(//
				new FC16WriteRegistersTask(0xE00B,
						m(SolarEdgeEss.ChannelId.REMOTE_CONTROL_COMMAND_TIMEOUT,
								new UnsignedDoublewordElement(0xE00B).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeEss.ChannelId.REMOTE_CONTROL_COMMAND_MODE,
								new UnsignedWordElement(0xE00D)),
						m(SolarEdgeEss.ChannelId.REMOTE_CONTROL_COMMAND_CHARGE_LIMIT,
								new FloatDoublewordElement(0xE00E).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeEss.ChannelId.REMOTE_CONTROL_COMMAND_DISCHARGE_LIMIT,
								new FloatDoublewordElement(0xE010).wordOrder(WordOrder.LSWMSW))));
		
		// Export Limit Control Block
		protocol.addTask(//
				new FC16WriteRegistersTask(0xE002,
						m(SolarEdgeEss.ChannelId.EXPORT_CONTROL_SITE_LIMIT,
								new FloatDoublewordElement(0xE002).wordOrder(WordOrder.LSWMSW))));
	}	
	
	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {
		this.calculateMaxAcPower(this.getMaxApparentPower().orElse(0));
		
		// For Test/Debug
		this.setChargePowerWanted(activePower);
		
		// Apply Power Set-Point
		this.applyPowerHandler.apply(this, activePower, this.config.controlMode(), this.sum.getGridActivePower(),
				this.getActivePower(), this.getMaxAcImport(), this.getMaxAcExport(), this.power.isPidEnabled());
	}
	
	@Override
	public void applyPower(int activePowerL1, int reactivePowerL1, int activePowerL2, int reactivePowerL2,
			int activePowerL3, int reactivePowerL3) throws OpenemsNamedException {
		if (this.config.phase() == Phase.ALL) {
			return;
		}

		ManagedSinglePhaseEss.super.applyPower(activePowerL1, reactivePowerL1, activePowerL2, reactivePowerL2,
				activePowerL3, reactivePowerL3);
	}	

	/**
	 * Calculate and store Max-AC-Export and -Import channels.
	 *
	 * @param maxApparentPower the max apparent power
	 */
	protected void calculateMaxAcPower(int maxApparentPower) {
		// Calculate and store Max-AC-Export and -Import for use in
		// getStaticConstraints()
		var maxDcChargePower = /* can be negative for force-discharge */
				TypeUtils.max(0, this.getBattery1MaxChargeContinuesPower().get());
		int pvProduction = TypeUtils.max(0, this.getPvProduction());

		// Calculates Max-AC-Import and Max-AC-Export as positive numbers
		var maxAcImport = TypeUtils.subtract(maxDcChargePower,
				TypeUtils.min(maxDcChargePower /* avoid negative number for `subtract` */, pvProduction));
		var maxAcExport = TypeUtils.sum(//
				/* Max DC-Discharge-Power */
				TypeUtils.max(0, this.getBattery1MaxDischargeContinuesPower().get()),
				/* PV Production */ pvProduction);

		// Limit Max-AC-Power to inverter specific limit
		maxAcImport = TypeUtils.min(maxAcImport, maxApparentPower);
		maxAcExport = TypeUtils.min(maxAcExport, maxApparentPower);

		// Set Channels
		this._setMaxAcImport(TypeUtils.multiply(maxAcImport, /* negate */ -1));
		this._setMaxAcExport(maxAcExport);
	}	
	
	@Override
	public String debugLog() {

		return "SoC:" + this.getSoc().asString() //
				+ "|L:" + this.getActivePower().asString()
				+ "|PowerLimit:" + this.getInverterPowerLimit().asString()
				+ "|Allowed:" + this.getAllowedChargePower().asStringWithoutUnit() + ";"
				+ this.getAllowedDischargePower().asString()
				+ "|" + this.getGridModeChannel().value().asOptionString()				
				//+ "|SN:" + this.channel(SolarEdgeEss.ChannelId.SERIAL_NUMBER).value().get()
				+ "|MaxAcImport:" + this.channel(SolarEdgeEss.ChannelId.MAX_AC_IMPORT).value().get()
				+ "|MaxAcExport:" + this.channel(SolarEdgeEss.ChannelId.MAX_AC_EXPORT).value().get()
				+ "|SmartMeter:" + this.channel(SolarEdgeEss.ChannelId.METER_COMMUNICATE_STATUS).value().asOptionString()
				+ "|ACCharge:" + this.channel(SolarEdgeEss.ChannelId.STORAGE_AC_CHARGE_POLICY).value().asOptionString()
				+ "|ControlMode:" + this.channel(SolarEdgeEss.ChannelId.STORAGE_CONTROL_MODE).value().asOptionString()
				+ "|ApplyPower:" + this.channel(SolarEdgeEss.ChannelId.DEBUG_REMOTE_CONTROL_COMMAND_CHARGE_LIMIT).value().asStringWithoutUnit() + ";"
				+ this.channel(SolarEdgeEss.ChannelId.DEBUG_REMOTE_CONTROL_COMMAND_DISCHARGE_LIMIT).value().asString()
				+ "|Mode:" + this.channel(SolarEdgeEss.ChannelId.DEBUG_REMOTE_CONTROL_COMMAND_MODE).value().asOptionString();

		/*		
		return "SoC:" + this.getSoc().asString() //
				+ "|L:" + this.getActivePower().asString() //
				+ "|Allowed:" + this.getAllowedChargePower().asStringWithoutUnit() + ";" //
				+ this.getAllowedDischargePower().asString() //
				+ "|" + this.getGridModeChannel().value().asOptionString();
		*/ 
	}
	
	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}

		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.updateMaxApparentPowerChannel();
			this.updateDcDischargePowerChannel();
			this.updateEnergyChannels();
			break;
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.allowedChargeDischargeHandler.accept(this.componentManager);
			break;
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			// Get ActiveExportPowerLimit that should be applied
			var activeExportPowerLimitChannel = (IntegerWriteChannel) this
					.channel(SolarEdgeEss.ChannelId.ACTIVE_EXPORT_POWER_LIMIT);
			var activeExportPowerLimitOpt = activeExportPowerLimitChannel.getNextWriteValueAndReset();
			
			// Set warning if pvExportLimit mode is disabled but a PV export limit was requested
			this.channel(SolarEdgeEss.ChannelId.DISABLED_PV_EXPORT_LIMIT_FAILED)
					.setNextValue(!this.config.pvExportLimit() && activeExportPowerLimitOpt.isPresent());
	
			// If pvExportLimit mode is disabled: stop here
			if (!this.config.pvExportLimit()) {
				return;
			}
	
			try {
				this.setPvExportLimitHandler.accept(activeExportPowerLimitOpt);
	
				this.channel(SolarEdgeEss.ChannelId.PV_EXPORT_LIMIT_FAILED).setNextValue(false);
			} catch (OpenemsNamedException e) {
				this.channel(SolarEdgeEss.ChannelId.PV_EXPORT_LIMIT_FAILED).setNextValue(true);
			}
			break;
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
	public SinglePhase getPhase() {
		return this.singlePhase;
	}	
	
	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public Integer getSurplusPower() {
		// Inverter will feed surplus power to grid. Ess.Hybrid.Surplus-Feed-To-Grid Controller not required 
		return null;
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricEss.getModbusSlaveNatureTable(accessMode), //
				HybridEss.getModbusSlaveNatureTable(accessMode), //
				this.getModbusSlaveNatureTable(accessMode)

		);
	}

	@Override
	public void addCharger(SolarEdgeCharger charger) {
		this.chargers.add(charger);
	}

	@Override
	public void removeCharger(SolarEdgeCharger charger) {
		this.chargers.remove(charger);
	}

	@Override
	public String getModbusBridgeId() {
		return this.config.modbus_id();
	}

	/**
	 * Gets the PV production from chargers ACTUAL_POWER. Returns null if the PV
	 * production is not available.
	 *
	 * @return production power
	 */
	public Integer getPvProduction() {
		Integer productionPower = null;
		for (SolarEdgeCharger charger : this.chargers) {
			productionPower = TypeUtils.sum(productionPower, charger.getActualPower().get());
		}
		return productionPower;
	}	

	protected void updateMaxApparentPowerChannel() {
		
		/*
		 * Fill MaxApparentPower Channel
		 */
		FloatReadChannel inverterMaxApparentPowerChannel = this.channel(SolarEdgeEss.ChannelId.INVERTER_MAX_APPARENT_POWER);
		var inverterMaxApparentPower = inverterMaxApparentPowerChannel.value();
		var inverterPowerLimit = this.getInverterPowerLimit();
		if(inverterMaxApparentPower != null && inverterPowerLimit != null) {
			this._setMaxApparentPower(Math.round(TypeUtils.multiply(inverterMaxApparentPower.get(), inverterPowerLimit.get(), 0.01f)));
		}
	}
	
	protected void updateDcDischargePowerChannel() {
		
		/*
		 * Fill DcDischargePower Channel
		 */
		var dcBatteryActualPower = this.getBattery1ActualPower().get();
		if(dcBatteryActualPower != null) {
			this._setDcDischargePower(dcBatteryActualPower * -1);
		}
	}

	protected void updateEnergyChannels() {

		/*
		 * Calculate AC Energy
		 */
		var acActivePower = this.getActivePower().get();
		if (acActivePower == null) {
			// Not available
			this.calculateAcChargeEnergy.update(null);
			this.calculateAcDischargeEnergy.update(null);
		} else if (acActivePower > 0) {
			// Discharge
			this.calculateAcChargeEnergy.update(0);
			this.calculateAcDischargeEnergy.update(acActivePower);
		} else {
			// Charge
			this.calculateAcChargeEnergy.update(acActivePower * -1);
			this.calculateAcDischargeEnergy.update(0);
		}
		
		/*
		 * Calculate DC Energy
		 */
		var dcDischargePower = this.getDcDischargePower().get();
		if (dcDischargePower == null) {
			// Not available
			this.calculateDcChargeEnergy.update(null);
			this.calculateDcDischargeEnergy.update(null);
		} else if (dcDischargePower > 0) {
			// Discharge
			this.calculateDcChargeEnergy.update(0);
			this.calculateDcDischargeEnergy.update(dcDischargePower);
		} else {
			// Charge
			this.calculateDcChargeEnergy.update(dcDischargePower * -1);
			this.calculateDcDischargeEnergy.update(0);
		}		
	}

	@Override
	public int getCycleTime() {
		return this.cycle != null ? this.cycle.getCycleTime() : DEFAULT_CYCLE_TIME;
	}
	
	// Sets the correct value for ACTIVE_POWER_L1, ACTIVE_POWER_L2 or ACTIVE_POWER_L3-Channel from ACTIVE_POWER
	public static void calculateSinglePhaseFromActivePower(SolarEdgeEssImpl solarEdge, Phase phase) {
		solarEdge.getActivePowerChannel().onSetNextValue(value -> {
			solarEdge.getActivePowerL1Channel().setNextValue(phase == Phase.L1||phase == Phase.ALL ? value : null); // Fallback to L1 on wrong configuration
			solarEdge.getActivePowerL2Channel().setNextValue(phase == Phase.L2 ? value : null);
			solarEdge.getActivePowerL3Channel().setNextValue(phase == Phase.L3 ? value : null);
		});
	}	
	
	// Calculate the ACTIVE_POWER_L1, ACTIVE_POWER_L2 and ACTIVE_POWER_L3-Channels from ACTIVE_POWER by dividing by three.
	public static void calculatePhasesFromActivePower(SolarEdgeEssImpl solarEdge) {
		solarEdge.getActivePowerChannel().onSetNextValue(value -> {
			var phase = TypeUtils.divide(value.get(), 3);
			solarEdge.getActivePowerL1Channel().setNextValue(phase);
			solarEdge.getActivePowerL2Channel().setNextValue(phase);
			solarEdge.getActivePowerL3Channel().setNextValue(phase);
		});
	}
	
	// Calculate the REACTIVE_POWER_L1, REACTIVE_POWER_L2 and REACTIVE_POWER_L3-Channels from REACTIVE_POWER by dividing by three.
	public static void calculatePhasesFromReactivePower(SolarEdgeEssImpl solarEdge) {
		solarEdge.getReactivePowerChannel().onSetNextValue(value -> {
			var phase = TypeUtils.divide(value.get(), 3);
			solarEdge.getReactivePowerL1Channel().setNextValue(phase);
			solarEdge.getReactivePowerL2Channel().setNextValue(phase);
			solarEdge.getReactivePowerL3Channel().setNextValue(phase);
		});
	}
	
}
