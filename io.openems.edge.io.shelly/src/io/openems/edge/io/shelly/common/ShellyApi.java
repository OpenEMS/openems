package io.openems.edge.io.shelly.common;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.bridge.http.HttpBridge;

/**
 * Implements the local Shelly REST Api.
 *
 * <p>
 * See https://shelly-api-docs.shelly.cloud
 */
public class ShellyApi {

	private final String baseUrl;
	private final HttpBridge bridge;

	private static final Logger LOG = LoggerFactory.getLogger(ShellyApi.class);
	
	public ShellyApi(String ip, HttpBridge bridge) {
		this.bridge = bridge;
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
	public JsonObject getState() throws OpenemsNamedException {
		var req = this.sendGetRequest("/status");
		if (req.isPresent()) {
			return JsonUtils.getAsJsonObject(req.get());
		}
		return new JsonObject();
	}

	/**
	 * Gets the "isOn" property of the relay with the given index.
	 *
	 * <p>
	 * See https://shelly-api-docs.shelly.cloud/#shelly2-5-relay-index
	 *
	 * @param index the index of the relay
	 * @return the boolean value
	 * @throws OpenemsNamedException on error
	 */
	public boolean isRelayOn(int index) throws OpenemsNamedException {
		var json = this.sendGetRequest("/relay/" + index);
		if (json.isPresent()) {
			return JsonUtils.getAsBoolean(json.get(), "ison");
		} else {
			return false;
		}
	}

	/**
	 * Turns the relay with the given index on or off.
	 *
	 * @param index the index of the relay
	 * @param value true to turn on; false to turn off
	 * @throws OpenemsNamedException on error
	 */
	public void setRelayState(int index, boolean value) throws OpenemsNamedException {
		this.sendGetRequest("/relay/" + index + "?turn=" + (value ? "on" : "off"));
	}

	/**
	 * Sends a get request to the Shelly API.
	 *
	 * @param endpoint the REST Api endpoint
	 * @return an optional JsonObject or JsonArray. No value present if the response could not be read or the request could not be made.
	 */
	private Optional<JsonElement> sendGetRequest(String endpoint) {
		try {
			var response = bridge.get(new URL(this.baseUrl + endpoint)) //
					.convert(JsonObject.class);
				return Optional.ofNullable(response);
		} catch (MalformedURLException e1) {
			LOG.error("Malformed URL for Shelly Plug: " + this.baseUrl + endpoint);
			return Optional.empty();
		}
	}

}
