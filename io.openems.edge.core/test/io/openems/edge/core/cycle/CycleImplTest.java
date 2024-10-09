package io.openems.edge.core.cycle;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.openems.common.worker.AbstractWorker;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.DummyEventAdmin;
import io.openems.edge.controller.test.DummyController;
import io.openems.edge.scheduler.api.Scheduler;

public class CycleImplTest {

	private final class SchedulerImplementation extends AbstractOpenemsComponent implements Scheduler {

		private LinkedHashSet<String> controllers;

		protected SchedulerImplementation(LinkedHashSet<String> controllers) {
			super(OpenemsComponent.ChannelId.values(), Scheduler.ChannelId.values());
			this.controllers = controllers;
		}

		@Override
		public LinkedHashSet<String> getControllers() {
			return controllers;
		}

		@Override
		public String id() {
			return "Bla";
		}
	}

	@Test
	public void testChangeCycleTime() throws Exception {

		var dummyController = new DummyController("BlaDummyController");
		var eventAdmin = new DummyEventAdmin();
		var controllerExecutionCount = new AtomicInteger();
		var newValueForCycle = new AtomicInteger();
		dummyController.setRunCallback(() -> {
			controllerExecutionCount.set(newValueForCycle.get());
		});

		var scheduler = new SchedulerImplementation(
				new LinkedHashSet<String>(Collections.singleton(dummyController.id())));

		var sut = new CycleImpl();
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("sumComponent", new DummySum()) //
				.addReference("addScheduler", scheduler) //
				.addReference("eventAdmin", eventAdmin) //
				.addComponent(dummyController) //
				.activate(MyConfig.create().cycleTime(AbstractWorker.DO_NOT_WAIT).build()) //
				.next(new TestCase("Run cycle with 0 cycle time") //
						.onAfterControllersCallbacks(() -> {
							System.out.println("Test 1");
							newValueForCycle.getAndIncrement();
							Thread.sleep(10); // required, because cycle does not have an external clock stopwatch
							assertEquals(newValueForCycle.get(), controllerExecutionCount.get()); //
							System.out.println("Done");
						})) //
				.modify(MyConfig.create().cycleTime(AbstractWorker.ALWAYS_WAIT_FOR_TRIGGER_NEXT_RUN).build()) //
				.next(new TestCase("No new cycle executes, as next run must be triggered manually") //
						.onAfterControllersCallbacks(() -> {
							System.out.println("Test 2");
							newValueForCycle.getAndIncrement();
							Thread.sleep(10); // required, because cycle does not have an external clock stopwatch
							assertEquals(newValueForCycle.get() - 1, controllerExecutionCount.get()); //
							System.out.println("Done");
						})) //
				.next(new TestCase("New cycle execution manually triggered") //
						.onAfterControllersCallbacks(() -> {
							System.out.println("Test 3");
							sut.triggerNextCycle();
							Thread.sleep(10); // required, because cycle does not have an external clock stopwatch
							assertEquals(newValueForCycle.get(), controllerExecutionCount.get());
							System.out.println("Done");
						}))
				.modify(MyConfig.create().cycleTime(20).build()) //
				.next(new TestCase("Set wait time again, but do not wait") //
						.onAfterControllersCallbacks(() -> {
							System.out.println("Test 4");
							newValueForCycle.getAndIncrement();
							Thread.sleep(10); // required, because cycle does not have an external clock stopwatch
							assertEquals(newValueForCycle.get() - 1, controllerExecutionCount.get());
							System.out.println("Done");
						}))
				.next(new TestCase("Wait until cycle time passed") //
						.onAfterControllersCallbacks(() -> {
							System.out.println("Test 5");
							Thread.sleep(20); // required, because cycle does not have an external clock stopwatch
							assertEquals(newValueForCycle.get(), controllerExecutionCount.get());
							System.out.println("Done");
						})); //; //

	}

}
