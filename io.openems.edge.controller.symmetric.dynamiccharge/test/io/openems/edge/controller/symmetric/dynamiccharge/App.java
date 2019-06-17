package io.openems.edge.controller.symmetric.dynamiccharge;

import java.time.LocalDateTime;
import java.util.Map.Entry;
import java.util.Map;
import java.util.TreeMap;

public class App {
	private static TreeMap<LocalDateTime, Float> HourlyPrices = new TreeMap<LocalDateTime, Float>();
	private static TreeMap<LocalDateTime, Long> hourlyConsumption = new TreeMap<LocalDateTime, Long>();
	public static TreeMap<LocalDateTime, Long> chargeSchedule = new TreeMap<LocalDateTime, Long>();
	private static float minPrice = Float.MAX_VALUE;
	private static LocalDateTime cheapTimeStamp = null;
	public static LocalDateTime t0 = null;
	public static LocalDateTime t1 = null;
	private static long chargebleConsumption;
	private static long demand_Till_Cheapest_Hour;
	private static long availableCapacity = 3000;
	private static long nettCapacity = 12000;
	private static long maxApparentPower = 9000;
	private static long totalDemand;
	private static long bufferAmountToCharge = 0;
	private static long remainingConsumption;
	private static long currentHourConsumption;

	public static void main(String[] args) {

		PricesTest.houlryPricesTest();
		HourlyPrices = PricesTest.getHourlyPricesTest();

		LocalDateTime now = LocalDateTime.of(2019, 6, 11, 16, 0);

		for (int i = 0; i < 17; i++) {

			hourlyConsumption.put(now.plusHours(i), (long) (700 + (150 * i)));
		}

		for (Entry<LocalDateTime, Float> entry : HourlyPrices.entrySet()) {
			System.out.println("Time: " + entry.getKey() + " Price: " + entry.getValue());
		}

		for (Entry<LocalDateTime, Long> entry : hourlyConsumption.entrySet()) {
			System.out.println("Time: " + entry.getKey() + " Consumption: " + entry.getValue());
		}

		totalDemand = (calculateDemandTillThishour(hourlyConsumption.firstKey(), hourlyConsumption.lastKey()))
				+ hourlyConsumption.lastEntry().getValue();
		System.out.println("total Consumption: " + totalDemand);
		getCheapestHoursIfBatteryNotSufficient(HourlyPrices.firstKey(), HourlyPrices.lastKey());

		for (Entry<LocalDateTime, Long> entry : chargeSchedule.entrySet()) {
			System.out.println("Time: " + entry.getKey() + " Consumption: " + entry.getValue());
		}

	}

	private static void getCheapestHoursIfBatteryNotSufficient(LocalDateTime start, LocalDateTime end) {

		// function to find the minimum priceHour
		cheapHour(start, end);
		System.out.println("Cheap Price: " + minPrice);
		System.out.println("total Demand: " + totalDemand);
		currentHourConsumption = hourlyConsumption.ceilingEntry(cheapTimeStamp.minusDays(1)).getValue();
		demand_Till_Cheapest_Hour = calculateDemandTillThishour(hourlyConsumption.firstKey(),
				cheapTimeStamp.minusDays(1));
		/*
		 * Calculates the amount of energy that needs to be charged during the cheapest
		 * price hours.
		 */
		if (totalDemand > 0) {
			// if the battery has sufficient energy!
			if (availableCapacity >= demand_Till_Cheapest_Hour) {
				System.out.println("entering second loop ");
				getCheapestHoursIfBatterySufficient(cheapTimeStamp, HourlyPrices.lastKey());
			} else {

				// if the battery doesn't has sufficient energy!

				System.out.println("greater than 0 ");
				System.out.println(currentHourConsumption);
				chargebleConsumption = totalDemand - demand_Till_Cheapest_Hour - currentHourConsumption;
				// bufferAmountToCharge = 0;
				System.out.println("chargebleConsumption: " + chargebleConsumption);

				if (chargebleConsumption > 0) {
					System.out.println("greater than 0 ");

					if (chargebleConsumption > maxApparentPower) {

						LocalDateTime lastCheapTimeStamp = cheapTimeStamp;
						float lasttMinPrice = minPrice;
						cheapHour(cheapTimeStamp.plusHours(1), hourlyConsumption.lastKey().plusDays(1));

						if (minPrice < lasttMinPrice) {
							remainingConsumption = chargebleConsumption - maxApparentPower;
							System.out.println("getting into adjusting remaining charge: ");
							adjustRemainigConsumption(lastCheapTimeStamp.plusHours(1),
									hourlyConsumption.lastKey().plusDays(1), remainingConsumption, maxApparentPower);
						} else {
							System.out.println("entering else:");

							if (chargebleConsumption > nettCapacity) {
								remainingConsumption = chargebleConsumption - nettCapacity;
								System.out.println("getting into adjusting remaining charge: ");
								System.out.println(remainingConsumption);
								adjustRemainigConsumption(lastCheapTimeStamp.plusHours(1),
										hourlyConsumption.lastKey().plusDays(1), remainingConsumption, nettCapacity);
							}

						}
						cheapTimeStamp = lastCheapTimeStamp;
						chargebleConsumption = maxApparentPower;
					}
					totalDemand = totalDemand - chargebleConsumption - currentHourConsumption - remainingConsumption;
					remainingConsumption = 0;
					chargeSchedule.put(cheapTimeStamp, chargebleConsumption);
					System.out.println("tota Demand: " + totalDemand + "chargebleConsumption: " + chargebleConsumption);
					System.out.println(totalDemand);
					chargeSchedule.put(cheapTimeStamp, chargebleConsumption);
					getCheapestHoursIfBatteryNotSufficient(HourlyPrices.firstKey(), cheapTimeStamp);
				} else {
					System.out.println("Not Scheduling ");
					totalDemand = totalDemand - currentHourConsumption;
					System.out.println("tota Demand: " + totalDemand);
					getCheapestHoursIfBatteryNotSufficient(HourlyPrices.firstKey(), cheapTimeStamp);
				}
			}
		}
	}

