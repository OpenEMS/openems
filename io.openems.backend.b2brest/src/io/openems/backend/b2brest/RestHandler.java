package io.openems.backend.b2brest;

import static io.openems.common.utils.JettyUtils.parseJson;
import static io.openems.common.utils.JettyUtils.sendErrorResponse;
import static io.openems.common.utils.JettyUtils.sendOkResponse;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;

import java.util.UUID;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.common.metadata.User;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcMessage;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.JettyUtils;

public class RestHandler extends Handler.Abstract {

	private final Logger log = LoggerFactory.getLogger(RestHandler.class);
	private final Backend2BackendRest parent;

	public RestHandler(Backend2BackendRest parent) {
		this.parent = parent;
	}

	@Override
	public boolean handle(Request request, Response response, Callback callback) throws Exception {
		try {

			// Determine the target from the path
			var target = request.getHttpURI().getDecodedPath();

			// Handle favicon.ico to avoid authentication issues
			if ("/favicon.ico".equals(target)) {
				response.setStatus(404);
				callback.succeeded();
				return true;
			}

			var user = this.authenticate(request);

			if (target == null || target.isEmpty() || "/".equals(target)) {
				throw new OpenemsException("Missing arguments to handle request");
			}
			// Remove leading '/' and split by '/'
			var parts = target.substring(1).split("/");
			var thisTarget = parts[0];

			if ("jsonrpc".equals(thisTarget)) {
				this.handleJsonRpc(user, request, response);
			}
			// Signal that the request has been handled.
			callback.succeeded();
			return true;

		} catch (Exception e) {
			this.log.error("Error handling request: " + e.getMessage(), e);
			callback.failed(e);
			return false;
		}
	}

	/**
	 * Authenticate a user.
	 *
	 * @param request the {@link Request}
	 * @return the {@link User}
	 * @throws OpenemsNamedException on error
	 */
	private User authenticate(Request request) throws OpenemsNamedException {
		var credentials = JettyUtils.parseCredentials(request);
		if (credentials != null) {
			try {
				return this.parent.metadata.authenticate(credentials.username(), credentials.password());
			} catch (Exception e) {
				throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
			}
		}
		throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
	}

	/**
	 * Handles an http request to 'jsonrpc' endpoint.
	 *
	 * @param user     the {@link User}
	 * @param request  the {@link Request}
	 * @param response the {@link Response}
	 */
	private void handleJsonRpc(User user, Request request, Response response) {
		var requestId = new UUID(0L, 0L); // dummy UUID in case of error
		try {
			if (!"POST".equals(request.getMethod())) {
				throw new OpenemsException(
						"Method [" + request.getMethod() + "] is not supported for JSON-RPC endpoint");
			}
			var json = parseJson(request);
			if (!json.has("jsonrpc")) {
				json.addProperty("jsonrpc", "2.0");
			}
			if (!json.has("id")) {
				json.addProperty("id", UUID.randomUUID().toString());
			}
			if (json.has("params")) {
				var params = getAsJsonObject(json, "params");
				if (params.has("payload")) {
					var payload = getAsJsonObject(params, "payload");
					if (!payload.has("jsonrpc")) {
						payload.addProperty("jsonrpc", "2.0");
					}
					if (!payload.has("id")) {
						payload.addProperty("id", UUID.randomUUID().toString());
					}
					params.add("payload", payload);
				}
				json.add("params", params);
			}
			var message = JsonrpcMessage.from(json);
			if (!(message instanceof JsonrpcRequest jsonRpcRequest)) {
				throw new OpenemsException("Only JSON-RPC Request is supported here.");
			}
			var responseFuture = this.parent.jsonRpcRequestHandler.handleRequest(this.parent.getName(), user,
					jsonRpcRequest);
			final JsonrpcResponseSuccess rpcResponse;
			try {
				rpcResponse = responseFuture.get();
			} catch (Exception e) {
				sendErrorResponse(response, jsonRpcRequest.getId(), e);
				return;
			}
			sendOkResponse(response, rpcResponse.toJsonObject());

		} catch (OpenemsNamedException e) {
			sendErrorResponse(response, requestId, new OpenemsException("Unable to get Response: " + e.getMessage()));
		}
	}
}
