package io.openems.common.websocket;

import java.net.Proxy;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.extensions.permessage_deflate.PerMessageDeflateExtension;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.jsonrpc.base.JsonrpcMessage;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;

/**
 * A Websocket Client implementation that automatically tries to reconnect a
 * closed connection.
 *
 * @param <T> the type of websocket attachments inheriting {@link WsData}
 */
public abstract class AbstractWebsocketClient<T extends WsData> extends AbstractWebsocket<T> {

	public static final Map<String, String> NO_HTTP_HEADERS = new HashMap<>();
	public static final Proxy NO_PROXY = null;
	public static final Draft DEFAULT_DRAFT = new Draft_6455(new PerMessageDeflateExtension());

	protected final WebSocketClient ws;

	private final Logger log = LoggerFactory.getLogger(AbstractWebsocketClient.class);
	private final URI serverUri;
	private final ClientReconnectorWorker reconnectorWorker;

	protected AbstractWebsocketClient(String name, URI serverUri) {
		this(name, serverUri, AbstractWebsocketClient.DEFAULT_DRAFT, AbstractWebsocketClient.NO_HTTP_HEADERS,
				AbstractWebsocketClient.NO_PROXY);
	}

	protected AbstractWebsocketClient(String name, URI serverUri, Map<String, String> httpHeaders) {
		this(name, serverUri, AbstractWebsocketClient.DEFAULT_DRAFT, httpHeaders, AbstractWebsocketClient.NO_PROXY);
	}

	protected AbstractWebsocketClient(String name, URI serverUri, Map<String, String> httpHeaders, Proxy proxy) {
		this(name, serverUri, AbstractWebsocketClient.DEFAULT_DRAFT, httpHeaders, proxy);
	}

	protected AbstractWebsocketClient(String name, URI serverUri, Draft draft, Map<String, String> httpHeaders,
			Proxy proxy) {
		super(name);
		this.serverUri = serverUri;
		this.ws = new WebSocketClient(serverUri, draft, httpHeaders) {

			private void logInfo(String message) {
				AbstractWebsocketClient.this.logInfo(AbstractWebsocketClient.this.log, message);
			}

			@Override
			public void onOpen(ServerHandshake handshake) {
				AbstractWebsocketClient.this.execute(new OnOpenHandler(//
						AbstractWebsocketClient.this.ws, handshake, //
						AbstractWebsocketClient.this.getOnOpen(), //
						AbstractWebsocketClient.this::logWarn, //
						AbstractWebsocketClient.this::handleInternalError));
			}

			@Override
			public void onMessage(String message) {
				AbstractWebsocketClient.this.execute(new OnMessageHandler(//
						AbstractWebsocketClient.this.ws, message, //
						AbstractWebsocketClient.this.getOnRequest(), //
						AbstractWebsocketClient.this.getOnNotification(), //
						AbstractWebsocketClient.this::sendMessage, //
						AbstractWebsocketClient.this::handleInternalError, //
						AbstractWebsocketClient.this::logWarn));
			}

			@Override
			public void onError(Exception ex) {
				AbstractWebsocketClient.this.execute(new OnErrorHandler(//
						AbstractWebsocketClient.this.ws, ex, //
						AbstractWebsocketClient.this.getOnError(), //
						AbstractWebsocketClient.this::handleInternalError));
			}

			@Override
			public void onClose(int code, String reason, boolean remote) {
				AbstractWebsocketClient.this.execute(new OnCloseHandler(//
						AbstractWebsocketClient.this.ws, code, reason, remote, //
						AbstractWebsocketClient.this.getOnClose(), //
						AbstractWebsocketClient.this::handleInternalError));

				this.logInfo(new StringBuilder() //
						.append("Websocket [").append(serverUri.toString()) //
						.append("] closed. Code [").append(code) //
						.append("] Reason [").append(reason).append("]") //
						.toString());
				AbstractWebsocketClient.this.reconnectorWorker.triggerNextRun();
			}
		};
		// https://github.com/TooTallNate/Java-WebSocket/wiki/Lost-connection-detection
		this.ws.setConnectionLostTimeout(100);

		// initialize WsData
		var wsData = AbstractWebsocketClient.this.createWsData(this.ws);
		this.ws.setAttachment(wsData);

		// Initialize reconnector
		this.reconnectorWorker = new ClientReconnectorWorker(this);

		if (proxy != null) {
			this.ws.setProxy(proxy);
		}
	}

	/**
	 * Starts the websocket client.
	 */
	@Override
	public void start() {
		this.logInfo(this.log, "Opening connection to websocket server [" + this.serverUri + "]");
		this.reconnectorWorker.activate(this.getName());
		this.reconnectorWorker.triggerNextRun();
	}

	/**
	 * Starts the {@link WebSocketClient}; waiting till it started.
	 *
	 * @throws InterruptedException on waiting error
	 */
	public void startBlocking() throws InterruptedException {
		this.logInfo(this.log, "Opening connection to websocket server [" + this.serverUri + "]");
		this.ws.connectBlocking();
		this.reconnectorWorker.activate(this.getName());
	}

	/**
	 * Stops the websocket client.
	 */
	@Override
	public void stop() {
		this.logInfo(this.log, "Closing connection to websocket server [" + this.serverUri + "]");
		// shutdown reconnector
		this.reconnectorWorker.deactivate();
		// close websocket
		this.ws.close(CloseFrame.NORMAL, "Closing connection [" + this.getName() + "]");
	}

	/**
	 * Sends a {@link JsonrpcMessage} to the {@link WebSocket}. Returns true if
	 * sending was successful, otherwise false. Also logs a warning in that case.
	 *
	 * @param message the {@link JsonrpcMessage}
	 * @return true if sending was successful
	 */
	public boolean sendMessage(JsonrpcMessage message) {
		return this.sendMessage(this.ws, message);
	}

	@Override
	protected OnInternalError getOnInternalError() {
		return (t, wsDataString) -> {
			this.logError(this.log, new StringBuilder() //
					.append("OnInternalError for ").append(wsDataString).append(". ") //
					.append(t.getClass()).append(": ").append(t.getMessage()) //
					.toString());
			t.printStackTrace();
		};
	}

	/**
	 * Sends a JSON-RPC Request and returns a future Response.
	 *
	 * @param request the JSON-RPC Request
	 * @return the future JSON-RPC Response
	 */
	public CompletableFuture<JsonrpcResponseSuccess> sendRequest(JsonrpcRequest request) {
		WsData wsData = this.ws.getAttachment();
		return wsData.send(request);
	}

}
