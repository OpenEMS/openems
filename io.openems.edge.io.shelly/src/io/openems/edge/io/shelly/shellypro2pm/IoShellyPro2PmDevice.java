package io.openems.edge.io.shelly.shellypro2pm;

import java.util.function.Consumer;

import com.google.gson.JsonObject;

import io.openems.common.types.Result;
import io.openems.edge.common.type.Phase;
import io.openems.edge.io.shelly.common.HttpBridgeShellyService;
import io.openems.edge.io.shelly.common.gen2.IoGen2ShellyBase;

public interface IoShellyPro2PmDevice extends IoGen2ShellyBase {

	/**
	 * Add callback that is executed when new status data arrives. Important! Remove
	 * the callback with removeStatusCallback() after dispose of your class.
	 * 
	 * @param callback Function to execute
	 */
	public void addStatusCallback(Consumer<Result<JsonObject>> callback);

	/**
	 * Remove a registered callback that is executed when new status data arrives.
	 *
	 * @param callback Function to execute
	 */
	public void removeStatusCallback(Consumer<Result<JsonObject>> callback);

	/**
	 * Returns the http shelly service used by this device.
	 * 
	 * @return Reference to {@link HttpBridgeShellyService}
	 */
	public HttpBridgeShellyService getShellyService();

	/**
	 * Gets the power phase this device is attached to.
	 * 
	 * @return Phase type
	 */
	public Phase.SinglePhase getPhase();

}
