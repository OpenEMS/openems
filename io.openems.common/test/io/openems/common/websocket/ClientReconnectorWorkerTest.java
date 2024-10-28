package io.openems.common.websocket;

import java.net.URI;

import org.java_websocket.WebSocket;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;

public class ClientReconnectorWorkerTest {

	private static class MyWebsocketClient extends AbstractWebsocketClient<WsData> {

		public MyWebsocketClient(String name, URI serverUri) {
			super(name, serverUri);
		}

		@Override
		protected WsData createWsData(WebSocket ws) {
			return new WsData(ws);
		}

		@Override
		protected OnOpen getOnOpen() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected OnRequest getOnRequest() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected OnNotification getOnNotification() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected OnError getOnError() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected OnClose getOnClose() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected void execute(Runnable command) {
			// TODO Auto-generated method stub
		}

		@Override
		protected void logInfo(Logger log, String message) {
			// TODO Auto-generated method stub

		}

		@Override
		protected void logWarn(Logger log, String message) {
			// TODO Auto-generated method stub

		}

		@Override
		protected void logError(Logger log, String message) {
			// TODO Auto-generated method stub

		}

	}

	@Ignore
	@Test
	public void testResetWebSocketClient() throws Exception {
		final var websocketClient = new MyWebsocketClient("name", URI.create("ws://localhost"));
		ClientReconnectorWorker.resetWebSocketClient(websocketClient.ws, websocketClient::createWsData);
	}

}
