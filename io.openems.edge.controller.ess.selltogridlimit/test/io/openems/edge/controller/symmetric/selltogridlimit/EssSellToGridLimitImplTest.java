package io.openems.edge.controller.symmetric.selltogridlimit;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class EssSellToGridLimitImplTest {

	private static final String CTRL_ID = "ctrl0";

	private static final String ESS_ID = "ess0";

	private static final ChannelAddress ESS_ACTIVE_POWER = new ChannelAddress(ESS_ID, "ActivePower");
	private static final ChannelAddress ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS = new ChannelAddress(ESS_ID,
			"SetActivePowerLessOrEquals");

	private static final String METER_ID = "meter00";

	private static final ChannelAddress METER_ACTIVE_POWER = new ChannelAddress(METER_ID, "ActivePower");

	@Test
	public void test() throws Exception {
		new ControllerTest(new EssSellToGridLimitImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)) //
				.addReference("meter", new DummyElectricityMeter(METER_ID)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setMeterId(METER_ID) //
						.setMaximumSellToGridPower(5000) //
						.build()) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, -5000) //
						.input(ESS_ACTIVE_POWER, 3000) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, null)) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, -6000) //
						.input(ESS_ACTIVE_POWER, 3000) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, 2000));
	}
}