package io.openems.backend.b2bwebsocket;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.request.GetStatusOfEdgesRequest;
import io.openems.common.jsonrpc.request.SetGridConnScheduleRequest;
import io.openems.common.jsonrpc.request.SetGridConnScheduleRequest.GridConnSchedule;

public class B2bWebsocketTest {

	private static TestClient preparteTestClient() throws URISyntaxException, InterruptedException {
//		String uri = "ws://localhost:" + B2bWebsocket.DEFAULT_PORT;
		String uri = "ws://fems-test.beegy-dev.cc:10002";

		TestClient client = new TestClient(new URI(uri));
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
		long now = System.currentTimeMillis() / 1000;
		request.addScheduleEntry(new GridConnSchedule(now, 60, -3000));
		request.addScheduleEntry(new GridConnSchedule(now + 60, 60, -5000));
		System.out.println("Sending Request " + request);
		client.sendRequest(request, response -> {
			System.out.println("Response: " + response);
		});

		while (true) {
			Thread.sleep(5000);
		}
	}
}
