package io.openems.common.websocket;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.extensions.permessage_deflate.PerMessageDeflateExtension;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.WebSocketServer;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.jsonrpc.base.GenericJsonrpcNotification;
import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcMessage;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.websocket.ClientReconnectorWorker.WebsocketReconnectorEvent;

public class WebsocketTest {

	private static final String HEADER = "X-Test";

	/**
	 * Server that echoes the "value" param of requests and rejects handshakes
	 * without {@link #HEADER}.
	 */
	private static class TestServer extends AbstractWebsocketServer<WsData> implements AutoCloseable {

		private final LinkedBlockingQueue<String> headers = new LinkedBlockingQueue<>();

		TestServer(int port) {
			super("TestServer", port, 2);
			this.start();
		}

		@Override
		protected WsData onHandshake(WebSocket ws, Handshake handshake) throws HandshakeRejectedException {
			var header = handshake.getHeader(HEADER.toLowerCase());
			if (header.isEmpty()) {
				throw new HandshakeRejectedException("Missing header");
			}
			this.headers.add(header.get());
			return super.onHandshake(ws, handshake);
		}

		@Override
		protected WsData createWsData(WebSocket ws) {
			return new WsData(ws);
		}

		@Override
		protected OnOpen getOnOpen() {
			return OnOpen.NO_OP;
		}

		@Override
		protected OnRequest getOnRequest() {
			return (ws, request) -> completedFuture(echo(request));
		}

		@Override
		protected OnNotification getOnNotification() {
			return OnNotification.NO_OP;
		}

		@Override
		protected OnError getOnError() {
			return OnError.NO_OP;
		}

