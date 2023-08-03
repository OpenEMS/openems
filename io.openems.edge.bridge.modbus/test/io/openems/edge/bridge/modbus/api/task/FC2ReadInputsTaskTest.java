package io.openems.edge.bridge.modbus.api.task;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ghgande.j2mod.modbus.msg.ReadInputDiscretesResponse;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.DummyModbusComponent;
import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.common.taskmanager.Priority;

public class FC2ReadInputsTaskTest {

	@Test
	public void testToLogMessage() throws OpenemsException {
		var component = new DummyModbusComponent();
		var task = new FC2ReadInputsTask(10, Priority.HIGH, new CoilElement(10), new CoilElement(11));
		task.setParent(component);
		var request = task.createModbusRequest();
		var response = (ReadInputDiscretesResponse) request.getResponse();
		response.setDiscreteStatus(0, true);
		response.setDiscreteStatus(1, false);

		assertEquals("FC2ReadCoils [device0;unitid=1;priority=HIGH;ref=10/0xa;length=2;response=10]",
				task.toLogMessage(LogVerbosity.READS_AND_WRITES_VERBOSE, request, response));
	}
}
