package io.openems.edge.common.modbusslave;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

public class ModbusRecordString16Test {

	@Test
	public void testValue() {
		var sut = new ModbusRecordString16(0, "foo", "bar");
		assertEquals("ModbusRecordString16 [value=bar, type=string16]", sut.toString());
		assertEquals("\"bar\"", sut.getValueDescription());
	}

	@Test
	public void testNull() {
		var sut = new ModbusRecordString16(0, "bar", null);
		assertEquals("ModbusRecordString16 [value=UNDEFINED, type=string16]", sut.toString());
		assertEquals("", sut.getValueDescription());
	}

	@Test
	public void testReserved() {
		var sut = new ModbusRecordString16Reserved(0);
		assertEquals("ModbusRecordString16Reserved [type=string16]", sut.toString());
		assertEquals("", sut.getValueDescription());
	}

	@Test
	public void testToByteArray() {
		assertEquals("[72, 101, 108, 108, 111, "//
				+ "32, " //
				+ "87, 111, 114, 108, 100, " //
				+ "0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]",
				Arrays.toString(ModbusRecordString16.toByteArray((Object) "Hello World")));
		assertEquals("[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]",
				Arrays.toString(ModbusRecordString16.toByteArray((Object) null)));
	}
}
