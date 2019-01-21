package io.openems.common.jsonrpc.base;

public abstract class JsonrpcNotification extends AbstractJsonrpcRequest {

	public JsonrpcNotification(String method) {
		super(method);
	}

}
