package io.openems.edge.battery.protection;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.linecharacteristic.PolyLine;
import io.openems.edge.common.type.TypeUtils;

public abstract class AbstractMaxCurrentHandler {

	public abstract static class Builder<T extends Builder<?>> {
		protected final ClockProvider clockProvider;
		protected final int initialBmsMaxEverCurrent;

		protected PolyLine voltageToPercent = PolyLine.empty();
		protected PolyLine temperatureToPercent = PolyLine.empty();
		protected Double maxIncreasePerSecond = null;

		protected Builder(ClockProvider clockProvider, int initialBmsMaxEverCurrent) {
			this.clockProvider = clockProvider;
			this.initialBmsMaxEverCurrent = initialBmsMaxEverCurrent;
		}

		public T setVoltageToPercent(PolyLine voltageToPercent) {
			this.voltageToPercent = voltageToPercent;
			return this.self();
		}

		public T setTemperatureToPercent(PolyLine temperatureToPercent) {
			this.temperatureToPercent = temperatureToPercent;
			return this.self();
		}

		public T setMaxIncreasePerSecond(double maxIncreasePerSecond) {
			this.maxIncreasePerSecond = maxIncreasePerSecond;
			return this.self();
		}

		protected abstract T self();
	}

	protected final ClockProvider clockProvider;
	protected final PolyLine voltageToPercent;
	protected final PolyLine temperatureToPercent;

	protected int bmsMaxEverCurrent;

	// used by 'getMaxIncreaseAmpereLimit()'
	private final Double maxIncreasePerSecond;
	Instant lastResultTimestamp = null;
	Double lastMaxIncreaseAmpereLimit = null;

	// used by 'getVoltageToPercentLimit()'
	private Map<VOLTAGE_REF, Double> activeCellVoltageToPercentLimit = new EnumMap<>(VOLTAGE_REF.class);

	protected AbstractMaxCurrentHandler(ClockProvider clockProvider, int initialBmsMaxEverCurrent,
			PolyLine voltageToPercent, PolyLine temperatureToPercent, Double maxIncreasePerSecond) {
		this.clockProvider = clockProvider;
		this.bmsMaxEverCurrent = initialBmsMaxEverCurrent;

		this.voltageToPercent = voltageToPercent;
		this.temperatureToPercent = temperatureToPercent;

		this.maxIncreasePerSecond = maxIncreasePerSecond;
	}

	/**
	 * Calculates the actual allowed current limit in [A] as minimum of:.
	 * 
	 * <ul>
	 * <li>Allowed Current Limit provided by Battery Management System
	 * <li>Voltage-to-Percent characteristics for Min-Cell-Voltage
	 * <li>Voltage-to-Percent characteristics for Max-Cell-Voltage
	 * <li>Temperature-to-Percent characteristics for Min-Cell-Temperature
	 * <li>Temperature-to-Percent characteristics for Max-Cell-Temperature
	 * <li>Applied max increase limit (e.g. 0.5 A per second)
	 * <li>Force Charge/Discharge mode (e.g. -1 A to enforce charge/discharge)
	 * </ul>
	 * 
	 * @param minCellVoltage     the actual Min-Cell-Voltage of the battery,
	 *                           possible null
	 * @param maxCellVoltage     the actual Max-Cell-Voltage of the battery,
	 *                           possible null
	 * @param minCellTemperature the actual Min-Cell-Temperature of the battery,
	 *                           possible null
	 * @param maxCellTemperature the actual Max-Cell-Temperature of the battery,
	 *                           possible null
	 * @param bmsMaxCurrent      the actual Max-Current-Limit of the battery
	 * @return the actual allowed current limit, mathematically rounded to [A]
	 */
	protected synchronized int calculateCurrentLimit(Integer minCellVoltage, Integer maxCellVoltage,
			Integer minCellTemperature, Integer maxCellTemperature, Integer bmsMaxCurrent) {
		// Update 'bmsMaxEverAllowedCurrent'
		this.bmsMaxEverCurrent = TypeUtils.max(this.bmsMaxEverCurrent, bmsMaxCurrent);


		// Get the minimum limit of all limits in Ampere
		Double limit = TypeUtils.min(//

				// Original 'AllowedCurrent' by the BMS
				TypeUtils.toDouble(bmsMaxCurrent),

				// Calculate Ampere limit for Min-Cell-Voltage
				this.getCellVoltageToPercentLimit(VOLTAGE_REF.MIN_CELL, minCellVoltage),

				// Calculate Ampere limit for Max-Cell-Voltage
				this.getCellVoltageToPercentLimit(VOLTAGE_REF.MAX_CELL, maxCellVoltage),

				// Calculate Ampere limit for Min-Cell-Temperature
				this.percentToAmpere(this.temperatureToPercent.getValue(minCellTemperature)),

				// Calculate Ampere limit for Min-Cell-Temperature
				this.percentToAmpere(this.temperatureToPercent.getValue(maxCellTemperature)),

				// Calculate Max Increase Ampere Limit
				this.getMaxIncreaseAmpereLimit(),

				// Calculate Force Current
				this.getForceCurrent(minCellVoltage, maxCellVoltage) //
		);

		// No limit? Set '0' to block charge/discharge
		if (limit == null) {
			limit = 0.;
		}

		this.lastMaxIncreaseAmpereLimit = limit;

		return (int) Math.round(limit);
	}

