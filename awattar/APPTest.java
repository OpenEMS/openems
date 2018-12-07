package io.openems.impl.controller.symmetric.awattar;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

public class APPTest {
	private static float minPrice = Float.MAX_VALUE;
	private static LocalDateTime startTimeStamp = null;
	private static TreeMap<LocalDateTime, Float> hourlyPrices = new TreeMap<LocalDateTime, Float>();
	private static LocalDateTime cheapTimeStamp = null;
	private static LocalDateTime lastHour;
	public static TreeMap<LocalDateTime, Long> result = new TreeMap<LocalDateTime, Long>();
	private static long availableConsumption;
	private static long chargebleConsumption;

	public static void main(String[] args) {

		try {

			OkHttpClient client = new OkHttpClient();
			Request request = new Request.Builder().url("https://api.awattar.com/v1/marketdata")
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

			for (JsonElement element : data) {
				JsonObject jsonelement = (JsonObject) element;

				float marketPrice = jsonelement.get("marketprice").getAsFloat();
				long start_Timestamp = jsonelement.get("start_timestamp").getAsLong();
				long end_Timestamp = jsonelement.get("start_timestamp").getAsLong();
				lastHour = LocalDateTime.ofInstant(Instant.ofEpochMilli(end_Timestamp), ZoneId.systemDefault());
				startTimeStamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(start_Timestamp), ZoneId.systemDefault());
				hourlyPrices.put(startTimeStamp, marketPrice);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/*public static TreeMap<LocalDateTime, Long> getCheapestHours(LocalDateTime end) {

		for (Map.Entry<LocalDateTime, Float> entry : hourlyPrices.subMap(hourlyPrices.firstKey(), end).entrySet()) {
			if (entry.getValue() < minPrice) {
				cheapTimeStamp = entry.getKey();
				minPrice = entry.getValue();
			}
		}
		for (Map.Entry<LocalDateTime, Long> entry1 : CalculateTotalConsumption2.hourlyConsumptionData.entrySet()) {
			if (entry1.getKey() == cheapTimeStamp.minusHours(1)) {
				if (availableConsumption >= entry1.getValue()) {
					System.out.println(availableConsumption + " is greater than " + entry1.getValue());
					chargebleConsumption = CalculateTotalConsumption2.TotalConsumption() - availableConsumption;
					result.put(lastHour, chargebleConsumption);
					return result;
				}
				result.put(lastHour, chargebleConsumption);
				minPrice = Float.MAX_VALUE;
				getCheapestHours(cheapTimeStamp);
				return result;
			}

		}

		return result;
	}*/

}
