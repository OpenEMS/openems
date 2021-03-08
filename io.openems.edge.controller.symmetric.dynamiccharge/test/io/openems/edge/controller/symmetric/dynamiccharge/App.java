package io.openems.edge.controller.symmetric.dynamiccharge;

import java.time.ZonedDateTime;
import java.time.ZonedDateTime;
import java.util.Map.Entry;

import io.openems.common.exceptions.InvalidValueException;

//import java.util.Optional;
//import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

public class App {
	private static TreeMap<ZonedDateTime, Float> hourlyPrices = new TreeMap<ZonedDateTime, Float>();
	private static TreeMap<ZonedDateTime, Integer> hourlyConsumption = new TreeMap<ZonedDateTime, Integer>();
	public static TreeMap<ZonedDateTime, Integer> chargeSchedule = new TreeMap<ZonedDateTime, Integer>();
	private static float minPrice;
	private static ZonedDateTime cheapTimeStamp = null;
	public static ZonedDateTime t0 = null;
	public static ZonedDateTime t1 = null;
	private static ZonedDateTime startTime;
	private static ZonedDateTime endTime;
	private static Integer chargebleConsumption;
	private static Integer demandTillCheapestHour = 0;
	private static Integer availableCapacity = 1350;
	private static Integer nettCapacity = 12000;
	private static Integer maxApparentPower = 9000;
	private static Integer totalDemand;
	private static Integer remainingConsumption;
	private static Integer currentHourConsumption;
	private static ZonedDateTime proLessThanCon = null;
	private static ZonedDateTime proMoreThanCon = null;

	private static long minEnergy = (15 * nettCapacity) / 100;

	public static void main(String[] args) throws InvalidValueException {

//		ZonedDateTime now = ZonedDateTime.of(2019, 6, 17, 16, 0);

		ZonedDateTime now = ZonedDateTime.now().withMinute(0).withSecond(0).withNano(0);

		proLessThanCon = ZonedDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0).plusHours(17);

		proMoreThanCon = ZonedDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0).plusHours(9)
				.plusDays(1);

		for (int i = 0; i < 16; i++) {
			hourlyConsumption.put(now.plusHours(i), 2500);
		}

//		if (!hourlyConsumption.isEmpty()) {
//			System.out.println(
//					"first Key: " + hourlyConsumption.firstKey() + " last Key: " + hourlyConsumption.lastKey());
//		}

		for (Entry<ZonedDateTime, Integer> entry : hourlyConsumption.entrySet()) {
			System.out.println("Time: " + entry.getKey() + " Consumption: " + entry.getValue());
		}

		totalDemand = calculateDemandTillThishour(proLessThanCon, proMoreThanCon)
				+ hourlyConsumption.get(proMoreThanCon);

		System.out.println(" [ " + nettCapacity + " ] " + " [ " + maxApparentPower + " ] " + " [ " + availableCapacity
				+ " ] " + " [ " + totalDemand + " ] ");

		hourlyPrices = PriceApi.houlryPrices();

		hourlyPrices.entrySet().forEach(p -> {
			System.out.println(" " + p.getKey() + " " + p.getValue());
		});

		System.out.println(" Getting schedule: ");
		chargeSchedule.clear();

		getChargeSchedule(proLessThanCon, proMoreThanCon.plusHours(1), totalDemand, availableCapacity);
