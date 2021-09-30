package io.openems.edge.shelly.core;

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

import io.openems.common.worker.AbstractCycleWorker;


/**
 * Implements the local Shelly REST Api.
 * 
 * <p>
 * See https://shelly-api-docs.shelly.cloud
 */
public class ShellyApi  {

	private ShellyCore parent;
	private final String baseUrl;
	private String shellyType;
	private int numMeters, numRelays, numEmeters;
	private boolean commFailed;
	private boolean valid;
	JsonObject status;

	public ShellyApi(ShellyCore parent,String ip) {
		this.baseUrl = "http://" + ip;
		this.parent = parent;
		this.resetBaseValues();
		this.commFailed = false;
	}

	/**
	 * Used to initialize members
	 */
	private void resetBaseValues() {
		this.numEmeters = 0;
		this.numMeters = 0;
		this.numRelays = 0;
		this.shellyType = "Unknown";
		this.valid = false;
		
		setParentBaseChannels();
		setParentDynChannels();
	}

	private void setParentDynChannels() {
		if(parent != null) {
			parent._setCommunicationFailed(this.commFailed);
		}
	}
	
	private void setParentBaseChannels() {
		if(parent != null) {
			parent._setShellyType(this.shellyType);
		}
	}
	
	private boolean readBaseValues() {
		JsonObject device;

		try {
			device = JsonUtils.getAsJsonObject(this.sendGetRequest("/shelly"));
			this.shellyType = JsonUtils.getAsString(device,"type");			
		} catch (OpenemsNamedException e) {			
			this.resetBaseValues();
			this.commFailed = true;
			parent._setCommunicationFailed(this.commFailed);
			return false;
		}	

		try {
			this.numRelays = JsonUtils.getAsInt(device,"num_outputs");			
		} catch (OpenemsNamedException e) {
			this.numRelays = 0;
		}

		try {
			this.numMeters = JsonUtils.getAsInt(device,"num_meters");			
		} catch (OpenemsNamedException e) {
			this.numMeters = 0;
		}

		try {
			this.numMeters = JsonUtils.getAsInt(device,"num_emeters");			
		} catch (OpenemsNamedException e) {
			this.numEmeters = 0;
		}

		this.valid = true;		
		return true;
	}


	void iterate() {

		if(!this.valid) {
			if(!readBaseValues()) {
				return;
			}
		}

		try {
			status = getStatus();
			this.commFailed = false;
		} catch(OpenemsNamedException e) {
			status = null;
			this.commFailed = true;
		}				
	}

	public boolean isValid() {
		return this.valid;
	}

	public boolean getCommFailed() {
		return this.commFailed;
	}
	
	public String getType() {
		return this.shellyType; 
	}
	
	public int getNumRelays() {
		return this.numRelays;
	}

	public int getNumMeters() {
		return this.numMeters;
	}

	public int getNumEmeters() {
		return this.numEmeters;
	}
	
	

	/**
	 * Gets the status of the device.
	 * 
	 * <p>
	 * See https://shelly-api-docs.shelly.cloud/#shelly2-5-status
	 * 
	 * @param index the index of the relay
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
		JsonElement json = this.sendGetRequest("/relay/" + index);
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
			URL url = new URL(this.baseUrl + endpoint);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
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
						"Error while reading from Shelly API. Response code: " + status + ". " + body);
			}
		} catch (OpenemsNamedException | IOException e) {
			throw new OpenemsException(
					"Unable to read from Shelly API. " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

}
