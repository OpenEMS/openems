package io.openems.common.websocket;

import java.util.function.Consumer;

import org.java_websocket.WebSocket;

import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;
import io.openems.common.jsonrpc.base.JsonrpcResponseError;

public class OnRequestHandler implements Runnable {

	private final AbstractWebsocket<?> parent;
	private final WebSocket ws;
	private final JsonrpcRequest request;
	private final Consumer<JsonrpcResponse> responseCallback;

	public OnRequestHandler(AbstractWebsocket<?> parent, WebSocket ws, JsonrpcRequest request,
			Consumer<JsonrpcResponse> responseCallback) {
		this.parent = parent;
		this.ws = ws;
		this.request = request;
		this.responseCallback = responseCallback;
	}

	@Override
	public final void run() {
		try {
			this.parent.getOnRequest().run(this.ws, this.request, this.responseCallback);
		} catch (Exception e) {
			// TODO catch Exception with explicit Error-enum
			responseCallback.accept(new JsonrpcResponseError(request.getId(), 0, e.getMessage()));
			this.parent.handleInternalErrorSync(e);
		}
	}

}
