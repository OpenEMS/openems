package io.openems.edge.battery.protection.currenthandler;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.protection.BatteryProtection;
import io.openems.edge.battery.protection.BatteryProtection.ChannelId;
import io.openems.edge.battery.protection.force.AbstractForceChargeDischarge;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.linecharacteristic.PolyLine;
import io.openems.edge.common.type.TypeUtils;

public abstract class AbstractMaxCurrentHandler {

	public abstract static class Builder<T extends Builder<?>> {
		protected final ClockProvider clockProvider;
		protected final int initialBmsMaxEverCurrent;

		protected PolyLine voltageToPercent = PolyLine.empty();
		protected PolyLine temperatureToPercent = PolyLine.empty();
		protected PolyLine socToPercent = PolyLine.empty();
		protected Double maxIncreasePerSecond = null;

		/**
		 * Creates a {@link Builder} for {@link AbstractMaxCurrentHandler}.
		 *
		 * @param clockProvider            a {@link ClockProvider}, mainly for JUnit
		 *                                 tests
		 * @param initialBmsMaxEverCurrent the (estimated) maximum allowed current. This
		 *                                 is used as a reference for percentage values.
		 *                                 If during runtime a higher value is provided,
		 *                                 that one is taken from then on.
		 */
		protected Builder(ClockProvider clockProvider, int initialBmsMaxEverCurrent) {
			this.clockProvider = clockProvider;
			this.initialBmsMaxEverCurrent = initialBmsMaxEverCurrent;
		}

		/**
		 * Sets the Voltage-To-Percent characteristics.
		 *
		 * @param voltageToPercent the {@link PolyLine}
		 * @return a {@link Builder}
		 */
		public T setVoltageToPercent(PolyLine voltageToPercent) {
			this.voltageToPercent = voltageToPercent;
			return this.self();
		}

		/**
		 * Sets the Temperature-To-Percent characteristics.
		 *
		 * @param temperatureToPercent the {@link PolyLine}
		 * @return a {@link Builder}
		 */
		public T setTemperatureToPercent(PolyLine temperatureToPercent) {
			this.temperatureToPercent = temperatureToPercent;
			return this.self();
		}

		/**
		 * Sets the SoC-To-Percent characteristics.
		 *
		 * @param socToPercent the {@link PolyLine}
		 * @return a {@link Builder}
		 */
		public T setSocToPercent(PolyLine socToPercent) {
			this.socToPercent = socToPercent;
			return this.self();
		}

		/**
		 * Sets the Max-Increase-Per-Second parameter in [A].
		 *
		 * @param maxIncreasePerSecond value in [A] per Second.
		 * @return a {@link Builder}
		 */
		public T setMaxIncreasePerSecond(double maxIncreasePerSecond) {
			this.maxIncreasePerSecond = maxIncreasePerSecond;
			return this.self();
		}

		protected abstract T self();
	}

	protected final ClockProvider clockProvider;
	protected final PolyLine voltageToPercent;
	protected final PolyLine temperatureToPercent;
	protected final PolyLine socToPercent;
	protected final AbstractForceChargeDischarge forceChargeDischarge;

	protected int bmsMaxEverCurrent;

	// used by 'getMaxIncreaseAmpereLimit()'
	private final Double maxIncreasePerSecond;
	protected Instant lastResultTimestamp = null;
	protected Double lastCurrentLimit = null;

	protected AbstractMaxCurrentHandler(ClockProvider clockProvider, int initialBmsMaxEverCurrent,
			PolyLine voltageToPercent, PolyLine temperatureToPercent, PolyLine socToPercent,
			Double maxIncreasePerSecond, AbstractForceChargeDischarge forceChargeDischarge) {
		this.clockProvider = clockProvider;
		this.bmsMaxEverCurrent = initialBmsMaxEverCurrent;
		this.voltageToPercent = voltageToPercent;
		this.temperatureToPercent = temperatureToPercent;
		this.socToPercent = socToPercent;
		this.maxIncreasePerSecond = maxIncreasePerSecond;
		this.forceChargeDischarge = forceChargeDischarge;
	}

	/**
	 * Gets the ChannelId for Battery-Protection Limit originating from BMS.
	 *
	 * <ul>
	 * <li>{@link ChannelId#BP_CHARGE_BMS}
	 * <li>{@link ChannelId#BP_DISCHARGE_BMS}
	 * </ul>
	 *
	 * @return the {@link ChannelId}
	 */
	protected abstract ChannelId getBpBmsChannelId();

