package io.openems.edge.controller.dynamicbatterycontrol;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

public class Test {

	private static TreeMap<ZonedDateTime, Integer> Consumption = new TreeMap<ZonedDateTime, Integer>();
	private static TreeMap<ZonedDateTime, Integer> production = new TreeMap<ZonedDateTime, Integer>();
	private static TreeMap<ZonedDateTime, Float> bci = new TreeMap<ZonedDateTime, Float>();
	private static List<ZonedDateTime> cheapHours = new ArrayList<ZonedDateTime>();
//	private static Integer remainingCapacity = 0;
	private static Integer remainingEnergy = 0;
	private static Float minPrice;
//	private static ZonedDateTime cheapTimeStamp = null;
	private static ZonedDateTime proLessThanCon = null;
	private static ZonedDateTime proMoreThanCon = null;
	private static ZonedDateTime startHour = null;
	private static int maxMorningHour = 8;
	private static int maxEveningHour = 17;
	private static int nettcapacity = 15000;
	private static int soc = 80;

	public static void main(String[] args) {

		ZonedDateTime now = ZonedDateTime.now().withMinute(0).withSecond(0).withNano(0);

		// generating Dummy bci for 24 hours

		bci.put(now, 30f);
		bci.put(now.plusHours(1), 50f);
		bci.put(now.plusHours(2), 50f);
		bci.put(now.plusHours(3), 50f);
		bci.put(now.plusHours(4), 50f);
		bci.put(now.plusHours(5), 50f);
		bci.put(now.plusHours(6), 50f);
		bci.put(now.plusHours(7), 30f);
		bci.put(now.plusHours(8), 30f);
		bci.put(now.plusHours(9), 30f);
		bci.put(now.plusHours(10), 30f);
		bci.put(now.plusHours(11), 30f);
		bci.put(now.plusHours(12), 30f);
		bci.put(now.plusHours(13), 30f);
		bci.put(now.plusHours(14), 30f);
		bci.put(now.plusHours(15), 30f);
		bci.put(now.plusHours(16), 50f);
		bci.put(now.plusHours(17), 50f);
		bci.put(now.plusHours(18), 50f);
		bci.put(now.plusHours(19), 70f);
		bci.put(now.plusHours(20), 70f);
		bci.put(now.plusHours(21), 70f);
		bci.put(now.plusHours(22), 70f);
		bci.put(now.plusHours(23), 70f);

		// generating Dummy Consumption for 24 hours
		for (int i = 0; i < 24; i++) {
			Consumption.put(now.plusHours(i), (200 + (100 * i)));
		}

		// generating Dummy Production for 24 hours
		for (int i = 0; i < 24; i++) {
			if (now.plusHours(i).getHour() > 8 && now.plusHours(i).getHour() < 17) {
				production.put(now.plusHours(i), (250 + (200 * i)));
			} else {
				production.put(now.plusHours(i), 0);
			}
		}

		// printing Consumption
		for (Map.Entry<ZonedDateTime, Integer> Entry : Consumption.entrySet()) {
			System.out.println("Time: " + Entry.getKey() + " consumption: " + Entry.getValue());
		}

		System.out.println("=========================================================================");

		// printing Production
		for (Map.Entry<ZonedDateTime, Integer> Entry : production.entrySet()) {
			System.out.println("Time: " + Entry.getKey() + " production: " + Entry.getValue());
		}

		System.out.println("=========================================================================");

		// Printing Bci List
		for (Map.Entry<ZonedDateTime, Float> Entry : bci.entrySet()) {
			System.out.println("Time: " + Entry.getKey() + " bci: " + Entry.getValue());
		}

//		remainingCapacity = 12000;

		System.out.println("=========================================================================");
		startHour = now.withMinute(0).withSecond(0).withNano(0);
		calculateBoundaryHours(production, Consumption);

		// Print the boundary Hours
		System.out.println("ProLessThanCon: " + proLessThanCon + " ProMoreThanCon " + proMoreThanCon);

		Integer availableCapacity = (100 / soc) * nettcapacity;

		System.out.println("=========================================================================");

		System.out.println("Available Capacity: " + availableCapacity);

		System.out.println("=========================================================================");

		Integer requiredEnergy = calculateRequiredEnergy(availableCapacity, production, Consumption);

		System.out.println("Required Energy: " + requiredEnergy);

//		
		calculateCheapHours(requiredEnergy);
		System.out.println("=========================================================================");
		cheapHours.forEach(value -> System.out.println(value));
	}

