package io.openems.edge.bridge.modbus.api;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ConversionTest {

	@Test
	public void testShortConversions() {

		var v1 = 0;
		var result = ModbusUtils.convert(v1, 0);
		Short expected = 0;
		assertEquals(expected, result);

		v1 = 1;
		result = ModbusUtils.convert(v1, 0);
		expected = 1;
		assertEquals(expected, result);

		v1 = 65536;
		result = ModbusUtils.convert(v1, 1);
		expected = 1;
		assertEquals(expected, result);
	}
}
