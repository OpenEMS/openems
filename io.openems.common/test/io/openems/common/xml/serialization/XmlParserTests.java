package io.openems.common.xml.serialization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedRuntimeException;

public class XmlParserTests {
	private static final String XML_1 = """
			<?xml version="1.0" encoding="UTF-8"?>
			<root>
				<intval>12345</intval>
				<doubleval>56765.56</doubleval>
				<stringval>HANS</stringval>
				<emptyval></emptyval>
				<ratings>
					<rating name="P1">
						<power>123.5</power>
						<voltage>223</voltage>
					</rating>
					<rating name="P2">
						<power>433.5</power>
						<voltage>230</voltage>
					</rating>
				</ratings>
			</root>
			""";

	@Test
	public void testParser() throws OpenemsNamedException {
		assertThrows(OpenemsNamedException.class, () -> XmlParser.INSTANCE.parseXml("This.Is.Not.A.Valid.XML"));

		var xml = XmlParser.INSTANCE.parseXml(XML_1);
		assertEquals(12345, xml.getChild("intval").getValueAsInt());
		assertEquals(56765.56, xml.getChild("doubleval").getValueAsDouble(), 0.001);
		assertEquals("HANS", xml.getChild("stringval").getValue());
		assertEquals("", xml.getChild("emptyval").getValue());
		assertFalse(xml.hasChild("Does.Not.Exist"));
		assertTrue(xml.hasChild("intval"));
		assertThrows(OpenemsNamedRuntimeException.class, () -> xml.getChild("doesNotExist"));

		var ratings = xml.getChildObject("ratings").getChildObjects("rating");
		assertEquals(2, ratings.size());

		assertEquals("P1", ratings.get(0).getAttribute("name"));
		assertEquals(123.5, ratings.get(0).getChild("power").getValueAsDouble(), 0.001);
		assertEquals(223, ratings.get(0).getChild("voltage").getValueAsInt());

		assertEquals("P2", ratings.get(1).getAttribute("name"));
		assertEquals(433.5, ratings.get(1).getChild("power").getValueAsDouble(), 0.001);
		assertEquals(230, ratings.get(1).getChild("voltage").getValueAsInt());
	}
}
