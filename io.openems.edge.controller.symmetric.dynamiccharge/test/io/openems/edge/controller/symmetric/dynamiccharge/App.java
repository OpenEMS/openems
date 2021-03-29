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
	private static TreeMap<ZonedDateTime, Integer> batteryReference = new TreeMap<ZonedDateTime, Integer>();
	public static TreeMap<ZonedDateTime, Integer> chargeSchedule = new TreeMap<ZonedDateTime, Integer>();
	private static float minPrice;
	private static ZonedDateTime cheapTimeStamp = null;
	public static ZonedDateTime t0 = null;
	public static ZonedDateTime t1 = null;
	private static ZonedDateTime startTime;
	private static ZonedDateTime endTime;
	private static Integer chargebleConsumption;
	private static Integer demandTillCheapestHour = 0;

	private static Integer maxApparentPower = 9000;
	private static Integer totalDemand;
	private static Integer remainingConsumption;
	private static Integer currentHourConsumption;
	private static ZonedDateTime proLessThanCon = null;
	private static ZonedDateTime proMoreThanCon = null;

	private static Integer nettCapacity = 12000;
	private static Integer minEnergy = (15 * nettCapacity) / 100;
	private static Integer availableCapacity = Math.max(0, 1350 - minEnergy);

	public static void main(String[] args) throws InvalidValueException {

//		ZonedDateTime now = ZonedDateTime.of(2019, 6, 17, 16, 0);

		ZonedDateTime now = ZonedDateTime.now().withMinute(0).withSecond(0).withNano(0);

		proLessThanCon = ZonedDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0).plusHours(17);

		proMoreThanCon = ZonedDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0).plusHours(9)
				.plusDays(1);

		System.out.println("proLessThanCon: " + proLessThanCon + " proMoreThanCon: " + proMoreThanCon);

		for (int i = 0; i < 24; i++) {
			hourlyConsumption.put(now.plusHours(i), 2500);
		}

		for (Entry<ZonedDateTime, Integer> entry : hourlyConsumption.entrySet()) {
			System.out.println("Time: " + entry.getKey() + " Consumption: " + entry.getValue());
		}

		totalDemand = calculateDemandTillThishour(proLessThanCon, proMoreThanCon);

		nettCapacity -= minEnergy;

		System.out.println(" nettCapacity [ " + nettCapacity + " ] " + " maxApparentPower [ " + maxApparentPower + " ] "
				+ " availableCapacity [ " + availableCapacity + " ] " + " totalDemand [ " + totalDemand + " ] ");

