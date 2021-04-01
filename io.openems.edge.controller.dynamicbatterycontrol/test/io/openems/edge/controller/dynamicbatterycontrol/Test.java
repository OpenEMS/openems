package io.openems.edge.controller.dynamicbatterycontrol;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
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
	private static int maxMorningHour = 8;
	private static int maxEveningHour = 17;
	private static int nettcapacity = 12000;
	private static int soc = 100;

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
		bci.put(now.plusHours(7), 45f);
		bci.put(now.plusHours(8), 47f);
		bci.put(now.plusHours(9), 49f);
		bci.put(now.plusHours(10), 52f);
		bci.put(now.plusHours(11), 51f);
		bci.put(now.plusHours(12), 50f);
		bci.put(now.plusHours(13), 45f);
		bci.put(now.plusHours(14), 44f);
		bci.put(now.plusHours(15), 32f);
		bci.put(now.plusHours(16), 43f);
		bci.put(now.plusHours(17), 46f);
		bci.put(now.plusHours(18), 47f);
		bci.put(now.plusHours(19), 53f);
		bci.put(now.plusHours(20), 61f);
		bci.put(now.plusHours(21), 62f);
		bci.put(now.plusHours(22), 70f);
		bci.put(now.plusHours(23), 70f);

		// generating Dummy Consumption for 24 hours
		Consumption.put(now.plusHours(0), 3000);
		Consumption.put(now.plusHours(1), 2000);
		Consumption.put(now.plusHours(2), 3000);
		Consumption.put(now.plusHours(3), 2500);
		Consumption.put(now.plusHours(4), 1600);
		Consumption.put(now.plusHours(5), 1750);
		Consumption.put(now.plusHours(6), 1890);
		Consumption.put(now.plusHours(7), 2000);
		Consumption.put(now.plusHours(8), 2200);
		Consumption.put(now.plusHours(9), 2500);
		Consumption.put(now.plusHours(10), 1900);
		Consumption.put(now.plusHours(11), 3200);
		Consumption.put(now.plusHours(12), 3500);
		Consumption.put(now.plusHours(13), 3600);
		Consumption.put(now.plusHours(14), 2700);
		Consumption.put(now.plusHours(15), 2500);
		Consumption.put(now.plusHours(16), 2700);
		Consumption.put(now.plusHours(17), 2900);
		Consumption.put(now.plusHours(18), 3000);
		Consumption.put(now.plusHours(19), 3200);
		Consumption.put(now.plusHours(20), 3300);
		Consumption.put(now.plusHours(21), 3400);
		Consumption.put(now.plusHours(22), 1800);
		Consumption.put(now.plusHours(23), 1900);

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
			System.out.println("Time: " + Entry.getKey() + " bci: " + (100 - Entry.getValue()));
		}

		System.out.println("=========================================================================");
		ZonedDateTime startHour = now.withMinute(0).withSecond(0).withNano(0);
		calculateBoundaryHours(production, Consumption, startHour);

		// Print the boundary Hours
		System.out.println("ProLessThanCon: " + proLessThanCon + " ProMoreThanCon " + proMoreThanCon);

		Integer availableCapacity = nettcapacity * soc / 100;

		System.out.println("=========================================================================");

		System.out.println("Available Capacity in the Battery: " + availableCapacity);

		System.out.println("=========================================================================");

		Integer requiredEnergy = calculateRequiredEnergy(availableCapacity, production, Consumption);

		System.out.println("Required Energy to cover from Grid: " + requiredEnergy);

		calculateCheapHours(requiredEnergy);
		System.out.println("=========================================================================");

		cheapHours.forEach(value -> System.out.println(value));

		Collections.sort(cheapHours);

		System.out.println("=========================================================================");

		cheapHours.forEach(value -> System.out.println(value));

		TreeMap<ZonedDateTime, Integer> batteryStateSchedule = withBatterySchedule(cheapHours, availableCapacity);

		System.out.println("=========================================================================");

		batteryStateSchedule.entrySet().forEach(a -> {
			System.out.println("Key " + a.getKey() + " value: " + a.getValue());
		});

		TreeMap<ZonedDateTime, Integer> batteryStateWithoutSchedule = withoutBatterySchedule(availableCapacity);

		System.out.println("=========================================================================");

		batteryStateWithoutSchedule.entrySet().forEach(a -> {
			System.out.println("Key " + a.getKey() + " value: " + a.getValue());
		});

	}

	private static Integer calculateRequiredEnergy(int availableCapacity,
			TreeMap<ZonedDateTime, Integer> hourlyProduction, TreeMap<ZonedDateTime, Integer> hourlyConsumption) {

		int consumptionTotal = 0;
		int requiredEnergy = 0;

		for (Entry<ZonedDateTime, Integer> entry : hourlyConsumption.subMap(proLessThanCon, proMoreThanCon)
				.entrySet()) {

			// TODO Confirm production is needed here?
			consumptionTotal += entry.getValue() - hourlyProduction.get(entry.getKey());
		}

		System.out.println("Consumption for whole night: " + consumptionTotal);

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
//			System.out.println("cheapTimeStamp:" + cheapTimeStamp);
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
			TreeMap<ZonedDateTime, Integer> hourlyConsumption, ZonedDateTime startHour) {

		for (Map.Entry<ZonedDateTime, Integer> entry : hourlyConsumption.entrySet()) {

			Integer consumption = entry.getValue();
			Integer production = hourlyProduction.get(entry.getKey());

			if (production != null && consumption != null) {
				// last hour of the day when production was greater than consumption
				if ((production > consumption) //
						&& (entry.getKey().getDayOfYear() == ZonedDateTime.now().getDayOfYear())) {
					proLessThanCon = entry.getKey();
				}

				// First hour of the day when production was greater than consumption
				if ((production > consumption) //
						&& (entry.getKey().getDayOfYear() == ZonedDateTime.now().plusDays(1).getDayOfYear()) //
						&& (proMoreThanCon == null) //
						&& (entry.getKey().getHour() <= maxMorningHour)) {
					proMoreThanCon = entry.getKey();
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

	private static TreeMap<ZonedDateTime, Integer> withBatterySchedule(List<ZonedDateTime> cheapHours,
			Integer availableCapacity) {

		TreeMap<ZonedDateTime, Integer> batteryState = new TreeMap<ZonedDateTime, Integer>();
		batteryState.put(proLessThanCon.minusHours(1), availableCapacity);

		for (Entry<ZonedDateTime, Integer> entry : Consumption.subMap(proLessThanCon, proMoreThanCon).entrySet()) {
			if (!cheapHours.contains(entry.getKey())) {
				availableCapacity -= Consumption.get(entry.getKey());
			}
			batteryState.put(entry.getKey(), availableCapacity);
		}
		return batteryState;
	}

	private static TreeMap<ZonedDateTime, Integer> withoutBatterySchedule(Integer availableCapacity) {

		TreeMap<ZonedDateTime, Integer> batteryState = new TreeMap<ZonedDateTime, Integer>();

		batteryState.put(proLessThanCon.minusHours(1), availableCapacity);

		for (Entry<ZonedDateTime, Integer> entry : Consumption.subMap(proLessThanCon, proMoreThanCon).entrySet()) {
			if (availableCapacity < 0) {
				availableCapacity = 0;
			}
			availableCapacity -= Consumption.get(entry.getKey());
			batteryState.put(entry.getKey(), availableCapacity);
		}
		return batteryState;
	}
}
