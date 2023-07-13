package io.openems.edge.bridge.modbus.api.task;

import com.ghgande.j2mod.modbus.msg.ReadInputRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersResponse;
import com.ghgande.j2mod.modbus.procimg.InputRegister;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.ModbusUtils;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.common.taskmanager.Priority;

/**
 * Implements a Read Input Register Task, implementing Modbus function code 4
 * (http://www.simplymodbus.ca/FC04.htm).
 */
public class FC4ReadInputRegistersTask
		extends AbstractReadInputRegistersTask<ReadInputRegistersRequest, ReadInputRegistersResponse> {

	public FC4ReadInputRegistersTask(int startAddress, Priority priority, ModbusElement<?>... elements) {
		super("FC4ReadInputRegisters", ReadInputRegistersResponse.class, startAddress, priority, elements);
	}

	@Override
	protected ReadInputRegistersRequest createModbusRequest() {
		return new ReadInputRegistersRequest(this.startAddress, this.length);
	}

	@Override
	protected InputRegister[] parseResponse(ReadInputRegistersResponse response) throws OpenemsException {
		return response.getRegisters();
	}

	@Override
	protected String payloadToString(ReadInputRegistersResponse response) {
		return ModbusUtils.registersToHexString(response.getRegisters());
	}
}