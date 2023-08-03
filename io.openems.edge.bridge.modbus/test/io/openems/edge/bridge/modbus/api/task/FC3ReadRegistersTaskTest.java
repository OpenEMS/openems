package io.openems.edge.bridge.modbus.api.task;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.DummyModbusComponent;
import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.common.taskmanager.Priority;

public class FC3ReadRegistersTaskTest {

	@Test
	public void testToLogMessage() throws OpenemsException {
		var component = new DummyModbusComponent();
		var task = new FC3ReadRegistersTask(20, Priority.LOW, new UnsignedDoublewordElement(20));
		task.setParent(component);
		var request = task.createModbusRequest();
		var response = (ReadMultipleRegistersResponse) request.getResponse();
		response.setRegisters(new Register[] { new SimpleRegister(100), new SimpleRegister(200) });

		assertEquals("FC3ReadHoldingRegisters [device0;unitid=1;priority=LOW;ref=20/0x14;length=2;response=0064 00c8]",
				task.toLogMessage(LogVerbosity.READS_AND_WRITES_VERBOSE, request, response));
	}
}
