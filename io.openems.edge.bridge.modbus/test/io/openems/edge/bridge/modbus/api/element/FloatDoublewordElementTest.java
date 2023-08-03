package io.openems.edge.bridge.modbus.api.element;

import static io.openems.common.types.OpenemsType.FLOAT;
import static io.openems.edge.bridge.modbus.api.element.WordOrder.LSWMSW;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;

public class FloatDoublewordElementTest {

	@Test
	public void testReadBigEndianMswLsw() throws OpenemsException {
		var sut = new ModbusTest.FC3ReadRegisters<>(//
				new FloatDoublewordElement(0), //
				FLOAT);
		sut.element.setInputValue(new Register[] { //
				new SimpleRegister((byte) 0x44, (byte) 0x9A), //
				new SimpleRegister((byte) 0x51, (byte) 0xEC) //
		});
		assertEquals(1234.56F, (float) sut.channel.getNextValue().get(), 0.001F);
	}

	@Test
	public void testReadBigEndianLswMsw() throws OpenemsException {
		var sut = new ModbusTest.FC3ReadRegisters<>(//
				new FloatDoublewordElement(0).wordOrder(LSWMSW), //
				FLOAT);
		sut.element.setInputValue(new Register[] { //
				new SimpleRegister((byte) 0x51, (byte) 0xEC), //
				new SimpleRegister((byte) 0x44, (byte) 0x9A) //
		});
		assertEquals(1234.56F, (float) sut.channel.getNextValue().get(), 0.001F);
	}

	@Test
	public void testReadLittleEndianMswLsw() throws OpenemsException {
		var sut = new ModbusTest.FC3ReadRegisters<>(//
				new FloatDoublewordElement(0).byteOrder(LITTLE_ENDIAN), //
				FLOAT);
		sut.element.setInputValue(new Register[] { //
				new SimpleRegister((byte) 0xEC, (byte) 0x51), //
				new SimpleRegister((byte) 0x9A, (byte) 0x44) //
		});
		assertEquals(1234.56F, (float) sut.channel.getNextValue().get(), 0.001F);
	}

	@Test
	public void testReadLittleEndianLswMsw() throws OpenemsException {
		var sut = new ModbusTest.FC3ReadRegisters<>(//
				new FloatDoublewordElement(0).wordOrder(LSWMSW).byteOrder(LITTLE_ENDIAN), //
				FLOAT);
		sut.element.setInputValue(new Register[] { //
				new SimpleRegister((byte) 0x9A, (byte) 0x44), //
				new SimpleRegister((byte) 0xEC, (byte) 0x51) //
		});
		assertEquals(1234.56F, (float) sut.channel.getNextValue().get(), 0.001F);
	}

	@Test
	public void testWrite() throws IllegalArgumentException, OpenemsNamedException {
		var sut = new ModbusTest.FC16WriteRegisters<>(//
				new FloatDoublewordElement(0), //
				FLOAT);
		sut.channel.setNextWriteValueFromObject(1234.56F);
		var registers = sut.element.getNextWriteValueAndReset();
		assertArrayEquals(new byte[] { (byte) 0x44, (byte) 0x9A }, registers[0].toBytes());
		assertArrayEquals(new byte[] { (byte) 0x51, (byte) 0xEC }, registers[1].toBytes());
	}

}
