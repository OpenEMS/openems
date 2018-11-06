package io.openems.edge.common.jsonapi;

import io.openems.common.websocket.JsonrpcRequest;
import io.openems.common.websocket.JsonrpcResponse;

public interface JsonApi {

	public JsonrpcResponse handleJsonrpcRequest(JsonrpcRequest message);

}