//		hourlyPrices = PriceApi.houlryPrices();
//

		hourlyPrices.put(now, 30f);
		hourlyPrices.put(now.plusHours(1), 50f);
		hourlyPrices.put(now.plusHours(2), 50f);
		hourlyPrices.put(now.plusHours(3), 50f);
		hourlyPrices.put(now.plusHours(4), 50f);
		hourlyPrices.put(now.plusHours(5), 50f);
		hourlyPrices.put(now.plusHours(6), 50f);
		hourlyPrices.put(now.plusHours(7), 29.2f);
		hourlyPrices.put(now.plusHours(8), 47f);
		hourlyPrices.put(now.plusHours(9), 49f);
		hourlyPrices.put(now.plusHours(10), 29f);
		hourlyPrices.put(now.plusHours(11), 51f);
		hourlyPrices.put(now.plusHours(12), 30f);
		hourlyPrices.put(now.plusHours(13), 45f);
		hourlyPrices.put(now.plusHours(14), 44f);
		hourlyPrices.put(now.plusHours(15), 32f);
		hourlyPrices.put(now.plusHours(16), 43f);
		hourlyPrices.put(now.plusHours(17), 46f);
		hourlyPrices.put(now.plusHours(18), 47f);
		hourlyPrices.put(now.plusHours(19), 53f);
		hourlyPrices.put(now.plusHours(20), 61f);
		hourlyPrices.put(now.plusHours(21), 62f);
		hourlyPrices.put(now.plusHours(22), 70f);
		hourlyPrices.put(now.plusHours(23), 70f);

		hourlyPrices.entrySet().forEach(p -> {
			System.out.println(" " + p.getKey() + " " + p.getValue());
		});

		System.out.println(" Getting schedule: ");
		chargeSchedule.clear();

		for (int i = 0; i < 24; i++) {

			batteryReference.put(now.plusHours(i), 4500);
		}

		getChargeSchedule(proLessThanCon, proMoreThanCon, totalDemand, availableCapacity);
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

		System.out.println("cheapestHour " + cheapestHour + " demandTillCheapestHour: " + demandTillCheapestHour
				+ " currentHourConsumption " + currentHourConsumption);

		// Calculates the amount of energy that needs to be charged during the cheapest
		// price hours.
		if (totalConsumption > 0) {

			// if the battery doesn't has sufficient energy!
			if (availableEnergy >= demandTillCheapestHour) {
				totalConsumption -= availableEnergy;
				adjustRemainigConsumption(cheapestHour, proMoreThanCon, totalConsumption, availableEnergy);
			} else {

				Integer chargebleConsumption = totalConsumption - demandTillCheapestHour - currentHourConsumption;

				System.out.println("chargebleConsumption " + chargebleConsumption);

				if (chargebleConsumption > 0) {
					if (chargebleConsumption > maxApparentPower) {

						ZonedDateTime lastCheapTimeStamp = cheapestHour;

						// checking for next cheap hour if it is before or after the first cheapest
						// hour.
						cheapestHour = cheapHour(proLessThanCon, lastCheapTimeStamp);
						float firstMinPrice = minPrice;

						cheapestHour = cheapHour(lastCheapTimeStamp.plusHours(1), proMoreThanCon);

						System.out.println("demandTillCheapestHour " + demandTillCheapestHour);

						if (minPrice < firstMinPrice) {
							remainingConsumption = chargebleConsumption - maxApparentPower;
							adjustRemainigConsumption(lastCheapTimeStamp.plusHours(1), proMoreThanCon,
									remainingConsumption, maxApparentPower);
						} else {
							if (chargebleConsumption > nettCapacity) {
								remainingConsumption = chargebleConsumption - nettCapacity;
								adjustRemainigConsumption(lastCheapTimeStamp.plusHours(1), proMoreThanCon,
										remainingConsumption, nettCapacity);
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
				} else {
					totalConsumption -= currentHourConsumption;
				}
				getChargeSchedule(proLessThanCon, cheapestHour, totalConsumption, availableEnergy);
			}
		}
	}

	private static void adjustRemainigConsumption(ZonedDateTime cheapestHour, ZonedDateTime proMoreThanCon,
			Integer remainingConsumption, Integer availableEnergy) throws InvalidValueException {

		if (!cheapestHour.isEqual(proMoreThanCon)) {

			if (remainingConsumption > 0) {

				ZonedDateTime cheapTimeStamp = cheapHour(cheapestHour, proMoreThanCon);
				Integer currentHourConsumption = hourlyConsumption.get(cheapTimeStamp);

				System.out.println("cheapTimeStamp: 2 ** " + cheapTimeStamp + " currentHourConsumption "
						+ currentHourConsumption + " demandTillCheapestHour: " + demandTillCheapestHour);

				int predictedDemand = calculateDemandTillThishour(cheapestHour, cheapTimeStamp);

				if (predictedDemand > availableEnergy) {
//					predictedDemand -= availableEnergy;
					availableEnergy = 0;
				} else {
					availableEnergy -= predictedDemand;
					predictedDemand = 0;
				}

				Integer allowedConsumption = nettCapacity - availableEnergy;

				System.out.println(" allowedConsumption:  " + allowedConsumption);

				if (allowedConsumption > 0) {
					if (allowedConsumption > maxApparentPower) {
						allowedConsumption = maxApparentPower;
					}
					remainingConsumption = remainingConsumption - currentHourConsumption - predictedDemand;
					System.out.println(" remainingConsumption:  " + remainingConsumption);

					if (remainingConsumption > 0) {
						if (remainingConsumption > allowedConsumption) {
							remainingConsumption -= allowedConsumption;
							availableEnergy += allowedConsumption;

							// adding into charge Schedule
							chargeSchedule.put(cheapTimeStamp, allowedConsumption);
							adjustRemainigConsumption(cheapTimeStamp.plusHours(1), proMoreThanCon, remainingConsumption,
									availableEnergy);
						} else {
							// adding into charge Schedule
							chargeSchedule.put(cheapTimeStamp, remainingConsumption);
						}
					}
				} else {
					availableEnergy -= currentHourConsumption;
					adjustRemainigConsumption(cheapTimeStamp.plusHours(1), proMoreThanCon, remainingConsumption,
							availableEnergy);
				}
			}
		}
	}

	private static Integer calculateDemandTillThishour(ZonedDateTime proLessThanCon, ZonedDateTime cheapestHour) {
		Integer demand = 0;

		for (Entry<ZonedDateTime, Integer> entry : hourlyConsumption.subMap(proLessThanCon, cheapestHour).entrySet()) {
			demand += entry.getValue();
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

//	private void batteryReference(ZonedDateTime d, Integer capacity) {
//
//		Integer availableCapacity = capacity;
//
//		for (Entry<ZonedDateTime, Integer> entry : hourlyConsumption.subMap(d, proMoreThanCon).entrySet()) {
//
//			batteryReference.put(d, availableCapacity);
//
//			availableCapacity = Math.max(0, (availableCapacity - entry.getValue()));
//		}
//
//	}
}