//
		for (Entry<ZonedDateTime, Integer> entry : chargeSchedule.entrySet()) {
			System.out.println("Time: " + entry.getKey() + " Consumption: " + entry.getValue());
		}

	}

	private static void getChargeSchedule(ZonedDateTime proLessThanCon, ZonedDateTime proMoreThanCon,
			Integer totalConsumption, Integer availableEnergy) throws InvalidValueException {
		// function to find the minimum priceHour
		ZonedDateTime cheapestHour = cheapHour(proLessThanCon, proMoreThanCon);

		Integer demandTillCheapestHour = calculateDemandTillThishour(proLessThanCon, cheapestHour);

		Integer currentHourConsumption = hourlyConsumption.get(cheapestHour);

		Integer remainingConsumption = 0;

		// Calculates the amount of energy that needs to be charged during the cheapest
		// price hours.

		if (totalConsumption > 0) {

			// if the battery doesn't has sufficient energy!
			if (availableEnergy >= demandTillCheapestHour) {
				totalConsumption -= availableEnergy;
				adjustRemainigConsumption(cheapestHour, proMoreThanCon, totalConsumption, availableEnergy,
						demandTillCheapestHour);
			} else {

				Integer chargebleConsumption = totalConsumption - demandTillCheapestHour - currentHourConsumption;

				if (chargebleConsumption > 0) {
					if (chargebleConsumption > maxApparentPower) {

						ZonedDateTime lastCheapTimeStamp = cheapestHour;

						// checking for next cheap hour if it is before or after the first cheapest
						// hour.
						cheapestHour = cheapHour(proLessThanCon, cheapestHour);
						float firstMinPrice = minPrice;

						cheapestHour = cheapHour(lastCheapTimeStamp.plusHours(1), proMoreThanCon);

						if (minPrice < firstMinPrice) {
							remainingConsumption = chargebleConsumption - maxApparentPower;
							adjustRemainigConsumption(lastCheapTimeStamp.plusHours(1),
									hourlyConsumption.lastKey().plusDays(1), remainingConsumption, maxApparentPower,
									demandTillCheapestHour);
						} else {
							if (chargebleConsumption > nettCapacity) {
								remainingConsumption = chargebleConsumption - nettCapacity;
								adjustRemainigConsumption(lastCheapTimeStamp.plusHours(1),
										hourlyConsumption.lastKey().plusDays(1), remainingConsumption, nettCapacity,
										demandTillCheapestHour);
							}

						}

						cheapestHour = lastCheapTimeStamp;
						chargebleConsumption = maxApparentPower;

					}
					totalConsumption = totalConsumption - chargebleConsumption - currentHourConsumption
							- remainingConsumption;
					remainingConsumption = 0;

					// adding into charge Schedule
					chargeSchedule.put(cheapestHour, chargebleConsumption);
					getChargeSchedule(proLessThanCon, cheapestHour, totalConsumption, availableEnergy);

				} else {
					totalConsumption -= currentHourConsumption;
					getChargeSchedule(proLessThanCon, cheapestHour, totalConsumption, availableEnergy);
				}

			}

		}

	}

	private static void adjustRemainigConsumption(ZonedDateTime cheapestHour, ZonedDateTime proMoreThanCon,
			Integer remainingConsumption, Integer availableEnergy, Integer demandTillCheapestHour)
			throws InvalidValueException {

		if (!cheapestHour.isEqual(proMoreThanCon)) {

			if (remainingConsumption > 0) {

				ZonedDateTime cheapTimeStamp = cheapHour(cheapestHour, proMoreThanCon);
				Integer currentHourConsumption = hourlyConsumption.get(cheapTimeStamp);

				if (demandTillCheapestHour > availableEnergy) {
					demandTillCheapestHour -= availableEnergy;
					availableEnergy = 0;
				} else {
					availableEnergy -= demandTillCheapestHour;
					demandTillCheapestHour = 0;
				}

				Integer allowedConsumption = nettCapacity - availableEnergy;

				if (allowedConsumption > 0) {
					if (allowedConsumption > maxApparentPower) {
						allowedConsumption = maxApparentPower;
					}
					remainingConsumption = remainingConsumption - currentHourConsumption - demandTillCheapestHour;

					if (remainingConsumption > 0) {
						if (remainingConsumption > allowedConsumption) {
							remainingConsumption -= allowedConsumption;
							availableEnergy += allowedConsumption;

							// adding into charge Schedule
							chargeSchedule.put(cheapTimeStamp, allowedConsumption);
							adjustRemainigConsumption(cheapTimeStamp.plusHours(1), proMoreThanCon, remainingConsumption,
									availableEnergy, demandTillCheapestHour);
						} else {
							// adding into charge Schedule
							chargeSchedule.put(cheapTimeStamp, remainingConsumption);
						}
					}

				} else {

					availableEnergy -= currentHourConsumption;
					adjustRemainigConsumption(cheapTimeStamp.plusHours(1), proMoreThanCon, remainingConsumption,
							availableEnergy, demandTillCheapestHour);
				}

			}

		}

	}

	private static Integer calculateDemandTillThishour(ZonedDateTime proLessThanCon, ZonedDateTime cheapestHour) {
		Integer demand = 0;

		for (Entry<ZonedDateTime, Integer> entry : hourlyConsumption.entrySet()) {
			if ((entry.getKey().isEqual(proLessThanCon) || entry.getKey().isAfter(proLessThanCon))
					&& entry.getKey().isBefore(cheapestHour)) {
				demand += entry.getValue();
			}
		}
		return demand;
	}

	private static ZonedDateTime cheapHour(ZonedDateTime proLessThanCon, ZonedDateTime proMoreThanCon) {
		minPrice = Float.MAX_VALUE;
		ZonedDateTime cheapTimeStamp = null;

		for (Entry<ZonedDateTime, Float> entry : hourlyPrices.subMap(proLessThanCon, proMoreThanCon).entrySet()) {
			if (entry.getValue() < minPrice) {
				cheapTimeStamp = entry.getKey();
				minPrice = entry.getValue();
			}
		}
		return cheapTimeStamp;
	}
}
