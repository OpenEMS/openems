package io.openems.edge.controller.highloadtimeslot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import org.junit.Test;

public class ControllerSimpleTests {

	@Test
	public void testIsActiveDay() {
		var startDate = LocalDate.of(2018, 11, 10);
		var endDate = LocalDate.of(2018, 11, 12);

		var currentDate = LocalDateTime.of(2018, 11, 9, 12, 0);
		assertFalse(HighLoadTimeslot.isActiveDate(startDate, endDate, currentDate));

		currentDate = LocalDateTime.of(2018, 11, 10, 12, 0);
		assertTrue(HighLoadTimeslot.isActiveDate(startDate, endDate, currentDate));

		currentDate = LocalDateTime.of(2018, 11, 11, 12, 0);
		assertTrue(HighLoadTimeslot.isActiveDate(startDate, endDate, currentDate));

		currentDate = LocalDateTime.of(2018, 11, 12, 12, 0);
		assertTrue(HighLoadTimeslot.isActiveDate(startDate, endDate, currentDate));

		currentDate = LocalDateTime.of(2018, 11, 13, 12, 0);
		assertFalse(HighLoadTimeslot.isActiveDate(startDate, endDate, currentDate));
	}

	@Test
	public void testIsActiveTime() {
		var startTime = LocalTime.of(8, 0);
		var endTime = LocalTime.of(8, 10);

		var currentDateTime = LocalDateTime.of(2018, 11, 11, 7, 59);
		assertFalse(HighLoadTimeslot.isActiveTime(startTime, endTime, currentDateTime));

		currentDateTime = LocalDateTime.of(2018, 11, 11, 8, 0);
		assertTrue(HighLoadTimeslot.isActiveTime(startTime, endTime, currentDateTime));

		currentDateTime = LocalDateTime.of(2018, 11, 11, 8, 1);
		assertTrue(HighLoadTimeslot.isActiveTime(startTime, endTime, currentDateTime));

		currentDateTime = LocalDateTime.of(2018, 11, 11, 8, 9);
		assertTrue(HighLoadTimeslot.isActiveTime(startTime, endTime, currentDateTime));

		currentDateTime = LocalDateTime.of(2018, 11, 11, 8, 10);
		assertTrue(HighLoadTimeslot.isActiveTime(startTime, endTime, currentDateTime));

		currentDateTime = LocalDateTime.of(2018, 11, 11, 8, 11);
		assertFalse(HighLoadTimeslot.isActiveTime(startTime, endTime, currentDateTime));
	}

	@Test
	public void testIsActiveWeekday() {
		var currentDate = LocalDateTime.of(2018, 11, 5, 12, 0);
		var weekdayFilter = WeekdayFilter.ONLY_WEEKDAYS;
		assertTrue(HighLoadTimeslot.isActiveWeekday(weekdayFilter, currentDate)); // Monday

		currentDate = LocalDateTime.of(2018, 11, 6, 12, 0);
		assertTrue(HighLoadTimeslot.isActiveWeekday(weekdayFilter, currentDate)); // Tuesday

		currentDate = LocalDateTime.of(2018, 11, 7, 12, 0);
		assertTrue(HighLoadTimeslot.isActiveWeekday(weekdayFilter, currentDate)); // Wednesday

		currentDate = LocalDateTime.of(2018, 11, 8, 12, 0);
		assertTrue(HighLoadTimeslot.isActiveWeekday(weekdayFilter, currentDate)); // Thursday

		currentDate = LocalDateTime.of(2018, 11, 9, 12, 0);
		assertTrue(HighLoadTimeslot.isActiveWeekday(weekdayFilter, currentDate)); // Friday

		currentDate = LocalDateTime.of(2018, 11, 10, 12, 0);
		assertFalse(HighLoadTimeslot.isActiveWeekday(weekdayFilter, currentDate)); // Saturday

		currentDate = LocalDateTime.of(2018, 11, 11, 12, 0);
		assertFalse(HighLoadTimeslot.isActiveWeekday(weekdayFilter, currentDate)); // Sunday
	}

	@Test
	public void testconvertDate() {
		var dateString = "11.11.2018";
		var expectedDate = LocalDate.of(2018, 11, 11);
		assertEquals(expectedDate, HighLoadTimeslot.convertDate(dateString));

		dateString = "karlheinz";
		try {
			HighLoadTimeslot.convertDate(dateString);
			fail();
		} catch (DateTimeParseException e) {
			assertTrue(e instanceof DateTimeParseException);
		}

		dateString = "1.1.2018";
		try {
			HighLoadTimeslot.convertDate(dateString);
			fail();
		} catch (DateTimeParseException e) {
			assertTrue(e instanceof DateTimeParseException);
		}

		dateString = "31.02.2018";
		expectedDate = LocalDate.of(2018, 2, 28);
		assertEquals(expectedDate, HighLoadTimeslot.convertDate(dateString));

		dateString = "32.12.2018";
		try {
			HighLoadTimeslot.convertDate(dateString);
			fail();
		} catch (DateTimeParseException e) {
			assertTrue(e instanceof DateTimeParseException);
		}
	}

	@Test
	public void testconvertTime() {
		var timeString = "11:11";
		var expectedTime = LocalTime.of(11, 11);
		assertEquals(expectedTime, HighLoadTimeslot.convertTime(timeString));

		timeString = "karlheinz";
		try {
			HighLoadTimeslot.convertTime(timeString);
			fail();
		} catch (DateTimeParseException e) {
			assertTrue(e instanceof DateTimeParseException);
		}

		timeString = "25:13";
		try {
			HighLoadTimeslot.convertTime(timeString);
			fail();
		} catch (DateTimeParseException e) {
			assertTrue(e instanceof DateTimeParseException);
		}

		timeString = "24:13";
		try {
			HighLoadTimeslot.convertTime(timeString);
			fail();
		} catch (DateTimeParseException e) {
			assertTrue(e instanceof DateTimeParseException);
		}

		timeString = "24:00";
		expectedTime = LocalTime.of(0, 00);
		assertEquals(expectedTime, HighLoadTimeslot.convertTime(timeString));

		timeString = "23:13";
		expectedTime = LocalTime.of(23, 13);
		assertEquals(expectedTime, HighLoadTimeslot.convertTime(timeString));

		timeString = "0:13";
		try {
			HighLoadTimeslot.convertTime(timeString);
			fail();
		} catch (DateTimeParseException e) {
			assertTrue(e instanceof DateTimeParseException);
		}

		timeString = "00:13";
		expectedTime = LocalTime.of(0, 13);
		assertEquals(expectedTime, HighLoadTimeslot.convertTime(timeString));
	}
}
