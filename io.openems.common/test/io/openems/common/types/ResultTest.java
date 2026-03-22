package io.openems.common.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ResultTest {

	@Test
	public void testOk() throws Exception {
		var result = Result.ok("Okay");
		assertEquals("Okay", result.getOrThrow());
		assertTrue(result.isOk());
		assertFalse(result.isError());
		assertTrue(result.get().isPresent());
		assertEquals("Okay", result.get().get());
		assertEquals("Okay", result.getOrElse("Not okay"));
		result.throwError();
		assertEquals("Still Okay", result.map(x -> {
			if (x.equals("Okay")) {
				return "Still Okay";
			}
			return "NOT OKAY";
		}).getOrThrow());
	}

	@Test
	public void testFailure() {
		var result = Result.<String>error(new RuntimeException("Test"));
		assertThrows(RuntimeException.class, () -> result.throwError());
		assertThrows(RuntimeException.class, () -> result.getOrThrow());
		assertTrue(result.isError());
		assertFalse(result.isOk());
		assertTrue(result.get().isEmpty());
		assertTrue(result.map(x -> "mapped").isError());
	}
}
