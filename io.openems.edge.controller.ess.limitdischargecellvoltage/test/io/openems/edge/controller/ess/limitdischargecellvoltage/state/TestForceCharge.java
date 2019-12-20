package io.openems.edge.controller.ess.limitdischargecellvoltage.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.controller.ess.limitdischargecellvoltage.Config;
import io.openems.edge.controller.ess.limitdischargecellvoltage.IState;
import io.openems.edge.controller.ess.limitdischargecellvoltage.State;
import io.openems.edge.controller.ess.limitdischargecellvoltage.helper.CreateTestConfig;
import io.openems.edge.controller.ess.limitdischargecellvoltage.helper.DummyComponentManager;
import io.openems.edge.controller.ess.limitdischargecellvoltage.helper.DummyEss;

public class TestForceCharge {

	private IState sut;
	private static DummyComponentManager componentManager;
	private static Config config;
	private DummyEss ess;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		config = CreateTestConfig.create();
		componentManager = new DummyComponentManager();
	}

	@Before
	public void setUp() throws Exception {
		// Always create ess newly to have an ess in "normal" situation that does
		// nothing
		componentManager.initEss();
		ess = componentManager.getComponent(CreateTestConfig.ESS_ID);
		sut = new ForceCharge(ess, config.chargePowerPercent(), config.chargingTime());
	}

	@Test
	public final void testGetState() {
		assertEquals(sut.getState(), State.FORCE_CHARGE);
	}

	@Test
	public final void testGetNextStateNoChanges() {
		State next = sut.getNextState();
		assertEquals(State.FORCE_CHARGE, next);
	}

	@Test
	public final void testGetNextStateCheckAfterWaitingPeriod() {
		State next = sut.getNextState();
		assertEquals(State.FORCE_CHARGE, next);

		// Wait the defined time, then the next state should always be CHECK
		try {
			Thread.sleep(TestForceCharge.config.chargingTime() * 1000 + 500);
		} catch (InterruptedException e) {
			fail(e.getMessage());
		}

		next = sut.getNextState();
		assertEquals(State.CHECK, next);
	}

	@Test
	public final void testGetNextStateObjectUndefined() {
		ess.setSocToUndefined();
		assertEquals(State.UNDEFINED, sut.getNextState());
	}

	@Test
	public final void testAct() {
		// After executing the act() function the channel SetActivePowerLessOrEquals
		// should have a value in the nextWriteValue
		try {
			sut.act();
		} catch (OpenemsNamedException e) {
			fail(e.getMessage());
		}

		int actual = ess.getSetActivePowerLessOrEquals().getNextWriteValue().get();

		// According to the dummy config 20% of -10000 (neg. values for charge are
		// expected)
		int expected = -1 * DummyEss.MAXIMUM_POWER * config.chargePowerPercent() / 100;
		assertEquals(expected, actual);

		actual = ess.getCurrentActivePower();

		assertEquals(expected, actual);
	}
}
