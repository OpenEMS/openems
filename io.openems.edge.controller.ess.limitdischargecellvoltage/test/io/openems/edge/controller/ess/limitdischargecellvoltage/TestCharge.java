package io.openems.edge.controller.ess.limitdischargecellvoltage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.controller.ess.limitdischargecellvoltage.helper.CreateTestConfig;
import io.openems.edge.controller.ess.limitdischargecellvoltage.helper.DummyComponentManager;
import io.openems.edge.controller.ess.limitdischargecellvoltage.helper.DummyEss;
import io.openems.edge.controller.ess.limitdischargecellvoltage.state.ForceCharge;
import io.openems.edge.controller.ess.limitdischargecellvoltage.state.Normal;
import io.openems.edge.controller.ess.limitdischargecellvoltage.state.Undefined;

public class TestCharge {

	private IState sut;
	private DummyComponentManager componentManager;
	private static Config config;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		config = CreateTestConfig.create();
	}

	@Before
	public void setUp() throws Exception {
		componentManager = new DummyComponentManager();
		sut = new ForceCharge(componentManager, config);
	}

	@Test
	public final void testGetState() {
		assertEquals(sut.getState(), State.CHARGE);
	}

	@Test
	public final void testGetNextStateObjectNoChanges() {
		IState next = sut.getNextStateObject();
		assertTrue(next instanceof ForceCharge);
		assertEquals(State.CHARGE, next.getState());
	}

	@Test
	public final void testGetNextStateObjectNormalAfterWaitingPeriod() {
		IState next = sut.getNextStateObject();
		assertTrue(next instanceof ForceCharge);
		assertEquals(State.CHARGE, next.getState());

		// Wait the defined time, then the next state should always be normal
		try {
			Thread.sleep(TestCharge.config.chargingTime() * 1000 + 500);
		} catch (InterruptedException e) {
			fail(e.getMessage());
		}

		next = sut.getNextStateObject();
		assertTrue(next instanceof Normal);
		assertEquals(State.NORMAL, next.getState());
	}

	@Test
	public final void testGetNextStateObjectUndefined() {
		IState next = sut.getNextStateObject();
		assertTrue(next instanceof ForceCharge);
		assertEquals(State.CHARGE, next.getState());

		componentManager.destroyEss();

		next = sut.getNextStateObject();
		assertTrue(next instanceof Undefined);
		assertEquals(State.UNDEFINED, next.getState());
	}

	@Test
	public final void testAct() {
		DummyEss ess = null;
		try {
			// After executing the act() function the channel SetActivePowerLessOrEquals
			// should have a value in the nextWriteValue
			sut.act();
			try {
				ess = componentManager.getComponent(CreateTestConfig.ESS_ID);
			} catch (OpenemsNamedException e) {
				fail();
			}

		} catch (Exception e) {
			fail();
		}
		if (ess == null) {
			fail("Ess is null");
		}

		int actual = ess.getSetActivePowerLessOrEquals().getNextWriteValue().get();

		// According to the dummy config 20% of -10000 (neg. values for charge are
		// expected)
		int expected = -2000;
		assertEquals(expected, actual);
	}
}
