package io.openems.edge.batteryinverter.victron.ess.symmetric;

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
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.battery.victron.VictronBattery;
import io.openems.edge.batteryinverter.victron.ro.VictronBatteryInverter;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.Phase.SinglePhase;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSinglePhaseEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SinglePhaseEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;
import io.openems.edge.victron.enums.AllowDisallow;
import io.openems.edge.victron.enums.EnableDisable;
import io.openems.edge.victron.enums.SymmetricAsymmetricMode;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "ESS.Victron", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS //
})
public class VictronEssImpl extends AbstractOpenemsModbusComponent implements VictronEss, ManagedSinglePhaseEss,
		SinglePhaseEss, ManagedAsymmetricEss, AsymmetricEss, ManagedSymmetricEss, SymmetricEss, ModbusComponent,
		ModbusSlave, EventHandler, OpenemsComponent, TimedataProvider {

	@Reference
	private Power power;

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private volatile Timedata timedata = null;

	@Reference
	protected ComponentManager componentManager;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	// @Reference(policy = ReferencePolicy.DYNAMIC, policyOption =
	// ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
	private volatile VictronBatteryInverter batteryInverter;

	// @Reference(policy = ReferencePolicy.DYNAMIC, policyOption =
	// ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
	private volatile VictronBattery battery;

	private final Logger log = LoggerFactory.getLogger(VictronEssImpl.class);

	// private volatile VictronBatteryInverter batteryInverter;
	// private volatile VictronBattery battery;

	private Config config;
	public SinglePhase singlePhase = null;

	private Integer MaxChargePower = null;
	private Integer MaxDischargePower = null;

	// AC-side
	private final CalculateEnergyFromPower calculateDischargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY);

	private final CalculateEnergyFromPower calculateChargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY);

	// DC-side
	/**
	 * We take internal meter values private final CalculateEnergyFromPower
	 * calculateDcDischargeEnergy = new CalculateEnergyFromPower(this,
	 * VictronEss.ChannelId.DC_DISCHARGE_ENERGY);
	 * 
	 * private final CalculateEnergyFromPower calculateDcChargeEnergy = new
	 * CalculateEnergyFromPower(this, VictronEss.ChannelId.DC_CHARGE_ENERGY);
	 */

	public VictronEssImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //

				AsymmetricEss.ChannelId.values(), //
				ManagedAsymmetricEss.ChannelId.values(), //
				SinglePhaseEss.ChannelId.values(), //
				ManagedSinglePhaseEss.ChannelId.values(), //

				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				VictronBatteryInverter.ChannelId.values(), //
				VictronBattery.ChannelId.values(), //
				VictronEss.ChannelId.values());
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {

		this.config = config;

		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
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

		// symmetric mode is only valid in 3p systems
		if (config.symmetricAsymmetricMode() == SymmetricAsymmetricMode.SYMMETRIC) {
			this.singlePhase = null;
		}

		if (this.singlePhase != null) {
			SinglePhaseEss.initializeCopyPhaseChannel(this, this.singlePhase);
			AsymmetricEss.initializePowerSumChannels(this);			
		}
		this._setGridMode(GridMode.ON_GRID);

		if (this.batteryInverter == null) {
			this.logError(this.log, "ESS->BatteryInverter not yet activated ");
			return;
		}

		if (this.battery == null) {
			this.logError(this.log, "ESS->Battery not yet activated ");
			return;
		}


	}

	@Override
	@Deactivate
	protected void deactivate() {
		this.unsetBattery(battery);
		this.unsetBatteryInverter(batteryInverter);
		super.deactivate();
	}

	@Override
	public int getPowerPrecision() {
		return 100;
	}

	private void pushAsymmetricLimitsToPower() throws OpenemsNamedException {
		if (this.batteryInverter == null)
			return;
		if (!this.batteryInverter.calculateHardwareLimits())
			return;

		Integer maxChargePower = this.batteryInverter.getMaxChargePower(); // positive
		Integer maxDischargePower = this.batteryInverter.getMaxDischargePower(); // positive
		if (maxChargePower == null || maxDischargePower == null)
			return;

		// check if null -> 3p. Cannot be done in switch-statement
		if (this.singlePhase == null) { // 3p system
			int perPhaseCharge = Math.max(0, maxChargePower / 3);
			int perPhaseDischarge = Math.max(0, maxDischargePower / 3);

			this.setSetActivePowerL1GreaterOrEquals(perPhaseCharge * -1);
			this.setSetActivePowerL1LessOrEquals(perPhaseDischarge);

			this.setSetActivePowerL2GreaterOrEquals(perPhaseCharge * -1);
			this.setSetActivePowerL2LessOrEquals(perPhaseDischarge);

			this.setSetActivePowerL3GreaterOrEquals(perPhaseCharge * -1);
			this.setSetActivePowerL3LessOrEquals(perPhaseDischarge);
		} else {

			//
			switch (this.singlePhase) { // 1p

			case SinglePhase.L1:
				this.setSetActivePowerL1GreaterOrEquals(maxChargePower * -1);
				this.setSetActivePowerL1LessOrEquals(maxDischargePower);

				this.setSetActivePowerL2GreaterOrEquals(0);
				this.setSetActivePowerL2LessOrEquals(0);

				this.setSetActivePowerL3GreaterOrEquals(0);
				this.setSetActivePowerL3LessOrEquals(0);
				break;
			case SinglePhase.L2:
				this.setSetActivePowerL1GreaterOrEquals(0);
				this.setSetActivePowerL1LessOrEquals(0);

				this.setSetActivePowerL2GreaterOrEquals(maxChargePower * -1);
				this.setSetActivePowerL2LessOrEquals(maxDischargePower);

				this.setSetActivePowerL3GreaterOrEquals(0);
				this.setSetActivePowerL3LessOrEquals(0);
				break;
			case SinglePhase.L3:
				this.setSetActivePowerL1GreaterOrEquals(0);
				this.setSetActivePowerL1LessOrEquals(0);

				this.setSetActivePowerL2GreaterOrEquals(0);
				this.setSetActivePowerL2LessOrEquals(0);

				this.setSetActivePowerL3GreaterOrEquals(maxChargePower * -1);
				this.setSetActivePowerL3LessOrEquals(maxDischargePower);
				break;

			}
		}

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
	public String debugLog() {
		return "SoC:" + this.getSoc().asString() //
				+ "|L:" + this.getActivePower().asString() + "/" + this.getReactivePower().asString() + "|Phase:"
				+ this.config.phase() + "|Allowed:" + this.getAllowedChargePower().asStringWithoutUnit() + ";" //
				+ this.getAllowedDischargePower().asString() //

				+ "\n" + "|" + this.getGridModeChannel().value().asOptionString();
	}

	// Asymmetric systems
	@Override
	public void applyPower(int activePowerTargetL1, int reactivePowerTargetL1, int activePowerTargetL2,
			int reactivePowerTargetL2, int activePowerTargetL3, int reactivePowerTargetL3)
			throws OpenemsNamedException {

		this.logDebug(this.log, "Asymm. PowerWanted L1: " + activePowerTargetL1 + "|L2: " + activePowerTargetL2
				+ "|L3: " + activePowerTargetL3);

		if (this.batteryInverter == null) {
			this.logDebug(this.log,
					"Power is not applied as BatteryInverter is not connected. Use Victron BatteryInverter");
			return;
		}

		if (this.battery == null) {
			// battery = this.batteryInverter.getBattery();
			this.logError(this.log, "ApplyPower->Battery not activated ");
			return;
		}

		if (!this.batteryInverter.calculateHardwareLimits()) {
			return;
		}

		if (this.batteryInverter.getMaxApparentPower().get() == null
				|| this.batteryInverter.getMaxApparentPower().get() == 0) {
			this.logError(this.log, "ApplyPower->Max. Apparent Power invalid");
			return;
		}

		this.logDebug(this.log, "Setting max. apparent power to batteryInverter-Channel");
		this._setMaxApparentPower(this.batteryInverter.getMaxApparentPower().get().intValue());

		// Victron: Negative values for Discharge
		// OpenEMS: Negative values for Charge

		// if we are in symmetric mode we have to device the wanted power by 3
		// In single phase

		MaxChargePower = this.batteryInverter.getMaxChargePower();
		MaxDischargePower = this.batteryInverter.getMaxDischargePower();

		this.logDebug(this.log,
				"Getting max. Charge/Discharge power values: " + MaxChargePower + "/" + MaxDischargePower + "W");

		if (MaxChargePower == null || MaxDischargePower == null) {
			this.logError(this.log, "power Limits not set.");
			return;
		}

		if (this.hasFaults() || (this.getVEBusBMSAllowBatteryCharge() != AllowDisallow.ALLOWED)
				|| (this.getVEBusBMSAllowBatteryDischarge() != AllowDisallow.ALLOWED)) {
			this.logDebug(this.log, "System is not ready. Values will not be applied");
			return;
		}

		this.logDebug(this.log,
				"Setting max. Charge/Discharge power values: " + MaxChargePower + "/" + MaxDischargePower + "W");

		this._setAllowedChargePower(MaxChargePower * -1); // Internal channels. No effect on asymmetric systems
		this._setAllowedDischargePower(MaxDischargePower);

		this.logDebug(this.log,
				"OpenEMS Apply Power L1: " + activePowerTargetL1 + "|L2: " + activePowerTargetL2 + "|L3: "
						+ activePowerTargetL3 + " \n Substract AC Out Power "
						+ this.batteryInverter.getAcConsumptionPowerL1().orElse(0) + "|L2: "
						+ this.batteryInverter.getAcConsumptionPowerL2().orElse(0) + "|L3: "
						+ this.batteryInverter.getAcConsumptionPowerL3().orElse(0));
		// at this point we add AC Out power values
		// i.e. -300W (charge battery)
		// 100W AC Out we have to draw 300W from grid

		if (activePowerTargetL1 == 0 && activePowerTargetL2 == 0 && activePowerTargetL3 == 0) {
			this.logDebug(this.log, "\n Disabling Charging / Discharging");
			this._setDisableChargeFlag(EnableDisable.ENABLE);
			this._setDisableDischargeFlag(EnableDisable.ENABLE);
		} else {
			if (this.getDisableChargeFlag() != EnableDisable.DISABLE
					|| this.getDisableDischargeFlag() != EnableDisable.DISABLE) {
				this._setDisableChargeFlag(EnableDisable.DISABLE);
				this._setDisableDischargeFlag(EnableDisable.DISABLE);
			}
		}

		if (activePowerTargetL1 < 0) {
			// CHARGE: AC-Out draws power from battery additionally
			activePowerTargetL1 -= this.batteryInverter.getAcConsumptionPowerL1().orElse(0);
		}

		if (activePowerTargetL2 < 0) {
			// CHARGE: AC-Out draws power from battery additionally
			activePowerTargetL2 -= this.batteryInverter.getAcConsumptionPowerL2().orElse(0);
		}

		if (activePowerTargetL3 < 0) {
			// CHARGE: AC-Out draws power from battery additionally
			activePowerTargetL3 -= this.batteryInverter.getAcConsumptionPowerL3().orElse(0);
		}

		if (this.config.readOnlyMode()) {
			this.logDebug(this.log, "Read Only Mode is active. Power is not applied");
			return;
		}

		// Falls 1p: die nicht genutzten Phasen auf 0 setzen
		if (this.singlePhase != null) {
			switch (this.singlePhase) {
			case L1 -> {
				activePowerTargetL2 = 0;
				activePowerTargetL3 = 0;
			}
			case L2 -> {
				activePowerTargetL1 = 0;
				activePowerTargetL3 = 0;
			}
			case L3 -> {
				activePowerTargetL1 = 0;
				activePowerTargetL2 = 0;
			}
			}
		}

		this.batteryInverter.run(battery, activePowerTargetL1 + activePowerTargetL2 + activePowerTargetL3,
				reactivePowerTargetL1 + reactivePowerTargetL2 + reactivePowerTargetL3); //

		this.logDebug(this.log, "Apply Power L1: " + activePowerTargetL1 + "|L2: " + activePowerTargetL2 + "|L3: "
				+ activePowerTargetL3);

		// Victron: Negative values for Discharge
		// OpenEMS: Negative values for Charge
		// Write values to ESS

		this.setEssActivePowerL1((short) (activePowerTargetL1 * -1)); // feed channel, commit to hardware
		this.setEssActivePowerL2((short) (activePowerTargetL2 * -1));
		this.setEssActivePowerL3((short) (activePowerTargetL3 * -1));

	}

	// applyPower method for symmetric systems. 1p or 3p
	@Override
	public void applyPower(int activePowerTarget, int reactivePower) throws OpenemsNamedException {

		if (this.batteryInverter == null) {
			this.logError(this.log, "ApplyPower->BatteryInverter not activated ");
			return;
		}

		if (this.battery == null) {
			// battery = this.batteryInverter.getBattery();
			this.logError(this.log, "ApplyPower->Battery not activated ");
			return;
		}

		if (!this.batteryInverter.calculateHardwareLimits()) {
			return;
		}
		this.logDebug(this.log, "ApplyPower Target: " + activePowerTarget + "W");

		if (this.batteryInverter.getMaxApparentPower().get() == null
				|| this.batteryInverter.getMaxApparentPower().get() == 0) {
			this.logError(this.log, "ApplyPower->Max. Apparent Power invalid");
			return;
		}
		this._setMaxApparentPower(this.batteryInverter.getMaxApparentPower().get().intValue());

		MaxChargePower = this.batteryInverter.getMaxChargePower();
		MaxDischargePower = this.batteryInverter.getMaxDischargePower();

		this.logDebug(this.log,
				"Max Charge/Discharge Power from Inverter: " + MaxChargePower + "/" + MaxDischargePower + "W");

		if (MaxChargePower == null || MaxDischargePower == null) {
			this.logError(this.log, "power Limits not set.");
			return;
		}

		this.logDebug(this.log, "Symm. PowerWanted: " + activePowerTarget);

		// at this point we add AC Out power values
		// i.e. -300W (charge battery)
		// 100W AC Out we have to draw 300W from grid
		// activePowerTarget = activePowerTarget -
		// (this.getActivePowerOutputL1().orElse(0) +
		// this.getActivePowerOutputL2().orElse(0) +
		// this.getActivePowerOutputL3().orElse(0));
		// activePowerTarget = activePowerTarget -
		// (this.batteryInverter.getAcConsumptionPowerL1().orElse(0)
		// + this.batteryInverter.getAcConsumptionPowerL2().orElse(0)
		// + this.batteryInverter.getAcConsumptionPowerL3().orElse(0));

		int acConsumptionPowerSum = this.batteryInverter.getAcConsumptionPowerL1().orElse(0)
				+ this.batteryInverter.getAcConsumptionPowerL2().orElse(0)
				+ this.batteryInverter.getAcConsumptionPowerL3().orElse(0);

		if (activePowerTarget == 0) {
			this.logDebug(this.log, "\n Disabling Charging / Discharging");
			this._setDisableChargeFlag(EnableDisable.ENABLE);
			this._setDisableDischargeFlag(EnableDisable.ENABLE);
		} else {
			if (this.getDisableChargeFlag() != EnableDisable.DISABLE
					|| this.getDisableDischargeFlag() != EnableDisable.DISABLE) {
				this._setDisableChargeFlag(EnableDisable.DISABLE);
				this._setDisableDischargeFlag(EnableDisable.DISABLE);
			}
		}

		if (activePowerTarget < 0) {
			// CHARGE: AC-Out zieht Leistung ab -> mehr (negativ) ziehen
			activePowerTarget -= acConsumptionPowerSum;
			this.logDebug(this.log, "Symm. PowerWanted ChargeMode after subtraction of AC Out: " + acConsumptionPowerSum
					+ " ->  " + activePowerTarget);
		} else if (activePowerTarget > 0) {
			// activePowerTarget += acConsumptionPowerSum;
			this.logDebug(this.log, "Symm. PowerWanted DischargeMode Target ->  " + activePowerTarget);
		}

		// Check if desired Power value is within limits
		// Consumption on AC Out is not part of the Setpoint.
		// But it has to be calculated, too
// ToDo: Think about Consumption when calculate max. powers 

		if (activePowerTarget < 0 && Math.abs(activePowerTarget) > MaxChargePower) {
			activePowerTarget = MaxChargePower * -1;
		}
		if (activePowerTarget > 0 && activePowerTarget > MaxDischargePower) {
			activePowerTarget = MaxDischargePower;
		}

		this._setAllowedChargePower(MaxChargePower * -1); // Negative for charging
		this._setAllowedDischargePower(MaxDischargePower);// Positive for discharging

		if (this.hasFaults() || (this.getVEBusBMSAllowBatteryCharge() != AllowDisallow.ALLOWED)
				|| (this.getVEBusBMSAllowBatteryDischarge() != AllowDisallow.ALLOWED)) {
			this.logDebug(this.log, "System is not ready. Values will not be applied");
			return;
		}

		// if we are in symmetric mode we have to device the wanted power by 3
		// In single phase
		int powerPerPhase = activePowerTarget;

		if (this.getPhase() == null) { // no single Phase
			if (Math.abs(activePowerTarget) > 10) {
				powerPerPhase = (int) Math.round(activePowerTarget / 3.0);
			}
		}

		if (this.config.readOnlyMode()) {
			this.logDebug(this.log, "Read Only Mode is active. Power is not applied");
			return;
		}

		this.batteryInverter.run(battery, activePowerTarget, reactivePower); //

		// Write values to ESS
		if (this.getPhase() == null) { // no single Phase

			this.setEssActivePowerL1((short) (powerPerPhase * -1));
			this.logDebug(this.log, "Applying L1 " + powerPerPhase);
			// this._setReactivePowerL1((short) (powerPerPhase * -1)); // dummy. We have no
			// channel for that

			this.setEssActivePowerL2((short) (powerPerPhase * -1));
			this.logDebug(this.log, "Applying L2 " + powerPerPhase);
			// this._setReactivePowerL2((short) (powerPerPhase * -1)); // dummy. We have no
			// channel for that

			this.setEssActivePowerL3((short) (powerPerPhase * -1));
			this.logDebug(this.log, "Applying L3 " + powerPerPhase);
			// this._setReactivePowerL3((short) (powerPerPhase * -1)); // dummy. We have no
			// channel for that

		} else { // On a single phase ESS, power is applied to L1
			this.setEssActivePowerL1((short) (powerPerPhase * -1));
			// this._setReactivePowerL1((short) (powerPerPhase * -1)); // dummy. We have no
			// channel for that
		}

	}

	@Override
	public Power getPower() {
		return this.power;
	}

	// helper for calculating values from mA / mV
	private static int apparentSumVaFromMilli(int u1_mV, int i1_mA, int u2_mV, int i2_mA, int u3_mV, int i3_mA,
			boolean threePhase) {
		long microVA = 0L;
		microVA += 1L * Math.abs(u1_mV) * Math.abs(i1_mA);
		if (threePhase) {
			microVA += 1L * Math.abs(u2_mV) * Math.abs(i2_mA);
			microVA += 1L * Math.abs(u3_mV) * Math.abs(i3_mA);
		}
		//
		long va = microVA / 1_000_000L;
		if (va < 0)
			va = 0; //
		return (int) Math.min(Integer.MAX_VALUE, va);
	}

	public void _setMyActivePower() {
		// ToDo: make it work for single and 3 phase

		int acActivePowerSumInput = 0;
		int acActivePowerSumOutput = 0;
		int acApparentPowerSumInput = 0;
		int acApparentPowerSumOutput = 0;

		boolean threePhase = false;
		if (this.singlePhase == null) {
			threePhase = true;
		}

		/*
		 * AC Power in includes Power to AC-Out 1/2 and self consumption
		 * 
		 * If power on AC In is negative (charging, Victron registers inverted), AC
		 * consumption has to be subtracted.
		 * 
		 * If power on AC In is positive while discharging, AC Out is feed from battery.
		 * So it is part of power coming from ESS an has to be added
		 * 
		 * Target is to get the power coming from battery + AC conversion
		 * 
		 * Voltages / Currents are in mmili
		 * 
		 */

		// ActivePower is the actual AC output including battery discharging
		var acActivePowerInputL1 = this.getActivePowerInputL1().orElse(0); // 12 int 16 signed, SF1 (*10)
		var acActivePowerInputL2 = this.getActivePowerInputL2().orElse(0); // 13
		var acActivePowerInputL3 = this.getActivePowerInputL3().orElse(0); // 14

		var acVoltageInputL1 = this.getVoltageInputL1().orElse(0);
		var acVoltageInputL2 = this.getVoltageInputL2().orElse(0);
		var acVoltageInputL3 = this.getVoltageInputL3().orElse(0);

		var acCurrentInputL1 = this.getCurrentInputL1().orElse(0);
		var acCurrentInputL2 = this.getCurrentInputL2().orElse(0);
		var acCurrentInputL3 = this.getCurrentInputL3().orElse(0);

		// Input power calculation
		acActivePowerSumInput = acActivePowerInputL1 + acActivePowerInputL2 + acActivePowerInputL3;

		if (acVoltageInputL1 > 0) { // everything else can be 0
			acApparentPowerSumInput = apparentSumVaFromMilli(acVoltageInputL1, acCurrentInputL1, acVoltageInputL2,
					acCurrentInputL2, acVoltageInputL3, acCurrentInputL3, threePhase);
		}

		// Cosumption includes self-consumption and is always positive
		var acActivePowerOutputL1 = this.batteryInverter.getAcConsumptionPowerL1().orElse(0); // 817 uint16
		var acActivePowerOutputL2 = this.batteryInverter.getAcConsumptionPowerL2().orElse(0); // 818
		var acActivePowerOutputL3 = this.batteryInverter.getAcConsumptionPowerL3().orElse(0); // 819

		var acVoltageOutputL1 = this.getVoltageOutputL1().orElse(0);
		var acVoltageOutputL2 = this.getVoltageOutputL2().orElse(0);
		var acVoltageOutputL3 = this.getVoltageOutputL3().orElse(0);

		var acCurrentOutputL1 = this.getCurrentOutputL1().orElse(0);
		var acCurrentOutputL2 = this.getCurrentOutputL2().orElse(0);
		var acCurrentOutputL3 = this.getCurrentOutputL3().orElse(0);

		// Output power calculation
		acActivePowerSumOutput = acActivePowerOutputL1 + acActivePowerOutputL2 + acActivePowerOutputL3;

		// apparentPower calculation comes from mA/mV
		if (acVoltageOutputL1 > 0) { // everything else can be 0
			acApparentPowerSumOutput = apparentSumVaFromMilli(acVoltageOutputL1, acCurrentOutputL1, acVoltageOutputL2,
					acCurrentOutputL2, acVoltageOutputL3, acCurrentOutputL3, threePhase);
		}

		if (acActivePowerSumInput > 0) { // discharging
			this._setApparentPower(acApparentPowerSumInput - acApparentPowerSumOutput);
			this._setActivePower(acActivePowerSumInput + acActivePowerSumOutput);

			this._setActivePowerL1(acActivePowerInputL1 + acActivePowerOutputL1); // Asymmetric ESS nature
			if (threePhase) { // 3p
				this._setActivePowerL2(acActivePowerInputL2 + acActivePowerOutputL2); // Asymmetric ESS nature
				this._setActivePowerL3(acActivePowerInputL3 + acActivePowerOutputL3); // Asymmetric ESS nature
			}

		} else { // charging, including battery standby (battery +/-10W, AC-Input negative)
			this._setApparentPower(Math.max(0, acApparentPowerSumInput - acApparentPowerSumOutput));

			this._setActivePower(acActivePowerSumInput + acActivePowerSumOutput);

			this._setActivePowerL1(acActivePowerInputL1 + acActivePowerOutputL1); // Asymmetric ESS nature
			if (threePhase) { // 3p
				this._setActivePowerL2(acActivePowerInputL2 + acActivePowerOutputL2); // Asymmetric ESS nature
				this._setActivePowerL3(acActivePowerInputL3 + acActivePowerOutputL3); // Asymmetric ESS nature
			}
		}

		this.logDebug(this.log,
				"ActivePower Sum-Calculation. \n" + "\n Input ActivePower " + acActivePowerInputL1 + "W/"
						+ acActivePowerInputL2 + "W/" + acActivePowerInputL3 + "W Sum: " + acActivePowerSumInput
						+ "\n Input Voltage " + acVoltageInputL1 + "mV/" + acVoltageInputL2 + "mV/" + acVoltageInputL3
						+ "mV ApparentPower: " + acApparentPowerSumInput + "VA" + "\n Input Current " + acCurrentInputL1
						+ "mA/" + acCurrentInputL2 + "mA/" + acCurrentInputL3 + "mA"

						+ "\n Output ActivePower " + acActivePowerOutputL1 + "W/" + acActivePowerOutputL2 + "W/"
						+ acActivePowerOutputL3 + "W Sum: " + acActivePowerSumOutput + "\n Output Voltage "
						+ acVoltageOutputL1 + "mV/" + acVoltageOutputL2 + "mV/" + acVoltageOutputL3
						+ "mV ApparentPower: " + acApparentPowerSumOutput + "VA" + "\n Output Current "
						+ acCurrentOutputL1 + "mA/" + acCurrentOutputL2 + "mA/" + acCurrentOutputL3 + "mA"

						+ "\n Applied values for Active Power" + this.getActivePower().asString() + "/"
						+ this.getApparentPower().asString()

		);

	}

	public void _setMyActivePower_deprecated() {
		// ToDo: make it work for single and 3 phase

		/*
		 * Totally wrong! AC Power in includes Power to AC-Out 1/2 and self consumption.
		 * It was a mistake to add these values
		 * 
		 * 
		 */

		// ActivePower is the actual AC output including battery discharging
		var acPowerInputL1 = this.getActivePowerInputL1().orElse(null); // 12 int 16 signed, SF1 (*10)
		var acPowerInputL2 = this.getActivePowerInputL2().orElse(null); // 13
		var acPowerInputL3 = this.getActivePowerInputL3().orElse(null); // 14

		// Cosumption includes self-consumption and is always positive
		var acPowerOutputL1 = this.batteryInverter.getAcConsumptionPowerL1().orElse(0); // 817 uint16
		var acPowerOutputL2 = this.batteryInverter.getAcConsumptionPowerL2().orElse(0); // 818
		var acPowerOutputL3 = this.batteryInverter.getAcConsumptionPowerL3().orElse(0); // 819
		// var acPowerOutputL2 = this.getActivePowerOutputL2().orElse(0);
		// var acPowerOutputL3 = this.getActivePowerOutputL3().orElse(0);

		if (acPowerInputL1 != null && acPowerInputL2 != null && acPowerInputL3 != null && acPowerOutputL1 != null
				&& acPowerOutputL2 != null && acPowerOutputL3 != null) {
			this._setActivePowerL1(acPowerInputL1 + acPowerOutputL1); // Asymmetric ESS nature
			this._setActivePowerL2(acPowerInputL2 + acPowerOutputL2); // Asymmetric ESS nature
			this._setActivePowerL3(acPowerInputL3 + acPowerOutputL3); // Asymmetric ESS nature

			int acPowerSum = acPowerInputL1 + acPowerInputL2 + acPowerInputL3 + acPowerOutputL1 + acPowerOutputL2
					+ acPowerOutputL3;
			this._setActivePower(acPowerSum);
		} else {
			this.logDebug(this.log, "Unable to calculate active power as at least one phase is NULL");
		}

		var acVoltageInputL1 = this.getVoltageInputL1().orElse(null);
		var acVoltageInputL2 = this.getVoltageInputL2().orElse(null);
		var acVoltageInputL3 = this.getVoltageInputL3().orElse(null);

		var acCurrentInputL1 = this.getCurrentInputL1().orElse(null);
		var acCurrentInputL2 = this.getCurrentInputL2().orElse(null);
		var acCurrentInputL3 = this.getCurrentInputL3().orElse(null);

		var acVoltageOutputL1 = this.getVoltageOutputL1().orElse(null);
		var acVoltageOutputL2 = this.getVoltageOutputL2().orElse(null);
		var acVoltageOutputL3 = this.getVoltageOutputL3().orElse(null);

		var acCurrentOutputL1 = this.getCurrentOutputL1().orElse(0);
		var acCurrentOutputL2 = this.getCurrentOutputL2().orElse(0);
		var acCurrentOutputL3 = this.getCurrentOutputL3().orElse(0);

		if (acVoltageInputL1 != null && acVoltageInputL2 != null && acVoltageInputL3 != null && acCurrentInputL1 != null
				&& acCurrentInputL2 != null && acCurrentInputL3 != null) {
			int apparentePowerSum = (acVoltageInputL1 * acCurrentInputL1) + (acVoltageInputL2 * acCurrentInputL2)
					+ (acVoltageInputL3 * acCurrentInputL3) + (acVoltageOutputL1 * acCurrentOutputL1)
					+ (acVoltageOutputL2 * acCurrentOutputL2) + (acVoltageOutputL3 * acCurrentOutputL3);

			this._setApparentPower(apparentePowerSum);
		}

	}

	@Override
	public void handleEvent(Event event) {

		// super.handleEvent(event);

		switch (event.getTopic()) {

		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this._setMyActivePower();
			// this.calculateEnergy();
			break;
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS:
			this._setMyActivePower();
			this.calculateEnergy();
			if (this.config.symmetricAsymmetricMode() == SymmetricAsymmetricMode.ASYMMETRIC) {
				try {
					this.pushAsymmetricLimitsToPower(); // <<< hier!
				} catch (OpenemsNamedException e) {
					this.logWarn(this.log, "Push limits failed: " + e.getMessage());
				}
				break;
			}

		}

	}

	@Override
	public SinglePhase getPhase() {
		return this.singlePhase;
	}

	/**
	 * Calculate the Energy values for AC-side
	 *
	 * negative values for Charge; positive for Discharge
	 */
	private void calculateEnergy() {

		var activeAcPower = this.getActivePower().get();
		if (activeAcPower == null) {
			// Not available
			this.calculateChargeEnergy.update(null);
			this.calculateDischargeEnergy.update(null); //
		} else if (activeAcPower > 0) {
			// Discharge
			this.calculateChargeEnergy.update(0);
			this.calculateDischargeEnergy.update(activeAcPower);
		} else if (activeAcPower < 0) {
			// Charge
			this.calculateChargeEnergy.update(activeAcPower * -1);
			this.calculateDischargeEnergy.update(0);
		} else {
			// Undefined
			this.calculateChargeEnergy.update(0);
			this.calculateDischargeEnergy.update(0);
		}

		// Capacity Channel is also needed for ESS
		if (this.battery != null) {
			this._setCapacity(this.battery.getCapacity().get());
			// this._setUseableCapacity(this.battery.getUseableCapacity().get());
		}

		var dcPower = this.batteryInverter.getActivePower().get();
		if (dcPower != null) {
			this._setDcDischargePower(dcPower);
		}

		this._setDcDischargeEnergy(this.battery.getDcDischargeEnergy().get());
		this._setDcChargeEnergy(this.battery.getDcChargeEnergy().get());

	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(3, Priority.HIGH, //

						// Voltage AC In
						this.m(VictronEss.ChannelId.VOLTAGE_INPUT_L1, new UnsignedWordElement(3),
								ElementToChannelConverter.SCALE_FACTOR_2),
						this.m(VictronEss.ChannelId.VOLTAGE_INPUT_L2, new UnsignedWordElement(4),
								ElementToChannelConverter.SCALE_FACTOR_2),
						this.m(VictronEss.ChannelId.VOLTAGE_INPUT_L3, new UnsignedWordElement(5),
								ElementToChannelConverter.SCALE_FACTOR_2),

						// Current AC In
						this.m(VictronEss.ChannelId.CURRENT_INPUT_L1, new SignedWordElement(6),
								ElementToChannelConverter.SCALE_FACTOR_2),
						this.m(VictronEss.ChannelId.CURRENT_INPUT_L2, new SignedWordElement(7),
								ElementToChannelConverter.SCALE_FACTOR_2),
						this.m(VictronEss.ChannelId.CURRENT_INPUT_L3, new SignedWordElement(8),
								ElementToChannelConverter.SCALE_FACTOR_2),

						// Frequencies AC In
						this.m(VictronEss.ChannelId.FREQUENCY_INPUT_L1, new UnsignedWordElement(9),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						this.m(VictronEss.ChannelId.FREQUENCY_INPUT_L2, new UnsignedWordElement(10),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						this.m(VictronEss.ChannelId.FREQUENCY_INPUT_L3, new UnsignedWordElement(11),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),

						// Power AC In
						this.m(VictronEss.ChannelId.ACTIVE_POWER_INPUT_L1, new SignedWordElement(12),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(true)),
						this.m(VictronEss.ChannelId.ACTIVE_POWER_INPUT_L2, new SignedWordElement(13),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(true)),
						this.m(VictronEss.ChannelId.ACTIVE_POWER_INPUT_L3, new SignedWordElement(14),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(true)),

						// Output Voltages
						this.m(VictronEss.ChannelId.VOLTAGE_OUTPUT_L1, new UnsignedWordElement(15),
								ElementToChannelConverter.SCALE_FACTOR_2),
						this.m(VictronEss.ChannelId.VOLTAGE_OUTPUT_L2, new UnsignedWordElement(16),
								ElementToChannelConverter.SCALE_FACTOR_2),
						this.m(VictronEss.ChannelId.VOLTAGE_OUTPUT_L3, new UnsignedWordElement(17),
								ElementToChannelConverter.SCALE_FACTOR_2),

						// Output Currents
						this.m(VictronEss.ChannelId.CURRENT_OUTPUT_L1, new SignedWordElement(18),
								ElementToChannelConverter.SCALE_FACTOR_2),
						this.m(VictronEss.ChannelId.CURRENT_OUTPUT_L2, new SignedWordElement(19),
								ElementToChannelConverter.SCALE_FACTOR_2),
						this.m(VictronEss.ChannelId.CURRENT_OUTPUT_L3, new SignedWordElement(20),
								ElementToChannelConverter.SCALE_FACTOR_2),

						// Output Frequency
						this.m(VictronEss.ChannelId.FREQUENCY_OUTPUT, new SignedWordElement(21),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),

						this.m(VictronEss.ChannelId.CURRENT_INPUT_LIMIT, new SignedWordElement(22),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),

						this.m(VictronEss.ChannelId.ACTIVE_POWER_OUTPUT_L1, new SignedWordElement(23),
								ElementToChannelConverter.SCALE_FACTOR_1),
						this.m(VictronEss.ChannelId.ACTIVE_POWER_OUTPUT_L2, new SignedWordElement(24),
								ElementToChannelConverter.SCALE_FACTOR_1),
						this.m(VictronEss.ChannelId.ACTIVE_POWER_OUTPUT_L3, new SignedWordElement(25),
								ElementToChannelConverter.SCALE_FACTOR_1),

						// Battery Voltage and Current
						this.m(VictronEss.ChannelId.BATTERY_VOLTAGE, new UnsignedWordElement(26),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						this.m(VictronEss.ChannelId.BATTERY_CURRENT, new SignedWordElement(27),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),

						// Phase Count & Active Input
						this.m(VictronEss.ChannelId.PHASE_COUNT, new UnsignedWordElement(28)),
						this.m(VictronEss.ChannelId.ACTIVE_INPUT, new UnsignedWordElement(29)),

						// VE.Bus State of Charge & State
						this.m(SymmetricEss.ChannelId.SOC, new UnsignedWordElement(30),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						this.m(VictronEss.ChannelId.VE_BUS_STATE, new UnsignedWordElement(31)),

						// VE.Bus Error & Switch Position
						this.m(VictronEss.ChannelId.VE_BUS_ERROR, new UnsignedWordElement(32)),
						this.m(VictronEss.ChannelId.SWITCH_POSITION, new UnsignedWordElement(33)),

						// Alarms
						this.m(VictronEss.ChannelId.TEMPERATURE_ALARM, new UnsignedWordElement(34)),
						this.m(VictronEss.ChannelId.LOW_BATTERY_ALARM, new UnsignedWordElement(35)),
						this.m(VictronEss.ChannelId.OVERLOAD_ALARM, new UnsignedWordElement(36)),

						// ESS Power Setpoints
						this.m(VictronEss.ChannelId.ESS_POWER_SETPOINT_PHASE_1, new SignedWordElement(37)),

						this.m(VictronEss.ChannelId.ESS_DISABLE_CHARGE_FLAG, new SignedWordElement(38)),
						this.m(VictronEss.ChannelId.ESS_DISABLE_FEEDBACK_FLAG, new SignedWordElement(39)),

						this.m(VictronEss.ChannelId.ESS_POWER_SETPOINT_PHASE_2, new SignedWordElement(40)),
						this.m(VictronEss.ChannelId.ESS_POWER_SETPOINT_PHASE_3, new SignedWordElement(41)),

						// Sensor Alarms & Ripple Alarms
						this.m(VictronEss.ChannelId.TEMPERATURE_SENSOR_ALARM, new UnsignedWordElement(42)),
						this.m(VictronEss.ChannelId.VOLTAGE_SENSOR_ALARM, new UnsignedWordElement(43)),

						this.m(VictronEss.ChannelId.TEMPERATURE_ALARM_L1, new UnsignedWordElement(44)),
						this.m(VictronEss.ChannelId.LOW_BATTERY_ALARM_L1, new UnsignedWordElement(45)),
						this.m(VictronEss.ChannelId.OVERLOAD_ALARM_L1, new UnsignedWordElement(46)),
						this.m(VictronEss.ChannelId.RIPPLE_ALARM_L1, new UnsignedWordElement(47)),

						this.m(VictronEss.ChannelId.TEMPERATURE_ALARM_L2, new UnsignedWordElement(48)),
						this.m(VictronEss.ChannelId.LOW_BATTERY_ALARM_L2, new UnsignedWordElement(49)),
						this.m(VictronEss.ChannelId.OVERLOAD_ALARM_L2, new UnsignedWordElement(50)),
						this.m(VictronEss.ChannelId.RIPPLE_ALARM_L2, new UnsignedWordElement(51)),

						this.m(VictronEss.ChannelId.TEMPERATURE_ALARM_L3, new UnsignedWordElement(52)),
						this.m(VictronEss.ChannelId.LOW_BATTERY_ALARM_L3, new UnsignedWordElement(53)),
						this.m(VictronEss.ChannelId.OVERLOAD_ALARM_L3, new UnsignedWordElement(54)),
						this.m(VictronEss.ChannelId.RIPPLE_ALARM_L3, new UnsignedWordElement(55)),

						// PV Inverter & VE.Bus BMS Settings
						this.m(VictronEss.ChannelId.DISABLE_PV_INVERTER, new UnsignedWordElement(56)),
						this.m(VictronEss.ChannelId.VE_BUS_BMS_ALLOW_BATTERY_CHARGE, new UnsignedWordElement(57)),
						this.m(VictronEss.ChannelId.VE_BUS_BMS_ALLOW_BATTERY_DISCHARGE, new UnsignedWordElement(58)),

						this.m(VictronEss.ChannelId.VE_BUS_BMS_EXPECTED, new UnsignedWordElement(59)),
						this.m(VictronEss.ChannelId.VE_BUS_BMS_ERROR, new UnsignedWordElement(60)),

						// Additional Battery Information
						this.m(VictronEss.ChannelId.BATTERY_TEMPERATURE, new SignedWordElement(61),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						this.m(VictronEss.ChannelId.VE_BUS_RESET, new UnsignedWordElement(62)),
						this.m(VictronEss.ChannelId.PHASE_ROTATION_WARNING, new UnsignedWordElement(63)),

						// Grid & Feed-in Limits
						this.m(VictronEss.ChannelId.GRID_LOST_ALARM, new UnsignedWordElement(64)),

						this.m(VictronEss.ChannelId.FEED_DC_OVERVOLTAGE_TO_GRID, new UnsignedWordElement(65)),
						this.m(VictronEss.ChannelId.MAX_DC_OVERVOLTAGE_POWER_TO_GRID_L1, new UnsignedWordElement(66)),
						this.m(VictronEss.ChannelId.MAX_DC_OVERVOLTAGE_POWER_TO_GRID_L2, new UnsignedWordElement(67)),
						this.m(VictronEss.ChannelId.MAX_DC_OVERVOLTAGE_POWER_TO_GRID_L3, new UnsignedWordElement(68)),

						this.m(VictronEss.ChannelId.AC_INPUT1_IGNORED, new UnsignedWordElement(69)),
						this.m(VictronEss.ChannelId.AC_INPUT2_IGNORED, new UnsignedWordElement(70)),

						this.m(VictronEss.ChannelId.AC_POWER_SETPOINT_AS_FEED_IN_LIMIT, new UnsignedWordElement(71)),
						this.m(VictronEss.ChannelId.SOLAR_OFFSET_VOLTAGE, new UnsignedWordElement(72)),

						this.m(VictronEss.ChannelId.SUSTAIN_ACTIVE, new UnsignedWordElement(73)),
						//
						// Attention! Energy values reset on system reboot
						this.m(VictronEss.ChannelId.ENERGY_FROM_AC_IN_1_TO_AC_OUT, new UnsignedDoublewordElement(74),
								ElementToChannelConverter.SCALE_FACTOR_1),
						this.m(VictronEss.ChannelId.ENERGY_FROM_AC_IN_1_TO_BATTERY, new UnsignedDoublewordElement(76),
								ElementToChannelConverter.SCALE_FACTOR_1),
						this.m(VictronEss.ChannelId.ENERGY_FROM_AC_IN_2_TO_AC_OUT, new UnsignedDoublewordElement(78),
								ElementToChannelConverter.SCALE_FACTOR_1),
						this.m(VictronEss.ChannelId.ENERGY_FROM_AC_IN_2_TO_BATTERY, new UnsignedDoublewordElement(80),
								ElementToChannelConverter.SCALE_FACTOR_1),
						this.m(VictronEss.ChannelId.ENERGY_FROM_AC_OUT_TO_AC_IN_1, new UnsignedDoublewordElement(82),
								ElementToChannelConverter.SCALE_FACTOR_1),
						this.m(VictronEss.ChannelId.ENERGY_FROM_AC_OUT_TO_AC_IN_2, new UnsignedDoublewordElement(84),
								ElementToChannelConverter.SCALE_FACTOR_1),
						this.m(VictronEss.ChannelId.ENERGY_FROM_BATTERY_TO_AC_IN_1, new UnsignedDoublewordElement(86),
								ElementToChannelConverter.SCALE_FACTOR_1),
						this.m(VictronEss.ChannelId.ENERGY_FROM_BATTERY_TO_AC_IN_2, new UnsignedDoublewordElement(88),
								ElementToChannelConverter.SCALE_FACTOR_1),
						this.m(VictronEss.ChannelId.ENERGY_FROM_BATTERY_TO_AC_OUT, new UnsignedDoublewordElement(90),
								ElementToChannelConverter.SCALE_FACTOR_1),
						this.m(VictronEss.ChannelId.ENERGY_FROM_AC_OUT_TO_BATTERY, new UnsignedDoublewordElement(92),
								ElementToChannelConverter.SCALE_FACTOR_1),
						this.m(VictronEss.ChannelId.LOW_CELL_VOLTAGE_IMMINENT, new UnsignedWordElement(94)),
						this.m(VictronEss.ChannelId.CHARGE_STATE, new UnsignedWordElement(95)),

						this.m(VictronEss.ChannelId.INT32_ESS_POWER_SETPOINT_PHASE_1, new SignedDoublewordElement(96)),
						this.m(VictronEss.ChannelId.INT32_ESS_POWER_SETPOINT_PHASE_2, new SignedDoublewordElement(98)),
						this.m(VictronEss.ChannelId.INT32_ESS_POWER_SETPOINT_PHASE_3, new SignedDoublewordElement(100)),

						this.m(VictronEss.ChannelId.PREFER_RENEWABLE_ENERGY, new UnsignedWordElement(102)),
						this.m(VictronEss.ChannelId.SELECT_REMOTE_GENERATOR, new UnsignedWordElement(103)),
						this.m(VictronEss.ChannelId.REMOTE_GENERATOR_SELECTED, new UnsignedWordElement(104))),

				new FC16WriteRegistersTask(37, //
						this.m(VictronEss.ChannelId.SET_ACTIVE_POWER_L1, new SignedWordElement(37)),
						this.m(VictronEss.ChannelId.ESS_DISABLE_CHARGE_FLAG, new SignedWordElement(38)),
						this.m(VictronEss.ChannelId.ESS_DISABLE_FEEDBACK_FLAG, new SignedWordElement(39)),
						this.m(VictronEss.ChannelId.SET_ACTIVE_POWER_L2, new SignedWordElement(40)),
						this.m(VictronEss.ChannelId.SET_ACTIVE_POWER_L3, new SignedWordElement(41)))

		);
	}

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	@Override
	public synchronized void setBatteryInverter(VictronBatteryInverter batteryInverter) {
		if (batteryInverter == null) {
			this.logError(this.log, "Attempt to bind a null BatteryInverter");
			return;
		}
		this.batteryInverter = batteryInverter;
		logInfo(this.log, "Battery Inverter bound successfully.");

		// Ensuring that the battery inverter is not null before attempting to get max
		// apparent power
		if (this.batteryInverter.getMaxApparentPower().get() != null) {
			Integer maxApparentPower = this.batteryInverter.getMaxApparentPower().get();
			this._setMaxApparentPower(maxApparentPower);
		} else {
			this.logError(this.log, "ESS->BatteryInverter max. apparent power not set ");
		}
	}

	@Override
	public void unsetBatteryInverter(VictronBatteryInverter batteryInverter) {
		this.batteryInverter = null;

	}

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	@Override
	public synchronized void setBattery(VictronBattery battery) {

		if (battery == null) {
			this.logError(this.log, "ESS->Battery not activated ");
			return;
		}
		this.battery = battery;
	}

	@Override
	public synchronized void unsetBattery(VictronBattery battery) {
		this.battery = null;

	}

}
