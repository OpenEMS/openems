package io.openems.edge.common.meta.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.regex.Pattern;

import org.junit.Test;

public class SubdivisionCodeTest {

	@Test
	public void testSubdivisionCode_ShouldFollowNamingConvention() {
		var pattern = Pattern.compile("^[A-Z]{2}_(?:[A-Z]{1,2}|[0-9]{1,2})$");
		for (var country : SubdivisionCode.values()) {
			if (country == SubdivisionCode.UNDEFINED) {
				continue;
			}
			assertTrue(pattern.matcher(country.name()).matches());
		}
	}

	@Test
	public void testGetCode_ShouldReplaceUnderscoreWithDash() {
		assertEquals("DE-BY", SubdivisionCode.DE_BY.getCode());
	}

	@Test
	public void testToString_ShouldReturnCode() {
		assertEquals("DE-BY", SubdivisionCode.DE_BY.toString());
	}

	@Test
	public void testGetSubdivisionPart_ShouldReturnRightPart() {
		assertEquals("BY", SubdivisionCode.DE_BY.getSubdivisionPart());
		assertEquals("BE", SubdivisionCode.DE_BE.getSubdivisionPart());
	}

	@Test
	public void testGetSubdivisionPart_ShouldReturnNullForUndefined() {
		assertNull(SubdivisionCode.UNDEFINED.getSubdivisionPart());
	}

	@Test
	public void testFromCode_ShouldReturnCorrectSubdivision() {
		assertEquals(SubdivisionCode.DE_BY, SubdivisionCode.fromCode("DE-BY"));
		assertEquals(SubdivisionCode.DE_BY, SubdivisionCode.fromCode("de-by"));
		assertEquals(SubdivisionCode.DE_BY, SubdivisionCode.fromCode(" De-By "));
		assertEquals(SubdivisionCode.DE_BY, SubdivisionCode.fromCode("DE_BY"));
	}

	@Test
	public void testFromCode_ShouldReturnUndefinedForInvalidInput() {
		assertEquals(SubdivisionCode.UNDEFINED, SubdivisionCode.fromCode("XX-YY"));
		assertEquals(SubdivisionCode.UNDEFINED, SubdivisionCode.fromCode(""));
		assertEquals(SubdivisionCode.UNDEFINED, SubdivisionCode.fromCode(null));
	}

	@Test
	public void testGetCountryCode_ShouldReturnCorrectCountry() {
		assertEquals(CountryCode.DE, SubdivisionCode.DE_BY.getCountryCode());
	}

	@Test
	public void testGetSubdivisionName_ShouldReturnCorrectName() {
		assertEquals("Bavaria", SubdivisionCode.DE_BY.getSubdivisionName());
	}
}
