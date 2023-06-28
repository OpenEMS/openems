package io.openems.edge.bridge.modbus.api.task;

import com.ghgande.j2mod.modbus.msg.WriteCoilRequest;
import com.ghgande.j2mod.modbus.msg.WriteCoilResponse;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.element.ModbusCoilElement;

/**
 * Implements a Write Single Coil Task, using Modbus function code 5
 * (http://www.simplymodbus.ca/FC05.htm).
 */
public class FC5WriteCoilTask extends AbstractWriteTask.Single<WriteCoilRequest, WriteCoilResponse, ModbusCoilElement> {

	public FC5WriteCoilTask(int startAddress, ModbusCoilElement element) {
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

//	private void writeCoil(AbstractModbusBridge bridge, int unitId, int startAddress, boolean value)
//			throws OpenemsException, ModbusException {
//		// debug output
//		switch (this.getLogVerbosity(bridge)) {
//		case READS_AND_WRITES:
//			bridge.logInfo(this.log, this.name //
//					+ " [" + unitId + ":" + startAddress + "/0x" + Integer.toHexString(startAddress) + "]: " //
//					+ value);
//			break;
//		case WRITES:
//		case DEV_REFACTORING:
//		case NONE:
//			break;
//		}
//
//		var request = new WriteCoilRequest(startAddress, value);
//		Utils.getResponse(WriteCoilResponse.class, request, unitId, bridge); // ignore actual result
//	}
}
