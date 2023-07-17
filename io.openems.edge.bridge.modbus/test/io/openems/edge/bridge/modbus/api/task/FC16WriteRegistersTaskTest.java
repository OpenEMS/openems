package io.openems.edge.bridge.modbus.api.task;

import static org.junit.Assert.assertEquals;

import java.util.Optional;

import org.junit.Test;

import com.ghgande.j2mod.modbus.msg.WriteMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.WriteMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.DummyModbusComponent;
import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;

public class FC16WriteRegistersTaskTest {

	@Test
	public void testMergeWriteRegisters() throws OpenemsException, IllegalArgumentException {
		final var element0 = new UnsignedWordElement(0);
		final var element1 = new UnsignedWordElement(1);
		final var element2 = new UnsignedWordElement(2);
		final var element3 = new UnsignedWordElement(3);
		var elements = new ModbusElement<?, ?>[] { element0, element1, element2, element3 };

		// Has Hole (no value for element2)
		element0.setNextWriteValue(Optional.empty());
		element1.setNextWriteValue(Optional.of(100));
		element2.setNextWriteValue(Optional.empty());
		element3.setNextWriteValue(Optional.of(300));

		{
			var result = FC16WriteRegistersTask.mergeWriteRegisters(elements, (message) -> System.out.println(message));

			assertEquals(2, result.size()); // Two individual requests
			assertEquals(1, result.get(0).startAddress());
			assertEquals(1, result.get(0).getRegisters().length);
			assertEquals(3, result.get(1).startAddress());
			assertEquals(1, result.get(1).getRegisters().length);
		}

		// Has NO Hole (all values set)
		element0.setNextWriteValue(Optional.of(100));
		element1.setNextWriteValue(Optional.of(200));
		element2.setNextWriteValue(Optional.of(300));
		element3.setNextWriteValue(Optional.of(400));
		{
			var result = FC16WriteRegistersTask.mergeWriteRegisters(elements, (message) -> System.out.println(message));

			assertEquals(1, result.size()); // One combined request
			assertEquals(0, result.get(0).startAddress());
			assertEquals(4, result.get(0).getRegisters().length);
		}
	}

	@Test
	public void testToLogMessage() throws OpenemsException {
		var component = new DummyModbusComponent();
		var task = new FC16WriteRegistersTask(30, new UnsignedDoublewordElement(30));
		task.setParent(component);
		var request = new WriteMultipleRegistersRequest(30,
				new Register[] { new SimpleRegister(123), new SimpleRegister(456) });
		var response = (WriteMultipleRegistersResponse) request.getResponse();

		assertEquals("FC16WriteRegisters [device0;unitid=1;ref=30/0x1e;length=2;request=007b 01c8]",
				task.toLogMessage(LogVerbosity.READS_AND_WRITES_VERBOSE, request, response));
	}
}
