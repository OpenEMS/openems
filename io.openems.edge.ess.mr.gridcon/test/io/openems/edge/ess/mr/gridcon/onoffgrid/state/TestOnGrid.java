package io.openems.edge.ess.mr.gridcon.onoffgrid.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.ess.mr.gridcon.onoffgrid.helper.Creator;
import io.openems.edge.ess.mr.gridcon.onoffgrid.helper.DummyComponentManager;
import io.openems.edge.ess.mr.gridcon.onoffgrid.helper.DummyDecisionTableCondition;
import io.openems.edge.ess.mr.gridcon.onoffgrid.helper.DummyGridcon;
import io.openems.edge.ess.mr.gridcon.onoffgrid.helper.DummyIo;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.DecisionTableCondition.GridconCommunicationFailed;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.DecisionTableCondition.MeterCommunicationFailed;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.DecisionTableCondition.NAProtection_1_On;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.DecisionTableCondition.NAProtection_2_On;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.DecisionTableCondition.SyncBridgeOn;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.DecisionTableCondition.VoltageInRange;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.OnGrid;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.OnOffGridState;

public class TestOnGrid {

	private OnGrid sut;
	private DummyComponentManager manager = Creator.getDummyComponentManager();
	private static DummyDecisionTableCondition condition;
		
	@Before
	public void setUp() throws Exception {
		condition = new DummyDecisionTableCondition(NAProtection_1_On.TRUE, NAProtection_2_On.TRUE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		sut = new OnGrid(//
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
				, Creator.TARGET_FREQUENCY_ONGRID//
				, Creator.INPUT_NA_PROTECTION_1_INVERTED//
				, Creator.INPUT_NA_PROTECTION_2_INVERTED//
				, Creator.INPUT_SYNC_DEVICE_BRIDGE_INVERTED
				);
	}

	@Test
	public final void testGetState() {
		assertEquals(OnOffGridState.ON_GRID_MODE, sut.getState());
	}
	
	@Test
	public void testGetNextUndefined() {
		// According to the state machine the next state is "UNDEFINED" if e.g. condition is 1,1,1,1,0,1
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.TRUE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.FALSE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.UNDEFINED, sut.getNextState());
	}
	
	@Test
	public void testGetNextStateOnGrid() {
		// According to the state machine the next state is "ON_GRID" if condition is 0,0,1,1,-,-
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.TRUE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.FALSE, SyncBridgeOn.FALSE);
		assertEquals(OnOffGridState.ON_GRID_MODE, sut.getNextState());
		
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.TRUE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.FALSE);
		assertEquals(OnOffGridState.ON_GRID_MODE, sut.getNextState());
		
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.TRUE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.FALSE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.ON_GRID_MODE, sut.getNextState());
		
		setCondition(NAProtection_1_On.TRUE, NAProtection_2_On.TRUE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.ON_GRID_MODE, sut.getNextState());
	}
	
	@Test
	public void testGetNextStateOffGrid() {
		// According to the state machine the next state is "OFF GRID" if condition is 0,0,0,0,0,-
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.FALSE, SyncBridgeOn.FALSE);
		assertEquals(OnOffGridState.OFF_GRID_MODE, sut.getNextState());
		
		setCondition(NAProtection_1_On.FALSE, NAProtection_2_On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.FALSE, SyncBridgeOn.TRUE);
		assertEquals(OnOffGridState.OFF_GRID_MODE, sut.getNextState());
	}
	
////	@Test
////	public void testGetNextStateWaitError() {
////		// According to the state machine the next state is "ERROR" if ?? --> NOT DEFINED YET		
////	}
//
//	//TODO more states that are not allowed
//
	
	@Test
	public void testAct() {
		//sync bridge has to bet set to false and frequency preset for gridcon 52,7 Hz
		//TODO act() causes an error becuse of the sub state machine that is not set!
		
			try {
				sut.act();
				
				String channelName = DummyIo.adaptChannelAdress(Creator.OUTPUT_SYNC_DEVICE_BRIDGE);
				ChannelAddress adress = ChannelAddress.fromString(channelName);
				BooleanWriteChannel outputSyncDeviceBridgeChannel = this.manager.getChannel(adress);
				boolean expected = false;
				boolean actual = outputSyncDeviceBridgeChannel.getNextWriteValue().get();
				
				assertEquals(expected, actual);
				
				DummyGridcon gridconPCS = this.manager.getComponent(Creator.GRIDCON_ID);
				float actualFrequency = gridconPCS.getSetFrequency();
				float expectedFrequency = Creator.TARGET_FREQUENCY_ONGRID;
				float delta = 0.001f;
				
				assertEquals(expectedFrequency, actualFrequency, delta);

			} catch (Exception e) {
				fail("Should not happen, OnGrid.act() should only set syncBridge and frequency!");
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
