package io.openems.impl.controller.symmetric.awattar;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.exception.InvalidValueException;

public class CalculateTotalConsumption {

	private final Logger log = LoggerFactory.getLogger(AwattarController.class);
	private final AwattarController awattarController;
	private LocalDate dateOfLastRun = null;
	private LocalDate dateOfT0 = null;

	private long totalConsumption = 0;
	private float availableConsumption = 0;
	private int totalConsumptionCounter = 0;
	private float chargebleConsumption = 0;
	private float consumptionPerSecond = 0;
	private float consumptionPerHour = 0;
	private float requiredConsumption;
	private long neededConsumption = 0;
	private Integer t0 = null; // last time of the day when production > consumption
	private Integer t1 = null; // first time of the day when production > consumption
	private Integer t = 0;
	private Integer t2 = null;
	private Long soc;
	// private Integer t3 = null;

	private State currentState = State.PRODUCTION_LOWER_THAN_CONSUMPTION;

	private enum State {

		PRODUCTION_LOWER_THAN_CONSUMPTION, PRODUCTION_DROPPED_BELOW_CONSUMPTION, PRODUCTION_HIGHER_THAN_CONSUMPTION, PRODUCTION_EXCEEDED_CONSUMPTION
	}

	public CalculateTotalConsumption(AwattarController awattarController) {
		this.awattarController = awattarController;
	}

	protected float getTotalConsumption() {
		// ...
		return this.requiredConsumption;
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
							t2 = 86400 - t0;
							t = t2 + t1;
							float hours = t / 3600;
							consumptionPerSecond = totalConsumption / t;
							consumptionPerHour = consumptionPerSecond * 3600 ;
							//requiredConsumption = (totalConsumption / totalConsumptionCounter);
							log.info(" Required Consumption " + requiredConsumption + " during " + now);
							// reset values
							log.info("Resetting Values during " + now);
							this.t0 = null;
							// this.t1 = null;
							dateOfLastRun = nowDate;
							log.info("dateOfLastRun " + dateOfLastRun);
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
					this.t0 = secondOfDay;
					dateOfT0 = nowDate;
					//LocalDateTime nowTime = now;
					soc = ess.soc.value();
					availableConsumption = (soc / 100) * 12000; // Available in the battery


					if (now.getHour() >= 17) {
						JsonData.jsonRead("Data.json");
						//JsonData.getCheapestHours(now, JsonData.end_TimeStamp(), availableConsumption, consumptionPerSecond);

						LocalDateTime cheapestTime = JsonData.startTimeStamp();
						long seconds = ChronoUnit.SECONDS.between(now, cheapestTime); // No. of Seconds  from t0 to cheapest hour second.
						//int availableTime = cheapestHourSecond - t0;
						neededConsumption = (long) (consumptionPerSecond * seconds);
						if (availableConsumption >= neededConsumption) {
							chargebleConsumption = totalConsumption - availableConsumption;
						} else {

							//JsonData.getCheapestHours(now, cheapestTime);
							// Number of hours, storage is sufficient to run with available energy.
							int sufficientHours = (int) Math.ceil((availableConsumption /  consumptionPerHour ));


							// Estimated target hour till the storage energy is sufficient.
							LocalDateTime targetHour = cheapestTime.plusHours(sufficientHours);
						}
					}

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
		} catch (InvalidValueException | NullPointerException e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
	}

	public float getChargebleConsumption() {
		return this.chargebleConsumption;
	}
}
