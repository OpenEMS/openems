package io.openems.edge.io.shelly.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;

/**
 * Implements the local Shelly REST Api.
 *
 * <p>
 * See https://shelly-api-docs.shelly.cloud
 */
public class ShellyApi {

	private final String baseUrl;

	public ShellyApi(String ip) {
		this.baseUrl = "http://" + ip;
	}

	/**
	 * Gets the status of the device.
	 *
	 * <p>
	 * See https://shelly-api-docs.shelly.cloud/#shelly2-5-status
	 *
	 * @return the status as JsonObject according to Shelly docs
	 * @throws OpenemsNamedException on error
	 */
	public JsonObject getStatus() throws OpenemsNamedException {
		return JsonUtils.getAsJsonObject(this.sendGetRequest("/status"));
	}

	/**
	 * Gets the "ison" state of the relay with the given index.
	 *
	 * <p>
	 * See https://shelly-api-docs.shelly.cloud/#shelly2-5-relay-index
	 *
	 * @param index the index of the relay
	 * @return the boolean value
	 * @throws OpenemsNamedException on error
	 */
	public boolean getRelayIson(int index) throws OpenemsNamedException {
		var json = this.sendGetRequest("/relay/" + index);
		return JsonUtils.getAsBoolean(json, "ison");
	}

	/**
	 * Turns the relay with the given index on or off.
	 *
	 * @param index the index of the relay
	 * @param value true to turn on; false to turn off
	 * @throws OpenemsNamedException on error
	 */
	public void setRelayTurn(int index, boolean value) throws OpenemsNamedException {
		this.sendGetRequest("/relay/" + index + "?turn=" + (value ? "on" : "off"));
	}

	/**
	 * Sends a get request to the Shelly API.
	 *
	 * @param endpoint the REST Api endpoint
	 * @return a JsonObject or JsonArray
	 * @throws OpenemsNamedException on error
	 */
	private JsonElement sendGetRequest(String endpoint) throws OpenemsNamedException {
		try {
			var url = new URL(this.baseUrl + endpoint);
			var con = (HttpURLConnection) url.openConnection();
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
				return JsonUtils.parseToJsonObject(body);
			}
			throw new OpenemsException("Error while reading from Shelly API. Response code: " + status + ". " + body);
		} catch (OpenemsNamedException | IOException e) {
			throw new OpenemsException(
					"Unable to read from Shelly API. " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

}
