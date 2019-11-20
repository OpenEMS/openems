package io.openems.edge.controller.dynamicdischarge;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Test {

	private static Map<LocalDateTime, Integer> Map1 = new TreeMap<LocalDateTime, Integer>();
	private static Map<LocalDateTime, Integer> Consumption = new TreeMap<LocalDateTime, Integer>();
	private static Map<LocalDateTime, Float> prices = new TreeMap<LocalDateTime, Float>();
	private static List<LocalDateTime> cheapHours = new ArrayList<LocalDateTime>();
	private static Integer consumptionTotal = 0;
	private static Integer remainingCapacity = 0;
	private static Integer remainingEnergy = 0;
	private static Integer nettCapacity = 0;
	private static Float minPrice;
	private static LocalDateTime cheapTimeStamp = null;
	private static boolean check = false;

	public static void main(String[] args) {

		LocalDateTime now = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);

		Map1.put(now, 1000);
		Map1.put(now.plusHours(1), 2000);
		Map1.put(now.plusHours(2), 5000);
		Map1.put(now.plusHours(3), 9000);
		Map1.put(now.plusHours(4), 10000);
		Map1.put(now.plusHours(5), 6000);
		Map1.put(now.plusHours(6), 11000);
		Map1.put(now.plusHours(7), 8000);

		Consumption.put(now, 1000);
		Consumption.put(now.plusHours(1), 3000);
		Consumption.put(now.plusHours(2), 4000);
		Consumption.put(now.plusHours(3), 5000);
		Consumption.put(now.plusHours(4), 6000);
		Consumption.put(now.plusHours(5), 7000);
		Consumption.put(now.plusHours(6), 8000);

		prices.put(now, 10.0f);
		prices.put(now.plusHours(1), 20.0f);
		prices.put(now.plusHours(2), 40.00f);
		prices.put(now.plusHours(3), 30.00f);
		prices.put(now.plusHours(4), 05.00f);
		prices.put(now.plusHours(5), 7.00f);
		prices.put(now.plusHours(6), 45.00f);

//		for(LocalDateTime key: Map1.keySet()) {
//			System.out.println(Map1.get(key));
//			System.out.println(Consumption.get(key));
//			int pro = Map1.get(key);
//			int con = Consumption.get(key);
//			if(pro != con) {
//				System.out.println(Map1.get(key) + "is not greatrer than" + Consumption.get(key));
//				return;
//			}
//		}

		nettCapacity = 12000;
		consumptionTotal = 22000;
		remainingCapacity = 22000;

		calculateCheapHours();
		cheapHours.forEach(value -> System.out.println(value));
	}

	private static TreeMap<LocalDateTime, Integer> calculateAdjustedHourlyProduction(int availableCapacity) {
		Integer[] productionValues = { 0, 0, 0, 0, 0, //
				0, 0, 0, 0, 0, //
				0, 0, 0, 0, 0, //
				0, 0, 0, 0, 0, //
				0, 0, 0, 0 };
		LocalDateTime startHour = LocalDateTime.now();

		TreeMap<LocalDateTime, Integer> hourlyPro = new TreeMap<LocalDateTime, Integer>();
		int j = productionValues.length;
		float diffRate = 0;
		int value = 0;
		for (int i = 0; i <= productionValues.length; i++) {
			if (productionValues[i] > productionValues[j]) {
				diffRate = productionValues[i] / productionValues[j];
				value = (int) (((diffRate * productionValues[i]) + productionValues[j]) / (diffRate + 1));
			} else {
				diffRate = productionValues[j] / productionValues[i];
				value = (int) (((diffRate * productionValues[j]) + productionValues[i]) / (diffRate + 1));
			}
			hourlyPro.put(startHour.plusHours(i), value);
			j = j - 1;


		}
		return null;

	}

	private static void check(List<LocalDateTime> cheapHour, LocalDateTime priceHour) {
		check = false;

		for (LocalDateTime hours : cheapHours) {
			if (priceHour == hours) {
				check = true;
			}
		}
	}

	private static void calculateCheapHours() {
		minPrice = Float.MAX_VALUE;
		remainingEnergy = remainingCapacity;

		// Calculates the cheapest price hour within certain Hours.
		if (!cheapHours.isEmpty()) {
			for (Map.Entry<LocalDateTime, Float> entry : prices.entrySet()) {
//				check(cheapHours, entry.getKey());	
//				if(!check) {

				if (!cheapHours.contains(entry.getKey())) {
					if (entry.getValue() < minPrice) {
						cheapTimeStamp = entry.getKey();
						minPrice = entry.getValue();
					}
				}

//				for (LocalDateTime hours : cheapHours) {
//					System.out.println(":" + entry.getKey() + hours);
//					if (entry.getKey() != hours) {
//						System.out.println(entry.getKey() + "!= " + hours);
//						if (entry.getValue() < minPrice) {
//							cheapTimeStamp = entry.getKey();
//							minPrice = entry.getValue();
//						}
//					}
//				}
			}
			System.out.println("cheapTimeStamp:" + cheapTimeStamp);
			cheapHours.add(cheapTimeStamp);
		} else {
			minPrice = prices.values() //
					.stream() //
					.min(Float::compare) //
					.get();
			for (Map.Entry<LocalDateTime, Float> entry : prices.entrySet()) {
				if (minPrice.equals(entry.getValue())) {
					cheapTimeStamp = entry.getKey();
				}
			}
			cheapHours.add(cheapTimeStamp);
			minPrice = Float.MAX_VALUE;
		}

		System.out.println("cheapTimeStamp: " + cheapTimeStamp);

		//
		for (Map.Entry<LocalDateTime, Integer> entry : Consumption.entrySet()) {
			if (!cheapHours.isEmpty()) {
				for (LocalDateTime hours : cheapHours) {
					// System.out.println(":" + entry.getKey() + hours);
					if (entry.getKey().getHour() == hours.getHour()) {
						System.out.println("entering:" + entry.getKey() + hours);
						remainingEnergy -= entry.getValue();
					}
				}
			}
		}
		System.out.println("remainingEnergy: " + remainingEnergy);

		if (remainingEnergy > 0) {
			calculateCheapHours();
		}
	}
}
