package io.openems.edge.controller.ess.limitdischargecellvoltage.state;

import static org.junit.Assert.assertEquals;
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

public class TestNormal {

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
		sut = new Normal(ess, config.warningLowCellVoltage(), config.criticalHighCellVoltage(), config.warningSoC(), config.lowTemperature(), config.highTemperature(), config.unusedTime());
	}

	@Test
	public final void testGetState() {
		assertEquals(sut.getState(), State.NORMAL);
	}

	@Test
	public final void testGetNextStateWithNoChanges() {
		State next = sut.getNextState();
		assertEquals(State.NORMAL, next);
	}

	@Test
	public final void testGetNextStateUndefinedNoVoltage() {
		ess.setMinimalCellVoltageToUndefined();
		State next = sut.getNextState();
		assertEquals(State.UNDEFINED, next);
	}

//	@Test //TODO discuss is this possible, necessary..?!
//	public final void testGetNextStateUndefinedNoEss() {
//		componentManager.destroyEss();
//		State next = sut.getNextState();
//		assertEquals(State.UNDEFINED, next);
//	}

	@Test
	public final void testGetNextStateLimitLowCellVoltage() {
		ess.setMinimalCellVoltage(config.warningLowCellVoltage() - 1);
		State next = sut.getNextState();
		assertEquals(State.LIMIT, next);
	}

	@Test
	public final void testGetNextStateLimitHighCellVoltage() {
		ess.setMaximalCellVoltage(config.criticalHighCellVoltage() + 1);
		State next = sut.getNextState();
		assertEquals(State.LIMIT, next);
	}
	
	@Test
	public final void testGetNextStateLimitLowCellTemperature() {
		ess.setMinimalCellTemperature(config.lowTemperature() - 1);
		State next = sut.getNextState();
		assertEquals(State.LIMIT, next);
	}

	@Test
	public final void testGetNextStateLimitHighCellTemperature() {
		ess.setMaximalCellTemperature(config.highTemperature() + 1);
		State next = sut.getNextState();
		assertEquals(State.LIMIT, next);
	}
	
	@Test
	public final void testGetNextStateLimitSoc() {
		ess.setSoc(config.warningSoC() - 1);
		State next = sut.getNextState();
		assertEquals(State.LIMIT, next);
	}

	@Test
	public final void testAct() {
		// act should have no interference on ess
		try {
			int power = 1000;
			ess.setCurrentActivePower(power);
			sut.act();
			assertEquals(power, ess.getCurrentActivePower());
			
			power = -1000;
			ess.setCurrentActivePower(power);
			sut.act();
			assertEquals(power, ess.getCurrentActivePower());
		} catch (Exception e) {
			fail();
		}
	}

}