	protected static enum VOLTAGE_REF {
		MIN_CELL, MAX_CELL;
	}

	/**
	 * Calculates the current limit based on Min-/Max-Cell-Voltage according to the
	 * 'voltageToPercent' characteristics.
	 * 
	 * <p>
	 * If for the given 'cellVoltage' value 'voltageToPercent' defines a limitation
	 * (i.e. the given percentage is less than 100 %), that limitation stays active
	 * until a future 'cellVoltage' results in no limitation (i.e. percentage == 100
	 * %). This is implemented to reduce fluctuations due to physical effects in the
	 * battery.
	 * 
	 * @param voltageRef
	 * @param cellVoltage
	 * @return
	 */
	protected synchronized Double getCellVoltageToPercentLimit(VOLTAGE_REF voltageRef, Integer cellVoltage) {
		if (cellVoltage == null) {
			return null;
		}
		Double percentage = this.voltageToPercent.getValue(cellVoltage);
		if (percentage == null) {
			return null;
		}

		double thisCurrent = this.percentToAmpere(percentage);
		final double result;
		if (percentage > 0.9999) {
			// We are in the 'no limitation' zone of the PolyLine -> unset all limitations
			result = thisCurrent;
			this.activeCellVoltageToPercentLimit.put(voltageRef, null);

		} else {
			// Current limit is active -> from now on only reduction of the limit is allowed
			result = TypeUtils.min(this.activeCellVoltageToPercentLimit.get(voltageRef), thisCurrent);
			this.activeCellVoltageToPercentLimit.put(voltageRef, result);
		}
		return result;
	}

	/**
	 * Calculates the maximum increase limit in Ampere from the
	 * 'maxIncreasePerSecond' parameter.
	 * 
	 * <p>
	 * If maxIncreasePerSecond is 0.5, last limit was 10 A and 1 second passed, this
	 * method returns 10.5.
	 * 
	 * @return the limit or null
	 */
	protected synchronized Double getMaxIncreaseAmpereLimit() {
		if (this.maxIncreasePerSecond == null) {
			return null;
		}
		Instant now = Instant.now(this.clockProvider.getClock());
		final Double result;
		if (this.lastResultTimestamp != null && this.lastMaxIncreaseAmpereLimit != null) {
			result = this.lastMaxIncreaseAmpereLimit
					+ (Duration.between(this.lastResultTimestamp, now).toMillis() * maxIncreasePerSecond) //
							/ 1000.; // convert [mA] to [A]
		} else {
			result = null;
		}
		this.lastResultTimestamp = now;
		return result;
	}

	/**
	 * Calculates Current in Force-Mode.
	 * 
	 * @param minCellVoltage the Min-Cell-Voltage, possibly null
	 * @param maxCellVoltage the Max-Cell-Voltage, possibly null
	 * @return the Current, possibly null
	 */
	protected abstract Double getForceCurrent(Integer minCellVoltage, Integer maxCellVoltage);

	/**
	 * Convert a Percent value to a concrete Ampere value in [A] by multiplying it
	 * with 'bmsMaxEverAllowedChargeCurrent'.
	 * 
	 * <ul>
	 * <li>null % -> null
	 * <li>0 % -> 0
	 * <li>anything else -> calculate percent; at least '1 A'.
	 * 
	 * @param percent the percent value in [0,1]
	 * @return the ampere value in [A]
	 */
	protected Double percentToAmpere(Double percent) {
		if (percent == null) {
			return null;
		} else if (percent == 0.) {
			return 0.;
		} else {
			return Math.max(1., this.bmsMaxEverCurrent * percent);
		}
	}

}