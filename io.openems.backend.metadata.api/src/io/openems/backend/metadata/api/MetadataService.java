package io.openems.backend.metadata.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.OpenemsException;

@ProviderType
public interface MetadataService {

	public void getInfoWithSession() throws OpenemsException;

}
