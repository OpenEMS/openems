package io.openems.common.websocket;

import static org.junit.Assert.assertEquals;

import java.util.Optional;
import java.util.UUID;

import org.junit.Test;

import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.utils.JsonUtils;

public class OnRequestHandlerTest {

	@Test
	public void testSimplifyJsonrpcMessage() {
		var r = new GenericJsonrpcRequest(UUID.randomUUID(), "fooMethod", JsonUtils.buildJsonObject() //
				.addProperty("foo", "bar") //
				.add("payload", new GenericJsonrpcRequest(UUID.randomUUID(), "barMethod", JsonUtils.buildJsonObject() //
						.addProperty("lorem", "ipsum") //
						.build(), Optional.empty()).toJsonObject()) //
				.build(), 123);

		assertEquals(JsonUtils.buildJsonObject() //
				.addProperty("method", "fooMethod") //
				.add("params", JsonUtils.buildJsonObject() //
						.addProperty("foo", "bar") //
						.add("payload", JsonUtils.buildJsonObject() //
								.addProperty("method", "barMethod") //
								.add("params", JsonUtils.buildJsonObject() //
										.addProperty("lorem", "ipsum") //
										.build()) //
								.build()) //
						.build())
				.addProperty("timeout", 123) //
				.build(), //
				OnRequestHandler.simplifyJsonrpcMessage(r.toJsonObject()));

		System.out.println(OnRequestHandler.simplifyJsonrpcMessage(r.toJsonObject()));
	}

}
