package io.openems.common.worker;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class AbstractWorkerTest {

	@Test
	public void testDefinedCycleTime() throws InterruptedException {

		final AtomicInteger counter = new AtomicInteger(0);

		AbstractWorker worker = new AbstractWorker() {

			@Override
			protected int getCycleTime() {
				return 100;
			}

			@Override
			protected void forever() {
				counter.incrementAndGet();
			}
		};

		worker.activate("test");

		Thread.sleep(450);

		assertEquals(4, counter.get());

		worker.deactivate();
	}

	@Test
	public void testCombined() throws InterruptedException {

		final AtomicInteger counter = new AtomicInteger(0);

		AbstractWorker worker = new AbstractWorker() {

			@Override
			protected int getCycleTime() {
				return 100;
			}

			@Override
			protected void forever() {
				counter.incrementAndGet();
			}
		};

		worker.activate("test");

		Thread.sleep(120);

		worker.triggerNextRun();

		Thread.sleep(20);

		assertEquals(2, counter.get());

		worker.deactivate();
	}

}
