package io.openems.backend.metadata.api;

import io.openems.common.exceptions.OpenemsException;

public interface OLD_MetadataDeviceModel {
	/**
	 * Gets the devices for this apikey.
	 *
	 * @param apikey
	 * @return device or null
	 * @throws OpenemsException
	 */
	public OLD_MetadataDevices getDevicesForApikey(String apikey) throws OpenemsException;
}
