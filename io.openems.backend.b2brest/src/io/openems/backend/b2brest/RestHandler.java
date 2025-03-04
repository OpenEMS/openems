package io.openems.backend.b2brest;

import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.common.utils.JsonUtils.parseToJsonObject;
import static java.util.stream.Collectors.joining;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.backend.common.metadata.User;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcMessage;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseError;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RestHandler extends Handler.Abstract {

	private final Logger log = LoggerFactory.getLogger(RestHandler.class);
	private final Backend2BackendRest parent;

	public RestHandler(Backend2BackendRest parent) {
		this.parent = parent;
	}

	@Override
	public boolean handle(Request baseRequest, Response response, Callback callback) throws Exception {
		HttpServletRequest httpRequest = (HttpServletRequest) baseRequest;
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		try {
			var user = this.authenticate(httpRequest);

			// Determine the target from the path info.
			var target = httpRequest.getPathInfo();
			if (target == null || target.isEmpty() || "/".equals(target)) {
				throw new OpenemsException("Missing arguments to handle request");
			}
			// Remove leading '/' and split by '/'
			var parts = target.substring(1).split("/");
			var thisTarget = parts[0];

			if ("jsonrpc".equals(thisTarget)) {
				this.handleJsonRpc(user, httpRequest, httpResponse);
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
	 * @param request the HttpServletRequest
	 * @return the {@link User}
	 * @throws OpenemsNamedException on error
	 */
	private User authenticate(HttpServletRequest request) throws OpenemsNamedException {
		var authHeader = request.getHeader("Authorization");
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

	private void sendOkResponse(HttpServletResponse response, JsonObject data) throws OpenemsException {
		try {
			response.setContentType("application/json");
			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().write(data.toString());

		} catch (IOException e) {
			throw new OpenemsException("Unable to send Ok-Response: " + e.getMessage());
		}
	}

	private void sendErrorResponse(HttpServletResponse response, UUID jsonrpcId, Throwable ex) {
		try {
			response.setContentType("application/json");
			// Using SC_OK so the error message is delivered in the response body.
			response.setStatus(HttpServletResponse.SC_OK);
			final JsonrpcResponseError message;
			if (ex instanceof OpenemsNamedException) {
				message = new JsonrpcResponseError(jsonrpcId, (OpenemsNamedException) ex);
			} else if (ex.getCause() instanceof OpenemsNamedException) {
				message = new JsonrpcResponseError(jsonrpcId, (OpenemsNamedException) ex.getCause());
			} else {
				message = new JsonrpcResponseError(jsonrpcId, ex.getMessage());
			}
			response.getWriter().write(message.toString());

		} catch (IOException e) {
			this.parent.logWarn(this.log, "Unable to send Error-Response: " + e.getMessage());
		}
	}

	/**
	 * Parses a Request to JSON.
	 *
	 * @param request the Request
	 * @return the {@link JsonObject}
	 * @throws OpenemsException on error
	 */
	private static JsonObject parseJson(HttpServletRequest request) throws OpenemsException {
		try (BufferedReader br = request.getReader()) {
			var jsonStr = br.lines().collect(joining("\n"));
			return parseToJsonObject(jsonStr);

		} catch (Exception e) {
			throw new OpenemsException("Unable to parse: " + e.getMessage());
		}
	}

	/**
	 * Handles an http request to 'jsonrpc' endpoint.
	 *
	 * @param user         the {@link User}
	 * @param httpRequest  the {@link HttpServletRequest}
	 * @param httpResponse the {@link HttpServletResponse}
	 */
	private void handleJsonRpc(User user, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		var requestId = new UUID(0L, 0L); // dummy UUID in case of error
		try {
			if (!"POST".equals(httpRequest.getMethod())) {
				throw new OpenemsException(
						"Method [" + httpRequest.getMethod() + "] is not supported for JSON-RPC endpoint");
			}
			var json = parseJson(httpRequest);
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
			if (!(message instanceof JsonrpcRequest request)) {
				throw new OpenemsException("Only JSON-RPC Request is supported here.");
			}
			var responseFuture = this.parent.jsonRpcRequestHandler.handleRequest(this.parent.getName(), user, request);
			final JsonrpcResponseSuccess rpcResponse;
			try {
				rpcResponse = responseFuture.get();
			} catch (Exception e) {
				this.sendErrorResponse(httpResponse, request.getId(), e);
				return;
			}
			this.sendOkResponse(httpResponse, rpcResponse.toJsonObject());

		} catch (OpenemsNamedException e) {
			this.sendErrorResponse(httpResponse, requestId,
					new OpenemsException("Unable to get Response: " + e.getMessage()));
		}
	}
}
