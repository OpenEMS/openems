package io.openems.edge.bridge.modbus.api.element;

import static io.openems.common.channel.AccessMode.READ_WRITE;
import static io.openems.common.types.OpenemsType.BOOLEAN;
import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.common.utils.ReflectionUtils.setAttributeViaReflection;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.function.Function;

import org.junit.Test;

import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.AbstractModbusBridge;
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

		sut.element.setInputValue(new Register[] { new SimpleRegister((byte) 0x00, (byte) 0x01) });

		assertTrue(channel0.getNextValue().get());
		assertFalse(channel1.getNextValue().get());
		assertTrue(channel2.getNextValue().get());
	}

	@Test
	public void testInvalidate() throws Exception {
		var sut = generateSut();
		final var bridge = (AbstractModbusBridge) sut.getBridgeModbus();

		final var channel0 = addBit(sut, 0);

		sut.element.setInputValue(new Register[] { new SimpleRegister((byte) 0x00, (byte) 0x01) });

		assertTrue(channel0.getNextValue().get());
		sut.element.invalidate(bridge); // invalidValueCounter = 1
		assertTrue(channel0.getNextValue().get());
		sut.element.invalidate(bridge); // invalidValueCounter = 2
		assertNull(channel0.getNextValue().get());
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

		var registers = sut.element.getNextWriteValueAndReset();
		assertArrayEquals(new byte[] { (byte) 0x01, (byte) 0x06 }, registers[0].toBytes());
	}

	@Test
	public void testSetToNull() throws Exception {
		var sutTrue = generateSut();

		final var channel0 = addBit(sutTrue, 0);// true
		final var channel1 = addBit(sutTrue, 1);// null
		final var channel2 = addBit(sutTrue, 2);// null
		convert(sutTrue, value -> {
			if (value[0] != null && value[0]) { // Reset all values if bit 0 is true
				return new Boolean[16];
			}
			return value;
		});
		sutTrue.element.setInputValue(new Register[] { new SimpleRegister(7) }); // 0x7 = 111b
		assertNull(channel0.getNextValue().get());
		assertNull(channel1.getNextValue().get());
		assertNull(channel2.getNextValue().get());

		var sutFalse = generateSut();

		final var channel3 = addBit(sutFalse, 0);// null
		final var channel4 = addBit(sutFalse, 1);// false
		final var channel5 = addBit(sutFalse, 2);// null
		convert(sutFalse, value -> {
			if (value[1] != null && !value[1]) {
				return new Boolean[16]; // Reset all values if bit 1 is false
			}
			return value;
		});

		sutFalse.element.setInputValue(new Register[] { new SimpleRegister(5) }); // 0x5 = 101b
		assertNull(channel3.getNextValue().get());
		assertNull(channel4.getNextValue().get());
		assertNull(channel5.getNextValue().get());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRegistersLengthDoesNotMatch() throws Exception {
		var sut = generateSut();
		sut.element.setInputValue(new Register[2]);
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
		setAttributeViaReflection(sut.element, "component", sut);
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

	private static void convert(ModbusTest.FC3ReadRegisters<BitsWordElement, ?> sut,
			Function<Boolean[], Boolean[]> converter) {
		sut.element.convert(converter);
	}
}
