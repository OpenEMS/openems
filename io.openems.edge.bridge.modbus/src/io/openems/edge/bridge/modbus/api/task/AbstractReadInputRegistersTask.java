package io.openems.edge.bridge.modbus.api.task;

import java.util.Arrays;

import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.procimg.InputRegister;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.element.ModbusRegisterElement;
import io.openems.edge.common.taskmanager.Priority;

public abstract class AbstractReadInputRegistersTask<REQUEST extends ModbusRequest, RESPONSE extends ModbusResponse>
		extends AbstractReadTask<REQUEST, RESPONSE, InputRegister> {

	public AbstractReadInputRegistersTask(String name, Class<RESPONSE> responseClazz, int startAddress,
			Priority priority, AbstractModbusElement<?, ?>... elements) {
		super(name, responseClazz, ModbusRegisterElement.class, startAddress, priority, elements);
	}

	@Override
	protected void doElementSetInput(ModbusElement<?> modbusElement, int position, InputRegister[] response)
			throws OpenemsException {
		((ModbusRegisterElement<?>) modbusElement)
				.setInputRegisters(Arrays.copyOfRange(response, position, position + modbusElement.getLength()));
	}

	@Override
	protected int increasePosition(int position, ModbusElement<?> modbusElement) {
		return position + modbusElement.getLength();
	}
}