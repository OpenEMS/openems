package io.openems.edge.bridge.modbus.api.task;

import com.ghgande.j2mod.modbus.msg.ReadInputDiscretesRequest;
import com.ghgande.j2mod.modbus.msg.ReadInputDiscretesResponse;
import com.ghgande.j2mod.modbus.util.BitVector;

import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.common.taskmanager.Priority;

/**
 * Implements a Read Inputs Task, implementing Modbus function code 2
 * (http://www.simplymodbus.ca/FC02.htm).
 */
public class FC2ReadInputsTask extends
		AbstractReadDigitalInputsTask<ReadInputDiscretesRequest, ReadInputDiscretesResponse> implements ReadTask {

	public FC2ReadInputsTask(int startAddress, Priority priority, ModbusElement<?>... elements) {
		super("FC2ReadCoils", ReadInputDiscretesResponse.class, startAddress, priority, elements);
	}

	@Override
	protected BitVector convertToBitVector(ReadInputDiscretesResponse response) {
		return response.getDiscretes();
	}

	@Override
	protected ReadInputDiscretesRequest createModbusRequest(int startAddress, int length) {
		return new ReadInputDiscretesRequest(startAddress, length);
	}
}
