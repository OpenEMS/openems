package io.openems.impl.controller.symmetric.awattar;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.exception.InvalidValueException;

public class CalculateTotalConsumption2 {

	private final Logger log = LoggerFactory.getLogger(AwattarController.class);
	private final AwattarController awattarController;
	private LocalDate dateOfLastRun = null;
	private LocalDate dateOfT0 = null;
	private LocalDateTime nowTime = null;

	private static long totalConsumption = 0;
	private static float availableConsumption = 0;

	private int totalConsumptionCounter = 0;
	private Integer t0 = null; // last time of the day when production > consumption
	private Integer t1 = null; // first time of the day when production > consumption
	private Long soc;
	public static TreeMap<LocalDateTime, Long> hourlyConsumptionData = new TreeMap<LocalDateTime, Long>();

	// private Integer t3 = null;

	private State currentState = State.PRODUCTION_LOWER_THAN_CONSUMPTION;

	private enum State {

		PRODUCTION_LOWER_THAN_CONSUMPTION, PRODUCTION_DROPPED_BELOW_CONSUMPTION, PRODUCTION_HIGHER_THAN_CONSUMPTION, PRODUCTION_EXCEEDED_CONSUMPTION
	}

	public CalculateTotalConsumption2(AwattarController awattarController) {
		this.awattarController = awattarController;
	}

	protected void run() {
		// ...
		try {

			/*
			 * Detect switch to next day
			 */
			LocalDate nowDate = LocalDate.now();
			if (dateOfLastRun == null || dateOfLastRun.isBefore(nowDate)) {
				// initialize
				this.t1 = null;
				log.info("New Day " + nowDate);
				log.info("t1: " + t1);
			}

			LocalDateTime now = LocalDateTime.now();
			int secondOfDay = now.getSecond() + now.getMinute() * 60 + now.getHour() * 3600;

			// Ess ess = this.awattarController.ess.value();
			for (Ess ess : this.awattarController.esss.value()) {

				// long pvproduction = this.awattarController.pvmeter.value().activePower.value();
				long essvalue = ess.activePowerL1.value() + ess.activePowerL2.value() + ess.activePowerL3.value();
				long gridvalue = this.awattarController.gridMeter.value().activePower.value();
				long consumption = essvalue + gridvalue;
				long production = 0;

				log.info("Consumption: " + consumption + " Total  production: " + production);

				switch (currentState) {
				case PRODUCTION_LOWER_THAN_CONSUMPTION:
					if (production > consumption) {
						log.info(production + " is greater than " + consumption
								+ " so switching the state from PRODUCTION LOWER THAN CONSUMPTION to PRODUCTION EXCEEDING CONSUMPTION");
						this.currentState = State.PRODUCTION_EXCEEDED_CONSUMPTION;
					} else {
						if (now.getHour() > nowTime.getHour()) {
							// change in hour.
							hourlyConsumptionData.put(nowTime, totalConsumption);
							nowTime = now;
						}
						this.totalConsumption += consumption;
						this.totalConsumptionCounter++;
						log.info(" Total Consumption " + totalConsumption + " Total Consumption Counter "
								+ totalConsumptionCounter);
					}
					break;

				case PRODUCTION_EXCEEDED_CONSUMPTION:
					if (this.t1 == null && this.t0 != null) {
						if (dateOfT0.isBefore(nowDate)) {
							// this is the first time of the day that production > consumption
							this.t1 = secondOfDay;
							log.info(" t1 is set: " + t1);

							// calculate the required kWh
							log.info("Calculate kWh from [" + this.t0 + "] till [" + this.t1 + "]. Sum ["
									+ this.totalConsumption + "] Counter [" + this.totalConsumptionCounter + "].");
							// long seconds = ChronoUnit.SECONDS.between(nowTime, now); // "nowTime" will be keep on
							// updating every hour

							// now we are calculating consumption for every hour rather than consumption in whole

							// requiredConsumption = (totalConsumption / totalConsumptionCounter);
							// log.info(" Required Consumption " + requiredConsumption + " during " + now);

							// reset values
							log.info("Resetting Values during " + now);
							this.t0 = null;
							dateOfLastRun = nowDate;
							log.info(" dateOfLastRun has been updated during " + dateOfLastRun);
							this.totalConsumption = 0;
							this.totalConsumptionCounter = 0;
						}
						log.info("dateOfT0 is equal to today " + dateOfT0 + " = " + nowDate);

					}

					log.info(production + " is greater than " + consumption
							+ " so switching the state from PRODUCTION EXCEEDING CONSUMPTION to PRODUCTION HIGHER THAN CONSUMPTION ");
					this.currentState = State.PRODUCTION_HIGHER_THAN_CONSUMPTION;
					break;

				case PRODUCTION_HIGHER_THAN_CONSUMPTION:
					if (production < consumption) {
						log.info(production + " is lesser than " + consumption
								+ " so switching the state from PRODUCTION HIGHER THAN CONSUMPTION to PRODUCTION DROPPED BELOW CONSUMPTION ");
						this.currentState = State.PRODUCTION_DROPPED_BELOW_CONSUMPTION;
					}
					break;

				case PRODUCTION_DROPPED_BELOW_CONSUMPTION:
					if (now.getHour() >= 17) { // delay charge controller set till 5 o clock for example
						this.t0 = secondOfDay;
						this.t1 = null;
						dateOfT0 = nowDate;
						soc = ess.soc.value();
						availableConsumption = (soc / 100) * 12000; // Available in the battery
						hourlyConsumptionData = null;
						nowTime = now;
						log.info("State of Charge " + soc);
						log.info("t0 is set at: " + dateOfT0);
						log.info("Resetting Values during " + now);
						this.totalConsumption = 0;
						this.totalConsumptionCounter = 0;
						log.info(production + " is lesser than " + consumption
								+ " so switching the state from PRODUCTION DROPPED BELOW CONSUMPTION to PRODUCTION LOWER THAN CONSUMPTION ");
						this.currentState = State.PRODUCTION_LOWER_THAN_CONSUMPTION;
						break;
					}
				}

			}
		} catch (InvalidValueException | NullPointerException e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
	}

	public static long TotalConsumption() {
		return totalConsumption;
	}

	public static float AvailableConsumption() {
		return availableConsumption;
	}

}
