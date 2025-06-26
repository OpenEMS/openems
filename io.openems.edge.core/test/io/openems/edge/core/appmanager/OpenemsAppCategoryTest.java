package io.openems.edge.core.appmanager;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.common.session.Language;

public class OpenemsAppCategoryTest {

	@Test
	public void testEnglishTranslations() {
		this.testTranslations(Language.EN);
	}

	@Test
	public void testGermanTranslations() {
		this.testTranslations(Language.DE);
	}

	private void testTranslations(Language l) {
		final var debugTranslator = TranslationUtil.enableDebugMode();

		for (var entry : OpenemsAppCategory.values()) {
			entry.getReadableName(l);
		}

		assertTrue(
				"Missing Translation Keys for Language " + l + " ["
						+ String.join(", ", debugTranslator.getMissingKeys()) + "]",
				debugTranslator.getMissingKeys().isEmpty());
	}
}
