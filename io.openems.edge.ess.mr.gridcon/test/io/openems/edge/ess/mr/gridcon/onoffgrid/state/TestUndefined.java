package io.openems.edge.ess.mr.gridcon.onoffgrid.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import io.openems.edge.ess.mr.gridcon.onoffgrid.helper.Creator;
import io.openems.edge.ess.mr.gridcon.onoffgrid.helper.DummyDecisionTableCondition;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.OnOffGridState;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.Undefined;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.DecisionTableCondition.GridconCommunicationFailed;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.DecisionTableCondition.MeterCommunicationFailed;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.DecisionTableCondition.NAProtection_1_On;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.DecisionTableCondition.NAProtection_2_On;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.DecisionTableCondition.SyncBridgeOn;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.DecisionTableCondition.VoltageInRange;

public class TestUndefined {

	private Undefined sut;
	private static DummyDecisionTableCondition condition;
		
	@Before
	public void setUp() throws Exception {
		condition = new DummyDecisionTableCondition(NAProtection_1_On.TRUE, NAProtection_2_On.TRUE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		sut = new Undefined(//
				Creator.getDummyComponentManager()//
				, condition//
				, Creator.GRIDCON_ID//
				, Creator.BMS_A_ID//
				, Creator.BMS_B_ID//
				, Creator.BMS_C_ID//
				, Creator.INPUT_NA_PROTECTION_1//
				, Creator.INPUT_NA_PROTECTION_2//
				, Creator.INPUT_SYNC_DEVICE_BRIDGE//
				, Creator.OUTPUT_SYNC_DEVICE_BRIDGE//
				, Creator.METER_ID//
				);
	}

	@Test
	public final void testGetState() {
		assertEquals(OnOffGridState.UNDEFINED, sut.getState());
	}

	@Test
	public void testGetNextUndefined() {
		// According to the state machine the next state is "UNDEFINED" if nothing is set before and state is undefined
		assertEquals(OnOffGridState.UNDEFINED, sut.getNextState());
	}
	
	@Test
	public void testGetNextStateStartSystem() {
		// According to the state machine the next state is "START SYSTEM" if
		// conditions are set and state before undefined was at least "START SYSTEM
		//(0,0,1,1,-,1);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.START_SYSTEM, sut.getNextState());
		
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.FALSE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.START_SYSTEM, sut.getNextState());

		sut.setStateBefore(OnOffGridState.START_SYSTEM);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.START_SYSTEM, sut.getNextState());
		
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.FALSE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.START_SYSTEM, sut.getNextState());
		
		//Test impossible transitions
		sut.setStateBefore(OnOffGridState.WAITING_FOR_DEVICES);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertNotEquals(OnOffGridState.START_SYSTEM, sut.getNextState());

		sut.setStateBefore(OnOffGridState.ON_GRID_MODE);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertNotEquals(OnOffGridState.START_SYSTEM, sut.getNextState());
		
		sut.setStateBefore(OnOffGridState.OFF_GRID_MODE);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertNotEquals(OnOffGridState.START_SYSTEM, sut.getNextState());
	
		sut.setStateBefore(OnOffGridState.OFF_GRID_MODE_GRID_BACK);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertNotEquals(OnOffGridState.START_SYSTEM, sut.getNextState());
		
		sut.setStateBefore(OnOffGridState.OFF_GRID_MODE_WAIT_FOR_GRID_AVAILABLE);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertNotEquals(OnOffGridState.START_SYSTEM, sut.getNextState());
		
		sut.setStateBefore(OnOffGridState.OFF_GRID_MODE_ADJUST_PARMETER);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertNotEquals(OnOffGridState.START_SYSTEM, sut.getNextState());
		
		sut.setStateBefore(OnOffGridState.ON_GRID_RESTART_GRIDCON_AFTER_SYNC);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertNotEquals(OnOffGridState.START_SYSTEM, sut.getNextState());		
	}
	
	@Test
	public void testGetNextStateWaitForDevices() {
		// According to the state machine the next state is "WAIT FOR DEVICES" if
		// conditions are set and state before undefined was "START SYSTEM" or "WAIT FOR DEVICES"
		//(false, false, true, true, -, false);
		
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.TRUE, SyncBridgeOn.FALSE);
		assertEquals(OnOffGridState.WAITING_FOR_DEVICES, sut.getNextState());
		
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.FALSE, SyncBridgeOn.FALSE);
		assertEquals(OnOffGridState.WAITING_FOR_DEVICES, sut.getNextState());

		
		sut.setStateBefore(OnOffGridState.START_SYSTEM);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.TRUE, SyncBridgeOn.FALSE);
		assertEquals(OnOffGridState.WAITING_FOR_DEVICES, sut.getNextState());
		
		sut.setStateBefore(OnOffGridState.START_SYSTEM);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.FALSE, SyncBridgeOn.FALSE);
		assertEquals(OnOffGridState.WAITING_FOR_DEVICES, sut.getNextState());
		
		
		sut.setStateBefore(OnOffGridState.WAITING_FOR_DEVICES);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.TRUE, SyncBridgeOn.FALSE);
		assertEquals(OnOffGridState.WAITING_FOR_DEVICES, sut.getNextState());
		
		sut.setStateBefore(OnOffGridState.WAITING_FOR_DEVICES);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.FALSE, SyncBridgeOn.FALSE);
		assertEquals(OnOffGridState.WAITING_FOR_DEVICES, sut.getNextState());
		
		//Test impossible transitions
		sut.setStateBefore(OnOffGridState.ON_GRID_MODE);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.FALSE, SyncBridgeOn.FALSE);
		assertNotEquals(OnOffGridState.WAITING_FOR_DEVICES, sut.getNextState());
		
		sut.setStateBefore(OnOffGridState.OFF_GRID_MODE);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.FALSE, SyncBridgeOn.FALSE);
		assertNotEquals(OnOffGridState.WAITING_FOR_DEVICES, sut.getNextState());
	
		sut.setStateBefore(OnOffGridState.OFF_GRID_MODE_GRID_BACK);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.FALSE, SyncBridgeOn.FALSE);
		assertNotEquals(OnOffGridState.WAITING_FOR_DEVICES, sut.getNextState());
		
		sut.setStateBefore(OnOffGridState.OFF_GRID_MODE_WAIT_FOR_GRID_AVAILABLE);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.FALSE, SyncBridgeOn.FALSE);
		assertNotEquals(OnOffGridState.WAITING_FOR_DEVICES, sut.getNextState());
		
		sut.setStateBefore(OnOffGridState.OFF_GRID_MODE_ADJUST_PARMETER);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.FALSE, SyncBridgeOn.FALSE);
		assertNotEquals(OnOffGridState.WAITING_FOR_DEVICES, sut.getNextState());
				
		sut.setStateBefore(OnOffGridState.ON_GRID_RESTART_GRIDCON_AFTER_SYNC);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.FALSE, SyncBridgeOn.FALSE);
		assertNotEquals(OnOffGridState.WAITING_FOR_DEVICES, sut.getNextState());		
	}
	
	@Test
	public void testGetNextStateOnGrid() {
		// According to the state machine the next state is "ON GRID" if
		// conditions are set and state before undefined was "START SYSTEM", "WAITING", "ON GRID", "RESTART AFTER SYNC"
		//(true, true, false, false, -, -);
		
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.TRUE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.ON_GRID_MODE, sut.getNextState());
		
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.TRUE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.FALSE);
		assertEquals(OnOffGridState.ON_GRID_MODE, sut.getNextState());

		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.TRUE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.FALSE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.ON_GRID_MODE, sut.getNextState());
		
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.TRUE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.FALSE, SyncBridgeOn.FALSE);
		assertEquals(OnOffGridState.ON_GRID_MODE, sut.getNextState());
		
		//Test possible states before
		sut.setStateBefore(OnOffGridState.START_SYSTEM);
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.TRUE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.ON_GRID_MODE, sut.getNextState());
		
		sut.setStateBefore(OnOffGridState.WAITING_FOR_DEVICES);
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.TRUE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.ON_GRID_MODE, sut.getNextState());
		
		sut.setStateBefore(OnOffGridState.ON_GRID_MODE);
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.TRUE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.ON_GRID_MODE, sut.getNextState());
		
		sut.setStateBefore(OnOffGridState.ON_GRID_RESTART_GRIDCON_AFTER_SYNC);
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.TRUE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.ON_GRID_MODE, sut.getNextState());
		
		//Test impossible transitions
		sut.setStateBefore(OnOffGridState.OFF_GRID_MODE);
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.TRUE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertNotEquals(OnOffGridState.ON_GRID_MODE, sut.getNextState());
		
		sut.setStateBefore(OnOffGridState.OFF_GRID_MODE_GRID_BACK);
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.TRUE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertNotEquals(OnOffGridState.ON_GRID_MODE, sut.getNextState());

		sut.setStateBefore(OnOffGridState.OFF_GRID_MODE_WAIT_FOR_GRID_AVAILABLE);
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.TRUE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertNotEquals(OnOffGridState.ON_GRID_MODE, sut.getNextState());
		
		sut.setStateBefore(OnOffGridState.OFF_GRID_MODE_ADJUST_PARMETER);
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.TRUE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertNotEquals(OnOffGridState.ON_GRID_MODE, sut.getNextState());
	}
	
	@Test
	public void testGetNextStateOffGrid() {
		// According to the state machine the next state is "OFF GRID" if
		// conditions are set and state before undefined was 
		//"ON GRID", "OFF_GRID", "OFFGRID_GRID_BACK", "WAIT_FOR_GRID_AVAILABLE", "ADJUST_PARAMETER"
		//(false, false, false, false, false, -);
		
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.FALSE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.OFF_GRID_MODE, sut.getNextState());
		
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.FALSE, SyncBridgeOn.FALSE);
		assertEquals(OnOffGridState.OFF_GRID_MODE, sut.getNextState());

		//Test possible states before
		sut.setStateBefore(OnOffGridState.ON_GRID_MODE);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.FALSE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.OFF_GRID_MODE, sut.getNextState());
			
		sut.setStateBefore(OnOffGridState.ON_GRID_MODE);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.FALSE, SyncBridgeOn.FALSE);
		assertEquals(OnOffGridState.OFF_GRID_MODE, sut.getNextState());
				
		sut.setStateBefore(OnOffGridState.OFF_GRID_MODE);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.FALSE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.OFF_GRID_MODE, sut.getNextState());
			
		sut.setStateBefore(OnOffGridState.OFF_GRID_MODE);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.FALSE, SyncBridgeOn.FALSE);
		assertEquals(OnOffGridState.OFF_GRID_MODE, sut.getNextState());
		
		sut.setStateBefore(OnOffGridState.OFF_GRID_MODE_GRID_BACK);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.FALSE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.OFF_GRID_MODE, sut.getNextState());
			
		sut.setStateBefore(OnOffGridState.OFF_GRID_MODE_GRID_BACK);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.FALSE, SyncBridgeOn.FALSE);
		assertEquals(OnOffGridState.OFF_GRID_MODE, sut.getNextState());
		
		sut.setStateBefore(OnOffGridState.OFF_GRID_MODE_WAIT_FOR_GRID_AVAILABLE);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.FALSE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.OFF_GRID_MODE, sut.getNextState());
			
		sut.setStateBefore(OnOffGridState.OFF_GRID_MODE_WAIT_FOR_GRID_AVAILABLE);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.FALSE, SyncBridgeOn.FALSE);
		assertEquals(OnOffGridState.OFF_GRID_MODE, sut.getNextState());
		
		sut.setStateBefore(OnOffGridState.OFF_GRID_MODE_ADJUST_PARMETER);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.FALSE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.OFF_GRID_MODE, sut.getNextState());
			
		sut.setStateBefore(OnOffGridState.OFF_GRID_MODE_ADJUST_PARMETER);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.FALSE, SyncBridgeOn.FALSE);
		assertEquals(OnOffGridState.OFF_GRID_MODE, sut.getNextState());
				
		//Test impossible transitions
		sut.setStateBefore(OnOffGridState.START_SYSTEM);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.FALSE, SyncBridgeOn.TRUE);
		assertNotEquals(OnOffGridState.OFF_GRID_MODE, sut.getNextState());
			
		sut.setStateBefore(OnOffGridState.START_SYSTEM);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.FALSE, SyncBridgeOn.FALSE);
		assertNotEquals(OnOffGridState.OFF_GRID_MODE, sut.getNextState());		
		
		sut.setStateBefore(OnOffGridState.WAITING_FOR_DEVICES);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.FALSE, SyncBridgeOn.TRUE);
		assertNotEquals(OnOffGridState.OFF_GRID_MODE, sut.getNextState());
			
		sut.setStateBefore(OnOffGridState.WAITING_FOR_DEVICES);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.FALSE, SyncBridgeOn.FALSE);
		assertNotEquals(OnOffGridState.OFF_GRID_MODE, sut.getNextState());
		
		sut.setStateBefore(OnOffGridState.ON_GRID_RESTART_GRIDCON_AFTER_SYNC);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.FALSE, SyncBridgeOn.TRUE);
		assertNotEquals(OnOffGridState.OFF_GRID_MODE, sut.getNextState());
			
		sut.setStateBefore(OnOffGridState.ON_GRID_RESTART_GRIDCON_AFTER_SYNC);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.FALSE, SyncBridgeOn.FALSE);
		assertNotEquals(OnOffGridState.OFF_GRID_MODE, sut.getNextState());
	}
	
	@Test
	public void testGetNextStateOffGridGridBack() {
		// According to the state machine the next state is "OFF GRID GRID BACK" if
		// conditions are set and state before undefined was 
		//"OFF_GRID" or "OFFGRID_GRID_BACK"
		//(false, false, false, false, true, false);
		
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.FALSE);
		assertEquals(OnOffGridState.OFF_GRID_MODE_GRID_BACK, sut.getNextState());
	
		//Test possible states before
		sut.setStateBefore(OnOffGridState.OFF_GRID_MODE_GRID_BACK);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.FALSE);
		assertEquals(OnOffGridState.OFF_GRID_MODE_GRID_BACK, sut.getNextState());
		
		sut.setStateBefore(OnOffGridState.OFF_GRID_MODE);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.FALSE);
		assertEquals(OnOffGridState.OFF_GRID_MODE_GRID_BACK, sut.getNextState());
		
		//Test impossible transitions
		sut.setStateBefore(OnOffGridState.START_SYSTEM);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.FALSE);
		assertNotEquals(OnOffGridState.OFF_GRID_MODE_GRID_BACK, sut.getNextState());
			
		sut.setStateBefore(OnOffGridState.WAITING_FOR_DEVICES);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.FALSE);
		assertNotEquals(OnOffGridState.OFF_GRID_MODE_GRID_BACK, sut.getNextState());
		
		sut.setStateBefore(OnOffGridState.ON_GRID_MODE);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.FALSE);
		assertNotEquals(OnOffGridState.OFF_GRID_MODE_GRID_BACK, sut.getNextState());
		
		sut.setStateBefore(OnOffGridState.OFF_GRID_MODE_WAIT_FOR_GRID_AVAILABLE);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.FALSE);
		assertNotEquals(OnOffGridState.OFF_GRID_MODE_GRID_BACK, sut.getNextState());
		
		sut.setStateBefore(OnOffGridState.OFF_GRID_MODE_ADJUST_PARMETER);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.FALSE);
		assertNotEquals(OnOffGridState.OFF_GRID_MODE_GRID_BACK, sut.getNextState());
		
		sut.setStateBefore(OnOffGridState.ON_GRID_RESTART_GRIDCON_AFTER_SYNC);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.FALSE);
		assertNotEquals(OnOffGridState.OFF_GRID_MODE_GRID_BACK, sut.getNextState());
	}
	
	@Test
	public void testGetNextStateOffGridWaitForGridAvailable() {
		// According to the state machine the next state is "OFF_GRID_MODE_WAIT_FOR_GRID_AVAILABLE" if
		// conditions are set and state before undefined was 
		//"OFF_GRID_MODE_WAIT_FOR_GRID_AVAILABLE" or "OFFGRID_GRID_BACK"
		//(false, false, false, false, true, true);
		
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.OFF_GRID_MODE_WAIT_FOR_GRID_AVAILABLE, sut.getNextState());
	
		//Test possible states before
		sut.setStateBefore(OnOffGridState.OFF_GRID_MODE_GRID_BACK);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.OFF_GRID_MODE_WAIT_FOR_GRID_AVAILABLE, sut.getNextState());
				
		sut.setStateBefore(OnOffGridState.OFF_GRID_MODE_WAIT_FOR_GRID_AVAILABLE);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.OFF_GRID_MODE_WAIT_FOR_GRID_AVAILABLE, sut.getNextState());
		
		//Test impossible transitions
		sut.setStateBefore(OnOffGridState.START_SYSTEM);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertNotEquals(OnOffGridState.OFF_GRID_MODE_GRID_BACK, sut.getNextState());
					
		sut.setStateBefore(OnOffGridState.WAITING_FOR_DEVICES);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertNotEquals(OnOffGridState.OFF_GRID_MODE_WAIT_FOR_GRID_AVAILABLE, sut.getNextState());
				
		sut.setStateBefore(OnOffGridState.ON_GRID_MODE);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertNotEquals(OnOffGridState.OFF_GRID_MODE_WAIT_FOR_GRID_AVAILABLE, sut.getNextState());
		
		sut.setStateBefore(OnOffGridState.OFF_GRID_MODE);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertNotEquals(OnOffGridState.OFF_GRID_MODE_WAIT_FOR_GRID_AVAILABLE, sut.getNextState());
				
		sut.setStateBefore(OnOffGridState.OFF_GRID_MODE_ADJUST_PARMETER);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertNotEquals(OnOffGridState.OFF_GRID_MODE_WAIT_FOR_GRID_AVAILABLE, sut.getNextState());
				
		sut.setStateBefore(OnOffGridState.ON_GRID_RESTART_GRIDCON_AFTER_SYNC);
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertNotEquals(OnOffGridState.OFF_GRID_MODE_WAIT_FOR_GRID_AVAILABLE, sut.getNextState());
	}
	
	@Test
	public void testGetNextStateAdjustParameter() {
		// According to the state machine the next state is "OFF_GRID_MODE_ADJUST_PARMETER" if
		// conditions are set and state before undefined was 
		//"OFF_GRID_MODE_WAIT_FOR_GRID_AVAILABLE" or "OFF_GRID_MODE_ADJUST_PARMETER"
		//(true, false, false, false, true, true);
		
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.OFF_GRID_MODE_ADJUST_PARMETER, sut.getNextState());
	
		//Test possible states before
		sut.setStateBefore(OnOffGridState.OFF_GRID_MODE_WAIT_FOR_GRID_AVAILABLE);
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.OFF_GRID_MODE_ADJUST_PARMETER, sut.getNextState());
						
		sut.setStateBefore(OnOffGridState.OFF_GRID_MODE_ADJUST_PARMETER);
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.OFF_GRID_MODE_ADJUST_PARMETER, sut.getNextState());
		
		//Test impossible transitions
		sut.setStateBefore(OnOffGridState.START_SYSTEM);
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertNotEquals(OnOffGridState.OFF_GRID_MODE_ADJUST_PARMETER, sut.getNextState());
							
		sut.setStateBefore(OnOffGridState.WAITING_FOR_DEVICES);
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertNotEquals(OnOffGridState.OFF_GRID_MODE_ADJUST_PARMETER, sut.getNextState());
						
		sut.setStateBefore(OnOffGridState.ON_GRID_MODE);
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertNotEquals(OnOffGridState.OFF_GRID_MODE_ADJUST_PARMETER, sut.getNextState());
				
		sut.setStateBefore(OnOffGridState.OFF_GRID_MODE);
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertNotEquals(OnOffGridState.OFF_GRID_MODE_ADJUST_PARMETER, sut.getNextState());
						
		sut.setStateBefore(OnOffGridState.OFF_GRID_MODE_GRID_BACK);
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertNotEquals(OnOffGridState.OFF_GRID_MODE_ADJUST_PARMETER, sut.getNextState());
						
		sut.setStateBefore(OnOffGridState.ON_GRID_RESTART_GRIDCON_AFTER_SYNC);
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertNotEquals(OnOffGridState.OFF_GRID_MODE_ADJUST_PARMETER, sut.getNextState());
	}

	@Test
	public void testGetNextStateRestartAfterSync() {
		// According to the state machine the next state is "ON_GRID_RESTART_GRIDCON_AFTER_SYNC" if
		// conditions are set and state before undefined was "ON_GRID_RESTART_GRIDCON_AFTER_SYNC"
		//(true, true, true, false, true, false);
		
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.TRUE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.FALSE);
		assertEquals(OnOffGridState.ON_GRID_RESTART_GRIDCON_AFTER_SYNC, sut.getNextState());
	
		//Test possible states before
		sut.setStateBefore(OnOffGridState.ON_GRID_RESTART_GRIDCON_AFTER_SYNC);
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.TRUE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.FALSE);
		assertEquals(OnOffGridState.ON_GRID_RESTART_GRIDCON_AFTER_SYNC, sut.getNextState());
		
		//Test impossible transitions
		sut.setStateBefore(OnOffGridState.START_SYSTEM);
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.TRUE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.FALSE);
		assertNotEquals(OnOffGridState.ON_GRID_RESTART_GRIDCON_AFTER_SYNC, sut.getNextState());
		
		sut.setStateBefore(OnOffGridState.WAITING_FOR_DEVICES);
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.TRUE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.FALSE);
		assertNotEquals(OnOffGridState.ON_GRID_RESTART_GRIDCON_AFTER_SYNC, sut.getNextState());
		
		sut.setStateBefore(OnOffGridState.ON_GRID_MODE);
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.TRUE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.FALSE);
		assertNotEquals(OnOffGridState.ON_GRID_RESTART_GRIDCON_AFTER_SYNC, sut.getNextState());
		
		sut.setStateBefore(OnOffGridState.OFF_GRID_MODE);
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.TRUE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.FALSE);
		assertNotEquals(OnOffGridState.ON_GRID_RESTART_GRIDCON_AFTER_SYNC, sut.getNextState());
		
		sut.setStateBefore(OnOffGridState.OFF_GRID_MODE_GRID_BACK);
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.TRUE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.FALSE);
		assertNotEquals(OnOffGridState.ON_GRID_RESTART_GRIDCON_AFTER_SYNC, sut.getNextState());
		
		sut.setStateBefore(OnOffGridState.OFF_GRID_MODE_WAIT_FOR_GRID_AVAILABLE);
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.TRUE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.FALSE);
		assertNotEquals(OnOffGridState.ON_GRID_RESTART_GRIDCON_AFTER_SYNC, sut.getNextState());
		
		sut.setStateBefore(OnOffGridState.OFF_GRID_MODE_ADJUST_PARMETER);
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.TRUE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.FALSE);
		assertNotEquals(OnOffGridState.ON_GRID_RESTART_GRIDCON_AFTER_SYNC, sut.getNextState());
	}
	
	@Test
	public void testAct() {
		try {
			sut.act();
		} catch (Exception e) {
			fail("Cannot happen, Undefined.act() should do nothing!");
		}
	}
	
	private static void setCondition(NAProtection_1_On b, NAProtection_2_On c, GridconCommunicationFailed d, MeterCommunicationFailed e, VoltageInRange f, SyncBridgeOn g) {
		condition.setNaProtection1On(b);
		condition.setNaProtection2On(c);
		condition.setGridconCommunicationFailed(d);
		condition.setMeterCommunicationFailed(e);
		condition.setVoltageInRange(f);
		condition.setSyncBridgeOn(g);
	}
}
