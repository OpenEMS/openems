package io.openems.edge.controller.dynamicdischarge;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Prices {

	private static LocalDateTime startTimeStamp = null;
	private TreeMap<LocalDateTime, Float> hourlyPrices = new TreeMap<LocalDateTime, Float>();


	public TreeMap<LocalDateTime, Float> houlryPrices(Config config) {

		try {

			OkHttpClient client = new OkHttpClient();
			Request request = new Request.Builder().url(config.url())
					.header("Authorization", Credentials.basic(config.apikey(), "")).build();
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
			this.hourlyPrices.clear();

			for (JsonElement element : data) {
				JsonObject jsonelement = (JsonObject) element;

				float marketPrice = jsonelement.get("marketprice").getAsFloat();
				long start_Timestamp = jsonelement.get("start_timestamp").getAsLong();
				startTimeStamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(start_Timestamp), ZoneId.systemDefault());
				this.hourlyPrices.put(startTimeStamp, marketPrice);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return this.hourlyPrices;
	}

	public TreeMap<LocalDateTime, Float> getHourlyPrices() {
		return this.hourlyPrices;
	}

}
