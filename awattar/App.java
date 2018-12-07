package io.openems.impl.controller.symmetric.awattar;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.List;
import java.util.Map;
//import java.util.SortedMap;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

public class App {
	private static float minPrice = Float.MAX_VALUE;
	// private static long start_timestamp = 0;
	// private static long end_timestamp = 0;
	private static LocalDateTime cheapest_start_Time_Stamp = null;
	private static LocalDateTime startTimeStamp = null;
	private static LocalDateTime endTimeStamp = null;
	private static TreeMap<LocalDateTime, Float> hourlyData = new TreeMap<LocalDateTime, Float>();
	private static long chargebleConsumption = 0;
	private static LocalDateTime cheapTimeStamp = null;
	private static long totalConsumption = 100000;
	public static TreeMap<LocalDateTime, Long> result = new TreeMap<LocalDateTime, Long>();

	void min(LocalDateTime end) {

		for (Map.Entry<LocalDateTime, Float> entry : hourlyData.entrySet()) {
			if (entry.getValue() < minPrice && entry.getKey().isBefore(end)) {
				end = entry.getKey();
				minPrice = entry.getValue();
			}

		}

	}

	public static void main(String[] args) {

		try {

			OkHttpClient client = new OkHttpClient();
			Request request = new Request.Builder().url("https://api.awattar.com/v1/marketdata?start=1526302800000&end=1526457600000")
					.header("Authorization", Credentials.basic("ak_7YTR42jBwtnk5kXuMZRYEju8hvj918H0", "")).build();
			Response response = null;

			response = client.newCall(request).execute();
			{
				if (!response.isSuccessful()) {
					throw new IOException("Unexpected code " + response);
				}

				// Headers responseHeaders = response.headers();
				// for (int i = 0; i < responseHeaders.size(); i++) {
				// System.out.println(responseHeaders.name(i) + ": " +
				// responseHeaders.value(i));
				// }
				// System.out.println(response.body().string());
			}
			String jsonData = response.body().string();
			JsonParser parser = new JsonParser();
			JsonObject jsonObject = (JsonObject) parser.parse(jsonData);
			JsonArray data = (JsonArray) jsonObject.get("data");
			// SortedMap<LocalDateTime, Float> treemapincl = new TreeMap<LocalDateTime,
			// Float>();

			for (JsonElement element : data) {
				JsonObject jsonelement = (JsonObject) element;

				float marketPrice = jsonelement.get("marketprice").getAsFloat();
				long start_Timestamp = jsonelement.get("start_timestamp").getAsLong();
				long end_Timestamp = jsonelement.get("end_timestamp").getAsLong();
				endTimeStamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(end_Timestamp), ZoneId.systemDefault());
				startTimeStamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(start_Timestamp), ZoneId.systemDefault());
				hourlyData.put(startTimeStamp, marketPrice);

				// if (marketPrice < minPrice) {
				// minPrice = marketPrice;
				// start_timestamp = jsonelement.get("start_timestamp").getAsLong();
				// cheapest_start_Time_Stamp =
				// LocalDateTime.ofInstant(Instant.ofEpochMilli(start_timestamp),
				// ZoneId.systemDefault());
				// end_timestamp = jsonelement.get("end_timestamp").getAsLong();
				// }
			}
			for (Map.Entry<LocalDateTime, Float> entry : hourlyData.entrySet()) {
				//System.out.println(" HOURS "+entry.getKey()+" price "+ entry.getValue());
				System.out.println(entry.getValue());
			}
			// Collection<Float> coll = hourlyData.subMap(hourlyData.firstKey(),
			// cheapest_start_Time_Stamp).values();
			//
			// List<Float> l = new ArrayList<>(coll);
			// l.forEach(list -> {
			// System.out.println(list);
			// });
			// System.out.println("Value of the collection: "+coll);
			// System.out.println();
			//
			// System.out.println("Price: " + minPrice);
			// System.out.println("start_timestamp: " + start_timestamp + " end_timestamp: "
			// + end_timestamp);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static TreeMap<LocalDateTime, Long> getCheapestHours(LocalDateTime start, LocalDateTime end,
			long availableConsumption, long consumptionPerSecond) {

		System.out.println("start: " + start + " end: " + end);

		for (Map.Entry<LocalDateTime, Float> entry : hourlyData.subMap(hourlyData.firstKey(), end).entrySet()) {
			if (entry.getValue() < minPrice) {
				cheapTimeStamp = entry.getKey();
				minPrice = entry.getValue();
			}

		}
		end = cheapTimeStamp;
		System.out.println("start: " + start + " end: " + end);
		long seconds = ChronoUnit.SECONDS.between(hourlyData.firstKey(), end);
		System.out.println(seconds);
		long neededConsumption = consumptionPerSecond * seconds;
		chargebleConsumption = totalConsumption - neededConsumption;
		totalConsumption -= chargebleConsumption;
		System.out.println("chargebleConsumption" + chargebleConsumption);
		System.out.println("neededConsumption" + neededConsumption);
		System.out.println("totalConsumption" + totalConsumption);
		if (availableConsumption >= neededConsumption) {

			System.out.println(availableConsumption + " is greater than " + neededConsumption);
			result.put(end, chargebleConsumption);
			return result;
		}

		System.out.println("lesser");
		result.put(end, chargebleConsumption);
		minPrice = Float.MAX_VALUE;
		getCheapestHours(start, end, availableConsumption, consumptionPerSecond);

		// result.putAll(getCheapestHours(start, end, availableConsumption,
		// consumptionPerSecond));
		return result;

	}

	public static LocalDateTime cheapestStartTimeStamp() {
		return cheapest_start_Time_Stamp;

	}

	public static LocalDateTime endTimeStamp() {
		return endTimeStamp;

	}

}
