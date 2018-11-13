package io.openems.backend.b2bwebsocket;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;

import io.openems.backend.b2bwebsocket.B2bWebsocket;
import io.openems.backend.b2bwebsocket.jsonrpc.GetStatusOfEdgesRequest;
import io.openems.common.exceptions.OpenemsException;

public class B2bWebsocketTest {

	@Test
	public void testGetStatusOfEdgesRequest() throws URISyntaxException, InterruptedException, OpenemsException {
		TestClient client = new TestClient(new URI("ws://localhost:" + B2bWebsocket.DEFAULT_PORT));
		client.connectBlocking();
		
		GetStatusOfEdgesRequest request = new GetStatusOfEdgesRequest();
		client.sendRequest(request, response -> {
			System.out.println(response);
		});

		while (true) {
			Thread.sleep(5000);
		}
	}
}
