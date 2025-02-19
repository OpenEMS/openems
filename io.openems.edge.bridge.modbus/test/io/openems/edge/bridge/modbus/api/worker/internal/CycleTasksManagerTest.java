package io.openems.edge.bridge.modbus.api.worker.internal;

import static io.openems.edge.bridge.modbus.api.worker.internal.CycleTasksManager.StateMachine.FINISHED;
import static io.openems.edge.bridge.modbus.api.worker.internal.CycleTasksManager.StateMachine.WAIT_BEFORE_READ;
import static io.openems.edge.bridge.modbus.api.worker.internal.CycleTasksManager.StateMachine.WRITE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.DummyModbusComponent;
import io.openems.edge.bridge.modbus.api.Config;
import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.bridge.modbus.api.task.WaitTask;
import io.openems.edge.bridge.modbus.api.worker.DummyReadTask;
import io.openems.edge.bridge.modbus.api.worker.DummyWriteTask;
import io.openems.edge.common.taskmanager.Priority;

public class CycleTasksManagerTest {

	public static final Consumer<Boolean> CYCLE_TIME_IS_TOO_SHORT = (cycleTimeIsTooShort) -> {
	};
	public static final Consumer<Long> CYCLE_DELAY = (cycleDelay) -> {
	};
	private static final Config CONFIG = new Config("foo", "bar", true, LogVerbosity.NONE, 1);
	public static final Supplier<Config.LogHandler> LOG_HANDLER = () -> CONFIG.log;

	private static DummyReadTask RT_H_1;
	private static DummyReadTask RT_H_2;
	private static DummyReadTask RT_L_1;
	private static DummyReadTask RT_L_2;
	private static DummyWriteTask WT_1;

	@Before
	public void before() {
		RT_H_1 = new DummyReadTask("RT_H_1", 49, Priority.HIGH);
		RT_H_2 = new DummyReadTask("RT_H_2", 70, Priority.HIGH);
		RT_L_1 = new DummyReadTask("RT_L_1", 20, Priority.LOW);
		RT_L_2 = new DummyReadTask("RT_L_2", 30, Priority.LOW);
		WT_1 = new DummyWriteTask("WT_1", 90);
	}

	@Test
	public void testIdealConditions() throws OpenemsException, InterruptedException {
		var cycle1 = CycleTasks.create() //
				.reads(RT_L_1, RT_H_1, RT_H_2) //
				.writes(WT_1) //
				.build();
		var cycle2 = CycleTasks.create() //
				.reads(RT_L_2, RT_H_1, RT_H_2) //
				.writes(WT_1) //
				.build();
		var tasksSupplier = new DummyTasksSupplier(cycle1, cycle2);
		var defectiveComponents = new DefectiveComponents(LOG_HANDLER);

		var sut = new CycleTasksManager(tasksSupplier, defectiveComponents, //
				CYCLE_TIME_IS_TOO_SHORT, CYCLE_DELAY, LOG_HANDLER);

		// Cycle 1
		sut.onBeforeProcessImage();
		var task = sut.getNextTask();
		assertTrue(task instanceof WaitTask.Delay);
		task.execute(null);

		task = sut.getNextTask();
		assertEquals(RT_L_1, task);
		task.execute(null);

		sut.onExecuteWrite();
		task = sut.getNextTask();
		assertEquals(WT_1, task);
		task.execute(null);

		task = sut.getNextTask();
		assertTrue(task instanceof WaitTask.Delay);
		task.execute(null);

		task = sut.getNextTask();
		assertEquals(RT_H_1, task);
		task.execute(null);

		task = sut.getNextTask();
		assertEquals(RT_H_2, task);
		task.execute(null);

		task = sut.getNextTask();
		assertTrue(task instanceof WaitTask.Mutex);
		// task.execute(null); -> this would block in single-threaded JUnit test

		// Cycle 2
		sut.onBeforeProcessImage();
		task = sut.getNextTask();
		assertTrue(task instanceof WaitTask.Delay);
		task.execute(null);

		task = sut.getNextTask();
		assertEquals(RT_L_2, task);
		task.execute(null);

		sut.onExecuteWrite();
		task = sut.getNextTask();
		assertEquals(WT_1, task);
		task.execute(null);

		task = sut.getNextTask();
		assertTrue(task instanceof WaitTask.Delay);
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
		assertTrue(task instanceof WaitTask.Mutex);
		// task.execute(null); -> this would block in single-threaded JUnit test
	}

