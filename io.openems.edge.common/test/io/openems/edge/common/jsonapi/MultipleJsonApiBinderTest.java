package io.openems.edge.common.jsonapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.common.utils.FunctionUtils;

public class MultipleJsonApiBinderTest {

	@Test
	public void testBindJsonApi() {
		final var binder = new MultipleJsonApiBinder();

		binder.bindJsonApi(new DummyJsonApi(new JsonApiBuilder().rpc("method1", FunctionUtils::doNothing)));
		binder.bindJsonApi(new DummyJsonApi(new JsonApiBuilder().rpc("method2", FunctionUtils::doNothing)));

		assertTrue(binder.getJsonApiBuilder().getEndpoints().containsKey("method1"));
		assertTrue(binder.getJsonApiBuilder().getEndpoints().containsKey("method2"));
		assertEquals(2, binder.getJsonApiBuilder().getEndpoints().size());

		binder.bindJsonApi(new DummyJsonApi(new JsonApiBuilder().rpc("method3", FunctionUtils::doNothing)));

		assertTrue(binder.getJsonApiBuilder().getEndpoints().containsKey("method3"));
		assertEquals(3, binder.getJsonApiBuilder().getEndpoints().size());
	}

	@Test
	public void testUnbindJsonApi() {
		final var binder = new MultipleJsonApiBinder();

		final var dummyApi1 = new DummyJsonApi(new JsonApiBuilder().rpc("method1", FunctionUtils::doNothing));
		binder.bindJsonApi(dummyApi1);
		final var dummyApi2 = new DummyJsonApi(new JsonApiBuilder().rpc("method2", FunctionUtils::doNothing));
		binder.bindJsonApi(dummyApi2);

		assertTrue(binder.getJsonApiBuilder().getEndpoints().containsKey("method1"));
		assertTrue(binder.getJsonApiBuilder().getEndpoints().containsKey("method2"));
		assertEquals(2, binder.getJsonApiBuilder().getEndpoints().size());

		final var dummyApi3 = new DummyJsonApi(new JsonApiBuilder().rpc("method3", FunctionUtils::doNothing));
		binder.bindJsonApi(dummyApi3);

		assertTrue(binder.getJsonApiBuilder().getEndpoints().containsKey("method3"));
		assertEquals(3, binder.getJsonApiBuilder().getEndpoints().size());

		binder.unbindJsonApi(dummyApi2);
		assertTrue(binder.getJsonApiBuilder().getEndpoints().containsKey("method1"));
		assertTrue(binder.getJsonApiBuilder().getEndpoints().containsKey("method3"));
		assertEquals(2, binder.getJsonApiBuilder().getEndpoints().size());
	}

}
