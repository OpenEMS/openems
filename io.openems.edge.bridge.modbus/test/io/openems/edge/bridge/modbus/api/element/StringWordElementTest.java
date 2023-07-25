package io.openems.edge.bridge.modbus.api.element;

import static io.openems.common.types.OpenemsType.STRING;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;

public class StringWordElementTest {

	@Test
	public void testReadBigEndian() throws OpenemsException {
		var sut = new ModbusTest.FC3ReadRegisters<>(//
				new StringWordElement(0, 4), STRING);
		sut.element.setInputRegisters(new Register[] { //
				new SimpleRegister((byte) 0x4F, (byte) 0x70), //
				new SimpleRegister((byte) 0x65, (byte) 0x6E), //
				new SimpleRegister((byte) 0x45, (byte) 0x4D), //
				new SimpleRegister((byte) 0x53, (byte) 0x00) //
		});
		assertEquals("OpenEMS", sut.channel.getNextValue().get());
	}

	@Test
	public void testReadLittleEndian() throws OpenemsException {
		var sut = new ModbusTest.FC3ReadRegisters<>(//
				// TODO byteOrder is not applied
				new StringWordElement(0, 4).byteOrder(LITTLE_ENDIAN), STRING);
		sut.element.setInputRegisters(new Register[] { //
				new SimpleRegister((byte) 0x4F, (byte) 0x70), //
				new SimpleRegister((byte) 0x65, (byte) 0x6E), //
				new SimpleRegister((byte) 0x45, (byte) 0x4D), //
				new SimpleRegister((byte) 0x53, (byte) 0x00) //
		});
		assertEquals("OpenEMS", sut.channel.getNextValue().get());
	}

	@Test
	public void testWriteBigEndian() throws IllegalArgumentException, OpenemsNamedException {
		// TODO StringWordElement._setNextWriteValue is non-functional
		// var sut = new ModbusTest.FC6WriteRegister<>(new StringWordElement(0, 10),
		// STRING);
		// sut.channel.setNextWriteValueFromObject("OpenEMS ");
		// var registers = sut.element.getNextWriteValueAndReset().get();
		// assertArrayEquals(new byte[] { (byte) 0x4F, (byte) 0x70 },
		// registers[0].toBytes());
	}
}
