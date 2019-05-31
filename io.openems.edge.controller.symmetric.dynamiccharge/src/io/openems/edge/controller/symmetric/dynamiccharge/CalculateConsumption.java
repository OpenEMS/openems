package io.openems.edge.controller.symmetric.dynamiccharge;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.sum.Sum;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.meter.api.SymmetricMeter;

public class CalculateConsumption {

	private final Logger log = LoggerFactory.getLogger(DynamicCharge.class);
	private DynamicCharge dynamicCharge;
	private LocalDate dateOfLastRun = null;
	private LocalDate dateOfT0 = null;
	private LocalDateTime currentHour = null;
	private static TreeMap<LocalDateTime, Long> hourlyConsumption = new TreeMap<LocalDateTime, Long>();
	private static TreeMap<LocalDateTime, Float> HourlyPrices = new TreeMap<LocalDateTime, Float>();
	public static TreeMap<LocalDateTime, Long> chargeSchedule = new TreeMap<LocalDateTime, Long>();
	private static float minPrice = Float.MAX_VALUE;
	private static LocalDateTime cheapTimeStamp = null;
	public static LocalDateTime t0 = null;
	public static LocalDateTime t1 = null;
	private long totalConsumption = 0;
	private static long chargebleConsumption;
	private static long demand_Till_Cheapest_Hour;
	private static long availableCapacity;
	private static long nettCapacity;
	private static long maxApparentPower;
	private static long totalDemand;
	private static int soc;

	public CalculateConsumption(DynamicCharge dynamicCharge) {
		this.dynamicCharge = dynamicCharge;
	}

	private State currentState = State.PRODUCTION_LOWER_THAN_CONSUMPTION;

	private enum State {

		PRODUCTION_LOWER_THAN_CONSUMPTION, PRODUCTION_DROPPED_BELOW_CONSUMPTION, PRODUCTION_HIGHER_THAN_CONSUMPTION,
		PRODUCTION_EXCEEDED_CONSUMPTION
	}

