package io.openems.backend.b2bwebsocket;

import static org.junit.Assert.assertNotNull;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.b2bwebsocket.jsonrpc.request.SubscribeEdgesChannelsRequest;
import io.openems.backend.common.jsonrpc.request.GetEdgesChannelsValuesRequest;
import io.openems.backend.common.jsonrpc.request.GetEdgesStatusRequest;
import io.openems.common.jsonrpc.request.EdgeRpcRequest;
import io.openems.common.jsonrpc.request.GetEdgeConfigRequest;
import io.openems.common.jsonrpc.request.SetGridConnScheduleRequest;
import io.openems.common.jsonrpc.request.SetGridConnScheduleRequest.GridConnSchedule;
import io.openems.common.types.ChannelAddress;

/**
 * This Test demonstrates the usage of the OpenEMS Backend-to-Backend API
 * interface. To start the tests make sure to start OpenEMS Backend and activate
 * the B2bWebsocket component via Apache Felix. Afterwards run this App via
 * main().
 */
@Ignore("Not a test, but a test application for B2B Websocket API; Only works if OpenEMS Backend is running")
public class B2bWebsocketTest {
	
	private static Logger log = LoggerFactory.getLogger(B2bWebsocketTest.class);

	private static final String URI = "ws://localhost:8076";
	private static final String USERNAME = "user";
	private static final String PASSWORD = "password";

	@Test
	public void getEdgesStatusRequest() throws Exception {
		var request = new GetEdgesStatusRequest("edge0");
		try (final var client = TestClient.prepareAndStart(URI, USERNAME, PASSWORD)) {
			var response = client.sendRequest(request).get();
			assertNotNull(response);
			log.info(response.toString());
		}
	}

	@Test
	public void getEdgeConfigRequest() throws Exception {
		var request = new EdgeRpcRequest("edge0", new GetEdgeConfigRequest());
		try (final var client = TestClient.prepareAndStart(URI, USERNAME, PASSWORD)) {
			var response = client.sendRequest(request).get();
			assertNotNull(response);
			log.info(response.toString());
		}
	}

	@Test
	public void getEdgesChannelsValuesRequest() throws Exception {
		var request = new GetEdgesChannelsValuesRequest();
		request.addEdgeId("edge0");
		request.addChannel(new ChannelAddress("_sum", "EssSoc"));
		request.addChannel(new ChannelAddress("_sum", "ProductionActivePower"));
		try (final var client = TestClient.prepareAndStart(URI, USERNAME, PASSWORD)) {
			var response = client.sendRequest(request).get();
			assertNotNull(response);
			log.info(response.toString());
		}
	}

	@Test
	public void subscribeEdgesChannelsRequest() throws Exception {
		var request = new SubscribeEdgesChannelsRequest(0);
		request.addEdgeId("edge0");
		request.addChannel(new ChannelAddress("_sum", "EssSoc"));
		request.addChannel(new ChannelAddress("_sum", "ProductionActivePower"));
		try (final var client = TestClient.prepareAndStart(URI, USERNAME, PASSWORD)) {
			client.setOnNotification((ws, notification) -> {
				log.info(notification.toString());
			});
			var response = client.sendRequest(request).get();
			assertNotNull(response);
			log.info(response.toString());
		}
	}

	@Test
	public void setGridConnSchedule() throws Exception {
		var request = new SetGridConnScheduleRequest("edge0");
		var now = System.currentTimeMillis() / 1000;
		request.addScheduleEntry(new GridConnSchedule(now, 60, 0));
		// request.addScheduleEntry(new GridConnSchedule(now + 60, 60, -5000));
		try (final var client = TestClient.prepareAndStart(URI, USERNAME, PASSWORD)) {
			var response = client.sendRequest(request).get();
			assertNotNull(response);
			log.info(response.toString());
		}
	}
}
