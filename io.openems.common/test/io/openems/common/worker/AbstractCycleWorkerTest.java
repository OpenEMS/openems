package io.openems.common.worker;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;

public class AbstractCycleWorkerTest {

	@Test
	public void testCycle() throws InterruptedException, ExecutionException, TimeoutException {
		final var future = new AtomicReference<CompletableFuture<Void>>(new CompletableFuture<>());
		final var counter = new AtomicInteger(0);

		AbstractCycleWorker worker = new AbstractCycleWorker() {

			@Override
			protected void forever() {
				counter.incrementAndGet();
				future.get().complete(null);
			}
		};

		worker.activate("test");

		for (var i = 0; i < 10; i++) {
			future.get().get(100, TimeUnit.MILLISECONDS);
			future.set(new CompletableFuture<>());
			worker.triggerNextRun();
		}

		future.get().get(100, TimeUnit.MILLISECONDS);
		Assert.assertEquals(11, counter.get());

		worker.deactivate();
	}
}
