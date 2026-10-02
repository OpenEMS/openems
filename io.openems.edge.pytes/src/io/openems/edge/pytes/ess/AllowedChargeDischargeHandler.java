package io.openems.edge.pytes.ess;

import static io.openems.edge.common.channel.ChannelUtils.setValue;

import org.slf4j.Logger;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.generic.common.AbstractAllowedChargeDischargeHandler;
import io.openems.edge.pytes.battery.PytesBattery;
import io.openems.edge.pytes.dccharger.PytesDcCharger;
import io.openems.edge.pytes.enums.RemoteDispatchRealtimeControlSwitch;

public class AllowedChargeDischargeHandler extends AbstractAllowedChargeDischargeHandler<PytesJs3Impl> {

	private final PytesBattery battery;
	private final PytesDcCharger dcCharger;
	private final RemoteDispatchRealtimeControlSwitch essSetpoint;
	private final Logger log;

	public AllowedChargeDischargeHandler(PytesJs3Impl parent, PytesBattery battery, PytesDcCharger dcCharger, RemoteDispatchRealtimeControlSwitch essSetpoint) {
		super(parent);
		this.battery = battery;
		this.dcCharger = dcCharger;
		this.essSetpoint = essSetpoint;
		this.log = this.parent.getLogger();
	}

	@Override
	public void accept(ClockProvider clockProvider, Battery battery, SymmetricBatteryInverter inverter) {

		if (battery == null) {

			this._setAllowedChargePower(0);
			parent._setAllowedDischargePower(0);
			return;
		}
		this.accept(clockProvider);
	}

