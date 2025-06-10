package io.openems.edge.common.jsonapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.common.jsonrpc.serialization.EmptyObject;

public class EndpointRequestDefinitionBuilderTest {

	@Test
	public void testSetGetSerializer() {
		final var defBuilder = new EndpointRequestDefinitionBuilder<EmptyObject>();
		defBuilder.setSerializer(EmptyObject.serializer());
		assertNotNull(defBuilder.getSerializer());
	}

	@Test
	public void testAddExampleStringRequest() {
		final var defBuilder = new EndpointRequestDefinitionBuilder<EmptyObject>();

		final var exampleKey = "exampleKey";
		final var exampleObject = EmptyObject.INSTANCE;
		defBuilder.addExample(exampleKey, exampleObject);
		assertEquals(1, defBuilder.getExamples().size());
		assertEquals(exampleKey, defBuilder.getExamples().get(0).identifier());
		assertEquals(exampleObject, defBuilder.getExamples().get(0).exampleObject());
	}

	@Test
	public void testAddExampleRequest() {
		final var defBuilder = new EndpointRequestDefinitionBuilder<EmptyObject>();

		final var exampleObject = EmptyObject.INSTANCE;
		defBuilder.addExample(exampleObject);
		assertEquals(1, defBuilder.getExamples().size());
		assertEquals(exampleObject, defBuilder.getExamples().get(0).exampleObject());
	}

	@Test
	public void testCreateExampleArray() {
		final var defBuilder = new EndpointRequestDefinitionBuilder<EmptyObject>();
		defBuilder.addExample(EmptyObject.INSTANCE);
		defBuilder.setSerializer(EmptyObject.serializer());

		assertEquals(2, defBuilder.createExampleArray().size());
	}

}
