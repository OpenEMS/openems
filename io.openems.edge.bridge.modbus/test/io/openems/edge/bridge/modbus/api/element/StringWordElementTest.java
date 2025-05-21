package io.openems.edge.bridge.modbus.api.element;

import static io.openems.common.types.OpenemsType.STRING;
import static io.openems.edge.bridge.modbus.api.element.WordOrder.LSWMSW;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;

public class StringWordElementTest {

	@Test
	public void testReadBigEndianMswLsw() throws OpenemsException {
		var sut = new ModbusTest.FC3ReadRegisters<>(//
				new StringWordElement(0, 4), STRING);
		sut.element.setInputValue(new Register[] { //
				new SimpleRegister((byte) 0x4F, (byte) 0x70), //
				new SimpleRegister((byte) 0x65, (byte) 0x6E), //
				new SimpleRegister((byte) 0x45, (byte) 0x4D), //
				new SimpleRegister((byte) 0x53, (byte) 0x00) //
		});
		assertEquals("OpenEMS", sut.channel.getNextValue().get());
	}

	@Test
	public void testReadBigEndianLswMsw() throws OpenemsException {
		var sut = new ModbusTest.FC3ReadRegisters<>(//
				new StringWordElement(0, 4).wordOrder(LSWMSW), STRING);
		sut.element.setInputValue(new Register[] { //
				new SimpleRegister((byte) 0x4F, (byte) 0x70), //
				new SimpleRegister((byte) 0x65, (byte) 0x6E), //
				new SimpleRegister((byte) 0x45, (byte) 0x4D), //
				new SimpleRegister((byte) 0x53, (byte) 0x00) //
		});
		assertEquals("S EMenOp", sut.channel.getNextValue().get());
	}

	@Test
	public void testReadLittleEndianMswLsw() throws OpenemsException {
		var sut = new ModbusTest.FC3ReadRegisters<>(//
				new StringWordElement(0, 4).byteOrder(LITTLE_ENDIAN), STRING);
		sut.element.setInputValue(new Register[] { //
				new SimpleRegister((byte) 0x4F, (byte) 0x70), //
				new SimpleRegister((byte) 0x65, (byte) 0x6E), //
				new SimpleRegister((byte) 0x45, (byte) 0x4D), //
				new SimpleRegister((byte) 0x53, (byte) 0x00) //
		});
		assertEquals("pOneME S", sut.channel.getNextValue().get());
	}

	@Test
	public void testReadLittleEndianLswMsw() throws OpenemsException {
		var sut = new ModbusTest.FC3ReadRegisters<>(//
				new StringWordElement(0, 4).wordOrder(LSWMSW).byteOrder(LITTLE_ENDIAN), STRING);
		sut.element.setInputValue(new Register[] { //
				new SimpleRegister((byte) 0x4F, (byte) 0x70), //
				new SimpleRegister((byte) 0x65, (byte) 0x6E), //
				new SimpleRegister((byte) 0x45, (byte) 0x4D), //
				new SimpleRegister((byte) 0x53, (byte) 0x00) //
		});
		assertEquals("SMEnepO", sut.channel.getNextValue().get());
	}

	@Test
	public void testWriteBigEndian() throws IllegalArgumentException, OpenemsNamedException {
		var sut = new ModbusTest.FC16WriteRegisters<>(new StringWordElement(0, 6), STRING);
		sut.channel.setNextWriteValueFromObject("OpenEMS");
		var registers = sut.element.getNextWriteValueAndReset();
		assertEquals(6, registers.length);
		assertArrayEquals(new byte[] { (byte) 0x4F, (byte) 0x70 }, registers[0].toBytes());
		assertArrayEquals(new byte[] { (byte) 0x65, (byte) 0x6E }, registers[1].toBytes());
		assertArrayEquals(new byte[] { (byte) 0x45, (byte) 0x4D }, registers[2].toBytes());
		assertArrayEquals(new byte[] { (byte) 0x53, (byte) 0x00 }, registers[3].toBytes());
		assertArrayEquals(new byte[] { (byte) 0x00, (byte) 0x00 }, registers[4].toBytes());
	}
}
