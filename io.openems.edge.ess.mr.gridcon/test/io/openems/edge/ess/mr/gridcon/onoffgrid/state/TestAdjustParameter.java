package io.openems.edge.ess.mr.gridcon.onoffgrid.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import io.openems.edge.ess.mr.gridcon.onoffgrid.helper.Creator;
import io.openems.edge.ess.mr.gridcon.onoffgrid.helper.DummyComponentManager;
import io.openems.edge.ess.mr.gridcon.onoffgrid.helper.DummyDecisionTableCondition;
import io.openems.edge.ess.mr.gridcon.onoffgrid.helper.DummyGridcon;
import io.openems.edge.ess.mr.gridcon.onoffgrid.helper.DummyMeter;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.DecisionTableCondition.GridconCommunicationFailed;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.DecisionTableCondition.MeterCommunicationFailed;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.DecisionTableCondition.NAProtection_1_On;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.DecisionTableCondition.NAProtection_2_On;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.DecisionTableCondition.SyncBridgeOn;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.DecisionTableCondition.VoltageInRange;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.AdjustParameter;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.OnOffGridState;

public class TestAdjustParameter {

	private AdjustParameter sut;
	private DummyComponentManager manager = Creator.getDummyComponentManager();
	private static DummyDecisionTableCondition condition;
		
	@Before
	public void setUp() throws Exception {
		condition = new DummyDecisionTableCondition(NAProtection_1_On.TRUE, NAProtection_2_On.TRUE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		sut = new AdjustParameter(//
				manager  
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
				, Creator.DELTA_FREQUENCY//
				, Creator.DELTA_VOLTAGE//
				, Creator.INPUT_NA_PROTECTION_1_INVERTED//
				, Creator.INPUT_NA_PROTECTION_2_INVERTED//
				, Creator.INPUT_SYNC_DEVICE_BRIDGE_INVERTED				
				);
	}

	@Test
	public final void testGetState() {
		assertEquals(OnOffGridState.OFF_GRID_MODE_ADJUST_PARMETER, sut.getState());
	}
	
	@Test
	public void testGetNextUndefined() {
		// According to the state machine the next state is "UNDEFINED" if e.g. condition is 1,1,1,1,0,1
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.TRUE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.FALSE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.UNDEFINED, sut.getNextState());
	}
	
	@Test
	public void testGetNextStateOffGrid() {
		// According to the state machine the next state is "OFF_GRID" if condition is 0,0,0,0,0,-
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.FALSE, SyncBridgeOn.FALSE);
		assertEquals(OnOffGridState.OFF_GRID_MODE, sut.getNextState());

		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.FALSE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.OFF_GRID_MODE, sut.getNextState());
	}
	

	
	@Test
	public void testGetNextStateAdjustParameter() {
		// According to the state machine the next state is "OFF GRID" if condition is 1,0,0,0,1,1
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.OFF_GRID_MODE_ADJUST_PARMETER, sut.getNextState());
	}
	
//	@Test
//	public void testGetNextStateRestartAfterSync() {
//		// According to the state machine the next state is "OFF GRID" if condition is 1,1,1,0,1,0
//		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.TRUE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.FALSE);
//		assertEquals(OnOffGridState.ON_GRID_RESTART_GRIDCON_AFTER_SYNC, sut.getNextState());
//	}

	@Test
	public void testGetNextStateOnGrid() {
		// According to the state machine the next state is "ON GRID" if condition is 1,1,0,0,-,-
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.TRUE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.FALSE, SyncBridgeOn.FALSE);
		assertEquals(OnOffGridState.ON_GRID_MODE, sut.getNextState());
		
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.TRUE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.FALSE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.ON_GRID_MODE, sut.getNextState());
		
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.TRUE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.FALSE);
		assertEquals(OnOffGridState.ON_GRID_MODE, sut.getNextState());
		
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.TRUE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.ON_GRID_MODE, sut.getNextState());		
	}
	
	//TODO more states that are not allowed
//	@Test
//	public void testGetNextStateErrorNotAllowed() {		
//		// According to the state machine the next state cannot be ERROR
//		// TODO what is error
//		assertNotEquals(OnOffGridState.ERROR, sut.getNextState());
//		
//	}

	
	@Test
	public void testAct() {
		// frequency preset for gridcon 50,6 Hz
			
			try {
				sut.act();
				
				DummyGridcon gridconPCS = this.manager.getComponent(Creator.GRIDCON_ID);
				DummyMeter meter = this.manager.getComponent(Creator.METER_ID);

				float delta = 0.001f;
				
				float actualFrequency = gridconPCS.getSetFrequency();
				float expectedFrequency = meter.getFrequency().value().get() / 1000 +  Creator.DELTA_FREQUENCY;
				
				assertEquals(expectedFrequency, actualFrequency, delta);
				
				float actualVoltage = gridconPCS.getSetVoltage();
				float expectedVoltage = meter.getVoltage().value().get() / 1000  +  Creator.DELTA_VOLTAGE;
				
				assertEquals(expectedVoltage, actualVoltage, delta);
				
			} catch (Exception e) {
				fail("Should not happen");
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
