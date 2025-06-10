package io.openems.edge.bridge.modbus.api.task;

import java.util.function.Consumer;

import com.ghgande.j2mod.modbus.msg.ReadCoilsRequest;
import com.ghgande.j2mod.modbus.msg.ReadCoilsResponse;
import com.ghgande.j2mod.modbus.util.BitVector;

import io.openems.common.utils.FunctionUtils;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.common.taskmanager.Priority;

/**
 * Implements a Read Coils Task, implementing Modbus function code 1
 * (http://www.simplymodbus.ca/FC01.htm).
 */
public class FC1ReadCoilsTask extends AbstractReadDigitalInputsTask<ReadCoilsRequest, ReadCoilsResponse> {

	public FC1ReadCoilsTask(int startAddress, Priority priority, CoilElement... elements) {
		this(FunctionUtils::doNothing, startAddress, priority, elements);
	}

	public FC1ReadCoilsTask(Consumer<ExecuteState> onExecute, int startAddress, Priority priority,
			CoilElement... elements) {
		super("FC1ReadCoils", onExecute, ReadCoilsResponse.class, startAddress, priority, elements);
	}

	@Override
	protected ReadCoilsRequest createModbusRequest() {
		return new ReadCoilsRequest(this.startAddress, this.length);
	}

	@Override
	protected BitVector parseBitResponse(ReadCoilsResponse response) {
		return response.getCoils();
	}
}
