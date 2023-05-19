package io.openems.edge.bridge.modbus.api.worker.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.bridge.modbus.api.worker.DummyReadTask;
import io.openems.edge.bridge.modbus.api.worker.DummyWriteTask;
import io.openems.edge.common.taskmanager.Priority;

public class CycleTasksTest {

	public final static Consumer<Boolean> CYCLE_TIME_IS_TOO_SHORT_CALLBACK = (cycleTimeIsTooShort) -> {
	};

	public static DummyReadTask RT_H_1;
	public static DummyReadTask RT_H_2;
	public static DummyReadTask RT_L_1;
	public static DummyReadTask RT_L_2;
	public static DummyWriteTask WT_1;

	@Before
	public void before() {
		RT_H_1 = new DummyReadTask("RT_H_1", 49, Priority.HIGH);
		RT_H_2 = new DummyReadTask("RT_H_2", 70, Priority.HIGH);
		RT_L_1 = new DummyReadTask("RT_L_1", 20, Priority.LOW);
		RT_L_2 = new DummyReadTask("RT_L_2", 30, Priority.LOW);
		WT_1 = new DummyWriteTask("WT_1", 90);
	}

	@Test
	public void test() throws OpenemsException, InterruptedException {
		var cycle1 = CycleTasks.create() //
				.reads(RT_L_1, RT_H_1, RT_H_2) //
				.writes(WT_1) //
				.build();
		var cycle2 = CycleTasks.create() //
				.reads(RT_L_2, RT_H_1, RT_H_2) //
				.writes(WT_1) //
				.build();
		var cycleTasksSupplier = new DummyCycleTasksSupplier(cycle1, cycle2);

		var sut = new CycleTasksManager(LogVerbosity.DEV_REFACTORING, cycleTasksSupplier,
				CYCLE_TIME_IS_TOO_SHORT_CALLBACK);

		// Cycle 1
		sut.onBeforeProcessImage();
		var task = sut.getNextTask();
		assertTrue(task instanceof WaitDelayTask);
		task.execute(null);

		task = sut.getNextTask();
		assertEquals(RT_L_1, task);
		task.execute(null);

		sut.onExecuteWrite();
		task = sut.getNextTask();
		assertEquals(WT_1, task);
		task.execute(null);

		task = sut.getNextTask();
		assertTrue(task instanceof WaitDelayTask);
		task.execute(null);

		task = sut.getNextTask();
		assertEquals(RT_H_1, task);
		task.execute(null);

		task = sut.getNextTask();
		assertEquals(RT_H_2, task);
		task.execute(null);

		task = sut.getNextTask();
		assertTrue(task instanceof WaitMutexTask);
		// task.execute(null); -> this would block in single-threaded JUnit test

		// Cycle 2
		sut.onBeforeProcessImage();
		task = sut.getNextTask();
		assertTrue(task instanceof WaitDelayTask);
		task.execute(null);

		task = sut.getNextTask();
		assertEquals(RT_L_2, task);
		task.execute(null);

		sut.onExecuteWrite();
		task = sut.getNextTask();
		assertEquals(WT_1, task);
		task.execute(null);

		task = sut.getNextTask();
		assertTrue(task instanceof WaitDelayTask);
		task.execute(null);

		task = sut.getNextTask();
		assertEquals(RT_H_1, task);
		task.execute(null);

		task = sut.getNextTask();
		assertEquals(RT_H_2, task);
		task.execute(null);

		// Cycle 3
		sut.onBeforeProcessImage();
		task = sut.getNextTask();
		assertTrue(task instanceof WaitMutexTask);
		// task.execute(null); -> this would block in single-threaded JUnit test
	}

}
