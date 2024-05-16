package io.openems.edge.common.jsonapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Optional;
import java.util.UUID;

import org.junit.Test;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;

public class SingleJsonApiBinderTest {

	@Test
	public void testBind() throws Exception {
		final var binder = new SingleJsonApiBinder();

		final var id = UUID.randomUUID();
		final var response = new GenericJsonrpcResponseSuccess(id);
		binder.bind(new DummyJsonApi(new JsonApiBuilder().handleRequest("method", call -> response)));

		final var handledResponse = binder
				.handleRequest(new GenericJsonrpcRequest(id, "method", new JsonObject(), Optional.empty())).get();
		assertEquals(response.getResult(), handledResponse.getResult());
	}

	@Test
	public void testUnbind() throws Exception {
		final var binder = new SingleJsonApiBinder();

		final var id = UUID.randomUUID();
		final var response = new GenericJsonrpcResponseSuccess(id);
		binder.bind(new DummyJsonApi(new JsonApiBuilder().handleRequest("method", call -> response)));

		final var handledResponse = binder
				.handleRequest(new GenericJsonrpcRequest(id, "method", new JsonObject(), Optional.empty())).get();
		assertEquals(response.getResult(), handledResponse.getResult());

		binder.unbind();

		OpenemsNamedException exception = null;
		try {
			binder.handleRequest(new GenericJsonrpcRequest("method", new JsonObject())).get();
		} catch (OpenemsNamedException e) {
			exception = e;
		}
		assertNotNull(exception);
	}

}