	protected void run(ManagedSymmetricEss ess, SymmetricMeter meter, Config config, Sum sum) {

		long production = sum.getProductionActiveEnergy().value().orElse(0L);
		long consumption = sum.getGridActivePower().value().orElse(0);

		LocalDate nowDate = LocalDate.now();
		LocalDateTime now = LocalDateTime.now();

		/*
		 * Detect switch to next day
		 */

		if (dateOfLastRun == null || dateOfLastRun.isBefore(nowDate)) {

			/*
			 * should work on switching next day function
			 */

			t1 = null;
			log.info("New Day " + nowDate);
			log.info("t1: " + t1);
		}

		log.info("Consumption: " + consumption + " Production: " + production + " t0: " + t0 + " t1: " + t1
				+ " total Consumption: " + totalConsumption + " currenthour: " + currentHour);

		switch (currentState) {
		case PRODUCTION_LOWER_THAN_CONSUMPTION:

			if (t0 != null) {

				// First time of the day when production > consumption.
				// Avoids the fluctuations and shifts to next state only the next day.
				if ((production > consumption || now.getHour() >= config.Max_Morning_hour())
						&& dateOfT0.isBefore(nowDate)) {
					log.info(production + " is greater than " + consumption
							+ " so switching the state from PRODUCTION LOWER THAN CONSUMPTION to PRODUCTION EXCEEDING CONSUMPTION");
					this.currentState = State.PRODUCTION_EXCEEDED_CONSUMPTION;
				}

				// Detects the switching of hour
				else if (now.getHour() == currentHour.plusHours(1).getHour()) {
					log.info(" Switching of the hour detected and updating " + currentHour);
					hourlyConsumption.put(currentHour, totalConsumption);
					currentHour = now;
					totalConsumption = 0;
				}

				this.totalConsumption += consumption - production;
				log.info(" Total Consumption " + totalConsumption);

				// condition for initial run.
			} else if (production < consumption
					|| (now.getHour() >= config.Max_Morning_hour() && now.getHour() <= config.Max_Evening_hour())) {
				this.currentState = State.PRODUCTION_EXCEEDED_CONSUMPTION;
			}
			break;

		case PRODUCTION_EXCEEDED_CONSUMPTION:
			if (t1 == null && t0 != null) {

				// This is the first time of the day that "production > consumption".
				hourlyConsumption.put(currentHour, totalConsumption);
				totalDemand = hourlyConsumption.lastEntry().getValue();
				t1 = now;
				log.info(" t1 is set: " + t1);

				// reset values
				log.info("Resetting Values during " + now);
				t0 = null;
				chargeSchedule = null;
				this.dateOfLastRun = nowDate;
				log.info("dateOfLastRun " + dateOfLastRun);
				this.totalConsumption = 0;
			}

			log.info(production + " is greater than " + consumption
					+ " so switching the state from PRODUCTION EXCEEDING CONSUMPTION to PRODUCTION HIGHER THAN CONSUMPTION ");
			this.currentState = State.PRODUCTION_HIGHER_THAN_CONSUMPTION;
			break;

		case PRODUCTION_HIGHER_THAN_CONSUMPTION:

			// avoid switching to next state during the day.
			if (production < consumption && now.getHour() >= config.Max_Evening_hour()) {
				log.info(production + " is lesser than " + consumption
						+ " so switching the state from PRODUCTION HIGHER THAN CONSUMPTION to PRODUCTION DROPPED BELOW CONSUMPTION ");
				this.currentState = State.PRODUCTION_DROPPED_BELOW_CONSUMPTION;
			}
			break;

		case PRODUCTION_DROPPED_BELOW_CONSUMPTION:

			t0 = now;
			this.dateOfT0 = nowDate;
			log.info("t0 is set at: " + dateOfT0);
			currentHour = now;

			// avoids the initial run
			if (dateOfLastRun != null) {
				soc = ess.getSoc().value().orElse(0);
				nettCapacity = ess.getNetCapacity().value().orElse(0);
				maxApparentPower = ess.getMaxApparentPower().value().orElse(0);
				availableCapacity = (soc / 100) * nettCapacity;
				Prices.houlryprices();
				HourlyPrices = Prices.getHourlyPrices();
				getCheapestHoursFirst(HourlyPrices.firstKey(), HourlyPrices.lastKey());
			}

			// Resetting Values
			this.totalConsumption = 0;
			log.info(production + "is lesser than" + consumption
					+ "so switching the state from PRODUCTION DROPPED BELOW CONSUMPTION to PRODUCTION LOWER THAN CONSUMPTION");
			this.currentState = State.PRODUCTION_LOWER_THAN_CONSUMPTION;
			break;

		}

	}

	private static TreeMap<LocalDateTime, Long> getCheapestHoursFirst(LocalDateTime start, LocalDateTime end) {

		// function to find the minimum priceHour
		cheapHour(start, end);
		System.out.println("Cheap Price: " + minPrice);

		demand_Till_Cheapest_Hour = calculateDemandTillThishour(hourlyConsumption.firstKey().plusDays(1),
				cheapTimeStamp);

		/*
		 * Calculates the amount of energy that needs to be charged during the cheapest
		 * price hours.
		 */

		// if the battery has sufficient energy!
		if (availableCapacity >= demand_Till_Cheapest_Hour) {
			getCheapestHoursSecond(cheapTimeStamp, hourlyConsumption.lastKey());
		}

		// if the battery doesn't has sufficient energy!
		/*
		 * During the cheap hour, Grid is used for both charging the battery and also to
		 * satisfy the current loads
		 * (hourlyConsumption.get(cheapTimeStamp.minusDays(1))).
		 */
		chargebleConsumption = totalDemand - demand_Till_Cheapest_Hour
				- hourlyConsumption.get(cheapTimeStamp.minusDays(1));

		if (chargebleConsumption > 0) {
			if (chargebleConsumption > maxApparentPower) {
				chargebleConsumption = maxApparentPower;
				totalDemand -= chargebleConsumption;
			} else {
				totalDemand -= chargebleConsumption;
			}
			chargeSchedule.put(cheapTimeStamp, chargebleConsumption);
			getCheapestHoursFirst(HourlyPrices.firstKey(), cheapTimeStamp);
		}
		return chargeSchedule;
	}

