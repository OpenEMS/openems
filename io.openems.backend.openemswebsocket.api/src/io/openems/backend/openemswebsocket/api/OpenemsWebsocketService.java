package io.openems.backend.openemswebsocket.api;

import java.util.Optional;

import org.osgi.annotation.versioning.ProviderType;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;

@ProviderType
public interface OpenemsWebsocketService {

	boolean isWebsocketConnected(String name);

	Optional<JsonObject> getConfig(String deviceName);

	void send(String deviceName, JsonObject j) throws OpenemsException;
}
