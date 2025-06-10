package io.openems.common.jsonrpc.base;

import java.util.Optional;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;

public class GenericJsonrpcRequestTest {

	@Test
	public void test() throws OpenemsNamedException {
		var id = UUID.randomUUID();
		var method = "dummyMethod";
		var params = JsonUtils.buildJsonObject() //
				.addProperty("hello", "world") //
				.addProperty("foo", "bar") //
				.build();
		var timeout = 10;

		// Test toString()
		var sut = new GenericJsonrpcRequest(id, method, params, timeout);
		var json = sut.toString();

		Assert.assertEquals(
				"{\"jsonrpc\":\"2.0\",\"method\":\"dummyMethod\",\"params\":{\"hello\":\"world\",\"foo\":\"bar\"},\"id\":\""
						+ id.toString() + "\",\"timeout\":10}",
				json);

		// Test from()
		sut = GenericJsonrpcRequest.from(json);
		Assert.assertEquals(id, sut.getId());
		Assert.assertEquals(method, sut.getMethod());
		Assert.assertEquals(Optional.of(timeout), sut.getTimeout());
		Assert.assertEquals("{\"hello\":\"world\",\"foo\":\"bar\"}", sut.getParams().toString());

	}

}
