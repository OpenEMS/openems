package io.openems.edge.controller.api.rest.readwrite;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static io.openems.edge.common.test.DummyUser.DUMMY_GUEST;
import static io.openems.edge.common.test.DummyUser.DUMMY_INSTALLER;
import static io.openems.edge.common.test.DummyUser.DUMMY_OWNER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

import org.junit.Test;
import org.osgi.framework.Constants;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.GetEdgeConfigRequest;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyUserService;
import io.openems.edge.common.test.TestUtils;
import io.openems.edge.controller.api.common.handler.ComponentConfigRequestHandler;
import io.openems.edge.controller.api.common.handler.ComponentRequestHandler;
import io.openems.edge.controller.api.common.handler.RoutesJsonApiHandler;
import io.openems.edge.controller.api.rest.DummyJsonRpcRestHandlerFactory;
import io.openems.edge.controller.api.rest.JsonRpcRestHandler;
import io.openems.edge.controller.api.rest.handler.BindingComponentConfigRequestHandler;
import io.openems.edge.controller.api.rest.handler.BindingComponentRequestHandler;
import io.openems.edge.controller.api.rest.handler.BindingRoutesJsonApiHandler;
import io.openems.edge.controller.api.rest.handler.RootRequestHandler;
import io.openems.edge.controller.test.ControllerTest;

public class ControllerApiRestReadWriteImplTest {

	private static final String CTRL_ID = "ctrlApiRest0";
	private static final String DUMMY_ID = "dummy0";

	@Test
	public void test() throws OpenemsException, Exception {
		final var port = TestUtils.findRandomOpenPortOnAllLocalInterfaces();

		final var componentManager = new DummyComponentManager();

		final var rootHandler = new RootRequestHandler(new BindingRoutesJsonApiHandler(new RoutesJsonApiHandler()));
		rootHandler.bindJsonApi(
				new BindingComponentConfigRequestHandler(new ComponentConfigRequestHandler(componentManager)));
		final var componentRequestHandler = new ComponentRequestHandler();
		componentRequestHandler.bindJsonApi(componentManager, ImmutableMap.<String, Object>builder() //
				.put(Constants.SERVICE_ID, 0L) //
				.build());
		rootHandler.bindJsonApi(new BindingComponentRequestHandler(componentRequestHandler));

		final var factory = new DummyJsonRpcRestHandlerFactory(() -> {
			final var restHandler = new JsonRpcRestHandler();
			restHandler.bindRootHandler(rootHandler);
			return restHandler;
		});

		var sut = new ControllerApiRestReadWriteImpl();
		var test = new ControllerTest(sut) //
				.addReference("componentManager", componentManager) //
				.addReference("userService", new DummyUserService(//
						DUMMY_GUEST, DUMMY_OWNER, DUMMY_INSTALLER, DUMMY_ADMIN)) //
				.addReference("restHandlerFactory", factory) //
				.addComponent(new DummyComponent(DUMMY_ID) //
						.withReadChannel(1234)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setApiTimeout(60) //
						.setConnectionlimit(5) //
						.setDebugMode(false) //
						.setPort(port) //
						.build());

		/*
		 * /rest/channel/*
		 */
		// GET successful as GUEST
		var channelGet = sendGetRequest(port, DUMMY_GUEST.password, "/rest/channel/dummy0/ReadChannel");
		assertEquals(JsonUtils.buildJsonObject() //
				.addProperty("address", "dummy0/ReadChannel") //
				.addProperty("type", "INTEGER") //
				.addProperty("accessMode", "RO") //
				.addProperty("text", "This is a Read-Channel") //
				.addProperty("unit", "W") //
				.addProperty("value", 1234) //
				.build(), channelGet);

		// POST successful as OWNER
		var channelPost = sendPostRequest(port, DUMMY_OWNER.password, "/rest/channel/dummy0/WriteChannel",
				JsonUtils.buildJsonObject() //
						.addProperty("value", 4321) //
						.build());
		assertEquals(new JsonObject(), channelPost);
		test //
				.next(new TestCase() //
						.output(new ChannelAddress("dummy0", "WriteChannel"), 4321) //
						.output(new ChannelAddress(CTRL_ID, "ApiWorkerLog"), "dummy0/WriteChannel:4321"));

		// POST fails as GUEST
		try {
			sendPostRequest(port, DUMMY_GUEST.password, "/rest/channel/dummy0/WriteChannel", JsonUtils.buildJsonObject() //
					.addProperty("value", 4321) //
					.build());
			assertTrue(false);
		} catch (OpenemsNamedException e) {
			// ignore
		}

		/*
		 * JSON-RPC
		 */
		// POST successful as OWNER
		var request = new GetEdgeConfigRequest().toJsonObject();
		JsonrpcResponseSuccess.from(//
				JsonUtils.getAsJsonObject(//
						sendPostRequest(port, DUMMY_OWNER.password, "/jsonrpc", request)));

		// POST fails as GUEST
		try {
			sendPostRequest(port, DUMMY_GUEST.password, "/jsonrpc", new GetEdgeConfigRequest().toJsonObject());
			assertTrue(false);
		} catch (OpenemsNamedException e) {
			// ignore
		}

		sut.deactivate();
	}

