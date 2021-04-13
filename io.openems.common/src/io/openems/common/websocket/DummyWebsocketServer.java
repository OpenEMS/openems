package io.openems.common.websocket;

import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;

import io.openems.common.exceptions.NotImplementedException;

public class DummyWebsocketServer extends AbstractWebsocketServer<WsData> implements AutoCloseable {

	public static class Builder {
		private OnOpen onOpen = (ws, handshake) -> {
		};
		private OnRequest onRequest = (ws, request) -> {
			throw new NotImplementedException("On-Request handler is not implemented");
		};
		private OnNotification onNotification = (ws, notification) -> {
		};
		private OnError onError = (ws, ex) -> {
		};
		private OnClose onClose = (ws, code, reason, remote) -> {
		};

		private Builder() {
		}

		public DummyWebsocketServer.Builder onOpen(OnOpen onOpen) {
			this.onOpen = onOpen;
			return this;
		}

		public DummyWebsocketServer.Builder onRequest(OnRequest onRequest) {
			this.onRequest = onRequest;
			return this;
		}

		public DummyWebsocketServer.Builder onNotification(OnNotification onNotification) {
			this.onNotification = onNotification;
			return this;
		}

		public DummyWebsocketServer.Builder onError(OnError onError) {
			this.onError = onError;
			return this;
		}

		public DummyWebsocketServer.Builder onClose(OnClose onClose) {
			this.onClose = onClose;
			return this;
		}

		public DummyWebsocketServer build() {
			return new DummyWebsocketServer(this);
		}
	}

	/**
	 * Create a Config builder.
	 * 
	 * @return a {@link Builder}
	 */
	public static DummyWebsocketServer.Builder create() {
		return new Builder();
	}

	private final DummyWebsocketServer.Builder builder;

	private DummyWebsocketServer(DummyWebsocketServer.Builder builder) {
		super("DummyWebsocketServer", 0 /* auto-select port */, 1 /* pool size */, false);
		this.builder = builder;
	}

	@Override
	protected WsData createWsData() {
		return new DummyWsData();
	}

	@Override
	protected OnOpen getOnOpen() {
		return this.builder.onOpen;
	}

	public void withOnOpen(OnOpen onOpen) {
		this.builder.onOpen = onOpen;
	}

	@Override
	protected OnRequest getOnRequest() {
		return this.builder.onRequest;
	}

	public void withOnRequest(OnRequest onRequest) {
		this.builder.onRequest = onRequest;
	}

	@Override
	protected OnNotification getOnNotification() {
		return this.builder.onNotification;
	}

	public void withOnNotification(OnNotification onNotification) {
		this.builder.onNotification = onNotification;
	}

	@Override
	protected OnError getOnError() {
		return this.builder.onError;
	}

	public void withOnError(OnError onError) {
		this.builder.onError = onError;
	}

	@Override
	protected OnClose getOnClose() {
		return this.builder.onClose;
	}

	public void withOnClose(OnClose onClose) {
		this.builder.onClose = onClose;
	}

	@Override
	protected void logInfo(Logger log, String message) {
		log.info(message);
	}

	@Override
	protected void logWarn(Logger log, String message) {
		log.info(message);
	}

	/**
	 * Starts the {@link WebSocketServer} and waits.
	 * 
	 * @return the dynamically assigned Port.
	 * @throws InterruptedException on error
	 */
	public int startBlocking() throws InterruptedException {
		this.start();

		// block until Port is not anymore zero
		int port;
		do {
			Thread.sleep(500);
			port = this.getPort();
		} while (port == 0);
		return port;
	}

	@Override
	public void close() throws Exception {
		this.stop();
	}
}
