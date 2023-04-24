package io.openems.common.utils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;

public class DateUtilsTest {

	@Test
	public void testParseZonedDateTimeOrNull() {
		assertNull(DateUtils.parseZonedDateTimeOrNull(null));
		assertNull(DateUtils.parseZonedDateTimeOrNull(""));
		assertNull(DateUtils.parseZonedDateTimeOrNull("    "));
		assertNull(DateUtils.parseZonedDateTimeOrNull("abcde"));
		assertNotNull(DateUtils.parseZonedDateTimeOrNull("2007-12-03T10:15:30+01:00[Europe/Paris]"));
	}

	@Test(expected = OpenemsException.class)
	public void testParseZonedDateTimeOrErrorNull() throws Exception {
		DateUtils.parseZonedDateTimeOrError(null);
	}

	@Test(expected = OpenemsException.class)
	public void testParseZonedDateTimeOrErrorEmpty() throws Exception {
		DateUtils.parseZonedDateTimeOrError("");
	}

	@Test(expected = OpenemsException.class)
	public void testParseZonedDateTimeOrErrorBlank() throws Exception {
		DateUtils.parseZonedDateTimeOrError("      ");
	}

	@Test(expected = OpenemsException.class)
	public void testParseZonedDateTimeOrErrorLetters() throws Exception {
		DateUtils.parseZonedDateTimeOrError("abc");
	}

	@Test
	public void testParseZonedDateTimeOrErrorSuccess() throws Exception {
		DateUtils.parseZonedDateTimeOrError("2007-12-03T10:15:30+01:00[Europe/Paris]");
	}

	@Test
	public void testParseLocalDateOrNull() {
		assertNull(DateUtils.parseLocalDateOrNull(null));
		assertNull(DateUtils.parseLocalDateOrNull(""));
		assertNull(DateUtils.parseLocalDateOrNull("    "));
		assertNull(DateUtils.parseLocalDateOrNull("abcde"));
		assertNotNull(DateUtils.parseLocalDateOrNull("2007-12-03"));
	}

	@Test(expected = OpenemsException.class)
	public void testParseLocalDateOrErrorNull() throws Exception {
		DateUtils.parseLocalDateOrError(null);
	}

	@Test(expected = OpenemsException.class)
	public void testParseLocalDateOrErrorEmpty() throws Exception {
		DateUtils.parseLocalDateOrError("");
	}

	@Test(expected = OpenemsException.class)
	public void testParseLocalDateOrErrorBlank() throws Exception {
		DateUtils.parseLocalDateOrError("      ");
	}

	@Test(expected = OpenemsException.class)
	public void testParseLocalDateOrErrorLetters() throws Exception {
		DateUtils.parseLocalDateOrError("abc");
	}

	@Test
	public void testParseLocalDateOrErrorSuccess() throws Exception {
		DateUtils.parseLocalDateOrError("2007-12-03");
	}

	@Test
	public void testParseLocalTimeOrNull() {
		assertNull(DateUtils.parseLocalTimeOrNull(null));
		assertNull(DateUtils.parseLocalTimeOrNull(""));
		assertNull(DateUtils.parseLocalTimeOrNull("    "));
		assertNull(DateUtils.parseLocalTimeOrNull("abcde"));
		assertNotNull(DateUtils.parseLocalTimeOrNull("10:15"));
	}

	@Test(expected = OpenemsException.class)
	public void testParseLocalTimeOrErrorNull() throws Exception {
		DateUtils.parseLocalTimeOrError(null);
	}

	@Test(expected = OpenemsException.class)
	public void testParseLocalTimeOrErrorEmpty() throws Exception {
		DateUtils.parseLocalTimeOrError("");
	}

	@Test(expected = OpenemsException.class)
	public void testParseLocalTimeOrErrorBlank() throws Exception {
		DateUtils.parseLocalTimeOrError("      ");
	}

	@Test(expected = OpenemsException.class)
	public void testParseLocalTimeOrErrorLetters() throws Exception {
		DateUtils.parseLocalTimeOrError("abc");
	}

	@Test
	public void testParseLocalTimeOrErrorSuccess() throws Exception {
		DateUtils.parseLocalTimeOrError("10:15");
	}

}
