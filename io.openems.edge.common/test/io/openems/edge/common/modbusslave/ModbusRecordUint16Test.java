package io.openems.edge.common.modbusslave;

import static io.openems.common.test.DummyOptionsEnum.UNDEFINED;
import static io.openems.common.test.DummyOptionsEnum.VALUE_1;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

public class ModbusRecordUint16Test {

	@Test
	public void testValue() {
		var sut = new ModbusRecordUint16(0, "foo", (short) 12345);
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
		assertEquals("ModbusRecordUint16Hash [text=hash, value=-16114/0xc10e, type=uint16]", sut.toString());
		assertEquals("\"0xc10e\"", sut.getValueDescription());
	}
}
