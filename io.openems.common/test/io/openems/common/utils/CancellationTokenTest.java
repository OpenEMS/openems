package io.openems.common.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CancellationTokenTest {

	@Test
	void testCancellationToken() {
		final var token = new CancellationToken();
		assertFalse(token.isCancelled());
		assertDoesNotThrow(token::throwIfCancelled);
		token.cancel();
		assertTrue(token.isCancelled());
		assertThrows(CancellationToken.CancellationException.class, token::throwIfCancelled);
	}

	@Test
	void testDuplicatedCancel() {
		final var token = new CancellationToken();
		assertFalse(token.isCancelled());
		assertDoesNotThrow(token::throwIfCancelled);
		token.cancel();
		assertTrue(token.isCancelled());
		assertThrows(CancellationToken.CancellationException.class, token::throwIfCancelled);
		token.cancel();
		assertTrue(token.isCancelled());
		assertThrows(CancellationToken.CancellationException.class, token::throwIfCancelled);
	}

}