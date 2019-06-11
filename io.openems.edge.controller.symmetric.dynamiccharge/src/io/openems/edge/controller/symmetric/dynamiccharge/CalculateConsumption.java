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

	@SuppressWarnings("unused")
	private DynamicCharge dynamicCharge;
	private static int soc;
	private static long totalDemand = 0;
	private static long nettCapacity;
	private LocalDate dateOfT0 = null;
	private long totalConsumption = 0;
	private long currentConsumption = 0;
	private long currentProduction = 0;
	private static long maxApparentPower;
	private static LocalDateTime t0 = null;
	private static LocalDateTime t1 = null;
	private static long availableCapacity;
	private LocalDate dateOfLastRun = null;
	private static long chargebleConsumption;
	private LocalDateTime currentHour = null;
	// private static long remainingConsumption;
	private static long currentHourConsumption;
	private static long demand_Till_Cheapest_Hour;
	private static float minPrice = Float.MAX_VALUE;
	private static LocalDateTime cheapTimeStamp = null;

	private static TreeMap<LocalDateTime, Long> chargeSchedule = new TreeMap<LocalDateTime, Long>();
	private static TreeMap<LocalDateTime, Float> HourlyPrices = new TreeMap<LocalDateTime, Float>();
	private static TreeMap<LocalDateTime, Long> hourlyConsumption = new TreeMap<LocalDateTime, Long>();

	public CalculateConsumption(DynamicCharge dynamicCharge) {
		this.dynamicCharge = dynamicCharge;
	}

	private enum State {

		PRODUCTION_LOWER_THAN_CONSUMPTION, PRODUCTION_DROPPED_BELOW_CONSUMPTION, PRODUCTION_HIGHER_THAN_CONSUMPTION,
		PRODUCTION_EXCEEDED_CONSUMPTION
	}

	private State currentState = State.PRODUCTION_LOWER_THAN_CONSUMPTION;

	protected void run(ManagedSymmetricEss ess, SymmetricMeter meter, Config config, Sum sum) {
		

		int production = sum.getProductionActivePower().value().orElse(0);
		int consumption = sum.getConsumptionActivePower().value().orElse(0);
//		long production = sum.getProductionActiveEnergy().value().orElse(0L);
//		long consumption = sum.getConsumptionActiveEnergy().value().orElse(0L);
		long productionEnergy = sum.getProductionActiveEnergy().value().orElse(0L);
		long consumptionEnergy = sum.getConsumptionActiveEnergy().value().orElse(0L);

		LocalDate nowDate = LocalDate.now();
		LocalDateTime now = LocalDateTime.now();

		log.info("totalDemand: " + totalDemand + " t0: " + t0 + " t1: " + t1 + "current Hour: " + currentHour);
		
		if(!hourlyConsumption.isEmpty()) {
			log.info("first Key: " + hourlyConsumption.firstKey() + " last Key: " + hourlyConsumption.lastKey());
		}

		switch (currentState) {
		case PRODUCTION_LOWER_THAN_CONSUMPTION:
			log.info(" State: " + currentState);

			if (t0 != null) {

				// First time of the day when production > consumption.
				// Avoids the fluctuations and shifts to next state only the next day.
				/*
				 * if ((production > consumption || now.getHour() >= config.Max_Morning_hour())
				 * && dateOfT0.isBefore(nowDate)) { log.info(production + " is greater than " +
				 * consumption +
				 * " so switching the state from PRODUCTION LOWER THAN CONSUMPTION to PRODUCTION EXCEEDING CONSUMPTION"
				 * ); this.currentState = State.PRODUCTION_EXCEEDED_CONSUMPTION; }
				 */

				// to avoid Bug (production value in night or minus consumption value)
				if ((now.getHour() >= config.Max_Morning_hour()) && dateOfT0.isBefore(nowDate)) {
					if (production > consumption || now.getHour() > config.Max_Morning_hour()) {
						log.info(production + " is greater than " + consumption
								+ " so switching the state from PRODUCTION LOWER THAN CONSUMPTION to PRODUCTION EXCEEDING CONSUMPTION");
						this.currentState = State.PRODUCTION_EXCEEDED_CONSUMPTION;
					}
				}

				// Detects the switching of hour
				if (now.getHour() == currentHour.plusHours(1).getHour()) {
					log.info(" Switching of the hour detected and updating " + " [ " + currentHour + " ] ");
					this.totalConsumption = (consumptionEnergy - currentConsumption)
							- (productionEnergy - currentProduction);
					hourlyConsumption.put(currentHour, totalConsumption);
					this.currentConsumption = consumptionEnergy;
					this.currentProduction = productionEnergy;
					currentHour = now;
				}

				log.info(" Total Consumption: " + totalConsumption);

				// condition for initial run.
			} else if (production > consumption || now.getHour() >= config.Max_Morning_hour()) {
				this.currentState = State.PRODUCTION_EXCEEDED_CONSUMPTION;
			}
			break;

		case PRODUCTION_EXCEEDED_CONSUMPTION:
			log.info(" State: " + currentState);
			if (t1 == null && t0 != null) {

				// This is the first time of the day that "production > consumption".
				this.totalConsumption = (consumptionEnergy - currentConsumption)
						- (productionEnergy - currentProduction);
				hourlyConsumption.put(currentHour, totalConsumption);
				totalDemand = calculateDemandTillThishour(hourlyConsumption.firstKey().plusDays(1),
						hourlyConsumption.lastKey().plusDays(1)) + hourlyConsumption.lastEntry().getValue();
				t1 = now;
				log.info(" t1 is set: " + t1);

				// reset values
				log.info("Resetting Values during " + now);
				t0 = null;
				chargeSchedule = null;

				this.dateOfLastRun = nowDate;
				log.info("dateOfLastRun " + dateOfLastRun);
			}

			log.info(production + " is greater than " + consumption
					+ " so switching the state from PRODUCTION EXCEEDING CONSUMPTION to PRODUCTION HIGHER THAN CONSUMPTION ");
			this.currentState = State.PRODUCTION_HIGHER_THAN_CONSUMPTION;
			break;

		case PRODUCTION_HIGHER_THAN_CONSUMPTION:
			log.info(" State: " + currentState);

			// avoid switching to next state during the day.
			if (production < consumption && now.getHour() >= config.Max_Evening_hour()) {
				log.info(production + " is lesser than " + consumption
						+ " so switching the state from PRODUCTION HIGHER THAN CONSUMPTION to PRODUCTION DROPPED BELOW CONSUMPTION ");
				this.currentState = State.PRODUCTION_DROPPED_BELOW_CONSUMPTION;
			}
			break;

		case PRODUCTION_DROPPED_BELOW_CONSUMPTION:
			log.info(" State: " + currentState);

			t0 = now;
			this.dateOfT0 = nowDate;
			log.info("t0 is set at: " + dateOfT0);
			currentHour = now;
			currentConsumption = consumptionEnergy;
			currentProduction = productionEnergy;

			// avoids the initial run
			if (dateOfLastRun != null) {
				log.info("Entering Calculations: ");
				t1 = null;
				soc = ess.getSoc().value().orElse(0);
				nettCapacity = ess.getNetCapacity().value().orElse(0);
				maxApparentPower = ess.getMaxApparentPower().value().orElse(0);
				availableCapacity = (soc / 100) * nettCapacity;
				Prices.houlryprices();
				HourlyPrices = Prices.getHourlyPrices();
				/*totalDemand = calculateDemandTillThishour(hourlyConsumption.firstKey().plusDays(1),
						hourlyConsumption.lastKey().plusDays(1)) + hourlyConsumption.lastEntry().getValue();*/
				totalDemand = calculateDemandTillThishour(hourlyConsumption.firstKey().plusDays(1),
						hourlyConsumption.lastKey().plusDays(1)) + hourlyConsumption.lastEntry().getValue();
				log.info(hourlyConsumption.firstKey() + " ] " + " [ " + hourlyConsumption.lastKey() + " ] ");
				log.info(" Getting schedule: ");
				getChargeSchedule(hourlyConsumption.firstKey().plusDays(1), hourlyConsumption.lastKey().plusDays(1));
				// hourlyConsumption.clear();
			}

			// Resetting Values
			log.info(production + "is lesser than" + consumption
					+ "so switching the state from PRODUCTION DROPPED BELOW CONSUMPTION to PRODUCTION LOWER THAN CONSUMPTION");
			this.currentState = State.PRODUCTION_LOWER_THAN_CONSUMPTION;
			break;
		}
	}

	private static void getChargeSchedule(LocalDateTime start, LocalDateTime end) {

		// function to find the minimum priceHour
		cheapHour(start, end);

		demand_Till_Cheapest_Hour = calculateDemandTillThishour(start, cheapTimeStamp);
		currentHourConsumption = hourlyConsumption.higherEntry(cheapTimeStamp.minusDays(1)).getValue();

		/*
		 * Calculates the amount of energy that needs to be charged during the cheapest
		 * price hours.
		 */

		if (totalDemand > 0) {

			// if the battery doesn't has sufficient energy!

			if (availableCapacity >= demand_Till_Cheapest_Hour) {
				getCheapestHoursIfBatterySufficient(cheapTimeStamp, HourlyPrices.lastKey());
			} else {
				chargebleConsumption = totalDemand - demand_Till_Cheapest_Hour - currentHourConsumption;
				// bufferAmountToCharge = 0;

				if (chargebleConsumption > 0) {

					if (chargebleConsumption > maxApparentPower) {
						LocalDateTime lastCheapTimeStamp = cheapTimeStamp;
						float lastMinPrice = minPrice;
						cheapHour(cheapTimeStamp.plusHours(1), end.plusDays(1));

						if (minPrice < lastMinPrice) {
							// remainingConsumption = chargebleConsumption - maxApparentPower;
							chargebleConsumption = maxApparentPower;
							chargeSchedule.put(lastCheapTimeStamp, maxApparentPower);
							System.out.println("getting into adjusting remaining charge: ");
							// adjustRemainigConsumption(lastCheapTimeStamp.plusHours(1),
							// hourlyConsumption.lastKey().plusDays(1));
						} else {

							if (chargebleConsumption > nettCapacity) {
								// remainingConsumption = chargebleConsumption - nettCapacity;
								// System.out.println("getting into adjusting remaining charge: ");
								// adjustRemainigConsumption(lastCheapTimeStamp.plusHours(1),
								// hourlyConsumption.lastKey().plusDays(1));
								// hourlyConsumption.lastKey().plusDays(1));
							}
						}
						cheapTimeStamp = lastCheapTimeStamp;
						chargebleConsumption = maxApparentPower;
					}
					totalDemand = totalDemand - chargebleConsumption - currentHourConsumption;
					chargeSchedule.put(cheapTimeStamp, chargebleConsumption);
					getChargeSchedule(start, cheapTimeStamp);
				} else {
					totalDemand = totalDemand - currentHourConsumption;
					getChargeSchedule(start, cheapTimeStamp);
				}
			}
		}
	}

	private static void getCheapestHoursIfBatterySufficient(LocalDateTime start, LocalDateTime end) {

		availableCapacity -= demand_Till_Cheapest_Hour; // This will be the capacity during cheapest hour.
		long allowedConsumption = nettCapacity - availableCapacity;
		currentHourConsumption = hourlyConsumption.higherEntry(cheapTimeStamp.minusDays(1)).getValue();
		chargebleConsumption = totalDemand - availableCapacity - currentHourConsumption;

		if (chargebleConsumption > 0) {

			if ((chargebleConsumption > allowedConsumption)) {

				if ((chargebleConsumption > maxApparentPower)) {
					// remainingConsumption = chargebleConsumption - maxApparentPower;
					chargebleConsumption = maxApparentPower;
					chargeSchedule.put(cheapTimeStamp, chargebleConsumption);
					availableCapacity += maxApparentPower;
				} else {
					// remainingConsumption = chargebleConsumption - allowedConsumption;
					chargebleConsumption = allowedConsumption;
					chargeSchedule.put(cheapTimeStamp, chargebleConsumption);
					availableCapacity += allowedConsumption;
				}
				// adjustRemainigConsumption(cheapTimeStamp.plusHours(1),
				// hourlyConsumption.lastKey().plusDays(1));
			} else {
				if ((chargebleConsumption > maxApparentPower)) {
					// remainingConsumption = chargebleConsumption - maxApparentPower;
					chargebleConsumption = maxApparentPower;
					chargeSchedule.put(cheapTimeStamp, chargebleConsumption);
					availableCapacity += maxApparentPower;
					// adjustRemainigConsumption(cheapTimeStamp.plusHours(1),
					// hourlyConsumption.lastKey().plusDays(1));
				} else {
					totalDemand = totalDemand - chargebleConsumption - availableCapacity - currentHourConsumption;
					chargeSchedule.put(cheapTimeStamp, chargebleConsumption);
				}
			}
		}
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

	protected TreeMap<LocalDateTime, Long> getChargeSchedule() {
		return chargeSchedule;
	}

	protected LocalDateTime getT0() {
		return t0;
	}

	protected LocalDateTime getT1() {
		return t1;
	}
}
