package io.openems.edge.controller.ess.timeofusetariff.discharge;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map.Entry;
import java.util.TreeMap;

class BoundarySpace {
	final ZonedDateTime proLessThanCon; // Sunset
	final ZonedDateTime proMoreThanCon; // Sunrise

	/**
	 * Factory method for {@link BoundarySpace}: calculates the boundary space
	 * within which the schedule logic works.
	 *
	 * @param startQuarterHour {@link ZonedDateTime} start time of the prediction
	 * @param productionMap    predictions for production
	 * @param consumptionMap   predictions for consumption
	 * @param maxStartHour     fallback Morning-Hour from {@link Config}
	 * @param maxEndHour       fallback Evening-Hour from {@link Config}
	 * @param bufferMinutes    Number of minutes, sunrise to be adjusted.
	 * @return the {@link BoundarySpace}
	 */
	public static BoundarySpace from(ZonedDateTime startQuarterHour, TreeMap<ZonedDateTime, Integer> productionMap,
			TreeMap<ZonedDateTime, Integer> consumptionMap, int maxStartHour, int maxEndHour, int bufferMinutes) {

		ZonedDateTime proLessThanCon = null;
		ZonedDateTime proMoreThanCon = null;

		for (Entry<ZonedDateTime, Integer> entry : consumptionMap.entrySet()) {
			var production = productionMap.get(entry.getKey());
			var consumption = entry.getValue();

			if (production != null && consumption != null) {

				final ZonedDateTime start;
				if (isBeforeMidnight(startQuarterHour.getHour())) {
					// Last hour of the day when Production < Consumption.
					if (production > consumption //
							&& entry.getKey().getDayOfYear() == startQuarterHour.getDayOfYear()
							&& entry.getKey().getHour() >= 14) {
						proLessThanCon = entry.getKey(); // Sunset
					}

					start = startQuarterHour.plusDays(1);

				} else {
					start = startQuarterHour;
				}

				// First hour of the day when production > consumption
				if (production > consumption //
						&& entry.getKey().getDayOfYear() == start.getDayOfYear() //
						&& proMoreThanCon == null //
						&& entry.getKey().getHour() <= 10) {
					proMoreThanCon = entry.getKey(); // Sunrise
				}
			}
		}

		// if there is no production available, 'proLessThanCon' and 'proMoreThanCon'
		// are not calculated.
		if (proLessThanCon == null) {
			// Sunset
			final ZonedDateTime start;
			if (isBeforeMidnight(startQuarterHour.getHour())) {
				start = startQuarterHour;
			} else {
				start = startQuarterHour.minusDays(1);
			}
			proLessThanCon = start.truncatedTo(ChronoUnit.DAYS) //
					.plusHours(maxEndHour);
		}
		if (proMoreThanCon == null) {
			// Sunrise
			final ZonedDateTime start;
			if (isBeforeMidnight(startQuarterHour.getHour())) {
				start = startQuarterHour.plusDays(1);
			} else {
				start = startQuarterHour;
			}
			proMoreThanCon = start.truncatedTo(ChronoUnit.DAYS) //
					.plusHours(maxStartHour);
		}

		// adjust sunrise according to the buffer minutes.
		proMoreThanCon.minusMinutes(bufferMinutes);

		return new BoundarySpace(proLessThanCon, proMoreThanCon);
	}

	private BoundarySpace(ZonedDateTime proLessThanCon, ZonedDateTime proMoreThanCon) {
		this.proLessThanCon = proLessThanCon;
		this.proMoreThanCon = proMoreThanCon;
	}

	/**
	 * Is the given date between the boundaries?.
	 *
	 * @param now the given date
	 * @return true if it is within the boundaries
	 */
	public boolean isWithinBoundary(ZonedDateTime now) {
		if (now.isBefore(this.proLessThanCon)) {
			return false;
		}
		if (now.isAfter(this.proMoreThanCon)) {
			return false;
		}
		return true;
	}

	/**
	 * Is the given hour before or after the midnight?.
	 *
	 * @param hour the given hour
	 * @return true if it is before the midnight.
	 */
	public static boolean isBeforeMidnight(int hour) {
		if (hour >= 10 && hour <= 23) {
			return true;
		}
		return false;
	}
}