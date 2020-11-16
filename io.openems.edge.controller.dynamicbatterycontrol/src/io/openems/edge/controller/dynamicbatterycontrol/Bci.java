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

public class Bci {

	private static ZonedDateTime timeStampLocalDateTime = null;
	private TreeMap<ZonedDateTime, Float> bciList = new TreeMap<ZonedDateTime, Float>();

	public TreeMap<ZonedDateTime, Float> houlryPrices(String url) {

		try {

			OkHttpClient client = new OkHttpClient();
			Request request = new Request.Builder().url(url).build();
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
			this.bciList.clear();

			if (data == null) {
				return null;
			}

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

						this.bciList.put(tempDate.withMinute(0).withSecond(0).withNano(0), (100 - (tempBci / 4)));
						tempDate = null;
						tempBci = bci;

					}
				}

			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return this.bciList;
	}

}
