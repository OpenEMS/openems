package io.openems.edge.bridge.modbus.api.element;

import static io.openems.common.types.OpenemsType.BOOLEAN;
import static org.junit.Assert.assertEquals;

import java.util.Optional;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;

public class CoilElementTest {

	@Test
	public void testRead() throws OpenemsException {
		var sut = new ModbusTest.FC1ReadCoils<>(new CoilElement(0), BOOLEAN);
		sut.element.setInputCoil(true);
		assertEquals(true, sut.channel.getNextValue().get());

		sut.element.setInputCoil(false);
		assertEquals(false, sut.channel.getNextValue().get());

		sut.element.setInputCoil(null);
		assertEquals(null, sut.channel.getNextValue().get());
	}

	@Test
	public void testWrite() throws IllegalArgumentException, OpenemsNamedException {
		var sut = new ModbusTest.FC5WriteCoil<>(new CoilElement(0), BOOLEAN);
		sut.channel.setNextWriteValueFromObject(true);
		assertEquals(true, ((CoilElement) sut.element).getNextWriteValueAndReset().get());

		sut.channel.setNextWriteValueFromObject(null);
		assertEquals(Optional.empty(), ((CoilElement) sut.element).getNextWriteValueAndReset());
	}

}
