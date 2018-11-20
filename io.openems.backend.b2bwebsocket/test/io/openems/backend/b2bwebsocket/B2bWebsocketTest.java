package io.openems.backend.b2bwebsocket;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;

import io.openems.backend.b2bwebsocket.B2bWebsocket;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.request.GetStatusOfEdgesRequest;
import io.openems.common.jsonrpc.request.SetGridConnScheduleRequest;
import io.openems.common.jsonrpc.request.SetGridConnScheduleRequest.GridConnSchedule;

public class B2bWebsocketTest {

	private static TestClient preparteTestClient() throws URISyntaxException, InterruptedException {
		TestClient client = new TestClient(new URI("ws://localhost:" + B2bWebsocket.DEFAULT_PORT));
		client.startBlocking();
		return client;
	}

	@Test
	public void testGetStatusOfEdgesRequest() throws URISyntaxException, InterruptedException, OpenemsException {
		TestClient client = preparteTestClient();

		GetStatusOfEdgesRequest request = new GetStatusOfEdgesRequest();
		client.sendRequest(request, response -> {
			System.out.println(response);
		});

		while (true) {
			Thread.sleep(5000);
		}
	}

	@Test
	public void testSetGridConnSchedule() throws URISyntaxException, InterruptedException, OpenemsException {
		TestClient client = preparteTestClient();

		SetGridConnScheduleRequest request = new SetGridConnScheduleRequest("edge0");
		request.addScheduleEntry(new GridConnSchedule(1536041040, 900, 150));

		client.sendRequest(request, response -> {
			System.out.println(response);
		});

		while (true) {
			Thread.sleep(5000);
		}
	}
}
