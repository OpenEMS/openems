package io.openems.edge.controller.battery.batteryprotection.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.controller.battery.batteryprotection.Config;
import io.openems.edge.controller.battery.batteryprotection.IState;
import io.openems.edge.controller.battery.batteryprotection.State;
import io.openems.edge.controller.battery.batteryprotection.helper.Creator;
import io.openems.edge.controller.battery.batteryprotection.helper.DummyBattery;
import io.openems.edge.controller.battery.batteryprotection.helper.DummyComponentManager;
import io.openems.edge.controller.battery.batteryprotection.helper.DummyEss;

public class TestForceCharge {

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
		// Always create ess newly to have an ess in "normal" situation
		// This ess has a min cell voltage below the limit, and no active power
		componentManager.initEss();
		ess = componentManager.getComponent(Creator.ESS_ID);
		componentManager.initBms();
		bms = componentManager.getComponent(Creator.BMS_ID);
		bms.setMinimalCellVoltage(config.criticalLowCellVoltage() - 1);
		sut = new ForceCharge(ess, bms, config.chargePowerPercent(), config.chargingTime(),
				config.forceChargeReachableMinCellVoltage());
	}

	@Test
	public final void testGetState() {
		assertEquals(sut.getState(), State.FORCE_CHARGE);
	}

	@Test
	public final void testGetNextStateNoChanges() {
		State next = sut.getNextState();
		assertEquals(State.FORCE_CHARGE, next);
	}

	@Test
	public final void testGetNextStateCheckAfterWaitingPeriod() {
		State next = sut.getNextState();
		assertEquals(State.FORCE_CHARGE, next);

		// Wait the defined time, then the next state should always be CHECK
		try {
			Thread.sleep(TestForceCharge.config.chargingTime() * 1000 + 500);
		} catch (InterruptedException e) {
			fail(e.getMessage());
		}

		next = sut.getNextState();
		assertEquals(State.CHECK, next);
	}

	@Test
	public final void testGetNextStateCheckAfterReachingMinCellLimit() {
		State next = sut.getNextState();
		assertEquals(State.FORCE_CHARGE, next);

		bms.setMinimalCellVoltage(config.forceChargeReachableMinCellVoltage() + 1);

		next = sut.getNextState();
		assertEquals(State.CHECK, next);
	}

	@Test
	public final void testGetNextStateObjectUndefined() {
		bms.setSocToUndefined();
		assertEquals(State.UNDEFINED, sut.getNextState());
	}

	@Test
	public final void testAct() {
		// After executing the act() function the channel SetActivePowerLessOrEquals
		// should have a value in the nextWriteValue
		try {
			sut.act();
		} catch (OpenemsNamedException e) {
			fail(e.getMessage());
		}

		int actual = ess.getSetActivePowerLessOrEquals().getNextWriteValue().get();

		// According to the dummy config 20% of -10000 (neg. values for charge are
		// expected)
		int expected = -1 * DummyEss.MAXIMUM_POWER * config.chargePowerPercent() / 100;
		assertEquals(expected, actual);

		actual = ess.getCurrentActivePower();

		assertEquals(expected, actual);
	}
}
