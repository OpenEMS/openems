package io.openems.backend.metadata.api.device;

import java.util.Optional;

import io.openems.common.exceptions.OpenemsException;

public interface MetadataDeviceModel {
	/**
	 * Gets the device for this apikey.
	 *
	 * Note: if there is more than one matching device it returns the first match.
	 *
	 * @param apikey
	 * @return device or null
	 * @throws OpenemsException
	 */
	public Optional<MetadataDevice> getDeviceForApikey(String apikey) throws OpenemsException;
}
