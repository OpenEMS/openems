package io.openems.edge.bridge.modbus.api.task;

import java.util.Arrays;

import com.ghgande.j2mod.modbus.procimg.InputRegister;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.element.ModbusRegisterElement;
import io.openems.edge.common.taskmanager.Priority;

public abstract class AbstractReadInputRegistersTask extends AbstractReadTask<InputRegister> {

	public AbstractReadInputRegistersTask(int startAddress, Priority priority, AbstractModbusElement<?>... elements) {
		super(startAddress, priority, elements);
	}

	@Override
	protected boolean isCorrectElementInstance(ModbusElement<?> modbusElement) {
		return modbusElement instanceof ModbusRegisterElement;
	}

	@Override
	protected void doElementSetInput(ModbusElement<?> modbusElement, int position, InputRegister[] response)
			throws OpenemsException {
		((ModbusRegisterElement<?>) modbusElement)
				.setInputRegisters(Arrays.copyOfRange(response, position, position + modbusElement.getLength()));
	}

	@Override
	protected String getRequiredElementName() {
		return "ModbusRegisterElement";
	}

	@Override
	protected int increasePosition(int position, ModbusElement<?> modbusElement) {
		return position + modbusElement.getLength();
	}
}