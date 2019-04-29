package io.openems.edge.controller.symmetric.dynamiccharge;

import java.time.LocalDateTime;
import java.util.TreeMap;

public class App {
	private static TreeMap<LocalDateTime, Float> HourlyPrices = new TreeMap<LocalDateTime, Float>();
	
	public static void main(String[] args) {
		
		PricesTest.houlryPrices();
		HourlyPrices = PricesTest.getHourlyPrices();
		System.out.println(HourlyPrices.firstKey());
	}
	

}