	/**
	 * Gets the ChannelId for Battery-Protection Limit by Min-Cell-Voltage.
	 *
	 * <ul>
	 * <li>{@link ChannelId#BP_CHARGE_MIN_VOLTAGE}
	 * <li>{@link ChannelId#BP_DISCHARGE_MIN_VOLTAGE}
	 * </ul>
	 *
	 * @return the {@link ChannelId}
	 */
	protected abstract ChannelId getBpMinVoltageChannelId();

	/**
	 * Gets the ChannelId for Battery-Protection Limit by Max-Cell-Voltage.
	 *
	 * <ul>
	 * <li>{@link ChannelId#BP_CHARGE_MAX_VOLTAGE}
	 * <li>{@link ChannelId#BP_DISCHARGE_MAX_VOLTAGE}
	 * </ul>
	 *
	 * @return the {@link ChannelId}
	 */
	protected abstract ChannelId getBpMaxVoltageChannelId();

	/**
	 * Gets the ChannelId for Battery-Protection Limit by Min-Cell-Temperature.
	 *
	 * <ul>
	 * <li>{@link ChannelId#BP_CHARGE_MIN_TEMPERATURE}
	 * <li>{@link ChannelId#BP_DISCHARGE_MIN_TEMPERATURE}
	 * </ul>
	 *
	 * @return the {@link ChannelId}
	 */
	protected abstract ChannelId getBpMinTemperatureChannelId();

	/**
	 * Gets the ChannelId for Battery-Protection Limit by Max-Cell-Temperature.
	 *
	 * <ul>
	 * <li>{@link ChannelId#BP_CHARGE_MAX_TEMPERATURE}
	 * <li>{@link ChannelId#BP_DISCHARGE_MAX_TEMPERATURE}
	 * </ul>
	 *
	 * @return the {@link ChannelId}
	 */
	protected abstract ChannelId getBpMaxTemperatureChannelId();

	/**
	 * Gets the ChannelId for Battery-Protection Limit by state of charge.
	 *
	 * <ul>
	 * <li>{@link ChannelId#BP_CHARGE_MAX_SOC}
	 * <li>{@link ChannelId#BP_DISCHARGE_MAX_SOC}
	 * </ul>
	 *
	 * @return the {@link ChannelId}
	 */
	protected abstract ChannelId getBpMaxSocChannelId();

	/**
	 * Gets the ChannelId for Battery-Protection Limit by Force Charge/Discharge
	 * Mode.
	 *
	 * <ul>
	 * <li>{@link ChannelId#BP_FORCE_CHARGE}
	 * <li>{@link ChannelId#BP_FORCE_DISCHARGE}
	 * </ul>
	 *
	 * @return the {@link ChannelId}
	 */
	protected abstract ChannelId getBpForceCurrentChannelId();

	/**
	 * Gets the ChannelId for Battery-Protection Limit by Max-Increase-Ampere ramp.
	 * Mode.
	 *
	 * <ul>
	 * <li>{@link ChannelId#BP_FORCE_CHARGE}
	 * <li>{@link ChannelId#BP_FORCE_DISCHARGE}
	 * </ul>
	 *
	 * @return the {@link ChannelId}
	 */
	protected abstract ChannelId getBpMaxIncreaseAmpereChannelId();

