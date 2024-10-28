package io.openems.edge.common.modbusslave;

import static io.openems.common.test.DummyOptionsEnum.UNDEFINED;
import static io.openems.common.test.DummyOptionsEnum.VALUE_1;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

public class ModbusRecordUint32Test {

	@Test
	public void testValue() {
		var sut = new ModbusRecordUint32(0, "foo", 123456789);
		assertEquals("ModbusRecordUInt32 [value=123456789/0x75bcd15, type=uint32]", sut.toString());
		assertEquals("\"123456789\"", sut.getValueDescription());
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
