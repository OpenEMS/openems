package io.openems.edge.battery.bmw;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.common.bridge.http.api.HttpMethod;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.HttpStatus;
import io.openems.common.utils.JsonUtils;

public class BmwTokenTest {

	@Test
	public void testTokenInitialization() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		final var fetcher = DummyBridgeHttpFactory.dummyEndpointFetcher();
		final var executor = DummyBridgeHttpFactory.dummyBridgeHttpExecutor(clock, true);

		var httpBridge = DummyBridgeHttpFactory.ofBridgeImpl(() -> fetcher, () -> executor).get();
		var token = new BmwToken(httpBridge);

		// Initially token should be null
		assertNull("Token should be null initially", token.getToken());
	}

	@Test
	public void testTokenFetchingWithLifetimeHours() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		final var fetcher = DummyBridgeHttpFactory.dummyEndpointFetcher();
		final var executor = DummyBridgeHttpFactory.dummyBridgeHttpExecutor(clock, true);

		fetcher.addEndpointHandler(t -> {
			if (t.url().contains("login") && t.method() == HttpMethod.POST) {
				var responseJson = JsonUtils.buildJsonObject() //
						.addProperty("jwtToken", "test-jwt-token-123") //
						.addProperty("lifetimeHours", 12) //
						.build();
				return HttpResponse.ok(responseJson.toString());
			}
			return new HttpResponse<>(HttpStatus.NOT_FOUND, java.util.Collections.emptyMap(), "Not found");
		});

		var httpBridge = DummyBridgeHttpFactory.ofBridgeImpl(() -> fetcher, () -> executor).get();
		var token = new BmwToken(httpBridge);

		var endpoint = new Endpoint("http://test.com/login", //
				HttpMethod.POST, //
				5000, 5000, //
				null, //
				java.util.Collections.emptyMap());

		assertNull("Token should be null initially", token.getToken());

		// Fetch token
		token.fetchToken(endpoint);

		// Trigger the executor to process the scheduled task
		executor.update();
		clock.leap(31, ChronoUnit.SECONDS); // Past the 30 second delay
		executor.update();

		// Token should now be set
		assertNotNull("Token should be set after fetching", token.getToken());
		assertEquals("Token value should match", "test-jwt-token-123", token.getToken());
	}

	@Test
	public void testTokenFetchingWithExpiresIn() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		final var fetcher = DummyBridgeHttpFactory.dummyEndpointFetcher();
		final var executor = DummyBridgeHttpFactory.dummyBridgeHttpExecutor(clock, true);

		// Mock token server response with expiresIn (in seconds)
		fetcher.addEndpointHandler(t -> {
			if (t.url().contains("login") && t.method() == HttpMethod.POST) {
				var responseJson = JsonUtils.buildJsonObject() //
						.addProperty("jwtToken", "test-jwt-token-456") //
						.addProperty("expiresIn", 21600) // 6 hours in seconds
						.build();
				return HttpResponse.ok(responseJson.toString());
			}
			return new HttpResponse<>(HttpStatus.NOT_FOUND, java.util.Collections.emptyMap(), "Not found");
		});

		var httpBridge = DummyBridgeHttpFactory.ofBridgeImpl(() -> fetcher, () -> executor).get();
		var token = new BmwToken(httpBridge);

		var endpoint = new Endpoint("http://test.com/login", //
				HttpMethod.POST, //
				5000, 5000, //
				null, //
				java.util.Collections.emptyMap());

		assertNull("Token should be null initially", token.getToken());

		token.fetchToken(endpoint);
		executor.update();
		clock.leap(31, ChronoUnit.SECONDS);
		executor.update();

		assertNotNull("Token should be set after fetching", token.getToken());
		assertEquals("Token value should match", "test-jwt-token-456", token.getToken());
	}

	@Test
	public void testTokenFetchingWithDefaultLifetime() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		final var fetcher = DummyBridgeHttpFactory.dummyEndpointFetcher();
		final var executor = DummyBridgeHttpFactory.dummyBridgeHttpExecutor(clock, true);

		// Mock token server response without lifetime fields (should use default)
		fetcher.addEndpointHandler(t -> {
			if (t.url().contains("login") && t.method() == HttpMethod.POST) {
				var responseJson = JsonUtils.buildJsonObject() //
						.addProperty("jwtToken", "test-jwt-token-default") //
						.build();
				return HttpResponse.ok(responseJson.toString());
			}
			return new HttpResponse<>(HttpStatus.NOT_FOUND, java.util.Collections.emptyMap(), "Not found");
		});

		var httpBridge = DummyBridgeHttpFactory.ofBridgeImpl(() -> fetcher, () -> executor).get();
		var token = new BmwToken(httpBridge);

		var endpoint = new Endpoint("http://test.com/login", //
				HttpMethod.POST, //
				5000, 5000, //
				null, //
				java.util.Collections.emptyMap());

		assertNull("Token should be null initially", token.getToken());

		token.fetchToken(endpoint);
		executor.update();
		clock.leap(31, ChronoUnit.SECONDS);
		executor.update();

		assertNotNull("Token should be set after fetching", token.getToken());
		assertEquals("Token value should match", "test-jwt-token-default", token.getToken());
	}

	@Test
	public void testTokenWithInvalidResponse() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		final var fetcher = DummyBridgeHttpFactory.dummyEndpointFetcher();
		final var executor = DummyBridgeHttpFactory.dummyBridgeHttpExecutor(clock, true);

		// Mock server returns invalid JSON
		fetcher.addEndpointHandler(t -> {
			return HttpResponse.ok("invalid-json-response");
		});

		var httpBridge = DummyBridgeHttpFactory.ofBridgeImpl(() -> fetcher, () -> executor).get();
		var token = new BmwToken(httpBridge);

		var endpoint = new Endpoint("http://test.com/login", //
				HttpMethod.POST, //
				5000, 5000, //
				null, //
				java.util.Collections.emptyMap());

		assertNull("Token should be null initially", token.getToken());

		token.fetchToken(endpoint);
		executor.update();
		clock.leap(31, ChronoUnit.SECONDS);
		executor.update();

		// Token should remain null due to invalid response
		assertNull("Token should remain null due to invalid JSON", token.getToken());
	}

}