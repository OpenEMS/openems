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
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
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
	private final CalculateEnergyFromPower calculateDcDischargeEnergy = new CalculateEnergyFromPower(this,
			VictronEss.ChannelId.DC_DISCHARGE_ENERGY);

	private final CalculateEnergyFromPower calculateDcChargeEnergy = new CalculateEnergyFromPower(this,
			VictronEss.ChannelId.DC_CHARGE_ENERGY);

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

		if (this.singlePhase != null) {
			SinglePhaseEss.initializeCopyPhaseChannel(this, this.singlePhase);
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

	private int limitPhasePower(int phasePower, int maxCharge, int maxDischarge) {
		if (phasePower < 0 && Math.abs(phasePower) > maxCharge) {
			return -maxCharge;
		}
		if (phasePower > 0 && phasePower > maxDischarge) {
			return maxDischarge;
		}
		return phasePower;
	}

	// Asymmetric systems
	@Override
	public void applyPower(int activePowerL1, int reactivePowerL1, int activePowerL2, int reactivePowerL2,
			int activePowerL3, int reactivePowerL3) throws OpenemsNamedException {

		this.logDebug(this.log,
				"Asymm. PowerWanted L1: " + activePowerL1 + "|L2: " + activePowerL2 + "|L3: " + activePowerL3);

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
		this._setAllowedChargePower(MaxChargePower * -1);
		this._setAllowedDischargePower(MaxChargePower);

		this.logDebug(this.log,
				"OpenEMS Apply Power L1: " + activePowerL1 + "|L2: " + activePowerL2 + "|L3: " + activePowerL3
						+ " \n Substract AC Out Power " + this.batteryInverter.getAcConsumptionPowerL1().orElse(0)
						+ "|L2: " + this.batteryInverter.getAcConsumptionPowerL2().orElse(0) + "|L3: "
						+ this.batteryInverter.getAcConsumptionPowerL3().orElse(0));
		// at this point we add AC Out power values
		// i.e. -300W (charge battery)
		// 100W AC Out we have to draw 300W from grid
		activePowerL1 -= this.batteryInverter.getAcConsumptionPowerL1().orElse(0);
		activePowerL2 -= this.batteryInverter.getAcConsumptionPowerL2().orElse(0);
		activePowerL3 -= this.batteryInverter.getAcConsumptionPowerL3().orElse(0);

		switch (config.phase()) {
		case ALL:
			int MaxChargePowerSum = activePowerL1 + activePowerL2 + activePowerL3;
			// Check if desired Power value is within limits

			activePowerL1 = limitPhasePower(activePowerL1, MaxChargePower, MaxDischargePower);
			activePowerL2 = limitPhasePower(activePowerL2, MaxChargePower, MaxDischargePower);
			activePowerL3 = limitPhasePower(activePowerL3, MaxChargePower, MaxDischargePower);

			// limit phases according to scaling factor
			if (MaxChargePowerSum < 0 && Math.abs(MaxChargePowerSum) > MaxChargePower) {

				double scalingFactor = (double) MaxChargePower / Math.abs(MaxChargePowerSum);
				activePowerL1 = (int) Math.round(activePowerL1 * scalingFactor);
				activePowerL2 = (int) Math.round(activePowerL2 * scalingFactor);
				activePowerL3 = (int) Math.round(activePowerL3 * scalingFactor);
				this.logDebug(this.log, "Asymmetric Ess. ALL PHASE LIMITATION. Apply Power L1: " + activePowerL1
						+ "|L2: " + activePowerL2 + "|L3: " + activePowerL3);
			}
			if (MaxChargePowerSum > 0 && MaxChargePowerSum > MaxDischargePower) {

				double scalingFactor = (double) MaxDischargePower / Math.abs(MaxChargePowerSum);
				activePowerL1 = (int) Math.round(activePowerL1 * scalingFactor);
				activePowerL2 = (int) Math.round(activePowerL2 * scalingFactor);
				activePowerL3 = (int) Math.round(activePowerL3 * scalingFactor);
				this.logDebug(this.log, "Asymmetric Ess. ALL PHASE LIMITATION. Apply Power L1: " + activePowerL1
						+ "|L2: " + activePowerL2 + "|L3: " + activePowerL3);
			}

			break;
		case L1:
			// Check if desired Power value is within limits
			if (activePowerL1 < 0 && Math.abs(activePowerL1) > MaxChargePower) {
				activePowerL1 = MaxChargePower * -1;
			}
			if (activePowerL1 > 0 && activePowerL1 > MaxDischargePower) {
				activePowerL1 = MaxDischargePower;
			}
			activePowerL2 = 0;
			activePowerL3 = 0;
			this._setActivePowerL1((short) (activePowerL1 * -1));

			break;
		case L2:
			// Check if desired Power value is within limits
			if (activePowerL2 < 0 && Math.abs(activePowerL2) > MaxChargePower) {
				activePowerL2 = MaxChargePower * -1;
			}
			if (activePowerL2 > 0 && activePowerL2 > MaxChargePower) {
				activePowerL2 = MaxChargePower;
			}
			activePowerL1 = 0;
			activePowerL3 = 0;
			this._setActivePowerL2((short) (activePowerL2 * -1));

			break;
		case L3:
			// Check if desired Power value is within limits
			if (activePowerL3 < 0 && Math.abs(activePowerL3) > MaxChargePower) {
				activePowerL3 = MaxChargePower * -1;
			}
			if (activePowerL3 > 0 && activePowerL3 > MaxChargePower) {
				activePowerL3 = MaxChargePower;
			}
			activePowerL1 = 0;
			activePowerL2 = 0;
			this._setActivePowerL3((short) (activePowerL3 * -1));

			break;
		}

		this.batteryInverter.run(battery, activePowerL1 + activePowerL2 + activePowerL3,
				reactivePowerL1 + reactivePowerL2 + reactivePowerL3); //

		if (this.config.readOnlyMode()) {
			this.logDebug(this.log, "Read Only Mode is active. Power is not applied");
			return;
		}
		this.logDebug(this.log, "Apply Power L1: " + activePowerL1 + "|L2: " + activePowerL2 + "|L3: " + activePowerL3);

		// Victron: Negative values for Discharge
		// OpenEMS: Negative values for Charge
		// Write values to ESS
		if (this.getPhase() == null) { // no single Phase

			this._setSymmetricEssActivePowerL1((short) (activePowerL1 * -1));
			// this._setReactivePowerL1((short) (powerPerPhase * -1)); // dummy. We have no
			// channel for that

			this._setSymmetricEssActivePowerL2((short) (activePowerL2 * -1));
			// this._setReactivePowerL2((short) (powerPerPhase * -1)); // dummy. We have no
			// channel for that

			this._setSymmetricEssActivePowerL3((short) (activePowerL3 * -1));
			// this._setReactivePowerL3((short) (powerPerPhase * -1)); // dummy. We have no
			// channel for that

		} else { // On a single phase ESS, power is applied to L1
			this._setSymmetricEssActivePowerL1((short) (activePowerL1 * -1));
			// this._setReactivePowerL1((short) (powerPerPhase * -1)); // dummy. We have no
			// channel for that
		}

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
		activePowerTarget = activePowerTarget - (this.batteryInverter.getAcConsumptionPowerL1().orElse(0)
				+ this.batteryInverter.getAcConsumptionPowerL2().orElse(0)
				+ this.batteryInverter.getAcConsumptionPowerL3().orElse(0));

		this.logDebug(this.log, "Symm. PowerWanted after subtraction of AC Out: " + activePowerTarget);

		// Check if desired Power value is within limits
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
				powerPerPhase = Math.round(activePowerTarget / 3);
			}
		}

		this.batteryInverter.run(battery, activePowerTarget, reactivePower); //

		if (this.config.readOnlyMode()) {
			this.logDebug(this.log, "Read Only Mode is active. Power is not applied");
			return;
		}

		// Write values to ESS
		if (this.getPhase() == null) { // no single Phase

			this._setSymmetricEssActivePowerL1((short) (powerPerPhase * -1));
			// this._setReactivePowerL1((short) (powerPerPhase * -1)); // dummy. We have no
			// channel for that

			this._setSymmetricEssActivePowerL2((short) (powerPerPhase * -1));
			// this._setReactivePowerL2((short) (powerPerPhase * -1)); // dummy. We have no
			// channel for that

			this._setSymmetricEssActivePowerL3((short) (powerPerPhase * -1));
			// this._setReactivePowerL3((short) (powerPerPhase * -1)); // dummy. We have no
			// channel for that

		} else { // On a single phase ESS, power is applied to L1
			this._setSymmetricEssActivePowerL1((short) (powerPerPhase * -1));
			// this._setReactivePowerL1((short) (powerPerPhase * -1)); // dummy. We have no
			// channel for that
		}

	}

	@Override
	public Power getPower() {
		return this.power;
	}

	public void _setMyActivePower() {
		// ToDo: make it work for single and 3 phase

		// ActivePower is the actual AC output including battery discharging
		var acPowerInputL1 = this.getActivePowerInputL1().orElse(null);
		var acPowerInputL2 = this.getActivePowerInputL2().orElse(null);
		var acPowerInputL3 = this.getActivePowerInputL3().orElse(null);

		var acPowerOutputL1 = this.batteryInverter.getAcConsumptionPowerL1().orElse(0);
		var acPowerOutputL2 = this.batteryInverter.getAcConsumptionPowerL2().orElse(0);
		var acPowerOutputL3 = this.batteryInverter.getAcConsumptionPowerL3().orElse(0);
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
			// this.checkSocControllers();
			break;

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

		var activeAcPower = this.getActivePower().get(); // AC-Power never gets negative. So it´s AC power out of the
															// ESS. Actually we don´t need the following calculation
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

		if (dcPower == null) {
			// Not available
			this.calculateDcChargeEnergy.update(null);
			this.calculateDcDischargeEnergy.update(null); //
		} else if (dcPower > 0) {
			// Discharge
			this.calculateDcChargeEnergy.update(0);
			this.calculateDcDischargeEnergy.update(dcPower);
		} else if (dcPower < 0) {
			// Charge
			this.calculateDcChargeEnergy.update(dcPower * -1);
			this.calculateDcDischargeEnergy.update(0);
		} else {
			// Undefined
			this.calculateDcChargeEnergy.update(0);
			this.calculateDcDischargeEnergy.update(0);
		}

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
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						this.m(VictronEss.ChannelId.VOLTAGE_INPUT_L2, new UnsignedWordElement(4),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						this.m(VictronEss.ChannelId.VOLTAGE_INPUT_L3, new UnsignedWordElement(5),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),

						// Current AC In
						this.m(VictronEss.ChannelId.CURRENT_INPUT_L1, new SignedWordElement(6),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						this.m(VictronEss.ChannelId.CURRENT_INPUT_L2, new SignedWordElement(7),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						this.m(VictronEss.ChannelId.CURRENT_INPUT_L3, new SignedWordElement(8),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),

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
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						this.m(VictronEss.ChannelId.VOLTAGE_OUTPUT_L2, new UnsignedWordElement(16),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						this.m(VictronEss.ChannelId.VOLTAGE_OUTPUT_L3, new UnsignedWordElement(17),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),

						// Output Currents
						this.m(VictronEss.ChannelId.CURRENT_OUTPUT_L1, new SignedWordElement(18),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						this.m(VictronEss.ChannelId.CURRENT_OUTPUT_L2, new SignedWordElement(19),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						this.m(VictronEss.ChannelId.CURRENT_OUTPUT_L3, new SignedWordElement(20),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),

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
						new DummyRegisterElement(38, 39),
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
