package io.openems.edge.controller.ess.limitdischargecellvoltage;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.controller.ess.limitdischargecellvoltage.helper.CreateComponentManager;
import io.openems.edge.controller.ess.limitdischargecellvoltage.helper.CreateTestConfig;
import io.openems.edge.controller.ess.limitdischargecellvoltage.helper.DummyEss;
import io.openems.edge.controller.ess.limitdischargecellvoltage.state.Undefined;

public class TestUndefinedState {

	private IState sut;
	private ComponentManager componentManager;
	private static Config config;
	

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		config = CreateTestConfig.create();
	}

	@Before
	public void setUp() throws Exception {
		componentManager = CreateComponentManager.create();
		sut = new Undefined(componentManager, config);
	}

	@Test
	public final void testGetState() {
		assertEquals(State.UNDEFINED, sut.getState());
	}

	@Test
	public final void testGetNextStateObjectUndefined() {
		Object actual = sut.getNextStateObject();
		assertEquals(sut, actual );
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
		assertEquals(State.CRITICAL, next.getState());
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
		assertEquals(State.WARNING, next.getState());
	}
	
	@Test
	public final void testGetNextStateObjectNormal() {
		try {
			DummyEss ess = componentManager.getComponent(CreateTestConfig.ESS_ID);
			ess.setMinimalCellVoltage(CreateTestConfig.WARNING_CELL_VOLTAGE + 1);
		} catch (OpenemsNamedException e) {
			fail();
		}
		IState next = sut.getNextStateObject();
		assertEquals(State.NORMAL, next.getState());
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
