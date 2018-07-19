package io.openems.edge.common.worker;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class AbstractImmediateWorkerTest {
	
	@Test
	public void testImmediate() throws InterruptedException {
		
		final AtomicInteger counter = new AtomicInteger(0);
		
		AbstractImmediateWorker worker = new AbstractImmediateWorker() {
			
			@Override
			protected void forever() {
				counter.incrementAndGet();
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					fail(e.getMessage());
				}
			}
		};
		
		worker.activate("test");
		
		Thread.sleep(101);

		assertTrue(counter.get() > 9);
		
		worker.deactivate();
	}
}
