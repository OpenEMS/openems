package io.openems.edge.bridge.modbus.api.task;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.DummyModbusComponent;
import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.common.taskmanager.Priority;

public class FC4ReadInputRegistersTaskTest {

	@Test
	public void testToLogMessage() throws OpenemsException {
		var component = new DummyModbusComponent();
		var element = new UnsignedDoublewordElement(20);
		var task = new FC4ReadInputRegistersTask(20, Priority.LOW, element);
		task.setParent(component);
		var request = task.createModbusRequest();
		var response = request.getResponse();
		response.setRegisters(new Register[] { new SimpleRegister(987), new SimpleRegister(654) });

		assertEquals("FC4ReadInputRegisters [device0;unitid=1;priority=LOW;ref=20/0x14;length=2;response=03db 028e]",
				task.toLogMessage(LogVerbosity.READS_AND_WRITES_VERBOSE, request, response));

		task.handleResponse(element, 0, response.getRegisters());
	}
}
