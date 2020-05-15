package io.openems.edge.controller.battery.batteryprotection.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.openems.edge.controller.battery.batteryprotection.IState;
import io.openems.edge.controller.battery.batteryprotection.State;
import io.openems.edge.controller.battery.batteryprotection.helper.Creator;
import io.openems.edge.controller.battery.batteryprotection.helper.DummyBattery;
import io.openems.edge.controller.battery.batteryprotection.helper.DummyComponentManager;
import io.openems.edge.controller.battery.batteryprotection.helper.DummyEss;

public class TestUndefinedState {

	private IState sut;
	private static DummyComponentManager componentManager;
	private DummyEss ess;
	private DummyBattery bms;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		componentManager = new DummyComponentManager();
	}

	@Before
	public void setUp() throws Exception {
		// Always create ess newly to have an ess in "normal" situation that does
		// nothing
		componentManager.initEss();
		ess = componentManager.getComponent(Creator.ESS_ID);
		bms = componentManager.getComponent(Creator.BMS_ID);
		sut = new Undefined(ess, bms);
	}

	@Test
	public final void testGetState() {
		assertEquals(State.UNDEFINED, sut.getState());
	}

	@Test
	public final void testGetNextStateUndefinedSoc() {
		assertNotEquals(State.UNDEFINED, sut.getNextState());

		bms.setSocToUndefined();
		State nextState = sut.getNextState();
		assertEquals(State.UNDEFINED, nextState);

		bms.setSoc(0);
		nextState = sut.getNextState();
		assertNotEquals(State.UNDEFINED, nextState);
		assertEquals(State.NORMAL, nextState);
	}

	@Test
	public final void testGetNextStateUndefinedMinCell() {
		assertNotEquals(State.UNDEFINED, sut.getNextState());

		bms.setMinimalCellVoltageToUndefined();
		State nextState = sut.getNextState();
		assertEquals(State.UNDEFINED, nextState);

		bms.setMinimalCellVoltage(0);
		nextState = sut.getNextState();
		assertNotEquals(State.UNDEFINED, nextState);
		assertEquals(State.NORMAL, nextState);
	}

	@Test
	public final void testGetNextStateNormal() {
		State nextState = sut.getNextState();
		assertEquals(State.NORMAL, nextState);
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
