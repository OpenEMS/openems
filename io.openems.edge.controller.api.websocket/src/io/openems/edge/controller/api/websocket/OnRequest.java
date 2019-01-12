package io.openems.edge.controller.api.websocket;

import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonElement;

import io.openems.common.OpenemsConstants;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.ComponentJsonApiRequest;
import io.openems.common.jsonrpc.request.GetEdgeConfigRequest;
import io.openems.common.jsonrpc.request.QueryHistoricTimeseriesDataRequest;
import io.openems.common.jsonrpc.request.SubscribeChannelsRequest;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.jsonrpc.response.QueryHistoricTimeseriesDataResponse;
import io.openems.common.types.ChannelAddress;
import io.openems.common.websocket.SubscribedChannelsWorker;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.JsonApi;

public class OnRequest implements io.openems.common.websocket.OnRequest {

	private final Logger log = LoggerFactory.getLogger(OnRequest.class);
	private final WebsocketApi parent;

	public OnRequest(WebsocketApi parent) {
		this.parent = parent;
	}

	@Override
	public CompletableFuture<JsonrpcResponseSuccess> run(WebSocket ws, JsonrpcRequest request)
			throws OpenemsException, OpenemsNamedException {
		log.info("WebsocketApi. OnRequest: " + request);

		// get websocket attachment
		WsData wsData = ws.getAttachment();

		// is user authenticated?
		if (!wsData.isUserAuthenticated()) {
			throw OpenemsError.EDGE_USER_NOT_AUTHENTICATED.exception(
					"Session [" + wsData.getSessionToken() + "]. Ignoring request [" + request.getMethod() + "]");
		}

		switch (request.getMethod()) {

		case SubscribeChannelsRequest.METHOD:
			return this.handleSubscribeChannelsRequest(ws, SubscribeChannelsRequest.from(request));

		case QueryHistoricTimeseriesDataRequest.METHOD:
			return this.handleQueryHistoricDataRequest(QueryHistoricTimeseriesDataRequest.from(request));

		case UpdateComponentConfigRequest.METHOD:
			return this.handleUpdateComponentConfigRequest(UpdateComponentConfigRequest.from(request));

		case GetEdgeConfigRequest.METHOD:
			return this.handleGetEdgeConfigRequest(GetEdgeConfigRequest.from(request));

		case ComponentJsonApiRequest.METHOD:
			return this.handleComponentJsonApiRequest(ComponentJsonApiRequest.from(request));

		default:
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
		}
	}

