package io.openems.edge.controller.ess.limitdischargecellvoltage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.controller.ess.limitdischargecellvoltage.helper.CreateTestConfig;
import io.openems.edge.controller.ess.limitdischargecellvoltage.helper.DummyComponentManager;
import io.openems.edge.controller.ess.limitdischargecellvoltage.helper.DummyEss;
import io.openems.edge.controller.ess.limitdischargecellvoltage.state.ForceCharge;
import io.openems.edge.controller.ess.limitdischargecellvoltage.state.Limit;

public class TestCriticalState {

	private IState sut;
	private ComponentManager componentManager;
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
		assertEquals(State.CRITICAL, sut.getState());
	}

	@Test
	public final void testGetNextStateObjectWithoutChanges() {
		// It is not important what happened, the next state after "Limit" is always
		// "ForceCharge"
		IState next = sut.getNextStateObject();
		assertTrue(next instanceof ForceCharge);
		assertEquals(State.CHARGE, next.getState());
	}

	@Test
	public final void testGetNextStateObjectCharge() {
		// It is not important what happened, the next state after "Limit" is always
		// "ForceCharge"
		try {
			DummyEss ess = componentManager.getComponent(CreateTestConfig.ESS_ID);
			ess.setMinimalCellVoltage(CreateTestConfig.WARNING_CELL_VOLTAGE + 1);
		} catch (OpenemsNamedException e) {
			fail();
		}
		IState next = sut.getNextStateObject();
		assertTrue(next instanceof ForceCharge);
		assertEquals(State.CHARGE, next.getState());
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
