package io.openems.common.worker;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.openems.common.worker.AbstractImmediateWorker;

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
