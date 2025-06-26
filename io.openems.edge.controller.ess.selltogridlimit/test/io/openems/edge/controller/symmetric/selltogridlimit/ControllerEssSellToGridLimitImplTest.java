package io.openems.edge.controller.symmetric.selltogridlimit;

import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_LESS_OR_EQUALS;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class ControllerEssSellToGridLimitImplTest {

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerEssSellToGridLimitImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0")) //
				.addReference("meter", new DummyElectricityMeter("meter0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setMeterId("meter0") //
						.setMaximumSellToGridPower(5000) //
						.build()) //
				.next(new TestCase() //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, -5000) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 3000) //
						.output("ess0", SET_ACTIVE_POWER_LESS_OR_EQUALS, null)) //
				.next(new TestCase() //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, -6000) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 3000) //
						.output("ess0", SET_ACTIVE_POWER_LESS_OR_EQUALS, 2000)) //
				.deactivate();
	}
}