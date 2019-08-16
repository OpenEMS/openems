package io.openems.edge.controller.api.rest;

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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.openems.common.accesscontrol.RoleId;
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
import io.openems.common.jsonrpc.request.CreateComponentConfigRequest;
import io.openems.common.jsonrpc.request.DeleteComponentConfigRequest;
import io.openems.common.jsonrpc.request.GetEdgeConfigRequest;
import io.openems.common.jsonrpc.request.SetChannelValueRequest;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.StringUtils;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.JsonApi;

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
			// TODO check for missing authorization
			RoleId roleId = this.authenticate(request);

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
				this.handleRest(remainingTargets, baseRequest, request, response, roleId);
				break;

			case "jsonrpc":
				this.handleJsonRpc(baseRequest, request, response, roleId);
				break;

			default:
				throw new OpenemsException("Unknown REST endpoint: " + target);

			}
		} catch (OpenemsNamedException e) {
			if (this.parent.isDebugModeEnabled()) {
				this.parent.logError(this.log, "REST call failed: " + e.getMessage());
			}
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
	private RoleId authenticate(HttpServletRequest request) throws OpenemsNamedException {
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
						return this.parent.accessControl.login(username,password, false).getValue();
					}
				}
			}
		}
		throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
	}

	private boolean handleRest(List<String> targets, Request baseRequest, HttpServletRequest request,
							   HttpServletResponse response, RoleId roleId) throws IOException, OpenemsNamedException {
		if (targets.isEmpty()) {
			throw new OpenemsException("Missing arguments to handle REST-request");
		}

		String thisTarget = targets.get(0);
		List<String> remainingTargets = targets.subList(1, targets.size());

		switch (thisTarget) {
		case "channel":
			return this.handleChannel(remainingTargets, baseRequest, request, response, roleId);

		default:
			throw new OpenemsException("Unhandled REST target [" + thisTarget + "]");

		}
	}

	private boolean handleChannel(List<String> targets, Request baseRequest, HttpServletRequest request,
								  HttpServletResponse response, RoleId roleId) throws IOException, OpenemsNamedException {
		if (targets.size() != 2) {
			throw new OpenemsException("Missing arguments to handle Channel");
		}

		// get request attributes
		ChannelAddress channelAddress = new ChannelAddress(targets.get(0), targets.get(1));

		// call handler methods
		switch (request.getMethod()) {
		case "GET":
			return this.handleGet(channelAddress, baseRequest, request, response, roleId);

		case "POST":
			return this.handlePost(channelAddress, baseRequest, request, response);

		default:
			throw new OpenemsException("Unhandled REST Channel request method [" + request.getMethod() + "]");
		}
	}

	/**
	 * Handles HTTP GET request.
	 * 
	 * @param channelAddress the ChannelAddress
	 * @param baseRequest    the HTTP POST base-request
	 * @param request        the HTTP POST request
	 * @param response       the result to be returned
	 * @param roleId
	 * @throws OpenemsNamedException
	 */
	private boolean handleGet(ChannelAddress channelAddress, Request baseRequest, HttpServletRequest request,
							  HttpServletResponse response, RoleId roleId) throws OpenemsNamedException {

		// get channel
		Channel<?> channel;
		try {
			channel = this.parent.componentManager.getChannel(channelAddress);
		} catch (IllegalArgumentException e) {
			this.parent.logWarn(this.log, e.getMessage());
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return false;
		}

		JsonObject j = new JsonObject();
		JsonElement value = channel.value().asJson();
		if (this.parent.isDebugModeEnabled()) {
			// TODO take the logged in user
			this.parent.logInfo(this.log, "REST call by Role [" + roleId + "]: GET Channel ["
					+ channelAddress.toString() + "] value [" + value + "]");
		}
		j.add("value", value);
		// type
		OpenemsType type = channel.channelDoc().getType();
		j.addProperty("type", type.toString().toLowerCase());
		// writable
		j.addProperty("writable", //
			channel instanceof WriteChannel<?> //
		);

		return this.sendOkResponse(baseRequest, response, j);
	}

	private boolean sendOkResponse(Request baseRequest, HttpServletResponse response, JsonObject data)
			throws OpenemsException {
		try {
			response.setContentType("application/json");
			response.setStatus(HttpServletResponse.SC_OK);
			baseRequest.setHandled(true);
			response.getWriter().write(data.toString());
			return true;
		} catch (IOException e) {
			throw new OpenemsException("Unable to send Ok-Response: " + e.getMessage());
		}
	}

	/**
	 * Handles HTTP POST request.
	 *
	 * @param channelAddress the ChannelAddress
	 * @param baseRequest    the HTTP POST base-request
	 * @param request        the HTTP POST request
	 * @param response       the result to be returned
	 * @throws OpenemsNamedException
	 */
	private boolean handlePost(ChannelAddress channelAddress, Request baseRequest,
							   HttpServletRequest request, HttpServletResponse response) throws OpenemsNamedException {

		// parse json
		JsonObject jHttpPost = RestHandler.parseJson(baseRequest);

		// parse value
		JsonElement jValue;
		if (jHttpPost.has("value")) {
			jValue = jHttpPost.get("value");
		} else {
			throw new OpenemsException("Value is missing");
		}

		if (this.parent.isDebugModeEnabled()) {
			// TODO take the logged in user
			this.parent.logInfo(this.log, "REST call by User [" + "user" + "]: POST Channel ["
					+ channelAddress.toString() + "] value [" + jValue + "]");
		}

		// send request to apiworker
		this.parent.apiWorker.handleSetChannelValueRequest(this.parent.componentManager,
                new SetChannelValueRequest(channelAddress.getComponentId(), channelAddress.getChannelId(), jValue));

		return this.sendOkResponse(baseRequest, response, new JsonObject());
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
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private void handleJsonRpc(Request baseRequest, HttpServletRequest httpRequest,
							   HttpServletResponse httpResponse, RoleId roleId) throws OpenemsNamedException {
		// call handler methods
		if (!httpRequest.getMethod().equals("POST")) {
			throw new OpenemsException(
					"Method [" + httpRequest.getMethod() + "] is not supported for JSON-RPC endpoint");
		}

		// parse json and add "jsonrpc" and "id" properties if missing
		JsonObject json = RestHandler.parseJson(baseRequest);
		if (this.parent.isDebugModeEnabled()) {
			this.parent.logInfo(this.log,
					// TODO take the logged in user
					"REST/JsonRpc call by Role [" + roleId + "]: " + StringUtils.toShortString(json, 100));
		}

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
		CompletableFuture<JsonrpcResponseSuccess> responseFuture = this.handleJsonRpcRequest(request, roleId);

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
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleJsonRpcRequest(JsonrpcRequest request, RoleId roleId)
			throws OpenemsNamedException {
		switch (request.getMethod()) {

		case GetEdgeConfigRequest.METHOD:
			return this.handleGetEdgeConfigRequest(GetEdgeConfigRequest.from(request));

		case CreateComponentConfigRequest.METHOD:
			return this.handleCreateComponentConfigRequest(CreateComponentConfigRequest.from(request));

		case UpdateComponentConfigRequest.METHOD:
			return this.handleUpdateComponentConfigRequest(UpdateComponentConfigRequest.from(request));

		case DeleteComponentConfigRequest.METHOD:
			return this.handleDeleteComponentConfigRequest(DeleteComponentConfigRequest.from(request));

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
	private CompletableFuture<JsonrpcResponseSuccess> handleGetEdgeConfigRequest(GetEdgeConfigRequest getEdgeConfigRequest) throws OpenemsNamedException {
		// wrap original request inside ComponentJsonApiRequest
		ComponentJsonApiRequest request = new ComponentJsonApiRequest(OpenemsConstants.COMPONENT_MANAGER_ID,
				getEdgeConfigRequest);

		return this.handleComponentJsonApiRequest(request);
	}

	/**
	 * Handles a CreateComponentConfigRequest.
	 * 
	 * @param createComponentConfigRequest the CreateComponentConfigRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleCreateComponentConfigRequest(CreateComponentConfigRequest createComponentConfigRequest) throws OpenemsNamedException {
		// wrap original request inside ComponentJsonApiRequest
		String componentId = OpenemsConstants.COMPONENT_MANAGER_ID;
		ComponentJsonApiRequest request = new ComponentJsonApiRequest(componentId, createComponentConfigRequest);

		return this.handleComponentJsonApiRequest(request);
	}

	/**
	 * Handles a UpdateComponentConfigRequest.
	 * 
	 * @param updateComponentConfigRequest the UpdateComponentConfigRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleUpdateComponentConfigRequest(UpdateComponentConfigRequest updateComponentConfigRequest) throws OpenemsNamedException {
		// wrap original request inside ComponentJsonApiRequest
		String componentId = OpenemsConstants.COMPONENT_MANAGER_ID;
		ComponentJsonApiRequest request = new ComponentJsonApiRequest(componentId, updateComponentConfigRequest);

		return this.handleComponentJsonApiRequest(request);
	}

	/**
	 * Handles a DeleteComponentConfigRequest.
	 * 
	 * @param deleteComponentConfigRequest the DeleteComponentConfigRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleDeleteComponentConfigRequest(DeleteComponentConfigRequest deleteComponentConfigRequest) throws OpenemsNamedException {
		// wrap original request inside ComponentJsonApiRequest
		String componentId = OpenemsConstants.COMPONENT_MANAGER_ID;
		ComponentJsonApiRequest request = new ComponentJsonApiRequest(componentId, deleteComponentConfigRequest);

		return this.handleComponentJsonApiRequest(request);
	}

	/**
	 * Handles a ComponentJsonApiRequest.
	 * 
	 * @param request the ComponentJsonApiRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleComponentJsonApiRequest(ComponentJsonApiRequest request) throws OpenemsNamedException {
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
		CompletableFuture<JsonrpcResponseSuccess> responseFuture = jsonApi.handleJsonrpcRequest(
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
