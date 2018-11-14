package io.openems.edge.controller.highloadtimeslot;

import static org.junit.Assert.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Test;

public class ControllerSimpleTests {

	@Test
	public void testIsInTimeSlot() {
		LocalDate startDate = LocalDate.of(2018, 11, 10);
		LocalDate endDate = LocalDate.of(2018, 11, 12);		
		LocalDateTime currentDate = LocalDateTime.of(2018, 11, 11, 12, 0);
		
		assertTrue(HighLoadTimeslot.isInDateSlot(currentDate, startDate, endDate));
		
		startDate = LocalDate.of(2018, 11, 10);
		endDate = LocalDate.of(2018, 11, 12);		
		currentDate = LocalDateTime.of(2018, 11, 10, 12, 0);
		
		assertTrue(HighLoadTimeslot.isInDateSlot(currentDate, startDate, endDate));
		
		
		startDate = LocalDate.of(2018, 11, 10);
		endDate = LocalDate.of(2018, 11, 12);		
		currentDate = LocalDateTime.of(2018, 11, 9, 12, 0);
		
		assertFalse(HighLoadTimeslot.isInDateSlot(currentDate, startDate, endDate));
	}

}
