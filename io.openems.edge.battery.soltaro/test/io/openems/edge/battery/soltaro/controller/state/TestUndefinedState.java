package io.openems.edge.battery.soltaro.controller.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.openems.edge.battery.soltaro.controller.helper.DummyComponentManager;
import io.openems.edge.battery.soltaro.controller.helper.DummyEss;
import io.openems.edge.battery.soltaro.controller.state.Undefined;
import io.openems.edge.battery.soltaro.controller.IState;
import io.openems.edge.battery.soltaro.controller.State;
import io.openems.edge.battery.soltaro.controller.helper.Creator;
import io.openems.edge.battery.soltaro.controller.helper.DummyBattery;

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
		//Always create ess newly to have an ess in "normal" situation that does nothing
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
		
		ess.setSocToUndefined();
		State nextState = sut.getNextState();
		assertEquals(State.UNDEFINED, nextState);
		
		ess.setSoc(0);
		nextState = sut.getNextState();
		assertNotEquals(State.UNDEFINED, nextState);
		assertEquals(State.NORMAL, nextState);
	}
	
	@Test
	public final void testGetNextStateUndefinedMinCell() {
		assertNotEquals(State.UNDEFINED, sut.getNextState());
		
		ess.setMinimalCellVoltageToUndefined();
		State nextState = sut.getNextState();
		assertEquals(State.UNDEFINED, nextState);
		
		ess.setMinimalCellVoltage(0);
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
