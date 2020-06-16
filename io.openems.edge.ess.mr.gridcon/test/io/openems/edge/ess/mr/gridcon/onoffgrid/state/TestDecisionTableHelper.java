package io.openems.edge.ess.mr.gridcon.onoffgrid.state;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.edge.ess.mr.gridcon.onoffgrid.helper.DummyDecisionTableCondition;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.DecisionTableCondition;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.DecisionTableHelper;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.DecisionTableCondition.GridconCommunicationFailed;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.DecisionTableCondition.MeterCommunicationFailed;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.DecisionTableCondition.NaProtection1On;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.DecisionTableCondition.NaProtection2On;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.DecisionTableCondition.SyncBridgeOn;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.DecisionTableCondition.VoltageInRange;

public class TestDecisionTableHelper {

	@Test
	public final void testIsStartSystem() {
		DecisionTableCondition c1 = new DummyDecisionTableCondition(NaProtection1On.FALSE, NaProtection2On.FALSE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		DecisionTableCondition c2 = new DummyDecisionTableCondition(NaProtection1On.FALSE, NaProtection2On.FALSE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		
		assertTrue(DecisionTableHelper.isStateStartSystem(c1));
		assertTrue(DecisionTableHelper.isStateStartSystem(c2));
	}

	@Test
	public final void testIsWaitingForDevices() {
		DecisionTableCondition c1 = new DummyDecisionTableCondition(NaProtection1On.FALSE, NaProtection2On.FALSE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.FALSE, SyncBridgeOn.FALSE);
		DecisionTableCondition c2 = new DummyDecisionTableCondition(NaProtection1On.FALSE, NaProtection2On.FALSE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.TRUE, SyncBridgeOn.FALSE);
		
		assertTrue(DecisionTableHelper.isWaitingForDevices(c1));
		assertTrue(DecisionTableHelper.isWaitingForDevices(c2));
	}
	
	@Test
	public final void testIsOnGridMode() {
		DecisionTableCondition c1 = new DummyDecisionTableCondition(NaProtection1On.TRUE, NaProtection2On.TRUE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.FALSE, SyncBridgeOn.FALSE);
		DecisionTableCondition c2 = new DummyDecisionTableCondition(NaProtection1On.TRUE, NaProtection2On.TRUE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.FALSE);
		DecisionTableCondition c3 = new DummyDecisionTableCondition(NaProtection1On.TRUE, NaProtection2On.TRUE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.FALSE, SyncBridgeOn.TRUE);
		DecisionTableCondition c4 = new DummyDecisionTableCondition(NaProtection1On.TRUE, NaProtection2On.TRUE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		
		assertTrue(DecisionTableHelper.isOnGridMode(c1));
		assertTrue(DecisionTableHelper.isOnGridMode(c2));
		assertTrue(DecisionTableHelper.isOnGridMode(c3));
		assertTrue(DecisionTableHelper.isOnGridMode(c4));
	}
	
	@Test
	public final void testIsOffGridMode() {
		DecisionTableCondition c1 = new DummyDecisionTableCondition(NaProtection1On.FALSE, NaProtection2On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.FALSE, SyncBridgeOn.FALSE);
		DecisionTableCondition c2 = new DummyDecisionTableCondition(NaProtection1On.FALSE, NaProtection2On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.FALSE, SyncBridgeOn.TRUE);
		
		assertTrue(DecisionTableHelper.isOffGridMode(c1));
		assertTrue(DecisionTableHelper.isOffGridMode(c2));
	}
	
	@Test
	public final void testIsOffGridGridBack() {
		DecisionTableCondition c1 = new DummyDecisionTableCondition(NaProtection1On.FALSE, NaProtection2On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.FALSE);
		
		assertTrue(DecisionTableHelper.isOffGridGridBack(c1));
	}
	
	@Test
	public final void testIsOffGridWaitForGridAvailable() {
		DecisionTableCondition c1 = new DummyDecisionTableCondition(NaProtection1On.FALSE, NaProtection2On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		
		assertTrue(DecisionTableHelper.isOffGridWaitForGridAvailable(c1));
	}
	
	@Test
	public final void testIsOffGridAdjustParameters() {
		DecisionTableCondition c1 = new DummyDecisionTableCondition(NaProtection1On.TRUE, NaProtection2On.FALSE, GridconCommunicationFailed.FALSE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		
		assertTrue(DecisionTableHelper.isAdjustParameters(c1));
	}
	
//	@Test
//	public final void testIsRestartGridconAfterSync() {
//		DecisionTableCondition c1 = new DummyDecisionTableCondition(NaProtection1On.TRUE, NaProtection2On.TRUE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.FALSE);
//		
//		assertTrue(DecisionTableHelper.isRestartGridconAfterSync(c1));
//	}
	
	@Test
	public final void testIsUndefined() {
		//TODO more tests
		DecisionTableCondition c1 = new DummyDecisionTableCondition(NaProtection1On.TRUE, NaProtection2On.TRUE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.TRUE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		
		assertTrue(DecisionTableHelper.isUndefined(c1));
	}
	
	@Test
	public final void testIsError() {
		//TODO more tests
		DecisionTableCondition c1 = new DummyDecisionTableCondition(NaProtection1On.TRUE, NaProtection2On.FALSE, GridconCommunicationFailed.TRUE, MeterCommunicationFailed.FALSE, VoltageInRange.TRUE, SyncBridgeOn.TRUE);
		
		assertTrue(DecisionTableHelper.isError(c1));
	}
}
