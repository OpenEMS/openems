package io.openems.edge.bridge.modbus.api.task;

import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.procimg.InputRegister;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.common.taskmanager.Priority;

/**
 * Implements a Read Holding Register Task, implementing Modbus function code 3
 * (http://www.simplymodbus.ca/FC03.htm).
 */
public class FC3ReadRegistersTask
		extends AbstractReadInputRegistersTask<ReadMultipleRegistersRequest, ReadMultipleRegistersResponse>
		implements ReadTask {

	public FC3ReadRegistersTask(int startAddress, Priority priority, ModbusElement<?>... elements) {
		super("FC3ReadHoldingRegisters", ReadMultipleRegistersResponse.class, startAddress, priority, elements);
	}

	@Override
	protected ReadMultipleRegistersRequest createModbusRequest(int startAddress, int length) {
		return new ReadMultipleRegistersRequest(startAddress, length);
	}

	@Override
	protected InputRegister[] handleResponse(ReadMultipleRegistersResponse response) throws OpenemsException {
		return response.getRegisters();
	}
}
