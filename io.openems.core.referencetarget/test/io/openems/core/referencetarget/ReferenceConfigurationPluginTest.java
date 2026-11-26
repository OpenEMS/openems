package io.openems.core.referencetarget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Hashtable;

import org.junit.jupiter.api.Test;

class ReferenceConfigurationPluginTest {

	@Test
	void replaceTargetPropertyIgnoringCase() {
		final var properties = new Hashtable<String, Object>();
		properties.put("Ess.target", "old-filter");
		properties.put("some.other.property", "keep");

		ReferenceConfigurationPlugin.setTargetProperty(properties, "ess.target", "new-filter");

		assertEquals("new-filter", properties.get("ess.target"));
		assertNull(properties.get("Ess.target"));
		assertEquals("keep", properties.get("some.other.property"));
		assertEquals(2, properties.size());
	}

}
