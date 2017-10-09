package io.openems.backend.metadata.api.device;

import io.openems.common.exceptions.OpenemsException;

public interface MetadataDeviceModel {
	/**
	 * Gets the devices for this apikey.
	 *
	 * @param apikey
	 * @return device or null
	 * @throws OpenemsException
	 */
	public MetadataDevices getDevicesForApikey(String apikey) throws OpenemsException;
}
