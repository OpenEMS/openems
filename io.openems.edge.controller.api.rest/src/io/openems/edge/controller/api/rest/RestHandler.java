package io.openems.edge.controller.api.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcMessage;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseError;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.ComponentJsonApiRequest;
import io.openems.common.jsonrpc.request.CreateComponentConfigRequest;
import io.openems.common.jsonrpc.request.DeleteComponentConfigRequest;
import io.openems.common.jsonrpc.request.GetEdgeConfigRequest;
import io.openems.common.jsonrpc.request.QueryHistoricTimeseriesDataRequest;
import io.openems.common.jsonrpc.request.QueryHistoricTimeseriesEnergyRequest;
import io.openems.common.jsonrpc.request.SetChannelValueRequest;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.jsonrpc.response.QueryHistoricTimeseriesDataResponse;
import io.openems.common.jsonrpc.response.QueryHistoricTimeseriesEnergyResponse;
import io.openems.common.session.Role;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.StringUtils;
import io.openems.common.utils.UuidUtils;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RestHandler extends AbstractHandler {

	private final Logger log = LoggerFactory.getLogger(RestHandler.class);

	private final AbstractRestApi parent;

	public RestHandler(AbstractRestApi parent) {
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
			var remainingTargets = targets.subList(1, targets.size());

			switch (thisTarget) {
			case "rest":
				this.handleRest(user, remainingTargets, baseRequest, request, response);
				break;

			case "jsonrpc":
				// Validate API Access-Mode
				switch (this.parent.getAccessMode()) {
				case READ_ONLY:
					throw new OpenemsException("REST-Api is in Read-Only mode");
				case READ_WRITE:
				case WRITE_ONLY:
					this.handleJsonRpc(user, baseRequest, request, response);
				}
				break;

			default:
				throw new OpenemsException("Unknown REST endpoint: " + target);

			}
		} catch (OpenemsNamedException e) {
			if (this.parent.isDebugModeEnabled()) {
				this.parent.logError(this.log, "REST call failed: " + e.getMessage());
			}
			this.sendErrorResponse(baseRequest, response, UuidUtils.getNilUuid(), e);
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
						var userOpt = this.parent.getUserService().authenticate(username, password);
						if (userOpt.isPresent()) {
							return userOpt.get();
						}
						// authenticate using password only
						userOpt = this.parent.getUserService().authenticate(password);
						if (userOpt.isPresent()) {
							return userOpt.get();
						}
					}
				}
			}
		}
		throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
	}

	private boolean handleRest(User user, List<String> targets, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws IOException, OpenemsNamedException {
		if (targets.isEmpty()) {
			throw new OpenemsException("Missing arguments to handle REST-request");
		}

		var thisTarget = targets.get(0);
		var remainingTargets = targets.subList(1, targets.size());

		switch (thisTarget) {
		case "channel":
			return this.handleChannel(user, remainingTargets, baseRequest, request, response);

		default:
			throw new OpenemsException("Unhandled REST target [" + thisTarget + "]");
		}
	}

	private boolean handleChannel(User user, List<String> targets, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws IOException, OpenemsNamedException {
		if (targets.size() != 2) {
			throw new OpenemsException("Missing arguments to handle Channel");
		}

		// get request attributes
		var channelAddress = new ChannelAddress(targets.get(0), targets.get(1));

		// call handler methods
		switch (request.getMethod()) {
		case "GET":
			return this.handleGet(user, channelAddress, baseRequest, request, response);

		case "POST":
			// Validate API Access-Mode
			switch (this.parent.getAccessMode()) {
			case READ_ONLY:
				throw new OpenemsException("REST-Api is in Read-Only mode");
			case READ_WRITE:
			case WRITE_ONLY:
				return this.handlePost(user, channelAddress, baseRequest, request, response);
			}

		default:
			throw new OpenemsException("Unhandled REST Channel request method [" + request.getMethod() + "]");
		}
	}

	/**
	 * Handles HTTP GET request.
	 *
	 * @param user           the {@link User}
	 * @param channelAddress the ChannelAddress (may include RegExp)
	 * @param baseRequest    the HTTP POST base-request
	 * @param request        the HTTP POST request
	 * @param response       the result to be returned
	 * @return false if request cannot be handled or ok response was not sent
	 * @throws OpenemsNamedException on error
	 */
	private boolean handleGet(User user, ChannelAddress channelAddress, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws OpenemsNamedException {
		user.assertRoleIsAtLeast("HTTP GET", Role.GUEST);

		var components = this.parent.getComponentManager().getEnabledComponents();
		var channels = getChannels(components, channelAddress);

		// Return with error when no matching channel was found
		if (channels.size() == 0) {
			if (this.parent.isDebugModeEnabled()) {
				this.parent.logWarn(this.log, "REST call by User [" + user.getName() + "]: GET Channel ["
						+ channelAddress.toString() + "] Result [No Match]");
			}
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return false;
		}

		// Creating JSON response for all matched channels
		var channeljson = new JsonArray();
		for (Channel<?> channel : channels) {
			var j = new JsonObject();
			// name
			j.addProperty("address", channel.address().toString());
			// type
			j.addProperty("type", channel.getType().name());
			// accessMode
			var accessMode = channel.channelDoc().getAccessMode();
			j.addProperty("accessMode", accessMode.getAbbreviation());
			// text
			j.addProperty("text", channel.channelDoc().getText());
			// unit
			j.addProperty("unit", channel.channelDoc().getUnit().getSymbol());
			// value
			if (accessMode != AccessMode.WRITE_ONLY) {
				j.add("value", channel.value().asJson());
			}
			channeljson.add(j);
		}

		// if this a request for a single channel only return a single JsonObject, not
		// an array (for compatibility to previous versions)
		var result = channeljson.size() == 1 ? channeljson.get(0) : channeljson;

		if (this.parent.isDebugModeEnabled()) {
			this.parent.logInfo(this.log, "REST call by User [" + user.getName() + "]: GET Channel ["
					+ channelAddress.toString() + "] Result [" + result.toString() + "]");
		}

		return this.sendOkResponse(baseRequest, response, result);
	}

	/**
	 * Gets a list of Channels that match the {@link ChannelAddress}; regular
	 * expressions are allowed.
	 * 
	 * @param components     a list of {@link OpenemsComponent}s
	 * @param channelAddress the {@link ChannelAddress} of the GET request
	 * @return a list of matching {@link Channel}s
	 * @throws PatternSyntaxException on regular expression error
	 */
	protected static List<Channel<?>> getChannels(List<OpenemsComponent> components, ChannelAddress channelAddress)
			throws PatternSyntaxException {
		return components.stream() //
				.filter(component -> Pattern.matches(channelAddress.getComponentId(), component.id())) //
				.flatMap(component -> component.channels().stream()) //
				.filter(channel -> Pattern.matches(channelAddress.getChannelId(), channel.channelId().id())) //
				.toList();
	}

	private void sendErrorResponse(Request baseRequest, HttpServletResponse response, UUID jsonrpcId, Throwable ex) {
		try {
			response.setContentType("application/json");
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			baseRequest.setHandled(true);
			JsonrpcResponseError message;
			if (ex instanceof OpenemsNamedException) {
				// Check for authentication error and set more specific response code
				// accordingly
				if (((OpenemsNamedException) ex).getError() == OpenemsError.COMMON_AUTHENTICATION_FAILED) {
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				}
				// Get Named Exception error response
				message = new JsonrpcResponseError(jsonrpcId, (OpenemsNamedException) ex);
			} else {
				// Get GENERIC error response
				message = new JsonrpcResponseError(jsonrpcId, ex.getMessage());
			}
			response.getWriter().write(message.toString());
		} catch (IOException e) {
			this.parent.logWarn(this.log, "Unable to send Error-Response: " + e.getMessage());
		}
	}

	private boolean sendOkResponse(Request baseRequest, HttpServletResponse response, JsonElement data)
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
	 * @param user           the {@link User}
	 * @param channelAddress the {@link ChannelAddress}
	 * @param baseRequest    the HTTP POST base-request
	 * @param request        the HTTP POST request
	 * @param response       the result to be returned
	 * @return false if ok response was not sent
	 * @throws OpenemsNamedException on error
	 */
	private boolean handlePost(User user, ChannelAddress channelAddress, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response) throws OpenemsNamedException {
		user.assertRoleIsAtLeast("HTTP POST", Role.OWNER);

		// parse json
		var jHttpPost = RestHandler.parseJson(baseRequest);

		// parse value
		JsonElement jValue;
		if (!jHttpPost.has("value")) {
			throw new OpenemsException("Value is missing");
		}
		jValue = jHttpPost.get("value");

		if (this.parent.isDebugModeEnabled()) {
			this.parent.logInfo(this.log, "REST call by User [" + user.getName() + "]: POST Channel ["
					+ channelAddress.toString() + "] value [" + jValue + "]");
		}

		// send request to apiworker
		this.parent.apiWorker.handleSetChannelValueRequest(this.parent.getComponentManager(), user,
				new SetChannelValueRequest(channelAddress.getComponentId(), channelAddress.getChannelId(), jValue));

		return this.sendOkResponse(baseRequest, response, new JsonObject());
	}

	/**
	 * Parses a Request to JSON.
	 *
	 * @param baseRequest the Request
	 * @return the request as JSON
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
	 * @param baseRequest  the HTTP POST base-request
	 * @param httpRequest  the HTTP POST request
	 * @param httpResponse the HTTP response
	 * @throws OpenemsNamedException on error
	 */
	private void handleJsonRpc(User user, Request baseRequest, HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) throws OpenemsNamedException {
		user.assertRoleIsAtLeast("HTTP POST JSON-RPC", Role.OWNER);

		var requestId = new UUID(0L, 0L); /* dummy UUID */
		try {
			// call handler methods
			if (!httpRequest.getMethod().equals("POST")) {
				throw new OpenemsException(
						"Method [" + httpRequest.getMethod() + "] is not supported for JSON-RPC endpoint");
			}

			// parse json and add "jsonrpc" and "id" properties if missing
			var json = RestHandler.parseJson(baseRequest);
			if (this.parent.isDebugModeEnabled()) {
				this.parent.logInfo(this.log,
						"REST/JsonRpc call by User [" + user.getName() + "]: " + StringUtils.toShortString(json, 100));
			}

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
			requestId = request.getId();

			// handle the request
			var responseFuture = this.handleJsonRpcRequest(user, request);

			// wait for response
			JsonrpcResponseSuccess response;
			try {
				response = responseFuture.get();
			} catch (InterruptedException | ExecutionException e) {
				this.sendErrorResponse(baseRequest, httpResponse, request.getId(),
						new OpenemsException("Unable to get Response: " + e.getMessage()));
				return;
			}

			// send response
			this.sendOkResponse(baseRequest, httpResponse, response.toJsonObject());

		} catch (Exception e) {
			this.sendErrorResponse(baseRequest, httpResponse, requestId,
					new OpenemsException("Unable to get Response: " + e.getMessage()));
		}
	}

	/**
	 * Handles an JSON-RPC Request.
	 *
	 * @param user    the {@link User}
	 * @param request the {@link JsonrpcRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleJsonRpcRequest(User user, JsonrpcRequest request)
			throws OpenemsException, OpenemsNamedException {
		switch (request.getMethod()) {

		case QueryHistoricTimeseriesDataRequest.METHOD:
			return this.handleQueryHistoricDataRequest(user, QueryHistoricTimeseriesDataRequest.from(request));

		case QueryHistoricTimeseriesEnergyRequest.METHOD:
			return this.handleQueryHistoricEnergyRequest(user, QueryHistoricTimeseriesEnergyRequest.from(request));

		case GetEdgeConfigRequest.METHOD:
			return this.handleGetEdgeConfigRequest(user, GetEdgeConfigRequest.from(request));

		case CreateComponentConfigRequest.METHOD:
			return this.handleCreateComponentConfigRequest(user, CreateComponentConfigRequest.from(request));

		case UpdateComponentConfigRequest.METHOD:
			return this.handleUpdateComponentConfigRequest(user, UpdateComponentConfigRequest.from(request));

		case DeleteComponentConfigRequest.METHOD:
			return this.handleDeleteComponentConfigRequest(user, DeleteComponentConfigRequest.from(request));

		case ComponentJsonApiRequest.METHOD:
			return this.handleComponentJsonApiRequest(user, ComponentJsonApiRequest.from(request));

		default:
			this.parent.logWarn(this.log, "Unhandled Request: " + request);
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
		}
	}

	/**
	 * Handles a QueryHistoricDataRequest.
	 *
	 * @param user    the {@link User}
	 * @param request the {@link QueryHistoricTimeseriesDataRequest}
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleQueryHistoricDataRequest(User user,
			QueryHistoricTimeseriesDataRequest request) throws OpenemsNamedException {
		var data = this.parent.getTimedata().queryHistoricData(//
				null, /* ignore Edge-ID */
				request);

		// JSON-RPC response
		return CompletableFuture.completedFuture(new QueryHistoricTimeseriesDataResponse(request.getId(), data));
	}

	/**
	 * Handles a QueryHistoricEnergyRequest.
	 *
	 * @param user    the {@link User}
	 * @param request the {@link QueryHistoricTimeseriesEnergyRequest}
	 * @return the Future JSPN-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleQueryHistoricEnergyRequest(User user,
			QueryHistoricTimeseriesEnergyRequest request) throws OpenemsNamedException {
		Map<ChannelAddress, JsonElement> data = this.parent.getTimedata().queryHistoricEnergy(//
				null, /* ignore Edge-ID */
				request.getFromDate(), request.getToDate(), request.getChannels());

		// JSON-RPC response
		return CompletableFuture.completedFuture(new QueryHistoricTimeseriesEnergyResponse(request.getId(), data));
	}

	/**
	 * Handles a GetEdgeConfigRequest.
	 *
	 * @param user                 the {@link User}
	 * @param getEdgeConfigRequest the {@link GetEdgeConfigRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleGetEdgeConfigRequest(User user,
			GetEdgeConfigRequest getEdgeConfigRequest) throws OpenemsNamedException {
		// wrap original request inside ComponentJsonApiRequest
		var request = new ComponentJsonApiRequest(ComponentManager.SINGLETON_COMPONENT_ID, getEdgeConfigRequest);

		return this.handleComponentJsonApiRequest(user, request);
	}

	/**
	 * Handles a CreateComponentConfigRequest.
	 *
	 * @param user                         the {@link User}
	 * @param createComponentConfigRequest the {@link CreateComponentConfigRequest}
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleCreateComponentConfigRequest(User user,
			CreateComponentConfigRequest createComponentConfigRequest) throws OpenemsNamedException {
		// wrap original request inside ComponentJsonApiRequest
		var request = new ComponentJsonApiRequest(ComponentManager.SINGLETON_COMPONENT_ID,
				createComponentConfigRequest);

		return this.handleComponentJsonApiRequest(user, request);
	}

	/**
	 * Handles a UpdateComponentConfigRequest.
	 *
	 * @param user                         the {@link User}
	 * @param updateComponentConfigRequest the {@link UpdateComponentConfigRequest}
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleUpdateComponentConfigRequest(User user,
			UpdateComponentConfigRequest updateComponentConfigRequest) throws OpenemsNamedException {
		// wrap original request inside ComponentJsonApiRequest
		var request = new ComponentJsonApiRequest(ComponentManager.SINGLETON_COMPONENT_ID,
				updateComponentConfigRequest);

		return this.handleComponentJsonApiRequest(user, request);
	}

	/**
	 * Handles a DeleteComponentConfigRequest.
	 *
	 * @param user                         the User
	 * @param deleteComponentConfigRequest the DeleteComponentConfigRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleDeleteComponentConfigRequest(User user,
			DeleteComponentConfigRequest deleteComponentConfigRequest) throws OpenemsNamedException {
		// wrap original request inside ComponentJsonApiRequest
		var request = new ComponentJsonApiRequest(ComponentManager.SINGLETON_COMPONENT_ID,
				deleteComponentConfigRequest);

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
		var componentId = request.getComponentId();
		var component = this.parent.getComponentManager().getComponent(componentId);

		if (component == null) {
			throw new OpenemsException("Unable to find Component [" + componentId + "]");
		}

		if (!(component instanceof JsonApi)) {
			throw new OpenemsException("Component [" + componentId + "] is no JsonApi");
		}

		// call JsonApi
		var jsonApi = (JsonApi) component;
		CompletableFuture<? extends JsonrpcResponseSuccess> responseFuture = jsonApi.handleJsonrpcRequest(user,
				request.getPayload());

		// handle null response
		if (responseFuture == null) {
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getPayload().getMethod());
		}

		// Wrap reply in EdgeRpcResponse
		var edgeRpcResponse = new CompletableFuture<JsonrpcResponseSuccess>();
		responseFuture.whenComplete((r, ex) -> {
			if (ex != null) {
				edgeRpcResponse.completeExceptionally(ex);
			} else if (r != null) {
				edgeRpcResponse.complete(new GenericJsonrpcResponseSuccess(request.getId(), r.getResult()));
			} else {
				edgeRpcResponse.completeExceptionally(new OpenemsNamedException(OpenemsError.JSONRPC_UNHANDLED_METHOD,
						request.getPayload().getMethod()));
			}
		});

		return edgeRpcResponse;
	}

}