	@Test
	public void testExecuteWriteBeforeNextProcessImage() throws OpenemsException, InterruptedException {
		var cycle1 = CycleTasks.create() //
				.reads(RT_L_1, RT_H_1, RT_H_2) //
				.writes(WT_1) //
				.build();
		var cycle2 = CycleTasks.create() //
				.reads(RT_L_2, RT_H_1, RT_H_2) //
				.writes(WT_1) //
				.build();
		var tasksSupplier = new DummyTasksSupplier(cycle1, cycle2);
		var defectiveComponents = new DefectiveComponents(LOG_HANDLER);

		var sut = new CycleTasksManager(tasksSupplier, defectiveComponents, //
				CYCLE_TIME_IS_TOO_SHORT, CYCLE_DELAY, LOG_HANDLER);

		sut.getNextTask();
		assertEquals(FINISHED, sut.getState());
		sut.onExecuteWrite();
		sut.getNextTask();
		assertEquals(WRITE, sut.getState());
		sut.onBeforeProcessImage();
		sut.getNextTask();
		assertEquals(WAIT_BEFORE_READ, sut.getState());
	}

	@Test
	public void testDefective() throws OpenemsException, InterruptedException {
		var component = new DummyModbusComponent();
		RT_L_1.setParent(component);

		var cycle1 = CycleTasks.create() //
				.reads(RT_L_1, RT_H_1, RT_H_2) //
				.writes(WT_1) //
				.build();
		var cycle2 = CycleTasks.create() //
				.reads(RT_L_2, RT_H_1, RT_H_2) //
				.writes(WT_1) //
				.build();
		var tasksSupplier = new DummyTasksSupplier(cycle1, cycle2);
		var defectiveComponents = new DefectiveComponents(LOG_HANDLER);
		defectiveComponents.add(component.id());

		var sut = new CycleTasksManager(tasksSupplier, defectiveComponents, //
				CYCLE_TIME_IS_TOO_SHORT, CYCLE_DELAY, LOG_HANDLER);

		// Cycle 1
		sut.onBeforeProcessImage();
		var task = sut.getNextTask();
		assertTrue(task instanceof WaitTask.Delay);
		task.execute(null);

		task = sut.getNextTask();
		assertEquals(RT_L_1, task);
		task.execute(null);

		sut.getNextTask().execute(null);
		sut.getNextTask().execute(null);
		sut.onExecuteWrite();
		sut.getNextTask().execute(null);
		sut.getNextTask().execute(null);
		sut.getNextTask();

		// Cycle 2
		sut.onBeforeProcessImage();
	}

	@Test
	public void testNoTasks() throws OpenemsException, InterruptedException {
		var cycle1 = CycleTasks.create() //
				.build();
		var tasksSupplier = new DummyTasksSupplier(cycle1);
		var defectiveComponents = new DefectiveComponents(LOG_HANDLER);

		var sut = new CycleTasksManager(tasksSupplier, defectiveComponents, //
				CYCLE_TIME_IS_TOO_SHORT, CYCLE_DELAY, LOG_HANDLER);

		// Cycle 1
		sut.onBeforeProcessImage();
		var task = sut.getNextTask();
		assertTrue(task instanceof WaitTask.Delay);
		task.execute(null);

		sut.onBeforeProcessImage();

		task = sut.getNextTask();
		assertTrue(task instanceof WaitTask.Mutex);
	}
}
