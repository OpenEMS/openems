package io.openems.common.worker;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

public class AbstractCycleWorkerTest {

	@Test
	public void testCycle() throws InterruptedException {

		final var counter = new AtomicInteger(0);

		AbstractCycleWorker worker = new AbstractCycleWorker() {

			@Override
			protected void forever() {
				counter.incrementAndGet();
			}
		};

		worker.activate("test");

		for (var i = 0; i < 10; i++) {
			Thread.sleep(100);
			worker.triggerNextRun();
		}

		Thread.sleep(100);

		Assert.assertEquals(11, counter.get());

		worker.deactivate();
	}
}
