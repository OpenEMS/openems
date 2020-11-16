package io.openems.common.websocket;

import java.net.Proxy;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcMessage;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.StringUtils;

/**
 * A Websocket Client implementation that automatically tries to reconnect a
 * closed connection.
 *
 * @param <T>
 */
public abstract class AbstractWebsocketClient<T extends WsData> extends AbstractWebsocket<T> {

	public final static Map<String, String> NO_HTTP_HEADERS = new HashMap<>();
	public final static Proxy NO_PROXY = null;
	public final static Draft DEFAULT_DRAFT = new Draft_6455();

	protected final WebSocketClient ws;

	private final Logger log = LoggerFactory.getLogger(AbstractWebsocketClient.class);
	private final URI serverUri;
	private final ClientReconnectorWorker reconnectorWorker;

	protected AbstractWebsocketClient(String name, URI serverUri) {
		this(name, serverUri, DEFAULT_DRAFT, NO_HTTP_HEADERS, NO_PROXY);
	}

	protected AbstractWebsocketClient(String name, URI serverUri, Map<String, String> httpHeaders) {
		this(name, serverUri, DEFAULT_DRAFT, httpHeaders, NO_PROXY);
	}

	protected AbstractWebsocketClient(String name, URI serverUri, Map<String, String> httpHeaders, Proxy proxy) {
		this(name, serverUri, DEFAULT_DRAFT, httpHeaders, proxy);
	}

	protected AbstractWebsocketClient(String name, URI serverUri, Draft draft, Map<String, String> httpHeaders,
			Proxy proxy) {
		super(name);
		this.serverUri = serverUri;
		this.ws = new WebSocketClient(serverUri, draft, httpHeaders) {

			@Override
			public void onOpen(ServerHandshake handshake) {
				JsonObject jHandshake = WebsocketUtils.handshakeToJsonObject(handshake);
				CompletableFuture.runAsync(
						new OnOpenHandler(AbstractWebsocketClient.this, AbstractWebsocketClient.this.ws, jHandshake));
			}

			@Override
			public void onMessage(String stringMessage) {
				try {
					JsonrpcMessage message = JsonrpcMessage.from(stringMessage);
					if (message instanceof JsonrpcRequest) {
						CompletableFuture.runAsync(new OnRequestHandler(AbstractWebsocketClient.this, ws,
								(JsonrpcRequest) message, (response) -> {
									AbstractWebsocketClient.this.sendMessage(response);
								}));

					} else if (message instanceof JsonrpcResponse) {
						CompletableFuture.runAsync(
								new OnResponseHandler(AbstractWebsocketClient.this, ws, (JsonrpcResponse) message));

					} else if (message instanceof JsonrpcNotification) {
						CompletableFuture.runAsync(new OnNotificationHandler(AbstractWebsocketClient.this, ws,
								(JsonrpcNotification) message));

					}
				} catch (OpenemsNamedException e) {
					AbstractWebsocketClient.this.handleInternalErrorAsync(e);
				}
			}

			@Override
			public void onError(Exception ex) {
				CompletableFuture.runAsync(new OnErrorHandler(AbstractWebsocketClient.this, ws, ex));
			}

			@Override
			public void onClose(int code, String reason, boolean remote) {
				CompletableFuture.runAsync(new OnCloseHandler(AbstractWebsocketClient.this, ws, code, reason, remote));

				AbstractWebsocketClient.this.log.info(
						"Websocket [" + serverUri.toString() + "] closed. Code [" + code + "] Reason [" + reason + "]");
				AbstractWebsocketClient.this.reconnectorWorker.triggerNextRun();
			}
		};
		// Disable lost connection detection
		// https://github.com/TooTallNate/Java-WebSocket/wiki/Lost-connection-detection
		this.ws.setConnectionLostTimeout(0);

		// initialize WsData
		T wsData = AbstractWebsocketClient.this.createWsData();
		wsData.setWebsocket(ws);
		this.ws.setAttachment(wsData);

		// Initialize reconnector
		this.reconnectorWorker = new ClientReconnectorWorker(this);
		this.reconnectorWorker.activate(this.getName());

		if (proxy != null) {
			this.ws.setProxy(proxy);
		}
	}

	/**
	 * Starts the websocket client
	 */
	public void start() {
		this.log.info("Opening connection [" + this.getName() + "] to websocket server [" + this.serverUri + "]");
		if(this.serverUri.toString().indexOf("wss") == 0) {
			System.err.println("Setting SSL");
			 try {
				SSLContext sslContext = SSLContext.getDefault();
				SSLSocketFactory sslFactory = sslContext.getSocketFactory();
				ws.setSocketFactory(sslFactory);
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		this.ws.connect();
	}

	/**
	 * Starts the websocket client
	 * 
	 * @throws InterruptedException
	 */
	public void startBlocking() throws InterruptedException {
		this.log.info("Opening connection [" + this.getName() + "] websocket server [" + this.serverUri + "]");
		this.ws.connectBlocking();
	}

	/**
	 * Stops the websocket client
	 */
	public void stop() {
		this.log.info("Closing connection [" + this.getName() + "] to websocket server [" + this.serverUri + "]");
		// shutdown reconnector
		this.reconnectorWorker.deactivate();
		// close websocket
		this.ws.close(CloseFrame.NORMAL, "Closing connection [" + this.getName() + "]");
	}

	protected OnInternalError getOnInternalError() {
		return (ex, wsDataString) -> {
			this.log.warn("OnInternalError for " + wsDataString + ". " + ex.getClass() + ": " + ex.getMessage());
			ex.printStackTrace();
		};
	};

	public void sendMessageOrError(JsonrpcMessage message) throws OpenemsException {
		try {
			this.ws.send(message.toString());
		} catch (Exception e) {
			if (e instanceof WebsocketNotConnectedException) {
				AbstractWebsocketClient.this.reconnectorWorker.triggerNextRun();
			}
			throw new OpenemsException("Unable to send JSON-RPC message. " + e.getClass().getSimpleName() + ": "
					+ StringUtils.toShortString(message.toString(), 100));
		}
	}

	/**
	 * Sends a JSON-RPC message. Returns true if sending was successful, otherwise
	 * false. Also logs a warning in that case.
	 * 
	 * @param message
	 * @return
	 */
	public boolean sendMessage(JsonrpcMessage message) {
		try {
			this.sendMessageOrError(message);
			return true;
		} catch (OpenemsException e) {
			log.warn(e.getMessage());
			return false;
		}
	}

	/**
	 * Sends a JSON-RPC Request and returns a future Response.
	 * 
	 * @param request the JSON-RPC Request
	 * @return the future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	public CompletableFuture<JsonrpcResponseSuccess> sendRequest(JsonrpcRequest request) throws OpenemsNamedException {
		WsData wsData = this.ws.getAttachment();
		return wsData.send(request);
	}

}
