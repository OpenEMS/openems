package io.openems.edge.battery.soltaro.controller.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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

public class TestLimit {

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
		// Always create ess newly to have an ess in "normal" situation that does
		// nothing
		componentManager.initEss();
		ess = componentManager.getComponent(Creator.ESS_ID);
		componentManager.destroyBms();
		componentManager.initBms();
		bms = componentManager.getComponent(Creator.BMS_ID);
		sut = new Limit(ess, bms, config.warningLowCellVoltage(), config.criticalLowCellVoltage(),
				config.criticalHighCellVoltage(), config.warningSoC(), config.lowTemperature(),
				config.highTemperature(), config.unusedTime());
	}

	@Test
	public final void testGetState() {
		assertEquals(State.LIMIT, sut.getState());
	}

	@Test
	public final void testGetNextStateForceCharge() {
		ess.setMinimalCellVoltage(config.criticalLowCellVoltage() - 1);
		assertEquals(State.FORCE_CHARGE, sut.getNextState());
	}

	@Test
	public final void testGetNextStateFullCharge() {
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

		// Waiting long enough means that the last charge or discharge action is too
		// long away
		ess.setMinimalCellVoltage(config.warningLowCellVoltage() - 1);
		next = sut.getNextState();
		assertEquals(State.LIMIT, next);
	}

	@Test
	public final void testGetNextStateNormal() {
		assertEquals(State.NORMAL, sut.getNextState());
	}

	@Test
	public final void testGetNextStateUndefined() {
		ess.setSocToUndefined();
		State next = sut.getNextState();
		assertEquals(State.UNDEFINED, next);
	}

	@Test
	public final void testGetNextStateLimitMinCellVoltage() {
		ess.setMinimalCellVoltage(config.warningLowCellVoltage() - 1);
		assertEquals(State.LIMIT, sut.getNextState());

		ess.setMinimalCellVoltage(config.warningLowCellVoltage());
		assertEquals(State.LIMIT, sut.getNextState());

		ess.setMinimalCellVoltage(config.warningLowCellVoltage() + 1);
		assertEquals(State.NORMAL, sut.getNextState());
	}

	@Test
	public final void testGetNextStateLimitMaxCellVoltage() {
		ess.setMaximalCellVoltage(config.criticalHighCellVoltage() + 1);
		assertEquals(State.LIMIT, sut.getNextState());

		ess.setMaximalCellVoltage(config.criticalHighCellVoltage());
		assertEquals(State.LIMIT, sut.getNextState());

		ess.setMaximalCellVoltage(config.criticalHighCellVoltage() - 1);
		assertEquals(State.NORMAL, sut.getNextState());
	}

	@Test
	public final void testGetNextStateLimitMinCellTemperature() {
		ess.setMinimalCellTemperature(config.lowTemperature() - 1);
		assertEquals(State.LIMIT, sut.getNextState());

		ess.setMinimalCellTemperature(config.lowTemperature());
		assertEquals(State.LIMIT, sut.getNextState());

		ess.setMinimalCellTemperature(config.lowTemperature() + 1);
		assertEquals(State.NORMAL, sut.getNextState());
	}

	@Test
	public final void testGetNextStateLimitMaxCellTemperature() {
		ess.setMaximalCellTemperature(config.highTemperature() + 1);
		assertEquals(State.LIMIT, sut.getNextState());

		ess.setMaximalCellTemperature(config.highTemperature());
		assertEquals(State.LIMIT, sut.getNextState());

		ess.setMaximalCellTemperature(config.highTemperature() - 1);
		assertEquals(State.NORMAL, sut.getNextState());
	}

	@Test
	public final void testGetNextStateLimitSoc() {
		ess.setSoc(config.warningSoC() - 1);
		assertEquals(State.LIMIT, sut.getNextState());

		ess.setSoc(config.warningSoC());
		assertEquals(State.LIMIT, sut.getNextState());

		ess.setSoc(config.warningSoC() + 1);
		assertEquals(State.NORMAL, sut.getNextState());
	}

	@Test
	public final void testDenyDischargingLowCellVoltage() {
		int power = 1000;
		ess.setCurrentActivePower(power);
		ess.setMinimalCellVoltage(config.warningLowCellVoltage());

		assertEquals(power, ess.getCurrentActivePower());
		try {
			sut.act();
		} catch (OpenemsNamedException e) {
			fail(e.getMessage());
		}

		assertTrue(ess.getCurrentActivePower() <= 0);
	}

	@Test
	public final void testDenyDischargingLowSoc() {
		int power = 1000;
		ess.setCurrentActivePower(power);
		ess.setSoc(config.warningSoC());

		assertEquals(power, ess.getCurrentActivePower());
		try {
			sut.act();
		} catch (OpenemsNamedException e) {
			fail(e.getMessage());
		}

		assertTrue(ess.getCurrentActivePower() <= 0);
	}

	@Test
	public final void testDenyChargingHighCellVoltage() {
		int power = -1000;
		ess.setCurrentActivePower(power);
		ess.setMaximalCellVoltage(config.criticalHighCellVoltage());

		assertEquals(power, ess.getCurrentActivePower());
		try {
			sut.act();
		} catch (OpenemsNamedException e) {
			fail(e.getMessage());
		}

		assertTrue(ess.getCurrentActivePower() >= 0);
	}

	@Test
	public final void testDenyChargingLowTemperature() {
		int power = -1000;
		ess.setCurrentActivePower(power);
		ess.setMinimalCellTemperature(config.lowTemperature());

		assertEquals(power, ess.getCurrentActivePower());
		try {
			sut.act();
		} catch (OpenemsNamedException e) {
			fail(e.getMessage());
		}

		assertTrue(ess.getCurrentActivePower() == 0);
	}

	@Test
	public final void testDenyDischargingLowTemperature() {
		int power = 1000;
		ess.setCurrentActivePower(power);
		ess.setMinimalCellTemperature(config.lowTemperature());

		assertEquals(power, ess.getCurrentActivePower());
		try {
			sut.act();
		} catch (OpenemsNamedException e) {
			fail(e.getMessage());
		}

		assertTrue(ess.getCurrentActivePower() == 0);
	}

	@Test
	public final void testDenyChargingHighTemperature() {
		int power = -1000;
		ess.setCurrentActivePower(power);
		ess.setMaximalCellTemperature(config.highTemperature());

		assertEquals(power, ess.getCurrentActivePower());
		try {
			sut.act();
		} catch (OpenemsNamedException e) {
			fail(e.getMessage());
		}

		assertTrue(ess.getCurrentActivePower() == 0);
	}

	@Test
	public final void testDenyDischargingHighTemperature() {
		int power = 1000;
		ess.setCurrentActivePower(power);
		ess.setMaximalCellTemperature(config.highTemperature());

		assertEquals(power, ess.getCurrentActivePower());
		try {
			sut.act();
		} catch (OpenemsNamedException e) {
			fail(e.getMessage());
		}

		assertTrue(ess.getCurrentActivePower() == 0);
	}
}
