package io.openems.edge.bridge.modbus.api;

import static io.openems.edge.bridge.modbus.api.ModbusUtils.doNotRetry;
import static io.openems.edge.bridge.modbus.api.ModbusUtils.intToHexString;
import static io.openems.edge.bridge.modbus.api.ModbusUtils.registersToHexString;
import static io.openems.edge.bridge.modbus.api.ModbusUtils.retryOnNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ghgande.j2mod.modbus.procimg.SimpleInputRegister;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import io.openems.edge.bridge.modbus.api.task.Task.ExecuteState;

public class ModbusUtilsTest {

	@Test
	public void testRetryOnNull() {
		assertTrue(retryOnNull(ExecuteState.OK, null));
		assertFalse(retryOnNull(ExecuteState.OK, 123));
	}

	@Test
	public void testDoNotRetry() {
		assertFalse(doNotRetry(ExecuteState.OK, null));
		assertFalse(doNotRetry(ExecuteState.OK, 123));
	}

	@Test
	public void testIntToHexString() {
		assertEquals("00af", intToHexString(0xAF));
	}

	@Test
	public void testRegistersToHexString() {
		assertEquals("00aa 00ff", registersToHexString(new SimpleRegister(0xAA), new SimpleRegister(0xFF)));
	}

	@Test
	public void testInputRegistersToHexString() {
		assertEquals("00aa 00ff", registersToHexString(new SimpleInputRegister(0xAA), new SimpleInputRegister(0xFF)));
	}

}
