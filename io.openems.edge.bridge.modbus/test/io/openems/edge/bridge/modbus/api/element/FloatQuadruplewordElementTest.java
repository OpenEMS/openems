package io.openems.edge.bridge.modbus.api.element;

import static io.openems.common.types.OpenemsType.DOUBLE;
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

public class FloatQuadruplewordElementTest {

	@Test
	public void testReadBigEndianMswLsw() throws OpenemsException {
		var sut = new ModbusTest.FC3ReadRegisters<>(//
				new FloatQuadruplewordElement(0), //
				DOUBLE);
		sut.element.setInputValue(new Register[] { //
				new SimpleRegister((byte) 0x40, (byte) 0x93), //
				new SimpleRegister((byte) 0x4A, (byte) 0x3D), //
				new SimpleRegister((byte) 0x70, (byte) 0xA3), //
				new SimpleRegister((byte) 0xD7, (byte) 0x0A) //
		});
		assertEquals(1234.56, (double) sut.channel.getNextValue().get(), 0.001);
	}

	@Test
	public void testReadBigEndianLswMsw() throws OpenemsException {
		var sut = new ModbusTest.FC3ReadRegisters<>(//
				new FloatQuadruplewordElement(0).wordOrder(WordOrder.LSWMSW), //
				DOUBLE);
		sut.element.setInputValue(new Register[] { //
				new SimpleRegister((byte) 0xD7, (byte) 0x0A), //
				new SimpleRegister((byte) 0x70, (byte) 0xA3), //
				new SimpleRegister((byte) 0x4A, (byte) 0x3D), //
				new SimpleRegister((byte) 0x40, (byte) 0x93) //
		});
		assertEquals(1234.56, (double) sut.channel.getNextValue().get(), 0.001);
	}

	@Test
	public void testReadLittleEndianMswLsw() throws OpenemsException {
		var sut = new ModbusTest.FC3ReadRegisters<>(//
				new FloatQuadruplewordElement(0).byteOrder(LITTLE_ENDIAN), //
				DOUBLE);
		sut.element.setInputValue(new Register[] { //
				new SimpleRegister((byte) 0x0A, (byte) 0xD7), //
				new SimpleRegister((byte) 0xA3, (byte) 0x70), //
				new SimpleRegister((byte) 0x3D, (byte) 0x4A), //
				new SimpleRegister((byte) 0x93, (byte) 0x40) //
		});
		assertEquals(1234.56F, (double) sut.channel.getNextValue().get(), 0.001);
	}

	@Test
	public void testReadLittleEndianLswMsw() throws OpenemsException {
		var sut = new ModbusTest.FC3ReadRegisters<>(//
				new FloatQuadruplewordElement(0).wordOrder(WordOrder.LSWMSW).byteOrder(LITTLE_ENDIAN), //
				DOUBLE);
		sut.element.setInputValue(new Register[] { //
				new SimpleRegister((byte) 0x93, (byte) 0x40), //
				new SimpleRegister((byte) 0x3D, (byte) 0x4A), //
				new SimpleRegister((byte) 0xA3, (byte) 0x70), //
				new SimpleRegister((byte) 0x0A, (byte) 0xD7) //
		});
		assertEquals(1234.56F, (double) sut.channel.getNextValue().get(), 0.001);
	}

	@Test
	public void testWriteLittleEndianLswMsw() throws IllegalArgumentException, OpenemsNamedException {
		var sut = new ModbusTest.FC16WriteRegisters<>(//
				new FloatQuadruplewordElement(0).wordOrder(LSWMSW).byteOrder(LITTLE_ENDIAN), //
				LONG);
		sut.channel.setNextWriteValueFromObject(1234.56F);
		var registers = sut.element.getNextWriteValueAndReset();
		assertArrayEquals(new byte[] { (byte) 0x93, (byte) 0x40 }, registers[0].toBytes());
		assertArrayEquals(new byte[] { (byte) 0x00, (byte) 0x48 }, registers[1].toBytes());
		assertArrayEquals(new byte[] { (byte) 0x00, (byte) 0x00 }, registers[2].toBytes());
		assertArrayEquals(new byte[] { (byte) 0x00, (byte) 0x00 }, registers[3].toBytes());
	}
}
