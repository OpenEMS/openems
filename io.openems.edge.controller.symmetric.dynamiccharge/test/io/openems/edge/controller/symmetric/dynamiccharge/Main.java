package io.openems.edge.controller.symmetric.dynamiccharge;

import java.time.LocalDateTime;
import java.util.TreeMap;
import java.util.Map.Entry;


public class Main {
	private static TreeMap<LocalDateTime, Float> hourlyPrices = new TreeMap<LocalDateTime, Float>();

	public static void main(String[] args) {
		PricesTest.houlryPricesTest();
		hourlyPrices = PricesTest.getHourlyPricesTest();
		
		
		
		for (Entry<LocalDateTime, Float> entry : hourlyPrices.entrySet()) {
			System.out.println("Time: " + entry.getKey() + " Price: " + entry.getValue());
		}
	}
}
