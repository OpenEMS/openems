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
import io.openems.edge.controller.ess.limitdischargecellvoltage.state.Charge;
import io.openems.edge.controller.ess.limitdischargecellvoltage.state.Critical;
import io.openems.edge.controller.ess.limitdischargecellvoltage.state.Normal;
import io.openems.edge.controller.ess.limitdischargecellvoltage.state.Undefined;
import io.openems.edge.controller.ess.limitdischargecellvoltage.state.Warning;

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
		sut = new Warning(componentManager, config);
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
		assertTrue(next instanceof Warning);
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
		assertTrue(next instanceof Warning);
		assertEquals(State.WARNING, next.getState());

		// Wait the defined time, then the next state should be charge
		try {
			Thread.sleep(TestWarning.config.warningCellVoltageTime() * 1000 + 500);
		} catch (InterruptedException e) {
			fail(e.getMessage());
		}

		next = sut.getNextStateObject();
		assertTrue(next instanceof Charge);
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
		assertTrue(next instanceof Critical);
		assertEquals(State.CRITICAL, next.getState());
	}

	@Test
	public final void testAct() {
		// There is nothing to do
		try {
			sut.act();
		} catch (Exception e) {
			fail();
		}
	}
}
