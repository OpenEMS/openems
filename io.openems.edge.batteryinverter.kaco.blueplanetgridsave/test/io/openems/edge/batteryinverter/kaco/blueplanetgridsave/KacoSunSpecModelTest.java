package io.openems.edge.batteryinverter.kaco.blueplanetgridsave;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import io.openems.common.session.Language;

public class KacoSunSpecModelTest {

	@Test
	public void testS64201StVndTranslationsDe() {
		assertS64201StVndTranslations(Language.DE);
	}

	@Test
	public void testS64201StVndTranslationsEn() {
		assertS64201StVndTranslations(Language.EN);
	}

	private static void assertS64201StVndTranslations(Language language) {
		final var missingKeys = Arrays.stream(KacoSunSpecModel.S64201.S64201StVnd.values()) //
				.filter(t -> t.getChannelId().doc().getText(language).equals(t.get().channelKey)) //
				.map(Enum::name) //
				.toList();

		assertTrue("Missing " + language + " translations for[" + missingKeys.size() + "]: "
				+ String.join(", ", missingKeys), missingKeys.isEmpty());
	}

}