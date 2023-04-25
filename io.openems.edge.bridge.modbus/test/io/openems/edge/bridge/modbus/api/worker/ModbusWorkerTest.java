package io.openems.edge.bridge.modbus.api.worker;

import static org.junit.Assert.assertArrayEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.common.taskmanager.Priority;

public class ModbusWorkerTest {

	public static final int UNIT_ID = 1;

	private static final String ON_EXECUTE_WRITE = "ON_EXECUTE_WRITE";
	private static final String ON_BEFORE_PROCESS_IMAGE = "ON_BEFORE_PROCESS_IMAGE";

	public static DummyReadTask RT_H_1;
	public static DummyReadTask RT_H_2;
	public static DummyReadTask RT_L_1;
	public static DummyReadTask RT_L_2;
	public static DummyWriteTask WT_1;

	private static class WorkerTest {

		private final DummyModbusBridge bridge;
		private final ModbusWorker sut;
		private final List<Object> executionOrder = new ArrayList<>();
		private final List<AbstractDummyComponent> components = new ArrayList<>();

		public WorkerTest() {
			this.bridge = new DummyModbusBridge("modbus0");
			this.sut = this.bridge.getWorker();
		}

		public void dispose() {
			this.components.stream() //
					.flatMap(c -> Stream.of(c.getTasks())) //
					.filter(t -> t instanceof AbstractDummyTask) //
					.map(t -> (AbstractDummyTask) t) //
					.forEach(t -> t.onExecute(null));
		}

		public AbstractDummyComponent addComponent(String componentId, AbstractDummyTask... tasks)
				throws OpenemsException {
			for (var task : tasks) {
				task.onExecute(() -> {
					this.executionOrder.add(task);
				});
			}
			var result = AbstractDummyComponent.of(componentId, this.bridge, tasks);
			this.components.add(result);
			return result;
		}

		public void onExecuteWrite() {
			this.executionOrder.add(ON_EXECUTE_WRITE);
//			this.sut.onExecuteWrite();
		}

		public void onBeforeProcessImage() {
			this.executionOrder.add(ON_BEFORE_PROCESS_IMAGE);
			this.sut.onBeforeProcessImage();
		}

		public void assertExecutionOrder(Object... expecteds) {
			assertArrayEquals(expecteds, this.executionOrder.toArray(Object[]::new));
			this.dispose();
		}
	}

	@Before
	public void before() {
		RT_H_1 = new DummyReadTask("RT_H_1", 49, Priority.HIGH);
		RT_H_2 = new DummyReadTask("RT_H_2", 70, Priority.HIGH);
		RT_L_1 = new DummyReadTask("RT_L_1", 20, Priority.LOW);
		RT_L_2 = new DummyReadTask("RT_L_2", 30, Priority.LOW);
		WT_1 = new DummyWriteTask("WT_1", 90);
	}

	@Test
	public void testX() throws OpenemsException, InterruptedException {
		var logVerbosity = new AtomicReference<LogVerbosity>(LogVerbosity.DEV_REFACTORING);
		var defectiveComponents = new DefectiveComponents();
		var waitHandler = new WaitHandler(logVerbosity);
		var sut = new ModbusTasksManager(defectiveComponents, waitHandler, logVerbosity);

		var bridge = new DummyModbusBridge("modbus0");
		var foo = AbstractDummyComponent.of("foo", bridge, new AbstractDummyTask[0]);
		var protocol0 = new ModbusProtocol(foo, RT_L_1, RT_L_2, RT_H_1, RT_H_2, WT_1);
		sut.addProtocol(foo.id(), protocol0);

		sut.onBeforeProcessImage();

		// TODO: less boiler-plate code; test ModbusTasksManager
		
//		var worker = new ModbusWorker(
//				// Execute Task
//				task -> {
//					System.out.println("Execute: " + task);
//					return 1;
//				},
//				// Invalidate ModbusElements
//				elements -> System.out.println("Invalidate: " + elements),
//				// Set ChannelId.CYCLE_TIME_IS_TOO_SHORT
//				state -> System.out.println("State: " + state),
//				// Log Warning
//				(logger, message) -> System.err.println(message),
//				// LogVerbosity
//				new AtomicReference<LogVerbosity>(LogVerbosity.DEV_REFACTORING) //
//		);
	}

	// TODO @Ignore
	@Test
	public void test() throws OpenemsException, InterruptedException {
		var test = new WorkerTest();
		test.addComponent("cmp0", RT_H_1, RT_L_1, RT_L_2, RT_H_2, WT_1);

		test.onExecuteWrite();
		Thread.sleep(500);
		test.onBeforeProcessImage();
		Thread.sleep(500);
		test.onExecuteWrite();
		Thread.sleep(500);
		test.onBeforeProcessImage();
		Thread.sleep(500);
		test.onExecuteWrite();
		Thread.sleep(500);
		test.onBeforeProcessImage();
		Thread.sleep(500);
		test.onExecuteWrite();
		Thread.sleep(500);
		test.onBeforeProcessImage();

		test.assertExecutionOrder(//
				ON_EXECUTE_WRITE, //
				WT_1, //
				ON_BEFORE_PROCESS_IMAGE, //
				RT_L_1, RT_H_1, RT_H_2, //
				ON_EXECUTE_WRITE, //
				WT_1, //
				ON_BEFORE_PROCESS_IMAGE, //
				RT_L_2, RT_H_1, RT_H_2, //
				ON_EXECUTE_WRITE, //
				WT_1, //
				ON_BEFORE_PROCESS_IMAGE, //
				RT_L_1, RT_H_1, RT_H_2, //
				ON_EXECUTE_WRITE, //
				WT_1, //
				ON_BEFORE_PROCESS_IMAGE);
	}

	// TODO @Ignore
	@Test
	public void testError() throws OpenemsException, InterruptedException {
		var test = new WorkerTest();
		test.addComponent("cmp0", RT_H_1, RT_L_1, RT_L_2, WT_1);
		test.addComponent("cmp1", RT_H_2);

		test.onExecuteWrite();
		Thread.sleep(500);
		test.onBeforeProcessImage();

		RT_H_2.setError(true);

		Thread.sleep(500);
		test.onExecuteWrite();
		Thread.sleep(500);
		test.onBeforeProcessImage();
		Thread.sleep(500);
		test.onExecuteWrite();
		Thread.sleep(500);
		test.onBeforeProcessImage();
		Thread.sleep(500);
		test.onExecuteWrite();
		Thread.sleep(500);
		test.onBeforeProcessImage();

		test.assertExecutionOrder(//
				ON_EXECUTE_WRITE, //
				WT_1, //
				ON_BEFORE_PROCESS_IMAGE, //
				RT_L_1, RT_H_1, RT_H_2, //
				ON_EXECUTE_WRITE, //
				ON_BEFORE_PROCESS_IMAGE, //
				WT_1, RT_L_2, RT_H_1, RT_H_2, //
				ON_EXECUTE_WRITE, //
				ON_BEFORE_PROCESS_IMAGE, //
				WT_1, //
				ON_EXECUTE_WRITE, //
				WT_1, RT_L_1, RT_H_1, //
				ON_BEFORE_PROCESS_IMAGE);
	}

}
