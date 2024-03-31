package io.openems.common.utils;

import static io.openems.common.utils.StringUtils.definedOrElse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class StringUtilsTest {

	@Test
	public void testToShortStringStringInt() {
		var test = "test to short string";
		assertEquals("te...", StringUtils.toShortString(test, 5));
	}

	@Test
	public void testToShortStringJsonObjectInt() {
		var j = new JsonObject();
		j.add("name", new JsonPrimitive("Testbert")); // {"name":"Testbert"} --> {"name":"T...
		assertEquals("{\"name\":\"T...", StringUtils.toShortString(j, 13));
	}

	@Test
	public void testCapitalizeFirstLetter() {
		assertEquals("Test", StringUtils.capitalizeFirstLetter("test"));
		assertEquals("TEST", StringUtils.capitalizeFirstLetter("TEST"));
		assertEquals("1test", StringUtils.capitalizeFirstLetter("1test"));
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void testCapitalizeFirstLetterWithEmptyString() {
		assertEquals("", StringUtils.capitalizeFirstLetter(""));
	}

	@Test
	public void testMatchWildcard() {
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
	public void testMatchesFloatPattern() {
		assertTrue(StringUtils.matchesFloatPattern("208.6"));
	}

	@Test
	public void testDefinedOrElse() {
		assertEquals("foo", definedOrElse("foo", "bar"));
		assertEquals("bar", definedOrElse(null, "bar"));
		assertEquals("bar", definedOrElse("", "bar"));
		assertEquals("bar", definedOrElse(" ", "bar"));
		assertEquals("bar", definedOrElse("	", "bar"));
	}

	@Test
	public void testParseNumberFromNameNull() throws Exception {
		assertTrue(StringUtils.parseNumberFromName(null).isEmpty());
	}

	@Test
	public void testParseNumberFromNameInvalidString() throws Exception {
		assertTrue(StringUtils.parseNumberFromName("edge").isEmpty());
	}

	@Test
	public void testParseNumberFromNameValidString() throws Exception {
		final var parsedNumber = StringUtils.parseNumberFromName("edge404");
		assertTrue(parsedNumber.isPresent());
		assertEquals(404, parsedNumber.getAsInt());
	}

}
