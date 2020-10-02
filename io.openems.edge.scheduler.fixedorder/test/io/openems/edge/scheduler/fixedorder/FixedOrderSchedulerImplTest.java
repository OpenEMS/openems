package io.openems.edge.scheduler.fixedorder;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.controller.test.DummyController;
import io.openems.edge.scheduler.api.Scheduler;

public class FixedOrderSchedulerImplTest {

	private static final String SCHEDULER_ID = "scheduler0";

	private static final String CTRL0_ID = "ctrl0";
	private static final String CTRL1_ID = "ctrl1";
	private static final String CTRL2_ID = "ctrl2";
	private static final String CTRL3_ID = "ctrl3";
	private static final String CTRL4_ID = "ctrl4";

	@Test
	public void test() throws Exception {
		final TimeLeapClock clock = new TimeLeapClock(Instant.parse("2020-01-01T00:00:00.00Z"), ZoneOffset.UTC);
		final FixedOrderScheduler sut = new FixedOrderSchedulerImpl();
		ComponentTest test = new ComponentTest(sut) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addComponent(new DummyController(CTRL0_ID)) //
				.addComponent(new DummyController(CTRL1_ID)) //
				.addComponent(new DummyController(CTRL2_ID)) //
				.addComponent(new DummyController(CTRL3_ID)) //
				.addComponent(new DummyController(CTRL4_ID)) //
				.activate(MyConfig.create() //
						.setId(SCHEDULER_ID) //
						.setControllersIds(CTRL3_ID, CTRL1_ID) //
						.build()); //

		test.next(new TestCase()); //
		assertEquals(//
				Arrays.asList(CTRL3_ID, CTRL1_ID), //
				getControllerIds(sut));

	}

	private static List<String> getControllerIds(Scheduler scheduler) throws OpenemsNamedException {
		return scheduler.getControllers().stream() //
				.map(c -> c.id()) //
				.collect(Collectors.toList());
	}

}
