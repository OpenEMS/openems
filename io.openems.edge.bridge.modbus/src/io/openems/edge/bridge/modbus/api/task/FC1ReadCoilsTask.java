package io.openems.edge.bridge.modbus.api.task;

import com.ghgande.j2mod.modbus.msg.ReadCoilsRequest;
import com.ghgande.j2mod.modbus.msg.ReadCoilsResponse;
import com.ghgande.j2mod.modbus.util.BitVector;

import io.openems.edge.bridge.modbus.api.element.ModbusCoilElement;
import io.openems.edge.common.taskmanager.Priority;

/**
 * Implements a Read Coils Task, implementing Modbus function code 1
 * (http://www.simplymodbus.ca/FC01.htm).
 */
public class FC1ReadCoilsTask extends AbstractReadDigitalInputsTask<ReadCoilsRequest, ReadCoilsResponse> {

	public FC1ReadCoilsTask(int startAddress, Priority priority, ModbusCoilElement... elements) {
		super("FC1ReadCoils", ReadCoilsResponse.class, startAddress, priority, elements);
	}

	@Override
	protected ReadCoilsRequest createModbusRequest(int startAddress, int length) {
		return new ReadCoilsRequest(startAddress, length);
	}

	@Override
	protected BitVector parseBitResponse(ReadCoilsResponse response) {
		return response.getCoils();
	}
}
