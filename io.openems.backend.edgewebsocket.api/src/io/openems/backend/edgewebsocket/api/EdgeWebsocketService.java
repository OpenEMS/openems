package io.openems.backend.edgewebsocket.api;

import org.osgi.annotation.versioning.ProviderType;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;

@ProviderType
public interface EdgeWebsocketService {

	boolean isOnline(int edgeId);

	void forwardMessageFromUi(int edgeId, JsonObject jMessage) throws OpenemsException;
}
