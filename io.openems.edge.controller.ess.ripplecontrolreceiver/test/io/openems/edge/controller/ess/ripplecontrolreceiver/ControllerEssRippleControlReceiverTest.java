package io.openems.edge.controller.ess.ripplecontrolreceiver;

import static io.openems.edge.common.sum.Sum.ChannelId.GRID_MODE;
import static io.openems.edge.controller.ess.ripplecontrolreceiver.ControllerEssRippleControlReceiver.ChannelId.RESTRICTION_MODE;
import static io.openems.edge.controller.ess.ripplecontrolreceiver.EssRestrictionLevel.NO_RESTRICTION;
import static io.openems.edge.controller.ess.ripplecontrolreceiver.EssRestrictionLevel.SIXTY_PERCENT;
import static io.openems.edge.controller.ess.ripplecontrolreceiver.EssRestrictionLevel.THIRTY_PERCENT;
import static io.openems.edge.controller.ess.ripplecontrolreceiver.EssRestrictionLevel.ZERO_PERCENT;
import static io.openems.edge.io.test.DummyInputOutput.ChannelId.INPUT_OUTPUT0;
import static io.openems.edge.io.test.DummyInputOutput.ChannelId.INPUT_OUTPUT1;
import static io.openems.edge.io.test.DummyInputOutput.ChannelId.INPUT_OUTPUT2;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.io.test.DummyInputOutput;

public class ControllerEssRippleControlReceiverTest {

	@Test
	public void testController() throws OpenemsException, Exception {
		var sut = new ControllerEssRippleControlReceiverImpl();
		new ControllerTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("sum", new DummySum()) //
				.addComponent(new DummyInputOutput("io0")) //
				.activate(MyConfig.create() //
						.setId("ctrlEssRippleControlReceiver0") //
						.setInputChannelAddress1("io0/InputOutput0")//
						.setInputChannelAddress2("io0/InputOutput1")//
						.setInputChannelAddress3("io0/InputOutput2")//
						.build())
				.next(new TestCase() //
						.input(GRID_MODE, GridMode.ON_GRID) //
						.input("io0", INPUT_OUTPUT0, true) //
						.input("io0", INPUT_OUTPUT1, true) //
						.input("io0", INPUT_OUTPUT2, true) //
						.output(RESTRICTION_MODE, NO_RESTRICTION)) //
				.next(new TestCase() //
						.input("io0", INPUT_OUTPUT0, false) //
						.input("io0", INPUT_OUTPUT1, true) //
						.input("io0", INPUT_OUTPUT2, true) //
						.output(RESTRICTION_MODE, ZERO_PERCENT)) //
				.next(new TestCase() //
						.input("io0", INPUT_OUTPUT0, true) //
						.input("io0", INPUT_OUTPUT1, false) //
						.input("io0", INPUT_OUTPUT2, true) //
						.output(RESTRICTION_MODE, THIRTY_PERCENT)) //
				.next(new TestCase() //
						.input("io0", INPUT_OUTPUT0, true) //
						.input("io0", INPUT_OUTPUT1, true) //
						.input("io0", INPUT_OUTPUT2, false) //
						.output(RESTRICTION_MODE, SIXTY_PERCENT)) //
				.next(new TestCase() //
						// "strongest" restriction should be applied
						.input("io0", INPUT_OUTPUT0, false) //
						.input("io0", INPUT_OUTPUT1, false) //
						.input("io0", INPUT_OUTPUT2, false) //
						.output(RESTRICTION_MODE, ZERO_PERCENT)) //
				.deactivate();

	}
}
