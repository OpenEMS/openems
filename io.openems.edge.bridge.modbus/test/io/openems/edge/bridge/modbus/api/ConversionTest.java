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

	@Test
	public void multiplyTest() {
		var value = 10;
		var result = ElementToChannelConverter.multiply(2).elementToChannel(value);
		assertEquals(20, result);
		var result2 = ElementToChannelConverter.multiply(2).channelToElement(result);
		assertEquals(value, result2);
	}

	@Test
	public void divideTest() {
		var value = 10;
		var result = ElementToChannelConverter.divide(2).elementToChannel(value);
		assertEquals(5, result);
		var result2 = ElementToChannelConverter.divide(2).channelToElement(result);
		assertEquals(value, result2);
	}

	@Test
	public void addTest() {
		var value = 10;
		var result = ElementToChannelConverter.add(2).elementToChannel(value);
		assertEquals(12, result);
		var result2 = ElementToChannelConverter.add(2).channelToElement(result);
		assertEquals(value, result2);
	}

	@Test
	public void subtractTest() {
		var value = 10;
		var result = ElementToChannelConverter.subtract(2).elementToChannel(value);
		assertEquals(8, result);
		var result2 = ElementToChannelConverter.subtract(2).channelToElement(result);
		assertEquals(value, result2);
	}
}
