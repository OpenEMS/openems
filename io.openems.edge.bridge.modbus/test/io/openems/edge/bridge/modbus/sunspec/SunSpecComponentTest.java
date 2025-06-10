package io.openems.edge.bridge.modbus.sunspec;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;

public class SunSpecComponentTest {

	@Test
	public void test() throws OpenemsException {
		var component = new DummySunSpecComponent();
		assertTrue(component.maximumTaskLenghth() <= 126);
	}

}
