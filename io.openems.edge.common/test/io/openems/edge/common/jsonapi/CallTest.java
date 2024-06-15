package io.openems.edge.common.jsonapi;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;

public class CallTest {

	@Test
	public void testGetSetResponse() {
		final var call = new Call<JsonrpcRequest, JsonrpcResponse>(
				new GenericJsonrpcRequest("method", new JsonObject()));

		final var exampleResponse = new GenericJsonrpcResponseSuccess(call.getRequest().getId());
		call.setResponse(exampleResponse);

		assertEquals(exampleResponse, call.getResponse());
	}

	@Test
	public void testGetPutProperties() {
		final var call = new Call<JsonrpcRequest, JsonrpcResponse>(
				new GenericJsonrpcRequest("method", new JsonObject()));

		final var dummyKey = new Key<>("dummy", String.class);

		final var dummyValue = "dummyValue";
		call.put(dummyKey, dummyValue);
		assertEquals(dummyValue, call.get(dummyKey));
	}

	@Test
	public void testMapRequest() {
		final var originalRequest = new GenericJsonrpcRequest("method", new JsonObject());
		final var call = new Call<JsonrpcRequest, JsonrpcResponse>(originalRequest);
		class DummyRequestClass {

		}
		
		final var dummyRequest = new DummyRequestClass();
		final var newCall = call.mapRequest(dummyRequest);

		assertEquals(dummyRequest, newCall.getRequest());
		assertEquals(originalRequest, call.getRequest());
	}

	@Test
	public void testMapResponse() {
		final var call = new Call<JsonrpcRequest, JsonrpcResponse>(
				new GenericJsonrpcRequest("method", new JsonObject()));
		class DummyResponseClass {

		}
		
		final var mappedCall = call.<DummyResponseClass>mapResponse();

		final var originalResponse = new GenericJsonrpcResponseSuccess(call.getRequest().getId());
		call.setResponse(originalResponse);
		final var mappedResponse = new DummyResponseClass();
		mappedCall.setResponse(mappedResponse);

		assertEquals(originalResponse, call.getResponse());
		assertEquals(mappedResponse, mappedCall.getResponse());
	}

	@Test
	public void testGetRequest() {
		final var exampleRequest = new GenericJsonrpcRequest("method", new JsonObject());
		final var call = new Call<JsonrpcRequest, JsonrpcResponse>(exampleRequest);

		assertEquals(exampleRequest, call.getRequest());
	}

}
