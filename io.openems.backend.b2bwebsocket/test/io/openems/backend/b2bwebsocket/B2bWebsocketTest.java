package io.openems.backend.b2bwebsocket;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.GetChannelsValuesRequest;
import io.openems.common.jsonrpc.request.GetStatusOfEdgesRequest;
import io.openems.common.jsonrpc.request.SetGridConnScheduleRequest;
import io.openems.common.jsonrpc.request.SetGridConnScheduleRequest.GridConnSchedule;
import io.openems.common.types.ChannelAddress;

public class B2bWebsocketTest {

	private static final String URI = "ws://localhost:" + B2bWebsocket.DEFAULT_PORT;
	private static final String USERNAME = "demo@fenecon.de";
	private static final String PASSWORD = "femsdemo";

	private static TestClient preparteTestClient() throws URISyntaxException, InterruptedException {
		Map<String, String> httpHeaders = new HashMap<>();
		String auth = new String(Base64.getEncoder().encode((USERNAME + ":" + PASSWORD).getBytes()),
				StandardCharsets.UTF_8);
		httpHeaders.put("Authorization", "Basic " + auth);
		TestClient client = new TestClient(new URI(URI), httpHeaders);
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
		client.stop();
	}

	@Test
	public void testGetChannelsValuesRequest()
			throws URISyntaxException, InterruptedException, ExecutionException, OpenemsNamedException {
		TestClient client = preparteTestClient();

		GetChannelsValuesRequest request = new GetChannelsValuesRequest();
		request.addEdgeId("edge1");
		request.addChannel(new ChannelAddress("_sum", "EssSoc"));
		request.addChannel(new ChannelAddress("_sum", "ProductionActivePower"));
		CompletableFuture<JsonrpcResponseSuccess> responseFuture = client.sendRequest(request);
		System.out.println(responseFuture.get().toString());
		client.stop();
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
