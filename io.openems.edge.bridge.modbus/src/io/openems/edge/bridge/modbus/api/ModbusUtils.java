package io.openems.edge.bridge.modbus.api;

import static io.openems.edge.bridge.modbus.api.task.Task.ExecuteState.NO_OP;
import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;

import io.openems.common.function.Disposable;
import io.openems.common.utils.FunctionUtils;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.ModbusRegisterElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.bridge.modbus.api.task.Task.ExecuteState;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.taskmanager.Priority;

public class ModbusUtils {

	/**
	 * Enum representing Modbus function codes used to select the appropriate task.
	 * 
	 * <p>
	 * This enum defines the available function codes for use in modbus tasks.
	 */
	public static enum FunctionCode {
		FC3, FC4;
	}

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
	 * Predefined `retryPredicate` that aborts after nth errors in a row occurred.
	 *
	 * @param <T>            the Type of the element
	 * @param numberOfErrors the number of errors until the task is removed
	 * @param onValue        callback to receive the value
	 * @return a predicate which returns true until the nth error in a row occurred
	 */
	public static <T> Supplier<BiPredicate<ExecuteState, T>> abortAfterNthErrors(//
			int numberOfErrors, //
			Consumer<T> onValue //
	) {
		return () -> {
			final var counter = new AtomicInteger(0);
			return (executeState, t) -> {
				onValue.accept(t);

				if (executeState instanceof ExecuteState.Error) {
					return counter.incrementAndGet() < numberOfErrors;
				}

				counter.set(0);
				return true;
			};
		};
	}

	/**
	 * Predefined `retryPredicate` that aborts after nth errors in a row occurred.
	 *
	 * @param <T>            the Type of the element
	 * @param numberOfErrors the number of errors until the task is removed
	 * @return a predicate which returns true until the nth error in a row occurred
	 */
	public static <T> Supplier<BiPredicate<ExecuteState, T>> abortAfterNthErrors(int numberOfErrors) {
		return abortAfterNthErrors(numberOfErrors, FunctionUtils::doNothing);
	}

	/**
	 * Predefined `restartFunction` that restart when the value of the channel
	 * changes.
	 * 
	 * @param channel the channel to subscribe to
	 * @return a {@link Function} which triggers a restart when the channel value
	 *         changes
	 */
	public static Function<Runnable, Disposable> restartAfterChannelChange(Channel<?> channel) {
		return restart -> {
			final var onChangeListener = channel.onChange((value, value2) -> restart.run());

			return () -> channel.removeOnChangeCallback(onChangeListener);
		};
	}

	/**
	 * Reads given Element once from Modbus.
	 *
	 * @param <T>            the Type of the element
	 * @param functionCode   the {@link FunctionCode}
	 * @param modbusProtocol the {@link ModbusProtocol}, that is linked with a
	 *                       {@link BridgeModbus}
	 * @param retryPredicate yield true to retry reading values; false otherwise.
	 *                       Parameters are the {@link ExecuteState} of the entire
	 *                       task and the individual element value
	 * @param element        the {@link ModbusRegisterElement}
	 * @return a future value, e.g. a Integer or null (if tryAgainOnError is false)
	 */
	@SuppressWarnings("unchecked")
	public static <T> CompletableFuture<T> readElementOnce(FunctionCode functionCode, ModbusProtocol modbusProtocol,
			BiPredicate<ExecuteState, T> retryPredicate, ModbusRegisterElement<?, T> element) {
		return readElementsOnce(functionCode, modbusProtocol, retryPredicate, //
				new ModbusRegisterElement[] { element }) //
				.thenApply(rsr -> ((ReadElementsResult<T>) rsr).values().get(0));
	}

	/**
	 * Reads given Elements once from Modbus.
	 *
	 * @param <T>            the Type of the elements
	 * @param functionCode   the {@link FunctionCode}
	 * @param modbusProtocol the {@link ModbusProtocol}, that is linked with a
	 *                       {@link BridgeModbus}
	 * @param retryPredicate yield true to retry reading values. Parameters are the
	 *                       Task success state and individual element value
	 * @param elements       the {@link ModbusRegisterElement}s
	 * @return a future array of values, e.g. Integer[] or null. If an array is
	 *         returned, it is guaranteed to have the same length as `elements`
	 */
	@SafeVarargs
	public static <T> CompletableFuture<ReadElementsResult<T>> readElementsOnce(FunctionCode functionCode,
			ModbusProtocol modbusProtocol, BiPredicate<ExecuteState, T> retryPredicate,
			ModbusRegisterElement<?, T>... elements) {
		if (elements.length == 0) {
			return completedFuture(new ReadElementsResult<>(NO_OP, emptyList()));
		}

		// Register listener for each element
		final var executeState = new AtomicReference<ExecuteState>(NO_OP);

		// Activate task based on functionCode
		Task task = switch (functionCode) {
		case FC4 -> new FC4ReadInputRegistersTask(executeState::set, elements[0].startAddress, Priority.HIGH, elements);
		case FC3 -> new FC3ReadRegistersTask(executeState::set, elements[0].startAddress, Priority.HIGH, elements);
		};
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

	/**
	 * Reads given Elements from Modbus until the predicate returns false.
	 *
	 * @param modbusProtocol           the {@link ModbusProtocol}, that is linked
	 *                                 with a {@link BridgeModbus}
	 * @param continuePredicateFactory yield true to continue reading values.
	 *                                 Parameters are the Task success state and
	 *                                 individual element value
	 * @param restartFunction          a function with a {@link Runnable} to restart
	 *                                 the reading process
	 * @param taskFactory              the factory to create the {@link Task}
	 * @return a {@link Disposable} to clean everything up
	 */
	public static Disposable readElementsUntil(//
			ModbusProtocol modbusProtocol, //
			Supplier<BiPredicate<ExecuteState, Object>> continuePredicateFactory, //
			Function<Runnable, Disposable> restartFunction, //
			Function<Consumer<ExecuteState>, Task> taskFactory //
	) {
		final var executeState = new AtomicReference<ExecuteState>(NO_OP);
		final var task = taskFactory.apply(executeState::set);

		if (task.getElements().length == 0) {
			return FunctionUtils::doNothing;
		}

		final var predicate = new AtomicReference<>(continuePredicateFactory.get());
		final var finished = new AtomicBoolean(false);

		for (var element : task.getElements()) {
			switch (element) {
			case AbstractModbusElement<?, ?, ?> e -> e.onUpdateCallback(value -> {
				final var p = predicate.get();
				if (p.test(executeState.get(), value)) {
					return;
				}
				synchronized (finished) {
					if (predicate.get() != p) {
						return;
					}
					finished.set(true);
					modbusProtocol.removeTask(task);
				}
			});
			}
		}

		modbusProtocol.addTask(task);

		final Runnable restart = () -> {
			synchronized (finished) {
				predicate.set(continuePredicateFactory.get());
				if (finished.get()) {
					finished.set(false);
					modbusProtocol.addTask(task);
				}
			}
		};

		restart.run();

		return restartFunction.apply(restart);
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