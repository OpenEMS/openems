package io.openems.edge.controller.api.websocket;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;
import org.junit.Test;

import io.openems.common.websocket.JsonrpcRequest;
import io.openems.edge.common.jsonapi.ComponentJsonApi;

public class JsonApiTest {

	@Test
	public void testGetModbusProtocol() throws URISyntaxException, InterruptedException {
		WebSocketClient ws = new WebSocketClient(new URI("ws://localhost:8085")) {

			@Override
			public void onOpen(ServerHandshake arg0) {
			}

			@Override
			public void onMessage(String m) {
				System.out.println("onMessage: " + m);
			}

			@Override
			public void onError(Exception arg0) {
			}

			@Override
			public void onClose(int arg0, String arg1, boolean arg2) {
			}
		};
		ws.connect();

		ComponentJsonApi r = new ComponentJsonApi( //
				UUID.randomUUID().toString(), //
				"ctrlApiModbusTcp0", new JsonrpcRequest( //
						UUID.randomUUID().toString(), //
						"getModbusProtocol", //
						new JSONObject()));

		ws.send(r.toString());

		Thread.sleep(2000);

		ws.close();
	}

}
