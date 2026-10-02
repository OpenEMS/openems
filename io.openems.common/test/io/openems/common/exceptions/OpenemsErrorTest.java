package io.openems.common.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.openems.common.session.Language;

class OpenemsErrorTest {

	private static final String DUMMY_APP_ID = "dummyApp";

	@Test
	void testTranslatedValidationError() {
		final var singleCardinalityException = OpenemsError.EDGE_APP_VALIDATION_CHECK_CARDINALITY_SINGLE
				.exception(Language.DEFAULT, DUMMY_APP_ID);
		final var thirdPartyException = OpenemsError.EDGE_APP_VALIDATION_CHECK_3RD_PARTY_ACCESS_ACCEPTED
				.exception(Language.EN);

		assertEquals("Es ist bereits eine App dummyApp installiert.", singleCardinalityException.getMessage());
		assertEquals(
				"This App requires access to third-party systems. Please confirm <a class=\"open-modal-thirdpartyaccess\">here</a>, that you agree.",
				thirdPartyException.getMessage());
	}
}
