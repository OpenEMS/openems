package io.openems.backend.b2brest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.backend.common.metadata.User;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcMessage;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseError;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.JsonUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RestHandler extends AbstractHandler {

	private final Logger log = LoggerFactory.getLogger(RestHandler.class);

	private final Backend2BackendRest parent;

	public RestHandler(Backend2BackendRest parent) {
		this.parent = parent;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		try {
			var user = this.authenticate(request);

			List<String> targets = Arrays.asList(//
					target.substring(1) // remove leading '/'
							.split("/"));

			if (targets.isEmpty()) {
				throw new OpenemsException("Missing arguments to handle request");
			}

			var thisTarget = targets.get(0);
			switch (thisTarget) {
			case "jsonrpc":
				this.handleJsonRpc(user, baseRequest, request, response);
				break;
			}
		} catch (OpenemsNamedException e) {
			throw new IOException(e.getMessage());
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
			var st = new StringTokenizer(authHeader);
			if (st.hasMoreTokens()) {
				var basic = st.nextToken();
				if (basic.equalsIgnoreCase("Basic")) {
					String credentials;
					try {
						credentials = new String(Base64.getDecoder().decode(st.nextToken()), "UTF-8");
					} catch (UnsupportedEncodingException e) {
						throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
					}
					var p = credentials.indexOf(":");
					if (p != -1) {
						var username = credentials.substring(0, p).trim();
						var password = credentials.substring(p + 1).trim();
						// authenticate using username & password
						return this.parent.metadata.authenticate(username, password);
					}
				}
			}
		}
		throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
	}

	private void sendOkResponse(Request baseRequest, HttpServletResponse response, JsonObject data)
			throws OpenemsException {
		try {
			response.setContentType("application/json");
			response.setStatus(HttpServletResponse.SC_OK);
			baseRequest.setHandled(true);
			response.getWriter().write(data.toString());
		} catch (IOException e) {
			throw new OpenemsException("Unable to send Ok-Response: " + e.getMessage());
		}
	}

	private void sendErrorResponse(Request baseRequest, HttpServletResponse response, UUID jsonrpcId, Throwable ex) {
		try {
			response.setContentType("application/json");
			// Choosing SC_OK here instead of SC_BAD_REQUEST, because otherwise no error
			// message is sent to client
			response.setStatus(HttpServletResponse.SC_OK);
			baseRequest.setHandled(true);
			JsonrpcResponseError message;
			if (ex instanceof OpenemsNamedException) {
				// Get Named Exception error response
				message = new JsonrpcResponseError(jsonrpcId, (OpenemsNamedException) ex);
			} else if (ex.getCause() != null && ex.getCause() instanceof OpenemsNamedException) {
				message = new JsonrpcResponseError(jsonrpcId, (OpenemsNamedException) ex.getCause());
			} else {
				// Get GENERIC error response
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
	 * @param baseRequest the Request
	 * @return the {@link JsonObject}
	 * @throws OpenemsException on error
	 */
	private static JsonObject parseJson(Request baseRequest) throws OpenemsException {
		try {
			return JsonParser.parseString(//
					new BufferedReader(new InputStreamReader(baseRequest.getInputStream())) //
							.lines() //
							.collect(Collectors.joining("\n"))) //
					.getAsJsonObject();
		} catch (Exception e) {
			throw new OpenemsException("Unable to parse: " + e.getMessage());
		}
	}

	/**
	 * Handles an http request to 'jsonrpc' endpoint.
	 *
	 * @param user         the {@link User}
	 * @param baseRequest  the {@link Request}
	 * @param httpRequest  the {@link HttpServletRequest}
	 * @param httpResponse the {@link HttpServletResponse}
	 */
	private void handleJsonRpc(User user, Request baseRequest, HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {
		var requestId = new UUID(0L, 0L); /* dummy UUID */
		try {
			// call handler methods
			if (!httpRequest.getMethod().equals("POST")) {
				throw new OpenemsException(
						"Method [" + httpRequest.getMethod() + "] is not supported for JSON-RPC endpoint");
			}

			// parse json and add "jsonrpc" and "id" properties if missing
			var json = RestHandler.parseJson(baseRequest);
			if (!json.has("jsonrpc")) {
				json.addProperty("jsonrpc", "2.0");
			}
			if (!json.has("id")) {
				json.addProperty("id", UUID.randomUUID().toString());
			}
			if (json.has("params")) {
				var params = JsonUtils.getAsJsonObject(json, "params");
				if (params.has("payload")) {
					var payload = JsonUtils.getAsJsonObject(params, "payload");
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
			// parse JSON-RPC Request
			var message = JsonrpcMessage.from(json);
			if (!(message instanceof JsonrpcRequest)) {
				throw new OpenemsException("Only JSON-RPC Request is supported here.");
			}
			var request = (JsonrpcRequest) message;

			// handle the request
			CompletableFuture<? extends JsonrpcResponseSuccess> responseFuture = this.parent.jsonRpcRequestHandler
					.handleRequest(this.parent.getName(), user, request);

			// wait for response
			JsonrpcResponseSuccess response;
			try {
				response = responseFuture.get();
			} catch (InterruptedException | ExecutionException e) {
				this.sendErrorResponse(baseRequest, httpResponse, request.getId(), e);
				return;
			}

			// send response
			this.sendOkResponse(baseRequest, httpResponse, response.toJsonObject());

		} catch (OpenemsNamedException e) {
			this.sendErrorResponse(baseRequest, httpResponse, requestId,
					new OpenemsException("Unable to get Response: " + e.getMessage()));
		}
	}

}
