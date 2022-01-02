package io.openems.backend.b2bwebsocket;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import io.openems.backend.b2bwebsocket.jsonrpc.request.SubscribeEdgesChannelsRequest;
import io.openems.backend.common.jsonrpc.request.GetEdgesChannelsValuesRequest;
import io.openems.backend.common.jsonrpc.request.GetEdgesStatusRequest;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.request.SetGridConnScheduleRequest;
import io.openems.common.jsonrpc.request.SetGridConnScheduleRequest.GridConnSchedule;
import io.openems.common.types.ChannelAddress;

/**
 * This Test demonstrates the usage of the OpenEMS Backend-to-Backend API
 * interface. To start the tests make sure to start OpenEMS Backend and activate
 * the B2bWebsocket component via Apache Felix. Afterwards uncomment the "@Test"
 * annotations below and execute the Tests.
 */
public class B2bWebsocketTest {

	private static final String URI = "ws://localhost:8076";
	private static final String USERNAME = "user";
	private static final String PASSWORD = "password";

	private static TestClient prepareTestClient() throws URISyntaxException, InterruptedException {
		Map<String, String> httpHeaders = new HashMap<>();
		var auth = new String(
				Base64.getEncoder().encode((B2bWebsocketTest.USERNAME + ":" + B2bWebsocketTest.PASSWORD).getBytes()),
				StandardCharsets.UTF_8);
		httpHeaders.put("Authorization", "Basic " + auth);
		var client = new TestClient(new URI(B2bWebsocketTest.URI), httpHeaders);
		client.startBlocking();
		return client;
	}

	// @Test
	protected void testGetEdgesStatusRequest()
			throws URISyntaxException, InterruptedException, ExecutionException, OpenemsNamedException {
		var client = B2bWebsocketTest.prepareTestClient();

		var request = new GetEdgesStatusRequest();
		try {
			var responseFuture = client.sendRequest(request);
			System.out.println(responseFuture.get().toString());
		} catch (InterruptedException | ExecutionException | OpenemsNamedException e) {
			System.out.println(e.getMessage());
		}
		client.stop();
	}

	// @Test
	protected void testGetEdgesChannelsValuesRequest() throws URISyntaxException, InterruptedException {
		var client = B2bWebsocketTest.prepareTestClient();

		var request = new GetEdgesChannelsValuesRequest();
		request.addEdgeId("edge0");
		request.addChannel(new ChannelAddress("_sum", "EssSoc"));
		request.addChannel(new ChannelAddress("_sum", "ProductionActivePower"));
		try {
			var responseFuture = client.sendRequest(request);
			System.out.println(responseFuture.get().toString());
		} catch (InterruptedException | ExecutionException | OpenemsNamedException e) {
			System.out.println(e.getMessage());
		}
		client.stop();
	}

	// @Test
	protected void testSubscribeEdgesChannelsRequest()
			throws URISyntaxException, InterruptedException, ExecutionException, OpenemsNamedException {
		var client = B2bWebsocketTest.prepareTestClient();
		client.setOnNotification((ws, notification) -> {
			System.out.println(notification.toString());
		});

		var request = new SubscribeEdgesChannelsRequest(0);
		request.addEdgeId("edge0");
		request.addChannel(new ChannelAddress("_sum", "EssSoc"));
		request.addChannel(new ChannelAddress("_sum", "ProductionActivePower"));
		try {
			var responseFuture = client.sendRequest(request);
			System.out.println(responseFuture.get().toString());
		} catch (InterruptedException | ExecutionException | OpenemsNamedException e) {
			System.out.println(e.getMessage());
		}

		Thread.sleep(10000);
		client.stop();
	}

	// @Test
	protected void testSetGridConnSchedule() throws URISyntaxException, InterruptedException {
		var client = B2bWebsocketTest.prepareTestClient();

		var request = new SetGridConnScheduleRequest("edge0");
		var now = System.currentTimeMillis() / 1000;
		request.addScheduleEntry(new GridConnSchedule(now, 60, 0));
		// request.addScheduleEntry(new GridConnSchedule(now + 60, 60, -5000));
		try {
			var responseFuture = client.sendRequest(request);
			System.out.println(responseFuture.get().toString());
		} catch (InterruptedException | ExecutionException | OpenemsNamedException e) {
			System.out.println(e.getMessage());
		}
	}
}
