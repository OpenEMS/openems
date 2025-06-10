package io.openems.edge.bridge.modbus.api.element;

import static io.openems.common.types.OpenemsType.LONG;
import static io.openems.edge.bridge.modbus.api.element.WordOrder.LSWMSW;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;

public class UnsignedDoublewordElementTest {

	@Test
	public void testReadBigEndianMswLsw() throws OpenemsException {
		var sut = new ModbusTest.FC3ReadRegisters<>(//
				new UnsignedDoublewordElement(0), //
				LONG);
		sut.element.setInputValue(new Register[] { //
				new SimpleRegister((byte) 0xAB, (byte) 0xCD), //
				new SimpleRegister((byte) 0x12, (byte) 0x34) //
		});
		assertEquals(0xABCD_1234L, sut.channel.getNextValue().get());
	}

	@Test
	public void testReadBigEndianLswMsw() throws OpenemsException {
		var sut = new ModbusTest.FC3ReadRegisters<>(//
				new UnsignedDoublewordElement(0).wordOrder(LSWMSW), //
				LONG);
		sut.element.setInputValue(new Register[] { //
				new SimpleRegister((byte) 0xAB, (byte) 0xCD), //
				new SimpleRegister((byte) 0x12, (byte) 0x34) //
		});
		assertEquals(0x1234_ABCDL, sut.channel.getNextValue().get());
	}

	@Test
	public void testReadLittleEndianMswLsw() throws OpenemsException {
		var sut = new ModbusTest.FC3ReadRegisters<>(//
				new UnsignedDoublewordElement(0).byteOrder(LITTLE_ENDIAN), //
				LONG);
		sut.element.setInputValue(new Register[] { //
				new SimpleRegister((byte) 0xAB, (byte) 0xCD), //
				new SimpleRegister((byte) 0x12, (byte) 0x34) //
		});
		assertEquals(0x3412_CDABL, sut.channel.getNextValue().get());
	}

	@Test
	public void testReadLittleEndianLswMsw() throws OpenemsException {
		var sut = new ModbusTest.FC3ReadRegisters<>(//
				new UnsignedDoublewordElement(0).wordOrder(LSWMSW).byteOrder(LITTLE_ENDIAN), //
				LONG);
		sut.element.setInputValue(new Register[] { //
				new SimpleRegister((byte) 0xAB, (byte) 0xCD), //
				new SimpleRegister((byte) 0x12, (byte) 0x34) //
		});
		assertEquals(0xCDAB_3412L, sut.channel.getNextValue().get());
	}

	@Test
	public void testWriteBigEndianMswLsw() throws IllegalArgumentException, OpenemsNamedException {
		var sut = new ModbusTest.FC16WriteRegisters<>(//
				new UnsignedDoublewordElement(0), //
				LONG);
		sut.channel.setNextWriteValueFromObject(0xABCD1234L);
		var registers = sut.element.getNextWriteValueAndReset();
		assertArrayEquals(new byte[] { (byte) 0xAB, (byte) 0xCD }, registers[0].toBytes());
		assertArrayEquals(new byte[] { (byte) 0x12, (byte) 0x34 }, registers[1].toBytes());
	}

	@Test
	public void testWriteBigEndianLswMsw() throws IllegalArgumentException, OpenemsNamedException {
		var sut = new ModbusTest.FC16WriteRegisters<>(//
				new UnsignedDoublewordElement(0).wordOrder(LSWMSW), //
				LONG);
		sut.channel.setNextWriteValueFromObject(0xABCD1234L);
		var registers = sut.element.getNextWriteValueAndReset();
		assertArrayEquals(new byte[] { (byte) 0x12, (byte) 0x34 }, registers[0].toBytes());
		assertArrayEquals(new byte[] { (byte) 0xAB, (byte) 0xCD }, registers[1].toBytes());
	}

	@Test
	public void testWriteLittleEndianMswLsw() throws IllegalArgumentException, OpenemsNamedException {
		var sut = new ModbusTest.FC16WriteRegisters<>(//
				new UnsignedDoublewordElement(0).byteOrder(LITTLE_ENDIAN), //
				LONG);
		sut.channel.setNextWriteValueFromObject(0xABCD1234L);
		var registers = sut.element.getNextWriteValueAndReset();
		assertArrayEquals(new byte[] { (byte) 0x34, (byte) 0x12 }, registers[0].toBytes());
		assertArrayEquals(new byte[] { (byte) 0xCD, (byte) 0xAB }, registers[1].toBytes());
	}

	@Test
	public void testWriteLittleEndianLswMsw() throws IllegalArgumentException, OpenemsNamedException {
		var sut = new ModbusTest.FC16WriteRegisters<>(//
				new UnsignedDoublewordElement(0).wordOrder(LSWMSW).byteOrder(LITTLE_ENDIAN), //
				LONG);
		sut.channel.setNextWriteValueFromObject(0xABCD1234L);
		var registers = sut.element.getNextWriteValueAndReset();
		assertArrayEquals(new byte[] { (byte) 0xCD, (byte) 0xAB }, registers[0].toBytes());
		assertArrayEquals(new byte[] { (byte) 0x34, (byte) 0x12 }, registers[1].toBytes());
	}
}
