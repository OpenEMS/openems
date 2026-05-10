package io.openems.edge.common.modbusslave;

import static io.openems.common.test.DummyOptionsEnum.UNDEFINED;
import static io.openems.common.test.DummyOptionsEnum.VALUE_1;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.junit.Test;

public class ModbusRecordUint16Test {

	@Test
	public void testUndefined() {
		assertEquals(//
				ModbusRecordUint16.UNDEFINED_VALUE, //
				ByteBuffer.wrap(ModbusRecordUint16.UNDEFINED_BYTE_ARRAY).getShort(0) & 0xffff);
		assertArrayEquals(//
				ModbusRecordUint16.UNDEFINED_BYTE_ARRAY, //
				ModbusRecordUint16.toByteArray(ModbusRecordUint16.UNDEFINED_VALUE));
		assertEquals(//
				"0xFFFF", //
				"0x" + Integer.toHexString(ModbusRecordUint16.UNDEFINED_VALUE & 0xffff).toUpperCase());
		assertEquals(ModbusRecordUint16.UNDEFINED_BYTE_ARRAY.length, ModbusRecordUint16.BYTE_LENGTH);
	}

	@Test
	public void testValue() {
		var sut = new ModbusRecordUint16(0, "foo", 12345);
		assertEquals("ModbusRecordUInt16 [value=12345/0x3039, type=uint16]", sut.toString());
		assertEquals("\"12345\"", sut.getValueDescription());
	}

	@Test
	public void testNull() {
		var sut = new ModbusRecordUint16(0, "bar", null);
		assertEquals("ModbusRecordUInt16 [value=UNDEFINED, type=uint16]", sut.toString());
		assertEquals("", sut.getValueDescription());
	}

	@Test
	public void testOptionsEnum() {
		assertEquals("[-1, -1]", Arrays.toString(ModbusRecordUint16.toByteArray(UNDEFINED)));
		assertEquals("[0, 1]", Arrays.toString(ModbusRecordUint16.toByteArray(VALUE_1)));
	}

	@Test
	public void testReserved() {
		var sut = new ModbusRecordUint16Reserved(0);
		assertEquals("ModbusRecordUint16Reserved [type=uint16]", sut.toString());
		assertEquals("", sut.getValueDescription());
	}

	@Test
	public void testBlockLength() {
		assertEquals("ModbusRecordUint16BlockLength [blockName=block, value=12345/0x3039, type=uint16]",
				new ModbusRecordUint16BlockLength(0, "block", (short) 12345).toString());
	}

	@Test
	public void testHash() {
		var sut = new ModbusRecordUint16Hash(0, "hash");
		assertEquals("ModbusRecordUint16Hash [text=hash, value=49422/0xc10e, type=uint16]", sut.toString());
		assertEquals("\"0xc10e\"", sut.getValueDescription());
	}
}
