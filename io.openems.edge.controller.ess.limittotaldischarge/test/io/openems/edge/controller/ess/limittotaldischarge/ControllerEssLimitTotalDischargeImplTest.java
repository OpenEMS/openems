package io.openems.edge.controller.ess.limittotaldischarge;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.controller.ess.limittotaldischarge.ControllerEssLimitTotalDischarge.ChannelId.AWAITING_HYSTERESIS;
import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_LESS_OR_EQUALS;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.SOC;
import static org.junit.Assert.assertEquals;

import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyHybridEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

public class ControllerEssLimitTotalDischargeImplTest {

	@Test
	public void test() throws Exception {
		final var clock = createDummyClock();
		new ControllerTest(new ControllerEssLimitTotalDischargeImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addComponent(new DummyManagedSymmetricEss("ess0") //
						.withSoc(20) //
						.withCapacity(9000)) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setHybridEssMode(HybridEssMode.TARGET_DC) //
						.setMinSoc(15) //
						.setForceChargeSoc(10) //
						.setForceChargePower(1000) //
						.build())
				.next(new TestCase() //
						.input("ess0", SOC, 20) //
						.output("ess0", SET_ACTIVE_POWER_LESS_OR_EQUALS, null)//
						.output(AWAITING_HYSTERESIS, false)) //
				.next(new TestCase() //
						.input("ess0", SOC, 15) //
						.output("ess0", SET_ACTIVE_POWER_LESS_OR_EQUALS, 0) //
						.output(AWAITING_HYSTERESIS, false)) //
				.next(new TestCase() //
						.input("ess0", SOC, 16) //
						.output("ess0", SET_ACTIVE_POWER_LESS_OR_EQUALS, 0) //
						.output(AWAITING_HYSTERESIS, true)) //
				.next(new TestCase() //
						.timeleap(clock, 6, ChronoUnit.MINUTES) //
						.input("ess0", SOC, 16) //
						.output("ess0", SET_ACTIVE_POWER_LESS_OR_EQUALS, null) //
						.output(AWAITING_HYSTERESIS, false)) //
				.deactivate();
	}

	@Test
	public void testGetAcPower() throws OpenemsException, Exception {
		var hybridEss = new DummyHybridEss("ess0") //
				.withActivePower(7000) //
				.withDcDischargePower(3000); //

		assertEquals(Integer.valueOf(0), //
				ControllerEssLimitTotalDischargeImpl.getAcPower(hybridEss, HybridEssMode.TARGET_AC, 0));

		assertEquals(Integer.valueOf(4000), //
				ControllerEssLimitTotalDischargeImpl.getAcPower(hybridEss, HybridEssMode.TARGET_DC, 0));

		assertEquals(Integer.valueOf(-1000), //
				ControllerEssLimitTotalDischargeImpl.getAcPower(hybridEss, HybridEssMode.TARGET_AC, -1000));

		assertEquals(Integer.valueOf(3000), //
				ControllerEssLimitTotalDischargeImpl.getAcPower(hybridEss, HybridEssMode.TARGET_DC, -1000));
	}

}
