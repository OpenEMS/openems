package io.openems.edge.bridge.onewire.jsonrpc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.container.TemperatureContainer;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.JsonUtils;

/**
 * Wraps a JSON-RPC Response to "getDevices" Request.
 *
 * <p>
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "devices": [{
 *       "address": string,
 *       "name": string,
 *       "alternateName": string,
 *       "description": string,
 *       "details": device-specific object
 *     }]
 *   }
 * }
 * </pre>
 */
public class GetDeviceResponse extends JsonrpcResponseSuccess {

	public static class Device {
		final String address;
		final String name;
		final String alternateName;
		final String description;
		final JsonObject details;

		/**
		 * Extracts {@link Device} info from a {@link OneWireContainer}.
		 * 
		 * @param owc the {@link OneWireContainer}
		 * @return the {@link Device}
		 */
		public static Device from(OneWireContainer owc) {
			final var details = new JsonObject();

			if (owc instanceof TemperatureContainer) {
				details.addProperty("type", "TemperatureContainer");
				try {
					var tc = (TemperatureContainer) owc;
					var state = tc.readDevice();
					tc.doTemperatureConvert(state);
					state = tc.readDevice();
					var temp = tc.getTemperature(state);
					details.addProperty("temperature", temp);
				} catch (OneWireException e) {
					e.printStackTrace();
					details.addProperty("error", e.getMessage());
				}
			}

			return new Device(owc.getAddressAsString(), owc.getName(), owc.getAlternateNames(), owc.getDescription(),
					details);
		}

		private Device(String address, String name, String alternateName, String description, JsonObject details) {
			this.address = address;
			this.name = name;
			this.alternateName = alternateName;
			this.description = description;
			this.details = details;
		}
	}

	private final List<Device> devices = new ArrayList<>();

	public GetDeviceResponse(UUID id) {
		super(id);
	}

	/**
	 * Add a {@link Device}.
	 * 
	 * @param device the {@link Device}
	 */
	public void addDevice(Device device) {
		this.devices.add(device);
	}

	@Override
	public JsonObject getResult() {
		var devices = new JsonArray();
		for (Device device : this.devices) {
			devices.add(JsonUtils.buildJsonObject() //
					.addProperty("address", device.address) //
					.addProperty("name", device.name) //
					.addProperty("alternateName", device.alternateName) //
					.addProperty("description", device.description) //
					.add("details", device.details) //
					.build());
		}
		var j = new JsonObject();
		j.add("devices", devices);
		return j;
	}

}
