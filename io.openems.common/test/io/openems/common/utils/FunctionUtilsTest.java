package io.openems.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.openems.common.function.ThrowingBiConsumer;
import io.openems.common.function.ThrowingConsumer;
import io.openems.common.function.ThrowingRunnable;

public class FunctionUtilsTest {

	@Test
	public void testEmptyRunnable() {
		final Runnable runnable = FunctionUtils::doNothing;
		runnable.run();
		assertTrue(true);
	}

	@Test
	public void testEmptyThrowingRunnable() throws Exception {
		final ThrowingRunnable<Exception> runnable = FunctionUtils::doNothing;
		runnable.run();
		assertTrue(true);
	}

	@Test
	public void testEmptyConsumer() {
		final Consumer<String> consumer = FunctionUtils::doNothing;
		consumer.accept("");
		consumer.accept(null);
		assertTrue(true);
	}

	@Test
	public void testEmptyBiConsumer() {
		final BiConsumer<String, String> consumer = FunctionUtils::doNothing;
		consumer.accept("", "");
		consumer.accept("", null);
		consumer.accept(null, "");
		consumer.accept(null, null);
		assertTrue(true);
	}

	@Test
	public void testEmptyThrowingConsumer() throws Exception {
		final ThrowingConsumer<String, Exception> consumer = FunctionUtils::doNothing;
		consumer.accept("");
		consumer.accept(null);
		assertTrue(true);
	}

	@Test
	public void testEmptyThrowingBiConsumer() throws Exception {
		final ThrowingBiConsumer<String, String, Exception> consumer = FunctionUtils::doNothing;
		consumer.accept("", "");
		consumer.accept("", null);
		consumer.accept(null, "");
		consumer.accept(null, null);
		assertTrue(true);
	}

	@Test
	public void testSupplier() {
		final var supplier = FunctionUtils.supplier(() -> {
			return "success";
		});
		assertEquals("success", supplier.get());
	}

	@Test
	public void testLazySingleton() throws Exception {
		final var count = new AtomicInteger(0);
		final var singleton = FunctionUtils.lazySingleton(count::incrementAndGet);

		assertEquals(1, singleton.get().intValue());
		assertEquals(1, singleton.get().intValue());
		assertEquals(2, count.incrementAndGet());
		assertEquals(1, singleton.get().intValue());
	}

	@Test
	public void testRunWithTimeout() throws Exception {
		final var wasExecuted = new AtomicBoolean(false);
		var result = FunctionUtils.runWithTimeout("TimeoutTest", 1_000L, () -> {
			wasExecuted.set(true);
		});
		assertInstanceOf(FunctionUtils.RunWithTimeoutResult.Success.class, result);
		assertTrue(wasExecuted.get());

		wasExecuted.set(false);
		result = FunctionUtils.runWithTimeout("TimeoutTest", 1_000L, () -> {
			try {
				Thread.sleep(5_000L);
				fail("This should not be reached because sleep should interrupt.");
			} catch (InterruptedException ex) {
				// Empty
			}
		});
		assertInstanceOf(FunctionUtils.RunWithTimeoutResult.TimeoutReached.class, result);

		wasExecuted.set(false);
		result = FunctionUtils.runWithTimeout("TimeoutTest", 1_000L, () -> {
			try {
				Thread.sleep(5_000L);
				wasExecuted.set(true);
			} catch (Exception ex) {
				// Empty
			}
		});
		assertInstanceOf(FunctionUtils.RunWithTimeoutResult.TimeoutReached.class, result);
		assertFalse(wasExecuted.get());
	}

	@Test
	public void testReturnNothing() {
		final Supplier<?> supplier = FunctionUtils::returnNothing;
		assertNull(supplier.get());
	}

}
