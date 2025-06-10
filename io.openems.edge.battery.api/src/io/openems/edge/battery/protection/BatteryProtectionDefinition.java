package io.openems.edge.battery.protection;

import io.openems.edge.battery.protection.force.ForceCharge;
import io.openems.edge.battery.protection.force.ForceDischarge;
import io.openems.edge.common.linecharacteristic.PolyLine;

public interface BatteryProtectionDefinition {

	/**
	 * Defines the (estimated) maximum expected Charge current.
	 *
	 * <p>
	 * This is used as a reference for percentage values in Voltage-To-Percent and
	 * Temperature-To-Percent definitions. If during runtime a higher value is
	 * provided, that one is taken from then on.
	 *
	 * @return the (estimated) maximum expected Charge current in [A]
	 */
	public int getInitialBmsMaxEverChargeCurrent();

	/**
	 * Defines the (estimated) maximum expected Charge current.
	 *
	 * <p>
	 * This is used as a reference for percentage values in Voltage-To-Percent and
	 * Temperature-To-Percent definitions. If during runtime a higher value is
	 * provided, that one is taken from then on.
	 *
	 * @return the (estimated) maximum expected Charge current in [A]
	 */
	public int getInitialBmsMaxEverDischargeCurrent();

	/**
	 * Defines the Voltage-to-Percent limits for Charging.
	 *
	 * <p>
	 * Voltage values are in [mV], Percentage in [0,1].
	 *
	 * @return a {@link PolyLine}
	 */
	public PolyLine getChargeVoltageToPercent();

	/**
	 * Defines the Voltage-to-Percent limits for Discharging.
	 *
	 * <p>
	 * Voltage values are in [mV], Percentage in [0,1].
	 *
	 * @return a {@link PolyLine}
	 */
	public PolyLine getDischargeVoltageToPercent();

	/**
	 * Defines the Temperature-to-Percent limits for Charging.
	 *
	 * <p>
	 * Temperature values are in [degC], Percentage in [0,1].
	 *
	 * @return a {@link PolyLine}
	 */
	public PolyLine getChargeTemperatureToPercent();

	/**
	 * Defines the Temperature-to-Percent limits for Discharging.
	 *
	 * <p>
	 * Temperature values are in [degC], Percentage in [0,1].
	 *
	 * @return a {@link PolyLine}
	 */
	public PolyLine getDischargeTemperatureToPercent();

	/**
	 * Defines the SoC-to-Percent limits for Charging.
	 *
	 * <p>
	 * SoC values are in [%], Percentage in [0,1].
	 *
	 * @return a {@link PolyLine}
	 */
	public PolyLine getChargeSocToPercent();

	/**
	 * Defines the SoC-to-Percent limits for Discharging.
	 *
	 * <p>
	 * SoC values are in [%], Percentage in [0,1].
	 *
	 * @return a {@link PolyLine}
	 */
	public PolyLine getDischargeSocToPercent();

	/**
	 * Defines the parameters for Force-Discharge mode.
	 *
	 * @return the parameters
	 */
	public ForceDischarge.Params getForceDischargeParams();

	/**
	 * Defines the parameters for Force-Charge mode.
	 *
	 * @return the ForceChargeParams
	 */
	public ForceCharge.Params getForceChargeParams();

	/**
	 * Limits the maximum increase in [A] per second. Decrease is never limited for
	 * safety reasons.
	 *
	 * @return the limit or null
	 */
	public Double getMaxIncreaseAmperePerSecond();

}
