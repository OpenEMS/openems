package io.openems.edge.controller.symmetric.dynamiccharge;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CalculateConsumption {

	private final Logger log = LoggerFactory.getLogger(DynamicCharge.class);
	private DynamicCharge dynamicCharge;
	private Integer production;
	private Integer consumption;
	private LocalDate dateOfLastRun = null;
	private LocalDate dateOfT0 = null;
	private int currentHour = 0;
	private static TreeMap<LocalDateTime, Float> hourlyConsumption = new TreeMap<LocalDateTime, Float>();
	private static TreeMap<LocalDateTime, Float> HourlyPrices = new TreeMap<LocalDateTime, Float>();
	public static TreeMap<LocalDateTime, Float> chargeSchedule = new TreeMap<LocalDateTime, Float>();
	public static Integer t0 = null; // last time of the day when production > consumption
	public static Integer t1 = null; // first time of the day when production > consumption
	private long totalConsumption = 0;
	private float hourConsumption = 0;
	private static float availableConsumption = 0;
	private static float totalDemand;
	private int totalConsumptionCounter = 0;

	public CalculateConsumption(DynamicCharge dynamicCharge) {
		this.dynamicCharge = dynamicCharge;
	}

	private State currentState = State.PRODUCTION_LOWER_THAN_CONSUMPTION;

	private enum State {

		PRODUCTION_LOWER_THAN_CONSUMPTION, PRODUCTION_DROPPED_BELOW_CONSUMPTION, PRODUCTION_HIGHER_THAN_CONSUMPTION,
		PRODUCTION_EXCEEDED_CONSUMPTION
	}

	protected void run() {

		/*
		 * Detect switch to next day
		 */

		LocalDate nowDate = LocalDate.now();
		if (dateOfLastRun == null || dateOfLastRun.isBefore(nowDate)) {
			// initialize

			/*
			 * should work on this
			 */

			t1 = null;
			log.info("New Day " + nowDate);
			log.info("t1: " + t1);
		}

		LocalDateTime now = LocalDateTime.now();
		int secondOfDay = now.getSecond() + now.getMinute() * 60 + now.getHour() * 3600;

		switch (currentState) {
		case PRODUCTION_LOWER_THAN_CONSUMPTION:
			if (production > consumption) {
				log.info(production + " is greater than " + consumption
						+ " so switching the state from PRODUCTION LOWER THAN CONSUMPTION to PRODUCTION EXCEEDING CONSUMPTION");
				this.currentState = State.PRODUCTION_EXCEEDED_CONSUMPTION;

			}
			this.totalConsumption += consumption;
			this.totalConsumptionCounter++;
			log.info(
					" Total Consumption " + totalConsumption + " Total Consumption Counter " + totalConsumptionCounter);
			break;

		case PRODUCTION_EXCEEDED_CONSUMPTION:
			if (t1 == null && t0 != null) {

				// this is the first time of the day that production > consumption
				t1 = secondOfDay;
				log.info(" t1 is set: " + t1);

				// reset values
				log.info("Resetting Values during " + now);
				t0 = null;
				dateOfLastRun = nowDate;
				log.info("dateOfLastRun " + dateOfLastRun);
				this.totalConsumption = 0;
				this.totalConsumptionCounter = 0;
			}

			log.info(production + " is greater than " + consumption
					+ " so switching the state from PRODUCTION EXCEEDING CONSUMPTION to PRODUCTION HIGHER THAN CONSUMPTION ");
			this.currentState = State.PRODUCTION_HIGHER_THAN_CONSUMPTION;
			break;

		case PRODUCTION_HIGHER_THAN_CONSUMPTION:

			/*
			 * TODO Need to set the hard timestamp. For Example: after 5 o clock to start
			 * calculating the consumption data.
			 */

			if (production < consumption && now.getHour() >= 17) {
				log.info(production + " is lesser than " + consumption
						+ " so switching the state from PRODUCTION HIGHER THAN CONSUMPTION to PRODUCTION DROPPED BELOW CONSUMPTION ");
				this.currentState = State.PRODUCTION_DROPPED_BELOW_CONSUMPTION;
			}
			break;

		case PRODUCTION_DROPPED_BELOW_CONSUMPTION:

			t0 = secondOfDay;
			dateOfT0 = nowDate;
			log.info("t0 is set at: " + dateOfT0);

			if (dateOfLastRun != null) {

				/*
				 * TODO
				 * SoC
				 */

				Prices.houlryprices();
				HourlyPrices = Prices.getHourlyPrices();
				getCheapestHours(HourlyPrices.lastKey());

			}

			// Resetting Values

			this.totalConsumption = 0;
			this.totalConsumptionCounter = 0;
			log.info(production + "is lesser than" + consumption
					+ "so switching the state from PRODUCTION DROPPED BELOW CONSUMPTION to PRODUCTION LOWER THAN CONSUMPTION");
			this.currentState = State.PRODUCTION_LOWER_THAN_CONSUMPTION;
			break;

		}

	}

	private static TreeMap<LocalDateTime, Float> getCheapestHours(LocalDateTime end) {

		return chargeSchedule;
	}
}
