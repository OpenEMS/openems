package io.openems.edge.controller.ess.fastfrequencyreserve;

import java.net.URI;
//import org.junit.Test;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import io.openems.edge.controller.ess.fastfrequencyreserve.enums.ActivationTime;
import io.openems.edge.controller.ess.fastfrequencyreserve.enums.SupportDuration;
import io.openems.edge.controller.ess.fastfrequencyreserve.jsonrpc.SetActivateFastFreqReserveRequest;
import io.openems.edge.controller.ess.fastfrequencyreserve.jsonrpc.SetActivateFastFreqReserveRequest.ActivateFastFreqReserveSchedule;

/**
 * This Test demonstrates the usage of the OpenEMS Backend-to-Backend API
 * interface. To start the tests make sure to start OpenEMS Backend and activate
 * the B2bWebsocket component via Apache Felix. Afterwards uncomment the "@Test"
 * annotations below and execute the Tests.
 */
public class JsonRpcTest {

	private static final String URI = "ws://localhost:8076";
	private static final String USERNAME = "user";
	private static final String PASSWORD = "password";

	private static TestClient prepareTestClient() throws URISyntaxException, InterruptedException {
		Map<String, String> httpHeaders = new HashMap<>();
		var auth = new String(
				Base64.getEncoder().encode((JsonRpcTest.USERNAME + ":" + JsonRpcTest.PASSWORD).getBytes()),
				StandardCharsets.UTF_8);
		httpHeaders.put("Authorization", "Basic " + auth);
		var client = new TestClient(new URI(JsonRpcTest.URI), httpHeaders);
		client.startBlocking();
		return client;
	}

	/**
	 * Tests the activation of Fast Frequency Reserve schedule.
	 *
	 * @throws URISyntaxException   String could not be parsed as a URI reference.
	 * @throws InterruptedException interrupted exception.
	 */
	// @Test
	public void testActivateFastFreqReserveSchedule() throws URISyntaxException, InterruptedException {
		var client = JsonRpcTest.prepareTestClient();

		var request = new SetActivateFastFreqReserveRequest("edge0");
		var now = System.currentTimeMillis() / 1000;
		ActivateFastFreqReserveSchedule newEntry = new ActivateFastFreqReserveSchedule(//
				now, //
				1000, //
				92000, //
				50000, //
				ActivationTime.LONG_ACTIVATION_RUN, //
				SupportDuration.LONG_SUPPORT_DURATION);
		request.addScheduleEntry(newEntry);

		try {
			var responseFuture = client.sendRequest(request);
			System.out.println(responseFuture.get().toString());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

}
