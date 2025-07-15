package io.openems.edge.evse.chargepoint.keba.modbus;

import static io.openems.edge.evse.chargepoint.keba.modbus.KebaModbusUtils.CONVERT_FIRMWARE_VERSION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class KebaModbusUtilsTest {

	@Test
	public void testConvertFirmwareVersion() {
		assertNull(CONVERT_FIRMWARE_VERSION.elementToChannel(null));
		assertEquals("1.0.0", CONVERT_FIRMWARE_VERSION.elementToChannel(10000));
		assertEquals("1.1.1", CONVERT_FIRMWARE_VERSION.elementToChannel(10101));
		assertEquals("1.1.4", CONVERT_FIRMWARE_VERSION.elementToChannel(10104));
	}
}
