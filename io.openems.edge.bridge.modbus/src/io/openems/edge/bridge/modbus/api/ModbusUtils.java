package io.openems.edge.bridge.modbus.api;

import static io.openems.edge.bridge.modbus.api.task.Task.ExecuteState.NO_OP;
import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;

import io.openems.edge.bridge.modbus.api.element.ModbusRegisterElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.bridge.modbus.api.task.Task.ExecuteState;
import io.openems.edge.common.taskmanager.Priority;

public class ModbusUtils {

	/**
	 * Predefined `retryPredicate` that triggers a retry whenever `value` is null,
	 * i.e. on any error.
	 * 
	 * @param <T>          the Type of the element
	 * @param executeState the Task {@link ExecuteState}
	 * @param value        the value
	 * @return true for retry
	 */
	public static <T> boolean retryOnNull(ExecuteState executeState, T value) {
		return value == null;
	}

	/**
	 * Predefined `retryPredicate` that never retries.
	 * 
	 * @param <T>          the Type of the element
	 * @param executeState the Task {@link ExecuteState}
	 * @param value        the value
	 * @return always false
	 */
	public static <T> boolean doNotRetry(ExecuteState executeState, T value) {
		return false;
	}

	/**
	 * Reads given Element once from Modbus.
	 *
	 * @param <T>            the Type of the element
	 * @param modbusProtocol the {@link ModbusProtocol}, that is linked with a
	 *                       {@link BridgeModbus}
	 * @param retryPredicate yield true to retry reading values; false otherwise.
	 *                       Parameters are the {@link ExecuteState} of the entire
	 *                       task and the individual element value
	 * @param element        the {@link ModbusRegisterElement}
	 * @return a future value, e.g. a Integer or null (if tryAgainOnError is false)
	 */
	@SuppressWarnings("unchecked")
	public static <T> CompletableFuture<T> readElementOnce(ModbusProtocol modbusProtocol,
			BiPredicate<ExecuteState, T> retryPredicate, ModbusRegisterElement<?, T> element) {
		return readElementsOnce(modbusProtocol, retryPredicate, //
				new ModbusRegisterElement[] { element }) //
				.thenApply(rsr -> ((ReadElementsResult<T>) rsr).values().get(0));
	}

	/**
	 * Reads given Elements once from Modbus.
	 *
	 * @param <T>            the Type of the elements
	 * @param modbusProtocol the {@link ModbusProtocol}, that is linked with a
	 *                       {@link BridgeModbus}
	 * @param retryPredicate yield true to retry reading values. Parameters are the
	 *                       Task success state and individual element value
	 * @param elements       the {@link ModbusRegisterElement}s
	 * @return a future array of values, e.g. Integer[] or null. If an array is
	 *         returned, it is guaranteed to have the same length as `elements`
	 */
	@SafeVarargs
	public static <T> CompletableFuture<ReadElementsResult<T>> readElementsOnce(ModbusProtocol modbusProtocol,
			BiPredicate<ExecuteState, T> retryPredicate, ModbusRegisterElement<?, T>... elements) {
		if (elements.length == 0) {
			return completedFuture(new ReadElementsResult<>(NO_OP, emptyList()));
		}

		// Register listener for each element
		final var executeState = new AtomicReference<ExecuteState>(ExecuteState.NO_OP);

		// Activate task
		final Task task = new FC3ReadRegistersTask(executeState::set, //
				elements[0].startAddress, Priority.HIGH, elements);
		modbusProtocol.addTask(task);

		@SuppressWarnings("unchecked")
		final var subResults = (CompletableFuture<T>[]) new CompletableFuture<?>[elements.length];
		for (var i = 0; i < elements.length; i++) {
			var subResult = new CompletableFuture<T>();
			subResults[i] = subResult;
			elements[i].onUpdateCallback(value -> {
				if (retryPredicate.test(executeState.get(), value)) {
					// try again
					return;
				} else {
					// do not try again
					subResult.complete(value);
				}
			});
		}

		return CompletableFuture //
				.allOf(subResults) //
				.thenApply(ignore -> {
					// remove task
					modbusProtocol.removeTask(task);

					// return combined future
					return new ReadElementsResult<>(executeState.get(), //
							Stream.of(subResults) //
									.map(CompletableFuture::join) //
									.toList());
				});
	}

	public static record ReadElementsResult<T>(ExecuteState executeState, List<T> values) {

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
