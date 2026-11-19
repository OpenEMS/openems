package io.openems.edge.controller.ess.limittotalcharge;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_GREATER_OR_EQUALS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.test.DummyHybridEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyPower;

public class HybridEssTest {
	@ParameterizedTest
	@CsvSource(value = { "true, TARGET_AC, 1000, -1000, 0, 0", "true, TARGET_AC, 1000, -1000, 1000, 1000",
			"true, TARGET_AC, 1000, -1000, -1000, -1000", "true, TARGET_AC, null, null, 1000, 1000",
			"true, TARGET_DC, 1000, -1000, 1000, 3000", "true, TARGET_DC, 7000, 3000, 0, 4000",
			"true, TARGET_DC, 7000, 3000, -1000, 3000", "true, TARGET_DC, 1000, 1500, 0, 0",
			"true, TARGET_DC, null, -1000, 1000, 2000", "true, TARGET_DC, 1000, null, 1000, 2000",
			"true, TARGET_DC, null, null, 1000, 1000", "false, TARGET_AC, null, null, 1000, 1000",
			"false, TARGET_DC, null, null, 1000, 1000" }, nullValues = "null")
	void conversionMatchesDischargeSemantics(boolean hybrid, HybridEssMode mode, Integer ac, Integer dc, int power,
			int expected) {
		ManagedSymmetricEss ess = hybrid ? new DummyHybridEss("ess0").withActivePower(ac).withDcDischargePower(dc)
				: new DummyManagedSymmetricEss("ess0");
		assertEquals(expected, ControllerEssLimitTotalChargeImpl.getAcPower(ess, mode, power));
	}

	@Test
	public void nullModeIsRejectedLikeDischarge() {
		assertThrows(NullPointerException.class,
				() -> ControllerEssLimitTotalChargeImpl.getAcPower(new DummyHybridEss("hybrid"), null, 1000));
		assertThrows(NullPointerException.class,
				() -> ControllerEssLimitTotalChargeImpl.getAcPower(new DummyManagedSymmetricEss("ac"), null, 1000));
	}

	@Test
	public void missingDcPowerUsesZeroAtMaxSoc() throws Exception {
		var ess = new DummyHybridEss("ess0").withSoc(85).withActivePower(1000).withDcDischargePower(null);
		new ControllerTest(new ControllerEssLimitTotalChargeImpl()) //
				.addReference("componentManager", new DummyComponentManager(createDummyClock())) //
				.addComponent(ess) //
				.activate(MyConfig.create().setId("ctrl0").setEssId("ess0").setMaxSoc(85) //
						.setForceDischargeSoc(90).setHybridEssMode(HybridEssMode.TARGET_DC).build()) //
				.next(new TestCase().output("ess0", SET_ACTIVE_POWER_GREATER_OR_EQUALS, 1000)) //
				.deactivate();
	}

	@Test
	public void limitAndForcedPower() throws Exception {
		this.run(70, 1000, null, true);
		this.run(85, 1000, 2000, true);
		this.run(95, 1000, 3000, true);
		this.run(85, 1000, 0, false);
		this.run(95, 0, 3600, true);
	}

	private void run(int soc, int forcePower, Integer expected, boolean hybrid) throws Exception {
		ManagedSymmetricEss ess = hybrid
				? new DummyHybridEss("ess0").withActivePower(1000).withDcDischargePower(-1000).withSoc(soc)
				: new DummyManagedSymmetricEss("ess0").withSoc(soc);
		((DummyPower) ess.getPower()).setMaxApparentPower(10000);
		new ControllerTest(new ControllerEssLimitTotalChargeImpl())
				.addReference("componentManager", new DummyComponentManager(createDummyClock())).addComponent(ess)
				.activate(MyConfig.create().setId("ctrl0").setEssId("ess0").setMaxSoc(85).setForceDischargeSoc(90)
						.setForceDischargePower(forcePower).setHybridEssMode(HybridEssMode.TARGET_DC).build())
				.next(new TestCase().output("ess0", SET_ACTIVE_POWER_GREATER_OR_EQUALS, expected));
	}
}
