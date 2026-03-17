package io.openems.edge.common.type;

import static org.junit.Assert.assertEquals;

import io.openems.common.session.Language;
import org.junit.Test;

public class TextProviderTest {
	@Test
	public void testTextProvider() {
		var translationProvider = TextProvider.byTranslation(TextProviderTest.class, "Testing");
		assertEquals("Das ist ein deutscher Text", translationProvider.getText(Language.DE));
		assertEquals("This is an English test", translationProvider.getText(Language.EN));
		assertEquals("Das ist ein deutscher Text", translationProvider.getText(null));

		var staticProvider = TextProvider.byStatic("Testing");
		assertEquals("Testing", staticProvider.getText(Language.DE));
		assertEquals("Testing", staticProvider.getText(null));
	}
}
