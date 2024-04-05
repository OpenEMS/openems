package io.openems.common.websocket;

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
		super("DummyWebsocketServer", 0 /* auto-select port */, 1 /* pool size */, DebugMode.OFF);
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
	public void close() throws Exception {
		this.stop();
	}
}
