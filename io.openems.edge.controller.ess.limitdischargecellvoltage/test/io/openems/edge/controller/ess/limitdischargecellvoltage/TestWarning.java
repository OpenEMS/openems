package io.openems.edge.controller.ess.limitdischargecellvoltage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.openems.edge.controller.ess.limitdischargecellvoltage.helper.CreateTestConfig;
import io.openems.edge.controller.ess.limitdischargecellvoltage.helper.DummyComponentManager;
import io.openems.edge.controller.ess.limitdischargecellvoltage.helper.DummyEss;
import io.openems.edge.controller.ess.limitdischargecellvoltage.state.ForceCharge;
import io.openems.edge.controller.ess.limitdischargecellvoltage.state.Limit;
import io.openems.edge.controller.ess.limitdischargecellvoltage.state.Normal;
import io.openems.edge.controller.ess.limitdischargecellvoltage.state.Undefined;
import io.openems.edge.controller.ess.limitdischargecellvoltage.state.Limit;

public class TestWarning {

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
		sut = new Limit(componentManager, config);
	}

	@Test
	public final void testGetState() {
		assertEquals(sut.getState(), State.WARNING);
	}

	@Test
	public final void testGetNextStateObjectNoChanges() {
		// Minimal voltage must be between warning and critical
		DummyEss ess = null;
		try {
			ess = componentManager.getComponent(CreateTestConfig.ESS_ID);
		} catch (Exception e) {
			fail();
		}
		if (ess == null) {
			fail("Ess was null");
		}
		ess.setMinimalCellVoltage(TestWarning.config.warningCellVoltage() - 1);
		IState next = sut.getNextStateObject();
		assertTrue(next instanceof Limit);
		assertEquals(State.WARNING, next.getState());
	}

	@Test
	public final void testGetNextStateObjectAfterWaitingPeriod() {
		DummyEss ess = null;
		try {
			ess = componentManager.getComponent(CreateTestConfig.ESS_ID);
		} catch (Exception e) {
			fail();
		}
		if (ess == null) {
			fail("Ess was null");
		}
		ess.setMinimalCellVoltage(TestWarning.config.warningCellVoltage() - 1);

		IState next = sut.getNextStateObject();
		assertTrue(next instanceof Limit);
		assertEquals(State.WARNING, next.getState());

		// Wait the defined time, then the next state should be charge
		try {
			Thread.sleep(TestWarning.config.warningCellVoltageTime() * 1000 + 500);
		} catch (InterruptedException e) {
			fail(e.getMessage());
		}

		next = sut.getNextStateObject();
		assertTrue(next instanceof ForceCharge);
		assertEquals(State.CHARGE, next.getState());
	}

	@Test
	public final void testGetNextStateObjectUndefined() {
		componentManager.destroyEss();

		IState next = sut.getNextStateObject();
		assertTrue(next instanceof Undefined);
		assertEquals(State.UNDEFINED, next.getState());
	}

	@Test
	public final void testGetNextStateObjectNormal() {
		// Minimal voltage must be upper than warning cell voltage
		DummyEss ess = null;
		try {
			ess = componentManager.getComponent(CreateTestConfig.ESS_ID);
		} catch (Exception e) {
			fail();
		}
		if (ess == null) {
			fail("Ess was null");
		}
		ess.setMinimalCellVoltage(TestWarning.config.warningCellVoltage() + 1);

		IState next = sut.getNextStateObject();
		assertTrue(next instanceof Normal);
		assertEquals(State.NORMAL, next.getState());
	}

	@Test
	public final void testGetNextStateObjectCritical() {
		// Minimal voltage must be upper than warning cell voltage
		DummyEss ess = null;
		try {
			ess = componentManager.getComponent(CreateTestConfig.ESS_ID);
		} catch (Exception e) {
			fail();
		}
		if (ess == null) {
			fail("Ess was null");
		}
		ess.setMinimalCellVoltage(TestWarning.config.criticalCellVoltage() - 1);

		IState next = sut.getNextStateObject();
		assertTrue(next instanceof Limit);
		assertEquals(State.CRITICAL, next.getState());
	}

	@Test
	public final void testActEssIsDischarging() {
		// Discharging should be denied, i.e. if ESS discharges it should be stopped
		
		DummyEss ess = null;
		try {
			ess = componentManager.getComponent(CreateTestConfig.ESS_ID);
		} catch (Exception e) {
			fail();
		}
		if (ess == null) {
			fail("Ess was null");
		}
		
		assertEquals(0, ess.getCurrentActivePower());
		
		// Simulate a discharging ess
		ess.setCurrentActivePower(DummyEss.MAXIMUM_POWER);
		
		assertEquals(DummyEss.MAXIMUM_POWER, ess.getCurrentActivePower());
		
		try {
			sut.act();
			int activePower = ess.getCurrentActivePower();
			if (activePower > 0) {
				fail("Active power is > 0");
			}
		} catch (Exception e) {
			fail();
		}
	}
	
	@Test
	public final void testActEssIsCharging() {
		// Charging should be still allowed, i.e. if ess is charging, there should be no change
		DummyEss ess = null;
		try {
			ess = componentManager.getComponent(CreateTestConfig.ESS_ID);
		} catch (Exception e) {
			fail();
		}
		if (ess == null) {
			fail("Ess was null");
		}
		
		assertEquals(0, ess.getCurrentActivePower());
		
		// Simulate a charging ess
		ess.setCurrentActivePower(-1 * DummyEss.MAXIMUM_POWER);
		
		assertEquals(-1 * DummyEss.MAXIMUM_POWER, ess.getCurrentActivePower());
		
		try {
			sut.act();
			int activePower = ess.getCurrentActivePower();
			if (activePower > 0) {
				fail("Active power is > 0");
			}
			assertEquals(-1 * DummyEss.MAXIMUM_POWER, ess.getCurrentActivePower());
		} catch (Exception e) {
			fail();
		}
	}
}
