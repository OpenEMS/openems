package io.openems.edge.bridge.modbus.api;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.ADD;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.DIVIDE;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.MULTIPLY;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SUBTRACT;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ConversionTest {

	@Test
	public void multiplyTest() {
		var value = 10;
		var result = MULTIPLY(2).elementToChannel(value);
		assertEquals(20, result);
		var result2 = MULTIPLY(2).channelToElement(result);
		assertEquals(value, result2);
	}

	@Test
	public void divideTest() {
		var value = 10;
		var result = DIVIDE(2).elementToChannel(value);
		assertEquals(5, result);
		var result2 = DIVIDE(2).channelToElement(result);
		assertEquals(value, result2);
	}

	@Test
	public void addTest() {
		var value = 10;
		var result = ADD(2).elementToChannel(value);
		assertEquals(12, result);
		var result2 = ADD(2).channelToElement(result);
		assertEquals(value, result2);
	}

	@Test
	public void subtractTest() {
		var value = 10;
		var result = SUBTRACT(2).elementToChannel(value);
		assertEquals(8, result);
		var result2 = SUBTRACT(2).channelToElement(result);
		assertEquals(value, result2);
	}
}
