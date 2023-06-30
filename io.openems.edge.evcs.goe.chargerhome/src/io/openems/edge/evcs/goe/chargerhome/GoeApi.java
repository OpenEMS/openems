package io.openems.edge.evcs.goe.chargerhome;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;

public class GoeApi {
	private final String ipAddress;
	private final int executeEveryCycle = 10;
	private int cycle;
	private JsonObject jsonStatus;
	private final EvcsGoeChargerHomeImpl parent;

	public GoeApi(EvcsGoeChargerHomeImpl p) {
		this.ipAddress = p.config.ip();
		this.cycle = 0;
		this.jsonStatus = null;
		this.parent = p;
	}

	/**
	 * Gets the status from go-e. See https://github.com/goecharger
	 *
	 * @return the boolean value
	 * @throws OpenemsNamedException on error
	 */
	public JsonObject getStatus() {

		try {
			// Execute every x-Cycle
			if (this.cycle == 0 || this.cycle % this.executeEveryCycle == 0) {
				var json = new JsonObject();
				var url = "http://" + this.ipAddress + "/status";
				json = this.sendRequest(url, "GET");

				this.cycle = 1;
				this.jsonStatus = json;
				return json;
			}
			this.cycle++;
			return this.jsonStatus;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Sets the activation status for go-e.
	 *
	 * <p>
	 * See https://github.com/goecharger.
	 *
	 * @param active boolean if the charger should be set to active
	 * @return JsonObject with new settings
	 */
	public JsonObject setActive(boolean active) {

		try {
			if (active == this.parent.isActive) {
				return this.jsonStatus;
			}
			var json = new JsonObject();
			Integer status = 0;
			if (active) {
				status = 1;
			}
			var url = "http://" + this.ipAddress + "/mqtt?payload=alw=" + Integer.toString(status);
			json = this.sendRequest(url, "PUT");
			this.parent.isActive = active;
			this.jsonStatus = json;
			return json;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Sets the Current in Ampere for go-e See https://github.com/goecharger.
	 *
	 * @param current current in mA
	 * @return JsonObject with new settings
	 */
	public JsonObject setCurrent(int current) {

		try {
			Integer currentAmpere = current / 1000;
			if (currentAmpere != this.parent.activeCurrent / 1000) {
				var json = new JsonObject();
				var url = "http://" + this.ipAddress + "/mqtt?payload=amp=" + Integer.toString(currentAmpere);
				json = this.sendRequest(url, "PUT");
				this.parent.activeCurrent = currentAmpere * 1000;
				this.jsonStatus = json;
				return json;
			}
			return this.jsonStatus;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Limit MaxEnergy for go-e See https://github.com/goecharger.
	 *
	 * @param limit maximum energy limit enabled
	 * @return JsonObject with new settings
	 */
	public boolean limitMaxEnergy(boolean limit) {

		try {
			var json = new JsonObject();
			var stp = 0;
			if (limit) {
				stp = 2;
			}
			var url = "http://" + this.ipAddress + "/mqtt?payload=stp=" + Integer.toString(stp);
			json = this.sendRequest(url, "PUT");
			if (json != null) {
				this.jsonStatus = json;
				return true;
			}
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Sets the MaxEnergy in 0.1 kWh for go-e See https://github.com/goecharger.
	 *
	 * @param maxEnergy maximum allowed energy
	 * @return JsonObject with new settings
	 */
	public boolean setMaxEnergy(int maxEnergy) {

		try {
			var json = new JsonObject();
			if (maxEnergy > 0) {
				this.limitMaxEnergy(true);
			} else {
				this.limitMaxEnergy(false);
			}
			var url = "http://" + this.ipAddress + "/mqtt?payload=dwo=" + Integer.toString(maxEnergy);
			json = this.sendRequest(url, "PUT");
			if (json != null) {
				this.jsonStatus = json;
				return true;
			}
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Sends a get or set request to the go-e API.
	 *
	 *
	 * @param urlString     used URL
	 * @param requestMethod requested method
	 * @return a JsonObject or JsonArray
	 */
	private JsonObject sendRequest(String urlString, String requestMethod) throws OpenemsNamedException {
		try {
			var url = new URL(urlString);
			var con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod(requestMethod);
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
			throw new OpenemsException("Error while reading from go-e API. Response code: " + status + ". " + body);
		} catch (OpenemsNamedException | IOException e) {
			throw new OpenemsException(
					"Unable to read from go-e API. " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

}
