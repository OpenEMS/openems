package io.openems.impl.controller.symmetric.awattar;

import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JsonData {

	private static float minPrice = Float.MAX_VALUE;
	private static long start_timestamp = 0;
	private static long end_timestamp = 0;
	private static int cheapestHour = 0;
	private static LocalDateTime startTimeStamp = null;
	private static LocalDateTime start_Time_Stamp = null;
	private static LocalDateTime endTimeStamp = null;

	public static void jsonRead(String fileName) {
		try {

			JsonParser parser = new JsonParser();

			URL url = JsonData.class.getResource(fileName);

			Object obj;
			obj = (parser).parse(new FileReader(url.getPath())).getAsJsonObject();
			JsonObject jsonObject = (JsonObject) obj;
			JsonArray data = (JsonArray) jsonObject.get("data");
			//List<Float> hourlyPrices = new ArrayList<Float>();
			Map<LocalDateTime, Float> HourlyData = new TreeMap<>();

			for (JsonElement element : data) {
				JsonObject jsonelement = (JsonObject) element;

				float marketPrice = jsonelement.get("marketprice").getAsFloat();
				long start_Timestamp = jsonelement.get("start_timestamp").getAsLong();
				startTimeStamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(start_Timestamp), ZoneId.systemDefault());
				long end_Timestamp = jsonelement.get("start_timestamp").getAsLong();
				endTimeStamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(end_Timestamp), ZoneId.systemDefault());
				HourlyData.put(startTimeStamp, marketPrice);
				//hourlyPrices.add(((JsonObject) element).get("marketprice").getAsFloat());


				if (marketPrice < minPrice) {
					minPrice = marketPrice;
					start_timestamp = jsonelement.get("startTimeStamp").getAsLong();
					start_Time_Stamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(start_timestamp), ZoneId.systemDefault());
					end_timestamp = jsonelement.get("end_timestamp").getAsLong();
				}
			}

			for (Map.Entry<LocalDateTime, Float> entry : HourlyData.entrySet()) {
				System.out.println("Key: " + entry.getKey() + ". Value: " + entry.getValue());
			}
			System.out.println("Price: " + minPrice);
			System.out.println("start_timestamp: " + start_timestamp + " end_timestamp: " + end_timestamp);
			//startTimeStamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(start_timestamp), ZoneId.systemDefault());
			// endTimeStamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(end_timestamp), ZoneId.systemDefault());
			cheapestHour = startTimeStamp.getHour();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*public static List<LocalDateTime> getCheapestHours(LocalDateTime start, LocalDateTime end, float availableConsumption, float consumptionPerSecond) {
		List<LocalDateTime> result = new ArrayList<>();

		if (true) {
			return result;
		}
		result.addAll(getCheapestHours(start, end, availableConsumption, consumptionPerSecond));
		return result;
	}*/


	public static int getCheapestHour() {
		return cheapestHour;
	}

	public static LocalDateTime startTimeStamp() {
		return start_Time_Stamp;

	}

	public static LocalDateTime end_TimeStamp() {
		return endTimeStamp;

	}

}