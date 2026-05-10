package io.openems.edge.common.modbusslave;

import static io.openems.common.test.DummyOptionsEnum.UNDEFINED;
import static io.openems.common.test.DummyOptionsEnum.VALUE_1;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.junit.Test;

public class ModbusRecordUint32Test {

	@Test
	public void testUndefined() {
		assertEquals(//
				ModbusRecordUint32.UNDEFINED_VALUE, //
				ByteBuffer.wrap(ModbusRecordUint32.UNDEFINED_BYTE_ARRAY).getInt(0) & 0xffffffffL);
		assertArrayEquals(//
				ModbusRecordUint32.UNDEFINED_BYTE_ARRAY, //
				ModbusRecordUint32.toByteArray(ModbusRecordUint32.UNDEFINED_VALUE));
		assertEquals(//
				"0xFFFFFFFF", //
				"0x" + Long.toHexString(ModbusRecordUint32.UNDEFINED_VALUE & 0xffffffffL).toUpperCase());
		assertEquals(ModbusRecordUint32.UNDEFINED_BYTE_ARRAY.length, ModbusRecordUint32.BYTE_LENGTH);
	}

	@Test
	public void testValue() {
		{
			// Some value
			var sut = new ModbusRecordUint32(0, "foo", 123456789L);
			assertEquals("ModbusRecordUInt32 [value=123456789/0x75bcd15, type=uint32]", sut.toString());
			assertEquals("\"123456789\"", sut.getValueDescription());
		}
		{
			// Max valid value
			var sut = new ModbusRecordUint32(0, "foo", 4294967295L);
			assertEquals("ModbusRecordUInt32 [value=4294967295/0xffffffff, type=uint32]", sut.toString());
			assertEquals("\"4294967295\"", sut.getValueDescription());
		}
	}

	@Test
	public void testNull() {
		var sut = new ModbusRecordUint32(0, "bar", null);
		assertEquals("ModbusRecordUInt32 [value=UNDEFINED, type=uint32]", sut.toString());
		assertEquals("", sut.getValueDescription());
	}

	@Test
	public void testOptionsEnum() {
		assertEquals("[-1, -1, -1, -1]", Arrays.toString(ModbusRecordUint32.toByteArray(UNDEFINED)));
		assertEquals("[0, 0, 0, 1]", Arrays.toString(ModbusRecordUint32.toByteArray(VALUE_1)));
	}

	@Test
	public void testReserved() {
		var sut = new ModbusRecordUint32Reserved(0);
		assertEquals("ModbusRecordUint32Reserved [type=uint32]", sut.toString());
		assertEquals("", sut.getValueDescription());
	}

}
