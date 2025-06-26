package io.openems.edge.bridge.modbus.api.task;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ghgande.j2mod.modbus.msg.WriteCoilRequest;
import com.ghgande.j2mod.modbus.msg.WriteCoilResponse;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.DummyModbusComponent;
import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.bridge.modbus.api.element.CoilElement;

public class FC5WriteCoilTaskTest {

	@Test
	public void testToLogMessage() throws OpenemsException {
		var component = new DummyModbusComponent();
		var task = new FC5WriteCoilTask(20, new CoilElement(20));
		task.setParent(component);
		var request = new WriteCoilRequest(20, true);
		var response = (WriteCoilResponse) request.getResponse();
		response.setCoil(true);

		assertEquals("FC5WriteCoil [device0;unitid=1;ref=20/0x14;length=1;request=ON]",
				task.toLogMessage(LogVerbosity.READS_AND_WRITES_VERBOSE, request, response));
	}
}
