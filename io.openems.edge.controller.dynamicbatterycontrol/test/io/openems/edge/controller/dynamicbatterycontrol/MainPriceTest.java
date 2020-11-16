package io.openems.edge.controller.dynamicbatterycontrol;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainPriceTest {

	private static ZonedDateTime timeStampLocalDateTime = null;
	private static TreeMap<ZonedDateTime, Float> HourlyPrices = new TreeMap<ZonedDateTime, Float>();

	public static void main(String[] args) {

		try {

			OkHttpClient client = new OkHttpClient();
			// Request request = new
			// Request.Builder().url("https://api.awattar.com/v1/marketdata?start=1560866400000")
			Request request = new Request.Builder().url("https://portal.blogpv.net/api/bci/signal").build();
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
			JsonArray data = (JsonArray) jsonObject.get("values");

			ZonedDateTime tempDate = null;
			float tempBci = 0;

			for (JsonElement element : data) {
				JsonObject jsonelement = (JsonObject) element;

				long timeStampLong = jsonelement.get("timeStamp").getAsLong();
				float bci = jsonelement.get("bci").getAsFloat();
				timeStampLocalDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timeStampLong),
						ZoneId.systemDefault());

				if (tempDate == null) {
					tempDate = timeStampLocalDateTime;
					tempBci += bci;
				} else {
					if (tempDate.getHour() == timeStampLocalDateTime.getHour()) {
						tempBci += bci;
					} else if (tempDate.isBefore(timeStampLocalDateTime)) {
						HourlyPrices.put(tempDate.withMinute(0).withSecond(0).withNano(0), (100 - (tempBci / 4)));
						tempDate = null;
						tempBci = bci;
					}
				}
			}

			HourlyPrices.entrySet().forEach(entry -> {
				System.out.println("key" + entry.getKey() + "value" + entry.getValue());
			});

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static TreeMap<ZonedDateTime, Float> getHourlyPricesTest() {

		return HourlyPrices;

	}

}
