package io.openems.edge.bridge.modbus.api;

import static io.openems.edge.bridge.modbus.api.ModbusUtils.abortAfterNthErrors;
import static io.openems.edge.bridge.modbus.api.ModbusUtils.doNotRetry;
import static io.openems.edge.bridge.modbus.api.ModbusUtils.intToHexString;
import static io.openems.edge.bridge.modbus.api.ModbusUtils.readElementsUntil;
import static io.openems.edge.bridge.modbus.api.ModbusUtils.registersToHexString;
import static io.openems.edge.bridge.modbus.api.ModbusUtils.restartAfterChannelChange;
import static io.openems.edge.bridge.modbus.api.ModbusUtils.retryOnNull;
import static io.openems.edge.bridge.modbus.api.task.Task.ExecuteState.NO_OP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.ghgande.j2mod.modbus.procimg.SimpleInputRegister;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import io.openems.common.function.Disposable;
import io.openems.common.utils.FunctionUtils;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.task.ReadTask;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.bridge.modbus.api.task.Task.ExecuteState;
import io.openems.edge.common.channel.Channel;

class ModbusUtilsTest {

	@Test
	void testRetryOnNull() {
		assertTrue(retryOnNull(ExecuteState.OK, null));
		assertFalse(retryOnNull(ExecuteState.OK, 123));
	}

	@Test
	void testDoNotRetry() {
		assertFalse(doNotRetry(ExecuteState.OK, null));
		assertFalse(doNotRetry(ExecuteState.OK, 123));
	}

	@Test
	void testIntToHexString() {
		assertEquals("00af", intToHexString(0xAF));
	}

	@Test
	void testRegistersToHexString() {
		assertEquals("00aa 00ff", registersToHexString(new SimpleRegister(0xAA), new SimpleRegister(0xFF)));
	}

	@Test
	void testInputRegistersToHexString() {
		assertEquals("00aa 00ff", registersToHexString(new SimpleInputRegister(0xAA), new SimpleInputRegister(0xFF)));
	}

	@Test
	void testAbortAfterNthErrors() {
		final var onValue = (Consumer<?>) mock(Consumer.class);

		final var abortFunction = abortAfterNthErrors(2, onValue);

		final var predicate = abortFunction.get();

		final var errorState = new ExecuteState.Error(new RuntimeException("Test error"));
		assertTrue(predicate.test(errorState, null));
		assertFalse(predicate.test(errorState, null));
		verify(onValue, times(2)).accept(any());

		final var newPredicate = abortFunction.get();
		assertTrue(newPredicate.test(errorState, null));
		assertFalse(predicate.test(errorState, null));
		verify(onValue, times(4)).accept(any());
	}

	@Test
	void testAbortAfterNthErrorsReset() {
		final var onValue = (Consumer<?>) mock(Consumer.class);

		final var abortFunction = abortAfterNthErrors(2, onValue);

		final var predicate = abortFunction.get();

		final var errorState = new ExecuteState.Error(new RuntimeException("Test error"));
		assertTrue(predicate.test(errorState, null));
		assertTrue(predicate.test(ExecuteState.OK, null));
		assertTrue(predicate.test(errorState, null));
		assertFalse(predicate.test(errorState, null));
		verify(onValue, times(4)).accept(any());
	}

	@Test
	void testRestartAfterChannelChange() {
		final var channel = (Channel<?>) mock(Channel.class);
		final var function = restartAfterChannelChange(channel);

		final var disposable = function.apply(FunctionUtils::doNothing);
		verify(channel, times(1)).onChange(any());

		disposable.dispose();
		verify(channel, times(1)).removeOnChangeCallback(any());
	}

	@Test
	void testReadElementsUntilContinuePredicate() {
		var modbusProtocol = mock(ModbusProtocol.class);
		var task = mock(ReadTask.class);
		var element = (AbstractModbusElement<?, ?, ?>) mock(AbstractModbusElement.class);

		when(task.getElements()).thenReturn(new AbstractModbusElement[] { element });

		var updateCallback = new AtomicReference<Consumer<Object>>();
		doAnswer(invocation -> {
			updateCallback.set(invocation.getArgument(0));
			return null;
		}).when(element).onUpdateCallback(any());

		final var predicateCalls = new AtomicInteger(0);
		Supplier<BiPredicate<ExecuteState, Object>> continuePredicateFactory = () -> (state,
				value) -> predicateCalls.incrementAndGet() == 1;

		final var restartFunction = FunctionUtils.<Runnable, Disposable>alwaysReturn(FunctionUtils::doNothing);
		final var taskFactory = FunctionUtils.<Consumer<ExecuteState>, Task>alwaysReturn(task);

		readElementsUntil(modbusProtocol, continuePredicateFactory, restartFunction, taskFactory);

		assertNotNull(updateCallback.get());

		updateCallback.get().accept(123);
		verify(modbusProtocol, never()).removeTask(task);

		updateCallback.get().accept(456);
		verify(modbusProtocol, times(1)).removeTask(task);
		verify(modbusProtocol, times(1)).addTask(task);
		assertEquals(2, predicateCalls.get());
	}

	@Test
	void testReadElementsUntilRestartFunction() {
		var modbusProtocol = mock(ModbusProtocol.class);
		var task = mock(ReadTask.class);
		var element = (AbstractModbusElement<?, ?, ?>) mock(AbstractModbusElement.class);

		when(task.getElements()).thenReturn(new AbstractModbusElement[] { element });

		var updateCallback = new AtomicReference<Consumer<Object>>();
		doAnswer(invocation -> {
			updateCallback.set(invocation.getArgument(0));
			return null;
		}).when(element).onUpdateCallback(any());

		var factoryCalls = new AtomicInteger(0);
		Supplier<BiPredicate<ExecuteState, Object>> continuePredicateFactory = () -> {
			factoryCalls.incrementAndGet();
			return (state, value) -> false;
		};

		var restartRef = new AtomicReference<Runnable>();
		Function<Runnable, Disposable> restartFunction = restart -> {
			restartRef.set(restart);
			return FunctionUtils::doNothing;
		};
		Function<Consumer<ExecuteState>, Task> taskFactory = onExecute -> {
			onExecute.accept(NO_OP);
			return task;
		};

		readElementsUntil(modbusProtocol, continuePredicateFactory, restartFunction, taskFactory);

		assertNotNull(updateCallback.get());
		assertNotNull(restartRef.get());

		updateCallback.get().accept(1);
		verify(modbusProtocol, times(1)).removeTask(task);

		restartRef.get().run();
		verify(modbusProtocol, times(2)).addTask(task);
		assertEquals(3, factoryCalls.get());
	}

}
