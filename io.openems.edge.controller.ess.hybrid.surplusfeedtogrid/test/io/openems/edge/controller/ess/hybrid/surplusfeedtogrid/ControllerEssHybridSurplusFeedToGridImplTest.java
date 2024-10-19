package io.openems.edge.controller.ess.hybrid.surplusfeedtogrid;

import static io.openems.edge.controller.ess.hybrid.surplusfeedtogrid.ControllerEssHybridSurplusFeedToGrid.ChannelId.SURPLUS_FEED_TO_GRID_IS_LIMITED;
import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_GREATER_OR_EQUALS;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyHybridEss;

public class ControllerEssHybridSurplusFeedToGridImplTest {

	@Test
	public void test() throws Exception {
		final var ess = new DummyHybridEss("ess0");
		final var test = new ControllerTest(new ControllerEssHybridSurplusFeedToGridImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", ess) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.build());

		ess.withSurplusPower(null);
		test.next(new TestCase() //
				.output("ess0", SET_ACTIVE_POWER_GREATER_OR_EQUALS, null));

		ess.withSurplusPower(5000);
		ess.withMaxApparentPower(10000);
		test.next(new TestCase() //
				.output(SURPLUS_FEED_TO_GRID_IS_LIMITED, false) //
				.output("ess0", SET_ACTIVE_POWER_GREATER_OR_EQUALS, 5000));

		ess.withSurplusPower(5000);
		ess.withMaxApparentPower(2000);
		test.next(new TestCase() //
				.output(SURPLUS_FEED_TO_GRID_IS_LIMITED, true) //
				.output("ess0", SET_ACTIVE_POWER_GREATER_OR_EQUALS, 2000)) //
		
				.deactivate();
	}
}