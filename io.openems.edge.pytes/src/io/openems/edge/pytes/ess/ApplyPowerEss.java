package io.openems.edge.pytes.ess;

import org.slf4j.Logger;

import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;

/**
 * What {@link ApplyPowerHandler} needs from the ESS. Extracted from
 * {@link PytesJs3Impl} so the handler can be unit-tested against a fake.
 */
interface ApplyPowerEss extends PytesJs3, ManagedSymmetricEss, HybridEss {

	/**
	 * Gets the {@link Logger} of the ESS.
	 *
	 * @return the Logger
	 */
	Logger getLogger();

	/**
	 * Gets the configured cycle time in [ms].
	 *
	 * @return the cycle time
	 */
	int getCycleTime();

	/**
	 * Gets the raw battery discharge limit in W (BMS/config, DC side).
	 *
	 * @return the limit in W, positive
	 */
	int getBatteryDischargeLimit();

	/**
	 * Gets the raw battery charge limit in W (BMS/config, DC side).
	 *
	 * @return the limit in W, negative or 0
	 */
	int getBatteryChargeLimit();

	/**
	 * Gets the grid feed-in limit to be written into the inverter.
	 *
	 * @return the limit in W, or null for no limitation
	 */
	Integer getGridFeedInLimit();

	/**
	 * Gets the failsafe timeout for the remote dispatch (reg 44101).
	 *
	 * @return minutes, 1..1440
	 */
	int getFailsafeMinutes();

	/**
	 * Gets the grid meter power.
	 *
	 * @return the power in W (negative = export), or null without meter
	 */
	Integer getGridPower();

	/**
	 * Whether the dynamic feed-in limitation (reg 43052) currently curtails PV.
	 *
	 * @return true while limiting
	 */
	boolean isPvLimitActive();

	/**
	 * Logs a debug message.
	 *
	 * @param message the message
	 */
	void debugLog(String message);
}
