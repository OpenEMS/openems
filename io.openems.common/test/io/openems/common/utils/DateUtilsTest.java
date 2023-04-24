package io.openems.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;

public class DateUtilsTest {

	@Test
	public void testParseDateWithDMYFormat() throws Exception {
		var dateString = "11.11.2018";
		var expectedDate = LocalDate.of(2018, 11, 11);
		assertEquals(expectedDate, DateUtils.parseLocalDateOrError(dateString, DateUtils.DMY_FORMATTER));

		dateString = "31.02.2018";
		expectedDate = LocalDate.of(2018, 2, 28);
		assertEquals(expectedDate, DateUtils.parseLocalDateOrError(dateString, DateUtils.DMY_FORMATTER));
	}

	@Test(expected = OpenemsException.class)
	public void testParseDateWithDMYFormatMissingLeadingZeros() throws Exception {
		DateUtils.parseLocalDateOrError("1.1.2018", DateUtils.DMY_FORMATTER);
	}

	@Test(expected = OpenemsException.class)
	public void testParseDateWithDMYFormatInvalidDate() throws Exception {
		DateUtils.parseLocalDateOrError("32.12.2018", DateUtils.DMY_FORMATTER);
	}

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
		var timeString = "10:15";
		var expectedTime = LocalTime.of(10, 15);
		assertEquals(expectedTime, DateUtils.parseLocalTimeOrError(timeString));

		timeString = "23:13";
		expectedTime = LocalTime.of(23, 13);
		assertEquals(expectedTime, DateUtils.parseLocalTimeOrError(timeString));

		timeString = "00:13";
		expectedTime = LocalTime.of(0, 13);
		assertEquals(expectedTime, DateUtils.parseLocalTimeOrError(timeString));
	}

	@Test(expected = OpenemsException.class)
	public void testParseLocalTimeOrErrorSuccess24Hour() throws Exception {
		DateUtils.parseLocalTimeOrError("24:00");
	}

	@Test
	public void testParseLocalTimeOrErrorSuccess24HourWithTimeFormatter() throws Exception {
		assertEquals(LocalTime.of(0, 0), DateUtils.parseLocalTimeOrError("24:00", DateUtils.TIME_FORMATTER));
	}

	@Test(expected = OpenemsException.class)
	public void testParseLocalTimeOrErrorSuccessInvalidTime() throws Exception {
		DateUtils.parseLocalTimeOrError("25:21");
	}

	@Test(expected = OpenemsException.class)
	public void testParseLocalTimeOrErrorSuccessInvalidTime2() throws Exception {
		DateUtils.parseLocalTimeOrError("24:13");
	}

	@Test(expected = OpenemsException.class)
	public void testParseLocalTimeOrErrorSuccessMissingLeadingZero() throws Exception {
		DateUtils.parseLocalTimeOrError("0:13");
	}

}
