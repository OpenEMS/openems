package io.openems.edge.bridge.modbus.api;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.element.ModbusRegisterElement;
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
			ModbusRegisterElement<?, T> element, boolean tryAgainOnError) throws OpenemsException {
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
	 * Reads given Elements once from Modbus.
	 *
	 * @param <T>             the Type of the elements
	 * @param modbusProtocol  the {@link ModbusProtocol}, that is linked with a
	 *                        {@link BridgeModbus}
	 * @param elements        the {@link AbstractModbusElement}s
	 * @param tryAgainOnError if true, tries to read till it receives a value on
	 *                        first register; if false, stops after first try and
	 *                        possibly return null
	 * @return a future array of values, e.g. Integer[] or null (if tryAgainOnError
	 *         is false). If an array is returned, it is guaranteed to have the same
	 *         length as `elements`
	 * @throws OpenemsException on error with the {@link ModbusProtocol} object
	 */
	public static <T> CompletableFuture<List<T>> readELementsOnce(ModbusProtocol modbusProtocol,
			ModbusRegisterElement<?, T>[] elements, boolean tryAgainOnError) throws OpenemsException {
		if (elements.length == 0) {
			return CompletableFuture.completedFuture(Collections.emptyList());
		}

		// Prepare result
		final var result = new CompletableFuture<List<T>>();

		// Activate task
		final Task task = new FC3ReadRegistersTask(elements[0].getStartAddress(), Priority.HIGH, elements);
		modbusProtocol.addTask(task);

		// Register listener for each element
		final var subResults = new ArrayList<CompletableFuture<T>>();
		{
			var subResult = new CompletableFuture<T>();
			subResults.add(subResult);
			elements[0].onUpdateCallback(value -> {
				if (value == null) {
					if (tryAgainOnError) {
						// try again
						return;
					} else {
						result.complete(null);
					}
				}

				// do not try again
				modbusProtocol.removeTask(task);
				subResult.complete(value);
			});
		}

		for (var i = 1; i < elements.length; i++) {
			var subResult = new CompletableFuture<T>();
			subResults.add(subResult);
			elements[i].onUpdateCallback(value -> {
				modbusProtocol.removeTask(task);
				subResult.complete(value);
			});
		}

		CompletableFuture //
				.allOf(subResults.toArray(new CompletableFuture[subResults.size()])) //
				.thenAccept(ignored -> result.complete(//
						subResults.stream() //
								.map(CompletableFuture::join) //
								.toList()));

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

	/**
	 * Converts a byte array to a String in the form "00C1 00B2".
	 * 
	 * @param data byte array
	 * @return string
	 */
	public static String byteArrayToHexString(byte[] data) {
		return IntStream.range(0, data.length / 2) //
				.mapToObj(i -> String.format("%2s%2s", //
						Integer.toHexString(data[i]), Integer.toHexString(data[i + 1])).replace(' ', '0'))
				.collect(Collectors.joining(" "));
	}

	/**
	 * Converts a int to a String in the form "00C1".
	 * 
	 * @param data byte array
	 * @return string
	 */
	public static String intToHexString(int data) {
		return String.format("%4s", Integer.toHexString(data)).replace(' ', '0');
	}

	/**
	 * Converts a {@link Register} array to a String in the form "00C1 00B2".
	 * 
	 * @param registers {@link Register} array
	 * @return string
	 */
	public static String registersToHexString(Register... registers) {
		return registersToHexString(registers, Register::getValue);
	}

	/**
	 * Converts a {@link InputRegister} array to a String in the form "00C1 00B2".
	 * 
	 * @param registers {@link InputRegister} array
	 * @return string
	 */
	public static String registersToHexString(InputRegister... registers) {
		return registersToHexString(registers, InputRegister::getValue);
	}

	private static <T> String registersToHexString(T[] registers, Function<T, Integer> fnct) {
		return Arrays.stream(registers) //
				.map(r -> intToHexString(fnct.apply(r))) //
				.collect(Collectors.joining(" "));
	}
}
