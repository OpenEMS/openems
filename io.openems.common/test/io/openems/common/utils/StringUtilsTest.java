package io.openems.common.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class StringUtilsTest {

	@Test
	public void testToShortStringStringInt() {
		String test = "test to short string";
		assertEquals("te...", StringUtils.toShortString(test, 5));

	}

	@Test
	public void testToShortStringJsonObjectInt() {
		JsonObject j = new JsonObject();
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
		String activePower = "ActivePower";
		String anyPower = "*Power";
		String anyActive = "Active*";
		String any = "*";
		String foobar = "foobar";

		assertEquals(anyPower.length(), StringUtils.matchWildcard(activePower, anyPower));
		assertEquals(anyActive.length(), StringUtils.matchWildcard(activePower, anyActive));
		assertEquals(1, StringUtils.matchWildcard(activePower, any));
		assertEquals(-1, StringUtils.matchWildcard(activePower, foobar));
	}

}
