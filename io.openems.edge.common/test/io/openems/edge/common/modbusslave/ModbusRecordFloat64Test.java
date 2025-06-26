package io.openems.edge.common.modbusslave;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ModbusRecordFloat64Test {

	@Test
	public void testValue() {
		var sut = new ModbusRecordFloat64(0, "foo", 1234567.89);
		assertEquals("ModbusRecordFloat64 [value=1234567.89, type=float64]", sut.toString());
		assertEquals("\"1234567.89\"", sut.getValueDescription());
	}

	@Test
	public void testNull() {
		var sut = new ModbusRecordFloat64(0, "bar", null);
		assertEquals("ModbusRecordFloat64 [value=UNDEFINED, type=float64]", sut.toString());
		assertEquals("", sut.getValueDescription());
	}

	@Test
	public void testReserved() {
		var sut = new ModbusRecordFloat64Reserved(0);
		assertEquals("ModbusRecordFloat64Reserved [type=float64]", sut.toString());
		assertEquals("", sut.getValueDescription());
	}

}
