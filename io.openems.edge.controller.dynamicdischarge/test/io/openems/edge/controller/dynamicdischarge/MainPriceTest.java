package io.openems.edge.controller.dynamicdischarge;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainPriceTest {

	public static void main(String[] args) throws OpenemsNamedException, IOException {

		OkHttpClient client = new OkHttpClient();
		// Request request = new
		// Request.Builder().url("https://api.awattar.com/v1/marketdata?start=1560866400000")
		Request request = new Request.Builder().url("https://api.awattar.com/v1/marketdata").header("Authorization", "")
				.build();
		Response response = null;
		LocalDateTime startTimeStamp = null;
		TreeMap<LocalDateTime, Float> HourlyPrices = new TreeMap<LocalDateTime, Float>();

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

			startTimeStamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(start_Timestamp), ZoneId.systemDefault());
			HourlyPrices.put(startTimeStamp, marketPrice);
			System.out.println(startTimeStamp + " " + marketPrice);
		}

	}

}
