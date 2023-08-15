package io.openems.edge.bridge.modbus.api.task;

import java.util.stream.Stream;

import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.element.ModbusRegisterElement;
import io.openems.edge.common.taskmanager.Priority;

@SuppressWarnings("rawtypes")
public abstract class AbstractReadRegistersTask<//
		REQUEST extends ModbusRequest, //
		RESPONSE extends ModbusResponse, //
		T extends InputRegister> //
		extends AbstractReadTask<REQUEST, RESPONSE, ModbusRegisterElement, T> {

	public AbstractReadRegistersTask(String name, Class<RESPONSE> responseClazz, int startAddress, Priority priority,
			ModbusElement... elements) {
		super(name, responseClazz, ModbusRegisterElement.class, startAddress, priority, elements);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected final void handleResponse(ModbusRegisterElement element, int position, T[] response)
			throws OpenemsException {
		try {
			// Extract parts of response and cast to Register, because ModbusRegisterElement
			// requires 'Register' type
			var registers = Stream.of(response) //
					.skip(position) //
					.limit(element.length) //
					.map(Register.class::cast) //
					.toArray(Register[]::new);
			element.setInputValue(registers);

		} catch (ClassCastException e) {
			throw new OpenemsException("Response must be of type Register for Element [" + element.toString() + "]");
		}
	}

	@Override
	protected final int calculateNextPosition(ModbusElement modbusElement, int position) {
		return position + modbusElement.length;
	}
}