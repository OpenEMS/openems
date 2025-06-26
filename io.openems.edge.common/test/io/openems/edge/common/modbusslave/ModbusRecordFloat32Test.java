package io.openems.edge.common.modbusslave;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ModbusRecordFloat32Test {

	@Test
	public void testValue() {
		var sut = new ModbusRecordFloat32(0, "foo", 1234567.89F);
		assertEquals("ModbusRecordFloat32 [value=1234567.9, type=float32]", sut.toString());
		assertEquals("\"1234567.9\"", sut.getValueDescription());
	}

	@Test
	public void testNull() {
		var sut = new ModbusRecordFloat32(0, "bar", null);
		assertEquals("ModbusRecordFloat32 [value=UNDEFINED, type=float32]", sut.toString());
		assertEquals("", sut.getValueDescription());
	}

	@Test
	public void testReserved() {
		var sut = new ModbusRecordFloat32Reserved(0);
		assertEquals("ModbusRecordFloat32Reserved [type=float32]", sut.toString());
		assertEquals("", sut.getValueDescription());
	}

}
