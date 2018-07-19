package io.openems.edge.common.taskmanager;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.taskmanager.TaskManager;

public class TasksManagerTest {

	private class Task implements ManagedTask {

		private final Priority priority;

		public Task(Priority priority) {
			this.priority = priority;
		}

		@Override
		public Priority getPriority() {
			return this.priority;
		}

	}

	@Test
	public void testNextReadTasks() {
		ManagedTask o1 = new Task(Priority.ONCE);
		ManagedTask o2 = new Task(Priority.ONCE);
		ManagedTask l1 = new Task(Priority.LOW);
		ManagedTask l2 = new Task(Priority.LOW);
		ManagedTask l3 = new Task(Priority.LOW);
		ManagedTask h1 = new Task(Priority.HIGH);
		ManagedTask h2 = new Task(Priority.HIGH);
		ManagedTask h3 = new Task(Priority.HIGH);

		TaskManager<ManagedTask> m = new TaskManager<ManagedTask>(o1, o2, l1, l2, l3, h1, h2, h3);

		List<ManagedTask> t1 = m.getNextReadTasks();
		assertEquals(5, t1.size());
		assertTrue(t1.contains(h1));
		assertTrue(t1.contains(h2));
		assertTrue(t1.contains(h3));
		assertTrue(t1.contains(o1));
		assertTrue(t1.contains(l1));

		List<ManagedTask> t2 = m.getNextReadTasks();
		assertEquals(5, t2.size());
		assertTrue(t2.contains(h1));
		assertTrue(t2.contains(h2));
		assertTrue(t2.contains(h3));
		assertTrue(t2.contains(o2));
		assertTrue(t2.contains(l2));

		List<ManagedTask> t3 = m.getNextReadTasks();
		assertEquals(4, t3.size());
		assertTrue(t3.contains(h1));
		assertTrue(t3.contains(h2));
		assertTrue(t3.contains(h3));
		assertTrue(t3.contains(l3));

		List<ManagedTask> t4 = m.getNextReadTasks();
		assertEquals(4, t4.size());
		assertTrue(t4.contains(h1));
		assertTrue(t4.contains(h2));
		assertTrue(t4.contains(h3));
		assertTrue(t4.contains(l1));

	}

}
