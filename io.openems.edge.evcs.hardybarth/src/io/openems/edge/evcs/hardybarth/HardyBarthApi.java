package io.openems.edge.evcs.hardybarth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;

/**
 * Implements the Hardy Barth Api.
 */
public class HardyBarthApi {

	private final String baseUrl;
	private final String authorizationHeader;
	private final EvcsHardyBarthImpl hardyBarthImpl;

	public HardyBarthApi(String ip, EvcsHardyBarthImpl hardyBarthImpl) {
		this.baseUrl = "http://" + ip;
		this.authorizationHeader = "Basic ";
		this.hardyBarthImpl = hardyBarthImpl;
	}

	/**
	 * Sends a get request to the Hardy Barth.
	 *
	 * @param endpoint the REST Api endpoint
	 * @return a JsonObject or JsonArray
	 * @throws OpenemsNamedException on error
	 */
	public JsonElement sendGetRequest(String endpoint) throws OpenemsNamedException {
		var putRequestFailed = false;
		JsonObject result = null;

		try {
			// Create URL like "http://192.168.8.101/api/"
			var url = new URL(this.baseUrl + endpoint);

			// Open http url connection
			var con = (HttpURLConnection) url.openConnection();

			// Set general information
			con.setRequestProperty("Authorization", this.authorizationHeader);
			con.setRequestMethod("GET");
			con.setConnectTimeout(5000);
			con.setReadTimeout(5000);

			// Read response
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

			// Get response code
			var status = con.getResponseCode();
			if (status >= 300) {
				putRequestFailed = true;
				throw new OpenemsException(
						"Error while reading from Hardy Barth API. Response code: " + status + ". " + body);
			}
			putRequestFailed = false;
			// Parse response to JSON
			result = JsonUtils.parseToJsonObject(body);
		} catch (OpenemsNamedException | IOException e) {
			putRequestFailed = true;
		}

		// Set state and return result
		this.hardyBarthImpl._setChargingstationCommunicationFailed(putRequestFailed);
		return result;
	}

	/**
	 * Sends a get request to the Hardy Barth.
	 *
	 * @param endpoint the REST Api endpoint @return a JsonObject or
	 *                 JsonArray @throws OpenemsNamedException on error @throws
	 * @param key      The key in the properties
	 * @param value    The value of the key property
	 * @return A JsonObject
	 * @throws OpenemsNamedException on error
	 */
	public JsonObject sendPutRequest(String endpoint, String key, String value) throws OpenemsNamedException {
		var putRequestFailed = false;
		JsonObject result = null;

		try {
			// Create URL like "http://192.168.8.101/api/"
			var url = new URL(this.baseUrl + endpoint);

			// Open http url connection
			var connection = (HttpURLConnection) url.openConnection();

			// Set general information
			connection.setRequestProperty("Authorization", this.authorizationHeader);
			connection.setRequestMethod("PUT");
			connection.setDoOutput(true);
			connection.setConnectTimeout(5000);
			connection.setReadTimeout(5000);

			// Write "topic" and "value" on request properties
			var osw = new OutputStreamWriter(connection.getOutputStream());
			osw.write("{\"" + key + "\":\"" + value + "\"}");
			osw.flush();
			osw.close();

			String body;
			try (var in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {

				// Read HTTP response
				var content = new StringBuilder();
				String line;
				while ((line = in.readLine()) != null) {
					content.append(line);
					content.append(System.lineSeparator());
				}
				body = content.toString();
			}

			// Get response code
			var status = connection.getResponseCode();
			if ((status >= 300) && (status >= 0)) {
				// Respond error status-code
				putRequestFailed = true;
				throw new OpenemsException(
						"Error while reading from Hardy Barth API. Response code: " + status + ". " + body);
			}
			// Result OK
			result = JsonUtils.parseToJsonObject(body);
		} catch (IOException e) {
			putRequestFailed = true;
		}

		// Set state and return result
		this.hardyBarthImpl._setChargingstationCommunicationFailed(putRequestFailed);
		return result;
	}
}
