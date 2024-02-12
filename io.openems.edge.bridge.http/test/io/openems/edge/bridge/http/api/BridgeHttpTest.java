package io.openems.edge.bridge.http.api;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingFunction;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.ReflectionUtils;
import io.openems.edge.bridge.http.BridgeHttpImpl;
import io.openems.edge.bridge.http.CycleSubscriber;
import io.openems.edge.bridge.http.DummyUrlFetcher;
import io.openems.edge.bridge.http.api.BridgeHttp.Endpoint;

public class BridgeHttpTest {

	private DummyUrlFetcher fetcher;
	private CycleSubscriber cycleSubscriber;
	private BridgeHttp bridgeHttp;

	@Before
	public void before() throws Exception {
		this.cycleSubscriber = new CycleSubscriber();
		this.bridgeHttp = new BridgeHttpImpl();
		ReflectionUtils.setAttribute(BridgeHttpImpl.class, this.bridgeHttp, "cycleSubscriber", this.cycleSubscriber);

		this.fetcher = new DummyUrlFetcher();
		ReflectionUtils.setAttribute(BridgeHttpImpl.class, this.bridgeHttp, "urlFetcher", this.fetcher);

		((BridgeHttpImpl) this.bridgeHttp).activate();
	}

	@After
	public void after() throws Exception {
		((BridgeHttpImpl) this.bridgeHttp).deactivate();
	}

	@Test
	public void testGet() throws Exception {
		this.fetcher.addUrlHandler(assertExact("dummy", HttpMethod.GET));
		assertEquals("success", this.bridgeHttp.get("dummy").get());
	}

	@Test
	public void testGetJson() throws Exception {
		this.fetcher.addUrlHandler(assertExactJson("dummy", HttpMethod.GET));
		assertEquals(successJson(), this.bridgeHttp.getJson("dummy").get());
	}

	@Test
	public void testPut() throws Exception {
		this.fetcher.addUrlHandler(assertExact("dummy", HttpMethod.PUT));
		assertEquals("success", this.bridgeHttp.put("dummy").get());
	}

	@Test
	public void testPutJson() throws Exception {
		this.fetcher.addUrlHandler(assertExactJson("dummy", HttpMethod.PUT));
		assertEquals(successJson(), this.bridgeHttp.putJson("dummy").get());
	}

	@Test
	public void testPost() throws Exception {
		final var body = "body";
		this.fetcher.addUrlHandler(assertExact("dummy", HttpMethod.POST, body));
		assertEquals("success", this.bridgeHttp.post("dummy", body).get());
	}

	@Test
	public void testPostJson() throws Exception {
		final var body = JsonUtils.buildJsonObject() //
				.addProperty("body", true) //
				.build();
		this.fetcher.addUrlHandler(assertExactJson("dummy", HttpMethod.POST, body));
		assertEquals(successJson(), this.bridgeHttp.postJson("dummy", body).get());
	}

	@Test
	public void testDelete() throws Exception {
		this.fetcher.addUrlHandler(assertExact("dummy", HttpMethod.DELETE));
		assertEquals("success", this.bridgeHttp.delete("dummy").get());
	}

	@Test
	public void testDeleteJson() throws Exception {
		this.fetcher.addUrlHandler(assertExactJson("dummy", HttpMethod.DELETE));
		assertEquals(successJson(), this.bridgeHttp.deleteJson("dummy").get());
	}

	@Test
	public void testRequest() throws Exception {
		this.fetcher.addUrlHandler(assertExact("dummy", HttpMethod.DELETE));

		final var response = this.bridgeHttp
				.request(new Endpoint("dummy", HttpMethod.DELETE, 12345, 1245, null, emptyMap()));

		assertEquals("success", response.get());
	}

	@Test
	public void testRequestJson() throws Exception {
		this.fetcher.addUrlHandler(assertExactJson("dummy", HttpMethod.DELETE));

		final var response = this.bridgeHttp
				.requestJson(new Endpoint("dummy", HttpMethod.DELETE, 12345, 1245, null, emptyMap()));

		assertEquals(successJson(), response.get());
	}

	private static ThrowingFunction<Endpoint, String, OpenemsNamedException> assertExact(//
			String url, //
			HttpMethod method //
	) {
		return assertExact(url, method, null);
	}

	private static ThrowingFunction<Endpoint, String, OpenemsNamedException> assertExact(//
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

			return "success";
		};
	}

	private static ThrowingFunction<Endpoint, String, OpenemsNamedException> assertExactJson(//
			String url, //
			HttpMethod method //
	) {
		return assertExactJson(url, method, null);
	}

	private static ThrowingFunction<Endpoint, String, OpenemsNamedException> assertExactJson(//
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
				assertEquals(body, JsonUtils.parse(endpoint.body()));
			} else {
				assertNull(endpoint.body());
			}

			return successJson().toString();
		};
	}

	private static JsonElement successJson() {
		return JsonUtils.buildJsonObject() //
				.addProperty("success", true) //
				.build();
	}

}
