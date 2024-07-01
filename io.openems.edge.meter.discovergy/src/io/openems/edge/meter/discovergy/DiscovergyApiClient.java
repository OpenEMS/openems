package io.openems.edge.meter.discovergy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.meter.discovergy.jsonrpc.Field;

/**
 * Client for the Discovergy API (<a href=
 * "https://api.discovergy.com/docs/">https://api.discovergy.com/docs/</a>).
 */
public class DiscovergyApiClient {

	private static final String BASE_URL = "https://api.discovergy.com/public/v1";

	private final String authorizationHeader;

	public DiscovergyApiClient(String email, String password) {
		// Generate authorization header
		this.authorizationHeader = "Basic "
				+ new String(Base64.getEncoder().encode((email + ":" + password).getBytes()));
	}

	/**
	 * Returns all meters that the user has access to.
	 *
	 * <p>
	 * See https://api.discovergy.com/docs/ for details.
	 *
	 * @return the Meters as a JsonArray.
	 * @throws OpenemsNamedException on error
	 */
	public JsonArray getMeters() throws OpenemsNamedException {
		return JsonUtils.getAsJsonArray(this.sendGetRequest("/meters"));
	}

	/**
	 * Returns the available measurement field names for the specified meter.
	 *
	 * <p>
	 * See https://api.discovergy.com/docs/ for details.
	 *
	 * @param meterId the Discovergy Meter-ID
	 * @return the Meters as a JsonArray.
	 * @throws OpenemsNamedException on error
	 */
	public JsonArray getFieldNames(String meterId) throws OpenemsNamedException {
		var endpoint = String.format("/field_names?meterId=%s", meterId);
		return JsonUtils.getAsJsonArray(//
				this.sendGetRequest(endpoint));
	}

	/**
	 * Returns the last measurement for the specified meter.
	 *
	 * <p>
	 * See https://api.discovergy.com/docs/ for details.
	 *
	 * @param meterId the Discovergy Meter-ID
	 * @param fields  the fields to be queried
	 * @return the Meters as a JsonArray.
	 * @throws OpenemsNamedException on error
	 */
	public JsonObject getLastReading(String meterId, Field... fields) throws OpenemsNamedException {
		var endpoint = String.format("/last_reading?meterId=%s&fields=%s", //
				meterId, //
				Arrays.stream(fields) //
						.map(Field::n) //
						.collect(Collectors.joining(",")));
		return JsonUtils.getAsJsonObject(//
				this.sendGetRequest(endpoint));
	}

	/**
	 * Sends a get request to the Discovergy API.
	 *
	 * @param endpoint the REST Api endpoint
	 * @return a JsonObject or JsonArray
	 * @throws OpenemsNamedException on error
	 */
	private JsonElement sendGetRequest(String endpoint) throws OpenemsNamedException {
		try {
			var url = new URL(BASE_URL + endpoint);
			var con = (HttpURLConnection) url.openConnection();
			con.setRequestProperty("Authorization", this.authorizationHeader);
			con.setRequestMethod("GET");
			con.setConnectTimeout(5000);
			con.setReadTimeout(5000);
			var status = con.getResponseCode();
			String body;
			try (var in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
				// Read HTTP response
				var content = new StringBuilder();
				String line;
				while ((line = in.readLine()) != null) {
					content.append(line);
					content.append(System.lineSeparator());
				}
				body = content.toString();
			}
			if (status < 300) {
				// Parse response to JSON
				return JsonUtils.parse(body);
			}
			throw new OpenemsException(
					"Error while reading from Discovergy API. Response code: " + status + ". " + body);
		} catch (OpenemsNamedException | IOException e) {
			throw new OpenemsException(
					"Unable to read from Discovergy API. " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}
}
