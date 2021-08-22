package io.openems.common.jsonrpc.base;

import static org.junit.Assert.assertEquals;

import java.util.Optional;
import java.util.UUID;

import org.junit.Test;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;

public class GenericJsonrpcRequestTest {

	@Test
	public void test() throws OpenemsNamedException {
		UUID id = UUID.randomUUID();
		String method = "dummyMethod";
		JsonObject params = JsonUtils.buildJsonObject() //
				.addProperty("hello", "world") //
				.addProperty("foo", "bar") //
				.build();
		int timeout = 10;

		// Test toString()
		GenericJsonrpcRequest sut = new GenericJsonrpcRequest(id, method, params, timeout);
		String json = sut.toString();

		assertEquals(
				"{\"jsonrpc\":\"2.0\",\"method\":\"dummyMethod\",\"params\":{\"hello\":\"world\",\"foo\":\"bar\"},\"id\":\""
						+ id.toString() + "\",\"timeout\":10}",
				json);

		// Test from()
		sut = GenericJsonrpcRequest.from(json);
		assertEquals(id, sut.getId());
		assertEquals(method, sut.getMethod());
		assertEquals(Optional.of(timeout), sut.getTimeout());
		assertEquals("{\"hello\":\"world\",\"foo\":\"bar\"}", sut.getParams().toString());

	}

}
