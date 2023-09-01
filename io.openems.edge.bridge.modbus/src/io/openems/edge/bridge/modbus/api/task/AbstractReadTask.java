package io.openems.edge.bridge.modbus.api.task;

import java.util.ArrayList;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.common.taskmanager.Priority;

/**
 * An abstract Modbus 'AbstractTask' is holding references to one or more Modbus
 * {@link ModbusElement}s which have register addresses in the same range.
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractReadTask<//
		REQUEST extends ModbusRequest, //
		RESPONSE extends ModbusResponse, //
		ELEMENT extends AbstractModbusElement, //
		T> //
		extends AbstractTask<REQUEST, RESPONSE> implements ReadTask {

	private final Logger log = LoggerFactory.getLogger(AbstractReadTask.class);
	private final Priority priority;
	private final Class<?> elementClazz;

	public AbstractReadTask(String name, Class<RESPONSE> responseClazz, Class<ELEMENT> elementClazz, int startAddress,
			Priority priority, ModbusElement... elements) {
		super(name, responseClazz, startAddress, elements);
		this.elementClazz = elementClazz;
		this.priority = priority;
	}

	@Override
	public ExecuteState execute(AbstractModbusBridge bridge) {
		try {
			var response = this.executeRequest(bridge, this.createModbusRequest());
			// On error a log message has already been logged

			try {
				var result = this.parseResponse(response);
				validateResponse(result, this.length);
				this.fillElements(result);

			} catch (OpenemsException e1) {
				logError(this.log, e1, "Parsing Response failed.");
				throw e1;
			}
			return ExecuteState.OK;

		} catch (Exception e) {
			// Invalidate Elements
			Stream.of(this.elements).forEach(el -> el.invalidate(bridge));

			return ExecuteState.ERROR;
		}
	}

	/**
	 * Verify length of response array.
	 * 
	 * @param response response array
	 * @param length   expected length
	 * @throws OpenemsException on failed validation
	 */
	private static void validateResponse(Object[] response, int length) throws OpenemsException {
		if (response.length < length) {
			throw new OpenemsException("Received message is too short. " //
					+ "Expected [" + length + "] " //
					+ "Got [" + response.length + "]");
		}
	}

	/**
	 * Fills {@link ModbusElement}s with values from response.
	 * 
	 * @param response the response values
	 * @throws OpenemsException on error
	 */
	@SuppressWarnings("unchecked")
	private void fillElements(T[] response) throws OpenemsException {
		var position = 0;
		var errors = new ArrayList<String>();

		for (var element : this.elements) {
			if (this.elementClazz.isInstance(element)) {
				try {
					this.handleResponse((ELEMENT) element, position, response);
				} catch (OpenemsException e) {
					errors.add("Unable to fill Modbus Element. " //
							+ element.toString() + " Error: " + e.getMessage());
				}
			} else {
				errors.add("Wrong type while filling Modbus Element. " //
						+ element.toString() + " " //
						+ "Expected [" + this.elementClazz.getSimpleName() + "] " //
						+ "Got [" + element.getClass().getSimpleName() + "]");
			}
			position = this.calculateNextPosition(element, position);
		}

		if (!errors.isEmpty()) {
			throw new OpenemsException(String.join(", ", errors));
		}
	}

	@Override
	public Priority getPriority() {
		return this.priority;
	}

	/**
	 * Handle a Response, e.g. set the internal value.
	 * 
	 * @param element  the {@link ModbusElement}
	 * @param position the current position
	 * @param response the converted {@link ModbusResponse} values
	 * @throws OpenemsException on error
	 */
	protected abstract void handleResponse(ELEMENT element, int position, T[] response) throws OpenemsException;

	/**
	 * Calculate the position of the next Element.
	 * 
	 * @param position      current position
	 * @param modbusElement current Element
	 * @return next position
	 */
	protected abstract int calculateNextPosition(ModbusElement modbusElement, int position);

	/**
	 * Factory for a {@link ModbusRequest}.
	 * 
	 * @return a new {@link ModbusRequest}
	 */
	protected abstract REQUEST createModbusRequest();

	/**
	 * Parses a {@link ModbusResponse} to an array of values.
	 * 
	 * @param response the {@link ModbusResponse}
	 * @return array of results
	 * @throws OpenemsException on error
	 */
	protected abstract T[] parseResponse(RESPONSE response) throws OpenemsException;

	@Override
	protected final String payloadToString(REQUEST request) {
		return "";
	}
}
