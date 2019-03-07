package io.openems.edge.controller.api.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.common.OpenemsConstants;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcMessage;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.ComponentJsonApiRequest;
import io.openems.common.jsonrpc.request.GetEdgeConfigRequest;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.session.Role;
import io.openems.common.session.User;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.user.EdgeUser;
import io.openems.edge.controller.api.core.WritePojo;

public class RestHandler extends AbstractHandler {

	private final Logger log = LoggerFactory.getLogger(RestHandler.class);

	private final RestApi parent;

	public RestHandler(RestApi parent) {
		this.parent = parent;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		try {
			User user = this.authenticate(request);

			List<String> targets = Arrays.asList(//
					target.substring(1) // remove leading '/'
							.split("/"));

			if (targets.isEmpty()) {
				throw new OpenemsException("Missing arguments to handle request");
			}

			String thisTarget = targets.get(0);
			List<String> remainingTargets = targets.subList(1, targets.size());

			switch (thisTarget) {
			case "rest":
				this.handleRest(user, remainingTargets, baseRequest, request, response);
				break;

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
	 * @return the User
	 * @throws OpenemsNamedException on error
	 */
	private User authenticate(HttpServletRequest request) throws OpenemsNamedException {
		String authHeader = request.getHeader("Authorization");
		if (authHeader != null) {
			StringTokenizer st = new StringTokenizer(authHeader);
			if (st.hasMoreTokens()) {
				String basic = st.nextToken();
				if (basic.equalsIgnoreCase("Basic")) {
					String credentials;
					try {
						credentials = new String(Base64.getDecoder().decode(st.nextToken()), "UTF-8");
					} catch (UnsupportedEncodingException e) {
						throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
					}
					int p = credentials.indexOf(":");
					if (p != -1) {
						String username = credentials.substring(0, p).trim();
						String password = credentials.substring(p + 1).trim();
						// authenticate using username & password
						Optional<EdgeUser> userOpt = this.parent.userService.authenticate(username, password);
						if (userOpt.isPresent()) {
							return userOpt.get();
						}
						// authenticate using password only
						userOpt = this.parent.userService.authenticate(password);
						if (userOpt.isPresent()) {
							return userOpt.get();
						}
					}
				}
			}
		}
		throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
	}

	private void handleRest(User user, List<String> targets, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws IOException, OpenemsNamedException {
		if (targets.isEmpty()) {
			throw new OpenemsException("Missing arguments to handle REST-request");
		}

		String thisTarget = targets.get(0);
		List<String> remainingTargets = targets.subList(1, targets.size());

		switch (thisTarget) {
		case "channel":
			this.handleChannel(user, remainingTargets, baseRequest, request, response);
			break;
		}
	}

	private void handleChannel(User user, List<String> targets, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws IOException, OpenemsNamedException {
		if (targets.size() != 2) {
			throw new OpenemsException("Missing arguments to handle Channel");
		}

		// get request attributes
		ChannelAddress channelAddress = new ChannelAddress(targets.get(0), targets.get(1));

		// get channel
		Channel<?> channel;
		try {
			channel = this.parent.componentManager.getChannel(channelAddress);
		} catch (IllegalArgumentException e) {
			this.parent.logWarn(this.log, e.getMessage());
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		// call handler methods
		switch (request.getMethod()) {
		case "GET":
			this.handleGet(user, channel, baseRequest, request, response);
			break;
		case "POST":
			this.handlePost(user, channel, baseRequest, request, response);
			break;
		}
	}

	/**
	 * Handles HTTP GET request.
	 * 
	 * @param user        the User
	 * @param channel     the affected channel
	 * @param baseRequest the HTTP POST base-request
	 * @param request     the HTTP POST request
	 * @param response    the result to be returned
	 * @throws OpenemsNamedException
	 */
	private void handleGet(User user, Channel<?> channel, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws OpenemsNamedException {
		user.assertRoleIsAtLeast("HTTP GET", Role.GUEST);

		JsonObject j = new JsonObject();
		j.add("value", channel.value().asJson());
		// type
		Optional<OpenemsType> typeOpt = channel.channelDoc().getType();
		String type;
		if (typeOpt.isPresent()) {
			type = typeOpt.get().toString().toLowerCase();
		} else {
			type = "UNDEFINED";
		}
		j.addProperty("type", type);
		// writable
		j.addProperty("writable", //
				channel instanceof WriteChannel<?> ? true : false //
		);

		this.sendOkResponse(baseRequest, response, j);
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

	/**
	 * Handles HTTP POST request.
	 *
	 * @param user        the User
	 * @param channel     the affected channel
	 * @param baseRequest the HTTP POST base-request
	 * @param request     the HTTP POST request
	 * @param response    the result to be returned
	 * @throws OpenemsNamedException
	 */
	private void handlePost(User user, Channel<?> channel, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws OpenemsNamedException {
		user.assertRoleIsAtLeast("HTTP POST", Role.ADMIN);

		// check for writable channel
		if (!(channel instanceof WriteChannel<?>)) {
			throw new OpenemsException("[" + channel + "] is not a Write Channel");
		}

		// parse json
		JsonObject jHttpPost = RestHandler.parseJson(baseRequest);

		// parse value
		JsonElement jValue;
		if (jHttpPost.has("value")) {
			jValue = jHttpPost.get("value");
		} else {
			throw new OpenemsException("Value is missing");
		}

		// set channel value
		Object value;
		if (jValue.isJsonNull()) {
			value = null;
		} else {
			value = jValue.toString();
		}
		this.parent.apiWorker.addValue((WriteChannel<?>) channel, new WritePojo(value));
		log.info("Updated Channel [" + channel.address() + "] to value [" + jValue.toString() + "].");

		this.sendOkResponse(baseRequest, response, new JsonObject());
	}

	/**
	 * Parses a Request to JSON.
	 * 
	 * @param baseRequest the Request
	 * @return
	 * @throws OpenemsException on error
	 */
	private static JsonObject parseJson(Request baseRequest) throws OpenemsException {
		JsonParser parser = new JsonParser();
		try {
			return parser.parse(new BufferedReader(new InputStreamReader(baseRequest.getInputStream())).lines()
					.collect(Collectors.joining("\n"))).getAsJsonObject();
		} catch (Exception e) {
			throw new OpenemsException("Unable to parse: " + e.getMessage());
		}
	}

	/**
	 * Handles an http request to 'jsonrpc' endpoint.
	 * 
	 * @param user           the User
	 * @param edgeRpcRequest the EdgeRpcRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private void handleJsonRpc(User user, Request baseRequest, HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) throws OpenemsNamedException {
		// call handler methods
		if (!httpRequest.getMethod().equals("POST")) {
			throw new OpenemsException(
					"Method [" + httpRequest.getMethod() + "] is not supported for JSON-RPC endpoint");
		}

		// parse json and add "jsonrpc" and "id" properties if missing
		JsonObject json = RestHandler.parseJson(baseRequest);
		if (!json.has("jsonrpc")) {
			json.addProperty("jsonrpc", "2.0");
		}
		if (!json.has("id")) {
			json.addProperty("id", UUID.randomUUID().toString());
		}
		if (json.has("params")) {
			JsonObject params = JsonUtils.getAsJsonObject(json, "params");
			if (params.has("payload")) {
				JsonObject payload = JsonUtils.getAsJsonObject(params, "payload");
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
		JsonrpcMessage message = JsonrpcMessage.from(json);
		if (!(message instanceof JsonrpcRequest)) {
			throw new OpenemsException("Only JSON-RPC Request is supported here.");
		}
		JsonrpcRequest request = (JsonrpcRequest) message;

		// handle the request
		CompletableFuture<JsonrpcResponseSuccess> responseFuture = this.handleJsonRpcRequest(user, request);

		// wait for response
		JsonrpcResponseSuccess response;
		try {
			response = responseFuture.get();
		} catch (InterruptedException | ExecutionException e) {
			throw new OpenemsException("Unable to get Response: " + e.getMessage());
		}

		// send response
		this.sendOkResponse(baseRequest, httpResponse, response.toJsonObject());
	}

	/**
	 * Handles an JSON-RPC Request.
	 * 
	 * @param user           the User
	 * @param edgeRpcRequest the EdgeRpcRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleJsonRpcRequest(User user, JsonrpcRequest request)
			throws OpenemsException, OpenemsNamedException {
		switch (request.getMethod()) {

		case GetEdgeConfigRequest.METHOD:
			return this.handleGetEdgeConfigRequest(user, GetEdgeConfigRequest.from(request));

		case UpdateComponentConfigRequest.METHOD:
			return this.handleUpdateComponentConfigRequest(user, UpdateComponentConfigRequest.from(request));

		case ComponentJsonApiRequest.METHOD:
			return this.handleComponentJsonApiRequest(user, ComponentJsonApiRequest.from(request));

		default:
			this.parent.logWarn(this.log, "Unhandled Request: " + request);
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
		}
	}

	/**
	 * Handles a GetEdgeConfigRequest.
	 *
	 * @param user                 the User
	 * @param getEdgeConfigRequest the GetEdgeConfigRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleGetEdgeConfigRequest(User user,
			GetEdgeConfigRequest getEdgeConfigRequest) throws OpenemsNamedException {
		// wrap original request inside ComponentJsonApiRequest
		ComponentJsonApiRequest request = new ComponentJsonApiRequest(OpenemsConstants.COMPONENT_MANAGER_ID,
				getEdgeConfigRequest);

		return this.handleComponentJsonApiRequest(user, request);
	}

	/**
	 * Handles a UpdateComponentConfigRequest.
	 * 
	 * @param user                         the User
	 * @param updateComponentConfigRequest the UpdateComponentConfigRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleUpdateComponentConfigRequest(User user,
			UpdateComponentConfigRequest updateComponentConfigRequest) throws OpenemsNamedException {
		// wrap original request inside ComponentJsonApiRequest
		String componentId = OpenemsConstants.COMPONENT_MANAGER_ID;
		ComponentJsonApiRequest request = new ComponentJsonApiRequest(componentId, updateComponentConfigRequest);

		return this.handleComponentJsonApiRequest(user, request);
	}

	/**
	 * Handles a ComponentJsonApiRequest.
	 * 
	 * @param user    the User
	 * @param request the ComponentJsonApiRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleComponentJsonApiRequest(User user,
			ComponentJsonApiRequest request) throws OpenemsNamedException {
		// get Component
		String componentId = request.getComponentId();
		OpenemsComponent component = this.parent.componentManager.getComponent(componentId);

		if (component == null) {
			throw new OpenemsException("Unable to find Component [" + componentId + "]");
		}

		if (!(component instanceof JsonApi)) {
			throw new OpenemsException("Component [" + componentId + "] is no JsonApi");
		}

		// call JsonApi
		JsonApi jsonApi = (JsonApi) component;
		CompletableFuture<JsonrpcResponseSuccess> responseFuture = jsonApi.handleJsonrpcRequest(user,
				request.getPayload());

		// handle null response
		if (responseFuture == null) {
			OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getPayload().getMethod());
		}

		// Wrap reply in EdgeRpcResponse
		CompletableFuture<JsonrpcResponseSuccess> edgeRpcResponse = new CompletableFuture<>();
		responseFuture.thenAccept(response -> {
			edgeRpcResponse.complete(new GenericJsonrpcResponseSuccess(request.getId(), response.getResult()));
		});

		return edgeRpcResponse;
	}

}
