package io.openems.edge.common.jsonapi;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.junit.Test;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;
import io.openems.common.jsonrpc.base.JsonrpcResponseError;
import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.utils.FunctionUtils;

public class JsonApiBuilderTest {

	@Test
	public void testAddMethodHandler() {
		final var builder = new JsonApiBuilder();

		final var testMethod = "method";
		builder.handleRequest(testMethod, _ -> null);

		assertEquals(1, builder.getEndpoints().size());
		final var addedEndpoint = builder.getEndpoints().get(testMethod);
		assertNotNull(addedEndpoint);
	}

	@Test
	public void testAddMethodHandler2() {
		final var builder = new JsonApiBuilder();

		final var testMethod = "method";
		final var dummyDescription = "description";
		builder.rpc(testMethod, endpoint -> {
			endpoint.setDescription(dummyDescription);
		}, FunctionUtils::doNothing);

		assertEquals(1, builder.getEndpoints().size());
		final var addedEndpoint = builder.getEndpoints().get(testMethod);
		assertNotNull(addedEndpoint);
		assertEquals(dummyDescription, addedEndpoint.getDef().getDescription());
	}

	@Test
	public void testAddMethodHandler3() {
		final var builder = new JsonApiBuilder();

		final var testMethod = "method";
		final var dummyDescription = "description";
		builder.rpc(testMethod, endpoint -> {
			endpoint.setDescription(dummyDescription);
		}, () -> List.of(new Subrequest(new JsonObject())), FunctionUtils::doNothing);

		assertEquals(1, builder.getEndpoints().size());
		final var addedEndpoint = builder.getEndpoints().get(testMethod);
		assertNotNull(addedEndpoint);
		assertEquals(dummyDescription, addedEndpoint.getDef().getDescription());
		final var subroutes = addedEndpoint.getSubroutes().get();
		assertEquals(1, subroutes.size());
	}

	@Test
	public void testAddMethodHandler4() {
		final var builder = new JsonApiBuilder();

		final var testMethod = "method";
		final var dummyDescription = "description";
		builder.rpc(testMethod, endpoint -> {
			endpoint.setDescription(dummyDescription);
		}, () -> List.of(new Subrequest(new JsonObject())), FunctionUtils::doNothing, EmptyObject.serializer());

		assertEquals(1, builder.getEndpoints().size());
		final var addedEndpoint = builder.getEndpoints().get(testMethod);
		assertNotNull(addedEndpoint);
		assertEquals(dummyDescription, addedEndpoint.getDef().getDescription());
		assertNotNull(addedEndpoint.getDef().getEndpointRequestBuilder().getSerializer());
		final var subroutes = addedEndpoint.getSubroutes().get();
		assertEquals(1, subroutes.size());
	}

	@Test
	public void testAddMethodHandler5() {
		final var builder = new JsonApiBuilder();

		final var testMethod = "method";
		final var dummyDescription = "description";
		builder.rpc(testMethod, endpoint -> {
			endpoint.setDescription(dummyDescription);
		}, FunctionUtils::doNothing, EmptyObject.serializer(), EmptyObject.serializer());

		assertEquals(1, builder.getEndpoints().size());
		final var addedEndpoint = builder.getEndpoints().get(testMethod);
		assertNotNull(addedEndpoint);
		assertEquals(dummyDescription, addedEndpoint.getDef().getDescription());
		assertNotNull(addedEndpoint.getDef().getEndpointRequestBuilder().getSerializer());
		assertNotNull(addedEndpoint.getDef().getEndpointResponseBuilder().getSerializer());
	}

	@Test
	public void testDelegate() throws Exception {
		final var builder = new JsonApiBuilder();

		final var testMethod = "method";
		final var dummyDescription = "description";
		builder.delegate(testMethod, endpoint -> {
			endpoint.setDescription(dummyDescription);
		}, _ -> {
			return new GenericJsonrpcRequest("method2", new JsonObject());
		}, Function.identity(), Function.identity(), () -> emptyList());

		final var isMethod2Called = new AtomicBoolean(false);
		builder.handleRequest("method2", _ -> {
			isMethod2Called.set(true);
			return null;
		});

		assertEquals(2, builder.getEndpoints().size());
		final var addedEndpoint = builder.getEndpoints().get(testMethod);
		assertNotNull(addedEndpoint);
		assertEquals(dummyDescription, addedEndpoint.getDef().getDescription());

		final var testCall = new Call<JsonrpcRequest, JsonrpcResponse>(
				new GenericJsonrpcRequest(testMethod, new JsonObject()));
		builder.handle(testCall);
		assertTrue(isMethod2Called.get());
	}

	@Test
	public void testDelegateRunsGuards() throws Exception {
		final var builder = new JsonApiBuilder();

		final var testMethod = "method";
		builder.delegate(testMethod, endpoint -> {
			endpoint.setGuards(_ -> {
				throw new OpenemsException("Access denied");
			});
		}, _ -> {
			return new GenericJsonrpcRequest("method2", new JsonObject());
		}, Function.identity(), Function.identity(), () -> emptyList());

		final var isMethod2Called = new AtomicBoolean(false);
		builder.handleRequest("method2", _ -> {
			isMethod2Called.set(true);
			return null;
		});

		final var testCall = new Call<JsonrpcRequest, JsonrpcResponse>(
				new GenericJsonrpcRequest(testMethod, new JsonObject()));
		builder.handle(testCall);

		assertFalse("A failing guard must stop the delegated request", isMethod2Called.get());
		assertNotNull("A failing guard must produce an error response", testCall.getResponse());
		assertTrue(testCall.getResponse() instanceof JsonrpcResponseError);
	}

	@Test
	public void testCallEndpoint() {
		final var builder = new JsonApiBuilder();

		final var testMethod = "method";
		builder.handleRequest(testMethod, call -> {
			return new GenericJsonrpcResponseSuccess(call.getRequest().getId());
		});

		assertEquals(1, builder.getEndpoints().size());
		final var addedEndpoint = builder.getEndpoints().get(testMethod);
		assertNotNull(addedEndpoint);

		final var testCall = new Call<JsonrpcRequest, JsonrpcResponse>(
				new GenericJsonrpcRequest(testMethod, new JsonObject()));
		builder.handle(testCall);

		assertNotNull(testCall.getResponse());
	}

	@Test
	public void testRemoveEndpoint() throws Exception {
		final var builder = new JsonApiBuilder();

		final var testMethod = "method";
		builder.handleRequest(testMethod, _ -> null);

		assertEquals(1, builder.getEndpoints().size());

		builder.removeEndpoint(testMethod);
		assertEquals(0, builder.getEndpoints().size());
	}

	@Test
	public void testAddBuilder() throws Exception {
		final var builder = new JsonApiBuilder();
		final var subBuilder = new JsonApiBuilder();

		builder.addBuilder(subBuilder);

		final var testMethod = "subMethod";
		subBuilder.handleRequest(testMethod, call -> {
			return new GenericJsonrpcResponseSuccess(call.getRequest().getId());
		});

		final var testCall = new Call<JsonrpcRequest, JsonrpcResponse>(
				new GenericJsonrpcRequest(testMethod, new JsonObject()));
		builder.handle(testCall);

		assertNotNull(testCall.getResponse());
	}

}
