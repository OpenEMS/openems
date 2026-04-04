package io.openems.edge.solaredge.enums;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EnumsTest {

	@Test
	public void test() {
		assertEquals("Undefined", AcChargePolicy.UNDEFINED.getName());
		assertEquals("Undefined", BatteryStatus.UNDEFINED.getName());
		assertEquals("Undefined", CommandMode.UNDEFINED.getName());
		assertEquals("Undefined", MeterCommunicateStatus.UNDEFINED.getName());
		assertEquals("Undefined", SeControlMode.UNDEFINED.getName());
	}
}
