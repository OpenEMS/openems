package io.openems.edge.common.meta.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.regex.Pattern;

import org.junit.Test;

public class CountryCodeTest {

	@Test
	public void testCountryCode_ShouldFollowNamingConvention() {
		var pattern = Pattern.compile("^[A-Z]{2}$");
		for (var country : CountryCode.values()) {
			if (country == CountryCode.UNDEFINED) {
				continue;
			}
			assertTrue(pattern.matcher(country.name()).matches());
		}
	}

	@Test
	public void testGetCode_ShouldReturnEnumName() {
		assertEquals("DE", CountryCode.DE.getCode());
	}

	@Test
	public void testGetCountryName_ShouldReturnReadableName() {
		assertEquals("Germany", CountryCode.DE.getCountryName());
	}

	@Test
	public void testFromCode_ShouldReturnCorrectCountry() {
		assertEquals(CountryCode.DE, CountryCode.fromCode("DE"));
		assertEquals(CountryCode.DE, CountryCode.fromCode("de"));
		assertEquals(CountryCode.DE, CountryCode.fromCode(" De "));
	}

	@Test
	public void testFromCode_ShouldReturnUndefinedForInvalidInput() {
		assertEquals(CountryCode.UNDEFINED, CountryCode.fromCode("XX"));
		assertEquals(CountryCode.UNDEFINED, CountryCode.fromCode(""));
		assertEquals(CountryCode.UNDEFINED, CountryCode.fromCode(null));
	}
}
