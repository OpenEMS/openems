package io.openems.common.websocket;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcMessage;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;
import io.openems.common.jsonrpc.base.JsonrpcResponseError;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.JsonUtils;

public class OnRequestHandler implements Runnable {

	private final Logger log = LoggerFactory.getLogger(OnRequestHandler.class);

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
		JsonrpcResponse response;
		try {
			CompletableFuture<? extends JsonrpcResponseSuccess> responseFuture = this.parent.getOnRequest().run(this.ws,
					this.request);
			// Get success response
			if (this.request.getTimeout().isPresent() && this.request.getTimeout().get() > 0) {
				// ...with timeout
				response = responseFuture.get(this.request.getTimeout().get(), TimeUnit.SECONDS);
			} else {
				// ...without timeout
				response = responseFuture.get();
			}

		} catch (Exception e) {
			// Log Error
			var log = new StringBuilder() //
					.append("JSON-RPC Error ") //
					.append("Response \"").append(e.getMessage()).append("\" ");
			if (!(e instanceof OpenemsNamedException)) {
				log.append("of type ").append(e.getClass().getCanonicalName()).append("] ");
			}
			log.append("for Request ").append(simplifyJsonrpcMessage(this.request.toJsonObject()).toString()); //
			this.parent.logWarn(this.log, log.toString());

			// Get JSON-RPC Response Error
			if (e instanceof OpenemsNamedException) {
				response = new JsonrpcResponseError(this.request.getId(), (OpenemsNamedException) e);
			} else {
				response = new JsonrpcResponseError(this.request.getId(), e.getMessage());
			}
		}

		this.responseCallback.accept(response);
	}

	/**
	 * Simplifies a {@link JsonrpcMessage} by recursively removing unnecessary
	 * elements "jsonrpc" and "id".
	 *
	 * @param j the {@link JsonrpcMessage#toJsonObject()}
	 * @return a simplified {@link JsonObject}
	 */
	protected static JsonObject simplifyJsonrpcMessage(JsonObject j) {
		if (j.has("jsonrpc")) {
			j.remove("jsonrpc");
			j.remove("id");
		}
		if (j.has("params")) {
			try {
				var params = JsonUtils.getAsJsonObject(j, "params");
				if (params.has("payload")) {
					var originalPayload = JsonUtils.getAsJsonObject(params, "payload");
					var simplifiedPayload = simplifyJsonrpcMessage(originalPayload);
					params.add("payload", simplifiedPayload);
					j.add("params", params);
				}
			} catch (OpenemsNamedException e) {
				// ignore -> do not replace params/payload
			}
		}
		return j;
	}

}
