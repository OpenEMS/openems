package io.openems.edge.bridge.modbus.api.task;

import com.ghgande.j2mod.modbus.msg.WriteCoilRequest;
import com.ghgande.j2mod.modbus.msg.WriteCoilResponse;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.element.CoilElement;

/**
 * Implements a Write Single Coil Task, using Modbus function code 5
 * (http://www.simplymodbus.ca/FC05.htm).
 */
public class FC5WriteCoilTask extends AbstractWriteTask.Single<WriteCoilRequest, WriteCoilResponse, CoilElement> {

	public FC5WriteCoilTask(int startAddress, CoilElement element) {
		super("FC5WriteCoil", WriteCoilResponse.class, startAddress, element);
	}

	@Override
	protected WriteCoilRequest createModbusRequest() throws OpenemsException {
		var valueOpt = this.element.getNextWriteValueAndReset();
		if (valueOpt.isPresent()) {
			boolean value = valueOpt.get();
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
