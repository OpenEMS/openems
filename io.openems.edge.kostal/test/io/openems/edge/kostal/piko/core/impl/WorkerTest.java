package io.openems.edge.kostal.piko.core.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.taskmanager.TasksManager;

public class WorkerTest {

	private static class Task extends ReadTask {

		public Task(Priority priority) {
			super(null, null, priority, null, 0);
		}

	}

	@Test
	public void testNextReadTasks() {
		Task l1 = new Task(Priority.LOW);
		Task l2 = new Task(Priority.LOW);
		Task l3 = new Task(Priority.LOW);
		Task h1 = new Task(Priority.HIGH);
		Task h2 = new Task(Priority.HIGH);
		Task h3 = new Task(Priority.HIGH);

		var m = new TasksManager<ReadTask>(l1, l2, l3, h1, h2, h3);
		var sut = new Worker(null, m);

		var t1 = sut.getNextTasks();
		assertEquals(4, t1.size());
		assertTrue(t1.contains(h1));
		assertTrue(t1.contains(h2));
		assertTrue(t1.contains(h3));
		assertTrue(t1.contains(l1));

		var t2 = sut.getNextTasks();
		assertEquals(4, t2.size());
		assertTrue(t2.contains(h1));
		assertTrue(t2.contains(h2));
		assertTrue(t2.contains(h3));
		assertTrue(t2.contains(l2));

		var t3 = sut.getNextTasks();
		assertEquals(4, t3.size());
		assertTrue(t3.contains(h1));
		assertTrue(t3.contains(h2));
		assertTrue(t3.contains(h3));
		assertTrue(t3.contains(l3));

		var t4 = sut.getNextTasks();
		assertEquals(4, t4.size());
		assertTrue(t4.contains(h1));
		assertTrue(t4.contains(h2));
		assertTrue(t4.contains(h3));
		assertTrue(t4.contains(l1));

	}
}
