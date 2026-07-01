package io.openems.edge.controller.api.backend.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

import org.junit.Test;

import io.openems.common.jsonrpc.base.JsonrpcMessage;
import io.openems.common.jsonrpc.notification.EdgeRpcNotification;
import io.openems.common.jsonrpc.notification.SystemLogNotification;
import io.openems.common.types.SystemLog;
import io.openems.common.websocket.AbstractWebsocketClient;
import io.openems.edge.controller.api.backend.ControllerApiBackendImpl;
import io.openems.edge.controller.api.backend.WebsocketClient;

public class SubscribeSystemLogJsonApiHandlerTest {

	@Test
	public void testSystemLogNotificationIsWrappedForEdgeManager() {
		final var sut = new SubscribeSystemLogJsonApiHandler();
		final var webSocket = new CapturingWebsocketClient();

		sut.subscribe(webSocket, notification -> new EdgeRpcNotification("edge0", notification));
		sut.sendSystemLogNotification(new SystemLogNotification(new SystemLog(//
				ZonedDateTime.of(2026, 7, 1, 12, 0, 0, 0, ZoneOffset.UTC), //
				SystemLog.Level.INFO, //
				"test", //
				"message")));

		assertTrue(webSocket.message instanceof EdgeRpcNotification);
		final var message = (EdgeRpcNotification) webSocket.message;
		assertEquals("edge0", message.getEdgeId());
		assertEquals(SystemLogNotification.METHOD, message.getPayload().getMethod());
	}

	private static final class CapturingWebsocketClient extends WebsocketClient {
		private JsonrpcMessage message;

		private CapturingWebsocketClient() {
			super(new ControllerApiBackendImpl(), "test", URI.create("ws://localhost"), Map.of(),
					AbstractWebsocketClient.NO_PROXY);
		}

		@Override
		public boolean sendMessage(JsonrpcMessage message) {
			this.message = message;
			return true;
		}
	}
}
