package io.openems.edge.bridge.modbus.api;

import static org.junit.Assert.*;

import org.junit.Test;

public class ConversionTest {

	@Test
	public void testShortConversions() {
	
		int v1 = 0;
		Short result = AbstractOpenemsModbusComponent.convert(v1, 0);
		Short expected = 0;
		assertEquals(expected, result);
	
		v1 = 1;
		result = AbstractOpenemsModbusComponent.convert(v1, 0);
		expected = 1;
		assertEquals(expected, result);
		
		v1 = 65536;
		result = AbstractOpenemsModbusComponent.convert(v1, 1);
		expected = 1;
		assertEquals(expected, result);
	}
}
