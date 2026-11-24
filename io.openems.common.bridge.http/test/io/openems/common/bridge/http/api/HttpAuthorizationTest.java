package io.openems.common.bridge.http.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HttpAuthorizationTest {

	@Test
	void testBearer() {
		assertEquals("Bearer my-token", HttpAuthorization.bearer("my-token"));
	}

	@Test
	void testIsBearer() {
		assertTrue(HttpAuthorization.isBearer("Bearer my-token"));
		// auth scheme is case-insensitive, see RFC 6750
		assertTrue(HttpAuthorization.isBearer("bearer my-token"));
		assertTrue(HttpAuthorization.isBearer("BEARER my-token"));
		assertFalse(HttpAuthorization.isBearer("Basic abc123"));
		assertFalse(HttpAuthorization.isBearer("Bearer")); // missing trailing space
		assertFalse(HttpAuthorization.isBearer(null));
	}

	@Test
	void testExtractBearerToken() {
		assertEquals("my-token", HttpAuthorization.extractBearerToken("Bearer my-token"));
		assertEquals("my-token", HttpAuthorization.extractBearerToken("bearer my-token"));
		assertEquals("", HttpAuthorization.extractBearerToken("Bearer "));
		assertNull(HttpAuthorization.extractBearerToken("Basic abc123"));
		assertNull(HttpAuthorization.extractBearerToken(null));
	}
}
