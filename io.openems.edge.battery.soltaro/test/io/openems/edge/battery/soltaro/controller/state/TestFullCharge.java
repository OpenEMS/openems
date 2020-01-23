package io.openems.edge.battery.soltaro.controller.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.openems.edge.battery.soltaro.controller.helper.DummyComponentManager;
import io.openems.edge.battery.soltaro.controller.helper.DummyEss;
import io.openems.edge.battery.soltaro.controller.state.FullCharge;
import io.openems.edge.battery.soltaro.controller.Config;
import io.openems.edge.battery.soltaro.controller.IState;
import io.openems.edge.battery.soltaro.controller.State;
import io.openems.edge.battery.soltaro.controller.helper.Creator;
import io.openems.edge.battery.soltaro.controller.helper.DummyBattery;

public class TestFullCharge {

	private IState sut;
	private static DummyComponentManager componentManager;
	private static Config config;
	private DummyEss ess;
	private DummyBattery bms;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		config = Creator.createConfig();
		componentManager = new DummyComponentManager();
	}

	@Before
	public void setUp() throws Exception {
		//Always create ess newly to have an ess in "normal" situation that does nothing
		componentManager.initEss();
		ess = componentManager.getComponent(Creator.ESS_ID);
		bms = componentManager.getComponent(Creator.BMS_ID);
		sut = new FullCharge(ess, bms, config.criticalHighCellVoltage(), config.chargePowerPercent());
	}

	@Test
	public final void testGetState() {
		assertEquals(State.FULL_CHARGE, sut.getState());
	}

	@Test
	public final void testGetNextStateUndefinedSoCUndefined() {
		ess.setSocToUndefined();
		State nextState = sut.getNextState();
		assertEquals(State.UNDEFINED, nextState);
		
		ess.setSoc(0);
		nextState = sut.getNextState();
		assertEquals(State.FULL_CHARGE, nextState);
	}
	
	@Test
	public final void testGetNextStateUndefinedMaxCellVoltageUndefined() {
		ess.setMaximalCellVoltageToUndefined();
		State nextState = sut.getNextState();
		assertEquals(State.UNDEFINED, nextState);
		
		ess.setMaximalCellVoltage(0);
		nextState = sut.getNextState();
		assertEquals(State.FULL_CHARGE, nextState);
	}
	
	@Test
	public final void testGetNextStateNormalCriticalVoltageReached() {
		ess.setMaximalCellVoltage(config.criticalHighCellVoltage());
		State nextState = sut.getNextState();
		assertEquals(State.NORMAL, nextState);		
	}
	
	@Test
	public final void testGetNextStateNormalCriticalVoltageExceeded() {
		ess.setMaximalCellVoltage(config.criticalHighCellVoltage() + 1);
		State nextState = sut.getNextState();
		assertEquals(State.NORMAL, nextState);		
	}
	
	@Test
	public final void testGetNextStateFullChargeNothingChanged() {
		// If values are defined and max cell voltage is not above critical value state should remain in full charge
		State nextState = sut.getNextState();
		assertEquals(State.FULL_CHARGE, nextState);		
	}
	
	@Test
	public final void testGetNextStateFullChargeMinVoltageChanged() {
		// If values are defined and max cell voltage has not reached critical value state should remain in full charge
		// Other values are not interesting
		ess.setMinimalCellVoltage(DummyEss.DEFAULT_MIN_CELL_VOLTAGE - 1);
		State nextState = sut.getNextState();
		assertEquals(State.FULL_CHARGE, nextState);		
	}

	@Test
	public final void testAct() {
		try {
			//ess should charge
			sut.act();			
			int activePower = ess.getCurrentActivePower();			
			assertTrue(activePower < 0);			
		} catch (Exception e) {
			fail();
		}
	}
}
