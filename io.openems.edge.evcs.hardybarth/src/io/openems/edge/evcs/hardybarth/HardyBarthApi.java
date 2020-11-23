package io.openems.edge.evcs.hardybarth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;

/**
 * Implements the Hardy Barth Api.
 * 
 */
public class HardyBarthApi {

	private final String baseUrl;
	
	private final String authorizationHeader;

	public HardyBarthApi(String ip) {
		this.baseUrl = "http://" + ip;
		this.authorizationHeader = "Basic ";
	}

	/**
	 * Sends a get request to the Hardy Barth.
	 * 
	 * @param endpoint the REST Api endpoint
	 * @return a JsonObject or JsonArray
	 * @throws OpenemsNamedException on error
	 */
	public JsonElement sendGetRequest(String endpoint) throws OpenemsNamedException {
		try {
			URL url = new URL(this.baseUrl + endpoint);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestProperty("Authorization", this.authorizationHeader);
			con.setRequestMethod("GET");
			con.setConnectTimeout(5000);
			con.setReadTimeout(5000);
			int status = con.getResponseCode();
			String body;
			try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
				// Read HTTP response
				StringBuilder content = new StringBuilder();
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
			} else {
				throw new OpenemsException(
						"Error while reading from Hardy Barth API. Response code: " + status + ". " + body);
			}
		} catch (OpenemsNamedException | IOException e) {
			throw new OpenemsException(
					"Unable to read from Hardy Barth API. " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

}
