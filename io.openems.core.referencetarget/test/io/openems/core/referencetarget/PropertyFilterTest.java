package io.openems.core.referencetarget;

import static io.openems.core.referencetarget.PropertyFilter.fromGenerateTargets;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PropertyFilterTest {

	@Test
	public void testFromGenerateTargets() {
		final var result = fromGenerateTargets(new String[] { "component=(id=${config.component_id})" });

		assertEquals(1, result.size());
		final var propertyFilter = result.getFirst();
		assertEquals("component", propertyFilter.property());
		assertEquals(1, propertyFilter.targetTemplate().parameter().size());
		final var parameter = propertyFilter.targetTemplate().parameter().getFirst();
		assertEquals("config", parameter.topic());
		assertEquals("component_id", parameter.variable());
	}

}