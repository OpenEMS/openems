package io.openems.edge.bridge.modbus.api.task;

import com.ghgande.j2mod.modbus.msg.ReadCoilsRequest;
import com.ghgande.j2mod.modbus.msg.ReadCoilsResponse;
import com.ghgande.j2mod.modbus.util.BitVector;

import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.common.taskmanager.Priority;

/**
 * Implements a Read Coils Task, implementing Modbus function code 1
 * (http://www.simplymodbus.ca/FC01.htm).
 */
public class FC1ReadCoilsTask extends AbstractReadDigitalInputsTask<ReadCoilsRequest, ReadCoilsResponse>
		implements ReadTask {

	public FC1ReadCoilsTask(int startAddress, Priority priority, ModbusElement<?>... elements) {
		super("FC1ReadCoils", ReadCoilsResponse.class, startAddress, priority, elements);
	}

	@Override
	protected BitVector convertToBitVector(ReadCoilsResponse response) {
		return response.getCoils();
	}

	@Override
	protected ReadCoilsRequest createModbusRequest(int startAddress, int length) {
		return new ReadCoilsRequest(startAddress, length);
	}
}
