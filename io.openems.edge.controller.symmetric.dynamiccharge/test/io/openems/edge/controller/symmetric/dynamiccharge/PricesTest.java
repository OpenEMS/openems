package io.openems.edge.controller.symmetric.dynamiccharge;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PricesTest {

	private static LocalDateTime startTimeStamp = null;
	private static TreeMap<LocalDateTime, Float> HourlyPrices = new TreeMap<LocalDateTime, Float>();

	public static void houlryPricesTest() {

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

			}
			String jsonData = response.body().string();
			JsonParser parser = new JsonParser();
			JsonObject jsonObject = (JsonObject) parser.parse(jsonData);
			JsonArray data = (JsonArray) jsonObject.get("data");

			for (JsonElement element : data) {
				JsonObject jsonelement = (JsonObject) element;

				float marketPrice = jsonelement.get("marketprice").getAsFloat();
				long start_Timestamp = jsonelement.get("start_timestamp").getAsLong();
				startTimeStamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(start_Timestamp), ZoneId.systemDefault());
				HourlyPrices.put(startTimeStamp, marketPrice);

			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static TreeMap<LocalDateTime, Float> getHourlyPricesTest() {

		return HourlyPrices;

	}

}
