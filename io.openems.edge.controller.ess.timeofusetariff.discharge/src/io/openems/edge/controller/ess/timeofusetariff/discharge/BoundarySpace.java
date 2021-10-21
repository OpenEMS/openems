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
	 * @return the {@link BoundarySpace}
	 */
	public static BoundarySpace from(ZonedDateTime startQuarterHour, TreeMap<ZonedDateTime, Integer> productionMap,
			TreeMap<ZonedDateTime, Integer> consumptionMap, int maxStartHour, int maxEndHour) {

		ZonedDateTime proLessThanCon = null;
		ZonedDateTime proMoreThanCon = null;

		for (Entry<ZonedDateTime, Integer> entry : consumptionMap.entrySet()) {
			Integer production = productionMap.get(entry.getKey());
			Integer consumption = entry.getValue();

			if (production != null && consumption != null) {

				// Last hour of the day when Production < Consumption.
				if ((production > consumption) //
						&& (entry.getKey().getDayOfYear() == startQuarterHour.getDayOfYear())) {
					proLessThanCon = entry.getKey();
				}

				// First hour of the day when production > consumption
				if ((production > consumption) //
						&& (entry.getKey().getDayOfYear() == startQuarterHour.plusDays(1).getDayOfYear()) //
						&& (proMoreThanCon == null) //
						&& (entry.getKey().getHour() <= 10)) {
					proMoreThanCon = entry.getKey();
				}
			}
		}

		// if there is no production available, 'proLessThanCon' and 'proMoreThanCon'
		// are not calculated.
		if (proLessThanCon == null) {
			proLessThanCon = startQuarterHour.truncatedTo(ChronoUnit.DAYS) //
					.plusHours(maxEndHour);
		}

		if (proMoreThanCon == null) {
			proMoreThanCon = startQuarterHour.truncatedTo(ChronoUnit.DAYS) //
					.plusHours(maxStartHour) //
					.plusDays(1);
		}

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
}