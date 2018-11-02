package io.openems.edge.controller.api.websocket;

import java.util.Optional;

import org.java_websocket.WebSocket;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO deprecate GSON; replace with org.json
import com.google.gson.JsonParser;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;
import io.openems.common.websocket.AbstractOnMessage;
import io.openems.common.websocket.DefaultMessages;
import io.openems.common.websocket.JsonrpcRequest;
import io.openems.common.websocket.JsonrpcResponse;
import io.openems.common.websocket.LogBehaviour;
import io.openems.common.websocket.Notification;
import io.openems.common.websocket.WebSocketUtils;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.user.User;

public class OnMessage extends AbstractOnMessage {

	private final Logger log = LoggerFactory.getLogger(OnMessage.class);
	private final WebsocketApiServer parent;

	public OnMessage(WebsocketApiServer parent, WebSocket websocket, String message) {
		super(websocket, message);
		this.parent = parent;
	}

	protected void run(WebSocket websocket, String message) {
		try {
			JsonrpcResponse response = null;
			JsonrpcRequest request = JsonrpcRequest.from(message);
			try {
				// TODO: first check if Websocket connection was authenticated. This is currently happening only in handleCompatibilty.
				
				/*
				 * Handle JsonrpcRequest
				 */
				switch (request.getMethod()) {
				case ComponentJsonApi.METHOD:
					ComponentJsonApi componentJsonApi = ComponentJsonApi.from(request);
					response = this.handleComponentJsonApi(componentJsonApi);
					break;
				}

				/*
				 * Reply with JsonrpcResponse
				 */
				if (response != null) {
					WebSocketUtils.send(this.websocket, response.toString());
				}

			} catch (OpenemsException e) {
				log.error("Unable to handle message: " + e.getMessage());
			}

		} catch (JSONException e) {
			/*
			 * Handle Compatibility for pre-JSONRPC-Requests
			 */
			this.handleCompatibilty((new JsonParser()).parse(message).getAsJsonObject());
		}
	}

	/**
	 * Handles a JSON-RPC call with method "componentJsonApi"
	 * 
	 * @param jMessage
	 * @throws OpenemsException
	 */
	private JsonrpcResponse handleComponentJsonApi(ComponentJsonApi request) throws OpenemsException {
		// get Component
		String componentId = request.getComponentId();
		OpenemsComponent component = null;
		for (OpenemsComponent c : this.parent.parent.getComponents()) {
			if (c.id().equals(componentId)) {
				component = c;
				break;
			}
		}
		if (component == null) {
			throw new OpenemsException("Unable to find Component [" + componentId + "]");
		}

		if (!(component instanceof JsonApi)) {
			throw new OpenemsException("Component [" + componentId + "] is no JsonApi");
		}

		// call JsonApi
		JsonApi jsonApi = (JsonApi) component;
		return jsonApi.handleJsonrpcRequest(request.getPayload());
	}

	private void handleCompatibilty(com.google.gson.JsonObject jMessage) {
		/*
		 * Authenticate
		 */
		Optional<com.google.gson.JsonObject> jAuthenticateOpt = JsonUtils.getAsOptionalJsonObject(jMessage,
				"authenticate");
		if (jAuthenticateOpt.isPresent()) {
			// authenticate by username/password
			try {
				authenticate(jAuthenticateOpt.get(), websocket);
			} catch (OpenemsException e) {
				WebSocketUtils.sendNotificationOrLogError(websocket,
						new com.google.gson.JsonObject() /* empty message id */, LogBehaviour.WRITE_TO_LOG,
						Notification.ERROR, e.getMessage());
			}
			return;
		}

		// get handler
		UiEdgeWebsocketHandler handler;
		try {
			handler = this.parent.getHandlerOrCloseWebsocket(websocket);
		} catch (OpenemsException e) {
			WebSocketUtils.sendNotificationOrLogError(websocket,
					new com.google.gson.JsonObject() /* empty message id */, LogBehaviour.WRITE_TO_LOG,
					Notification.ERROR, "onMessage Error: " + e.getMessage());
			return;
		}

		// get session Token from handler
		String token = handler.getSessionToken();
		if (!this.parent.sessionTokens.containsKey(token)) {
			WebSocketUtils.sendNotificationOrLogError(websocket,
					new com.google.gson.JsonObject() /* empty message id */, LogBehaviour.WRITE_TO_LOG,
					Notification.ERROR, "Token [" + token + "] is not anymore valid.");
			websocket.close();
			return;
		}

		// From here authentication was successful

		/*
		 * Rest -> forward to websocket handler
		 */
		handler.onMessage(jMessage);
	}

