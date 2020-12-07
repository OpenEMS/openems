package io.openems.edge.controller.ess.fixactivepower;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedAsymmetricEss;

public class EssFixActivePowerImplTest {

	private static final String CTRL_ID = "ctrl0";

	private static final String ESS_ID = "ess0";

	private static final ChannelAddress ESS_SET_ACTIVE_POWER_EQUALS = new ChannelAddress(ESS_ID,
			"SetActivePowerEquals");

	@Test
	public void testOn() throws OpenemsException, Exception {
		new ControllerTest(new EssFixActivePowerImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedAsymmetricEss(ESS_ID)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setMode(Mode.MANUAL_ON) //
						.setPower(1234) //
						.build()) //
				.next(new TestCase() //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 1234));
	}

	@Test
	public void testOff() throws OpenemsException, Exception {
		new ControllerTest(new EssFixActivePowerImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedAsymmetricEss(ESS_ID)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setMode(Mode.MANUAL_OFF) //
						.setPower(1234) //
						.build()) //
				.next(new TestCase() //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, null));
	}

}
