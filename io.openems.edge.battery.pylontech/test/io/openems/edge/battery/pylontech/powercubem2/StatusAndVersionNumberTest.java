package io.openems.edge.battery.pylontech.powercubem2;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import io.openems.edge.battery.pylontech.powercubem2.PylontechPowercubeM2Battery.Status;

/**
 * Test to check that the battery component is correctly calculating the battery version number from registers
 * and correctly reading the status bit.
 */
public class StatusAndVersionNumberTest {

	@Test
	public void testVersionNumber() throws Exception {
		var battery = new PylontechPowercubeM2BatteryImpl();

		// Tests for calculating Version Number
		assertEquals("V01.06", battery.convertVersionNumber(0x0106));
		assertEquals("V13.02", battery.convertVersionNumber(0x1302));
		assertEquals("V02.11", battery.convertVersionNumber(0x0211));
		assertNotEquals("V01.12", battery.convertVersionNumber(0x010C));
	}

	@Test
	public void testStatus() throws Exception {
		var battery = new PylontechPowercubeM2BatteryImpl();

		// Tests for calculating status from register

		// Normal states
		assertEquals(Status.UNDEFINED, battery.getStatusFromRegisterValue(null));
		assertEquals(Status.CHARGE, battery.getStatusFromRegisterValue(1));
		assertEquals(Status.DISCHARGE, battery.getStatusFromRegisterValue(2));
		assertEquals(Status.IDLE, battery.getStatusFromRegisterValue(3));

		// Check that it rejects values out of bounds
		assertEquals(Status.UNDEFINED, battery.getStatusFromRegisterValue(-1));
		assertEquals(Status.UNDEFINED, battery.getStatusFromRegisterValue(6));

		// Check that it ignore values outside 3 LSBs
		assertEquals(Status.SLEEP, battery.getStatusFromRegisterValue(0xF0));
		assertEquals(Status.CHARGE, battery.getStatusFromRegisterValue(0xF1));
		assertEquals(Status.CHARGE, battery.getStatusFromRegisterValue(0b0111001));
		assertEquals(Status.SLEEP, battery.getStatusFromRegisterValue(0b0111000));
		assertEquals(Status.DISCHARGE, battery.getStatusFromRegisterValue(0b0111010));
		assertEquals(Status.IDLE, battery.getStatusFromRegisterValue(0b0111011));

		// Check that sending a reserved value, with additional OOB bits, leads to undefined
		assertEquals(Status.UNDEFINED, battery.getStatusFromRegisterValue(0b0111100));
		assertEquals(Status.UNDEFINED, battery.getStatusFromRegisterValue(0b0111101));

	}
}