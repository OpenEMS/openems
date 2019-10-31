package io.openems.backend.b2brest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.backend.common.jsonrpc.request.GetEdgesChannelsValuesRequest;
import io.openems.backend.common.jsonrpc.request.GetEdgesStatusRequest;
import io.openems.backend.common.jsonrpc.response.GetEdgesChannelsValuesResponse;
import io.openems.backend.common.jsonrpc.response.GetEdgesStatusResponse;
import io.openems.backend.common.jsonrpc.response.GetEdgesStatusResponse.EdgeInfo;
import io.openems.backend.metadata.api.BackendUser;
import io.openems.backend.metadata.api.Edge;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcMessage;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseError;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.ComponentJsonApiRequest;
import io.openems.common.jsonrpc.request.EdgeRpcRequest;
import io.openems.common.jsonrpc.request.GetEdgeConfigRequest;
import io.openems.common.jsonrpc.request.SetGridConnScheduleRequest;
import io.openems.common.jsonrpc.response.EdgeRpcResponse;
import io.openems.common.jsonrpc.response.GetEdgeConfigResponse;
import io.openems.common.session.Role;
import io.openems.common.session.User;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;

public class RestHandler extends AbstractHandler {

	private final Logger log = LoggerFactory.getLogger(RestHandler.class);

	private final B2bRest parent;