	/**
	 * Calculates AllowedChargePower and AllowedDischargePower and sets the
	 * Channels.
	 *
	 * <p>
	 * Semantics (both derived from the BMS current limits): AllowedChargePower
	 * and AllowedDischargePower are AC-side in both modes, i.e. the range of
	 * ActivePower the solver may ask for: battery limit + PV. A controller that
	 * works on the battery (e.g. the ChargeDischargeLimiter's taper) subtracts
	 * PV again and gets the DC limit back; the raw DC limits are kept in
	 * {@code setBatteryChargeLimit()} / {@code setBatteryDischargeLimit()} for
	 * the set-point clamp.
	 *
	 * @param clockProvider a {@link ClockProvider}
	 */
	public void accept(ClockProvider clockProvider) {

		if (this.battery == null) {
		    this._setAllowedChargePower(0);
		    parent._setAllowedDischargePower(0);
		    return;

		}

		Integer batteryMaxChargeCurrent = this.battery.getBmsChargeCurrentLimit().get(); // mA
		Integer batteryMaxDischargeCurrent = this.battery.getBmsDischargeCurrentLimit().get(); // mA

		Integer batteryVoltage = this.battery.getBatteryVoltage().get(); // mV. NOT the battery nature
		
		Integer maxApparentPower = parent.getMaxApparentPower().get();

		if (batteryMaxChargeCurrent == null ||  batteryMaxDischargeCurrent == null || batteryVoltage == null || maxApparentPower == null) {
			this.parent.debugLog("[AllowChargeDischarge Handler] values not available yet, setting 0 W");

			this._setAllowedChargePower(0);
			this.parent._setAllowedDischargePower(0);
			return;
		}

		// mA -> A, rounded towards the safe side (never above the BMS limit)
		batteryMaxChargeCurrent = (int) Math.floor(batteryMaxChargeCurrent / 1000.0);
		batteryMaxDischargeCurrent = (int) Math.floor(batteryMaxDischargeCurrent / 1000.0);



		Integer configuredMaxChargeCurrent = this.battery.getConfiguredMaxChargeCurrent(); // A
		Integer configuredMaxDischargeCurrent = this.battery.getConfiguredMaxDischargeCurrent();

		// The smallest of all known limits wins (measured 2026-09-17):
		// - EMS config (battery0 maxCharge/DischargeCurrent) - enforced by us
		// - BMS request (regs 33143/33144) - the BMS only protects hard, the
		//   inverter is supposed to honour it (it exceeded it by ~10 % once)
		// - inverter storage-control setting (regs 43117/43118) - what the
		//   inverter's own control uses (e.g. 48.3 A discharge derating)
		// - inverter battery-model setting (regs 43012/43013) - 999 A placeholder
		//   with communicating lithium batteries, kept in case a battery model
		//   sets it
		int maxChargeCurrent = Math.min(configuredMaxChargeCurrent, batteryMaxChargeCurrent);
		int maxDischargeCurrent = Math.min(configuredMaxDischargeCurrent, batteryMaxDischargeCurrent);
		maxChargeCurrent = minWithInverterLimit(maxChargeCurrent,
				this.parent.channel(PytesJs3.ChannelId.STORAGE_CTRL_MAX_CHARGE_CURRENT).value().get());
		maxDischargeCurrent = minWithInverterLimit(maxDischargeCurrent,
				this.parent.channel(PytesJs3.ChannelId.STORAGE_CTRL_MAX_DISCHARGE_CURRENT).value().get());
		maxChargeCurrent = minWithInverterLimit(maxChargeCurrent,
				this.parent.channel(PytesJs3.ChannelId.INVERTER_MAX_CHARGE_CURRENT).value().get());
		maxDischargeCurrent = minWithInverterLimit(maxDischargeCurrent,
				this.parent.channel(PytesJs3.ChannelId.INVERTER_MAX_DISCHARGE_CURRENT).value().get());

		int allowedChargePower = (int) Math.min(0, Math.ceil(Math.round((maxChargeCurrent * batteryVoltage * -1) / 1000.0))); // Voltage is mV
		int allowedDischargePower = (int) Math.max(0, Math.floor(Math.round((maxDischargeCurrent * batteryVoltage) / 1000.0)));

		this.parent.debugLog("[AllowChargeDischarge Handler] max. ChargeCurrent  " + maxChargeCurrent
		+ "A maxDischargeCurrent: " + maxDischargeCurrent
		+ "A Voltage:"  + batteryVoltage
		+ "V Allowed Charge Power " + allowedChargePower
		+ "W/Allowed Discharge Power " + allowedDischargePower

		 );

		// PV production straight from the charger (ActivePower - DcDischargePower
		// is the same value one cycle later)
		int pvProduction = this.dcCharger != null ? Math.max(0, this.dcCharger.getActualPower().orElse(0)) : 0;

		this.parent.setBatteryDischargeLimit(allowedDischargePower); // raw BMS limits (DC side)
		this.parent.setBatteryChargeLimit(allowedChargePower);

		final int reportedCharge;
		final int reportedDischarge;
		if (this.essSetpoint == RemoteDispatchRealtimeControlSwitch.AC_OUTPUT_CONTROL) {
			// The inverter regulates its AC output and does not know the OpenEMS
			// battery limits, so the solver must never ask for an AC power the
			// battery cannot cover: AC = battery + PV. The bias is handled by the
			// inverter itself; the losses are subtracted on the discharge side so
			// the DC current stays below the limit.
			reportedCharge = Math.min(0, allowedChargePower + pvProduction);
			reportedDischarge = Math.max(0, allowedDischargePower
					- ApplyPowerHandler.expectedLosses(allowedDischargePower, pvProduction)) + pvProduction;
		} else {
			// Battery control: report what actually arrives on the AC side. The
			// inverter delivers BIAS_W less battery power than commanded and the
			// conversion losses sit in between (see ApplyPowerHandler). Charging
			// needs no correction (the bias works in favour there; the set-point is
			// clamped on the DC side anyway), but is AC-side as well: the AC
			// output cannot go below PV minus what the battery takes.
			reportedCharge = Math.min(0, allowedChargePower + pvProduction);
			reportedDischarge = Math.max(0, allowedDischargePower - ApplyPowerHandler.BIAS_W
					- ApplyPowerHandler.expectedLosses(allowedDischargePower, pvProduction)) + pvProduction;
		}
		// both directions are additionally capped by the inverter's apparent power
		this._setAllowedChargePower(Math.max(-maxApparentPower, reportedCharge)); // 0 or negative
		this.parent._setAllowedDischargePower(Math.min(maxApparentPower, reportedDischarge)); // positive
	}

	
	/**
	 * Applies an inverter-side current limit (mA, may be null) to a limit in A.
	 * Values of 0 (not set) and above 150 A (placeholder such as 999.0 A, the
	 * datasheet range ends at 100 A) are ignored.
	 *
	 * @param limitA         the limit so far in A
	 * @param inverterLimitMa the inverter's limit in mA or null
	 * @return the smaller limit in A
	 */
	static int minWithInverterLimit(int limitA, Object inverterLimitMa) {
		if (!(inverterLimitMa instanceof Integer ma) || ma <= 0 || ma > 150_000) {
			return limitA;
		}
		return Math.min(limitA, (int) Math.floor(ma / 1000.0));
	}

	// 2026 03 26 Helper to set allowed charge power via new method
	private void _setAllowedChargePower(int allowedChargePower) {
		setValue(this.parent, ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER,
				allowedChargePower);
	}	
	
}
