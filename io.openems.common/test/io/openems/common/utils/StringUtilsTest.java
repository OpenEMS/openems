package io.openems.common.utils;

import static io.openems.common.utils.StringUtils.definedOrElse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

class StringUtilsTest {

	@Test
	void testToShortStringStringInt() {
		var test = "test to short string";
		assertEquals("te...", StringUtils.toShortString(test, 5));
	}

	@Test
	void testToShortStringJsonObjectInt() {
		var j = new JsonObject();
		j.add("name", new JsonPrimitive("Testbert")); // {"name":"Testbert"} --> {"name":"T...
		assertEquals("{\"name\":\"T...", StringUtils.toShortString(j, 13));
	}

	@Test
	void testCapitalizeFirstLetter() {
		assertEquals("Test", StringUtils.capitalizeFirstLetter("test"));
		assertEquals("TEST", StringUtils.capitalizeFirstLetter("TEST"));
		assertEquals("1test", StringUtils.capitalizeFirstLetter("1test"));
	}

	@Test
	void testCapitalizeFirstLetterWithEmptyString() {
		Assertions.assertThrows(IndexOutOfBoundsException.class, () -> StringUtils.capitalizeFirstLetter(""));
	}

	@Test
	void testMatchWildcard() {
		final var activePower = "ActivePower";
		final var anyPower = "*Power";
		final var anyActive = "Active*";
		final var any = "*";
		final var foobar = "foobar";

		assertEquals(anyPower.length(), StringUtils.matchWildcard(activePower, anyPower));
		assertEquals(anyActive.length(), StringUtils.matchWildcard(activePower, anyActive));
		assertEquals(1, StringUtils.matchWildcard(activePower, any));
		assertEquals(-1, StringUtils.matchWildcard(activePower, foobar));
	}

	@Test
	void testDefinedOrElse() {
		assertEquals("foo", definedOrElse("foo", "bar"));
		assertEquals("bar", definedOrElse(null, "bar"));
		assertEquals("bar", definedOrElse("", "bar"));
		assertEquals("bar", definedOrElse(" ", "bar"));
		assertEquals("bar", definedOrElse("	", "bar"));
	}

	@Test
	void testParseNumberFromNameNull() {
		assertTrue(StringUtils.parseNumberFromName(null).isEmpty());
	}

	@Test
	void testParseNumberFromNameInvalidString() {
		assertTrue(StringUtils.parseNumberFromName("edge").isEmpty());
	}

	@Test
	void testParseNumberFromNameValidString() {
		final var parsedNumber = StringUtils.parseNumberFromName("edge404");
		assertTrue(parsedNumber.isPresent());
		assertEquals(404, parsedNumber.getAsInt());
	}

	@Test
	void testEmptyToNull_ShouldReturnNull_WhenNullEmptyOrBlank() {
		String[] inputs = { null, "", "   ", "\t\n" };
		for (String input : inputs) {
			assertNull(StringUtils.emptyToNull(input));
		}
	}

	@Test
	void testEmptyToNull_ShouldReturnSameString_WhenNonBlank() {
		String[] inputs = { "abc", "  abc  ", "0", "true" };
		for (String input : inputs) {
			assertEquals(input, StringUtils.emptyToNull(input));
		}
	}

	@Test
	void testContainsIgnoreCase() {
		assertTrue(StringUtils.containsIgnoreCase("Hello, its me", "ITS"));
		assertTrue(StringUtils.containsIgnoreCase("Hello, its me", "its"));
		assertFalse(StringUtils.containsIgnoreCase("Hello, its me", "itse"));
		assertFalse(StringUtils.containsIgnoreCase("Hello, its me", "ITSE"));
	}

	@Test
	void testExtractTextSurroundedBy() {
		assertEquals("Baum", StringUtils.extractTextSurroundedBy("Zug[Baum]", "[", "]"));
		assertEquals("Test", StringUtils.extractTextSurroundedBy("==Test==", "==", "=="));
		assertEquals("", StringUtils.extractTextSurroundedBy("[]", "[", "]"));
		assertNull(StringUtils.extractTextSurroundedBy("Zug[Baum]", "]", "["));
		assertNull(StringUtils.extractTextSurroundedBy("Zug[Baum", "[", "]"));
		assertNull(StringUtils.extractTextSurroundedBy("ZugBaum]", "[", "]"));
	}

}