	/**
	 * Authenticates a user according to the "authenticate" message. Stores the User
	 * if valid.
	 *
	 * @param jAuthenticateElement
	 * @param handler
	 * @throws OpenemsException
	 */
	private void authenticate(com.google.gson.JsonObject jAuthenticate, WebSocket websocket) throws OpenemsException {
		if (jAuthenticate.has("mode")) {
			String mode = JsonUtils.getAsString(jAuthenticate, "mode");
			switch (mode) {
			case "login":
				try {
					/*
					 * Authenticate using password (and optionally username)
					 */
					String password = JsonUtils.getAsString(jAuthenticate, "password");
					Optional<String> usernameOpt = JsonUtils.getAsOptionalString(jAuthenticate, "username");
					Optional<User> userOpt;
					if (usernameOpt.isPresent()) {
						userOpt = this.parent.parent.userService.authenticate(usernameOpt.get(), password);
					} else {
						userOpt = this.parent.parent.userService.authenticate(password);
					}

					if (!userOpt.isPresent()) {
						throw new OpenemsException("Authentication failed");
					}
					// authentication successful
					User user = userOpt.get();
					UiEdgeWebsocketHandler handler = this.parent.getHandlerOrCloseWebsocket(websocket);
					this.parent.sessionTokens.put(handler.getSessionToken(), user);
					this.parent.handleAuthenticationSuccessful(handler, user);

				} catch (OpenemsException e) {
					/*
					 * send authentication failed reply
					 */
					com.google.gson.JsonObject jReply = DefaultMessages.uiLogoutReply();
					WebSocketUtils.send(websocket, jReply);
					this.parent.parent.logInfo(this.log, e.getMessage());
					return;
				}
				break;
			case "logout":
				/*
				 * Logout and close session
				 */
				String sessionToken = "none";
				String username = "UNKNOWN";
				try {
					UiEdgeWebsocketHandler handler = this.parent.getHandlerOrCloseWebsocket(websocket);
					Optional<User> thisUserOpt = handler.getUserOpt();
					if (thisUserOpt.isPresent()) {
						username = thisUserOpt.get().getName();
						handler.unsetRole();
					}
					sessionToken = handler.getSessionToken();
					this.parent.sessionTokens.remove(sessionToken);
					this.parent.parent.logInfo(this.log,
							"User [" + username + "] logged out. Invalidated token [" + sessionToken + "]");

					// find and close all websockets for this user
					if (thisUserOpt.isPresent()) {
						User thisUser = thisUserOpt.get();
						for (UiEdgeWebsocketHandler h : this.parent.handlers.values()) {
							Optional<User> otherUserOpt = h.getUserOpt();
							if (otherUserOpt.isPresent()) {
								if (otherUserOpt.get().equals(thisUser)) {
									com.google.gson.JsonObject jReply = DefaultMessages.uiLogoutReply();
									h.send(jReply);
									h.dispose();
								}
							}
						}
					}
					com.google.gson.JsonObject jReply = DefaultMessages.uiLogoutReply();
					WebSocketUtils.send(websocket, jReply);
				} catch (OpenemsException e) {
					WebSocketUtils.sendNotificationOrLogError(websocket,
							new com.google.gson.JsonObject() /* empty message id */, LogBehaviour.WRITE_TO_LOG,
							Notification.ERROR, "Unable to close session [" + sessionToken + "]: " + e.getMessage());
				}
			}
		}
	}
}
