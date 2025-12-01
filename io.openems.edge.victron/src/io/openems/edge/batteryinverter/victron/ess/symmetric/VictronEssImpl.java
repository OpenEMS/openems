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
import io.openems.edge.common.type.Phase;
import static io.openems.edge.common.type.Phase.SingleOrAllPhase.ALL;
import static io.openems.edge.ess.power.api.Pwr.ACTIVE;
import static io.openems.edge.ess.power.api.Pwr.REACTIVE;
import static io.openems.edge.ess.power.api.Relationship.EQUALS;

import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSinglePhaseEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SinglePhaseEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;
import io.openems.edge.victron.enums.AllowDisallow;
import io.openems.edge.victron.enums.EnableDisable;

/**
 * Implementation of the Victron ESS component.
 *
 * <p>
 * This component integrates Victron Energy storage systems with OpenEMS via
 * Modbus TCP through Venus OS / Cerbo GX. It supports single-phase and
 * three-phase configurations, as well as symmetric and asymmetric power
 * distribution.
 *
 * <p>
 * The component communicates with the VE.Bus Multiplus-II inverter/charger
 * using the Venus Modbus-TCP registers (Unit-ID 227 for VE.Bus system).
 *
 * @see <a href="https://github.com/victronenergy/dbus_modbustcp">Venus
 *      Modbus-TCP</a>
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "ESS.Victron", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS //
})
public class VictronEssImpl extends AbstractOpenemsModbusComponent
		implements VictronEss, ManagedSinglePhaseEss, SinglePhaseEss, ManagedSymmetricEss, SymmetricEss, AsymmetricEss,
		ManagedAsymmetricEss, ModbusComponent, ModbusSlave, EventHandler, OpenemsComponent, TimedataProvider {

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

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
	private volatile VictronBatteryInverter batteryInverter;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
	private volatile VictronBattery battery;

	private final Logger log = LoggerFactory.getLogger(VictronEssImpl.class);

	private Config config;
	private SinglePhase singlePhase = null;

	private boolean operationalValuesOk = false;

	private Integer maxChargePower = null;
	private Integer maxDischargePower = null;

	private final CalculateEnergyFromPower calculateDischargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY);

	private final CalculateEnergyFromPower calculateChargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY);

	public VictronEssImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SinglePhaseEss.ChannelId.values(), //
				ManagedSinglePhaseEss.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				AsymmetricEss.ChannelId.values(), //
				ManagedAsymmetricEss.ChannelId.values(), //
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

		// Set initial values from config
		this._setMaxApparentPower(config.maxApparentPower());
		this._setCapacity(config.capacity());

		switch (config.phase()) {
		case L1:
			this.singlePhase = SinglePhase.L1;
			break;
		case L2:
			this.singlePhase = SinglePhase.L2;
			break;
		case L3:
			this.singlePhase = SinglePhase.L3;
			break;
		default:
			this.singlePhase = null;
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
		this.unsetBattery(this.battery);
		this.unsetBatteryInverter(this.batteryInverter);
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

	@Override
	public boolean isManaged() {
		return !this.config.readOnlyMode();
	}

	/**
	 * Updates the operational values from battery inverter.
	 *
	 * <p>
	 * Validates that battery, inverter, and BMS are ready for operation and updates
	 * allowed charge/discharge power limits accordingly.
	 */
	private void updateOperationalValues() {
		if (this.batteryInverter == null) {
			this.log.warn("ESS not ready. BatteryInverter not available");
			this.operationalValuesOk = false;
			return;
		}

		if (this.battery == null) {
			this.log.warn("ESS not ready. Battery not available");
			this.operationalValuesOk = false;
			return;
		}

		if (!Boolean.TRUE.equals(this.batteryInverter.calculateHardwareLimits())) {
			this.log.warn("BatteryInverter hardware limits not available");
			this.operationalValuesOk = false;
			return;
		}

		if (this.batteryInverter.getMaxApparentPower().get() == null
				|| this.batteryInverter.getMaxApparentPower().get() == 0) {
			this.log.warn("Max apparent power not available");
			this.operationalValuesOk = false;
			return;
		}

		if ((this.getVeBusBmsAllowBatteryCharge() != AllowDisallow.ALLOWED)) {
			this.log.warn("VEBus -> charging not allowed!");
			this.operationalValuesOk = false;
			return;

		}

		if ((this.getVeBusBmsAllowBatteryDischarge() != AllowDisallow.ALLOWED)) {
			this.log.warn("VEBus -> Discharging not allowed!");
			this.operationalValuesOk = false;
			return;

		}

		if ((this.getVeBusBmsAllowBatteryCharge() != AllowDisallow.ALLOWED)
				|| (this.getVeBusBmsAllowBatteryDischarge() != AllowDisallow.ALLOWED)) {
			this.log.warn(
					"BMS Allowed Charge/Discharge values not available -> System is not ready. Values will not be applied");
			return;
		}

		Integer maxChargePower = this.batteryInverter.getMaxChargePower(); // [W], positiv
		Integer maxDischargePower = this.batteryInverter.getMaxDischargePower(); // [W], positiv
		if (maxChargePower == null || maxDischargePower == null || maxChargePower < 0 || maxDischargePower < 0) {
			this.log.warn(
					"BatteryInverter Allowed Charge/Discharge values not available -> System is not ready. Values will not be applied");
			this.operationalValuesOk = false;
			return;
		}

		Integer maxApparentPower = this.batteryInverter.getMaxApparentPower().get();
		if (maxApparentPower == null || maxApparentPower < 0) {
			this.log.warn(
					"BatteryInverter max. Apparent power not available -> System is not ready. Values will not be applied");
			this.operationalValuesOk = false;
			return;
		}
		this.logDebug(this.log,
				"Getting max. Charge/Discharge power values: " + maxChargePower + "/" + maxDischargePower + "W");
		this._setAllowedChargePower(-maxChargePower);
		this._setAllowedDischargePower(maxDischargePower);
		this._setMaxApparentPower(maxApparentPower);

		this.operationalValuesOk = true;
	}

	/**
	 * Applies power setpoints for asymmetric ESS operation.
	 *
	 * <p>
	 * Note: Victron uses negative values for discharge, OpenEMS uses negative for
	 * charge. Values are inverted when writing to hardware.
	 *
	 * <p>
	 * AC-Out consumption is subtracted during charging to ensure accurate battery
	 * power control.
	 */
	@Override
	public void applyPower(int activePowerTargetL1, int reactivePowerTargetL1, int activePowerTargetL2,
			int reactivePowerTargetL2, int activePowerTargetL3, int reactivePowerTargetL3)
			throws OpenemsNamedException {

		if (!this.operationalValuesOk) {
			this.logWarn(this.log, "ESS is not ready for operation. Canceling ApplyPower(p1,p2,p3,q1,q2,q3");
			return;
		}

		this.logDebug(this.log, "Asymm. PowerWanted L1: " + activePowerTargetL1 + "|L2: " + activePowerTargetL2
				+ "|L3: " + activePowerTargetL3);

		this.logDebug(this.log, "Setting max. apparent power to batteryInverter-Channel");

		// Victron: Negative values for Discharge
		// OpenEMS: Negative values for Charge

		// if we are in symmetric mode we have to device the wanted power by 3
		// In single phase

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

		this.batteryInverter.run(this.battery, activePowerTargetL1 + activePowerTargetL2 + activePowerTargetL3,
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

	/**
	 * Applies power setpoints for symmetric ESS operation.
	 *
	 * <p>
	 * For three-phase systems, power is distributed equally across all phases. For
	 * single-phase systems, power is applied only to the configured phase.
	 *
	 * <p>
	 * Note: Victron uses negative values for discharge, OpenEMS uses negative for
	 * charge. Values are inverted when writing to hardware.
	 */
	@Override
	public void applyPower(int activePowerTarget, int reactivePower) throws OpenemsNamedException {

		if (!this.operationalValuesOk) {
			this.logWarn(this.log, "ESS is not ready for operation. Canceling ApplyPower(p1,q1)");
			return;
		}

		this.logDebug(this.log, "ApplyPower Target: " + activePowerTarget + "W");

		this._setMaxApparentPower(this.batteryInverter.getMaxApparentPower().get().intValue());

		this.maxChargePower = this.batteryInverter.getMaxChargePower();
		this.maxDischargePower = this.batteryInverter.getMaxDischargePower();

		this.logDebug(this.log, "Max Charge/Discharge Power from Inverter: " + this.maxChargePower + "/"
				+ this.maxDischargePower + "W");

		if (this.maxChargePower == null || this.maxDischargePower == null) {
			this.logError(this.log, "power Limits not set.");
			return;
		}

		this.logDebug(this.log, "Symm. PowerWanted: " + activePowerTarget);

		// AC Output power (Reg 23, 24, 25) is always positive
		int acOutputActivePowerSum = this.getActivePowerOutputL1().orElse(0) + this.getActivePowerOutputL2().orElse(0)
				+ this.getActivePowerOutputL3().orElse(0);

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
			// CHARGE: AC-Out draws power from battery, subtract from target
			activePowerTarget -= acOutputActivePowerSum;
			this.logDebug(this.log, "Symm. PowerWanted ChargeMode after subtraction of AC Out: "
					+ acOutputActivePowerSum + " ->  " + activePowerTarget);
		} else if (activePowerTarget > 0) {
			this.logDebug(this.log, "Symm. PowerWanted DischargeMode Target ->  " + activePowerTarget);
		}

		// Clamp power to hardware limits

		if (activePowerTarget < 0 && Math.abs(activePowerTarget) > this.maxChargePower) {
			activePowerTarget = this.maxChargePower * -1;
		}
		if (activePowerTarget > 0 && activePowerTarget > this.maxDischargePower) {
			activePowerTarget = this.maxDischargePower;
		}

		this._setAllowedChargePower(this.maxChargePower * -1); // Negative for charging
		this._setAllowedDischargePower(this.maxDischargePower); // Positive for discharging

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

		this.batteryInverter.run(this.battery, activePowerTarget, reactivePower); //

		// Write values to ESS
		if (this.getPhase() == null) { // no single Phase

			this.setEssActivePowerL1((short) (powerPerPhase * -1));
			this.logDebug(this.log, "Applying L1 " + powerPerPhase);

			this.setEssActivePowerL2((short) (powerPerPhase * -1));
			this.logDebug(this.log, "Applying L2 " + powerPerPhase);

			this.setEssActivePowerL3((short) (powerPerPhase * -1));
			this.logDebug(this.log, "Applying L3 " + powerPerPhase);

		} else { // On a single phase ESS, power is applied to L1
			this.setEssActivePowerL1((short) (powerPerPhase * -1));
		}

	}

	@Override
	public Power getPower() {
		return this.power;
	}

	/**
	 * Calculates apparent power sum from millivolt and milliampere values.
	 *
	 * @param u1_mV      voltage L1 in millivolts
	 * @param i1_mA      current L1 in milliamperes
	 * @param u2_mV      voltage L2 in millivolts
	 * @param i2_mA      current L2 in milliamperes
	 * @param u3_mV      voltage L3 in millivolts
	 * @param i3_mA      current L3 in milliamperes
	 * @param threePhase true if three-phase system, false for single-phase
	 * @return apparent power sum in VA
	 */
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
		if (va < 0) {
			va = 0;
		}
		return (int) Math.min(Integer.MAX_VALUE, va);
	}

	/**
	 * Calculates and sets the active/apparent power values.
	 *
	 * <p>
	 * This method calculates total power from AC input and output measurements. AC
	 * Power input includes power to AC-Out 1/2 and self-consumption.
	 *
	 * <p>
	 * Sign convention: Negative values for charge, positive for discharge.
	 */
	public void _setMyActivePower() {

		int acActivePowerSumInput = 0;
		int acApparentPowerSumInput = 0;
		boolean threePhase = this.singlePhase == null;

		// Read AC Input measurements (Grid side)
		var acVoltageInputL1 = this.getVoltageInputL1().orElse(0);
		var acVoltageInputL2 = this.getVoltageInputL2().orElse(0);
		var acVoltageInputL3 = this.getVoltageInputL3().orElse(0);

		var acCurrentInputL1 = this.getCurrentInputL1().orElse(0);
		var acCurrentInputL2 = this.getCurrentInputL2().orElse(0);
		var acCurrentInputL3 = this.getCurrentInputL3().orElse(0);

		// ActivePower is the actual AC output including battery discharging
		var acActivePowerInputL1 = this.getActivePowerInputL1().orElse(0);
		var acActivePowerInputL2 = this.getActivePowerInputL2().orElse(0);
		var acActivePowerInputL3 = this.getActivePowerInputL3().orElse(0);

		// Input power calculation
		acActivePowerSumInput = acActivePowerInputL1 + acActivePowerInputL2 + acActivePowerInputL3;

		if (acVoltageInputL1 > 0) { // everything else can be 0
			acApparentPowerSumInput = apparentSumVaFromMilli(acVoltageInputL1, acCurrentInputL1, acVoltageInputL2,
					acCurrentInputL2, acVoltageInputL3, acCurrentInputL3, threePhase);
		}

		var acVoltageOutputL1 = this.getVoltageOutputL1().orElse(0);
		var acVoltageOutputL2 = this.getVoltageOutputL2().orElse(0);
		var acVoltageOutputL3 = this.getVoltageOutputL3().orElse(0);

		var acCurrentOutputL1 = this.getCurrentOutputL1().orElse(0);
		var acCurrentOutputL2 = this.getCurrentOutputL2().orElse(0);
		var acCurrentOutputL3 = this.getCurrentOutputL3().orElse(0);

		// AC Output power (Reg 23-25) is always positive
		var acPowerOutputL1 = this.getActivePowerOutputL1().orElse(0);
		var acPowerOutputL2 = this.getActivePowerOutputL2().orElse(0);
		var acPowerOutputL3 = this.getActivePowerOutputL3().orElse(0);

		// Output power calculation
		int acOutputActivePowerSum = acPowerOutputL1 + acPowerOutputL2 + acPowerOutputL3;

		// apparentPower calculation comes from mA/mV
		int acApparentPowerSumOutput = 0;
		if (acVoltageOutputL1 > 0) {
			acApparentPowerSumOutput = apparentSumVaFromMilli(acVoltageOutputL1, acCurrentOutputL1, acVoltageOutputL2,
					acCurrentOutputL2, acVoltageOutputL3, acCurrentOutputL3, threePhase);
		}

		int activePowerSumWithOutput = acActivePowerSumInput + acOutputActivePowerSum;

		this._setApparentPower(acApparentPowerSumInput - acApparentPowerSumOutput);
		this._setActivePower(activePowerSumWithOutput);

		this._setActivePowerL1(acActivePowerInputL1 + acPowerOutputL1); // Asymmetric ESS nature
		if (threePhase) { // 3p
			this._setActivePowerL2(acActivePowerInputL2 + acPowerOutputL2); // Asymmetric ESS nature
			this._setActivePowerL3(acActivePowerInputL3 + acPowerOutputL3); // Asymmetric ESS nature
		}

		this.logDebug(this.log, "ActivePower Sum-Calculation. \n" + "\n Input ActivePower " + acActivePowerInputL1
				+ "W/" + acActivePowerInputL2 + "W/" + acActivePowerInputL3 + "W Sum: " + acActivePowerSumInput
				+ "\n Input Voltage " + acVoltageInputL1 + "mV/" + acVoltageInputL2 + "mV/" + acVoltageInputL3
				+ "mV ApparentPower: " + acApparentPowerSumInput + "VA" + "\n Input Current " + acCurrentInputL1 + "mA/"
				+ acCurrentInputL2 + "mA/" + acCurrentInputL3 + "mA \n" + "\n\n Output ActivePower " + acPowerOutputL1
				+ "W/" + acPowerOutputL2 + "W/" + acPowerOutputL3 + "W Sum: " + acOutputActivePowerSum + "W "
				+ "\n Output Voltage " + acVoltageOutputL1 + "mV/" + acVoltageOutputL2 + "mV/" + acVoltageOutputL3
				+ "mV ApparentPower: " + acApparentPowerSumOutput + "VA" + "\n Output Current " + acCurrentOutputL1
				+ "mA/" + acCurrentOutputL2 + "mA/" + acCurrentOutputL3 + "mA" + "\nActivePower (with OutputPower) "
				+ activePowerSumWithOutput + "W" + "\n ActivePower to Channel -> " + this.getActivePower().asString()
				+ "/"

				+ this.getApparentPower().asString()

		);

	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> {
			this.updateOperationalValues();
			this._setMyActivePower();

		}
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS -> {
			this._setMyActivePower();
			this.calculateEnergy();
		}
		}
	}

	@Override
	public SinglePhase getPhase() {
		return this.singlePhase;
	}

	/**
	 * Calculate the Energy values for AC-side.
	 *
	 * <p>
	 * Negative values for Charge; positive for Discharge.
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
		}

		if (this.batteryInverter != null) {
			this._setDcDischargePower(this.batteryInverter.getActivePower().get());
		}

		this._setDcDischargeEnergy(this.battery.getDcDischargeEnergy().get());
		this._setDcChargeEnergy(this.battery.getDcChargeEnergy().get());

	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() { // Unit-ID 227
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
						this.m(VictronEss.ChannelId.FREQUENCY_INPUT_L1, new SignedWordElement(9),
								ElementToChannelConverter.SCALE_FACTOR_1),
						this.m(VictronEss.ChannelId.FREQUENCY_INPUT_L2, new SignedWordElement(10),
								ElementToChannelConverter.SCALE_FACTOR_1),
						this.m(VictronEss.ChannelId.FREQUENCY_INPUT_L3, new SignedWordElement(11),
								ElementToChannelConverter.SCALE_FACTOR_1),

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
								ElementToChannelConverter.SCALE_FACTOR_1),

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

						this.m(VictronEss.ChannelId.ESS_DISABLE_CHARGE_FLAG, new UnsignedWordElement(38)),
						this.m(VictronEss.ChannelId.ESS_DISABLE_FEEDBACK_FLAG, new UnsignedWordElement(39)),

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

				(this.config.phase() == Phase.SingleOrAllPhase.ALL) // Do not write L2/L3 values in 1p
						? new FC16WriteRegistersTask(37,
								this.m(VictronEss.ChannelId.SET_ACTIVE_POWER_L1, new SignedWordElement(37)),
								this.m(VictronEss.ChannelId.ESS_DISABLE_CHARGE_FLAG, new SignedWordElement(38)),
								this.m(VictronEss.ChannelId.ESS_DISABLE_FEEDBACK_FLAG, new SignedWordElement(39)),
								this.m(VictronEss.ChannelId.SET_ACTIVE_POWER_L2, new SignedWordElement(40)),
								this.m(VictronEss.ChannelId.SET_ACTIVE_POWER_L3, new SignedWordElement(41)))
						: new FC16WriteRegistersTask(37,
								this.m(VictronEss.ChannelId.SET_ACTIVE_POWER_L1, new SignedWordElement(37)),
								this.m(VictronEss.ChannelId.ESS_DISABLE_CHARGE_FLAG, new SignedWordElement(38)),
								this.m(VictronEss.ChannelId.ESS_DISABLE_FEEDBACK_FLAG, new SignedWordElement(39)))

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

	@Override
	public Constraint[] getStaticConstraints() throws OpenemsNamedException {
		if (this.config.readOnlyMode() || !this.operationalValuesOk) {
			return new Constraint[] { this.createPowerConstraint("Read-Only-Mode", ALL, ACTIVE, EQUALS, 0),
					this.createPowerConstraint("Read-Only-Mode", ALL, REACTIVE, EQUALS, 0) };
		}
		return new Constraint[] { createPowerConstraint("NoQ", ALL, REACTIVE, EQUALS, 0) };

	}

}
