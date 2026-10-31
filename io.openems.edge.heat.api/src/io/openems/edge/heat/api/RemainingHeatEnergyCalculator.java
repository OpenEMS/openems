package io.openems.edge.heat.api;

public final class RemainingHeatEnergyCalculator {

	private static final double WATER_HEAT_CAPACITY_WH_PER_LITER_KELVIN = 1.163;

	private RemainingHeatEnergyCalculator() {
	}

	/**
	 * Calculates the thermal energy required to reach the target temperature.
	 *
	 * @param effectiveStorageVolume the effective storage volume in litres
	 * @param actualTemperature      the actual temperature in deci-degrees Celsius
	 * @param targetTemperature      the target temperature in deci-degrees Celsius
	 * @return the remaining thermal energy in Wh; {@code null} if a temperature is
	 *         unavailable
	 */
	public static Integer calculate(double effectiveStorageVolume, Integer actualTemperature,
			Integer targetTemperature) {
		if (actualTemperature == null || targetTemperature == null) {
			return null;
		}

		var deltaTemperatureDeciKelvin = Math.max(0, targetTemperature - actualTemperature);
		var deltaTemperatureKelvin = deltaTemperatureDeciKelvin / 10.0;
		return (int) Math
				.round(effectiveStorageVolume * WATER_HEAT_CAPACITY_WH_PER_LITER_KELVIN * deltaTemperatureKelvin);
	}
}
