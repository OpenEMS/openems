package io.openems.edge.bridge.modbus.api.task;

import static org.junit.Assert.assertEquals;

import java.util.Optional;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;

public class FC16WriteRegistersTaskTest {

	@Test
	public void testMergeWriteRegisters() throws OpenemsException, IllegalArgumentException {
		final var element0 = new UnsignedWordElement(0);
		final var element1 = new UnsignedWordElement(1);
		final var element2 = new UnsignedWordElement(2);
		var elements = new ModbusElement<?>[] { element0, element1, element2 };

		// Has Hole (no value for element1)
		element0.setNextWriteValue(Optional.of(100));
		element1.setNextWriteValue(Optional.empty());
		element2.setNextWriteValue(Optional.of(300));

		{
			var result = FC16WriteRegistersTask.mergeWriteRegisters(elements, (message) -> System.out.println(message));

			assertEquals(2, result.size()); // Two individual requests
			assertEquals(0, result.get(0).startAddress());
			assertEquals(1, result.get(0).getRegisters().length);
			assertEquals(2, result.get(1).startAddress());
			assertEquals(1, result.get(1).getRegisters().length);
		}

		// Has NO Hole (all values set)
		element0.setNextWriteValue(Optional.of(100));
		element1.setNextWriteValue(Optional.of(200));
		element2.setNextWriteValue(Optional.of(300));
		{
			var result = FC16WriteRegistersTask.mergeWriteRegisters(elements, (message) -> System.out.println(message));

			assertEquals(1, result.size()); // One combined request
			assertEquals(0, result.get(0).startAddress());
			assertEquals(3, result.get(0).getRegisters().length);
		}
	}

}