	private static void getCheapestHoursIfBatterySufficient(LocalDateTime start, LocalDateTime end) {

		availableCapacity -= demand_Till_Cheapest_Hour; // This will be the capacity during cheapest hour.
		long allowedConsumption = nettCapacity - availableCapacity;
		currentHourConsumption = hourlyConsumption.ceilingEntry(cheapTimeStamp.minusDays(1)).getValue();
		// totalDemand -= demand_Till_Cheapest_Hour;
		System.out.println(availableCapacity + " " + demand_Till_Cheapest_Hour);
		System.out.println(chargeSchedule.firstKey());
		chargebleConsumption = totalDemand - availableCapacity - currentHourConsumption;

		System.out.println(chargebleConsumption);
		System.out.println(currentHourConsumption);

		if (chargebleConsumption > 0) {
			System.out.println("greater than 0: ");

			if ((chargebleConsumption > allowedConsumption)) {

				if ((chargebleConsumption > maxApparentPower)) {
					chargeSchedule.put(cheapTimeStamp, maxApparentPower);
					remainingConsumption = chargebleConsumption - maxApparentPower;
					availableCapacity += maxApparentPower;

				} else {
					remainingConsumption = chargebleConsumption - allowedConsumption;
					chargeSchedule.put(cheapTimeStamp, allowedConsumption);
					availableCapacity += allowedConsumption;
				}
				// adjustRemainigConsumption(cheapTimeStamp, lastTimeStamp,
				// remainingConsumption, availableCapacity);
			} else {
				if ((chargebleConsumption > maxApparentPower)) {
					chargeSchedule.put(cheapTimeStamp, maxApparentPower);
					remainingConsumption = chargebleConsumption - maxApparentPower;
					availableCapacity += maxApparentPower;
				}
				System.out.println("putting in schedule: " + chargebleConsumption);
				totalDemand = totalDemand - chargebleConsumption - availableCapacity - currentHourConsumption;
				System.out.println("totalDemand: " + totalDemand);
				chargeSchedule.put(cheapTimeStamp, chargebleConsumption);
			}
		}
	}

	private static void adjustRemainigConsumption(LocalDateTime start, LocalDateTime end, long remainingConsumption,
			long availableCapacity) {

		cheapHour(start, end);
		long currentConsumption = hourlyConsumption.ceilingEntry(cheapTimeStamp.minusDays(1)).getValue();
		demand_Till_Cheapest_Hour = calculateDemandTillThishour(start, cheapTimeStamp.minusDays(1));
		System.out.println("demand_Till_Cheapest_Hour "+ demand_Till_Cheapest_Hour);
		availableCapacity -= demand_Till_Cheapest_Hour;
		System.out.println("availableCapacity "+ availableCapacity);

		long allowedConsumption = nettCapacity - availableCapacity;
		System.out.println("allowedConsumption "+ allowedConsumption);
		System.out.println("remainingConsumption "+ remainingConsumption);

		if (allowedConsumption > 0) {
			if (remainingConsumption > allowedConsumption) {
				//chargebleConsumption = allowedConsumption;
				remainingConsumption -= allowedConsumption;
				availableCapacity = availableCapacity + allowedConsumption - currentConsumption;
				chargeSchedule.put(cheapTimeStamp, allowedConsumption);
				adjustRemainigConsumption(cheapTimeStamp.plusHours(1), end, remainingConsumption, availableCapacity);
			} else {
				//chargebleConsumption = remainingConsumption;
				chargeSchedule.put(cheapTimeStamp, remainingConsumption);
			}
		}else {
			
			availableCapacity -= currentConsumption;
			adjustRemainigConsumption(cheapTimeStamp.plusHours(1), end, remainingConsumption, availableCapacity);
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
		System.out.println(cheapTimeStamp + " " + minPrice);
	}

	private static long calculateDemandTillThishour(LocalDateTime start, LocalDateTime end) {
		long demand = 0;
		for (Entry<LocalDateTime, Long> entry : hourlyConsumption.entrySet()) {
			if ((entry.getKey().isEqual(start) || entry.getKey().isAfter(start)) && entry.getKey().isBefore(end)) {
				demand += entry.getValue();
			}
		}
		System.out.println("demand Till cheap Hour: " + demand);
		return demand;
	}
}
