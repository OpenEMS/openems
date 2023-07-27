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

public class UnsignedQuadruplewordElementTest {

	@Test
	public void testReadBigEndianMswLsw() throws OpenemsException {
		var sut = new ModbusTest.FC3ReadRegisters<>(//
				new UnsignedQuadruplewordElement(0), //
				LONG);
		sut.element.debug();
		sut.element.setInputRegisters(new Register[] { //
				new SimpleRegister((byte) 0x01, (byte) 0x23), //
				new SimpleRegister((byte) 0x45, (byte) 0x67), //
				new SimpleRegister((byte) 0x89, (byte) 0xAB), //
				new SimpleRegister((byte) 0xCD, (byte) 0xEF), //
		});
		assertEquals(0x0123_4567_89AB_CDEFL, sut.channel.getNextValue().get());
	}

	@Test
	public void testReadBigEndianLswMsw() throws OpenemsException {
		var sut = new ModbusTest.FC3ReadRegisters<>(//
				new UnsignedQuadruplewordElement(0).wordOrder(LSWMSW), //
				LONG);
		sut.element.setInputRegisters(new Register[] { //
				new SimpleRegister((byte) 0x01, (byte) 0x23), //
				new SimpleRegister((byte) 0x45, (byte) 0x67), //
				new SimpleRegister((byte) 0x89, (byte) 0xAB), //
				new SimpleRegister((byte) 0xCD, (byte) 0xEF), //
		});
		assertEquals(0xCDEF_89AB_4567_0123L, sut.channel.getNextValue().get());
	}

	@Test
	public void testReadLittleEndianMswLsw() throws OpenemsException {
		var sut = new ModbusTest.FC3ReadRegisters<>(//
				new UnsignedQuadruplewordElement(0).byteOrder(LITTLE_ENDIAN), //
				LONG);
		sut.element.setInputRegisters(new Register[] { //
				new SimpleRegister((byte) 0x01, (byte) 0x23), //
				new SimpleRegister((byte) 0x45, (byte) 0x67), //
				new SimpleRegister((byte) 0x89, (byte) 0xAB), //
				new SimpleRegister((byte) 0xCD, (byte) 0xEF), //
		});
		assertEquals(0xEFCD_AB89_6745_2301L, sut.channel.getNextValue().get());
	}

	@Test
	public void testReadLittleEndianLswMsw() throws OpenemsException {
		var sut = new ModbusTest.FC3ReadRegisters<>(//
				new UnsignedQuadruplewordElement(0).wordOrder(LSWMSW).byteOrder(LITTLE_ENDIAN), //
				LONG);
		sut.element.setInputRegisters(new Register[] { //
				new SimpleRegister((byte) 0x01, (byte) 0x23), //
				new SimpleRegister((byte) 0x45, (byte) 0x67), //
				new SimpleRegister((byte) 0x89, (byte) 0xAB), //
				new SimpleRegister((byte) 0xCD, (byte) 0xEF), //
		});
		assertEquals(0x2301_6745_AB89_EFCDL, sut.channel.getNextValue().get());
	}

	@Test
	public void testWriteLittleEndianMswLsw() throws IllegalArgumentException, OpenemsNamedException {
		var sut = new ModbusTest.FC16WriteRegisters<>(//
				new UnsignedQuadruplewordElement(0).byteOrder(LITTLE_ENDIAN), //
				LONG);
		sut.channel.setNextWriteValueFromObject(0x2301_6745_AB89_EFCDL);
		var registers = sut.element.getNextWriteValueAndReset().get();
		assertArrayEquals(new byte[] { (byte) 0xCD, (byte) 0xEF }, registers[0].toBytes());
		assertArrayEquals(new byte[] { (byte) 0x89, (byte) 0xAB }, registers[1].toBytes());
		assertArrayEquals(new byte[] { (byte) 0x45, (byte) 0x67 }, registers[2].toBytes());
		assertArrayEquals(new byte[] { (byte) 0x01, (byte) 0x23 }, registers[3].toBytes());
	}

	@Test
	public void testWriteLittleEndianLswMsw() throws IllegalArgumentException, OpenemsNamedException {
		var sut = new ModbusTest.FC16WriteRegisters<>(//
				new UnsignedQuadruplewordElement(0).wordOrder(LSWMSW).byteOrder(LITTLE_ENDIAN), //
				LONG);
		sut.channel.setNextWriteValueFromObject(0x2301_6745_AB89_EFCDL);
		var registers = sut.element.getNextWriteValueAndReset().get();
		assertArrayEquals(new byte[] { (byte) 0x01, (byte) 0x23 }, registers[0].toBytes());
		assertArrayEquals(new byte[] { (byte) 0x45, (byte) 0x67 }, registers[1].toBytes());
		assertArrayEquals(new byte[] { (byte) 0x89, (byte) 0xAB }, registers[2].toBytes());
		assertArrayEquals(new byte[] { (byte) 0xCD, (byte) 0xEF }, registers[3].toBytes());
	}
}
