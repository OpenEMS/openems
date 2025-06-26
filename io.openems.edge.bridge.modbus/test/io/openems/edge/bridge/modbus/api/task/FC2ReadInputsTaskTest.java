package io.openems.edge.bridge.modbus.api.task;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import org.junit.Test;

import com.ghgande.j2mod.modbus.msg.ReadInputDiscretesResponse;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.DummyModbusComponent;
import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.common.taskmanager.Priority;

public class FC2ReadInputsTaskTest {

	@Test
	public void test() throws OpenemsException {
		var component = new DummyModbusComponent();
		var value = new AtomicReference<Boolean>();
		var element10 = new CoilElement(10);
		element10.onUpdateCallback(v -> value.set(v));
		var element11 = new CoilElement(11);
		element11.onUpdateCallback(v -> value.set(v));
		var task = new FC2ReadInputsTask(10, Priority.HIGH, element10, element11);
		task.setParent(component);
		var request = task.createModbusRequest();
		var response = (ReadInputDiscretesResponse) request.getResponse();
		response.setDiscreteStatus(0, true);
		response.setDiscreteStatus(1, false);

		assertEquals("FC2ReadCoils [device0;unitid=1;priority=HIGH;ref=10/0xa;length=2;response=10]",
				task.toLogMessage(LogVerbosity.READS_AND_WRITES_VERBOSE, request, response));

		var discretes = response.getDiscretes();
		var values = IntStream.range(0, discretes.size()) //
				.mapToObj(i -> Boolean.valueOf(discretes.getBit(i))) //
				.toArray(Boolean[]::new);
		assertNull(value.get());
		task.handleResponse(element10, 0, values);
		assertEquals(Boolean.valueOf(true), value.get());
		task.handleResponse(element11, 1, values);
		assertEquals(Boolean.valueOf(false), value.get());
	}
}
