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
	private static long availableCapacity;
	private static long nettCapacity;
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

		long production = meter.getActivePower().value().orElse(0);
		long consumption = sum.getConsumptionActivePower().value().orElse(0)
				+ sum.getEssActivePower().value().orElse(0);

		LocalDate nowDate = LocalDate.now();
		LocalDateTime now = LocalDateTime.now();
		// int secondOfDay = now.getSecond() + now.getMinute() * 60 + now.getHour() *
		// 3600;

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

		log.info("Consumption: " + consumption + " Total  production: " + production);

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

				/*// First time of the day when production > consumption.
				// Avoids the fluctuations and shifts to next state only the next day.
				if (production > consumption && dateOfT0.isBefore(nowDate)) {
					log.info(production + " is greater than " + consumption
							+ " so switching the state from PRODUCTION LOWER THAN CONSUMPTION to PRODUCTION EXCEEDING CONSUMPTION");
					this.currentState = State.PRODUCTION_EXCEEDED_CONSUMPTION;
				}

				// shifts to next state when there is no production available.
				else if (now.getHour() >= config.Max_Morning_hour() && dateOfT0.isBefore(nowDate)) {
					log.info(production + " is greater than " + consumption
							+ " so switching the state from PRODUCTION LOWER THAN CONSUMPTION to PRODUCTION EXCEEDING CONSUMPTION");
					this.currentState = State.PRODUCTION_EXCEEDED_CONSUMPTION;
				}*/

				// Detects the switching of hour
				else if (now.getHour() == currentHour.plusHours(1).getHour() && dateOfT0.isBefore(nowDate)) {
					hourlyConsumption.put(currentHour, totalConsumption);
					currentHour = now;
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
				Prices.houlryprices();
				HourlyPrices = Prices.getHourlyPrices();
				getCheapestHours(HourlyPrices.lastKey());
			}

			// Resetting Values
			this.totalConsumption = 0;
			log.info(production + "is lesser than" + consumption
					+ "so switching the state from PRODUCTION DROPPED BELOW CONSUMPTION to PRODUCTION LOWER THAN CONSUMPTION");
			this.currentState = State.PRODUCTION_LOWER_THAN_CONSUMPTION;
			break;

		}

	}

	private static TreeMap<LocalDateTime, Long> getCheapestHours(LocalDateTime end) {

		Long demand_Till_Cheapest_Hour;

		availableCapacity = (soc / 100) * nettCapacity;

		// Calculates the cheapest price hour within certain Hours.
		for (Map.Entry<LocalDateTime, Float> entry : HourlyPrices.subMap(HourlyPrices.firstKey(), end).entrySet()) {
			if (entry.getValue() < minPrice) {
				cheapTimeStamp = entry.getKey();
				minPrice = entry.getValue();
			}
		}

		/*
		 * Calculates the amount of energy that needs to be charged during the cheapest
		 * price hours.
		 */

		for (Entry<LocalDateTime, Long> entry1 : hourlyConsumption.entrySet()) {

			if (entry1.getKey().getHour() == cheapTimeStamp.getHour()) {

				demand_Till_Cheapest_Hour = entry1.getValue();
				System.out.println("Cheap Price: " + minPrice);
				Long chargebleConsumption;

				// if the battery has sufficient energy!
				if (availableCapacity >= demand_Till_Cheapest_Hour) {
					chargebleConsumption = totalDemand - availableCapacity;
					chargeSchedule.put(entry1.getKey(), chargebleConsumption);
					minPrice = Float.MAX_VALUE;
					return chargeSchedule;
				}

				// if the battery doesn't has sufficient energy!
				chargebleConsumption = totalDemand - demand_Till_Cheapest_Hour;

				/*
				 * During the cheap hour, Grid is used for both charging the battery and also to
				 * satisfy the current loads.
				 */

				if (chargebleConsumption != 0) {
					chargeSchedule.put(entry1.getKey(), chargebleConsumption);
				}
				minPrice = Float.MAX_VALUE;
				end = cheapTimeStamp;
				getCheapestHours(end);
			}
		}
		return chargeSchedule;
	}
}
