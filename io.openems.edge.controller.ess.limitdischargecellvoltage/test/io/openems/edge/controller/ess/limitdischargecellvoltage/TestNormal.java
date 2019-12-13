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
import io.openems.edge.controller.ess.limitdischargecellvoltage.state.Limit;
import io.openems.edge.controller.ess.limitdischargecellvoltage.state.Normal;
import io.openems.edge.controller.ess.limitdischargecellvoltage.state.Undefined;
import io.openems.edge.controller.ess.limitdischargecellvoltage.state.Limit;

public class TestNormal {

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
		sut = new Normal(componentManager, config);
		try {
			DummyEss ess = componentManager.getComponent(CreateTestConfig.ESS_ID);
			// Set voltage into a normal range
			ess.setMinimalCellVoltage(CreateTestConfig.WARNING_CELL_VOLTAGE + 1);
		} catch (OpenemsNamedException e) {
			fail();
		}
	}

	@Test
	public final void testGetState() {
		assertEquals(sut.getState(), State.NORMAL);
	}

	@Test
	public final void testGetNextStateObjectWithNoChanges() {
		IState next = sut.getNextStateObject();
		assertTrue(next instanceof Normal);
		assertEquals(next.getState(), State.NORMAL);
	}

	@Test
	public final void testGetNextStateObjectUndefinedNoVoltage() {
		// set min cell voltage to null
		try {
			DummyEss ess = componentManager.getComponent(CreateTestConfig.ESS_ID);
			ess.setMinimalCellVoltageToUndefined();
		} catch (OpenemsNamedException e) {
			fail();
		}
		IState next = sut.getNextStateObject();
		assertTrue(next instanceof Undefined);
		assertEquals(next.getState(), State.UNDEFINED);
	}

	@Test
	public final void testGetNextStateObjectUndefinedNoEss() {
		componentManager.destroyEss();
		IState next = sut.getNextStateObject();
		assertTrue(next instanceof Undefined);
		assertEquals(next.getState(), State.UNDEFINED);
	}

	@Test
	public final void testGetNextStateObjectWarning() {
		try {
			DummyEss ess = componentManager.getComponent(CreateTestConfig.ESS_ID);
			ess.setMinimalCellVoltage(CreateTestConfig.WARNING_CELL_VOLTAGE - 1);
		} catch (OpenemsNamedException e) {
			fail();
		}
		IState next = sut.getNextStateObject();
		assertTrue(next instanceof Limit);
		assertEquals(next.getState(), State.WARNING);
	}

	@Test
	public final void testGetNextStateObjectCritical() {
		try {
			DummyEss ess = componentManager.getComponent(CreateTestConfig.ESS_ID);
			ess.setMinimalCellVoltage(CreateTestConfig.CRITICAL_CELL_VOLTAGE - 1);
		} catch (OpenemsNamedException e) {
			fail();
		}
		IState next = sut.getNextStateObject();
		assertTrue(next instanceof Limit);
		assertEquals(next.getState(), State.CRITICAL);
	}

	@Test
	public final void testAct() {
		try {
			sut.act();
		} catch (Exception e) {
			fail();
		}
	}

}
