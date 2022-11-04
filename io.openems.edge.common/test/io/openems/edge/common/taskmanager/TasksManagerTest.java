package io.openems.edge.common.taskmanager;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TasksManagerTest {

	private static class Task implements ManagedTask {

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
	public void testGetOneTask() {
		ManagedTask l1 = new Task(Priority.LOW);
		ManagedTask h1 = new Task(Priority.HIGH);

		var m = new TasksManager<>(l1, h1);

		assertEquals(l1, m.getOneTask());
		assertEquals(h1, m.getOneTask());

	}

}