		@Override
		protected OnClose getOnClose() {
			return OnClose.NO_OP;
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

	private static GenericJsonrpcResponseSuccess echo(JsonrpcRequest request) {
		var result = new JsonObject();
		result.add("value", request.getParams().get("value"));
		return new GenericJsonrpcResponseSuccess(request.getId(), result);
	}

	private static GenericJsonrpcRequest request(String value) {
		var params = new JsonObject();
		params.addProperty("value", value);
		return new GenericJsonrpcRequest("echo", params);
	}

	private static String valueOf(CompletableFuture<? extends JsonrpcMessage> response) throws Exception {
		var success = (GenericJsonrpcResponseSuccess) response.get(10, TimeUnit.SECONDS);
		return success.getResult().get("value").getAsString();
	}

	private static URI uri(AbstractWebsocketServer<?> server) {
		return URI.create("ws://localhost:" + server.getPort() + "/any/path");
	}

	private static ClientReconnectorWorker.Config config(LinkedBlockingQueue<WebsocketReconnectorEvent> events) {
		return new ClientReconnectorWorker.Config(5, 1, 1, events == null ? e -> {
		} : events::add);
	}

	private static void await(BooleanSupplier condition) throws InterruptedException {
		var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (!condition.getAsBoolean()) {
			assertTrue(System.nanoTime() < deadline, "Timeout");
			Thread.sleep(20);
		}
	}

	@Test
	public void testRequestsInBothDirections() throws Exception {
		var notifications = new LinkedBlockingQueue<String>();
		try (var server = new TestServer(0);
				var client = new TestWebsocketClient(uri(server), Map.of(HEADER, "foo"), config(null), OnOpen.NO_OP,
						(ws, request) -> completedFuture(echo(request)),
						(ws, notification) -> notifications.add(notification.getMethod()), OnClose.NO_OP)) {
			client.startBlocking();
			assertEquals("foo", server.headers.poll(10, TimeUnit.SECONDS));

			// Client to server; a large message is sent in one frame by Java-WebSocket
			assertEquals("bar", valueOf(client.sendRequest(request("bar"))));
			var large = "x".repeat(2 * 1024 * 1024);
			assertEquals(large, valueOf(client.sendRequest(request(large))));

			// Server to client
			await(() -> server.getConnections().size() == 1);
			WsData wsData = server.getConnections().iterator().next().getAttachment();
			assertEquals("baz", valueOf(wsData.send(request("baz"))));
			server.broadcastMessage(new GenericJsonrpcNotification("hello", new JsonObject()));
			assertEquals("hello", notifications.poll(10, TimeUnit.SECONDS));
		}
	}

	@Test
	public void testHandshakeRejected() throws Exception {
		var events = new LinkedBlockingQueue<WebsocketReconnectorEvent>();
		var closeCodes = new LinkedBlockingQueue<Integer>();
		try (var server = new TestServer(0);
				var client = new TestWebsocketClient(uri(server), Map.of(), config(events), OnOpen.NO_OP,
						OnRequest.NO_OP, OnNotification.NO_OP, (ws, code, reason, remote) -> closeCodes.add(code))) {
			client.start();
			assertEquals(WebsocketReconnectorEvent.HANDSHAKE_FAILED, events.poll(10, TimeUnit.SECONDS));
			assertEquals(CloseCode.PROTOCOL_ERROR, closeCodes.poll(10, TimeUnit.SECONDS));
			assertTrue(server.getConnections().isEmpty());
		}
	}

	@Test
	public void testReconnectAfterServerRestart() throws Exception {
		var events = new LinkedBlockingQueue<WebsocketReconnectorEvent>();
		var server = new TestServer(0);
		var port = server.getPort();
		try (var client = new TestWebsocketClient(uri(server), Map.of(HEADER, "foo"), config(events), OnOpen.NO_OP,
				OnRequest.NO_OP, OnNotification.NO_OP, OnClose.NO_OP)) {
			client.start();
			assertEquals(WebsocketReconnectorEvent.CONNECTED, events.poll(10, TimeUnit.SECONDS));

			server.close();
			await(() -> !client.isConnected());

			try (var restarted = new TestServer(port)) {
				assertEquals(WebsocketReconnectorEvent.CONNECTED, events.poll(10, TimeUnit.SECONDS));
				assertEquals("bar", valueOf(client.sendRequest(request("bar"))));
			}
		}
	}

	@Test
	public void testJavaWebSocketClientToServer() throws Exception {
		var responses = new LinkedBlockingQueue<String>();
		try (var server = new TestServer(0)) {
			var client = new WebSocketClient(uri(server), new Draft_6455(new PerMessageDeflateExtension()),
					Map.of(HEADER, "old-edge")) {
				@Override
				public void onOpen(ServerHandshake handshake) {
				}

				@Override
				public void onMessage(String message) {
					responses.add(message);
				}

				@Override
				public void onClose(int code, String reason, boolean remote) {
				}

				@Override
				public void onError(Exception ex) {
				}
			};
			// Closes the connection if pings are not answered within 1.5 seconds
			client.setConnectionLostTimeout(1);
			assertTrue(client.connectBlocking(10, TimeUnit.SECONDS));
			Thread.sleep(4_000);
			assertTrue(client.isOpen());

			var request = request("bar");
			client.send(request.toString());
			var response = (GenericJsonrpcResponseSuccess) JsonrpcMessage
					.from(responses.poll(10, TimeUnit.SECONDS));
			assertEquals(new JsonPrimitive("bar"), response.getResult().get("value"));
			client.closeBlocking();
		}
	}

	@Test
	public void testClientToJavaWebSocketServer() throws Exception {
		var started = new CompletableFuture<Void>();
		var server = new WebSocketServer(new InetSocketAddress("localhost", 0), List.of(new Draft_6455())) {
			@Override
			public void onOpen(org.java_websocket.WebSocket conn, ClientHandshake handshake) {
			}

			@Override
			public void onMessage(org.java_websocket.WebSocket conn, String message) {
				try {
					conn.send(echo((JsonrpcRequest) JsonrpcMessage.from(message)).toString());
				} catch (Exception e) {
					throw new IllegalStateException(e);
				}
			}

			@Override
			public void onClose(org.java_websocket.WebSocket conn, int code, String reason, boolean remote) {
			}

			@Override
			public void onError(org.java_websocket.WebSocket conn, Exception ex) {
			}

			@Override
			public void onStart() {
				started.complete(null);
			}
		};
		server.start();
		try {
			started.get(10, TimeUnit.SECONDS);
			try (var client = new TestWebsocketClient(URI.create("ws://localhost:" + server.getPort()), Map.of(),
					config(null), OnOpen.NO_OP, OnRequest.NO_OP, OnNotification.NO_OP, OnClose.NO_OP)) {
				client.startBlocking();
				assertEquals("bar", valueOf(client.sendRequest(request("bar"))));
			}
		} finally {
			server.stop(1000);
		}
	}
}
