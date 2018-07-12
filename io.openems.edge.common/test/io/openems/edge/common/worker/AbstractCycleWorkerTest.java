package io.openems.edge.common.worker;

import static org.junit.Assert.*;

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
		
		Thread.sleep(100);

		worker.triggerNextCycle();

		Thread.sleep(100);
		
		worker.triggerNextCycle();

		assertEquals(3, counter.get());
		
		worker.deactivate();
	}
}
