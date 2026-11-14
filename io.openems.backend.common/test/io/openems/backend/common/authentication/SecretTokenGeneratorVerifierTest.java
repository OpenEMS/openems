package io.openems.backend.common.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import io.openems.common.test.TimeLeapClock;

class SecretTokenGeneratorVerifierTest {

	@Test
	void testTokenExpired() {
		final var id = "id";
		final var secret = "my-top-secret-secret-0123456789ab";
		final var testClock = new TimeLeapClock(Instant.now());
		try (MockedStatic<Clock> clock = Mockito.mockStatic(Clock.class)) {
			clock.when(Clock::systemUTC).thenReturn(testClock);

			final var verifier = new SecretTokenVerifier(secret);
			final var token = SecretTokenGenerator.generateAuthToken(id, secret);

			assertEquals(id, verifier.verify(token));
			testClock.leap(4 * 60 + 59, ChronoUnit.SECONDS);
			assertEquals(id, verifier.verify(token));
			testClock.leap(1, ChronoUnit.SECONDS);
			assertNull(verifier.verify(token));
		}
	}

	@Test
	void testInvalidSecret() {
		final var testClock = new TimeLeapClock(Instant.now());
		try (MockedStatic<Clock> clock = Mockito.mockStatic(Clock.class)) {
			clock.when(Clock::systemUTC).thenReturn(testClock);

			final var verifier = new SecretTokenVerifier("my-top-secret-secret-0123456789ab");
			final var token = SecretTokenGenerator.generateAuthToken("id", "my-top-secret-secret-0123456789ac");

			assertNull(verifier.verify(token));
		}
	}

	@Test
	void testInvalidSecretRejected() {
		assertThrows(IllegalArgumentException.class, () -> new SecretTokenVerifier(null));
		assertThrows(IllegalArgumentException.class, () -> new SecretTokenVerifier("too-short-secret"));
		assertThrows(IllegalArgumentException.class, () -> SecretTokenGenerator.generateAuthToken("id", null));
		assertThrows(IllegalArgumentException.class,
				() -> SecretTokenGenerator.generateAuthToken("id", "too-short-secret"));
	}

}
