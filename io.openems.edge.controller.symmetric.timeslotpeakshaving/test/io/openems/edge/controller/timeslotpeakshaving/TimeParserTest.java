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
		assertEquals(time, ControllerEssTimeslotPeakshavingImpl.convertTime(startTime));

	}

	@Test
	public void testFormat() throws OpenemsException {
		String startTime = "9:00";
		LocalTime time = LocalTime.of(9, 00);
		assertEquals(time, ControllerEssTimeslotPeakshavingImpl.convertTime(startTime));

	}

	@Test
	public void testFormat1() throws OpenemsException {
		String startTime = "09:00";
		LocalTime time = LocalTime.of(9, 00);
		assertEquals(time, ControllerEssTimeslotPeakshavingImpl.convertTime(startTime));

	}

	@Test(expected = OpenemsException.class)
	public void testWrongFormat() throws OpenemsException {
		ControllerEssTimeslotPeakshavingImpl.convertTime(".9:00");
	}

	@Test(expected = OpenemsException.class)
	public void testWrongFormat1() throws OpenemsException {
		ControllerEssTimeslotPeakshavingImpl.convertTime("55:00");
	}

}
