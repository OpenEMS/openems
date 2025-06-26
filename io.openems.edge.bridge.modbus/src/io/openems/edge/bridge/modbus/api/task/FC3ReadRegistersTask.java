package io.openems.edge.bridge.modbus.api.task;

import java.util.function.Consumer;

import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.procimg.Register;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.FunctionUtils;
import io.openems.edge.bridge.modbus.api.ModbusUtils;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.common.taskmanager.Priority;

/**
 * Implements a Read Holding Register Task, implementing Modbus function code 3
 * (http://www.simplymodbus.ca/FC03.htm).
 */
public class FC3ReadRegistersTask
		extends AbstractReadRegistersTask<ReadMultipleRegistersRequest, ReadMultipleRegistersResponse> {

	public FC3ReadRegistersTask(int startAddress, Priority priority, ModbusElement... elements) {
		this(FunctionUtils::doNothing, startAddress, priority, elements);
	}

	public FC3ReadRegistersTask(Consumer<ExecuteState> onExecute, int startAddress, Priority priority,
			ModbusElement... elements) {
		super("FC3ReadHoldingRegisters", onExecute, ReadMultipleRegistersResponse.class, startAddress, priority,
				elements);
	}

	@Override
	protected ReadMultipleRegistersRequest createModbusRequest() {
		return new ReadMultipleRegistersRequest(this.startAddress, this.length);
	}

	@Override
	protected Register[] parseResponse(ReadMultipleRegistersResponse response) throws OpenemsException {
		return response.getRegisters();
	}

	@Override
	protected String payloadToString(ReadMultipleRegistersResponse response) {
		return ModbusUtils.registersToHexString(response.getRegisters());
	}
}
