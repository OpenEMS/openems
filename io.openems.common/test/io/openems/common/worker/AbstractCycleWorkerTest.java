package io.openems.common.worker;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class AbstractCycleWorkerTest {

	@Test
	public void testCycle() throws InterruptedException {

		final AtomicInteger counter = new AtomicInteger(0);

		AbstractCycleWorker worker = new AbstractCycleWorker() {

			@Override
			protected void forever() {
				counter.incrementAndGet();
			}
		};

		worker.activate("test");

		for (int i = 0; i < 10; i++) {
			Thread.sleep(100);
			worker.triggerNextRun();
		}

		Thread.sleep(100);

		assertEquals(10, counter.get());

		worker.deactivate();
	}
}
