package io.openems.edge.controller.api.backend;

import java.net.Proxy;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.websocket.AbstractWebsocketClient;
import io.openems.common.websocket.ClientReconnectorWorker;
import io.openems.common.websocket.CommonHttpHeader;
import io.openems.common.websocket.OnClose;
import io.openems.common.websocket.WebsocketUtils;
import io.openems.common.websocket.WsData;
import io.openems.edge.common.channel.ChannelUtils;
import io.openems.edge.controller.api.backend.api.ControllerApiBackend;

public class WebsocketClient extends AbstractWebsocketClient<WsData> {

	private final Logger log = LoggerFactory.getLogger(WebsocketClient.class);

	private final ControllerApiBackendImpl parent;
	private final OnOpen onOpen;
	private final OnNotification onNotification;
	private final OnError onError;
	private final OnClose onClose;

	protected WebsocketClient(ControllerApiBackendImpl parent, String name, URI serverUri,
			Map<String, String> httpHeaders, Proxy proxy) {
		super(name, serverUri, AbstractWebsocketClient.DEFAULT_DRAFT, httpHeaders, proxy, null,
				ClientReconnectorWorker.DEFAULT_CONFIG.withEventHandler(e -> onReconnectEvent(parent, e)));
		this.parent = parent;
		this.onOpen = new OnOpen(parent);
		this.onNotification = new OnNotification(parent);
		this.onError = new OnError(parent);
		this.onClose = (ws, code, reason, remote) -> {
			final var serverUriStr = serverUri.toString();
			final var proxyStr = (proxy != AbstractWebsocketClient.NO_PROXY) ? " via Proxy" : "";

			if (code == CloseFrame.NEVER_CONNECTED || code == CloseFrame.PROTOCOL_ERROR) {
				this.log.error("Failed to connect to OpenEMS Backend [{}{}]: {}", //
						serverUriStr, proxyStr, reason);
			} else {
				this.log.error("Disconnected from OpenEMS Backend [{}{}]: {}", //
						serverUriStr, proxyStr, reason);
			}

			this.parent.getUnableToSendChannel().setNextValue(true);
		};
	}

	private static void onReconnectEvent(ControllerApiBackendImpl parent,
			ClientReconnectorWorker.WebsocketReconnectorEvent event) {
		if (event == ClientReconnectorWorker.WebsocketReconnectorEvent.CLOSE_FAILED) {
			ChannelUtils.setValue(parent, ControllerApiBackend.ChannelId.CONNECTION_CLOSE_FAILURE, true);
		}
	}

	@Override
	protected void onWebsocketHandshakeSent(ClientHandshake request) {
		final String systemId = WebsocketUtils //
				.getAsOptionalString(request, CommonHttpHeader.INSTANCE_ID) //
				.orElse("N/A");
		this.log.info("Initiating handshake with OpenEMS Backend [InstanceID={}]", systemId);
	}

	@Override
	public OnOpen getOnOpen() {
		return this.onOpen;
	}

	@Override
	public BackendOnRequest getOnRequest() {
		return this.parent.requestHandler;
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
	protected WsData createWsData(WebSocket ws) {
		return new WsData(ws);
	}

	@Override
	protected void logInfo(Logger log, String message) {
		this.parent.logInfo(log, message);
	}

	@Override
	protected void logWarn(Logger log, String message) {
		this.parent.logWarn(log, message);
	}

	@Override
	protected void logError(Logger log, String message) {
		this.parent.logError(log, message);
	}

	public boolean isConnected() {
		return this.ws.isOpen();
	}

	@Override
	protected void execute(Runnable command) {
		this.parent.execute(command);
	}

	/**
	 * Schedules a command using the {@link ScheduledExecutorService}.
	 *
	 * @param command      a {@link Runnable}
	 * @param initialDelay the initial delay
	 * @param delay        the delay
	 * @param unit         the {@link TimeUnit}
	 * @return a {@link ScheduledFuture}, or null if Executor is shutting down
	 */
	protected ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay,
			TimeUnit unit) {
		return this.parent.scheduleWithFixedDelay(command, initialDelay, delay, unit);
	}
}
