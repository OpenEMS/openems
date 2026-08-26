package io.openems.edge.common.modbusslave;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;

import org.junit.Test;

public class ModbusRecordFloat64Test {

	@Test
	public void testUndefined() {
		assertEquals(//
				ModbusRecordFloat64.UNDEFINED_VALUE, //
				ByteBuffer.wrap(ModbusRecordFloat64.UNDEFINED_BYTE_ARRAY).getDouble(), //
				0.001);
		assertArrayEquals(//
				ModbusRecordFloat64.UNDEFINED_BYTE_ARRAY, //
				ModbusRecordFloat64.toByteArray(ModbusRecordFloat64.UNDEFINED_VALUE));
		assertArrayEquals(//
				new byte[] { (byte) 0x7F, (byte) 0xF8, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
						(byte) 0x00 }, //
				ModbusRecordFloat64.UNDEFINED_BYTE_ARRAY);
		assertEquals(//
				"NaN", //
				Double.toHexString(ModbusRecordFloat64.UNDEFINED_VALUE));
		assertEquals(ModbusRecordFloat64.UNDEFINED_BYTE_ARRAY.length, ModbusRecordFloat64.BYTE_LENGTH);
	}

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
