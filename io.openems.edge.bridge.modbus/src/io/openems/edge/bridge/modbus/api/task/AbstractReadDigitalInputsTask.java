package io.openems.edge.bridge.modbus.api.task;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.util.BitVector;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.common.taskmanager.Priority;

public abstract class AbstractReadDigitalInputsTask<//
		REQUEST extends ModbusRequest, //
		RESPONSE extends ModbusResponse> //
		extends AbstractReadTask<REQUEST, RESPONSE, CoilElement, Boolean> {

	public AbstractReadDigitalInputsTask(String name, Class<RESPONSE> responseClazz, int startAddress,
			Priority priority, CoilElement... elements) {
		super(name, responseClazz, CoilElement.class, startAddress, priority, elements);
	}

	@Override
	protected void handleResponse(CoilElement element, int position, Boolean[] response) throws OpenemsException {
		element.setInputValue(response[position]);
	}

	@Override
	protected int calculateNextPosition(ModbusElement modbusElement, int position) {
		return position + 1;
	}

	@Override
	protected final Boolean[] parseResponse(RESPONSE response) {
		return toBooleanArray(this.parseBitResponse(response));
	}

	protected abstract BitVector parseBitResponse(RESPONSE response);

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

	@Override
	protected final String payloadToString(RESPONSE response) {
		return Stream.of(this.parseResponse(response)) //
				.map(b -> b == null ? "-" : (b ? "1" : "0")) //
				.collect(Collectors.joining());
	}
}