package io.openems.core.referencetarget;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

class StringWithParamsTest {

	@Test
	public void testSingleArgument() {
		final var s = new StringWithParams("(id=${config.id})");

		assertEquals(1, s.parameter().size());
		final var result = s.withParameters(Map.of(//
				s.parameter().getFirst(), "component0") //
		);
		assertEquals("(id=component0)", result);
	}

	@Test
	public void testArrayArgument() {
		final var s = new StringWithParams("(id=${config.ids})");

		assertEquals(1, s.parameter().size());
		final var result = s.withParameters(Map.of(//
				s.parameter().getFirst(), new String[] { "component0", "component1" }) //
		);
		assertEquals("(|(id=component0)(id=component1))", result);
	}

	@Test
	public void testArraySingleValueArgument() {
		final var s = new StringWithParams("(id=${config.ids})");

		assertEquals(1, s.parameter().size());
		final var result = s.withParameters(Map.of(//
				s.parameter().getFirst(), new String[] { "component0" }) //
		);
		assertEquals("(id=component0)", result);
	}

	@Test
	public void testArrayEmptyValueArgument() {
		final var s = new StringWithParams("(id=${config.ids})");

		assertEquals(1, s.parameter().size());
		final var result = s.withParameters(Map.of(//
				s.parameter().getFirst(), new String[] {}) //
		);
		assertEquals("(id=)", result);
	}

}