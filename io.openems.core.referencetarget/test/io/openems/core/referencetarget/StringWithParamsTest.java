package io.openems.core.referencetarget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
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

	@Test
	public void testTransformerExpression() {
		final var s = new StringWithParams("(id=${config.relays;componentIdFromChannel})");

		final var parameter = s.parameter().getFirst();
		assertEquals("config", parameter.topic());
		assertEquals("relays", parameter.variable());
		assertEquals(List.of("componentIdFromChannel"), parameter.transformers());

		final var result = s.withParameters(Map.of(parameter, "io0"));
		assertEquals("(id=io0)", result);
	}

	@Test
	public void testChainedTransformerNames() {
		final var s = new StringWithParams("(id=${config.relays; first ; second })");

		assertEquals(List.of("first", "second"), s.parameter().getFirst().transformers());
	}

	@Test
	public void testEmptyTransformerRejected() {
		assertThrows(IllegalArgumentException.class,
				() -> new StringWithParams("(id=${config.relays;})"));
	}
}