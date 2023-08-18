package io.openems.edge.bridge.modbus.api.element;

import static io.openems.common.types.OpenemsType.INTEGER;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractModbusBridge;

public class UnsignedWordElementTest {

	@Test
	public void testReadBigEndian() throws OpenemsException {
		var sut = new ModbusTest.FC3ReadRegisters<>(new UnsignedWordElement(0), INTEGER);
		sut.element.setInputValue(new Register[] { new SimpleRegister((byte) 0xAB, (byte) 0xCD) });
		assertEquals(0xABCD, sut.channel.getNextValue().get());
	}

	@Test
	public void testReadLittleEndian() throws OpenemsException {
		var sut = new ModbusTest.FC3ReadRegisters<>(new UnsignedWordElement(0).byteOrder(LITTLE_ENDIAN), INTEGER);
		sut.element.setInputValue(new Register[] { new SimpleRegister((byte) 0xAB, (byte) 0xCD) });
		assertEquals(0xCDAB, sut.channel.getNextValue().get());
	}

	@Test
	public void testWriteBigEndian() throws IllegalArgumentException, OpenemsNamedException {
		var sut = new ModbusTest.FC6WriteRegister<>(new UnsignedWordElement(0), INTEGER);
		sut.channel.setNextWriteValueFromObject(0xBCDE);
		var registers = sut.element.getNextWriteValueAndReset();
		assertArrayEquals(new byte[] { (byte) 0xBC, (byte) 0xDE }, registers[0].toBytes());
	}

	@Test
	public void testWriteLittleEndian() throws IllegalArgumentException, OpenemsNamedException {
		var sut = new ModbusTest.FC6WriteRegister<>(new UnsignedWordElement(0).byteOrder(LITTLE_ENDIAN), INTEGER);
		sut.channel.setNextWriteValueFromObject(0xBCDE);
		var registers = sut.element.getNextWriteValueAndReset();
		assertArrayEquals(new byte[] { (byte) 0xDE, (byte) 0xBC }, registers[0].toBytes());
	}

	@Test
	public void testInvalidate() throws OpenemsException {
		var sut = new ModbusTest.FC3ReadRegisters<>(new UnsignedWordElement(0), INTEGER);
		var bridge = (AbstractModbusBridge) sut.getBridgeModbus();
		sut.element.setInputValue(new Register[] { new SimpleRegister((byte) 0xAB, (byte) 0xCD) });
		assertEquals(0xABCD, sut.channel.getNextValue().get());
		sut.element.invalidate(bridge); // invalidValueCounter = 1
		assertEquals(0xABCD, sut.channel.getNextValue().get());
		sut.element.invalidate(bridge); // invalidValueCounter = 2
		assertNull(sut.channel.getNextValue().get());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testReadWrongLength() throws OpenemsException {
		var sut = new ModbusTest.FC3ReadRegisters<>(new UnsignedWordElement(0), INTEGER);
		sut.element.setInputValue(new Register[3]);
	}

	@Test
	public void testInputValueNull() throws OpenemsException {
		var sut = new ModbusTest.FC3ReadRegisters<>(new UnsignedWordElement(0), INTEGER);
		sut.element.setInputValue(null);
		assertNull(sut.channel.getNextValue().get());
	}

	@Test
	public void testWriteNone() throws IllegalArgumentException, OpenemsNamedException {
		var sut = new ModbusTest.FC6WriteRegister<>(new UnsignedWordElement(0), INTEGER);
		assertNull(sut.element.getNextWriteValueAndReset());
	}
}
