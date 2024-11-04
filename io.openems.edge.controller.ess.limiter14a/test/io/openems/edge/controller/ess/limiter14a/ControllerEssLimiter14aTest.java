package io.openems.edge.controller.ess.limiter14a;

import static io.openems.edge.common.sum.Sum.ChannelId.GRID_MODE;
import static io.openems.edge.controller.ess.limiter14a.ControllerEssLimiter14a.ChannelId.RESTRICTION_MODE;
import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_GREATER_OR_EQUALS;
import static io.openems.edge.io.test.DummyInputOutput.ChannelId.INPUT_OUTPUT0;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.io.test.DummyInputOutput;
import io.openems.edge.timedata.test.DummyTimedata;

public class ControllerEssLimiter14aTest {

	@Test
	public void testController() throws OpenemsException, Exception {
		new ControllerTest(new ControllerEssLimiter14aImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0")) //
				.addReference("sum", new DummySum()) //
				.addComponent(new DummyInputOutput("io0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0")//
						.setInputChannelAddress("io0/InputOutput0")//
						.build())
				.next(new TestCase() //
						// Since logic is reversed
						.input("io0", INPUT_OUTPUT0, false) //
						.input(GRID_MODE, GridMode.ON_GRID) //
						.output("ess0", SET_ACTIVE_POWER_GREATER_OR_EQUALS, -4200) //
						.output(RESTRICTION_MODE, RestrictionMode.ON)) //
				.next(new TestCase() //
						.input("io0", INPUT_OUTPUT0, null) //
						.output("ess0", SET_ACTIVE_POWER_GREATER_OR_EQUALS, null)) //
				.next(new TestCase() //
						.input("io0", INPUT_OUTPUT0, 1) //
						.input(GRID_MODE, GridMode.OFF_GRID) //
						.output(RESTRICTION_MODE, RestrictionMode.OFF)) //
				.next(new TestCase() //
						.input("io0", INPUT_OUTPUT0, false) //
						.input(GRID_MODE, GridMode.OFF_GRID) //
						.output("ess0", SET_ACTIVE_POWER_GREATER_OR_EQUALS, null)) //
				.deactivate();
	}

}
