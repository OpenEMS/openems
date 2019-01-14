package io.openems.backend.b2bwebsocket;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.GetStatusOfEdgesRequest;
import io.openems.common.jsonrpc.request.SetGridConnScheduleRequest;
import io.openems.common.jsonrpc.request.SetGridConnScheduleRequest.GridConnSchedule;

public class B2bWebsocketTest {

	private static TestClient preparteTestClient() throws URISyntaxException, InterruptedException {
		String uri = "ws://localhost:" + B2bWebsocket.DEFAULT_PORT;
		TestClient client = new TestClient(new URI(uri));
		client.startBlocking();
		return client;
	}

	@Test
	public void testGetStatusOfEdgesRequest()
			throws URISyntaxException, InterruptedException, ExecutionException, OpenemsNamedException {
		TestClient client = preparteTestClient();

		GetStatusOfEdgesRequest request = new GetStatusOfEdgesRequest();
		CompletableFuture<JsonrpcResponseSuccess> responseFuture = client.sendRequest(request);
		System.out.println(responseFuture.get().toString());
	}

	@Test
	public void testSetGridConnSchedule()
			throws URISyntaxException, InterruptedException, ExecutionException, OpenemsNamedException {
		TestClient client = preparteTestClient();

		SetGridConnScheduleRequest request = new SetGridConnScheduleRequest("edge0");
		long now = System.currentTimeMillis() / 1000;
		request.addScheduleEntry(new GridConnSchedule(now, 60, -3000));
		request.addScheduleEntry(new GridConnSchedule(now + 60, 60, -5000));
		CompletableFuture<JsonrpcResponseSuccess> responseFuture = client.sendRequest(request);
		System.out.println(responseFuture.get().toString());
	}
}
