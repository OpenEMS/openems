package io.openems.edge.common.jsonapi;

import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;

public interface JsonApi {

	public JsonrpcResponse handleJsonrpcRequest(JsonrpcRequest message);

}
