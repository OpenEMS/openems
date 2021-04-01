package io.openems.edge.controller.dynamicdischarge;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Prices {

	/**
	 * This method returns the Hourly prices from API in a {@link TreeMap} format
	 * 
	 * @param url
	 * @param apiKey
	 * @return hourlyPrices {@link TreeMap}
	 * @throws OpenemsNamedException
	 */
	public TreeMap<ZonedDateTime, Float> houlryPrices(String url, String apiKey) throws OpenemsNamedException {

		OkHttpClient client = new OkHttpClient();
		Request request = new Request.Builder().url(url).header("Authorization", Credentials.basic(apiKey, "")).build();

		ZonedDateTime startTimeStamp = null;
		TreeMap<ZonedDateTime, Float> hourlyPrices = new TreeMap<>();
		Response response = null;

		try {
			response = client.newCall(request).execute();
			{
				if (!response.isSuccessful()) {
					throw new IOException("Unexpected code " + response);
				}
			}
			String jsonData = response.body().string();
			JsonObject line = JsonUtils.getAsJsonObject(JsonUtils.parse(jsonData));
			JsonArray data = JsonUtils.getAsJsonArray(line, "data");

			for (JsonElement element : data) {

				float marketPrice = JsonUtils.getAsFloat(element, "marketprice");
				long start_Timestamp = JsonUtils.getAsLong(element, "start_timestamp");
				startTimeStamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(start_Timestamp), ZoneId.systemDefault());
				hourlyPrices.put(startTimeStamp, marketPrice);

			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return hourlyPrices;
	}
}
