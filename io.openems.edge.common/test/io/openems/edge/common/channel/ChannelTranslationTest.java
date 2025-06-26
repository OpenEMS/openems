package io.openems.edge.common.channel;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.channel.Level;
import io.openems.common.session.Language;

public class ChannelTranslationTest {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		TEST_CHANNEL(Doc.of(Level.WARNING) //
				.translationKey(ChannelTranslationTest.class, "Test.TestChannel")), //
		ONLY_ENGLISH(Doc.of(Level.INFO) //
				.translationKey(ChannelTranslationTest.class, "Test.OnlyEnglish")), //
		NO_TRANSLATION(Doc.of(Level.OK) //
				.text("No Translation")),;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	@Test
	public void testTranslatedGermanChannelText() {
		assertEquals("German Test", ChannelTranslationTest.ChannelId.TEST_CHANNEL.doc().getText(Language.DE));
	}

	@Test
	public void testTranslatedEnglishChannelText() {
		assertEquals("English Test", ChannelTranslationTest.ChannelId.TEST_CHANNEL.doc().getText(Language.EN));
	}

	@Test
	public void testOnlyEnglishTranslationTest() {
		assertEquals("Only English", ChannelTranslationTest.ChannelId.ONLY_ENGLISH.doc().getText(Language.DE));
	}

	@Test
	public void testNoTranslation() {
		assertEquals("No Translation", ChannelTranslationTest.ChannelId.NO_TRANSLATION.doc().getText(Language.DE));
	}
}
