package io.openems.edge.solaredge.hybrid.ess;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

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
import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;

import io.openems.edge.common.channel.EnumReadChannel;

import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;

import io.openems.edge.controller.ess.limittotaldischarge.ControllerEssLimitTotalDischarge;
import io.openems.edge.controller.ess.emergencycapacityreserve.ControllerEssEmergencyCapacityReserve;

import io.openems.edge.controller.ess.chargedischargelimiter.ControllerEssChargeDischargeLimiter;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
//import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.ess.power.api.Power;

import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;
import io.openems.edge.pvinverter.sunspec.SunSpecPvInverter;
import io.openems.edge.solaredge.enums.ControlMode;
import io.openems.edge.solaredge.enums.PvMode;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;
import io.openems.edge.solaredge.enums.AcChargePolicy;
import io.openems.edge.solaredge.enums.ChargeDischargeMode;
import io.openems.edge.solaredge.charger.SolaredgeDcCharger;
import io.openems.edge.solaredge.common.AverageCalculator;


@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "SolarEdge.Hybrid.ESS", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE) //

@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS //
})

public class SolarEdgeHybridEssImpl extends AbstractSunSpecEss implements SolarEdgeHybridEss, ManagedSymmetricEss,
		SymmetricEss, HybridEss, ModbusComponent, OpenemsComponent, EventHandler, ModbusSlave, TimedataProvider {

	private static boolean DEBUG_MODE = false;
	private static final int READ_FROM_MODBUS_BLOCK = 1;
	// private final List<SolaredgeDcCharger> chargers = new ArrayList<>();
	private SolaredgeDcCharger charger;

	private final Logger log = LoggerFactory.getLogger(SolarEdgeHybridEss.class);

	// Hardware-Limits
	// ToDo: make configurable
	protected static final int HW_MAX_APPARENT_POWER = 10000;
	protected static final int HW_ALLOWED_CHARGE_POWER = -5000;
	protected static final int HW_ALLOWED_DISCHARGE_POWER = 5000;

	protected static final int HW_TOLERANCE = 500; // Tolerance in Watt before new charge power value is applied

	// AC-side
	// private final CalculateEnergyFromPower calculateAcChargeEnergyCalculated =
	// new CalculateEnergyFromPower(this,
	// SolarEdgeHybridEss.ChannelId.ACTIVE_CHARGE_ENERGY_CALCULATED);

	private final CalculateEnergyFromPower calculateAcChargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY);

	// Active discharge Energy comes from SunSpec-Register
	// private final CalculateEnergyFromPower calculateAcDischargeEnergy = new
	// CalculateEnergyFromPower(this,
	// SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY);

	// DC-side
	private final CalculateEnergyFromPower calculateDcChargeEnergy = new CalculateEnergyFromPower(this,
			HybridEss.ChannelId.DC_CHARGE_ENERGY);

	private final CalculateEnergyFromPower calculateDcDischargeEnergy = new CalculateEnergyFromPower(this,
			HybridEss.ChannelId.DC_DISCHARGE_ENERGY);

	// Calculate moving average over last x values - we use 5 here
	private AverageCalculator acPowerAverageCalculator = new AverageCalculator(5);
	private AverageCalculator activePowerWantedAverageCalculator = new AverageCalculator(5);
	private AverageCalculator pvProductionAverageCalculator = new AverageCalculator(5);
	private AverageCalculator feedToGridAverageCalculator = new AverageCalculator(5);

	private Config config;

	private int originalActivePowerWanted;

	@Reference
	private Power power;

	private int minSocPercentage;

	private static final Map<SunSpecModel, Priority> ACTIVE_MODELS = ImmutableMap.<SunSpecModel, Priority>builder()
			.put(DefaultSunSpecModel.S_1, Priority.LOW) //
			.put(DefaultSunSpecModel.S_103, Priority.LOW) //
			.put(DefaultSunSpecModel.S_160, Priority.LOW) //
			.put(DefaultSunSpecModel.S_203, Priority.LOW) //
			.put(DefaultSunSpecModel.S_802, Priority.LOW) //
			.build();
	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private volatile Timedata timedata = null;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(&(enabled=true)(isReserveSocEnabled=true))")
	private volatile List<ControllerEssEmergencyCapacityReserve> ctrlEmergencyCapacityReserves = new CopyOnWriteArrayList<>();

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(enabled=true)")
	private volatile List<ControllerEssLimitTotalDischarge> ctrlLimitTotalDischarges = new CopyOnWriteArrayList<>();

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(enabled=true)")
	private volatile List<ControllerEssChargeDischargeLimiter> ctrlChargeDischargeLimiters = new CopyOnWriteArrayList<>();

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private ElectricityMeter meter;

	private int maxSocPercentage;

	public SolarEdgeHybridEssImpl() throws OpenemsException {
		super(//
				ACTIVE_MODELS, //
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				HybridEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				SunSpecPvInverter.ChannelId.values(), //
				SolarEdgeHybridEss.ChannelId.values());

		if (DEBUG_MODE == false) {
			this.addStaticModbusTasks(this.getModbusProtocol());
		}

	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		try {
			if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
					"Modbus", config.modbus_id(), READ_FROM_MODBUS_BLOCK)) {
				return;
			}
			this.config = config;
			
			// update filter for 'Controllers'
			if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "Controllers", config.id())) {
				return;
			}			

			// update filter for 'meter'
			if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "meter", config.meter_id())) {
				return;
			}

			this.installListener();

		} catch (Exception e) {
			this.logError(this.log, "Error activating component: " + e.getMessage());
			throw new OpenemsException("Activation failed: " + e.getMessage());
		}

	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		// super.handleEvent(event);

		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			this._setMyActivePower();
			break;
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS:
			this._setMyActivePower();
			this.setLimits();
			break;
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:

			this._setMyActivePower();
			this.calculateEnergy();

			break;
		}
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public int getPowerPrecision() {
		//
		return 1;
	}

	@Override
	public boolean isManaged() {
		return true;

		// Just for Testing
		// return !this.config.readOnlyMode();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}


	// Update method to add a single charger
	@Override
	public void addCharger(SolaredgeDcCharger charger) {
		this.charger = charger;
	}

	// Update method to remove the single charger
	@Override
	public void removeCharger(SolaredgeDcCharger charger) {
		if (this.charger == charger) {
			this.charger = null;
		}
	}

	@Override
	public String getModbusBridgeId() {
		return this.config.modbus_id();
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Override
	public void applyPower(int activePowerTarget, int reactivePowerWanted) throws OpenemsNamedException {

		Integer surplusPower = this.getSurplusPower(); // is NULL if battery´s not fully charged yet

		this.logDebug(this.log, "ApplyPower Target: " + activePowerTarget + "W\n");
		if (surplusPower != null) {
			this.logDebug(this.log, "ESS 	Surplus Power: " + surplusPower + "W\n");
		}

		this.limitPvPower();

		/**
		 * version to limit according to configured limit // Get the grid power and ess
		 * power int gridPower = this.meter.getActivePower().getOrError(); // current
		 * buy-from/sell-to grid
		 * 
		 * // Checking if the grid power is above the maximum feed-in if (gridPower * -1
		 * > this.config.feedToGridPowerLimit()) {
		 * 
		 * // Calculate actual limit for Ess var essPowerLimit = gridPower +
		 * this.getActivePower().getOrError() + this.config.feedToGridPowerLimit();
		 * this.logDebug(this.log, "Ess power limit: " + essPowerLimit + "W");
		 * 
		 * // Apply limit int powerPerCharger = (int) Math.round(Math.abs(essPowerLimit)
		 * / this.chargers.size()); for (SolaredgeDcCharger charger : this.chargers) {
		 * charger._calculateAndSetPvPowerLimit(powerPerCharger);
		 * this.logDebug(this.log, "Limit per Charger Wanted: " + powerPerCharger +
		 * "W");
		 * 
		 * }
		 * 
		 * }
		 * 
		 * if (surplusPower != null && surplusPower > activePowerTarget) { // Apply
		 * limit int powerPerCharger = (int) Math.round(Math.abs(activePowerTarget) /
		 * this.chargers.size()); for (SolaredgeDcCharger charger : this.chargers) {
		 * powerPerCharger = 10000;
		 * charger._calculateAndSetPvPowerLimit(powerPerCharger);
		 * this.logDebug(this.log, "Limit per Charger Wanted: " + powerPerCharger +
		 * "W");
		 * 
		 * } }
		 **/

		this.applyChargePower(activePowerTarget);

	}

	protected void limitPvPower() {

		if (this.charger == null) {
			return; // Exit early if no chargers are connected
		}

		int tolerance = 500;
		// Safely fetch values and handle potential nulls
		Integer gridPower = this.meter.getActivePower().get(); // could be null. negative while feed to grid

		Integer maxPvProductionPowerLimit = this.config.maxPvProductionPowerLimit(); // could be null, Positive value

		Integer feedToGridPowerLimit = this.config.feedToGridPowerLimit(); // could be null, Positive value
		// Integer essActivePower = this.getActivePower().get(); // could be null
		Integer currentPvProductionPower = this.getPvProductionPower(); // always positive

		this.charger.getPvMode();

		if (currentPvProductionPower == null) {
			this.logDebug(this.log, "PV Power NULL or ");
			return;
		}

		int pvPowerSetPoint = currentPvProductionPower; // initial SetPoint

		// If limitation is active we have to control the limitation.
		// Reason: If feed to grid exceeds limit we have to decrease limitation, too.
		if (gridPower != null && feedToGridPowerLimit != null && -gridPower > feedToGridPowerLimit
				|| this.charger.getPvMode() == PvMode.LIMIT_ACTIVE) {
			feedToGridAverageCalculator.addValue(gridPower);

			int feedToGrid = feedToGridAverageCalculator.getAverage(); // negative value
			int pvProduction = pvProductionAverageCalculator.getAverage();

			pvPowerSetPoint = pvProduction + feedToGrid + feedToGridPowerLimit - tolerance;

			this.logDebug(this.log,
					String.format("PV Setpoint Adjustment: FeedToGridAvg: %d ProductionAvg: %d, , Adjusted: %d",
							feedToGrid, pvProduction, pvPowerSetPoint));

			pvPowerSetPoint = Math.min(pvPowerSetPoint, maxPvProductionPowerLimit);

		} else {
			// If grid power is positive or does not exceed the feed-to-grid limit, maximize
			// PV output
			pvPowerSetPoint = maxPvProductionPowerLimit;
		}

		// Log the calculated or default pv power set point
		this.logDebug(this.log, "final PV Power Setpoint: " + pvPowerSetPoint);

		// Distribute power to chargers if the setpoint is above a minimal operational
		// threshold
		if (pvPowerSetPoint > 1000) {
			distributePowerToCharger(pvPowerSetPoint);

		} else {
			this.logDebug(this.log, "PV power setpoint below operational threshold: " + pvPowerSetPoint);
		}

		this.logDebug(this.log, "Final PV Power Setpoint: " + pvPowerSetPoint);
	}

	// Method to distribute power directly to the single charger
	private void distributePowerToCharger(int pvPowerSetPoint) {
		if (charger != null) {
			charger._calculateAndSetPvPowerLimit(pvPowerSetPoint);
			this.logDebug(this.log, "<<<PV per Charger limit " + pvPowerSetPoint + "W>>>>>");
		} else {
			this.logDebug(this.log, "No charger available for setting power limit.");
		}
	}

	/**
	 * Applies charge power to the battery.
	 */
	public void applyChargePower(Integer chargePower) {
		if (this.config.readOnlyMode()) {
			switchToAutomaticMode();
			return;
		}

		this.originalActivePowerWanted = chargePower;
		this.activePowerWantedAverageCalculator.addValue(chargePower);
		chargePower = adjustChargePowerBasedOnAverage(chargePower);

		try {

			setChargeDischargeModes();
		} catch (OpenemsNamedException e) {

		}
		int maxDischargePower = determineMaxDischargeContinuesPower();
		int maxChargePower = determineMaxChargeContinuesPower();
		// Discharge-Power is hardware- or config-limit + PV-Production
		// Integer pvProductionPower = this.getPvProductionPower();

		if (DEBUG_MODE == true) {
			maxDischargePower = 1234;
			maxChargePower = 2345;
			// pvProductionPower = 456;

		}

		// if (pvProductionPower != null) {
		// maxDischargePower += pvProductionPower;
		// }

		this.logDebug(this.log, "Apply->ChargePower/MaxChargePower/MaxDischargePower " + chargePower + "W/"
				+ maxChargePower + "W/" + maxDischargePower + "W");
		applyDcPowerSettings(chargePower, maxChargePower, maxDischargePower);
	}

	private void setChargeDischargeModes() throws OpenemsNamedException {
		this.setControlMode(ControlMode.SE_CTRL_MODE_REMOTE); // enable device' remote control mode
		this.setAcChargePolicy(AcChargePolicy.SE_CHARGE_DISCHARGE_MODE_ALWAYS); // Allows charging/discharging on
																				// AC-side

		// If modes fail - go back to automatic mode after 60 seconds
		if (!isControlModeRemote() || !isStorageChargePolicyAlways()) {
			this.setChargeDischargeDefaultMode(ChargeDischargeMode.SE_CHARGE_POLICY_MAX_SELF_CONSUMPTION);
			this.setRemoteControlTimeout(60);

		}
	}

	/**
	 * Apply Power setting to DC-side
	 */
	private void applyDcPowerSettings(Integer chargePower, int maxChargePower, int maxDischargePower) {
		try {

			this.setChargePowerWanted(chargePower);
			this.channel(SolarEdgeHybridEss.ChannelId.CHARGE_POWER).setNextValue(chargePower);

			// setPowerModes(chargePower, maxChargePower, maxDischargePower);
			adjustChargePowerModes(chargePower, maxChargePower, maxDischargePower);
		} catch (OpenemsNamedException e) {
			this.logError(this.log, "DC ChargePower NOT set: " + e.getMessage());
		}
	}

	/**
	 * Different modes for charging / discharging have to be applied
	 */
	private void adjustChargePowerModes(Integer chargePower, Integer maxChargePower, Integer maxDischargePower)
			throws OpenemsNamedException {
		// chargePower =-1;
		if (chargePower < 0) {
			// Limit according to hardware or config values
			if (chargePower < (maxChargePower * -1)) {
				chargePower = maxChargePower * -1;
			}
			this.logDebug(this.log,
					"Apply Charge Mode->ChargePower/MaxChargePower " + chargePower + "W/" + maxChargePower * -1 + "W");
			applyChargeMode(chargePower);
		} else {
			if (chargePower > (maxDischargePower)) {
				chargePower = maxDischargePower;
			}
			this.logDebug(this.log, "Apply Discharge Mode->DishargePower/MaxDishargePower " + chargePower + "W/"
					+ maxDischargePower + "W");
			applyDischargeMode(chargePower);
		}
	}

	/**
	 * Applies target charge power: 1. Set the right Mode to SolarEdge 2. Apply
	 * target power. This is directly written to the device
	 */
	private void applyChargeMode(Integer chargePower) throws OpenemsNamedException {
		this.setRemoteControlCommandMode(ChargeDischargeMode.SE_CHARGE_POLICY_PV_AC);
		this.setMaxChargePower(Math.abs(chargePower));
		this.setMaxDischargePower(0);
	}

	/**
	 * Applies target Discharge power: 1. Set the right Mode to SolarEdge 2. Apply
	 * target power. This is directly written to the device
	 */
	private void applyDischargeMode(Integer chargePower) throws OpenemsNamedException {
		this.setRemoteControlCommandMode(ChargeDischargeMode.SE_CHARGE_POLICY_MAX_EXPORT);
		this.setMaxDischargePower(chargePower);
		this.setMaxChargePower(0);
	}

	/**
	 * Internal mode from SolarEdge
	 */
	private boolean isControlModeRemote() {

		EnumReadChannel controlModeChannel = this.channel(SolarEdgeHybridEss.ChannelId.CONTROL_MODE);
		ControlMode controlMode = controlModeChannel.value().asEnum();

		return controlMode == ControlMode.SE_CTRL_MODE_REMOTE;

	}

	/**
	 * We use a floating average out of the last 5 values to smooth operation.
	 * 
	 */
	private Integer adjustChargePowerBasedOnAverage(Integer chargePower) {
		if (Math.abs(this.activePowerWantedAverageCalculator.getAverage() - chargePower) > HW_TOLERANCE) {
			this.logDebug(this.log, "| Error On Average: Wanted " + chargePower + "/Avg: "
					+ this.activePowerWantedAverageCalculator.getAverage());
			return this.activePowerWantedAverageCalculator.getAverage();
		}
		return chargePower;
	}

	/**
	 * Internal mode from SolarEdge needed for AC coupling operation. Allows
	 * unlimited charging from the AC. When used with Maximize self-consumption,
	 * only excess power is used for charging (charging from the grid is not allowed
	 */
	private boolean isStorageChargePolicyAlways() {
		EnumReadChannel acChargePolicyChannel = this.channel(SolarEdgeHybridEss.ChannelId.AC_CHARGE_POLICY);
		AcChargePolicy acChargePolicy = acChargePolicyChannel.value().asEnum();

		return acChargePolicy == AcChargePolicy.SE_CHARGE_DISCHARGE_MODE_ALWAYS;
	}

	/**
	 * Comes from hardware. Positive value in Watts
	 */
	private int determineMaxDischargeContinuesPower() {
		int maxDischargeContinuesPower = getMaxDischargeContinuesPower().orElse(0);
		if (config.dischargePowerLimit() < maxDischargeContinuesPower) {
			return config.dischargePowerLimit();
		}
		return maxDischargeContinuesPower;
	}

	/**
	 * Comes from hardware. Positive value in Watts
	 */
	private int determineMaxChargeContinuesPower() {
		/*
		 * int maxChargeContinuesPower = getMaxChargeContinuesPower().orElse(0) * -1; if
		 * ((config.chargePowerLimit() * -1) > maxChargeContinuesPower) { return
		 * (config.chargePowerLimit() * -1); }
		 */

		int maxChargeContinuesPower = getMaxChargeContinuesPower().orElse(0);
		if ((config.chargePowerLimit()) < maxChargeContinuesPower) {
			return (config.chargePowerLimit());
		}
		return maxChargeContinuesPower;
	}

	private void switchToAutomaticMode() {
		try {
			this.setControlMode(ControlMode.SE_CTRL_MODE_MAX_SELF_CONSUMPTION);
		} catch (OpenemsNamedException e) {
			this.logError(this.log, "Cannot fall back to automatic mode " + e.getMessage());
		}
	}

	/**
	 * SolarEdge provides max. usable Capacity. So we can calculate current useable
	 * capacity
	 * 
	 * private void installListener() { this.getCapacityChannel().onUpdate(value ->
	 * { this.checkSocControllers(); Integer soc = this.getSoc().get(); int
	 * minSocPercentage = this.minSocPercentage;
	 * 
	 * if (soc == null) { return; }
	 * 
	 * if (soc < minSocPercentage) { this.setUseableCapacity(0); } else { soc = soc
	 * - minSocPercentage; // Normalize the soc to a 0-100% range over the range
	 * (100 - minSocPercentage) float normalizedSoc = (float) soc / (100 -
	 * minSocPercentage) * 100; // Calculate useable capacity based on normalized
	 * SoC int useableCapacity = (int) Math.round((float) value.get() *
	 * (normalizedSoc / 100f)); this.setUseableCapacity(useableCapacity);
	 * this.setUseableSoc((int) normalizedSoc); }
	 * 
	 * if (soc <= 0) { this.setUseableCapacity(0); this.setUseableSoc(0); } }); }
	 * 
	 * 
	 * private void checkSocControllers() {
	 * 
	 * int minSocTotalDischarge = 0; int actualReserveSoc = 0;
	 * 
	 * if (this.ctrlEmergencyCapacityReserves == null ||
	 * this.ctrlLimitTotalDischarges == null) { return; }
	 * 
	 * for (ControllerEssEmergencyCapacityReserve ctrlEmergencyCapacityReserve :
	 * this.ctrlEmergencyCapacityReserves) {
	 * 
	 * if (ctrlEmergencyCapacityReserve != null &&
	 * ctrlEmergencyCapacityReserve.channel("_PropertyEssId").value().asString().equals(this.id()))
	 * { actualReserveSoc =
	 * ctrlEmergencyCapacityReserve.getActualReserveSoc().orElse(0); } }
	 * 
	 * for (ControllerEssLimitTotalDischarge ctrlLimitTotalDischarge :
	 * this.ctrlLimitTotalDischarges) {
	 * 
	 * if (ctrlLimitTotalDischarge != null &&
	 * ctrlLimitTotalDischarge.channel("_PropertyEssId").value().asString() ==
	 * this.id()) { minSocTotalDischarge =
	 * ctrlLimitTotalDischarge.getMinSoc().orElse(0); } } // take highest value and
	 * return this.minSocPercentage = (Math.max(minSocTotalDischarge,
	 * actualReserveSoc)); }
	 */

	private void checkSocControllers() {
		Utils.filterControllersByEssId(ctrlChargeDischargeLimiters, this.id()); // filter out controllers not referring to this ess

		// Method always gives valid values. Even 100 and 0 if no controllers are active
		int[] socRange = Utils.getEssUsableSocRange(this.ctrlChargeDischargeLimiters, this.ctrlLimitTotalDischarges,
				this.ctrlEmergencyCapacityReserves);

		this.minSocPercentage = socRange[0];
		this.maxSocPercentage = socRange[1];
		this.logDebug(this.log, "checkSocControllers: MinSoC set to " + this.minSocPercentage + ", MaxSoC set to "
				+ this.maxSocPercentage);
	}

	/**
	 * value is the total capacity of the battery in Watt Hours
	 
	private void installListener() {
	    this.getCapacityChannel().onUpdate(value -> {
	        this.logDebug(this.log, "Listener triggered with capacity value: " + value);

	        // Get the current SoC value and check for null or invalid values
	        Integer soc = this.getSoc().orElse(null);
	        if (soc == null || soc <= 0 || value == null || value.get() <= 0) {
	            this.logDebug(this.log, "SoC is null, zero, or negative, or capacity value is null/invalid; exiting listener.");
	            this.setUseableCapacity(0);
	            this.setUseableSoc(0);
	            return;
	        }

	        // Update minSocPercentage and maxSocPercentage based on current controller settings
	        this.checkSocControllers();
	        this.logDebug(this.log, "Updated SoC ranges -> MinSoC: " + this.minSocPercentage + " / MaxSoC: " + this.maxSocPercentage);

	        // Calculate the SoC range and check that it's valid (non-zero and positive)
	        int range = this.maxSocPercentage - this.minSocPercentage;
	        if (range <= 0) {
	            this.logError(this.log, "Invalid SoC range: MinSoC is greater than or equal to MaxSoC. Exiting listener.");
	            this.setUseableCapacity(0);
	            this.setUseableSoc(0);
	            return;
	        }

	        // Calculate total capacity based on current SoC and loaded capacity
	        int totalCapacity = value.get();

	        // Normalization of SoC: scale the SoC within the min/max range to a 0-100 scale
	        int useableSoc = soc > this.maxSocPercentage ? 100  // If SoC exceeds maxSoc, set to 100%
	                : soc < this.minSocPercentage ? 0            // If SoC is below minSoc, set to 0%
	                : (int) (((double) (soc - this.minSocPercentage) / range) * 100);  // Normalize SoC to a 0-100 range

	        useableSoc = Math.max(0, Math.min(useableSoc, 100));  // Ensure usable SoC is between 0 and 100%

	        // Use the normalized SoC to calculate the usable capacity
	        int useableCapacity = (int) Math.round(totalCapacity * (useableSoc / 100f));

	        this.logDebug(this.log, "installListener: SoC: real|usable " + soc + "|" + useableSoc + 
	                    "[%] Capacity real|usable " + totalCapacity + "|" + useableCapacity + " [Wh]");

	        // Set usable capacity and SoC
	        this.setUseableCapacity(useableCapacity);
	        this.setUseableSoc(useableSoc);
	    });
	}
	*/
	
	
	/**
	 * value is the total capacity of the battery in Watt Hours
	 */
	private void installListener() {
	    this.getCapacityChannel().onUpdate(value -> {
	        this.logDebug(this.log, "Listener triggered with capacity value: " + value);

	        // Get the current SoC value and check for null or invalid values
	        Integer soc = this.getSoc().orElse(null);
	        if (soc == null || soc <= 0 || value == null || value.get() <= 0) {
	            this.logDebug(this.log, "SoC is null, zero, or negative, or capacity value is null/invalid; exiting listener.");
	            this.setUseableCapacity(0);
	            this.setUseableSoc(0);
	            return;
	        }

	        // Update minSocPercentage and maxSocPercentage based on current controller settings
	        this.checkSocControllers();
	        this.logDebug(this.log, "Updated SoC ranges -> MinSoC: " + this.minSocPercentage + " / MaxSoC: " + this.maxSocPercentage);

	        // Calculate the SoC range and check that it's valid (non-zero and positive)
	        int socRange = this.maxSocPercentage - this.minSocPercentage;
	        if (socRange <= 0) {
	            this.logError(this.log, "Invalid SoC range: MinSoC is greater than or equal to MaxSoC. Exiting listener.");
	            this.setUseableCapacity(0);
	            this.setUseableSoc(0);
	            return;
	        }

	        // Calculate total capacity in watt-hours
	        int totalCapacity = value.get();

	        // Calculate the usable SoC within the MinSoC and MaxSoC limits
	        int useableSoc = soc > this.maxSocPercentage ? 100  // If SoC exceeds maxSoc, set to 100%
	                : soc < this.minSocPercentage ? 0            // If SoC is below minSoc, set to 0%
	                : (int) (((double) (soc - this.minSocPercentage) / socRange) * 100);  // Normalize SoC to a 0-100 range

	        useableSoc = Math.max(0, Math.min(useableSoc, 100));  // Ensure usable SoC is between 0 and 100%

	        // Calculate the usable capacity in watt-hours based on the usable SoC
	        // First, calculate the percentage of capacity that can be used between MinSoC and MaxSoC
	        int useableCapacityRange = (int) Math.round(totalCapacity * (socRange / 100.0)); // Usable capacity within min and max SoC range

	        // Now calculate the current usable capacity based on the usable SoC
	        int useableCapacity = (int) Math.round(useableCapacityRange * (useableSoc / 100.0));

	        this.logDebug(this.log, "installListener: SoC: real|usable " + soc + "|" + useableSoc + 
	                    "[%] Capacity real|usable " + totalCapacity + "|" + useableCapacity + " [Wh]");

	        // Set usable capacity and SoC
	        this.setUseableCapacity(useableCapacity);
	        this.setUseableSoc(useableSoc);
	    });
	}



	public void _setMyActivePower() {

		// ActivePower is the actual AC output including battery discharging
		int acPower = this.getAcPower().orElse(0);
		int acPowerScale = this.getAcPowerScale().orElse(0);
		double acPowerValue = acPower * Math.pow(10, acPowerScale);

		int dcPower = this.getDcPower().orElse(0);
		int dcPowerScale = this.getDcPowerScale().orElse(0);
		double dcPowerValue = dcPower * Math.pow(10, dcPowerScale);

		// The problem is that ac-power never gets negative (e.g. when charging battery
		// from grid)
		// so AC-Power can´t be used for further calculation.
		// We set DC-Power if: AC-Power is 0 AND DC-Power is negative
		// Yes, this look weird
		if (acPowerValue == 0 && dcPowerValue < 0) {
			acPowerValue = dcPowerValue;
		}

		if (DEBUG_MODE == true) {
			acPowerValue = 333;
			this._setActivePower((int) acPowerValue);
			this._setReactivePower(123);
			this._setMaxApparentPower(HW_MAX_APPARENT_POWER);
			this._setDcDischargePower(666);

		}

		acPowerAverageCalculator.addValue((int) acPowerValue);
		// Experimental!
		// to avoid scaling effects only values are valid that do not differ more than
		// 1000W
		if (Math.abs(acPowerAverageCalculator.getAverage() - acPowerValue) < 1000) {
			this._setActivePower((int) acPowerValue);
		}

	}

	/**
	 * Adds static modbus tasks.
	 * 
	 * @param protocol the {@link ModbusProtocol}
	 * @throws OpenemsException on error
	 */
	private void addStaticModbusTasks(ModbusProtocol protocol) throws OpenemsException {

		protocol.addTask(//
				new FC3ReadRegistersTask(0x9C93, Priority.HIGH, //
						//
						m(SolarEdgeHybridEss.ChannelId.AC_POWER, //
								new SignedWordElement(0x9C93)),
						m(SolarEdgeHybridEss.ChannelId.AC_POWER_SCALE, //
								new SignedWordElement(0x9C94))));

		protocol.addTask(//
				new FC3ReadRegistersTask(0x9CA4, Priority.HIGH, //

						m(SolarEdgeHybridEss.ChannelId.DC_POWER, //
								new SignedWordElement(0x9CA4)),
						m(SolarEdgeHybridEss.ChannelId.DC_POWER_SCALE, //
								new SignedWordElement(0x9CA5))));

		protocol.addTask(//
				// new FC3ReadRegistersTask(0xE142, Priority.LOW, //
				new FC3ReadRegistersTask(0xE144, Priority.LOW, //

						// m(HybridEss.ChannelId.DC_DISCHARGE_ENERGY, //
						// new FloatDoublewordElement(0xE142).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeHybridEss.ChannelId.MAX_CHARGE_CONTINUES_POWER, //
								new FloatDoublewordElement(0xE144).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeHybridEss.ChannelId.MAX_DISCHARGE_CONTINUES_POWER, //
								new FloatDoublewordElement(0xE146).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeHybridEss.ChannelId.MAX_CHARGE_PEAK_POWER, //
								new FloatDoublewordElement(0xE148).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeHybridEss.ChannelId.MAX_DISCHARGE_PEAK_POWER, //
								new FloatDoublewordElement(0xE14A).wordOrder(WordOrder.LSWMSW)),

						new DummyRegisterElement(0xE14C, 0xE16B), // Reserved
						m(SolarEdgeHybridEss.ChannelId.BATT_AVG_TEMPERATURE, //
								new FloatDoublewordElement(0xE16C).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeHybridEss.ChannelId.BATT_MAX_TEMPERATURE, //
								new FloatDoublewordElement(0xE16E).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeHybridEss.ChannelId.BATT_ACTUAL_VOLTAGE, //
								new FloatDoublewordElement(0xE170).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeHybridEss.ChannelId.BATT_ACTUAL_CURRENT, //
								new FloatDoublewordElement(0xE172).wordOrder(WordOrder.LSWMSW)),
						m(HybridEss.ChannelId.DC_DISCHARGE_POWER, // Instantaneous power to or from battery
								new FloatDoublewordElement(0xE174).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.INVERT),

						// Active Charge / Discharge energy are only valid until the next day/loading
						// cycle (not clear or verified)
						m(SolarEdgeHybridEss.ChannelId.BATT_LIFETIME_EXPORT_ENERGY, //
								new UnsignedQuadruplewordElement(0xE176).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeHybridEss.ChannelId.BATT_LIFETIME_IMPORT_ENERGY, //
								new UnsignedQuadruplewordElement(0xE17A).wordOrder(WordOrder.LSWMSW))

				));

		protocol.addTask(//
				new FC3ReadRegistersTask(0xE17E, Priority.LOW, //
						m(SolarEdgeHybridEss.ChannelId.BATT_MAX_CAPACITY, //
								new FloatDoublewordElement(0xE17E).wordOrder(WordOrder.LSWMSW)),
						m(SymmetricEss.ChannelId.CAPACITY, // Available capacity or "real" capacity
								new FloatDoublewordElement(0xE180).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeHybridEss.ChannelId.SOH, //
								new FloatDoublewordElement(0xE182).wordOrder(WordOrder.LSWMSW)),
						m(SymmetricEss.ChannelId.SOC, //
								new FloatDoublewordElement(0xE184).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeHybridEss.ChannelId.BATT_STATUS, //
								new UnsignedDoublewordElement(0xE186).wordOrder(WordOrder.LSWMSW))));

		protocol.addTask(//
				new FC3ReadRegistersTask(0xE004, Priority.HIGH, //
						m(SolarEdgeHybridEss.ChannelId.CONTROL_MODE, new UnsignedWordElement(0xE004)),
						m(SolarEdgeHybridEss.ChannelId.AC_CHARGE_POLICY, new UnsignedWordElement(0xE005)),
						m(SolarEdgeHybridEss.ChannelId.MAX_CHARGE_LIMIT,
								new FloatDoublewordElement(0xE006).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeHybridEss.ChannelId.STORAGE_BACKUP_LIMIT,
								new FloatDoublewordElement(0xE008).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeHybridEss.ChannelId.CHARGE_DISCHARGE_DEFAULT_MODE, new UnsignedWordElement(0xE00A)),
						m(SolarEdgeHybridEss.ChannelId.REMOTE_CONTROL_TIMEOUT,
								new UnsignedDoublewordElement(0xE00B).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeHybridEss.ChannelId.REMOTE_CONTROL_COMMAND_MODE, new UnsignedWordElement(0xE00D)),
						m(SolarEdgeHybridEss.ChannelId.MAX_CHARGE_POWER,
								new FloatDoublewordElement(0xE00E).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeHybridEss.ChannelId.MAX_DISCHARGE_POWER,
								new FloatDoublewordElement(0xE010).wordOrder(WordOrder.LSWMSW))));

		protocol.addTask(//
				new FC16WriteRegistersTask(0xE004,
						m(SolarEdgeHybridEss.ChannelId.SET_CONTROL_MODE, new SignedWordElement(0xE004)),
						m(SolarEdgeHybridEss.ChannelId.SET_AC_CHARGE_POLICY, new SignedWordElement(0xE005)), // Max.
																												// charge
																												// power.
																												// Negative
																												// values
						m(SolarEdgeHybridEss.ChannelId.SET_MAX_CHARGE_LIMIT,
								new FloatDoublewordElement(0xE006).wordOrder(WordOrder.LSWMSW)), // kWh or percent
						m(SolarEdgeHybridEss.ChannelId.SET_STORAGE_BACKUP_LIMIT,
								new FloatDoublewordElement(0xE008).wordOrder(WordOrder.LSWMSW)), // Percent of capacity
						m(SolarEdgeHybridEss.ChannelId.SET_CHARGE_DISCHARGE_DEFAULT_MODE,
								new UnsignedWordElement(0xE00A)), // Usually set to 1 (Charge PV excess only)
						m(SolarEdgeHybridEss.ChannelId.SET_REMOTE_CONTROL_TIMEOUT,
								new UnsignedDoublewordElement(0xE00B).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeHybridEss.ChannelId.SET_REMOTE_CONTROL_COMMAND_MODE,
								new UnsignedWordElement(0xE00D)),
						m(SolarEdgeHybridEss.ChannelId.SET_MAX_CHARGE_POWER,
								new FloatDoublewordElement(0xE00E).wordOrder(WordOrder.LSWMSW)), // Max. charge power.
																									// Negative values
						m(SolarEdgeHybridEss.ChannelId.SET_MAX_DISCHARGE_POWER,
								new FloatDoublewordElement(0xE010).wordOrder(WordOrder.LSWMSW)) // Max. discharge power.
																								// Positive values
				));
	}

	@Override
	public String debugLog() {
		if (config.debugMode()) {
			return "SoC:" + this.getSoc().asString() //
					+ "|L:" + this.getActivePower().asString() //
					+ "|ChargeEnergy:"
					+ this.channel(SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY).value().asStringWithoutUnit()
					+ "|DisChargeEnergy:"
					+ this.channel(SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY).value().asStringWithoutUnit()

					+ "|DcDisChargePower:"
					+ this.channel(HybridEss.ChannelId.DC_DISCHARGE_POWER).value().asStringWithoutUnit()
					+ "|DcDisChargeEnergy:"
					+ this.channel(HybridEss.ChannelId.DC_DISCHARGE_ENERGY).value().asStringWithoutUnit()
					+ "|DcChargeEnergy:"
					+ this.channel(HybridEss.ChannelId.DC_CHARGE_ENERGY).value().asStringWithoutUnit()

					+ ";" + "|ControlMode "
					+ this.channel(SolarEdgeHybridEss.ChannelId.CONTROL_MODE).value().asStringWithoutUnit() //
					+ "|ChargePolicy "
					+ this.channel(SolarEdgeHybridEss.ChannelId.AC_CHARGE_POLICY).value().asStringWithoutUnit() //
					+ "|DefaultMode "
					+ this.channel(SolarEdgeHybridEss.ChannelId.CHARGE_DISCHARGE_DEFAULT_MODE).value()
							.asStringWithoutUnit() //
					+ "|RemoteControlMode "
					+ this.channel(SolarEdgeHybridEss.ChannelId.REMOTE_CONTROL_COMMAND_MODE).value()
							.asStringWithoutUnit() //
					+ "\n|ChargePowerWantedAvg " + this.activePowerWantedAverageCalculator.getAverage()
					+ "|ChargePowerWantedOriginal " + this.originalActivePowerWanted + "|ChargePowerWanted "
					+ this.getChargePowerWanted().asString()

					+ "|Allowed Charge / Discharge Power " + this.getAllowedChargePower().asString() + " / "
					+ this.getAllowedDischargePower().asString()

					+ "\n|ChargePower "
					+ this.channel(SolarEdgeHybridEss.ChannelId.MAX_CHARGE_POWER).value().asStringWithoutUnit() //
					+ "|DischargePower "
					+ this.channel(SolarEdgeHybridEss.ChannelId.MAX_DISCHARGE_POWER).value().asStringWithoutUnit() //
					+ "|CommandTimeout "
					+ this.channel(SolarEdgeHybridEss.ChannelId.REMOTE_CONTROL_TIMEOUT).value().asStringWithoutUnit() //

					+ "|" + this.getGridModeChannel().value().asOptionString() //
					+ "|Feed-In:";
		} else
			return "SoC:" + this.getSoc().asString() //
					+ "|L:" + this.getActivePower().asString();
	}

	/**
	 * Uses Info Log for further debug features.
	 */
	@Override
	protected void logDebug(Logger log, String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
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

	public void handleSurplusPower() {
		// ToDo: Handle Power limitations from controllers
	}

	@Override
	public Integer getSurplusPower() {
		// TODO logic is insufficient
		if (this.getSoc().orElse(0) < 98) { // SolarEdge holds on 98% a long time
			return null;
		}
		var productionPower = this.getPvProductionPower();
		if (productionPower == null || productionPower < 100) {
			return null;
		}
		return productionPower + 200 /* discharge more than PV production to avoid PV curtail */;
	}

	/**
	 * Gets PV production power from charger.
	 * 
	 * @return PV production power in Watts
	 */
	public Integer getPvProductionPower() {
		if (charger == null) {
			return null;
		}
		if (charger.getActualPower().get() == null) {
			return null;
		}
		int pvProductionPower = charger.getActualPower().get();
		pvProductionAverageCalculator.addValue(pvProductionPower);
		return pvProductionPower;

	}

	@Override
	protected void onSunSpecInitializationCompleted() {
		// TODO Add mappings for registers from S1 and S103
		if (DEBUG_MODE == false) {

			this.mapFirstPointToChannel(//
					ManagedSymmetricPvInverter.ChannelId.MAX_APPARENT_POWER, //
					ElementToChannelConverter.DIRECT_1_TO_1, //
					DefaultSunSpecModel.S120.W_RTG);

			// AC-Output from the Inverter. Could be the combination from PV + battery
			/*
			 * this.mapFirstPointToChannel(// SymmetricEss.ChannelId.ACTIVE_POWER, //
			 * ElementToChannelConverter.DIRECT_1_TO_1, // DefaultSunSpecModel.S103.W);
			 */

			this.mapFirstPointToChannel(//
					SolarEdgeHybridEss.ChannelId.APPARENT_POWER, //
					ElementToChannelConverter.DIRECT_1_TO_1, //
					DefaultSunSpecModel.S103.VA);

			this.mapFirstPointToChannel(//
					SymmetricEss.ChannelId.REACTIVE_POWER, //
					ElementToChannelConverter.DIRECT_1_TO_1, //
					DefaultSunSpecModel.S103.V_AR);

			this.mapFirstPointToChannel(//
					SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY, //
					ElementToChannelConverter.DIRECT_1_TO_1, //
					DefaultSunSpecModel.S103.WH); // 103.WH holds value for lifetime production (battery + pv).
													// Remember:
													// battery can also be loaded from AC/grid

			this.mapFirstPointToChannel(//
					SolarEdgeHybridEss.ChannelId.VOLTAGE_L1, //
					ElementToChannelConverter.DIRECT_1_TO_1, //
					DefaultSunSpecModel.S103.PH_VPH_A);

			this.mapFirstPointToChannel(//
					SolarEdgeHybridEss.ChannelId.VOLTAGE_L2, //
					ElementToChannelConverter.DIRECT_1_TO_1, //
					DefaultSunSpecModel.S103.PH_VPH_B);

			this.mapFirstPointToChannel(//
					SolarEdgeHybridEss.ChannelId.VOLTAGE_L3, //
					ElementToChannelConverter.DIRECT_1_TO_1, //
					DefaultSunSpecModel.S103.PH_VPH_C);

			this.mapFirstPointToChannel(//
					SolarEdgeHybridEss.ChannelId.CURRENT_L1, //
					ElementToChannelConverter.DIRECT_1_TO_1, //
					DefaultSunSpecModel.S103.APH_A);

			this.mapFirstPointToChannel(//
					SolarEdgeHybridEss.ChannelId.CURRENT_L2, //
					ElementToChannelConverter.DIRECT_1_TO_1, //
					DefaultSunSpecModel.S103.APH_B);

			this.mapFirstPointToChannel(//
					SolarEdgeHybridEss.ChannelId.CURRENT_L3, //
					ElementToChannelConverter.DIRECT_1_TO_1, //
					DefaultSunSpecModel.S103.APH_C);

			this.setLimits();

		}

	}

	/**
	 * Calculate the Energy values from DcDischargePower. This should be the
	 * charging power from or to battery.
	 * 
	 * negative values for Charge; positive for Discharge
	 */
	private void calculateEnergy() {
		// Calculate Energy

		// Actual Power DC from or to battery
		var activeDcPower = this.getDcDischargePower().get(); // Instantaneous power to or from battery

		if (activeDcPower == null) {
			// Not available
			this.calculateDcChargeEnergy.update(null);
			this.calculateDcDischargeEnergy.update(null);
		} else if (activeDcPower > 0) {
			// DisCharging Battery
			this.calculateDcChargeEnergy.update(0);
			this.calculateDcDischargeEnergy.update(activeDcPower);
		} else if (activeDcPower < 0) {
			// Charging Battery
			this.calculateDcChargeEnergy.update(activeDcPower * -1);
			this.calculateDcDischargeEnergy.update(0);
		} else { // UNDEFINED??
			this.calculateDcChargeEnergy.update(null);
			this.calculateDcDischargeEnergy.update(null);
		}

		/*
		 * Calculate AC Energy AC Energy out/in the ESS including: - AC-Production (from
		 * PV) - Battery Discharging (producing AC) - Sell to grid - Consumption
		 * 
		 * DC-Output, i.e. Charging the battery is NOT included
		 */
		var activeAcPower = this.getActivePower().get(); // AC-Power never gets negative. So it´s AC power out of the
															// ESS. Actually we don´t need the following calculation
		if (activeAcPower == null) {
			// Not available
			this.calculateAcChargeEnergy.update(null);
			// this.calculateAcDischargeEnergy.update(null); // Mapped to SunSpec register
			// 103Wh.Energy leaving the hybrid-system: battery & PV
		} else if (activeAcPower > 0) {
			// Discharge
			this.calculateAcChargeEnergy.update(0);
			// this.calculateAcDischargeEnergy.update(activeAcPower);
		} else if (activeAcPower < 0) {
			// Charge
			this.calculateAcChargeEnergy.update(activeAcPower * -1);
			// this.calculateAcDischargeEnergy.update(activeAcPower);
		} else {
			// Charge
			this.calculateAcChargeEnergy.update(0);
			// this.calculateAcDischargeEnergy.update(0);
		}

	}

	private void setLimits() {
		int maxChargeContinuesPower = determineMaxChargeContinuesPower(); // Hardware or configured limit
		int maxDischargeContinuesPower = determineMaxDischargeContinuesPower(); // Hardware or configured limit

		// PV-Production
		var pvProduction = Math.max(//
				TypeUtils.orElse(//
						TypeUtils.subtract(this.getActivePower().get(), this.getDcDischargePower().get()), //
						0),
				0);

		int maxChargePower = maxChargeContinuesPower * -1;
		int maxDischargePower = maxDischargeContinuesPower + pvProduction;

		this.logDebug(this.log, "SetLimits->MaxChargePower/MaxDischargePower " + maxChargeContinuesPower + "/"
				+ maxChargePower + "/" + maxDischargePower + "W");

		// Apply AllowedChargePower and AllowedDischargePower
		this._setAllowedChargePower(maxChargePower/* inverted charge power */);
		this._setAllowedDischargePower(maxDischargePower);

		this._setMaxApparentPower(HW_MAX_APPARENT_POWER);
	}

}
