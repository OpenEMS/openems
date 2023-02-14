package io.openems.common.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class StringUtilsTest {

	@Test
	public void testToShortStringStringInt() {
		var test = "test to short string";
		Assert.assertEquals("te...", StringUtils.toShortString(test, 5));

	}

	@Test
	public void testToShortStringJsonObjectInt() {
		var j = new JsonObject();
		j.add("name", new JsonPrimitive("Testbert")); // {"name":"Testbert"} --> {"name":"T...
		Assert.assertEquals("{\"name\":\"T...", StringUtils.toShortString(j, 13));
	}

	@Test
	public void testCapitalizeFirstLetter() {
		Assert.assertEquals("Test", StringUtils.capitalizeFirstLetter("test"));
		Assert.assertEquals("TEST", StringUtils.capitalizeFirstLetter("TEST"));
		Assert.assertEquals("1test", StringUtils.capitalizeFirstLetter("1test"));
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void testCapitalizeFirstLetterWithEmptyString() {
		Assert.assertEquals("", StringUtils.capitalizeFirstLetter(""));
	}

	@Test
	public void testMatchWildcard() {
		var activePower = "ActivePower";
		var anyPower = "*Power";
		var anyActive = "Active*";
		var any = "*";
		var foobar = "foobar";

		Assert.assertEquals(anyPower.length(), StringUtils.matchWildcard(activePower, anyPower));
		Assert.assertEquals(anyActive.length(), StringUtils.matchWildcard(activePower, anyActive));
		Assert.assertEquals(1, StringUtils.matchWildcard(activePower, any));
		Assert.assertEquals(-1, StringUtils.matchWildcard(activePower, foobar));
	}

	@Test
	public void testMatchesFloatPattern() {
		assertTrue(StringUtils.matchesFloatPattern("208.6"));
		assertFalse(StringUtils.matchesIntegerPattern("001004        \""));
	}

	@Test
	public void testReverse() {
		Assert.assertEquals("OpenEMS", StringUtils.reverse("SMEnepO"));
	}

}
