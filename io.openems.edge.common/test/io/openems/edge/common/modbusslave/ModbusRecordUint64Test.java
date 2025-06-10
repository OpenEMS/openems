package io.openems.edge.common.modbusslave;

import static io.openems.common.test.DummyOptionsEnum.UNDEFINED;
import static io.openems.common.test.DummyOptionsEnum.VALUE_1;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

public class ModbusRecordUint64Test {

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
