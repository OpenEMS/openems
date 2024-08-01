package io.openems.common.websocket;

import static io.openems.common.exceptions.OpenemsError.JSONRPC_UNHANDLED_METHOD;
import static io.openems.common.utils.JsonrpcUtils.simplifyJsonrpcMessage;
import static io.openems.common.utils.StringUtils.toShortString;
import static io.openems.common.websocket.WebsocketUtils.generateWsDataString;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcMessage;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;
import io.openems.common.jsonrpc.base.JsonrpcResponseError;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;

/**
 * Handler for WebSocket OnMessage event.
 */
public final class OnMessageHandler implements Runnable {

	private final Logger log = LoggerFactory.getLogger(OnMessageHandler.class);
	private final WebSocket ws;
	private final String message;
	private final OnRequest onRequest;
	private final OnNotification onNotification;
	private final BiPredicate<WebSocket, JsonrpcMessage> sendMessage;
	private final BiConsumer<Throwable, String> handleInternalError;
	private final BiConsumer<Logger, String> logWarn;

	public OnMessageHandler(//
			WebSocket ws, String message, //
			OnRequest onRequest, //
			OnNotification onNotification, //
			BiPredicate<WebSocket, JsonrpcMessage> sendMessage, //
			BiConsumer<Throwable, String> handleInternalError, //
			BiConsumer<Logger, String> logWarn) {
		this.ws = ws;
		this.message = message;
		this.onRequest = onRequest;
		this.onNotification = onNotification;
		this.sendMessage = sendMessage;
		this.handleInternalError = handleInternalError;
		this.logWarn = logWarn;
	}

	@Override
	public final void run() {
		try {
			var message = JsonrpcMessage.from(this.message);

			if (message instanceof JsonrpcRequest request) {
				this.handleJsonrpcRequest(this.ws, request);

			} else if (message instanceof JsonrpcResponse response) {
				this.handleJsonrpcResponse(this.ws, response);

			} else if (message instanceof JsonrpcNotification notification) {
				this.handleJsonrpcNotification(this.ws, notification);
			}

		} catch (OpenemsNamedException e) {
			this.handleInternalError.accept(e, generateWsDataString(this.ws));
			return;
		}
	}

	/**
	 * Handle a {@link JsonrpcRequest}.
	 * 
	 * @param ws      the {@link WebSocket}
	 * @param request the {@link JsonrpcRequest}
	 */
	protected void handleJsonrpcRequest(WebSocket ws, JsonrpcRequest request) {
		CompletableFuture<? extends JsonrpcResponseSuccess> responseFuture;
		try {
			responseFuture = this.onRequest.apply(ws, request);
		} catch (Throwable t) {
			this.handleJsonrpcRequestException(ws, request, t);
			return;
		}

		var timeout = request.getTimeout();
		if (timeout.isPresent() && timeout.get() > 0) {
			// Apply timeout to CompleteableFuture
			responseFuture.orTimeout(timeout.get(), SECONDS);
		}

		// ...without timeout
		responseFuture.whenComplete((r, ex) -> {
			if (ex != null) {
				this.handleJsonrpcRequestException(ws, request, ex);
			} else if (r != null) {
				this.handleJsonrpcRequestResponse(ws, r);
			} else {
				this.handleJsonrpcRequestException(ws, request,
						new OpenemsNamedException(JSONRPC_UNHANDLED_METHOD, request.getMethod()));
			}
		});
	}

	private void handleJsonrpcRequestResponse(WebSocket ws, JsonrpcResponse response) {
		this.sendMessage.test(ws, response);
	}

	private void handleJsonrpcRequestException(WebSocket ws, JsonrpcRequest request, Throwable t) {
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
						.append("[").append(request.getTimeout().orElse(0)).append("s] ");
			}
		}

		log //
				.append("for Request ") //
				.append(toShortString(simplifyJsonrpcMessage(request), 200));
		this.logWarn.accept(this.log, log.toString());

		// Get JSON-RPC Response Error
		if (t instanceof OpenemsNamedException one) {
			this.sendMessage.test(ws, new JsonrpcResponseError(request.getId(), one));
		} else {
			this.sendMessage.test(ws, new JsonrpcResponseError(request.getId(), t.getMessage()));
		}
	}

	/**
	 * Handle a {@link JsonrpcResponse}.
	 * 
	 * @param ws       the {@link WebSocket}
	 * @param response the {@link JsonrpcResponse}
	 */
	protected void handleJsonrpcResponse(WebSocket ws, JsonrpcResponse response) {
		try {
			WsData wsData = this.ws.getAttachment();
			wsData.handleJsonrpcResponse(response);

		} catch (Throwable t) {
			this.handleInternalError.accept(t, generateWsDataString(this.ws));
		}
	}

	/**
	 * Handle a {@link JsonrpcNotification}.
	 * 
	 * @param ws           the {@link WebSocket}
	 * @param notification the {@link JsonrpcNotification}
	 */
	protected void handleJsonrpcNotification(WebSocket ws, JsonrpcNotification notification) {
		try {
			this.onNotification.accept(ws, notification);

		} catch (Throwable t) {
			this.handleInternalError.accept(t, generateWsDataString(this.ws));
		}
	}
}
