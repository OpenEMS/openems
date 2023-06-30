package io.openems.common.websocket;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;
import io.openems.common.jsonrpc.base.JsonrpcResponseError;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.JsonrpcUtils;
import io.openems.common.utils.StringUtils;

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
		CompletableFuture<? extends JsonrpcResponseSuccess> responseFuture;
		try {
			responseFuture = this.parent.getOnRequest().run(this.ws, this.request);
		} catch (Throwable t) {
			this.handleException(t);
			return;
		}

		if (this.request.getTimeout().isPresent() && this.request.getTimeout().get() > 0) {
			// Apply timeout to CompleteableFuture
			responseFuture.orTimeout(this.request.getTimeout().get(), TimeUnit.SECONDS);
		}

		// ...without timeout
		responseFuture.whenComplete((r, ex) -> {
			if (ex != null) {
				this.handleException(ex);
			} else if (r != null) {
				this.handleResponse(r);
			} else {
				this.handleException(
						new OpenemsNamedException(OpenemsError.JSONRPC_UNHANDLED_METHOD, this.request.getMethod()));
			}
		});
	}

	private void handleResponse(JsonrpcResponse response) {
		this.responseCallback.accept(response);
	}

	private void handleException(Throwable t) {
		// Log Error
		var log = new StringBuilder() //
				.append("JSON-RPC Error "); //

		if (t.getMessage() != null && !t.getMessage().isBlank()) {
			log //
					.append("\"") //
					.append(t.getMessage()) //
					.append("\" ");
		}

		if (!(t instanceof OpenemsNamedException)) {
			log //
					.append("of type ") //
					.append(t.getClass().getSimpleName()) //
					.append(" ");

			if (t instanceof TimeoutException) {
				log //
						.append("[").append(this.request.getTimeout().get()).append("s] ");
			}
		}

		log //
				.append("for Request ") //
				.append(StringUtils.toShortString(JsonrpcUtils.simplifyJsonrpcMessage(this.request), 100)); //
		this.parent.logWarn(this.log, log.toString());

		// Get JSON-RPC Response Error
		if (t instanceof OpenemsNamedException) {
			this.responseCallback.accept(new JsonrpcResponseError(this.request.getId(), (OpenemsNamedException) t));
		} else {
			this.responseCallback.accept(new JsonrpcResponseError(this.request.getId(), t.getMessage()));
		}
	}

	// TODO REMOVE DEBUG
	public String getRequestMethod() {
		return this.request.getFullyQualifiedMethod();
	}
}
