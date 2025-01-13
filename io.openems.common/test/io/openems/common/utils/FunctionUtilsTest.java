package io.openems.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.junit.Test;

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

}
