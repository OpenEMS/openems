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

import io.openems.common.exceptions.OpenemsException;

public class ControllerSimpleTests {

	@Test
	public void testIsActiveDay() {
		LocalDate startDate = LocalDate.of(2018, 11, 10);
		LocalDate endDate = LocalDate.of(2018, 11, 12);

		LocalDateTime currentDate = LocalDateTime.of(2018, 11, 9, 12, 0);
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
		LocalTime startTime = LocalTime.of(8, 0);
		LocalTime endTime = LocalTime.of(8, 10);

		LocalDateTime currentDateTime = LocalDateTime.of(2018, 11, 11, 7, 59);
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
		LocalDateTime currentDate = LocalDateTime.of(2018, 11, 5, 12, 0);
		WeekdayFilter weekdayFilter = WeekdayFilter.ONLY_WEEKDAYS;
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
		String dateString = "11.11.2018";
		LocalDate expectedDate = LocalDate.of(2018, 11, 11);
		assertEquals(expectedDate, HighLoadTimeslot.convertDate(dateString));

		dateString = "karlheinz";
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
		String timeString = "11:11";
		LocalTime expectedTime = LocalTime.of(11, 11);
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

		timeString = "00:13";
		expectedTime = LocalTime.of(0, 13);
		assertEquals(expectedTime, HighLoadTimeslot.convertTime(timeString));
	}

	@Test
	public void testConvertTime() throws OpenemsException {
		assertEquals(LocalTime.of(0, 0), HighLoadTimeslot.convertTime("0:0"));
		assertEquals(LocalTime.of(10, 0), HighLoadTimeslot.convertTime("10:0"));
		assertEquals(LocalTime.of(0, 10), HighLoadTimeslot.convertTime("0:10"));
		assertEquals(LocalTime.of(0, 10), HighLoadTimeslot.convertTime("00:10"));
		assertEquals(LocalTime.of(10, 10), HighLoadTimeslot.convertTime("10:10"));
		assertEquals(LocalTime.of(0, 0), HighLoadTimeslot.convertTime("24:0"));
	}

	@Test
	public void testConvertDate() throws OpenemsException {
		assertEquals(LocalDate.of(2020, 2, 1), HighLoadTimeslot.convertDate("01.02.2020"));
		assertEquals(LocalDate.of(2020, 2, 1), HighLoadTimeslot.convertDate("1.02.2020"));
		assertEquals(LocalDate.of(2020, 2, 1), HighLoadTimeslot.convertDate("1.2.2020"));
	}
}
