package io.openems.edge.controller.ess.limiter14a;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.timedata.test.DummyTimedata;
import io.openems.edge.io.test.DummyInputOutput;

public class ControllerEssLimiter14aTest {

	private static final String ESS_ID = "ess0";
	private static final String CTRL_ID = "ctrlEssLimiter14a0";
	
	private static final ChannelAddress RESTRICTION_MODE = new ChannelAddress(CTRL_ID, "RestrictionMode");
	private static final ChannelAddress GPIO = new ChannelAddress("io0", "InputOutput0");
	private static final ChannelAddress LIMITATION = new ChannelAddress(ESS_ID, "SetActivePowerGreaterOrEquals");
	private static final ChannelAddress GRID_MODE = new ChannelAddress("_sum", "GridMode");

	@Test
	public void testController() throws OpenemsException, Exception {
		new ControllerTest(new ControllerEssLimiter14aImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)) //
				.addReference("sum", new DummySum()) //
				.addComponent(new DummyInputOutput("io0")) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID)//
						.setInputChannelAddress("io0/InputOutput0")//
						.build())
				.next(new TestCase() //
						// Since logic is reversed
						.input(GPIO, false) //
						.input(GRID_MODE, GridMode.ON_GRID)
						.output(LIMITATION, -4200)
						.output(RESTRICTION_MODE, RestrictionMode.ON)) //
				.next(new TestCase() //
						.input(GPIO, null) // 
						.output(LIMITATION, null)) //
				.next(new TestCase() //
						.input(GPIO, 1) //
						.input(GRID_MODE, GridMode.OFF_GRID) //
						.output(RESTRICTION_MODE, RestrictionMode.OFF)) //
				.next(new TestCase() //
						.input(GPIO, false) //
						.input(GRID_MODE, GridMode.OFF_GRID) //
						.output(LIMITATION, null)) //
		;
	}

}
