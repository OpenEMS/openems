package io.openems.edge.bridge.modbus.api.task;

import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersResponse;
import com.ghgande.j2mod.modbus.procimg.InputRegister;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.common.taskmanager.Priority;

/**
 * Implements a Read Input Register Task, implementing Modbus function code 4
 * (http://www.simplymodbus.ca/FC04.htm).
 */
public class FC4ReadInputRegistersTask extends AbstractReadInputRegistersTask implements ReadTask {

	public FC4ReadInputRegistersTask(int startAddress, Priority priority, AbstractModbusElement<?>... elements) {
		super(startAddress, priority, elements);
	}

	@Override
	protected String getActiondescription() {
		return "FC4ReadInputRegisters";
	}

	@Override
	protected ModbusRequest getRequest() {
		return new ReadInputRegistersRequest(this.getStartAddress(), this.getLength());
	}

	@Override
	protected InputRegister[] handleResponse(ModbusResponse response) throws OpenemsException {
		if (response instanceof ReadInputRegistersResponse) {
			var registersResponse = (ReadInputRegistersResponse) response;
			return registersResponse.getRegisters();
		}
		throw new OpenemsException("Unexpected Modbus response. Expected [ReadInputRegistersResponse], got ["
				+ response.getClass().getSimpleName() + "]");
	}
}