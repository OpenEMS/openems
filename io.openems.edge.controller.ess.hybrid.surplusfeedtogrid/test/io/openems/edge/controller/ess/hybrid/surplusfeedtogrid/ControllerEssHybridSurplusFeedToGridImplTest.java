package io.openems.edge.controller.ess.hybrid.surplusfeedtogrid;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyHybridEss;

public class ControllerEssHybridSurplusFeedToGridImplTest {

	private static final String CTRL_ID = "ctrl0";

	private static final ChannelAddress CTRL_SURPLUS_FEED_TO_GRID_IS_LIMITED = new ChannelAddress(CTRL_ID,
			"SurplusFeedToGridIsLimited");

	private static final String ESS_ID = "ess0";

	private static final ChannelAddress ESS_SET_ACTIVE_POWER_GREATER_OR_EQUALS = new ChannelAddress(ESS_ID,
			"SetActivePowerGreaterOrEquals");

	@Test
	public void test() throws Exception {
		final var ess = new DummyHybridEss(ESS_ID);
		final var test = new ControllerTest(new ControllerEssHybridSurplusFeedToGridImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", ess) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.build());

		ess.withSurplusPower(null);
		test.next(new TestCase() //
				.output(ESS_SET_ACTIVE_POWER_GREATER_OR_EQUALS, null));

		ess.withSurplusPower(5000);
		ess.withMaxApparentPower(10000);
		test.next(new TestCase() //
				.output(CTRL_SURPLUS_FEED_TO_GRID_IS_LIMITED, false) //
				.output(ESS_SET_ACTIVE_POWER_GREATER_OR_EQUALS, 5000));

		ess.withSurplusPower(5000);
		ess.withMaxApparentPower(2000);
		test.next(new TestCase() //
				.output(CTRL_SURPLUS_FEED_TO_GRID_IS_LIMITED, true) //
				.output(ESS_SET_ACTIVE_POWER_GREATER_OR_EQUALS, 2000));
	}
}