package io.openems.common.websocket;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;

public class DummyWebsocketServer extends AbstractWebsocketServer<WsData> implements AutoCloseable {

	public static class Builder {
		private OnOpen onOpen = OnOpen.NO_OP;
		private OnRequest onRequest = OnRequest.NO_OP;
		private OnNotification onNotification = OnNotification.NO_OP;
		private OnError onError = OnError.NO_OP;
		private OnClose onClose = OnClose.NO_OP;

		private Builder() {
		}

		/**
		 * Sets the {@link OnOpen} callback.
		 *
		 * @param onOpen the callback
		 * @return the {@link Builder}
		 */
		public DummyWebsocketServer.Builder onOpen(OnOpen onOpen) {
			this.onOpen = onOpen;
			return this;
		}

		/**
		 * Sets the {@link OnRequest} callback.
		 *
		 * @param onRequest the callback
		 * @return the {@link Builder}
		 */
		public DummyWebsocketServer.Builder onRequest(OnRequest onRequest) {
			this.onRequest = onRequest;
			return this;
		}

		/**
		 * Sets the {@link OnNotification} callback.
		 *
		 * @param onNotification the callback
		 * @return the {@link Builder}
		 */
		public DummyWebsocketServer.Builder onNotification(OnNotification onNotification) {
			this.onNotification = onNotification;
			return this;
		}

		/**
		 * Sets the {@link OnError} callback.
		 *
		 * @param onError the callback
		 * @return the {@link Builder}
		 */
		public DummyWebsocketServer.Builder onError(OnError onError) {
			this.onError = onError;
			return this;
		}

		/**
		 * Sets the {@link OnClose} callback.
		 *
		 * @param onClose the callback
		 * @return the {@link Builder}
		 */
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
		super("DummyWebsocketServer", 0 /* auto-select port */, 1 /* pool size */);
		this.builder = builder;
	}

	@Override
	protected WsData createWsData(WebSocket ws) {
		return new WsData(ws);
	}

	@Override
	protected OnOpen getOnOpen() {
		return this.builder.onOpen;
	}

	@Override
	protected OnRequest getOnRequest() {
		return this.builder.onRequest;
	}

	@Override
	protected OnNotification getOnNotification() {
		return this.builder.onNotification;
	}

	@Override
	protected OnError getOnError() {
		return this.builder.onError;
	}

	@Override
	protected OnClose getOnClose() {
		return this.builder.onClose;
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

	@Override
	public void close() {
		this.stop();
	}
}
