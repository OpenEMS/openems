package io.openems.common.jsonrpc.base;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.request.EdgeRpcRequest;

public class AbstractJsonrpcRequestTest {

	@Test
	public void testGetFullyQualifiedMethod() {
		assertEquals("foo", //
				new GenericJsonrpcRequest("foo", new JsonObject()) //
						.getFullyQualifiedMethod());

		assertEquals("edgeRpc/foo", //
				new EdgeRpcRequest("edge0", //
						new GenericJsonrpcRequest("foo", new JsonObject())) //
						.getFullyQualifiedMethod());

		assertEquals("edgeRpc/edgeRpc/foo", //
				new EdgeRpcRequest("edge0", //
						new EdgeRpcRequest("edge1", //
								new GenericJsonrpcRequest("foo", new JsonObject()))) //
						.getFullyQualifiedMethod());
	}

}
