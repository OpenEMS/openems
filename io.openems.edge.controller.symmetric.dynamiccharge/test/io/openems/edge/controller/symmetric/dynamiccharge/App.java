package io.openems.edge.controller.symmetric.dynamiccharge;

import java.time.LocalDateTime;
import java.util.Map.Entry;
import java.util.TreeMap;

public class App {
	private static TreeMap<LocalDateTime, Float> HourlyPrices = new TreeMap<LocalDateTime, Float>();

	public static void main(String[] args) {

		PricesTest.houlryPrices();
		HourlyPrices = PricesTest.getHourlyPrices();

		for (Entry<LocalDateTime, Float> entry : HourlyPrices.entrySet()) {
			System.out.println("Time: " + entry.getKey() + " Price: " + entry.getValue());
		}

	}
}
