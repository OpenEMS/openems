package io.openems.edge.bridge.modbus.api.task;

import java.util.function.Consumer;
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
 * {@link AbstractModbusElement} which have register addresses in the same
 * range.
 */
public abstract class AbstractReadTask<//
		REQUEST extends ModbusRequest, //
		RESPONSE extends ModbusResponse, //
		ELEMENT extends ModbusElement<?>, //
		T> //
		extends AbstractTask<REQUEST, RESPONSE> implements ReadTask {

	private final Logger log = LoggerFactory.getLogger(AbstractReadTask.class);
	private final Priority priority;
	private final Class<?> elementClazz;

	public AbstractReadTask(String name, Class<RESPONSE> responseClazz, Class<ELEMENT> elementClazz, int startAddress,
			Priority priority, ModbusElement<?>... elements) {
		super(name, responseClazz, startAddress, elements);
		this.elementClazz = elementClazz;
		this.priority = priority;
	}

	@Override
	public ExecuteState execute(AbstractModbusBridge bridge) {
		try {
			var response = this.parseResponse(//
					this.executeRequest(bridge, //
							createModbusRequest(this.startAddress, this.length)));
			validateResponse(response, this.length);
			this.fillElements(response, (message) -> this.logError(bridge, this.log, message));
			return ExecuteState.OK;

		} catch (OpenemsException e) {
			// Invalidate Elements
			Stream.of(this.elements).forEach(el -> el.invalidate(bridge));

			this.logError(bridge, this.log, "Execute failed: " + e.getMessage());
			return ExecuteState.ERROR;
		}

//		// debug output
//		switch (this.getLogVerbosity(bridge)) {
//		case READS_AND_WRITES:
//			this.logInfo(bridge, //
//					"[" + unitId + ":" + this.startAddress + "/0x" + Integer.toHexString(this.startAddress) + "]: " //
//							+ Arrays.stream(result).map(r -> {
//								if (r instanceof InputRegister) {
//									return String.format("%4s", Integer.toHexString(((InputRegister) r).getValue()))
//											.replace(' ', '0');
//								}
//								if (r instanceof Boolean) {
//									return (Boolean) r ? "x" : "-";
//								} else {
//									return r.toString();
//								}
//							}) //
//									.collect(Collectors.joining(" ")));
//			break;
//		case WRITES:
//		case DEV_REFACTORING:
//		case NONE:
//			break;
//		}
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
	 * @param logError callback to log a error message
	 */
	@SuppressWarnings("unchecked")
	private void fillElements(T[] response, Consumer<String> logError) {
		var position = 0;

		for (var element : this.elements) {
			if (this.elementClazz.isInstance(element)) {
				try {
					this.doElementSetInput((ELEMENT) element, position, response);
				} catch (OpenemsException e) {
					logError.accept("Unable to fill Modbus Element. " //
							+ ModbusElement.toString(element) //
							+ ": " + e.getMessage());
				}
			} else {
				logError.accept("Wrong type while filling Modbus Element. " //
						+ ModbusElement.toString(element) + " " //
						+ "Expected [" + elementClazz.getSimpleName() + "] " //
						+ "Got [" + element.getClass().getSimpleName() + "]");
			}
			position = this.increasePosition(position, element);
		}
	}

	@Override
	public Priority getPriority() {
		return this.priority;
	}

	protected abstract int increasePosition(int position, ModbusElement<?> modbusElement);

	protected abstract void doElementSetInput(ELEMENT element, int position, T[] response) throws OpenemsException;

	/**
	 * Factory for a {@link ModbusRequest}.
	 * 
	 * @param startAddress the startAddress of the modbus register
	 * @param length       the length
	 * @return a new {@link ModbusRequest}
	 */
	protected abstract REQUEST createModbusRequest(int startAddress, int length);

	/**
	 * Parses a {@link ModbusResponse} to an array of values.
	 * 
	 * @param response the {@link ModbusResponse}
	 * @return array of results
	 * @throws OpenemsException on error
	 */
	protected abstract T[] parseResponse(RESPONSE response) throws OpenemsException;

}