	/**
	 * Handles a SubscribeChannelsRequest.
	 * 
	 * @param ws      the Websocket
	 * @param request the SubscribeChannelsRequest
	 * @throws ErrorException on error
	 * @return the JSON-RPC Success Response Future
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleSubscribeChannelsRequest(WebSocket ws,
			SubscribeChannelsRequest request) throws OpenemsNamedException {
		// activate SubscribedChannelsWorker
		WsData wsData = ws.getAttachment();
		SubscribedChannelsWorker worker = wsData.getSubscribedChannelsWorker();
		worker.setChannels(request.getChannels());

		// JSON-RPC response
		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}

	/**
	 * Handles a QueryHistoricDataRequest.
	 * 
	 * @param request the QueryHistoricDataRequest
	 * @throws OpenemsException on error
	 * @return the Future JSON-RPC Response
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleQueryHistoricDataRequest(
			QueryHistoricTimeseriesDataRequest request) throws OpenemsNamedException {
		TreeBasedTable<ZonedDateTime, ChannelAddress, JsonElement> data;
		data = this.parent.timedata.queryHistoricData( //
				null, /* igore Edge-ID */
				request.getFromDate(), //
				request.getToDate(), //
				request.getChannels());

		// JSON-RPC response
		return CompletableFuture.completedFuture(new QueryHistoricTimeseriesDataResponse(request.getId(), data));
	}

	/**
	 * Handles a UpdateComponentConfigRequest.
	 * 
	 * @param request the UpdateComponentConfigRequest
	 * @throws OpenemsException on error
	 * @return the Future JSON-RPC Response
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleUpdateComponentConfigRequest(
			UpdateComponentConfigRequest updateComponentConfigRequest) throws OpenemsNamedException {
		// wrap original request inside ComponentJsonApiRequest
		String componentId = OpenemsConstants.COMPONENT_MANAGER_ID;
		ComponentJsonApiRequest request = new ComponentJsonApiRequest(componentId, updateComponentConfigRequest);

		return this.handleComponentJsonApiRequest(request);
	}

	/**
	 * Handles a GetEdgeConfigRequest
	 * 
	 * @param request the GetEdgeConfigRequest
	 * @throws OpenemsException on error
	 * @return the Future JSON-RPC Response
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleGetEdgeConfigRequest(
			GetEdgeConfigRequest getEdgeConfigRequest) throws OpenemsNamedException {
		// wrap original request inside ComponentJsonApiRequest
		ComponentJsonApiRequest request = new ComponentJsonApiRequest(OpenemsConstants.COMPONENT_MANAGER_ID,
				getEdgeConfigRequest);

		return this.handleComponentJsonApiRequest(request);
	}

	/**
	 * Handles a ComponentJsonApiRequest
	 * 
	 * @param request the ComponentJsonApiRequest
	 * @throws OpenemsException on error
	 * @return the Future JSON-RPC Response
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
		JsonrpcResponse response = jsonApi.handleJsonrpcRequest(request.getPayload());

		// Wrap reply in EdgeRpcResponse
		return CompletableFuture
				.completedFuture(new GenericJsonrpcResponseSuccess(request.getId(), response.toJsonObject()));
	}

//	private void handleCompatibilty(JsonObject jMessage) {
//		/*
//		 * Authenticate
//		 */
//		Optional<com.google.gson.JsonObject> jAuthenticateOpt = JsonUtils.getAsOptionalJsonObject(jMessage,
//				"authenticate");
//		if (jAuthenticateOpt.isPresent()) {
//			// authenticate by username/password
//			try {
//				authenticate(jAuthenticateOpt.get(), websocket);
//			} catch (OpenemsNamedException e) {
//				WebSocketUtils.sendNotificationOrLogError(websocket,
//						new com.google.gson.JsonObject() /* empty message id */, LogBehaviour.WRITE_TO_LOG,
//						Notification.ERROR, e.getMessage());
//			}
//			return;
//		}
//
//		// get handler
//		UiEdgeWebsocketHandler handler;
//		try {
//			handler = this.parent.getHandlerOrCloseWebsocket(websocket);
//		} catch (OpenemsException e) {
//			WebSocketUtils.sendNotificationOrLogError(websocket,
//					new com.google.gson.JsonObject() /* empty message id */, LogBehaviour.WRITE_TO_LOG,
//					Notification.ERROR, "onMessage Error: " + e.getMessage());
//			return;
//		}
//
//		// get session Token from handler
//		String token = handler.getSessionToken();
//		if (!this.parent.sessionTokens.containsKey(token)) {
//			WebSocketUtils.sendNotificationOrLogError(websocket,
//					new com.google.gson.JsonObject() /* empty message id */, LogBehaviour.WRITE_TO_LOG,
//					Notification.ERROR, "Token [" + token + "] is not anymore valid.");
//			websocket.close();
//			return;
//		}
//
//		// From here authentication was successful
//
//		/*
//		 * Rest -> forward to websocket handler
//		 */
//		handler.onMessage(jMessage);
//	}
//
//	/**
//	 * Authenticates a user according to the "authenticate" message. Stores the User
//	 * if valid.
//	 *
//	 * @param jAuthenticateElement
//	 * @param handler
//	 * @throws OpenemsException
//	 */
//	private void authenticate(com.google.gson.JsonObject jAuthenticate, WebSocket websocket)
//			throws OpenemsNamedException {
//		if (jAuthenticate.has("mode")) {
//			String mode = JsonUtils.getAsString(jAuthenticate, "mode");
//			switch (mode) {
//			case "login":
//				try {
//					/*
//					 * Authenticate using password (and optionally username)
//					 */
//					String password = JsonUtils.getAsString(jAuthenticate, "password");
//					Optional<String> usernameOpt = JsonUtils.getAsOptionalString(jAuthenticate, "username");
//					Optional<User> userOpt;
//					if (usernameOpt.isPresent()) {
//						userOpt = this.parent.parent.userService.authenticate(usernameOpt.get(), password);
//					} else {
//						userOpt = this.parent.parent.userService.authenticate(password);
//					}
//
//					if (!userOpt.isPresent()) {
//						throw new OpenemsException("Authentication failed");
//					}
//					// authentication successful
//					User user = userOpt.get();
//					UiEdgeWebsocketHandler handler = this.parent.getHandlerOrCloseWebsocket(websocket);
//					this.parent.sessionTokens.put(handler.getSessionToken(), user);
//					this.parent.handleAuthenticationSuccessful(handler, user);
//
//				} catch (OpenemsException e) {
//					/*
//					 * send authentication failed reply
//					 */
//					com.google.gson.JsonObject jReply = DefaultMessages.uiLogoutReply();
//					WebSocketUtils.send(websocket, jReply);
//					this.parent.parent.logInfo(this.log, e.getMessage());
//					return;
//				}
//				break;
//			case "logout":
//				/*
//				 * Logout and close session
//				 */
//				String sessionToken = "none";
//				String username = "UNKNOWN";
//				try {
//					UiEdgeWebsocketHandler handler = this.parent.getHandlerOrCloseWebsocket(websocket);
//					Optional<User> thisUserOpt = handler.getUserOpt();
//					if (thisUserOpt.isPresent()) {
//						username = thisUserOpt.get().getName();
//						handler.unsetRole();
//					}
//					sessionToken = handler.getSessionToken();
//					this.parent.sessionTokens.remove(sessionToken);
//					this.parent.parent.logInfo(this.log,
//							"User [" + username + "] logged out. Invalidated token [" + sessionToken + "]");
//
//					// find and close all websockets for this user
//					if (thisUserOpt.isPresent()) {
//						User thisUser = thisUserOpt.get();
//						for (UiEdgeWebsocketHandler h : this.parent.handlers.values()) {
//							Optional<User> otherUserOpt = h.getUserOpt();
//							if (otherUserOpt.isPresent()) {
//								if (otherUserOpt.get().equals(thisUser)) {
//									com.google.gson.JsonObject jReply = DefaultMessages.uiLogoutReply();
//									h.send(jReply);
//									h.dispose();
//								}
//							}
//						}
//					}
//					com.google.gson.JsonObject jReply = DefaultMessages.uiLogoutReply();
//					WebSocketUtils.send(websocket, jReply);
//				} catch (OpenemsException e) {
//					WebSocketUtils.sendNotificationOrLogError(websocket,
//							new com.google.g	son.JsonObject() /* empty message id */, LogBehaviour.WRITE_TO_LOG,
//							Notification.ERROR, "Unable to close session [" + sessionToken + "]: " + e.getMessage());
//				}
//			}
//		}
//	}

}
