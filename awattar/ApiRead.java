package io.openems.impl.controller.symmetric.awattar;

import java.io.IOException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

public class ApiRead {
	private static float minPrice = Float.MAX_VALUE;
	private static long start_timestamp = 0;
	private static long end_timestamp = 0;

	public static void apiData() {

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

				if (marketPrice < minPrice) {
					minPrice = marketPrice;
					start_timestamp = jsonelement.get("start_timestamp").getAsLong();
					end_timestamp = jsonelement.get("end_timestamp").getAsLong();
				}
			}
			System.out.println("Price: " + minPrice);
			System.out.println("start_timestamp: " + start_timestamp + " end_timestamp: " + end_timestamp);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static long startTimeStamp() {
		return start_timestamp;

	}

	public static long endTimeStamp() {
		return end_timestamp;

	}
}