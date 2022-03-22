package io.openems.edge.timeofusetariff.api;

import java.time.ZonedDateTime;

/**
 * Holds time of use prices for 24 h and the time when it is retrieved; //
 * prices are one value per 15 minutes; 96 values in total.
 *
 * <p>
 * Values have unit EUR/MWh.
 */
public class TimeOfUsePrices {

	public static final int NUMBER_OF_VALUES = 96;

	private final ZonedDateTime updateTime;

	private final Float[] values = new Float[NUMBER_OF_VALUES];

	/**
	 * Constructs a {@link TimeOfUsePrices}.
	 *
	 * @param updateTime Retrieved time of the prices.
	 * @param values     the 96 quarterly price values[24 hours].
	 */
	public TimeOfUsePrices(ZonedDateTime updateTime, Float... values) {
		for (var i = 0; i < NUMBER_OF_VALUES && i < values.length; i++) {
			this.values[i] = values[i];
		}
		this.updateTime = updateTime;
	}

	/**
	 * Gives electricity prices for the next 24 h; one value per 15 minutes; 96
	 * values in total.
	 *
	 * <p>
	 * E.g. if called at 10:05, the first value stands for 10:00 to 10:15; second
	 * value for 10:15 to 10:30.
	 *
	 * @return the prices
	 */
	public Float[] getValues() {
		return this.values;
	}

	/**
	 * Gets the time of the last update of prices.
	 *
	 * @return the time
	 */
	public ZonedDateTime getUpdateTime() {
		return this.updateTime;
	}

}
