package io.openems.edge.common.jsonapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class EndpointResponseDefinitionBuilderTest {

	@Test
	public void testSetGetSerializer() {
		final var defBuilder = new EndpointResponseDefinitionBuilder<EmptyObject>();
		defBuilder.setSerializer(EmptyObject.serializer());
		assertNotNull(defBuilder.getSerializer());
	}

	@Test
	public void testAddExampleStringResponse() {
		final var defBuilder = new EndpointResponseDefinitionBuilder<EmptyObject>();

		final var exampleKey = "exampleKey";
		final var exampleObject = new EmptyObject();
		defBuilder.addExample(exampleKey, exampleObject);
		assertEquals(1, defBuilder.getExamples().size());
		assertEquals(exampleKey, defBuilder.getExamples().get(0).identifier());
		assertEquals(exampleObject, defBuilder.getExamples().get(0).exampleObject());
	}

	@Test
	public void testAddExampleResponse() {
		final var defBuilder = new EndpointResponseDefinitionBuilder<EmptyObject>();

		final var exampleObject = new EmptyObject();
		defBuilder.addExample(exampleObject);
		assertEquals(1, defBuilder.getExamples().size());
		assertEquals(exampleObject, defBuilder.getExamples().get(0).exampleObject());
	}

	@Test
	public void testCreateExampleArray() {
		final var defBuilder = new EndpointResponseDefinitionBuilder<EmptyObject>();
		defBuilder.addExample(new EmptyObject());
		defBuilder.setSerializer(EmptyObject.serializer());

		assertEquals(1, defBuilder.createExampleArray().size());
	}

}
