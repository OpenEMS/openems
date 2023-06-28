package io.openems.edge.bridge.modbus.api.worker.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.DummyModbusComponent;
import io.openems.edge.bridge.modbus.api.task.WaitTask;
import io.openems.edge.bridge.modbus.api.worker.DummyReadTask;
import io.openems.edge.bridge.modbus.api.worker.DummyWriteTask;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.taskmanager.Priority;

public class CycleTasksManagerTest {

	public static final Consumer<Boolean> CYCLE_TIME_IS_TOO_SHORT_CALLBACK = (cycleTimeIsTooShort) -> {
	};

	private static final String CMP = "foo";
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
		var cycleTasksSupplier = new DummyCycleTasksSupplier(cycle1, cycle2);
		var defectiveComponents = new DefectiveComponents();

		var sut = new CycleTasksManager(cycleTasksSupplier, defectiveComponents, CYCLE_TIME_IS_TOO_SHORT_CALLBACK);

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
	public void testDefective() throws OpenemsException, InterruptedException {
		var bridge = new DummyModbusBridge("modbus0");
		var foo = new DummyModbusComponent(CMP, bridge);
		RT_L_1.setParent(foo);

		var cycle1 = CycleTasks.create() //
				.reads(RT_L_1, RT_H_1, RT_H_2) //
				.writes(WT_1) //
				.build();
		var cycle2 = CycleTasks.create() //
				.reads(RT_L_2, RT_H_1, RT_H_2) //
				.writes(WT_1) //
				.build();
		var cycleTasksSupplier = new DummyCycleTasksSupplier(cycle1, cycle2);
		var defectiveComponents = new DefectiveComponents();
		defectiveComponents.add(CMP);

		var sut = new CycleTasksManager(cycleTasksSupplier, defectiveComponents, CYCLE_TIME_IS_TOO_SHORT_CALLBACK);

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
		var cycleTasksSupplier = new DummyCycleTasksSupplier(cycle1);
		var defectiveComponents = new DefectiveComponents();

		var sut = new CycleTasksManager(cycleTasksSupplier, defectiveComponents, CYCLE_TIME_IS_TOO_SHORT_CALLBACK);

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
