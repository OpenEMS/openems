package io.openems.edge.common.modbusslave;

import static io.openems.common.test.DummyOptionsEnum.UNDEFINED;
import static io.openems.common.test.DummyOptionsEnum.VALUE_1;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.junit.Test;

public class ModbusRecordUint64Test {

	@Test
	public void testUndefined() {
		assertEquals(//
				ModbusRecordUint64.UNDEFINED_VALUE, //
				ByteBuffer.wrap(ModbusRecordUint64.UNDEFINED_BYTE_ARRAY).getLong());
		assertArrayEquals(//
				ModbusRecordUint64.UNDEFINED_BYTE_ARRAY, //
				ModbusRecordUint64.toByteArray(ModbusRecordUint64.UNDEFINED_VALUE));
		assertEquals(//
				"0xFFFFFFFFFFFFFFFF", //
				"0x" + Long.toHexString(ModbusRecordUint64.UNDEFINED_VALUE).toUpperCase());
		assertEquals(ModbusRecordUint64.UNDEFINED_BYTE_ARRAY.length, ModbusRecordUint64.BYTE_LENGTH);
	}

	@Test
	public void testValue() {
		var sut = new ModbusRecordUint64(0, "foo", 123456789L);
		assertEquals("ModbusRecordUInt64 [value=123456789/0x75bcd15, type=uint64]", sut.toString());
		assertEquals("\"123456789\"", sut.getValueDescription());
	}

	@Test
	public void testNull() {
		var sut = new ModbusRecordUint64(0, "bar", null);
		assertEquals("ModbusRecordUInt64 [value=UNDEFINED, type=uint64]", sut.toString());
		assertEquals("", sut.getValueDescription());
	}

	@Test
	public void testOptionsEnum() {
		assertEquals("[-1, -1, -1, -1, -1, -1, -1, -1]", Arrays.toString(ModbusRecordUint64.toByteArray(UNDEFINED)));
		assertEquals("[0, 0, 0, 0, 0, 0, 0, 1]", Arrays.toString(ModbusRecordUint64.toByteArray(VALUE_1)));
	}

	@Test
	public void testReserved() {
		var sut = new ModbusRecordUint64Reserved(0);
		assertEquals("ModbusRecordUint64Reserved [type=uint64]", sut.toString());
		assertEquals("", sut.getValueDescription());
	}

}
