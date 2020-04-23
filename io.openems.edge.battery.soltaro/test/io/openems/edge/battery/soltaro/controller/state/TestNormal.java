package io.openems.edge.battery.soltaro.controller.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.openems.edge.battery.soltaro.ChargeIndication;
import io.openems.edge.battery.soltaro.controller.Config;
import io.openems.edge.battery.soltaro.controller.IState;
import io.openems.edge.battery.soltaro.controller.State;
import io.openems.edge.battery.soltaro.controller.helper.Creator;
import io.openems.edge.battery.soltaro.controller.helper.DummyBattery;
import io.openems.edge.battery.soltaro.controller.helper.DummyComponentManager;
import io.openems.edge.battery.soltaro.controller.helper.DummyEss;

public class TestNormal {

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
		// Always create ess and bms newly to have them in "normal" situation that does
		// nothing
		componentManager.initEss();
		ess = componentManager.getComponent(Creator.ESS_ID);
		componentManager.initBms();
		bms = componentManager.getComponent(Creator.BMS_ID);
		sut = new Normal(ess, bms, config.warningLowCellVoltage(), config.criticalHighCellVoltage(),
				config.warningSoC(), config.lowTemperature(), config.highTemperature(), config.unusedTime());
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
	public final void testGetNextStateNormalNoChargingValuesPresent() {
		// writing two times causes past values in the channel
		bms.getChargeIndication().setNextValue(ChargeIndication.DISCHARGE);
		bms.getChargeIndication().nextProcessImage();
		bms.getChargeIndication().setNextValue(ChargeIndication.DISCHARGE);
		bms.getChargeIndication().nextProcessImage();

		State next = sut.getNextState();
		assertEquals(State.NORMAL, next);

		try {
			Thread.sleep(1000 * config.unusedTime() + 500);
		} catch (InterruptedException e) {
			fail();
		}

		bms.getChargeIndication().setNextValue(ChargeIndication.DISCHARGE);
		bms.getChargeIndication().nextProcessImage();

		// Waiting long enough means that the last charge or discharge action is too
		// long away, but there are no values, so state should be normal
		next = sut.getNextState();
		assertEquals(State.NORMAL, next);
	}

	@Test
	public final void testGetNextStateNormalNoChargingValuePresent() {
		// writing two times causes past values in the channel
		bms.getChargeIndication().setNextValue(ChargeIndication.DISCHARGE);
		bms.getChargeIndication().nextProcessImage();
		bms.getChargeIndication().setNextValue(ChargeIndication.DISCHARGE);
		bms.getChargeIndication().nextProcessImage();

		State next = sut.getNextState();
		assertEquals(State.NORMAL, next);

		try {
			Thread.sleep(1000 * config.unusedTime() + 500);
		} catch (InterruptedException e) {
			fail();
		}

		// Waiting long enough means that the last charge or discharge action is too
		// long away, but there are no values, so state should be normal
		next = sut.getNextState();
		assertEquals(State.NORMAL, next);
	}

//	@Test
//	public final void testGetNextStateFullCharge() {
//		// writing two times causes past values in the channel
//		bms.setChargeIndication(1);
//		bms.setChargeIndication(1);
//
//		State next = sut.getNextState();
//		assertEquals(State.NORMAL, next);
//
//		try {
//			Thread.sleep(1000 * config.unusedTime() + 500);
//		} catch (InterruptedException e) {
//			fail();
//		}
//		
//		bms.setChargeIndication(0);
//		bms.setChargeIndication(0);
//
//		// Waiting long enough means that the last charge or discharge action is too
//		// long away, but there are no values, so state should be normal
//		next = sut.getNextState();
//		assertEquals(State.FULL_CHARGE, next);
//	}

	@Test
	public final void testGetNextStateUndefinedNoVoltage() {
		bms.setMinimalCellVoltageToUndefined();
		State next = sut.getNextState();
		assertEquals(State.UNDEFINED, next);
	}

	@Test
	public final void testGetNextStateLimitLowCellVoltage() {
		bms.setMinimalCellVoltage(config.warningLowCellVoltage() - 1);
		State next = sut.getNextState();
		assertEquals(State.LIMIT, next);

		bms.setMinimalCellVoltage(config.warningLowCellVoltage());
		next = sut.getNextState();
		assertEquals(State.NORMAL, next);
	}

	@Test
	public final void testGetNextStateLimitHighCellVoltage() {
		bms.setMaximalCellVoltage(config.criticalHighCellVoltage() + 1);
		State next = sut.getNextState();
		assertEquals(State.LIMIT, next);

		bms.setMaximalCellVoltage(config.criticalHighCellVoltage());
		next = sut.getNextState();
		assertEquals(State.NORMAL, next);
	}

	@Test
	public final void testGetNextStateLimitLowCellTemperature() {
		bms.setMinimalCellTemperature(config.lowTemperature() - 1);
		State next = sut.getNextState();
		assertEquals(State.LIMIT, next);

		bms.setMinimalCellTemperature(config.lowTemperature());
		next = sut.getNextState();
		assertEquals(State.NORMAL, next);
	}

	@Test
	public final void testGetNextStateLimitHighCellTemperature() {
		bms.setMaximalCellTemperature(config.highTemperature() + 1);
		State next = sut.getNextState();
		assertEquals(State.LIMIT, next);

		bms.setMaximalCellTemperature(config.highTemperature());
		next = sut.getNextState();
		assertEquals(State.NORMAL, next);
	}

	@Test
	public final void testGetNextStateLimitSoc() {
		bms.setSoc(config.warningSoC() - 1);
		State next = sut.getNextState();
		assertEquals(State.LIMIT, next);

		bms.setSoc(config.warningSoC());
		next = sut.getNextState();
		assertEquals(State.NORMAL, next);
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
