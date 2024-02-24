package io.openems.edge.io.opendtu.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;

/**
 * Implements the local openDTU REST Api.
 *
 * <p>
 * See https://github.com/tbnobody/OpenDTU
 */
public class OpendtuApi {

	private final String baseUrl;
    private final String username; // for basic auth
    private final String password; // for basic auth


    public OpendtuApi(String ip, String username, String password) {
        this.baseUrl = "http://" + ip;
        this.username = username;
        this.password = password;
    }
    
    /**
     * Gets the power limit status of all inverters.
     *
     * @return the power limit status as JsonObject
     * @throws OpenemsNamedException on error
     */
    public JsonObject getLimitStatus() throws OpenemsNamedException {
        return JsonUtils.getAsJsonObject(this.sendRequest("GET", "/api/limit/status", null));
    }

    /**
     * Sets the relative power limit of a specific inverter.
     *
     * @param serial the inverter's serial number
     * @param limitType the type of limit
     * @param limitValue the value of the limit
     * @return the response as JsonObject
     * @throws OpenemsNamedException on error
     */
    public JsonObject setPowerLimit(String serial, int limitType, int limitValue) throws OpenemsNamedException {
        JsonObject innerData = new JsonObject();
        innerData.addProperty("serial", serial);
        innerData.addProperty("limit_value", limitValue);
        innerData.addProperty("limit_type", limitType);
        
        // Convert innerData to a String, and then parse it back to a JsonObject
        JsonObject payload = innerData;
        return JsonUtils.getAsJsonObject(this.sendRequest("POST", "/api/limit/config", payload));
    }



	/**
	 * Gets the status of the device.
	 *
	 * <p>
	 * See https://github.com/tbnobody/OpenDTU
	 *
	 * @return the status as JsonObject according to Shelly docs
	 * @throws OpenemsNamedException on error
	 */
    public JsonObject getStatusForInverter(String serialNumber) throws OpenemsNamedException {
        return JsonUtils.getAsJsonObject(this.sendGetRequest("/api/livedata/status?inv=" + serialNumber));
    }


	/**
	 * Sends a get request to the openDTU.
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
			throw new OpenemsException("Error while reading from openDTU API. Response code: " + status + ". " + body);
		} catch (OpenemsNamedException | IOException e) {
			throw new OpenemsException(
					"Unable to read from openDTU API. " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

	
	
	
    /**
     * Writes a value to the openDTU.
     *
     * @param endpoint the REST Api endpoint
     * @param data the JSON data to be written
     * @throws OpenemsNamedException on error
     */
    public void writeValue(String endpoint, JsonObject data) throws OpenemsNamedException {
        this.sendRequest("POST", endpoint, data);
    }

    private JsonElement sendRequest(String method, String endpoint, JsonObject data) throws OpenemsNamedException {
        try {
            var url = new URL(this.baseUrl + endpoint);
            var con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod(method);
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            
            // Add Basic Authentication header
            String auth = this.username + ":" + this.password;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            con.setRequestProperty("Authorization", "Basic " + encodedAuth);
            
            if ("POST".equals(method) && data != null) {
                con.setDoOutput(true);
                con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); // Set the content type
                String postData = "data=" + data.toString();

                // Debug Log: Displaying the POST data being sent
             //   System.out.println("Sending POST request to URL: " + url.toString());
              //  System.out.println("POST Data: " + postData);

                try (var out = new OutputStreamWriter(con.getOutputStream())) {
                    out.write(postData); // Write the formatted POST data
                }
            }

            var status = con.getResponseCode();
            String body;
            try (var in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                var content = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    content.append(line);
                    content.append(System.lineSeparator());
                }
                body = content.toString();
            }
            
            if (status < 300) {
                return JsonUtils.parseToJsonObject(body);
            }
            
            throw new OpenemsException("Error with openDTU API. Response code: " + status + ". " + body);
        } catch (OpenemsNamedException | IOException e) {
            throw new OpenemsException(
                    "Unable to communicate with openDTU API. " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

}
