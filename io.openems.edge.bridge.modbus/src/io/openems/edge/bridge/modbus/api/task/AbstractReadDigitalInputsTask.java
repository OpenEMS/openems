package io.openems.edge.bridge.modbus.api.task;

import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.util.BitVector;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.element.ModbusCoilElement;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.common.taskmanager.Priority;

public abstract class AbstractReadDigitalInputsTask<//
		REQUEST extends ModbusRequest, //
		RESPONSE extends ModbusResponse> //
		extends AbstractReadTask<REQUEST, RESPONSE, ModbusCoilElement, Boolean> {

	public AbstractReadDigitalInputsTask(String name, Class<RESPONSE> responseClazz, int startAddress,
			Priority priority, ModbusCoilElement... elements) {
		super(name, responseClazz, ModbusCoilElement.class, startAddress, priority, elements);
	}

	@Override
	protected void handleResponse(ModbusCoilElement element, int position, Boolean[] response) throws OpenemsException {
		element.setInputCoil(response[position]);
	}

	@Override
	protected int calculateNextPosition(ModbusElement<?> modbusElement, int position) {
		return position + 1;
	}

	@Override
	protected final Boolean[] parseResponse(RESPONSE response) throws OpenemsException {
		return toBooleanArray(this.parseBitResponse(response));
	}

	protected abstract BitVector parseBitResponse(RESPONSE response) throws OpenemsException;

	/**
	 * Convert a {@link BitVector} to a {@link Boolean} array.
	 * 
	 * @param v the {@link BitVector}
	 * @return the {@link Boolean} array
	 */
	protected static Boolean[] toBooleanArray(BitVector v) {
		var bools = new Boolean[v.size()];
		for (var i = 0; i < v.size(); i++) {
			bools[i] = v.getBit(i);
		}
		return bools;
	}
}