package io.openems.edge.bridge.modbus.api.task;

import java.util.Arrays;
import java.util.function.Consumer;

import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.procimg.Register;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.element.ModbusRegisterElement;
import io.openems.edge.common.taskmanager.Priority;

@SuppressWarnings("rawtypes")
public abstract class AbstractReadRegistersTask<//
		REQUEST extends ModbusRequest, //
		RESPONSE extends ModbusResponse> //
		extends AbstractReadTask<REQUEST, RESPONSE, ModbusRegisterElement, Register> {

	public AbstractReadRegistersTask(String name, Consumer<ExecuteState> onExecute, Class<RESPONSE> responseClazz,
			int startAddress, Priority priority, ModbusElement... elements) {
		super(name, onExecute, responseClazz, ModbusRegisterElement.class, startAddress, priority, elements);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected final void handleResponse(ModbusRegisterElement element, int position, Register[] response)
			throws OpenemsException {
		element.setInputValue(Arrays.copyOfRange(response, position, position + element.length));
	}

	@Override
	protected final int calculateNextPosition(ModbusElement modbusElement, int position) {
		return position + modbusElement.length;
	}
}