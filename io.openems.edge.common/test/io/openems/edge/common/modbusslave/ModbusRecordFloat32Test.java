package io.openems.edge.common.modbusslave;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ModbusRecordFloat32Test {

	@Test
	public void test() {
		ModbusRecordFloat32 sut = new ModbusRecordFloat32(100, "Test-Record", 123.4F);
		byte[] value = sut.getValue();

		System.out.println(Integer.toHexString(value[1]));
		assertEquals(4, value.length);
		assertEquals(0x42, value[0] & 0xFF);
		assertEquals(0xf6, value[1] & 0xFF);
		assertEquals(0xcc, value[2] & 0xFF);
		assertEquals(0xcd, value[3] & 0xFF);
	}

}
