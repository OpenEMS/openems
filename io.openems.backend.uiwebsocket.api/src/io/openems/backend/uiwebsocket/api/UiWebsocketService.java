package io.openems.backend.uiwebsocket.api;

import org.osgi.annotation.versioning.ProviderType;

import com.google.gson.JsonObject;

@ProviderType
public interface UiWebsocketService {

	public abstract void handleEdgeReply(int edgeId, JsonObject jMessage);

}