	private static Integer calculateRequiredEnergy(int availableCapacity,
			TreeMap<ZonedDateTime, Integer> hourlyProduction, TreeMap<ZonedDateTime, Integer> hourlyConsumption) {

		int consumptionTotal = 0;
		int requiredEnergy = 0;

		for (Entry<ZonedDateTime, Integer> entry : hourlyConsumption.entrySet()) {
			if (entry.getKey().isAfter(proLessThanCon.minusHours(1))
					&& entry.getKey().isBefore(proMoreThanCon.plusHours(1))) {

				// TODO Confirm production is needed here?
				consumptionTotal += entry.getValue() - hourlyProduction.get(entry.getKey());
			}
		}

		System.out.println("Total Capcaity: " + consumptionTotal);

		// remaining amount of energy that should be covered from grid.
		requiredEnergy = consumptionTotal - availableCapacity;

		return requiredEnergy;
	}

	private static void calculateCheapHours(Integer requiredEnergy) {
		minPrice = Float.MAX_VALUE;
		remainingEnergy = requiredEnergy;
		ZonedDateTime cheapTimeStamp = null;

		for (Map.Entry<ZonedDateTime, Float> entry : bci.subMap(proLessThanCon, proMoreThanCon).entrySet()) {
			if (!cheapHours.contains(entry.getKey())) {
				if (entry.getValue() < minPrice) {
					cheapTimeStamp = entry.getKey();
					minPrice = entry.getValue();
				}
			}
		}

		if (cheapTimeStamp != null) {
			System.out.println("cheapTimeStamp:" + cheapTimeStamp);
			cheapHours.add(cheapTimeStamp);
		}
		//
		for (Map.Entry<ZonedDateTime, Integer> entry : Consumption.entrySet()) {
			if (!cheapHours.isEmpty()) {
				for (ZonedDateTime hours : cheapHours) {
					if (entry.getKey().getHour() == hours.getHour()) {
						remainingEnergy -= entry.getValue();
					}
				}
			}
		}

		if (remainingEnergy > 0) {
			calculateCheapHours(requiredEnergy);
		}
	}

	private static void calculateBoundaryHours(TreeMap<ZonedDateTime, Integer> hourlyProduction,
			TreeMap<ZonedDateTime, Integer> hourlyConsumption) {

		for (ZonedDateTime key : hourlyConsumption.keySet()) {
			Integer production = hourlyProduction.get(key);
			Integer consumption = hourlyConsumption.get(key);

			if (production != null && consumption != null) {
				// last hour of the day when production was greater than consumption
				if ((production > consumption) //
						&& (key.getDayOfYear() == ZonedDateTime.now().getDayOfYear())) {
					proLessThanCon = key;
				}

				// First hour of the day when production was greater than consumption
				if ((production > consumption) //
						&& (key.getDayOfYear() == ZonedDateTime.now().plusDays(1).getDayOfYear())
						&& (proMoreThanCon == null)) {
					proMoreThanCon = key;
				}
			}
		}

		// if there is no enough production available.
		if (proLessThanCon == null) {
			ZonedDateTime now = ZonedDateTime.now();
			proLessThanCon = now.withHour(0).withMinute(0).withSecond(0).withNano(0).plusHours(maxEveningHour);
		}
		if (proMoreThanCon == null) {
			ZonedDateTime now = ZonedDateTime.now();
			proMoreThanCon = now.withHour(0).withMinute(0).withSecond(0).withNano(0).plusHours(maxMorningHour)
					.plusDays(1);
		}
	}

}
