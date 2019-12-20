package io.openems.edge.controller.ess.limitdischargecellvoltage.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.openems.edge.controller.ess.limitdischargecellvoltage.Config;
import io.openems.edge.controller.ess.limitdischargecellvoltage.IState;
import io.openems.edge.controller.ess.limitdischargecellvoltage.State;
import io.openems.edge.controller.ess.limitdischargecellvoltage.helper.CreateTestConfig;
import io.openems.edge.controller.ess.limitdischargecellvoltage.helper.DummyComponentManager;
import io.openems.edge.controller.ess.limitdischargecellvoltage.helper.DummyEss;
import io.openems.edge.controller.ess.limitdischargecellvoltage.state.Undefined;

public class TestUndefinedState {

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
		//Always create ess newly to have an ess in "normal" situation that does nothing
		componentManager.initEss();
		ess = componentManager.getComponent(CreateTestConfig.ESS_ID);
		sut = new Undefined(ess, config.warningLowCellVoltage(), config.criticalHighCellVoltage(), config.warningSoC(), config.lowTemperature(), config.highTemperature());
	}

	@Test
	public final void testGetState() {
		assertEquals(State.UNDEFINED, sut.getState());
	}

	@Test
	public final void testGetNextStateUndefined() {
		assertNotEquals(State.UNDEFINED, sut.getNextState());
		
		ess.setSocToUndefined();
		State actual = sut.getNextState();
		assertEquals(State.UNDEFINED, actual);
	}

	@Test
	public final void testGetNextStateLimitMinCellVoltage() {
		ess.setMinimalCellVoltage(CreateTestConfig.WARNING_LOW_CELL_VOLTAGE - 1);
		State next = sut.getNextState();
		assertEquals(State.LIMIT, next);
	}

	@Test
	public final void testGetNextStateLimitMaxCellVoltage() {
		ess.setMaximalCellVoltage(CreateTestConfig.CRITICAL_HIGH_CELL_VOLTAGE + 1);
		State next = sut.getNextState();
		assertEquals(State.LIMIT, next);
	}
	
	@Test
	public final void testGetNextStateLimitMinCellTemperature() {
		ess.setMinimalCellTemperature(CreateTestConfig.LOW_TEMPERATURE -1);
		State next = sut.getNextState();
		assertEquals(State.LIMIT, next);
	}
	
	@Test
	public final void testGetNextStateLimitMaxCellTemperature() {
		ess.setMaximalCellTemperature(CreateTestConfig.HIGH_TEMPERATURE + 1);
		State next = sut.getNextState();
		assertEquals(State.LIMIT, next);
	}
	
	@Test
	public final void testGetNextStateLimitSoc() {
		ess.setSoc(CreateTestConfig.WARNING_SOC - 1);
		State next = sut.getNextState();
		assertEquals(State.LIMIT, next);
	}
	
	@Test
	public final void testGetNextStateNormal() {
		State next = sut.getNextState();
		assertEquals(State.NORMAL, next);
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
