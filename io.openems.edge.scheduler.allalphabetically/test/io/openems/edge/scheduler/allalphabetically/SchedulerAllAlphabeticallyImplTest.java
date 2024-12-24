package io.openems.edge.scheduler.allalphabetically;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.DummyController;
import io.openems.edge.scheduler.api.Scheduler;

public class SchedulerAllAlphabeticallyImplTest {

	@Test
	public void testWithFixedPriorities() throws Exception {
		final SchedulerAllAlphabetically sut = new SchedulerAllAlphabeticallyImpl();
		new ComponentTest(sut) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummyController("ctrl0")) //
				.addComponent(new DummyController("ctrl1")) //
				.addComponent(new DummyController("ctrl2")) //
				.addComponent(new DummyController("ctrl3")) //
				.addComponent(new DummyController("ctrl4")) //
				.activate(MyConfig.create() //
						.setId("scheduler0") //
						.setControllersIds("ctrl2", "ctrl1", "") //
						.build())
				.next(new TestCase()) //
				.deactivate();

		assertEquals(//
				Arrays.asList("ctrl2", "ctrl1", "ctrl0", "ctrl3", "ctrl4"), //
				getControllerIds(sut));
	}

	@Test
	public void testOnlyAlphabeticalOrdering() throws Exception {
		final var controllerIds = new ArrayList<>(List.of(//
				"ctrlController1", //
				"a", //
				"aa", //
				"aA", //
				"ab", //
				"aB", //
				"A", //
				"0", //
				"1", //
				"0controller", //
				"0Controller", //
				"bla", //
				"controller0", //
				"controller1", //
				"dontroller0", //
				"dontroller1", //
				"d0", //
				"D0", //
				"Z", //
				"z"));
		final var sut = new SchedulerAllAlphabeticallyImpl();
		final var test = new ComponentTest(sut); //
		controllerIds.forEach(controllerId -> test.addComponent(new DummyController(controllerId)));
		test //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId("scheduler0") //
						.setControllersIds() //
						.build()) //
				.next(new TestCase());

		Collections.sort(controllerIds);

		assertEquals(controllerIds, getControllerIds(sut));
	}

	private static List<String> getControllerIds(Scheduler scheduler) throws OpenemsNamedException {
		return scheduler.getControllers().stream() //
				.toList();
	}
}