	public RestHandler(B2bRest parent) {
		this.parent = parent;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		try {
			BackendUser user = this.authenticate(request);

			List<String> targets = Arrays.asList(//
					target.substring(1) // remove leading '/'
							.split("/"));

			if (targets.isEmpty()) {
				throw new OpenemsException("Missing arguments to handle request");
			}

			String thisTarget = targets.get(0);
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
	 * @return the User
	 * @throws OpenemsNamedException on error
	 */
	private BackendUser authenticate(HttpServletRequest request) throws OpenemsNamedException {
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
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			baseRequest.setHandled(true);
			JsonrpcResponseError message;
			if (ex instanceof OpenemsNamedException) {
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

	/**
	 * Parses a Request to JSON.
	 * 
	 * @param baseRequest the Request
	 * @return the {@link JsonObject}
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
	 * @param user         the User
	 * @param baseRequest  the {@link Request}
	 * @param httpRequest  the {@link HttpServletRequest}
	 * @param httpResponse the {@link HttpServletResponse}
	 */
	private void handleJsonRpc(BackendUser user, Request baseRequest, HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {
		UUID requestId = new UUID(0L, 0L); /* dummy UUID */
		try {
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
			CompletableFuture<? extends JsonrpcResponseSuccess> responseFuture = this.handleJsonRpcRequest(user,
					request);

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

		} catch (OpenemsNamedException e) {
			this.sendErrorResponse(baseRequest, httpResponse, requestId,
					new OpenemsException("Unable to get Response: " + e.getMessage()));
		}
	}

	/**
	 * Handles an JSON-RPC Request.
	 * 
	 * @param user    the User
	 * @param request the JsonrpcRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<? extends JsonrpcResponseSuccess> handleJsonRpcRequest(BackendUser user,
			JsonrpcRequest request) throws OpenemsNamedException {
		switch (request.getMethod()) {

		case EdgeRpcRequest.METHOD:
			return this.handleEdgeRpcRequest(user, request.getId(), EdgeRpcRequest.from(request));

		case GetEdgesStatusRequest.METHOD:
			return this.handleGetStatusOfEdgesRequest(user, request.getId(), GetEdgesStatusRequest.from(request));

		case GetEdgesChannelsValuesRequest.METHOD:
			return this.handleGetChannelsValuesRequest(user, request.getId(),
					GetEdgesChannelsValuesRequest.from(request));

		case SetGridConnScheduleRequest.METHOD:
			return this.handleSetGridConnScheduleRequest(user, request.getId(),
					SetGridConnScheduleRequest.from(request));

		default:
			this.parent.logWarn(this.log, "Unhandled Request: " + request);
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
		}
	}

	/**
	 * Handles an EdgeRpcRequest.
	 * 
	 * @param backendUser    the {@link BackendUser}
	 * @param edgeRpcRequest the {@link EdgeRpcRequest}
	 * @param messageId      the JSON-RPC Message-ID
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<EdgeRpcResponse> handleEdgeRpcRequest(BackendUser backendUser, UUID messageId,
			EdgeRpcRequest edgeRpcRequest) throws OpenemsNamedException {
		String edgeId = edgeRpcRequest.getEdgeId();
		JsonrpcRequest request = edgeRpcRequest.getPayload();
		User user = backendUser.getAsCommonUser(edgeId);
		user.assertRoleIsAtLeast(EdgeRpcRequest.METHOD, Role.GUEST);

		CompletableFuture<JsonrpcResponseSuccess> resultFuture;
		switch (request.getMethod()) {

		case GetEdgeConfigRequest.METHOD:
			resultFuture = this.handleGetEdgeConfigRequest(edgeId, user, GetEdgeConfigRequest.from(request));
			break;

		case ComponentJsonApiRequest.METHOD:
			resultFuture = this.handleComponentJsonApiRequest(edgeId, user, ComponentJsonApiRequest.from(request));
			break;

		default:
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
		}

		// Wrap reply in EdgeRpcResponse
		CompletableFuture<EdgeRpcResponse> result = new CompletableFuture<EdgeRpcResponse>();
		resultFuture.whenComplete((r, ex) -> {
			if (ex != null) {
				result.completeExceptionally(ex);
			} else if (r != null) {
				result.complete(new EdgeRpcResponse(edgeRpcRequest.getId(), r));
			} else {
				result.completeExceptionally(
						new OpenemsNamedException(OpenemsError.JSONRPC_UNHANDLED_METHOD, request.getMethod()));
			}
		});
		return result;
	}

	/**
	 * Handles a GetEdgeConfigRequest.
	 * 
	 * @param edgeId  the Edge-ID
	 * @param user    the User - no specific level required
	 * @param request the GetEdgeConfigRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleGetEdgeConfigRequest(String edgeId, User user,
			GetEdgeConfigRequest request) throws OpenemsNamedException {
		EdgeConfig config = this.parent.metadata.getEdgeOrError(edgeId).getConfig();

		// JSON-RPC response
		return CompletableFuture.completedFuture(new GetEdgeConfigResponse(request.getId(), config));
	}

	/**
	 * Handles a ComponentJsonApiRequest.
	 * 
	 * @param edgeId                  the Edge-ID
	 * @param user                    the User - Installer-level required
	 * @param componentJsonApiRequest the ComponentJsonApiRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleComponentJsonApiRequest(String edgeId, User user,
			ComponentJsonApiRequest componentJsonApiRequest) throws OpenemsNamedException {
		user.assertRoleIsAtLeast(ComponentJsonApiRequest.METHOD, Role.GUEST);

		return this.parent.edgeWebsocket.send(edgeId, user, componentJsonApiRequest);
	}

	/**
	 * Handles a GetStatusOfEdgesRequest.
	 * 
	 * @param user      the User
	 * @param messageId the JSON-RPC Message-ID
	 * @param request   the GetStatusOfEdgesRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<GetEdgesStatusResponse> handleGetStatusOfEdgesRequest(BackendUser user, UUID messageId,
			GetEdgesStatusRequest request) throws OpenemsNamedException {
		Map<String, EdgeInfo> result = new HashMap<>();
		for (Entry<String, Role> entry : user.getEdgeRoles().entrySet()) {
			String edgeId = entry.getKey();

			// assure read permissions of this User for this Edge.
			if (!user.edgeRoleIsAtLeast(edgeId, Role.GUEST)) {
				continue;
			}

			Optional<Edge> edgeOpt = this.parent.metadata.getEdge(edgeId);
			if (edgeOpt.isPresent()) {
				Edge edge = edgeOpt.get();
				EdgeInfo info = new EdgeInfo(edge.isOnline());
				result.put(edge.getId(), info);
			}
		}
		return CompletableFuture.completedFuture(new GetEdgesStatusResponse(messageId, result));
	}

	/**
	 * Handles a GetChannelsValuesRequest.
	 * 
	 * @param user      the User
	 * @param messageId the JSON-RPC Message-ID
	 * @param request   the GetChannelsValuesRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<GetEdgesChannelsValuesResponse> handleGetChannelsValuesRequest(BackendUser user,
			UUID messageId, GetEdgesChannelsValuesRequest request) throws OpenemsNamedException {
		GetEdgesChannelsValuesResponse response = new GetEdgesChannelsValuesResponse(messageId);
		for (String edgeId : request.getEdgeIds()) {
			// assure read permissions of this User for this Edge.
			if (!user.edgeRoleIsAtLeast(edgeId, Role.GUEST)) {
				continue;
			}

			for (ChannelAddress channel : request.getChannels()) {
				Optional<JsonElement> value = this.parent.timeData.getChannelValue(edgeId, channel);
				response.addValue(edgeId, channel, value.orElse(JsonNull.INSTANCE));
			}
		}
		return CompletableFuture.completedFuture(response);
	}

	/**
	 * Handles a SetGridConnScheduleRequest.
	 * 
	 * @param backendUser                the User
	 * @param messageId                  the JSON-RPC Message-ID
	 * @param setGridConnScheduleRequest the SetGridConnScheduleRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<GenericJsonrpcResponseSuccess> handleSetGridConnScheduleRequest(BackendUser backendUser,
			UUID messageId, SetGridConnScheduleRequest setGridConnScheduleRequest) throws OpenemsNamedException {
		String edgeId = setGridConnScheduleRequest.getEdgeId();
		User user = backendUser.getAsCommonUser(edgeId);
		user.assertRoleIsAtLeast(SetGridConnScheduleRequest.METHOD, Role.ADMIN);

		// wrap original request inside ComponentJsonApiRequest
		String componentId = "ctrlBalancingSchedule0"; // TODO find dynamic Component-ID of BalancingScheduleController
		ComponentJsonApiRequest request = new ComponentJsonApiRequest(componentId, setGridConnScheduleRequest);

		CompletableFuture<JsonrpcResponseSuccess> resultFuture = this.parent.edgeWebsocket.send(edgeId, user, request);

		// Wrap reply in GenericJsonrpcResponseSuccess
		CompletableFuture<GenericJsonrpcResponseSuccess> result = new CompletableFuture<GenericJsonrpcResponseSuccess>();
		resultFuture.whenComplete((r, ex) -> {
			if (ex != null) {
				result.completeExceptionally(ex);
			} else if (r != null) {
				result.complete(new GenericJsonrpcResponseSuccess(messageId, r.toJsonObject()));
			} else {
				result.completeExceptionally(new OpenemsNamedException(OpenemsError.JSONRPC_UNHANDLED_METHOD,
						SetGridConnScheduleRequest.METHOD));
			}
		});
		return result;
	}
}