	private static JsonElement sendGetRequest(int port, String password, String endpoint) throws OpenemsNamedException {
		return sendRequest(port, "GET", password, endpoint, null);
	}

	private static JsonElement sendPostRequest(int port, String password, String endpoint, JsonObject request)
			throws OpenemsNamedException {
		return sendRequest(port, "POST", password, endpoint, request);
	}

	private static JsonElement sendRequest(int port, String requestMethod, String password, String endpoint,
			JsonObject request) throws OpenemsNamedException {
		try {
			var url = new URL("http://127.0.0.1:" + port + endpoint);
			var con = (HttpURLConnection) url.openConnection();
			con.setRequestProperty("Authorization",
					"Basic " + new String(Base64.getEncoder().encode(("x:" + password).getBytes())));
			con.setRequestMethod(requestMethod);
			con.setRequestProperty("Content-Type", "application/json; utf-8");
			con.setRequestProperty("Accept", "application/json");
			con.setConnectTimeout(5000);
			con.setReadTimeout(50000);
			if (request != null) {
				con.setDoOutput(true);
				try (var os = con.getOutputStream()) {
					var input = request.toString().getBytes("utf-8");
					os.write(input, 0, input.length);
				}
			}

			var status = con.getResponseCode();
			String body;
			try (var in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
				// Read HTTP response
				var content = new StringBuilder();
				String line;
				while ((line = in.readLine()) != null) {
					content.append(line);
					content.append(System.lineSeparator());
				}
				body = content.toString();
			}
			if (status < 300) {
				// Parse response to JSON
				return JsonUtils.parse(body);
			}
			throw new OpenemsException("Error while reading from API. Response code: " + status + ". " + body);
		} catch (OpenemsNamedException | IOException e) {
			throw new OpenemsException(
					"Unable to read from API. " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

	private static class DummyComponent extends AbstractDummyOpenemsComponent<DummyComponent>
			implements OpenemsComponent {

		private static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
			READ_CHANNEL(new IntegerDoc() //
					.unit(Unit.WATT) //
					.text("This is a Read-Channel")), //
			WRITE_CHANNEL(Doc.of(OpenemsType.INTEGER) //
					.accessMode(AccessMode.READ_WRITE)); //

			private final Doc doc;

			private ChannelId(Doc doc) {
				this.doc = doc;
			}

			@Override
			public Doc doc() {
				return this.doc;
			}
		}

		public DummyComponent(String id) {
			super(id, //
					OpenemsComponent.ChannelId.values(), //
					DummyComponent.ChannelId.values() //
			);
		}

		@Override
		protected DummyComponent self() {
			return this;
		}

		/**
		 * Set {@link ChannelId#READ_CHANNEL}.
		 *
		 * @param value the value
		 * @return myself
		 */
		public DummyComponent withReadChannel(Integer value) {
			TestUtils.withValue(this, ChannelId.READ_CHANNEL, value);
			return this.self();
		}

	}
}
