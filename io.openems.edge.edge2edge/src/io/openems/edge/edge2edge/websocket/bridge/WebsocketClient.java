package io.openems.edge.edge2edge.websocket.bridge;

import java.net.Proxy;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.common.types.ChannelAddress;
import io.openems.common.types.EdgeConfig;
import io.openems.common.websocket.AbstractWebsocketClient;
import io.openems.common.websocket.ClientReconnectorWorker;
import io.openems.common.websocket.OnClose;
import io.openems.common.websocket.WsData;

public class WebsocketClient extends AbstractWebsocketClient<WsData> {

	private final Logger log = LoggerFactory.getLogger(WebsocketClient.class);

	private final OnOpen onOpen;
	private final OnNotification onNotification;
	private final OnError onError;
	private final OnClose onClose;
	private final OnRequest onRequest;

	private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

	public WebsocketClient(String name, URI serverUri, Map<String, String> httpHeaders, Proxy proxy,
			Consumer<ConnectionState> onStateChange, //
			Consumer<Map<ChannelAddress, JsonElement>> onCurrentData, //
			Consumer<EdgeConfig> onEdgeConfig, //
			Runnable onChannelChange //
	) {
		super(name, serverUri, DEFAULT_DRAFT, httpHeaders, proxy, null /* onConnectedChange */,
				new ClientReconnectorWorker.Config(5, 10, 5, 5 * 1000));
		this.onOpen = new OnOpen(onStateChange);
		this.onNotification = new OnNotification(onCurrentData, onEdgeConfig, onChannelChange);
		this.onRequest = new OnRequest();
		this.onError = new OnError();
		this.onClose = (ws, code, reason, remote) -> {
			onStateChange.accept(ConnectionState.NOT_CONNECTED);
			this.log.error("Disconnected from slave [" + serverUri.toString() //
					+ (proxy != AbstractWebsocketClient.NO_PROXY ? " via Proxy" : "") + "]");
		};
	}

	@Override
	public void stop() {
		super.stop();
		this.executor.close();
	}

	@Override
	public OnOpen getOnOpen() {
		return this.onOpen;
	}

	@Override
	public OnRequest getOnRequest() {
		return this.onRequest;
	}

	@Override
	public OnNotification getOnNotification() {
		return this.onNotification;
	}

	@Override
	public OnError getOnError() {
		return this.onError;
	}

	@Override
	public OnClose getOnClose() {
		return this.onClose;
	}

	@Override
	protected WsData createWsData(WebSocket es) {
		return new WsData(ws);
	}

	@Override
	protected void logInfo(Logger log, String message) {
		log.info(message);
	}

	@Override
	protected void logWarn(Logger log, String message) {
		log.warn(message);
	}

	@Override
	protected void logError(Logger log, String message) {
		log.error(message);
	}

	public boolean isConnected() {
		return this.ws.isOpen();
	}

	@Override
	protected void execute(Runnable command) {
		this.executor.execute(command);
	}

}
