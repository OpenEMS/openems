package io.openems.edge.bridge.modbus.api.task;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ghgande.j2mod.modbus.msg.ReadCoilsResponse;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.DummyModbusComponent;
import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.bridge.modbus.api.element.DummyCoilElement;
import io.openems.edge.common.taskmanager.Priority;

public class FC1ReadCoilsTaskTest {

	@Test
	public void testToLogMessage() throws OpenemsException {
		var component = new DummyModbusComponent();
		var task = new FC1ReadCoilsTask(10, Priority.HIGH, new DummyCoilElement(10), new DummyCoilElement(11));
		task.setParent(component);
		var request = task.createModbusRequest();
		var response = (ReadCoilsResponse) request.getResponse();
		response.setCoilStatus(0, true);
		response.setCoilStatus(1, false);

		assertEquals("FC1ReadCoils [device0;unitid=1;priority=HIGH;ref=10/0xa;length=2;response=10]",
				task.toLogMessage(LogVerbosity.READS_AND_WRITES_VERBOSE, request, response));
	}
}
