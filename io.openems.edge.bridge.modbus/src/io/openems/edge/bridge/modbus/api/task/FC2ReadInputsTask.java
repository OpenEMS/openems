package io.openems.edge.bridge.modbus.api.task;

import com.ghgande.j2mod.modbus.msg.ReadInputDiscretesRequest;
import com.ghgande.j2mod.modbus.msg.ReadInputDiscretesResponse;
import com.ghgande.j2mod.modbus.util.BitVector;

import io.openems.edge.bridge.modbus.api.element.ModbusCoilElement;
import io.openems.edge.common.taskmanager.Priority;

/**
 * Implements a Read Inputs Task, implementing Modbus function code 2
 * (http://www.simplymodbus.ca/FC02.htm).
 */
public class FC2ReadInputsTask
		extends AbstractReadDigitalInputsTask<ReadInputDiscretesRequest, ReadInputDiscretesResponse> {

	public FC2ReadInputsTask(int startAddress, Priority priority, ModbusCoilElement... elements) {
		super("FC2ReadCoils", ReadInputDiscretesResponse.class, startAddress, priority, elements);
	}

	@Override
	protected ReadInputDiscretesRequest createModbusRequest(int startAddress, int length) {
		return new ReadInputDiscretesRequest(startAddress, length);
	}

	@Override
	protected BitVector parseBitResponse(ReadInputDiscretesResponse response) {
		return response.getDiscretes();
	}
}
