package io.openems.edge.bridge.modbus.api.element;

import static io.openems.common.types.OpenemsType.SHORT;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;

public class SignedWordElementTest {

	@Test
	public void testReadBigEndian() throws OpenemsException {
		var sut = new ModbusTest.FC3ReadRegisters<>(new SignedWordElement(0), SHORT);
		sut.element.setInputValue(new Register[] { new SimpleRegister((byte) 0xAB, (byte) 0xCD) });
		assertEquals((short) 0xABCD, sut.channel.getNextValue().get());
	}

	@Test
	public void testReadLittleEndian() throws OpenemsException {
		var sut = new ModbusTest.FC3ReadRegisters<>(new SignedWordElement(0).byteOrder(LITTLE_ENDIAN), SHORT);
		sut.element.setInputValue(new Register[] { new SimpleRegister((byte) 0xAB, (byte) 0xCD) });
		assertEquals((short) 0xCDAB, sut.channel.getNextValue().get());
	}

	@Test
	public void testWriteBigEndian() throws IllegalArgumentException, OpenemsNamedException {
		var sut = new ModbusTest.FC6WriteRegister<>(new SignedWordElement(0), SHORT);
		sut.channel.setNextWriteValueFromObject(0x1234);
		var registers = sut.element.getNextWriteValueAndReset();
		assertArrayEquals(new byte[] { (byte) 0x12, (byte) 0x34 }, registers[0].toBytes());
	}

	@Test
	public void testWriteLittleEndian() throws IllegalArgumentException, OpenemsNamedException {
		var sut = new ModbusTest.FC6WriteRegister<>(new SignedWordElement(0).byteOrder(LITTLE_ENDIAN), SHORT);
		sut.channel.setNextWriteValueFromObject(0x1234);
		var registers = sut.element.getNextWriteValueAndReset();
		assertArrayEquals(new byte[] { (byte) 0x34, (byte) 0x12 }, registers[0].toBytes());
	}
}
