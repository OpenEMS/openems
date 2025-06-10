package io.openems.backend.metadata.odoo.postgres;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.LinkedBlockingQueue;

import org.junit.Test;

public class PeriodicWriteWorkerTest {

	@Test
	public void testDrainToSet() {
		final LinkedBlockingQueue<Integer> queue = new LinkedBlockingQueue<>();
		queue.add(1);
		queue.add(2);
		queue.add(3);
		queue.add(2);
		queue.add(4);
		var set = PeriodicWriteWorker.drainToSet(queue);
		assertEquals(4, set.size());
		assertEquals(0, queue.size());
	}

}
