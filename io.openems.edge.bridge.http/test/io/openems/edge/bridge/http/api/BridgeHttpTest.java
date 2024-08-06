package io.openems.edge.bridge.http.api;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingFunction;
import io.openems.common.test.TimeLeapClock;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.bridge.http.BridgeHttpImpl;
import io.openems.edge.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.edge.bridge.http.dummy.DummyEndpointFetcher;

public class BridgeHttpTest {

	private DummyEndpointFetcher fetcher;
	private CycleSubscriber cycleSubscriber;
	private BridgeHttpImpl bridgeHttp;

	@Before
	public void before() throws Exception {
		this.cycleSubscriber = DummyBridgeHttpFactory.cycleSubscriber();
		this.fetcher = DummyBridgeHttpFactory.dummyEndpointFetcher();
		this.bridgeHttp = new BridgeHttpImpl(this.cycleSubscriber, this.fetcher,
				DummyBridgeHttpFactory.dummyBridgeHttpExecutor(new TimeLeapClock(), true));
	}

	@After
	public void after() throws Exception {
		this.bridgeHttp.deactivate();
	}

	@Test
	public void testGet() throws Exception {
		this.fetcher.addEndpointHandler(assertExact("dummy", HttpMethod.GET));
		assertEquals("success", this.bridgeHttp.get("dummy").get().data());
	}

	@Test
	public void testGetJson() throws Exception {
		this.fetcher.addEndpointHandler(assertExactJson("dummy", HttpMethod.GET));
		assertEquals(successJson(), this.bridgeHttp.getJson("dummy").get().data());
	}

	@Test
	public void testPut() throws Exception {
		this.fetcher.addEndpointHandler(assertExact("dummy", HttpMethod.PUT));
		assertEquals("success", this.bridgeHttp.put("dummy").get().data());
	}

	@Test
	public void testPutJson() throws Exception {
		this.fetcher.addEndpointHandler(assertExactJson("dummy", HttpMethod.PUT));
		assertEquals(successJson(), this.bridgeHttp.putJson("dummy").get().data());
	}

	@Test
	public void testPost() throws Exception {
		final var body = "body";
		this.fetcher.addEndpointHandler(assertExact("dummy", HttpMethod.POST, body));
		assertEquals("success", this.bridgeHttp.post("dummy", body).get().data());
	}

	@Test
	public void testPostJson() throws Exception {
		final var body = JsonUtils.buildJsonObject() //
				.addProperty("body", true) //
				.build();
		this.fetcher.addEndpointHandler(assertExactJson("dummy", HttpMethod.POST, body));
		assertEquals(successJson(), this.bridgeHttp.postJson("dummy", body).get().data());
	}

	@Test
	public void testDelete() throws Exception {
		this.fetcher.addEndpointHandler(assertExact("dummy", HttpMethod.DELETE));
		assertEquals("success", this.bridgeHttp.delete("dummy").get().data());
	}

	@Test
	public void testDeleteJson() throws Exception {
		this.fetcher.addEndpointHandler(assertExactJson("dummy", HttpMethod.DELETE));
		assertEquals(successJson(), this.bridgeHttp.deleteJson("dummy").get().data());
	}

	@Test
	public void testRequest() throws Exception {
		this.fetcher.addEndpointHandler(assertExact("dummy", HttpMethod.DELETE));

		final var response = this.bridgeHttp
				.request(new Endpoint("dummy", HttpMethod.DELETE, 12345, 1245, null, emptyMap()));

		assertEquals("success", response.get().data());
	}

	@Test
	public void testRequestJson() throws Exception {
		this.fetcher.addEndpointHandler(assertExactJson("dummy", HttpMethod.DELETE));

		final var response = this.bridgeHttp
				.requestJson(new Endpoint("dummy", HttpMethod.DELETE, 12345, 1245, null, emptyMap()));

		assertEquals(successJson(), response.get().data());
	}

	private static ThrowingFunction<Endpoint, HttpResponse<String>, HttpError> assertExact(//
			String url, //
			HttpMethod method //
	) {
		return assertExact(url, method, null);
	}

	private static ThrowingFunction<Endpoint, HttpResponse<String>, HttpError> assertExact(//
			String url, //
			HttpMethod method, //
			String body //
	) {
		return endpoint -> {
			if (!endpoint.url().equals(url)) {
				return null;
			}

			assertEquals(method, endpoint.method());
			assertEquals(body, endpoint.body());

			return HttpResponse.ok("success");
		};
	}

	private static ThrowingFunction<Endpoint, HttpResponse<String>, HttpError> assertExactJson(//
			String url, //
			HttpMethod method //
	) {
		return assertExactJson(url, method, null);
	}

	private static ThrowingFunction<Endpoint, HttpResponse<String>, HttpError> assertExactJson(//
			String url, //
			HttpMethod method, //
			JsonElement body //
	) {
		return endpoint -> {
			if (!endpoint.url().equals(url)) {
				return null;
			}

			assertEquals(method, endpoint.method());

			if (body != null) {
				assertNotNull(endpoint.body());
				try {
					assertEquals(body, JsonUtils.parse(endpoint.body()));
				} catch (OpenemsNamedException e) {
					fail(e.getMessage());
				}
			} else {
				assertNull(endpoint.body());
			}

			return HttpResponse.ok(successJson().toString());
		};
	}

	private static JsonElement successJson() {
		return JsonUtils.buildJsonObject() //
				.addProperty("success", true) //
				.build();
	}

}