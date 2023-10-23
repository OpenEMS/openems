package io.openems.edge.bridge.modbus.api.worker.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.temporal.ChronoUnit;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.DummyModbusComponent;
import io.openems.edge.bridge.modbus.api.worker.DummyReadTask;
import io.openems.edge.bridge.modbus.api.worker.DummyWriteTask;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.common.test.TimeLeapClock;

public class TasksSupplierImplTest {

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
	public void testFull() throws OpenemsException {
		var clock = new TimeLeapClock();
		var defectiveComponents = new DefectiveComponents(clock);
		var sut = new TasksSupplierImpl();

		var component = new DummyModbusComponent();
		var protocol = component.getModbusProtocol();
		protocol.addTasks(RT_H_1, RT_H_2, RT_L_1, RT_L_2, WT_1);
		sut.addProtocol(component.id(), protocol);

		// 1st Cycle
		var tasks = sut.getCycleTasks(defectiveComponents);
		assertEquals(4, tasks.reads().size() + tasks.writes().size());
		assertEquals(RT_L_1, tasks.reads().get(0));
		assertEquals(RT_H_1, tasks.reads().get(1));
		assertEquals(RT_H_2, tasks.reads().get(2));
		assertEquals(WT_1, tasks.writes().get(0));
		assertFalse(tasks.reads().contains(RT_L_2)); // -> not

		// 2nd Cycle
		tasks = sut.getCycleTasks(defectiveComponents);
		assertEquals(4, tasks.reads().size() + tasks.writes().size());
		assertEquals(RT_L_2, tasks.reads().get(0));
		assertEquals(RT_H_1, tasks.reads().get(1));
		assertEquals(RT_H_2, tasks.reads().get(2));
		assertEquals(WT_1, tasks.writes().get(0));
		assertFalse(tasks.reads().contains(RT_L_1)); // -> not

		// Add to defective
		defectiveComponents.add(component.id());

		// 3rd Cycle -> not yet due
		tasks = sut.getCycleTasks(defectiveComponents);
		assertEquals(0, tasks.reads().size() + tasks.writes().size());

		// 4th Cycle -> due: total one task
		clock.leap(30_001, ChronoUnit.MILLIS);
		tasks = sut.getCycleTasks(defectiveComponents);
		assertEquals(1, tasks.reads().size() + tasks.writes().size());

		// Remove from defective
		defectiveComponents.remove(component.id());

		// 5th Cycle -> back to normal
		tasks = sut.getCycleTasks(defectiveComponents);
		assertEquals(4, tasks.reads().size() + tasks.writes().size());

		// Finish
		sut.removeProtocol(component.id());
	}

	@Test
	public void testHighOnly() throws OpenemsException {
		var clock = new TimeLeapClock();
		var defectiveComponents = new DefectiveComponents(clock);
		var sut = new TasksSupplierImpl();

		var component = new DummyModbusComponent();
		var protocol = component.getModbusProtocol();
		protocol.addTasks(RT_H_1, RT_H_2, WT_1);
		sut.addProtocol(component.id(), protocol);

		var tasks = sut.getCycleTasks(defectiveComponents);
		assertEquals(3, tasks.reads().size() + tasks.writes().size());
		assertTrue(tasks.reads().contains(RT_H_1));
		assertTrue(tasks.reads().contains(RT_H_2));
		assertTrue(tasks.writes().contains(WT_1));
	}

}
