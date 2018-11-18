package io.openems.backend.edgewebsocket.api;

import java.util.function.Consumer;

import org.osgi.annotation.versioning.ProviderType;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;

@ProviderType
public interface EdgeWebsocket {

	boolean isOnline(String edgeId);

	@Deprecated
	void forwardMessageFromUi(int edgeId, JsonObject jMessage) throws OpenemsException;

	public void send(String edgeId, JsonrpcRequest request, Consumer<JsonrpcResponse> responseCallback);
}
