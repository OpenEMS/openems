package io.openems.edge.controller.ess.limitdischargecellvoltage.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.controller.ess.limitdischargecellvoltage.Config;
import io.openems.edge.controller.ess.limitdischargecellvoltage.IState;
import io.openems.edge.controller.ess.limitdischargecellvoltage.State;
import io.openems.edge.controller.ess.limitdischargecellvoltage.helper.CreateTestConfig;
import io.openems.edge.controller.ess.limitdischargecellvoltage.helper.DummyComponentManager;
import io.openems.edge.controller.ess.limitdischargecellvoltage.helper.DummyEss;
import io.openems.edge.controller.ess.limitdischargecellvoltage.state.Check;
import io.openems.edge.controller.ess.limitdischargecellvoltage.state.ForceCharge;
import io.openems.edge.controller.ess.limitdischargecellvoltage.state.Normal;
import io.openems.edge.controller.ess.limitdischargecellvoltage.state.Undefined;

public class TestCheck {

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
		sut = new Check(ess, config.deltaSoC());
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
		assertEquals(State.CHECK, next);
	}

	@Test
	public final void testGetNextStateNormal() {
		ess.setSoc(ess.getSoc().value().get() + config.deltaSoC());
		State next = sut.getNextState();
		assertEquals(State.NORMAL, next);
	}

	@Test
	public final void testGetNextStateLimitLowCellVoltage() {
		ess.setMinimalCellVoltage(config.warningLowCellVoltage() - 1);
		State next = sut.getNextState();
		assertEquals(State.LIMIT, next);
	}
	
	@Test
	public final void testGetNextStateLimitLowTemperature() {
		ess.setMinimalCellTemperature(config.lowTemperature() - 1);
		State next = sut.getNextState();
		assertEquals(State.LIMIT, next);
	}
	
	@Test
	public final void testGetNextStateLimitHighTemperature() {
		ess.setMinimalCellTemperature(config.highTemperature() + 1);
		State next = sut.getNextState();
		assertEquals(State.LIMIT, next);
	}
	
	//Wie ich  Zeitdaten reinbekomm esehe ich in sum.impl --> TimeData Servide und ich sollte mir einen Channel schreiben für "NotActiveSince"
	
	
//	@Test
//	public final void testGetNextStateObjectNormalAfterWaitingPeriod() {
//		IState next = sut.getNextStateObject();
//		assertTrue(next instanceof ForceCharge);
//		assertEquals(State.CHARGE, next.getState());
//
//		// Wait the defined time, then the next state should always be normal
//		try {
//			Thread.sleep(TestCheck.config.chargingTime() * 1000 + 500);
//		} catch (InterruptedException e) {
//			fail(e.getMessage());
//		}
//
//		next = sut.getNextStateObject();
//		assertTrue(next instanceof Normal);
//		assertEquals(State.NORMAL, next.getState());
//	}
//
//	@Test
//	public final void testGetNextStateObjectUndefined() {
//		IState next = sut.getNextStateObject();
//		assertTrue(next instanceof ForceCharge);
//		assertEquals(State.CHARGE, next.getState());
//
//		componentManager.destroyEss();
//
//		next = sut.getNextStateObject();
//		assertTrue(next instanceof Undefined);
//		assertEquals(State.UNDEFINED, next.getState());
//	}
//
//	@Test
//	public final void testAct() {
//		DummyEss ess = null;
//		try {
//			// After executing the act() function the channel SetActivePowerLessOrEquals
//			// should have a value in the nextWriteValue
//			sut.act();
//			try {
//				ess = componentManager.getComponent(CreateTestConfig.ESS_ID);
//			} catch (OpenemsNamedException e) {
//				fail();
//			}
//
//		} catch (Exception e) {
//			fail();
//		}
//		if (ess == null) {
//			fail("Ess is null");
//		}
//
//		int actual = ess.getSetActivePowerLessOrEquals().getNextWriteValue().get();
//
//		// According to the dummy config 20% of -10000 (neg. values for charge are
//		// expected)
//		int expected = -2000;
//		assertEquals(expected, actual);
//	}
}
