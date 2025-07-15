package io.openems.backend.b2brest;

import static io.openems.common.utils.JettyUtils.parseJson;
import static io.openems.common.utils.JettyUtils.sendErrorResponse;
import static io.openems.common.utils.JettyUtils.sendOkResponse;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
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

public class RestHandler extends Handler.Abstract {

	private final Logger log = LoggerFactory.getLogger(RestHandler.class);
	private final Backend2BackendRest parent;

	public RestHandler(Backend2BackendRest parent) {
		this.parent = parent;
	}

	@Override
	public boolean handle(Request request, Response response, Callback callback) throws Exception {
		try {
			var user = this.authenticate(request);

			// Determine the target from the path info.
			var target = request.getHttpURI().getDecodedPath();
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
		var authHeader = request.getHeaders().get("Authorization");
		if (authHeader != null) {
			var parts = authHeader.split(" ");
			if (parts.length >= 2 && "Basic".equalsIgnoreCase(parts[0])) {
				try {
					var credentials = new String(Base64.getDecoder().decode(parts[1]), StandardCharsets.UTF_8);
					int p = credentials.indexOf(":");
					if (p != -1) {
						var username = credentials.substring(0, p).trim();
						var password = credentials.substring(p + 1).trim();
						return this.parent.metadata.authenticate(username, password);
					}
				} catch (Exception e) {
					throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
				}
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
			if (!(message instanceof JsonrpcRequest requestMessage)) {
				throw new OpenemsException("Only JSON-RPC Request is supported here.");
			}
			var responseFuture = this.parent.jsonRpcRequestHandler.handleRequest(this.parent.getName(), user,
					requestMessage);
			final JsonrpcResponseSuccess rpcResponse;
			try {
				rpcResponse = responseFuture.get();
			} catch (Exception e) {
				sendErrorResponse(response, requestMessage.getId(), e);
				return;
			}
			sendOkResponse(response, rpcResponse.toJsonObject());

		} catch (OpenemsNamedException e) {
			sendErrorResponse(response, requestId, new OpenemsException("Unable to get Response: " + e.getMessage()));
		}
	}
}
