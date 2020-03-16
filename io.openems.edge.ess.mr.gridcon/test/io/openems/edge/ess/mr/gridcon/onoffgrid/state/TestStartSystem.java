package io.openems.edge.ess.mr.gridcon.onoffgrid.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import io.openems.edge.ess.mr.gridcon.onoffgrid.OnOffGridState;
import io.openems.edge.ess.mr.gridcon.onoffgrid.helper.Creator;
import io.openems.edge.ess.mr.gridcon.onoffgrid.helper.DummyDecisionTableCondition;
import io.openems.edge.ess.mr.gridcon.onoffgrid.state.DecisionTableCondition.GridconCommunicationFailed;
import io.openems.edge.ess.mr.gridcon.onoffgrid.state.DecisionTableCondition.MeterCommunicationFailed;
import io.openems.edge.ess.mr.gridcon.onoffgrid.state.DecisionTableCondition.NAProtection_1_On;
import io.openems.edge.ess.mr.gridcon.onoffgrid.state.DecisionTableCondition.NAProtection_2_On;
import io.openems.edge.ess.mr.gridcon.onoffgrid.state.DecisionTableCondition.SyncBridgeOn;
import io.openems.edge.ess.mr.gridcon.onoffgrid.state.DecisionTableCondition.VoltageInRange;

public class TestStartSystem {

	private StartSystem sut;
	private static DummyDecisionTableCondition condition;
		
	@Before
	public void setUp() throws Exception {
		condition = new DummyDecisionTableCondition(NAProtection_1_On.TRUE, NAProtection_2_On.TRUE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		sut = new StartSystem(//
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
		assertEquals(OnOffGridState.START_SYSTEM, sut.getState());
	}

		
	@Test
	public void testAct() {
		try {
//			expected = syncbridge auf true;
//			actual = getSyncBridge();
//			assertEquals(expected, actual);
			sut.act();
//			expected = syncbridge auf false;
//			actual = getSyncBridge();
//			assertEquals(expected, actual);

		} catch (Exception e) {
			fail("Should not happen, StartSystem.act() should only set syncBridge!");
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
