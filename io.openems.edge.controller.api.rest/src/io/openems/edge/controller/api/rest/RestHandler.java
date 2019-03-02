package io.openems.edge.controller.api.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.JsonApi;
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
				this.handleRest(remainingTargets, baseRequest, request, response);
				break;

			case "jsonrpc":
				this.handleJsonRpc(baseRequest, request, response);
				break;
			}
		} catch (OpenemsNamedException e) {
			throw new IOException(e.getMessage());
		}
	}

	private void handleRest(List<String> targets, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws IOException, OpenemsNamedException {
		if (targets.isEmpty()) {
			throw new OpenemsException("Missing arguments to handle REST-request");
		}

		String thisTarget = targets.get(0);
		List<String> remainingTargets = targets.subList(1, targets.size());

		switch (thisTarget) {
		case "channel":
			this.handleChannel(remainingTargets, baseRequest, request, response);
			break;
		}
	}

	private void handleChannel(List<String> targets, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws IOException, OpenemsNamedException {
		if (targets.size() != 2) {
			throw new OpenemsException("Missing arguments to handle Channel");
		}

		// TODO check general permission
		// if (isAuthenticatedAsRole(request, Role.GUEST)) {
		// // pfff... it's only a "GUEST"! Deny anything but GET requests
		// if (!request.getMethod().equals(Method.GET)) {
		// throw new ResourceException(Status.CLIENT_ERROR_UNAUTHORIZED);
		// }
		// }

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
			this.handleGet(channel, baseRequest, request, response);
			break;
		case "POST":
			this.handlePost(channel, baseRequest, request, response);
			break;
		}
	}

	/**
	 * Handles HTTP GET request.
	 *
	 * @param channel     the affected channel
	 * @param baseRequest the HTTP POST base-request
	 * @param request     the HTTP POST request
	 * @param response    the result to be returned
	 * @throws OpenemsException on error
	 */
	private void handleGet(Channel<?> channel, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws OpenemsException {
		// TODO check read permission
		// assertAllowed(request, channel.readRoles());

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
	 * @param channel     the affected channel
	 * @param baseRequest the HTTP POST base-request
	 * @param request     the HTTP POST request
	 * @param response    the result to be returned
	 * @throws OpenemsException on error
	 */
	private void handlePost(Channel<?> channel, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws OpenemsException {
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
	 * @param edgeRpcRequest the EdgeRpcRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private void handleJsonRpc(Request baseRequest, HttpServletRequest httpRequest, HttpServletResponse httpResponse)
			throws OpenemsNamedException {
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
		CompletableFuture<JsonrpcResponseSuccess> responseFuture = this.handleJsonRpcRequest(request);

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
	 * @param edgeRpcRequest the EdgeRpcRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleJsonRpcRequest(JsonrpcRequest request)
			throws OpenemsException, OpenemsNamedException {
		switch (request.getMethod()) {

		case GetEdgeConfigRequest.METHOD:
			return this.handleGetEdgeConfigRequest(GetEdgeConfigRequest.from(request));

		case UpdateComponentConfigRequest.METHOD:
			return this.handleUpdateComponentConfigRequest(UpdateComponentConfigRequest.from(request));

		case ComponentJsonApiRequest.METHOD:
			return this.handleComponentJsonApiRequest(ComponentJsonApiRequest.from(request));

		default:
			this.parent.logWarn(this.log, "Unhandled Request: " + request);
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
		}
	}

	/**
	 * Handles a GetEdgeConfigRequest.
	 *
	 * @param getEdgeConfigRequest the GetEdgeConfigRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleGetEdgeConfigRequest(
			GetEdgeConfigRequest getEdgeConfigRequest) throws OpenemsNamedException {
		// wrap original request inside ComponentJsonApiRequest
		ComponentJsonApiRequest request = new ComponentJsonApiRequest(OpenemsConstants.COMPONENT_MANAGER_ID,
				getEdgeConfigRequest);

		return this.handleComponentJsonApiRequest(request);
	}

	/**
	 * Handles a UpdateComponentConfigRequest.
	 * 
	 * @param updateComponentConfigRequest the UpdateComponentConfigRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleUpdateComponentConfigRequest(
			UpdateComponentConfigRequest updateComponentConfigRequest) throws OpenemsNamedException {
		// wrap original request inside ComponentJsonApiRequest
		String componentId = OpenemsConstants.COMPONENT_MANAGER_ID;
		ComponentJsonApiRequest request = new ComponentJsonApiRequest(componentId, updateComponentConfigRequest);

		return this.handleComponentJsonApiRequest(request);
	}

	/**
	 * Handles a ComponentJsonApiRequest.
	 * 
	 * @param request the ComponentJsonApiRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleComponentJsonApiRequest(ComponentJsonApiRequest request)
			throws OpenemsNamedException {
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
		CompletableFuture<JsonrpcResponseSuccess> responseFuture = jsonApi.handleJsonrpcRequest(request.getPayload());

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