	/**
	 * Calculates the actual allowed current limit in [A] as minimum of:.
	 *
	 * <ul>
	 * <li>Is the battery started? (block any charge/discharge if not)
	 * <li>Is there any value from the BMS? (block any charge/discharge if not)
	 * <li>Allowed Current Limit provided by Battery Management System
	 * <li>Voltage-to-Percent characteristics for Min-Cell-Voltage
	 * <li>Voltage-to-Percent characteristics for Max-Cell-Voltage
	 * <li>Temperature-to-Percent characteristics for Min-Cell-Temperature
	 * <li>Temperature-to-Percent characteristics for Max-Cell-Temperature
	 * <li>SoC-to-Percent characteristics for SoC limitations
	 * <li>Applied max increase limit (e.g. 0.5 A per second)
	 * <li>Force Charge/Discharge mode (e.g. -1 A to enforce charge/discharge)
	 * </ul>
	 *
	 * @param battery the {@link Battery}
	 * @return the actual allowed current limit, mathematically rounded to [A]
	 */
	public synchronized int calculateCurrentLimit(Battery battery) {
		// Read input parameters from Battery
		var minCellVoltage = battery.getMinCellVoltage().get();
		var maxCellVoltage = battery.getMaxCellVoltage().get();
		var minCellTemperature = battery.getMinCellTemperature().get();
		var maxCellTemperature = battery.getMaxCellTemperature().get();
		var soc = battery.getSoc().get();
		IntegerReadChannel bpBmsChannel = battery.channel(this.getBpBmsChannelId());
		var bpBms = bpBmsChannel.value().get();

		// Update 'bmsMaxEverAllowedCurrent'
		this.bmsMaxEverCurrent = TypeUtils.max(this.bmsMaxEverCurrent, bpBms);

		/*
		 * Get all limits
		 */
		// Calculate Ampere limit for Min-Cell-Voltage
		final var minCellVoltageLimit = this.getMinCellVoltageToPercentLimit(minCellVoltage);
		// Calculate Ampere limit for Max-Cell-Voltage
		final var maxCellVoltageLimit = this.getMaxCellVoltageToPercentLimit(maxCellVoltage);
		// Calculate Ampere limit for Min-Cell-Temperature
		final var minCellTemperatureLimit = this
				.percentToAmpere(this.temperatureToPercent.getValue(minCellTemperature));
		// Calculate Ampere limit for Max-Cell-Temperature
		final var maxCellTemperatureLimit = this
				.percentToAmpere(this.temperatureToPercent.getValue(maxCellTemperature));
		// Calculate Ampere limit for State of Charge
		final var maxSocLimit = this.percentToAmpere(this.socToPercent.getValue(soc));
		// Calculate Max Increase Ampere Limit
		final var maxIncreaseAmpereLimit = this.getMaxIncreaseAmpereLimit();
		// Calculate Force Current
		final var forceCurrent = this.getForceCurrent(minCellVoltage, maxCellVoltage);

		/*
		 * Store limits in Channels. If value is 'null', store the bmsMaxEverCurrent
		 */
		battery.channel(BatteryProtection.ChannelId.BP_MAX_EVER_CURRENT).setNextValue(this.bmsMaxEverCurrent);
		battery.channel(this.getBpMinVoltageChannelId())
				.setNextValue(TypeUtils.orElse(minCellVoltageLimit, this.bmsMaxEverCurrent));
		battery.channel(this.getBpMaxVoltageChannelId())
				.setNextValue(TypeUtils.orElse(maxCellVoltageLimit, this.bmsMaxEverCurrent));
		battery.channel(this.getBpMinTemperatureChannelId())
				.setNextValue(TypeUtils.orElse(minCellTemperatureLimit, this.bmsMaxEverCurrent));
		battery.channel(this.getBpMaxTemperatureChannelId())
				.setNextValue(TypeUtils.orElse(maxCellTemperatureLimit, this.bmsMaxEverCurrent));
		battery.channel(this.getBpMaxSocChannelId())
				.setNextValue(TypeUtils.orElse(maxSocLimit, this.bmsMaxEverCurrent));
		battery.channel(this.getBpMaxIncreaseAmpereChannelId())
				.setNextValue(TypeUtils.orElse(maxIncreaseAmpereLimit, this.bmsMaxEverCurrent));
		battery.channel(this.getBpForceCurrentChannelId())
				.setNextValue(TypeUtils.orElse(forceCurrent, this.bmsMaxEverCurrent));

		// Get the minimum limit of all limits in Ampere
		var limit = TypeUtils.min(TypeUtils.toDouble(bpBms), minCellVoltageLimit, maxCellVoltageLimit,
				minCellTemperatureLimit, maxCellTemperatureLimit, maxIncreaseAmpereLimit, forceCurrent);

		// Set '0' to block charge/discharge?
		if (
		// Battery not started?
		!battery.isStarted()
				// No limit?
				|| limit == null
				// No value from BMS and no force charge/discharge?
				|| limit > 0 && bpBms == null //
		) {
			limit = 0.;
		}

		this.lastCurrentLimit = limit;

		return (int) Math.round(limit);
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
	 * <p>
	 * This method internally uses the abstract
	 * {@link #getActiveCellVoltageToPercentLimit()} method to distinguish between
	 * active charge/discharge limitations.
	 *
	 * @param activeLimit the currently active limit
	 * @param cellVoltage the cell-voltage
	 * @return the Cell-Voltage-To-Percent Limit
	 */
	private synchronized Double getCellVoltageToPercentLimit(AtomicReference<Double> activeLimit, Integer cellVoltage) {
		if (cellVoltage == null) {
			return null;
		}
		var percentage = this.voltageToPercent.getValue(cellVoltage);
		if (percentage == null) {
			return null;
		}

		double thisCurrent = this.percentToAmpere(percentage);
		final double result;
		if (percentage > Math.nextDown(1)) {
			// We are in the 'no limitation' zone of the PolyLine -> unset all limitations
			result = thisCurrent;
			activeLimit.set(null);

		} else {
			// Current limit is active -> from now on only reduction of the limit is allowed
			activeLimit.getAndUpdate(activeCurrent -> //
			TypeUtils.min(activeCurrent, thisCurrent));
			result = activeLimit.get();
		}
		return result;
	}

	private final AtomicReference<Double> activeMinCellVoltageToPercentLimit = new AtomicReference<>();

	protected Double getMinCellVoltageToPercentLimit(Integer minCellVoltage) {
		return this.getCellVoltageToPercentLimit(this.activeMinCellVoltageToPercentLimit, minCellVoltage);
	}

	private final AtomicReference<Double> activeMaxCellVoltageToPercentLimit = new AtomicReference<>();

	protected Double getMaxCellVoltageToPercentLimit(Integer minCellVoltage) {
		return this.getCellVoltageToPercentLimit(this.activeMaxCellVoltageToPercentLimit, minCellVoltage);
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
		var now = Instant.now(this.clockProvider.getClock());
		final Double result;
		if (this.lastResultTimestamp != null && this.lastCurrentLimit != null) {
			result = this.lastCurrentLimit
					+ Duration.between(this.lastResultTimestamp, now).toMillis() * this.maxIncreasePerSecond //
							/ 1000.; // convert [mA] to [A]
		} else {
			result = 0.;
		}
		this.lastResultTimestamp = now;
		return result;
	}

	/**
	 * Calculates the Ampere limit in Force Charge/Discharge mode. Returns:
	 *
	 * <ul>
	 * <li>-1 -> in force charge/discharge mode
	 * <li>0 -> in block discharge/charge mode
	 * <li>null -> otherwise
	 * </ul>
	 *
	 * @param minCellVoltage the Min-Cell-Voltage, possibly null
	 * @param maxCellVoltage the Max-Cell-Voltage, possibly null
	 * @return the Current, possibly null
	 */
	protected Double getForceCurrent(Integer minCellVoltage, Integer maxCellVoltage) {
		if (this.forceChargeDischarge == null) {
			return null;
		}

		final AbstractForceChargeDischarge.State state;

		// Apply State-Machine
		if (minCellVoltage == null || maxCellVoltage == null) {
			this.forceChargeDischarge.forceNextState(AbstractForceChargeDischarge.State.UNDEFINED);
			state = AbstractForceChargeDischarge.State.UNDEFINED;

		} else {
			try {
				this.forceChargeDischarge.run(
						new AbstractForceChargeDischarge.Context(this.clockProvider, minCellVoltage, maxCellVoltage));
			} catch (OpenemsNamedException e) {
				e.printStackTrace();
			}
			state = this.forceChargeDischarge.getCurrentState();
		}

		// Evaluate force charge/discharge current from current state
		switch (state) {
		case UNDEFINED:
		case WAIT_FOR_FORCE_MODE:
			return null;
		case FORCE_MODE:
			// TODO Plan is making the value adaptive, i.e. start with 1 A; if voltage still
			// decreases, then slowly increase force charge current.
			return -2.;
		case BLOCK_MODE:
			return 0.;
		}
		// will never happen
		return null;
	}

	/**
	 * Convert a Percent value to a concrete Ampere value in [A] by multiplying it
	 * with 'bmsMaxEverAllowedChargeCurrent'.
	 *
	 * <ul>
	 * <li>null % -> null
	 * <li>0 % -> 0
	 * <li>anything else -> calculate percent; at least '1 A'.
	 * </ul>
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