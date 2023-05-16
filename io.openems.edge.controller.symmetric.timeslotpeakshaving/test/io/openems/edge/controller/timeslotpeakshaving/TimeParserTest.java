package io.openems.edge.controller.timeslotpeakshaving;

import static org.junit.Assert.assertEquals;

import java.time.LocalTime;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;

public class TimeParserTest {

	@Test
	public void test() throws OpenemsException {

		String startTime = "23:30";
		LocalTime time = LocalTime.of(23, 30);
		assertEquals(time, TimeslotPeakshaving.convertTime(startTime));

	}

	@Test
	public void testFormat() throws OpenemsException {

		String startTime = "9:00";
		LocalTime time = LocalTime.of(9, 00);
		assertEquals(time, TimeslotPeakshaving.convertTime(startTime));

	}

	@Test
	public void testFormat1() throws OpenemsException {

		String startTime = "09:00";
		LocalTime time = LocalTime.of(9, 00);
		assertEquals(time, TimeslotPeakshaving.convertTime(startTime));

	}

	@Test
	public void testWrongFormat() {

		String startTime = ".9:00";
		String expected = "Text '.9:00' could not be parsed at index 0";
		try {
			TimeslotPeakshaving.convertTime(startTime);
		} catch (Exception e) {
			assertEquals(expected, e.getMessage());
		}
	}

	@Test
	public void testWrongFormat1() {

		String startTime = "55:00";
		String expected = "Text '55:00' could not be parsed: Invalid value for HourOfDay (valid values 0 - 23): 55";
		try {
			TimeslotPeakshaving.convertTime(startTime);
		} catch (Exception e) {
			assertEquals(expected, e.getMessage());
		}
	}

}
