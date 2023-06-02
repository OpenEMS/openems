package io.openems.edge.controller.highloadtimeslot;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.Test;

public class ControllerSimpleTests {

	@Test
	public void testIsActiveDay() {
		var startDate = LocalDate.of(2018, 11, 10);
		var endDate = LocalDate.of(2018, 11, 12);

		var currentDate = LocalDateTime.of(2018, 11, 9, 12, 0);
		assertFalse(HighLoadTimeslotImpl.isActiveDate(startDate, endDate, currentDate));

		currentDate = LocalDateTime.of(2018, 11, 10, 12, 0);
		assertTrue(HighLoadTimeslotImpl.isActiveDate(startDate, endDate, currentDate));

		currentDate = LocalDateTime.of(2018, 11, 11, 12, 0);
		assertTrue(HighLoadTimeslotImpl.isActiveDate(startDate, endDate, currentDate));

		currentDate = LocalDateTime.of(2018, 11, 12, 12, 0);
		assertTrue(HighLoadTimeslotImpl.isActiveDate(startDate, endDate, currentDate));

		currentDate = LocalDateTime.of(2018, 11, 13, 12, 0);
		assertFalse(HighLoadTimeslotImpl.isActiveDate(startDate, endDate, currentDate));
	}

	@Test
	public void testIsActiveTime() {
		var startTime = LocalTime.of(8, 0);
		var endTime = LocalTime.of(8, 10);

		var currentDateTime = LocalDateTime.of(2018, 11, 11, 7, 59);
		assertFalse(HighLoadTimeslotImpl.isActiveTime(startTime, endTime, currentDateTime));

		currentDateTime = LocalDateTime.of(2018, 11, 11, 8, 0);
		assertTrue(HighLoadTimeslotImpl.isActiveTime(startTime, endTime, currentDateTime));

		currentDateTime = LocalDateTime.of(2018, 11, 11, 8, 1);
		assertTrue(HighLoadTimeslotImpl.isActiveTime(startTime, endTime, currentDateTime));

		currentDateTime = LocalDateTime.of(2018, 11, 11, 8, 9);
		assertTrue(HighLoadTimeslotImpl.isActiveTime(startTime, endTime, currentDateTime));

		currentDateTime = LocalDateTime.of(2018, 11, 11, 8, 10);
		assertTrue(HighLoadTimeslotImpl.isActiveTime(startTime, endTime, currentDateTime));

		currentDateTime = LocalDateTime.of(2018, 11, 11, 8, 11);
		assertFalse(HighLoadTimeslotImpl.isActiveTime(startTime, endTime, currentDateTime));
	}

	@Test
	public void testIsActiveWeekday() {
		var currentDate = LocalDateTime.of(2018, 11, 5, 12, 0);
		var weekdayFilter = WeekdayFilter.ONLY_WEEKDAYS;
		assertTrue(HighLoadTimeslotImpl.isActiveWeekday(weekdayFilter, currentDate)); // Monday

		currentDate = LocalDateTime.of(2018, 11, 6, 12, 0);
		assertTrue(HighLoadTimeslotImpl.isActiveWeekday(weekdayFilter, currentDate)); // Tuesday

		currentDate = LocalDateTime.of(2018, 11, 7, 12, 0);
		assertTrue(HighLoadTimeslotImpl.isActiveWeekday(weekdayFilter, currentDate)); // Wednesday

		currentDate = LocalDateTime.of(2018, 11, 8, 12, 0);
		assertTrue(HighLoadTimeslotImpl.isActiveWeekday(weekdayFilter, currentDate)); // Thursday

		currentDate = LocalDateTime.of(2018, 11, 9, 12, 0);
		assertTrue(HighLoadTimeslotImpl.isActiveWeekday(weekdayFilter, currentDate)); // Friday

		currentDate = LocalDateTime.of(2018, 11, 10, 12, 0);
		assertFalse(HighLoadTimeslotImpl.isActiveWeekday(weekdayFilter, currentDate)); // Saturday

		currentDate = LocalDateTime.of(2018, 11, 11, 12, 0);
		assertFalse(HighLoadTimeslotImpl.isActiveWeekday(weekdayFilter, currentDate)); // Sunday
	}
}
