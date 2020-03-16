package io.openems.edge.ess.mr.gridcon.onoffgrid.helper;

import io.openems.edge.ess.mr.gridcon.state.onoffgrid.DecisionTableCondition;

public class DummyDecisionTableCondition implements DecisionTableCondition {

	NAProtection_1_On isNaProtection1On;
	NAProtection_2_On isNaProtection2On;
	GridconCommunicationFailed isGridconCommunicationFailed;
	MeterCommunicationFailed isMeterCommunicationFailed;
	VoltageInRange isVoltageInRange;
	SyncBridgeOn isSyncBridgeOn;

	public DummyDecisionTableCondition(NAProtection_1_On isNaProtection1On, NAProtection_2_On isNaProtection2On,
			GridconCommunicationFailed isGridconCommunicationFailed, MeterCommunicationFailed isMeterCommunicationFailed, VoltageInRange isVoltageInRange,
			SyncBridgeOn isSyncBridgeOn) {
		super();
		this.isNaProtection1On = isNaProtection1On;
		this.isNaProtection2On = isNaProtection2On;
		this.isGridconCommunicationFailed = isGridconCommunicationFailed;
		this.isMeterCommunicationFailed = isMeterCommunicationFailed;
		this.isVoltageInRange = isVoltageInRange;
		this.isSyncBridgeOn = isSyncBridgeOn;
	}

	@Override
	public NAProtection_1_On isNaProtection1On() {
		return isNaProtection1On;
	}

	@Override
	public NAProtection_2_On isNaProtection2On() {
		return isNaProtection2On;
	}

	@Override
	public GridconCommunicationFailed isGridconCommunicationFailed() {
		return isGridconCommunicationFailed;
	}

	@Override
	public MeterCommunicationFailed isMeterCommunicationFailed() {
		return isMeterCommunicationFailed;
	}

	@Override
	public VoltageInRange isVoltageInRange() {
		return isVoltageInRange;
	}

	@Override
	public SyncBridgeOn isSyncBridgeOn() {
		return isSyncBridgeOn;
	}

	public void setNaProtection1On(NAProtection_1_On isNaProtection1On) {
		this.isNaProtection1On = isNaProtection1On;
	}

	public void setNaProtection2On(NAProtection_2_On isNaProtection2On) {
		this.isNaProtection2On = isNaProtection2On;
	}

	public void setGridconCommunicationFailed(GridconCommunicationFailed isGridconCommunicationFailed) {
		this.isGridconCommunicationFailed = isGridconCommunicationFailed;
	}

	public void setMeterCommunicationFailed(MeterCommunicationFailed isMeterCommunicationFailed) {
		this.isMeterCommunicationFailed = isMeterCommunicationFailed;
	}

	public void setVoltageInRange(VoltageInRange isVoltageInRange) {
		this.isVoltageInRange = isVoltageInRange;
	}

	public void setSyncBridgeOn(SyncBridgeOn isSyncBridgeOn) {
		this.isSyncBridgeOn = isSyncBridgeOn;
	}

	
}
