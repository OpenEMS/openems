package io.openems.edge.bridge.modbus.api.task;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AbstractTaskTest {

	@Test
	public void testGenerateLogMessage() {
		assertEquals("FC4ReadInputRegisters [1:255/0xff]",
				AbstractTask.generateLogMessage("", "FC4ReadInputRegisters [1:255/0xff]", ""));

		assertEquals("MyPrefix FC4ReadInputRegisters [1:255/0xff] MySuffix",
				AbstractTask.generateLogMessage("MyPrefix", "FC4ReadInputRegisters [1:255/0xff]", "MySuffix"));
	}
}