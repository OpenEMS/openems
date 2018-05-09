package io.openems.edge.controller.api.websocket;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.SecureRandomSingleton;
import io.openems.common.websocket.AbstractWebsocketServer;
import io.openems.common.websocket.DefaultMessages;
import io.openems.common.websocket.LogBehaviour;
import io.openems.common.websocket.Notification;
import io.openems.common.websocket.WebSocketUtils;
import io.openems.edge.api.user.User;
import io.openems.edge.api.user.UserService;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Api.Websocket", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class WebsocketApi extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(WebsocketApi.class);
	private final ApiWorker apiWorker = new ApiWorker();

	private AbstractWebsocketServer websocketApiServer = null;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE, target = "(!(service.factoryPid=Controller.Api.Websocket))")
	protected volatile List<OpenemsComponent> components = new CopyOnWriteArrayList<>();

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	protected volatile Timedata timedataService = null;

	@Reference
	protected ConfigurationAdmin configAdmin;

	@Reference
	protected UserService userService;

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled());

		if (!this.isEnabled()) {
			// abort if disabled
			return;
		}

		/*
		 * Start WebsocketApiServer
		 */
		this.websocketApiServer = new AbstractWebsocketServer(config.port()) {

			private final static int TOKEN_LENGTH = 130;

			/**
			 * Stores valid session tokens for authentication via Cookie (this maps to a
			 * browser window)
			 */
			private final Map<String, User> sessionTokens = new ConcurrentHashMap<>();

			/**
			 * Stores handlers per websocket (this maps to a browser tab). The handler lives
			 * while the websocket is connected. Independently of the login/logout state.
			 */
			private final Map<UUID, UiEdgeWebsocketHandler> handlers = new HashMap<>();

			/**
			 * Open event of websocket.
			 */
			@Override
			protected void _onOpen(WebSocket websocket, ClientHandshake handshake) {
				// generate UUID for this websocket (browser tab)
				UUID uuid = UUID.randomUUID();

				// get token from cookie or generate new token
				String token;
				Optional<String> cookieTokenOpt = getFieldFromHandshakeCookie(handshake, "token");
				if (cookieTokenOpt.isPresent()) {
					token = cookieTokenOpt.get();
				} else {
					// Generate token (source: http://stackoverflow.com/a/41156)
					SecureRandom sr = SecureRandomSingleton.getInstance();
					token = new BigInteger(TOKEN_LENGTH, sr).toString(32);
				}

				// create new Handler and store it
				UiEdgeWebsocketHandler handler = new UiEdgeWebsocketHandler(WebsocketApi.this, websocket, token, uuid);
				this.handlers.put(uuid, handler);
				websocket.setAttachment(uuid);

				// login using token from the cookie
				if (cookieTokenOpt.isPresent()) {
					User user = this.sessionTokens.get(token);
					if (user != null) {
						/*
						 * token from cookie is valid -> authentication successful
						 */
						// send reply and log
						try {
							this.handleAuthenticationSuccessful(handler, user);
							log.info("User [" + user.getName() + "] logged in by token");
							return;
						} catch (OpenemsException e) {
							WebSocketUtils.sendNotificationOrLogError(websocket,
									new JsonObject() /* empty message id */, LogBehaviour.WRITE_TO_LOG,
									Notification.ERROR, e.getMessage());
						}
					}
				}

				// if we are here, automatic authentication was not possible -> notify client
				WebSocketUtils.sendNotificationOrLogError(websocket, new JsonObject() /* empty message id */,
						LogBehaviour.WRITE_TO_LOG, Notification.EDGE_AUTHENTICATION_BY_TOKEN_FAILED,
						cookieTokenOpt.orElse(""));
			}

			@Override
			protected void _onMessage(WebSocket websocket, JsonObject jMessage) {
				/*
				 * Authenticate
				 */
				Optional<JsonObject> jAuthenticateOpt = JsonUtils.getAsOptionalJsonObject(jMessage, "authenticate");
				if (jAuthenticateOpt.isPresent()) {
					// authenticate by username/password
					try {
						authenticate(jAuthenticateOpt.get(), websocket);
					} catch (OpenemsException e) {
						WebSocketUtils.sendNotificationOrLogError(websocket, new JsonObject() /* empty message id */,
								LogBehaviour.WRITE_TO_LOG, Notification.ERROR, e.getMessage());
					}
					return;
				}

				// get handler
				UiEdgeWebsocketHandler handler;
				try {
					handler = getHandlerOrCloseWebsocket(websocket);
				} catch (OpenemsException e) {
					WebSocketUtils.sendNotificationOrLogError(websocket, new JsonObject() /* empty message id */,
							LogBehaviour.WRITE_TO_LOG, Notification.ERROR, "onMessage Error: " + e.getMessage());
					return;
				}

				// get session Token from handler
				String token = handler.getSessionToken();
				if (!this.sessionTokens.containsKey(token)) {
					WebSocketUtils.sendNotificationOrLogError(websocket, new JsonObject() /* empty message id */,
							LogBehaviour.WRITE_TO_LOG, Notification.ERROR,
							"Token [" + token + "] is not anymore valid.");
					websocket.close();
					return;
				}

				// From here authentication was successful

				/*
				 * Rest -> forward to websocket handler
				 */
				handler.onMessage(jMessage);
			}

			@Override
			protected void _onError(WebSocket websocket, Exception ex) {
				logWarn(log, "User [" + getUserName(websocket) + "] error: " + ex.getMessage());
			}

			@Override
			protected void _onClose(WebSocket websocket) {
				logInfo(log, "User [" + getUserName(websocket) + "] closed websocket connection");
				this.disposeHandler(websocket);
			}

			private void handleAuthenticationSuccessful(UiEdgeWebsocketHandler handler, User user)
					throws OpenemsException {
				// add user to handler
				handler.setUser(user);

				// Create Edges entry
				JsonObject jEdge = new JsonObject();
				jEdge.addProperty("id", 0);
				jEdge.addProperty("name", "fems0");
				jEdge.addProperty("comment", "FEMS");
				jEdge.addProperty("producttype", "");
				jEdge.addProperty("role", user.getRole().toString().toLowerCase());
				jEdge.addProperty("online", true);
				JsonArray jEdges = new JsonArray();
				jEdges.add(jEdge);

				// send reply
				JsonObject jReply = DefaultMessages.uiLoginSuccessfulReply(handler.getSessionToken(), jEdges);
				handler.send(jReply);
			}

			// TODO implement Log
			// public void sendLog(long timestamp, String level, String source, String
			// message) {
			// for (UiEdgeWebsocketHandler handler : this.handlers.values()) {
			// handler.sendLog(timestamp, level, source, message);
			// }
			// }

			private String getUserName(WebSocket websocket) {
				Optional<UiEdgeWebsocketHandler> handlerOpt = getHandlerOpt(websocket);
				if (handlerOpt.isPresent()) {
					UiEdgeWebsocketHandler handler = handlerOpt.get();
					if (handler.getUserOpt().isPresent()) {
						User user = handler.getUserOpt().get();
						return user.getName();
					}
				}
				return "UNKNOWN";
			}

			private Optional<UiEdgeWebsocketHandler> getHandlerOpt(WebSocket websocket) {
				UUID uuid = websocket.getAttachment();
				return Optional.ofNullable(this.handlers.get(uuid));
			}

			private UiEdgeWebsocketHandler getHandlerOrCloseWebsocket(WebSocket websocket) throws OpenemsException {
				Optional<UiEdgeWebsocketHandler> handlerOpt = getHandlerOpt(websocket);
				UUID uuid = websocket.getAttachment();
				UiEdgeWebsocketHandler handler = this.handlers.get(uuid);
				if (!handlerOpt.isPresent()) {
					// no handler! close websocket
					websocket.close();
					throw new OpenemsException("Websocket had no Handler. Closing websocket.");
				}
				return handler;
			}

			private void disposeHandler(WebSocket websocket) {
				UiEdgeWebsocketHandler handler;
				try {
					handler = getHandlerOrCloseWebsocket(websocket);
					UUID uuid = handler.getUuid();
					this.handlers.remove(uuid);
					handler.dispose();
				} catch (OpenemsException e) {
					log.warn("Unable to dispose Handler: " + e.getMessage());
				}
			}

			// TODO handle config update
			// public void onConfigUpdate() {
			// TODO
			// for (UiEdgeWebsocketHandler handler : this.handlers.values()) {
			// try {
			// Role role = handler.getUserOpt().get().getRole();
			// JsonObject j = DefaultMessages.configQueryReply(new JsonObject(),
			// Config.getInstance().getJson(ConfigFormat.OPENEMS_UI, role,
			// DEFAULT_CONFIG_LANGUAGE));
			// handler.send(j);
			// } catch (OpenemsException | NoSuchElementException e) {
			// log.warn(e.getMessage());
			// }
			// }
			// }

			/**
			 * Authenticates a user according to the "authenticate" message. Stores the User
			 * if valid.
			 *
			 * @param jAuthenticateElement
			 * @param handler
			 * @throws OpenemsException
			 */
			private void authenticate(JsonObject jAuthenticate, WebSocket websocket) throws OpenemsException {
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
								userOpt = userService.authenticate(usernameOpt.get(), password);
							} else {
								userOpt = userService.authenticate(password);
							}

							if (!userOpt.isPresent()) {
								throw new OpenemsException("Authentication failed");
							}
							// authentication successful
							User user = userOpt.get();
							UiEdgeWebsocketHandler handler = getHandlerOrCloseWebsocket(websocket);
							this.sessionTokens.put(handler.getSessionToken(), user);
							this.handleAuthenticationSuccessful(handler, user);

						} catch (OpenemsException e) {
							/*
							 * send authentication failed reply
							 */
							JsonObject jReply = DefaultMessages.uiLogoutReply();
							WebSocketUtils.send(websocket, jReply);
							log.info(e.getMessage());
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
							UiEdgeWebsocketHandler handler = this.getHandlerOrCloseWebsocket(websocket);
							Optional<User> thisUserOpt = handler.getUserOpt();
							if (thisUserOpt.isPresent()) {
								username = thisUserOpt.get().getName();
								handler.unsetUser();
							}
							sessionToken = handler.getSessionToken();
							this.sessionTokens.remove(sessionToken);
							log.info("User [" + username + "] logged out. Invalidated token [" + sessionToken + "]");

							// find and close all websockets for this user
							if (thisUserOpt.isPresent()) {
								User thisUser = thisUserOpt.get();
								for (UiEdgeWebsocketHandler h : this.handlers.values()) {
									if (h.getUserOpt().isPresent()) {
										User otherUser = h.getUserOpt().get();
										if (otherUser.equals(thisUser)) {
											JsonObject jReply = DefaultMessages.uiLogoutReply();
											h.send(jReply);
											h.dispose();
										}
									}
								}
							}
							JsonObject jReply = DefaultMessages.uiLogoutReply();
							WebSocketUtils.send(websocket, jReply);
						} catch (OpenemsException e) {
							WebSocketUtils.sendNotificationOrLogError(websocket,
									new JsonObject() /* empty message id */, LogBehaviour.WRITE_TO_LOG,
									Notification.ERROR,
									"Unable to close session [" + sessionToken + "]: " + e.getMessage());
						}
					}
				}
			}
		};
		this.websocketApiServer.start();
		this.logInfo(this.log, "Websocket-Api started on port [" + config.port() + "].");
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		if (this.websocketApiServer != null) {
			try {
				this.websocketApiServer.stop();
			} catch (IOException | InterruptedException e) {
				this.logError(this.log, "Error while closing websocket: " + e.getMessage());
			}
		}
	}

	@Override
	public void run() {
		this.apiWorker.run();
	}
}
