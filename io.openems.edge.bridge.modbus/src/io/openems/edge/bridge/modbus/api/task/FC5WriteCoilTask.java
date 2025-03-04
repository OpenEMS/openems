package io.openems.edge.bridge.modbus.api.task;

import java.util.function.Consumer;

import com.ghgande.j2mod.modbus.msg.WriteCoilRequest;
import com.ghgande.j2mod.modbus.msg.WriteCoilResponse;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.FunctionUtils;
import io.openems.edge.bridge.modbus.api.element.CoilElement;

/**
 * Implements a Write Single Coil Task, using Modbus function code 5
 * (http://www.simplymodbus.ca/FC05.htm).
 */
public class FC5WriteCoilTask extends AbstractWriteTask.Single<WriteCoilRequest, WriteCoilResponse, CoilElement> {

	public FC5WriteCoilTask(int startAddress, CoilElement element) {
		this(FunctionUtils::doNothing, startAddress, element);
	}

	public FC5WriteCoilTask(Consumer<ExecuteState> onExecute, int startAddress, CoilElement element) {
		super("FC5WriteCoil", onExecute, WriteCoilResponse.class, startAddress, element);
	}

	@Override
	protected WriteCoilRequest createModbusRequest() throws OpenemsException {
		var value = this.element.getNextWriteValueAndReset();
		if (value != null) {
			return new WriteCoilRequest(startAddress, value);
		} else {
			return null;
		}
	}

	@Override
	protected String payloadToString(WriteCoilRequest request) {
		return request.getCoil() ? "ON" : "OFF";
	}
}
