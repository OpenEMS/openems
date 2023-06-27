package io.openems.edge.bridge.modbus.api.task;

import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.procimg.InputRegister;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.common.taskmanager.Priority;

/**
 * Implements a Read Holding Register Task, implementing Modbus function code 3
 * (http://www.simplymodbus.ca/FC03.htm).
 */
public class FC3ReadRegistersTask extends AbstractReadInputRegistersTask implements ReadTask {

	public FC3ReadRegistersTask(int startAddress, Priority priority, AbstractModbusElement<?>... elements) {
		super(startAddress, priority, elements);
	}

	@Override
	protected ModbusRequest createModbusRequest(int startAddress, int length) {
		return new ReadMultipleRegistersRequest(startAddress, length);
	}

	@Override
	protected InputRegister[] handleResponse(ModbusResponse response) throws OpenemsException {
		if (response instanceof ReadMultipleRegistersResponse) {
			var registersResponse = (ReadMultipleRegistersResponse) response;
			return registersResponse.getRegisters();
		}
		throw new OpenemsException("Unexpected Modbus response. Expected [ReadMultipleRegistersResponse], got ["
				+ response.getClass().getSimpleName() + "]");
	}

	@Override
	protected String getActiondescription() {
		return "FC3ReadHoldingRegisters";
	}
}
