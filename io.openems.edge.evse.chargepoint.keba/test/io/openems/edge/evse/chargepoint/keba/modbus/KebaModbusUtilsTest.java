package io.openems.edge.evse.chargepoint.keba.modbus;

import static io.openems.edge.evse.chargepoint.keba.modbus.KebaModbusUtils.handleFirmwareVersion;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.junit.Test;

import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.evcs.keba.modbus.EvcsKebaModbusImpl;
import io.openems.edge.evse.chargepoint.keba.common.KebaModbus;

public class KebaModbusUtilsTest {

	@Test
	public void testHandleFirmwareVersion() {
		final var sut = new EvcsKebaModbusImpl();
		final Supplier<Integer> major = () -> ((IntegerReadChannel) sut.channel(KebaModbus.ChannelId.FIRMWARE_MAJOR))
				.getNextValue().get();
		final Supplier<Integer> minor = () -> ((IntegerReadChannel) sut.channel(KebaModbus.ChannelId.FIRMWARE_MINOR))
				.getNextValue().get();
		final Supplier<Integer> patch = () -> ((IntegerReadChannel) sut.channel(KebaModbus.ChannelId.FIRMWARE_PATCH))
				.getNextValue().get();
		final Supplier<String> string = () -> ((StringReadChannel) sut.channel(KebaModbus.ChannelId.FIRMWARE)) //
				.getNextValue().get();
		final BooleanSupplier deviceSoftwareOutdated = () -> ((StateChannel) sut
				.channel(KebaModbus.ChannelId.DEVICE_SOFTWARE_OUTDATED)) //
				.getNextValue().get();

		handleFirmwareVersion(sut, 10101);

		assertNotEquals(0, (int) major.get());
		assertNotEquals(0, (int) minor.get());
		assertNotEquals(39, (int) patch.get());
		assertNotEquals("0.0.39", string.get());

		assertEquals(1, (int) major.get());
		assertEquals(1, (int) minor.get());
		assertEquals(1, (int) patch.get());
		assertEquals("1.1.1", string.get());
		assertTrue(deviceSoftwareOutdated.getAsBoolean());

		handleFirmwareVersion(sut, null);
		assertNull(major.get());
		assertNull(minor.get());
		assertNull(patch.get());
		assertNull(string.get());
		assertFalse(deviceSoftwareOutdated.getAsBoolean());

		handleFirmwareVersion(sut, 10201);
		assertEquals(1, (int) major.get());
		assertEquals(2, (int) minor.get());
		assertEquals(1, (int) patch.get());
		assertEquals("1.2.1", string.get());
		assertTrue(deviceSoftwareOutdated.getAsBoolean());

		handleFirmwareVersion(sut, 10202);
		assertEquals(1, (int) major.get());
		assertEquals(2, (int) minor.get());
		assertEquals(2, (int) patch.get());
		assertEquals("1.2.2", string.get());
		assertFalse(deviceSoftwareOutdated.getAsBoolean());
	}
}
