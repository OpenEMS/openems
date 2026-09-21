package io.openems.core.referencetarget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Hashtable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.service.metatype.ObjectClassDefinition;

import io.openems.common.test.DummyAttributeDefinition;
import io.openems.common.test.DummyObjectClassDefinition;

class ValueProviderFromConfigTest {

	private static final ObjectClassDefinition DUMMY_OBJECT_CLASS_DEFINITION = DummyObjectClassDefinition.EMPTY
			.withAttributes(//
					DummyAttributeDefinition.EMPTY.withId("meter.id") //
							.withDefaultValue("meter0"), //
					DummyAttributeDefinition.EMPTY.withId("batteryInverter.id") //
							.withDefaultValue("batteryInverter0") //
			);

	private ValueProviderFromConfig valueProvider;

	@BeforeEach
	void beforeEach() {
		final var props = new Hashtable<String, Object>();
		props.put("meter.id", "meter3");
		this.valueProvider = new ValueProviderFromConfig(props, () -> DUMMY_OBJECT_CLASS_DEFINITION);
	}

	@Test
	void testConfigValue() {
		assertEquals("meter3", this.valueProvider.getValue("meter_id"));
	}

	@Test
	void testDefaultValue() {
		assertEquals("batteryInverter0", this.valueProvider.getValue("batteryInverter_id"));
	}

	@Test
	void testValueNotAvailable() {
		assertNull(this.valueProvider.getValue("ess_id"));
	}

}