	private static TreeMap<LocalDateTime, Long> getCheapestHoursSecond(LocalDateTime start, LocalDateTime end) {

		availableCapacity -= demand_Till_Cheapest_Hour; // This will be the capacity during cheapest hour.
		chargebleConsumption = totalDemand - availableCapacity - hourlyConsumption.get(cheapTimeStamp.minusDays(1));
		if (chargebleConsumption > 0) {
			if (chargebleConsumption > maxApparentPower) {
				if ((maxApparentPower + availableCapacity) > nettCapacity) {
					chargebleConsumption = nettCapacity - availableCapacity;
					totalDemand -= chargebleConsumption;
					chargeSchedule.put(cheapTimeStamp, chargebleConsumption);
					availableCapacity += chargebleConsumption;
					cheapHour(cheapTimeStamp, hourlyConsumption.lastKey().plusDays(1));
					demand_Till_Cheapest_Hour = calculateDemandTillThishour(cheapTimeStamp,
							hourlyConsumption.lastKey().plusDays(1));
					getCheapestHoursSecond(cheapTimeStamp, hourlyConsumption.lastKey());
				}
				chargeSchedule.put(cheapTimeStamp, maxApparentPower);
				chargebleConsumption -= maxApparentPower;
				totalDemand -= chargebleConsumption;
				availableCapacity += maxApparentPower;
				getCheapestHoursSecond(cheapTimeStamp, hourlyConsumption.lastKey());
				return chargeSchedule;
			}
			chargeSchedule.put(cheapTimeStamp, chargebleConsumption);
			return chargeSchedule;
		}
		return chargeSchedule;

	}

	private static void cheapHour(LocalDateTime start, LocalDateTime end) {
		minPrice = Float.MAX_VALUE;

		// Calculates the cheapest price hour within certain Hours.
		for (Map.Entry<LocalDateTime, Float> entry : HourlyPrices.subMap(start, end).entrySet()) {
			if (entry.getValue() < minPrice) {
				cheapTimeStamp = entry.getKey();
				minPrice = entry.getValue();
			}
		}
	}

	private static long calculateDemandTillThishour(LocalDateTime start, LocalDateTime end) {
		long demand = 0;
		for (Entry<LocalDateTime, Long> entry : hourlyConsumption.entrySet()) {
			if ((entry.getKey().plusDays(1).isEqual(start) || entry.getKey().plusDays(1).isAfter(start))
					&& entry.getKey().plusDays(1).isBefore(end)) {
				demand += entry.getValue();
			}
		}
		return demand;
	}

	/*
	 * private static TreeMap<LocalDateTime, Long> getCheapestHours(LocalDateTime
	 * end) {
	 * 
	 * // function to find the minimum priceHour cheapHour(HourlyPrices.firstKey(),
	 * end); System.out.println("Cheap Price: " + minPrice);
	 * 
	 * 
	 * Calculates the amount of energy that needs to be charged during the cheapest
	 * price hours.
	 * 
	 * 
	 * for (Entry<LocalDateTime, Long> entry : hourlyConsumption.entrySet()) {
	 * 
	 * if (entry.getKey().getHour() == cheapTimeStamp.getHour()) {
	 * 
	 * demand_Till_Cheapest_Hour = entry.getValue(); long chargebleConsumption;
	 * 
	 * // if the battery has sufficient energy! if (availableCapacity >=
	 * demand_Till_Cheapest_Hour) { chargebleConsumption = totalDemand -
	 * availableCapacity; if (chargebleConsumption > maxApparentPower) {
	 * chargeSchedule.put(cheapTimeStamp, maxApparentPower);
	 * 
	 * long remainingConsumption = chargebleConsumption - maxApparentPower;
	 * cheapHour(cheapTimeStamp, HourlyPrices.lastKey()); for (Entry<LocalDateTime,
	 * Long> entry1 : hourlyConsumption.entrySet()) { if (entry1.getKey().getHour()
	 * == cheapTimeStamp.getHour()) {
	 * 
	 * } }
	 * 
	 * 
	 * } chargeSchedule.put(cheapTimeStamp, chargebleConsumption); return
	 * chargeSchedule; }
	 * 
	 * // if the battery doesn't has sufficient energy! chargebleConsumption =
	 * totalDemand - demand_Till_Cheapest_Hour; totalDemand -= chargebleConsumption;
	 * 
	 * 
	 * During the cheap hour, Grid is used for both charging the battery and also to
	 * satisfy the current loads.
	 * 
	 * 
	 * chargeSchedule.put(cheapTimeStamp, chargebleConsumption); end =
	 * cheapTimeStamp; getCheapestHours(end); } } return chargeSchedule; }
	 */
}
