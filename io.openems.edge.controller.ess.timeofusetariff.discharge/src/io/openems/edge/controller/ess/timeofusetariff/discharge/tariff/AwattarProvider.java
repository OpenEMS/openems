package io.openems.edge.controller.ess.timeofusetariff.discharge.tariff;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Time-Of-Use Tariff implementation for aWATTar.
 */
public class AwattarProvider implements TimeOfUseTariff {

	private static final String AWATTAR_API_URL = "https://api.awattar.com/v1/marketdata";

	@Override
	public TreeMap<ZonedDateTime, Float> getPrices() {
		try {
			String jsonData = getJsonFromAwattar();
			TreeMap<ZonedDateTime, Float> result = parsePrices(jsonData);
			return result;

		} catch (Exception e) {
			return new TreeMap<>();
		}
	}

	/**
	 * Reads the JSON from aWATTar API.
	 * 
	 * @return a JSON string
	 * @throws IOException on error
	 */
	protected static String getJsonFromAwattar() throws IOException {
		OkHttpClient client = new OkHttpClient();
		Request request = new Request.Builder() //
				.url(AWATTAR_API_URL) //
				// aWATTar currently does not anymore require an Apikey.
				// .header("Authorization", Credentials.basic(apikey, "")) //
				.build();

		Response response = client.newCall(request).execute();
		if (!response.isSuccessful()) {
			throw new IOException("Unexpected code " + response);
		}
		return response.body().string();
	}

	/**
	 * Parse the aWATTar JSON to the Price Map.
	 * 
	 * @param jsonData the aWATTar JSON
	 * @return the Price Map
	 * @throws OpenemsNamedException on error
	 */
	protected static TreeMap<ZonedDateTime, Float> parsePrices(String jsonData) throws OpenemsNamedException {
		TreeMap<ZonedDateTime, Float> result = new TreeMap<>();

		if (!jsonData.isEmpty()) {

			JsonObject line = JsonUtils.getAsJsonObject(JsonUtils.parse(jsonData));
			JsonArray data = JsonUtils.getAsJsonArray(line, "data");

			for (JsonElement element : data) {

				float marketPrice = JsonUtils.getAsFloat(element, "marketprice");
				long startTimestampLong = JsonUtils.getAsLong(element, "start_timestamp");

				// Converting Long time stamp to ZonedDateTime.
				ZonedDateTime startTimeStamp = ZonedDateTime //
						.ofInstant(Instant.ofEpochMilli(startTimestampLong), ZoneId.systemDefault())
						.truncatedTo(ChronoUnit.HOURS);

				// Adding the values in the Map.
				result.put(startTimeStamp, marketPrice);
				result.put(startTimeStamp.plusMinutes(15), marketPrice);
				result.put(startTimeStamp.plusMinutes(30), marketPrice);
				result.put(startTimeStamp.plusMinutes(45), marketPrice);
			}
		}
		return result;
	}
}