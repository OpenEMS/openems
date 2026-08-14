package io.openems.edge.app.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CleverPvUrlTest {

	private static final String CORRECT_URL = "https://push.clever-pv.com/api/v1/c343d418-2945-4030-9715-cbf998d07dde/electricMeters/fenecon/0c8baba5-e16c-4ab7-a722-b96df0628788?code=21b2f1dd-3c4e-4fa8-90d2-4003102e43ec";
	private static final String CORRECT_URL_V2 = "https://push.clever-pv.com/api/v2/c343d418-2945-4030-9715-cbf998d07dde/electricMeters/fenecon/0c8baba5-e16c-4ab7-a722-b96df0628788?code=21b2f1dd-3c4e-4fa8-90d2-4003102e43ec";
	private static final String INCOMPLETE_URL = "https://push.clever-pv.com/api/v1/c343d418-2945-4030-9715-cbf998d07dde/electricMeters/fenecon/0c8baba5-e16c-4ab7-a722-b96df0628788?code=";

	@Test
	void testCleverPvUrl() {
		assertTrue(CleverPvUrl.isValid(CORRECT_URL));
		assertTrue(CleverPvUrl.isValid(CORRECT_URL_V2));
		assertFalse(CleverPvUrl.isValid(INCOMPLETE_URL));
	}

}