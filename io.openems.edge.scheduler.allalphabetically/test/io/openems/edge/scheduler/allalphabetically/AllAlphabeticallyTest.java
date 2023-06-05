package io.openems.edge.scheduler.allalphabetically;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.DummyController;
import io.openems.edge.scheduler.api.Scheduler;

public class AllAlphabeticallyTest {

	private static final String SCHEDULER_ID = "scheduler0";

	private static final String CTRL0_ID = "ctrl0";
	private static final String CTRL1_ID = "ctrl1";
	private static final String CTRL2_ID = "ctrl2";
	private static final String CTRL3_ID = "ctrl3";
	private static final String CTRL4_ID = "ctrl4";

	@Test
	public void test() throws Exception {
		final SchedulerAllAlphabetically sut = new SchedulerAllAlphabeticallyImpl();
		new ComponentTest(sut) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummyController(CTRL0_ID)) //
				.addComponent(new DummyController(CTRL1_ID)) //
				.addComponent(new DummyController(CTRL2_ID)) //
				.addComponent(new DummyController(CTRL3_ID)) //
				.addComponent(new DummyController(CTRL4_ID)) //
				.activate(MyConfig.create() //
						.setId(SCHEDULER_ID) //
						.setControllersIds(CTRL2_ID, CTRL1_ID) //
						.build())
				.next(new TestCase());

		assertEquals(//
				Arrays.asList(CTRL2_ID, CTRL1_ID, CTRL0_ID, CTRL3_ID, CTRL4_ID), //
				getControllerIds(sut));
	}

	private static List<String> getControllerIds(Scheduler scheduler) throws OpenemsNamedException {
		return scheduler.getControllers().stream() //
				.toList();
	}
}
