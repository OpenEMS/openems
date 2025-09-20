package io.openems.edge.core.appmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.stream.Stream;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.common.session.Language;

public final class AssertOpenemsAppPropertyDefinition {

	/**
	 * Checks if the given prop exists in the props array and has the
	 * expectedDefaultValue as their default value.
	 * 
	 * @param props                the array of {@link OpenemsAppPropertyDefinition}
	 * @param prop                 the {@link Nameable}
	 * @param expectedDefaultValue the expected default value
	 */
	public static void assertPropertyDefaultValue(//
			OpenemsAppPropertyDefinition[] props, //
			Nameable prop, //
			String expectedDefaultValue //
	) {
		assertPropertyDefaultValue(props, prop, new JsonPrimitive(expectedDefaultValue));
	}

	/**
	 * Checks if the given prop exists in the props array and has the
	 * expectedDefaultValue as their default value.
	 * 
	 * @param props                the array of {@link OpenemsAppPropertyDefinition}
	 * @param prop                 the {@link Nameable}
	 * @param expectedDefaultValue the expected default value
	 */
	public static void assertPropertyDefaultValue(//
			OpenemsAppPropertyDefinition[] props, //
			Nameable prop, //
			JsonElement expectedDefaultValue //
	) {
		final var outputChannel = Stream.of(props).filter(t -> t.name.equals(prop.name())) //
				.findAny().orElse(null);

		assertNotNull(outputChannel);

		final var defaultValue = outputChannel.getDefaultValue(Language.DEFAULT);
		assertTrue(defaultValue.isPresent());
		assertEquals(expectedDefaultValue, defaultValue.get());
	}

	private AssertOpenemsAppPropertyDefinition() {
	}

}
