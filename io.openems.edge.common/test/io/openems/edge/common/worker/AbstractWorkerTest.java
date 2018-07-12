package io.openems.edge.common.worker;

import static org.junit.Assert.*;

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

		assertEquals(5, counter.get());
		
		worker.deactivate();
	}

	@Test
	public void testTriggerForceRun() throws InterruptedException {
		
		final AtomicInteger counter = new AtomicInteger(0);
		
		AbstractWorker worker = new AbstractWorker() {
			
			@Override
			protected int getCycleTime() {
				return 0;
			}
			
			@Override
			protected void forever() {
				counter.incrementAndGet();
			}
		};
		
		worker.activate("test");
		
		Thread.sleep(100);

		worker.triggerForceRun();
		
		Thread.sleep(100);

		worker.triggerForceRun();

		Thread.sleep(100);
		
		assertEquals(3, counter.get());
		
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

		worker.triggerForceRun();
		
		Thread.sleep(20);

		assertEquals(3, counter.get());
		
		worker.deactivate();
	}

	
}
