package io.openems.edge.battery.soltaro.controller.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.soltaro.controller.Config;
import io.openems.edge.battery.soltaro.controller.IState;
import io.openems.edge.battery.soltaro.controller.State;
import io.openems.edge.battery.soltaro.controller.helper.Creator;
import io.openems.edge.battery.soltaro.controller.helper.DummyBattery;
import io.openems.edge.battery.soltaro.controller.helper.DummyComponentManager;
import io.openems.edge.battery.soltaro.controller.helper.DummyEss;
import io.openems.edge.battery.soltaro.controller.state.Check;


public class TestCheck {

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
		componentManager.destroyBms();
		componentManager.initBms();
		bms = componentManager.getComponent(Creator.BMS_ID);
		sut = new Check(ess, bms, config.deltaSoC(), config.unusedTime(), config.criticalLowCellVoltage());
	}

	@Test
	public final void testGetState() {
		assertEquals(sut.getState(), State.CHECK);
	}

	@Test
	public final void testGetNextStateNoChanges() {
		State next = sut.getNextState();
		assertEquals(State.CHECK, next);
	}

	@Test
	public final void testGetNextStateUndefined() {
		ess.setSocToUndefined();
		State next = sut.getNextState();
		assertEquals(State.UNDEFINED, next);
	}

	@Test
	public final void testGetNextStateNormal() {
		State next = sut.getNextState();
		assertEquals(State.CHECK, next);
		
		ess.setSoc(ess.getSoc().value().get() + config.deltaSoC() + 1);
		next = sut.getNextState();
		assertEquals(State.NORMAL, next);
	}

	@Test
	public final void testGetNextStateForceCharge() {
		ess.setMinimalCellVoltage(config.criticalLowCellVoltage() - 1);
		State next = sut.getNextState();
		assertEquals(State.FORCE_CHARGE, next);
	}
	
	@Test
	public final void testGetNextStateFullCharge() {
		
		//TODO how do i tell the bms that there was action ?!
		// we find it out if there was no ChargeIndication for the last 2 weeks in the soltaro bms
		//Wie ich  Zeitdaten reinbekomm esehe ich in sum.impl --> TimeData Service und ich sollte mir einen Channel schreiben für "NotActiveSince"
		
		
		// writing two times causes past values in the channel
		bms.setChargeIndication(1);
		bms.setChargeIndication(1); 
		
		
		State next = sut.getNextState();
		assertEquals(State.FULL_CHARGE, next);
		
		try {
			Thread.sleep(1000 * config.unusedTime() + 500);
		} catch (InterruptedException e) {
			fail();
		}
		
		//Waiting long enough means that the last charge or discharge action is too long away
		next = sut.getNextState();
		assertEquals(State.CHECK, next);
	}
	
	@Test
	public final void testActAllowCharging() {
		int power = -2000;
		ess.setCurrentActivePower(power);
		try {
			sut.act();
		} catch (OpenemsNamedException e) {
			fail();
		}
		int expected = power;
		int actual = ess.getCurrentActivePower();
		assertEquals(expected, actual);
	}

	@Test
	public final void testActDenyDischarging() {
		int power = 2000;
		ess.setCurrentActivePower(power);
		try {
			sut.act();
		} catch (OpenemsNamedException e) {
			fail();
		}
		int expected = 0;
		int actual = ess.getCurrentActivePower();
		assertEquals(expected, actual);
	}
}
