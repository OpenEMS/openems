package io.openems.backend.metadata.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Session;

@ProviderType
public interface MetadataService {

	public UserDevicesInfo getInfoWithSession(String sessionId) throws OpenemsException;

}
