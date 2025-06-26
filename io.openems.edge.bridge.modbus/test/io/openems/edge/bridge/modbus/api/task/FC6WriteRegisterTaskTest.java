package io.openems.edge.bridge.modbus.api.task;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterRequest;
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterResponse;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.DummyModbusComponent;
import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;

public class FC6WriteRegisterTaskTest {

	@Test
	public void testToLogMessage() throws OpenemsException {
		var component = new DummyModbusComponent();
		var task = new FC6WriteRegisterTask(20, new UnsignedWordElement(20));
		task.setParent(component);
		var request = new WriteSingleRegisterRequest(20, new SimpleRegister(315));
		var response = new WriteSingleRegisterResponse(20, 315);

		assertEquals("FC6WriteRegister [device0;unitid=1;ref=20/0x14;length=1;request=013b]",
				task.toLogMessage(LogVerbosity.READS_AND_WRITES_VERBOSE, request, response));
	}
}
