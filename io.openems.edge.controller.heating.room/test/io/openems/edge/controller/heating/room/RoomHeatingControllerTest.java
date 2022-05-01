package io.openems.edge.controller.heating.room;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.controller.heating.room.RoomHeatingControllerImpl.ActualMode;

public class RoomHeatingControllerTest {

	@Test
	public void testGetActualModeFromSchedule() {
		var clock = new TimeLeapClock(Instant.parse("2020-01-01T00:00:00.00Z"), ZoneOffset.UTC);
		var ctrl = new RoomHeatingControllerImpl(clock);
		var schedule = "__##___________________________________________________________________________________________";
		assertEquals(ActualMode.LOW, ctrl.getActualModeFromSchedule(schedule));
		clock.leap(15, ChronoUnit.MINUTES);
		assertEquals(ActualMode.LOW, ctrl.getActualModeFromSchedule(schedule));
		clock.leap(15, ChronoUnit.MINUTES);
		assertEquals(ActualMode.HIGH, ctrl.getActualModeFromSchedule(schedule));
		clock.leap(5, ChronoUnit.MINUTES);
		assertEquals(ActualMode.HIGH, ctrl.getActualModeFromSchedule(schedule));
		clock.leap(24, ChronoUnit.MINUTES);
		assertEquals(ActualMode.HIGH, ctrl.getActualModeFromSchedule(schedule));
		clock.leap(1, ChronoUnit.MINUTES);
		assertEquals(ActualMode.LOW, ctrl.getActualModeFromSchedule(schedule));
	}

}
