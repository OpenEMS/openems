package io.openems.common.utils;

import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonrpcUtils.simplifyJsonrpcMessage;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;

import java.util.Optional;

import org.junit.Test;

import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;

public class JsonrpcUtilsTest {

	@Test
	public void testSimplifyJsonrpcMessage() {
		var r = new GenericJsonrpcRequest(randomUUID(), "fooMethod", buildJsonObject() //
				.addProperty("foo", "bar") //
				.add("payload", new GenericJsonrpcRequest(randomUUID(), "barMethod", buildJsonObject() //
						.addProperty("lorem", "ipsum") //
						.build(), Optional.empty()).toJsonObject()) //
				.build(), 123);

		assertEquals(buildJsonObject() //
				.addProperty("method", "fooMethod") //
				.add("params", buildJsonObject() //
						.addProperty("foo", "bar") //
						.add("payload", buildJsonObject() //
								.addProperty("method", "barMethod") //
								.add("params", buildJsonObject() //
										.addProperty("lorem", "ipsum") //
										.build()) //
								.build()) //
						.build())
				.addProperty("timeout", 123) //
				.build(), //
				simplifyJsonrpcMessage(r));
	}

}
