package io.openems.edge.controller.api.rest.readonly;

import static io.openems.common.test.TestUtils.findRandomOpenPortOnAllLocalInterfaces;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Base64;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.types.HttpStatus;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.HttpMethod;
import io.openems.edge.bridge.http.api.UrlBuilder;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyUser;
import io.openems.edge.common.test.DummyUserService;
import io.openems.edge.controller.api.rest.DummyJsonRpcRestHandlerFactory;
import io.openems.edge.controller.api.rest.JsonRpcRestHandler;
import io.openems.edge.controller.test.ControllerTest;

public class ControllerApiRestReadOnlyImplTest {

	private static final Map<String, String> ADMIN_AUTHENTICATION = Map.of("Authorization",
			"Basic " + Base64.getEncoder().encodeToString("admin:admin".getBytes()));

	private static Sum sum;
	private static ControllerTest test;
	private static int port;
	private static BridgeHttp bridgeHttp;

	/**
	 * Before class.
	 * 
	 * @throws Exception on error
	 */
	@BeforeClass
	public static void beforeClass() throws Exception {
		sum = new DummySum() //
				.withEssSoc(69) //
				.withGridActivePower(300);

		port = findRandomOpenPortOnAllLocalInterfaces();

		test = new ControllerTest(new ControllerApiRestReadOnlyImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("userService", new DummyUserService(DummyUser.DUMMY_ADMIN)) //
				.addReference("restHandlerFactory", new DummyJsonRpcRestHandlerFactory(JsonRpcRestHandler::new)) //
				.addComponent(new DummySum() //
						.withEssSoc(69) //
						.withGridActivePower(300)) //
				.activate(MyConfig.create() //
						.setId("ctrlApiRest0") //
						.setEnabled(true) // do not actually start server
						.setConnectionlimit(5) //
						.setDebugMode(false) //
						.setPort(port) //
						.build());

		final var executor = DummyBridgeHttpFactory.dummyBridgeHttpExecutor(true);
		final var bridgeHttpFactory = DummyBridgeHttpFactory.ofBridgeImpl(DummyBridgeHttpFactory::cycleSubscriber,
				DummyBridgeHttpFactory::networkEndpointFetcher, () -> executor);

		bridgeHttp = bridgeHttpFactory.get();
	}

	/**
	 * After class. Cleanup.
	 * 
	 * @throws Exception on error
	 */
	@AfterClass
	public static void afterClass() throws Exception {
		test.deactivate();
		test = null;
		bridgeHttp = null;
		sum = null;
	}

	@Test(timeout = 5_000)
	public void testGetChannel() throws Exception {
		final var result = bridgeHttp
				.requestJson(new BridgeHttp.Endpoint("http://localhost:" + port + "/rest/channel/_sum/EssSoc",
						HttpMethod.GET, BridgeHttp.DEFAULT_CONNECT_TIMEOUT, BridgeHttp.DEFAULT_READ_TIMEOUT, null,
						ADMIN_AUTHENTICATION))
				.get();

		assertEquals(HttpStatus.OK, result.status());
		assertEquals(new ChannelRecord("_sum/EssSoc", "INTEGER", "RO", "Range 0..100", "%", new JsonPrimitive(69)),
				ChannelRecord.serializer().deserialize(result.data()));
	}

	@Test(timeout = 5_000)
	public void testGetChannelRegexOr() throws Exception {
		final var result = bridgeHttp.requestJson(new BridgeHttp.Endpoint(//
				UrlBuilder.parse("http://localhost") //
						.withPort(port) //
						.withPath("/rest/channel/_sum/(EssSoc|GridActivePower)") //
						.toEncodedString(),
				HttpMethod.GET, BridgeHttp.DEFAULT_CONNECT_TIMEOUT, BridgeHttp.DEFAULT_READ_TIMEOUT, null,
				ADMIN_AUTHENTICATION)).get();

		assertEquals(HttpStatus.OK, result.status());

		final var channels = ChannelRecord.serializer().toListSerializer().deserialize(result.data());
		assertEquals(2, channels.size());
		assertTrue(channels.contains(
				new ChannelRecord("_sum/EssSoc", "INTEGER", "RO", "Range 0..100", "%", new JsonPrimitive(69))));
		assertTrue(channels.contains(new ChannelRecord("_sum/GridActivePower", "INTEGER", "RO",
				"Grid exchange power. Negative values for sell-to-grid; positive for buy-from-grid", "W",
				new JsonPrimitive(300))));
	}

	@Test(timeout = 5_000)
	public void testGetChannelRegexArea() throws Exception {
		final var result = bridgeHttp.requestJson(new BridgeHttp.Endpoint(//
				UrlBuilder.parse("http://localhost") //
						.withPort(port) //
						.withPath("/rest/channel/_sum/EssActivePowerL[1-3]") //
						.toEncodedString(),
				HttpMethod.GET, BridgeHttp.DEFAULT_CONNECT_TIMEOUT, BridgeHttp.DEFAULT_READ_TIMEOUT, null,
				ADMIN_AUTHENTICATION)).get();

		assertEquals(HttpStatus.OK, result.status());

		final var channels = ChannelRecord.serializer().toListSerializer().deserialize(result.data());
		assertEquals(3, channels.size());
	}

	@Test(timeout = 5_000)
	public void testGetChannelRegexEscape() throws Exception {
		// "%5C" -> \
		final var result = bridgeHttp.requestJson(new BridgeHttp.Endpoint(
				"http://localhost:" + port + "/rest/channel/_sum/EssActivePowerL%5Cd", HttpMethod.GET,
				BridgeHttp.DEFAULT_CONNECT_TIMEOUT, BridgeHttp.DEFAULT_READ_TIMEOUT, null, ADMIN_AUTHENTICATION)).get();

		assertEquals(HttpStatus.OK, result.status());

		final var channels = ChannelRecord.serializer().toListSerializer().deserialize(result.data());
		assertEquals(3, channels.size());
	}

	@Test(timeout = 5_000)
	public void testGetChannelRegexWildcard() throws Exception {
		final var result = bridgeHttp.requestJson(new BridgeHttp.Endpoint(//
				UrlBuilder.parse("http://localhost") //
						.withPort(port) //
						.withPath("/rest/channel/_sum/.*") //
						.toEncodedString(),
				HttpMethod.GET, BridgeHttp.DEFAULT_CONNECT_TIMEOUT, BridgeHttp.DEFAULT_READ_TIMEOUT, null,
				ADMIN_AUTHENTICATION)).get();

		assertEquals(HttpStatus.OK, result.status());

		final var channels = ChannelRecord.serializer().toListSerializer().deserialize(result.data());
		assertEquals(sum.channels().size(), channels.size());
	}

	private record ChannelRecord(//
			String address, //
			String type, //
			String accessMode, //
			String text, //
			String unit, //
			JsonElement value //
	) {

		public static JsonSerializer<ChannelRecord> serializer() {
			return JsonSerializerUtil.jsonObjectSerializer(json -> new ChannelRecord(//
					json.getString("address"), //
					json.getString("type"), //
					json.getString("accessMode"), //
					json.getString("text"), //
					json.getString("unit"), //
					json.getJsonElement("value") //
			), obj -> JsonUtils.buildJsonObject() //
					.addProperty("address", obj.address()) //
					.addProperty("type", obj.type()) //
					.addProperty("accessMode", obj.accessMode()) //
					.addProperty("text", obj.text()) //
					.addProperty("unit", obj.unit()) //
					.add("value", obj.value()) //
					.build());
		}

	}

}
