package io.openems.edge.evcs.dezony;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;

/**
 * Implements the dezony REST-Api.
 */
public class DezonyApi {

	private final String baseUrl;
	private final EvcsDezonyImpl parent;

	public DezonyApi(String ip, int port, EvcsDezonyImpl parent) {
		this.baseUrl = "http://" + ip + ":" + port;
		this.parent = parent;
	}

	/**
	 * Sends a GET Request to read the State.
	 * 
	 * @return result or null
	 * @throws OpenemsNamedException on error
	 */
	public JsonElement getState() throws OpenemsNamedException {
		return this.sendGetRequest("/api/v1/state");
	}

	/**
	 * Sends a GET Request to read the last metrics.
	 * 
	 * @return result or null
	 * @throws OpenemsNamedException on error
	 */
	public JsonElement getLastMetrics() throws OpenemsNamedException {
		return this.sendGetRequest("/api/v1/metrics/last");
	}

	/**
	 * Sends a GET Request to unlock charging.
	 * 
	 * @return result or null
	 * @throws OpenemsNamedException on error
	 */
	public JsonElement enableCharging() throws OpenemsNamedException {
		return this.sendPostRequest("/api/v1/charging/unlock");
	}

	/**
	 * Sends a GET Request to lock charging.
	 * 
	 * @return true if `charging_is_locked`; false otherwise
	 * @throws OpenemsNamedException on error
	 */
	public boolean disableCharging() throws OpenemsNamedException {
		final var response = this.sendPostRequest("/api/v1/charging/lock");
		final var result = JsonUtils.getAsOptionalBoolean(response, "charging_is_locked");

		return result.orElse(false);
	}

	/**
	 * Sends a POST Request to set the charging current.
	 * 
	 * @param current the new charging current in [mA]
	 * @return response charging_current
	 * @throws OpenemsNamedException on error
	 */
	public Optional<String> setCurrent(int current) throws OpenemsNamedException {
		final var response = this.sendPostRequest("/api/v1/charging/current?value=" + current);

		return JsonUtils.getAsOptionalString(response, "charging_current");
	}

	/**
	 * Sends a get request.
	 *
	 * @param endpoint the REST Api endpoint
	 * @return a JsonObject or JsonArray
	 * @throws OpenemsNamedException on error
	 */
	public JsonElement sendGetRequest(String endpoint) throws OpenemsNamedException {
		var getRequestFailed = false;
		JsonObject result = null;

		try {
			URL url = new URL(this.baseUrl + endpoint);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();

			con.setRequestMethod("GET");
			con.setConnectTimeout(5000);
			con.setReadTimeout(5000);

			try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
				StringBuilder content = new StringBuilder();
				String line;

				while ((line = in.readLine()) != null) {
					content.append(line);
					content.append(System.lineSeparator());
				}

				String body = content.toString();

				// Get response code
				int status = con.getResponseCode();

				if (status >= 300) {
					throw new OpenemsException(
							"Error while reading from dezony API. Response code: " + status + ". " + body);
				}

				result = JsonUtils.parseToJsonObject(body);
			}
		} catch (IOException e) {
			// Log the error
			e.printStackTrace();
			getRequestFailed = true;

		} finally {
			this.parent._setChargingstationCommunicationFailed(getRequestFailed);
		}

		// TODO consider throwing an Exception if result is null here
		return result;
	}

	/**
	 * Sends a post request to the dezony.
	 *
	 * @param endpoint the REST Api endpoint @return a JsonObject or
	 *                 JsonArray @throws OpenemsNamedException on error @throws
	 * @return A JsonObject
	 * @throws OpenemsNamedException on error
	 */
	public JsonObject sendPostRequest(String endpoint) throws OpenemsNamedException {
		var postRequestFailed = false;
		JsonObject result = null;

		try {
			var url = new URL(this.baseUrl + endpoint);
			var connection = (HttpURLConnection) url.openConnection();

			// Set general information
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			connection.setConnectTimeout(5000);
			connection.setReadTimeout(5000);

			// Send request and read response
			try (var in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
				var content = new StringBuilder();
				String line;
				while ((line = in.readLine()) != null) {
					content.append(line);
					content.append(System.lineSeparator());
				}
				var body = content.toString();

				// Get response code
				var status = connection.getResponseCode();
				if (status >= 300) {
					// Respond error status-code
					postRequestFailed = true;
					throw new OpenemsException(
							"Error while reading from dezony API. Response code: " + status + ". " + body);
				}

				// Result OK
				result = JsonUtils.parseToJsonObject(body);
			}
		} catch (IOException e) {
			postRequestFailed = true;
		} finally {
			// Set state
			this.parent._setChargingstationCommunicationFailed(postRequestFailed);
		}

		return result;
	}
}
