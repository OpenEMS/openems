package io.openems.edge.bridge.modbus.api;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CompletableFuture;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.common.taskmanager.Priority;

public class ModbusUtils {

	/**
	 * Reads given Element once from Modbus.
	 *
	 * @param <T>             the Type of the element
	 * @param modbusProtocol  the {@link ModbusProtocol}, that is linked with a
	 *                        {@link BridgeModbus}
	 * @param element         the {@link AbstractModbusElement}
	 * @param tryAgainOnError if true, tries to read till it receives a value; if
	 *                        false, stops after first try and possibly return null
	 * @return a future value, e.g. a Integer or null (if tryAgainOnError is false)
	 * @throws OpenemsException on error with the {@link ModbusProtocol} object
	 */
	public static <T> CompletableFuture<T> readELementOnce(ModbusProtocol modbusProtocol,
			AbstractModbusElement<T> element, boolean tryAgainOnError) throws OpenemsException {
		// Prepare result
		final var result = new CompletableFuture<T>();

		// Activate task
		final Task task = new FC3ReadRegistersTask(element.getStartAddress(), Priority.HIGH, element);
		modbusProtocol.addTask(task);

		// Register listener for element
		element.onUpdateCallback(value -> {
			if (value == null) {
				if (tryAgainOnError) {
					return;
				}
				result.complete(null);
			}
			// do not try again
			modbusProtocol.removeTask(task);
			result.complete(value);
		});

		return result;
	}

	/**
	 * Converts upper/lower bytes to Short.
	 *
	 * @param value      the int value
	 * @param upperBytes 1 = upper two bytes, 0 = lower two bytes
	 * @return the Short
	 */
	public static Short convert(int value, int upperBytes) {
		var b = ByteBuffer.allocate(4);
		b.order(ByteOrder.LITTLE_ENDIAN);
		b.putInt(value);

		var byte0 = b.get(upperBytes * 2);
		var byte1 = b.get(upperBytes * 2 + 1);

		var shortBuf = ByteBuffer.allocate(2);
		shortBuf.order(ByteOrder.LITTLE_ENDIAN);
		shortBuf.put(0, byte0);
		shortBuf.put(1, byte1);

		return shortBuf.getShort();
	}
}
