package io.openems.edge.bridge.modbus.sunspec;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;

public class AbstractOpenemsSunSpecComponentTest {

	@Test
	public void testPreprocessModbusElements() throws OpenemsException {
		var elements = new ArrayList<ModbusElement>();
		var startAddress = 0;
		for (var point : DefaultSunSpecModel.S_701.points()) {
			var element = point.get().generateModbusElement(startAddress);
			startAddress += element.length;
			elements.add(element);
		}

		var sut = AbstractOpenemsSunSpecComponent.preprocessModbusElements(elements);
		assertEquals(2, sut.size()); // two sublists
		assertEquals(69, sut.get(0).size()); // first task
		assertEquals(1, sut.get(1).size()); // second task
		assertEquals(StringWordElement.class, sut.get(1).get(0).getClass()); // second task
	}

}
