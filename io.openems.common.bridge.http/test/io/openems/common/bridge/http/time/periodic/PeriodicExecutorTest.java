package io.openems.common.bridge.http.time.periodic;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.Test;

import com.google.common.base.Stopwatch;

import io.openems.common.bridge.http.time.DelayTimeProvider;
import io.openems.common.utils.FunctionUtils;

public class PeriodicExecutorTest {
	@Test
	public void testReal() {
		var factory = new PeriodicExecutorFactory();
		var watch = Stopwatch.createStarted();
		var tester = new CallTester(DelayTimeProvider.Delay.of(Duration.ofSeconds(2)));
		final var executor = factory.execute("Test", tester, DelayTimeProvider.Delay.immediate());

		tester.awaitExecution(3000);
		assertTrue(watch.elapsed().getSeconds() < 1);
		tester.awaitExecution(3000);
		assertTrue(watch.elapsed().getSeconds() > 1);

		executor.dispose();
	}

	@Test
	public void testDummy() {
		var factory = new DummyPeriodicExecutorFactory();
		var tester = new CallTester(DelayTimeProvider.Delay.of(Duration.ofSeconds(2)));
		var executor = factory.execute("Test Dummy", tester, DelayTimeProvider.Delay.immediate());

		assertTrue(tester.hasCalled());

		executor.dispose();
	}

	@Test
	public void testDummyException() {
		var factory = new DummyPeriodicExecutorFactory();
		var tester = new CallTester(DelayTimeProvider.Delay.of(Duration.ofSeconds(2)));
		tester.task = () -> {
			throw new RuntimeException("That's a test");
		};

		assertThrows(RuntimeException.class,
				() -> factory.execute("Test Dummy", tester, DelayTimeProvider.Delay.immediate()));
		assertTrue(tester.hasCalled());
	}

	private static class CallTester implements Supplier<DelayTimeProvider.Delay> {
		private final AtomicInteger counter = new AtomicInteger();
		private final DelayTimeProvider.Delay delay;
		private Runnable task = FunctionUtils::doNothing;

		private CallTester(DelayTimeProvider.Delay delay) {
			this.delay = delay;
		}

		public boolean hasCalled() {
			return this.counter.get() > 0;
		}

		public synchronized void awaitExecution(int timeoutMillis) {
			int remainingCalls = this.counter.getAndUpdate(i -> Math.max(0, i - 1));
			if (remainingCalls > 0) {
				return;
			}

			try {
				this.wait(timeoutMillis);
				this.counter.updateAndGet(i -> Math.max(0, i - 1));
			} catch (InterruptedException ex) {
				throw new RuntimeException("Action was not executed in due time.");
			}
		}

		@Override
		public synchronized DelayTimeProvider.Delay get() {
			try {
				this.task.run();
			} finally {
				this.counter.incrementAndGet();
				this.notify();
			}
			return this.delay;
		}
	}
}
