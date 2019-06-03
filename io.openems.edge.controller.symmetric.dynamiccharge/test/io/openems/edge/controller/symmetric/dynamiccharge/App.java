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

	public static void main(String[] args) {

		PricesTest.houlryPrices();
		HourlyPrices = PricesTest.getHourlyPrices();

		LocalDateTime now = LocalDateTime.now();

		for (int i = 0; i < 19; i++) {

			if (i >= 0 && i <= 15) {
				hourlyConsumption.put(now.minusDays(6).plusHours(i).minusHours(6), (long) (300 + (150 * i)));
			}
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

		System.out.println("That's it: ");

	}

	private static void getCheapestHoursIfBatteryNotSufficient(LocalDateTime start, LocalDateTime end) {

		// function to find the minimum priceHour
		cheapHour(start, end);
		System.out.println("Cheap Price: " + minPrice);
		System.out.println("total Demand: " + totalDemand);

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
				chargebleConsumption = totalDemand - demand_Till_Cheapest_Hour
						- hourlyConsumption.higherEntry(cheapTimeStamp.minusDays(1)).getValue();
				// bufferAmountToCharge = 0;
				System.out.println(hourlyConsumption.higherEntry(cheapTimeStamp.minusDays(1)).getValue());
				System.out.println("chargebleConsumption: " + chargebleConsumption);

				if (chargebleConsumption > 0) {
					System.out.println("greater than 0 ");

					if (chargebleConsumption > maxApparentPower) {

						LocalDateTime lastCheapTimeStamp = cheapTimeStamp;
						float lasttMinPrice = minPrice;
						cheapHour(cheapTimeStamp.plusHours(1), hourlyConsumption.lastKey().plusDays(1));

						if (minPrice < lasttMinPrice) {
							remainingConsumption = chargebleConsumption - maxApparentPower;
							chargebleConsumption = maxApparentPower;
							chargeSchedule.put(lastCheapTimeStamp, maxApparentPower);
							System.out.println("getting into adjusting remaining charge: ");
							// adjustRemainigConsumption(lastCheapTimeStamp.plusHours(1),
							// hourlyConsumption.lastKey().plusDays(1));
						} else {

							if (chargebleConsumption > nettCapacity) {
								bufferAmountToCharge = nettCapacity - maxApparentPower;
								remainingConsumption = chargebleConsumption - nettCapacity;
								// System.out.println("getting into adjusting remaining charge: ");
								// adjustRemainigConsumption(lastCheapTimeStamp.plusHours(1),
								// hourlyConsumption.lastKey().plusDays(1));
								// hourlyConsumption.lastKey().plusDays(1));
							} else {
								bufferAmountToCharge = chargebleConsumption - maxApparentPower;
							}
							
						}
						cheapTimeStamp = lastCheapTimeStamp;
						System.out.println("Buffer Amount: " + bufferAmountToCharge);
						chargebleConsumption = maxApparentPower;
						System.out.println(
								"tota Demand: " + totalDemand + "chargebleConsumption: " + chargebleConsumption);
						
						
					}
					totalDemand = demand_Till_Cheapest_Hour + bufferAmountToCharge;
					bufferAmountToCharge = 0;
					System.out.println(totalDemand);
					chargeSchedule.put(cheapTimeStamp, chargebleConsumption);
					getCheapestHoursIfBatteryNotSufficient(HourlyPrices.firstKey(), cheapTimeStamp);
				} else {
					System.out.println("Not Scheduling ");
					totalDemand = totalDemand - hourlyConsumption.higherEntry(cheapTimeStamp.minusDays(1)).getValue();					
					System.out.println("tota Demand: " + totalDemand);
					getCheapestHoursIfBatteryNotSufficient(HourlyPrices.firstKey(), cheapTimeStamp);
				}
			}
		}
	}

	private static void getCheapestHoursIfBatterySufficient(LocalDateTime start, LocalDateTime end) {

		availableCapacity -= demand_Till_Cheapest_Hour; // This will be the capacity during cheapest hour.
		long allowedConsumption = nettCapacity - availableCapacity;
		// totalDemand -= demand_Till_Cheapest_Hour;
		System.out.println(availableCapacity + " " + demand_Till_Cheapest_Hour);
		chargebleConsumption = totalDemand - availableCapacity
				- hourlyConsumption.higherEntry(cheapTimeStamp.minusDays(1)).getValue();

		System.out.println(chargebleConsumption);
		System.out.println(hourlyConsumption.higherEntry(cheapTimeStamp.minusDays(1)).getValue());

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
				// adjustRemainigConsumption(cheapTimeStamp, lastTimeStamp);
			} else {
				if ((chargebleConsumption > maxApparentPower)) {
					chargeSchedule.put(cheapTimeStamp, maxApparentPower);
					remainingConsumption = chargebleConsumption - maxApparentPower;
					availableCapacity += maxApparentPower;
				}
				System.out.println("putting in schedule: " + chargebleConsumption);
				totalDemand = totalDemand - chargebleConsumption - availableCapacity
						- hourlyConsumption.higherEntry(cheapTimeStamp.minusDays(1)).getValue();
				System.out.println("totalDemand: " + totalDemand);
				chargeSchedule.put(cheapTimeStamp, chargebleConsumption);
			}
		}

	}

	/*
	 * private static void adjustRemainigConsumption(LocalDateTime start,
	 * LocalDateTime end) {
	 * 
	 * if(remainingConsumption > 0{
	 * 
	 * }
	 * 
	 * cheapHour(start, end);
	 * 
	 * 
	 * System.out.println(start.minusDays(1));
	 * System.out.println(cheapTimeStamp.minusDays(1));
	 * System.out.println(hourlyConsumption.higherEntry(cheapTimeStamp.minusDays(1))
	 * .getValue()); long demand =
	 * calculateDemandTillThishour(cheapTimeStamp.minusDays(1).plusHours(1),
	 * end.minusDays(1)) + hourlyConsumption.lastEntry().getValue();
	 * System.out.println("totalDemand: " + demand);
	 * 
	 * 
	 * 
	 * }
	 */

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
