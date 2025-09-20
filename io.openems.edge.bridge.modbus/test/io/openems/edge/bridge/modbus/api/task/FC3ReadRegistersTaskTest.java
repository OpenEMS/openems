package io.openems.edge.bridge.modbus.api.task;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.concurrent.atomic.AtomicReference;

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
	public void test() throws OpenemsException {
		var component = new DummyModbusComponent();
		var value = new AtomicReference<Long>();
		var element20 = new UnsignedDoublewordElement(20);
		element20.onUpdateCallback(v -> value.set(v));
		var element22 = new UnsignedDoublewordElement(22);
		element22.onUpdateCallback(v -> value.set(v));

		var task = new FC3ReadRegistersTask(20, Priority.LOW, element20, element22);
		task.setParent(component);
		var request = task.createModbusRequest();
		var response = (ReadMultipleRegistersResponse) request.getResponse();
		response.setRegisters(new Register[] { //
				new SimpleRegister(100), new SimpleRegister(200), //
				new SimpleRegister(300), new SimpleRegister(400) });

		assertEquals(
				"FC3ReadHoldingRegisters [device0;unitid=1;priority=LOW;ref=20/0x14;length=4;response=0064 00c8 012c 0190]",
				task.toLogMessage(LogVerbosity.READS_AND_WRITES_VERBOSE, request, response));

		var registers = task.parseResponse(response);
		assertNull(value.get());
		task.handleResponse(element20, 0, registers);
		assertEquals(Long.valueOf(6553800), value.get());
		task.handleResponse(element22, 2, registers);
		assertEquals(Long.valueOf(19661200), value.get());
	}
}
