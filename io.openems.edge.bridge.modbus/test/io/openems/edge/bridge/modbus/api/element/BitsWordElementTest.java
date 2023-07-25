package io.openems.edge.bridge.modbus.api.element;

import static io.openems.common.channel.AccessMode.READ_WRITE;
import static io.openems.common.types.OpenemsType.BOOLEAN;
import static io.openems.common.types.OpenemsType.INTEGER;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent.BitConverter;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId.ChannelIdImpl;
import io.openems.edge.common.channel.Doc;

public class BitsWordElementTest {

	@Test
	public void testRead() throws Exception {
		var sut = generateSut();

		final var channel0 = addBit(sut, 0);
		final var channel1 = addBit(sut, 1);
		final var channel2 = addBit(sut, 2, BitConverter.INVERT);

		// TODO ByteOrder is not handled here
		sut.element.setInputValue(new SimpleRegister((byte) 0x00, (byte) 0x01));

		assertTrue(channel0.getNextValue().get());
		assertFalse(channel1.getNextValue().get());
		assertTrue(channel2.getNextValue().get());
	}

	@Test
	public void testWriteNone1() throws Exception {
		var sut = generateSut();

		addBit(sut, 0);
		addBit(sut, 1);
		addBit(sut, 2, BitConverter.INVERT);
		addBit(sut, 3);

		assertNull(sut.element.getNextWriteValueAndReset());
	}

	@Test
	public void testWriteNone2() throws Exception {
		var sut = generateSut();

		final var channel0 = addBit(sut, 0);
		final var channel1 = addBit(sut, 1);
		final var channel2 = addBit(sut, 2, BitConverter.INVERT);
		addBit(sut, 3);

		channel0.setNextWriteValue(false);
		channel1.setNextWriteValue(true);
		channel2.setNextWriteValue(false);

		System.out.println("NOTE: the following IllegalArgumentException is expected");
		assertNull(sut.element.getNextWriteValueAndReset());
	}

	@Test
	public void testWriteBigEndian() throws Exception {
		var sut = generateSut();

		final var channel0 = addBit(sut, 0);
		final var channel1 = addBit(sut, 1);
		final var channel2 = addBit(sut, 2, BitConverter.INVERT);
		final var channel8 = addBit(sut, 8);

		channel0.setNextWriteValue(false);
		channel1.setNextWriteValue(true);
		channel2.setNextWriteValue(false);
		channel8.setNextWriteValue(true);

		var register = sut.element.getNextWriteValueAndReset();
		assertArrayEquals(new byte[] { (byte) 0x01, (byte) 0x06 }, register.toBytes());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHighIndex() throws Exception {
		var sut = generateSut();
		addBit(sut, 16);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testLowIndex() throws Exception {
		var sut = generateSut();
		addBit(sut, -1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNotBoolean() throws Exception {
		var sut = generateSut();
		addBit(sut, 0, null, OpenemsType.INTEGER);
	}

	private static ModbusTest.FC3ReadRegisters<BitsWordElement, ?> generateSut() throws IllegalArgumentException,
			IllegalAccessException, OpenemsException, NoSuchFieldException, SecurityException {
		var sut = new ModbusTest.FC3ReadRegisters<>(new BitsWordElement(0, null), INTEGER);

		// Some Reflection to properly initialize the BitsWordElement
		var field = BitsWordElement.class.getDeclaredField("component");
		field.setAccessible(true);
		field.set(sut.element, sut);

		return sut;
	}

	private static BooleanWriteChannel addBit(ModbusTest.FC3ReadRegisters<BitsWordElement, ?> sut, int i) {
		return addBit(sut, i, null);
	}

	private static BooleanWriteChannel addBit(ModbusTest.FC3ReadRegisters<BitsWordElement, ?> sut, int i,
			BitConverter bitConverter) {
		return addBit(sut, i, bitConverter, BOOLEAN);
	}

	private static <T extends Channel<?>> T addBit(ModbusTest.FC3ReadRegisters<BitsWordElement, ?> sut, int i,
			BitConverter bitConverter, OpenemsType openemsType) {
		var channelId = new ChannelIdImpl("CHANNEL_" + i, Doc.of(openemsType).accessMode(READ_WRITE));
		@SuppressWarnings("unchecked")
		var channel = (T) sut.addChannel(channelId);
		if (bitConverter != null) {
			sut.element.bit(i, channelId, bitConverter);
		} else {
			sut.element.bit(i, channelId);
		}
		return channel;
	}
}
