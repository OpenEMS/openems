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
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import io.openems.backend.metadata.api.Edge;
import io.openems.common.accesscontrol.RoleId;
import io.openems.common.types.ChannelAddress;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.backend.common.jsonrpc.request.GetEdgesChannelsValuesRequest;
import io.openems.backend.common.jsonrpc.request.GetEdgesStatusRequest;
import io.openems.backend.common.jsonrpc.response.GetEdgesChannelsValuesResponse;
import io.openems.backend.common.jsonrpc.response.GetEdgesStatusResponse;
import io.openems.backend.common.jsonrpc.response.GetEdgesStatusResponse.EdgeInfo;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcMessage;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.ComponentJsonApiRequest;
import io.openems.common.jsonrpc.request.SetGridConnScheduleRequest;
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

			RoleId roleId = this.authenticate(request);

			// remove leading '/'
			List<String> targets = Arrays.asList(target.substring(1).split("/"));

			if (targets.isEmpty()) {
				throw new OpenemsException("Missing arguments to handle request");
			}

			String thisTarget = targets.get(0);
			switch (thisTarget) {
			case "jsonrpc":
				this.handleJsonRpc(baseRequest, request, response, roleId);
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
	 * @param baseRequest the EdgeRpcRequest
	 * @param roleId
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
		CompletableFuture<? extends JsonrpcResponseSuccess> responseFuture = this.handleJsonRpcRequest(request, roleId);

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
	 * @param request the EdgeRpcRequest
	 * @param roleId
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsException on error
	 */
	private CompletableFuture<? extends JsonrpcResponseSuccess> handleJsonRpcRequest(JsonrpcRequest request, RoleId roleId) throws OpenemsException, OpenemsNamedException {
		switch (request.getMethod()) {

		case GetEdgesStatusRequest.METHOD:
			return this.handleGetStatusOfEdgesRequest(request.getId(), GetEdgesStatusRequest.from(request), roleId);

		case GetEdgesChannelsValuesRequest.METHOD:
			return this.handleGetChannelsValuesRequest(request.getId(),
					GetEdgesChannelsValuesRequest.from(request), roleId);

		case SetGridConnScheduleRequest.METHOD:
			return this.handleSetGridConnScheduleRequest(request.getId(),
					SetGridConnScheduleRequest.from(request), roleId);

		default:
			this.parent.logWarn(this.log, "Unhandled Request: " + request);
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
		}
	}

	/**
	 * Handles a GetStatusOfEdgesRequest.
	 * 
	 * @param messageId the JSON-RPC Message-ID
	 * @param request   the GetStatusOfEdgesRequest
	 * @param roleId the role io
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<GetEdgesStatusResponse> handleGetStatusOfEdgesRequest(UUID messageId,
																					GetEdgesStatusRequest request, RoleId roleId) throws OpenemsNamedException {
		Map<String, EdgeInfo> result = new HashMap<>();

		for (String edgeId : this.parent.accessControl.getEdgeIds(roleId)) {
			Optional<Edge> edgeOpt = this.parent.metadata.getEdge(edgeId);
			edgeOpt.ifPresent(edge -> {
				EdgeInfo info = new EdgeInfo(edge.isOnline());
				result.put(edge.getId(), info);
			});
		}

		return CompletableFuture.completedFuture(new GetEdgesStatusResponse(messageId, result));
	}

	/**
	 * Handles a GetChannelsValuesRequest.
	 * 
	 * @param messageId the JSON-RPC Message-ID
	 * @param request   the GetChannelsValuesRequest
	 * @param roleId
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<GetEdgesChannelsValuesResponse> handleGetChannelsValuesRequest(UUID messageId, GetEdgesChannelsValuesRequest request, RoleId roleId) throws OpenemsNamedException {
		GetEdgesChannelsValuesResponse response = new GetEdgesChannelsValuesResponse(messageId);
		for (String edgeId : request.getEdgeIds()) {
			Set<ChannelAddress> permittedChannels = this.parent.accessControl.intersectAccessPermission(roleId, edgeId, request.getChannels());
			for (ChannelAddress channel : permittedChannels) {
				Optional<JsonElement> value = this.parent.timeData.getChannelValue(edgeId, channel);
				response.addValue(edgeId, channel, value.orElse(JsonNull.INSTANCE));
			}
		}
		return CompletableFuture.completedFuture(response);
	}

	/**
	 * Handles a SetGridConnScheduleRequest.
	 * 
	 * @param messageId                  the JSON-RPC Message-ID
	 * @param setGridConnScheduleRequest the SetGridConnScheduleRequest
	 * @param roleId the role id
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<GenericJsonrpcResponseSuccess> handleSetGridConnScheduleRequest(UUID messageId, SetGridConnScheduleRequest setGridConnScheduleRequest, RoleId roleId) throws OpenemsNamedException {
		String edgeId = setGridConnScheduleRequest.getEdgeId();
		this.parent.accessControl.assertExecutePermission(roleId, edgeId, SetGridConnScheduleRequest.METHOD);

		// wrap original request inside ComponentJsonApiRequest
		String componentId = "ctrlBalancingSchedule0"; // TODO find dynamic Component-ID of BalancingScheduleController
		ComponentJsonApiRequest request = new ComponentJsonApiRequest(componentId, setGridConnScheduleRequest);

		CompletableFuture<JsonrpcResponseSuccess> resultFuture = this.parent.edgeWebsocket.send(edgeId, request);

		// Wrap reply in GenericJsonrpcResponseSuccess
		CompletableFuture<GenericJsonrpcResponseSuccess> result = new CompletableFuture<>();
		resultFuture.thenAccept(r -> {
			result.complete(new GenericJsonrpcResponseSuccess(messageId, r.toJsonObject()));
		});
		return result;
	}
}
