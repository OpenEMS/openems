package io.openems.edge.evse.chargepoint.keba.modbus;

import static io.openems.edge.evse.chargepoint.keba.modbus.KebaModbusUtils.handleFirmwareVersion;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.evcs.keba.modbus.EvcsKebaModbusImpl;
import io.openems.edge.evse.chargepoint.keba.common.KebaModbus;

public class KebaModbusUtilsTest {

	@Test
	public void testHandleFirmwareVersion() {
		final var sut = new EvcsKebaModbusImpl();
		final IntegerReadChannel major = sut.channel(KebaModbus.ChannelId.FIRMWARE_MAJOR);
		final IntegerReadChannel minor = sut.channel(KebaModbus.ChannelId.FIRMWARE_MINOR);
		final IntegerReadChannel patch = sut.channel(KebaModbus.ChannelId.FIRMWARE_PATCH);
		final StringReadChannel string = sut.channel(KebaModbus.ChannelId.FIRMWARE);

		handleFirmwareVersion(sut, 10101);

		assertNotEquals(0, (int) major.getNextValue().get());
		assertNotEquals(0, (int) minor.getNextValue().get());
		assertNotEquals(39, (int) patch.getNextValue().get());
		assertNotEquals("0.0.39", string.getNextValue().get());

		assertEquals(1, (int) major.getNextValue().get());
		assertEquals(1, (int) minor.getNextValue().get());
		assertEquals(1, (int) patch.getNextValue().get());
		assertEquals("1.1.1", string.getNextValue().get());

		handleFirmwareVersion(sut, null);
		assertNull(major.getNextValue().get());
		assertNull(minor.getNextValue().get());
		assertNull(patch.getNextValue().get());
		assertNull(string.getNextValue().get());
	}
}
