package io.openems.edge.ess.mr.gridcon.onoffgrid.state;

import java.util.Optional;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.bridge.modbus.api.AbstractModbusBridge;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.ess.mr.gridcon.GridconPCS;
import io.openems.edge.meter.api.SymmetricMeter;

public class DecisionTableConditionImpl implements DecisionTableCondition {

	private ComponentManager manager;
	private String gridconPcsId;
	private String meterModbusId;
	private String meterId;
	private String inputNAProtection1;
	private String inputNAProtection2;
	private String inputSyncBridge;
	
	@Override
	public NAProtection_1_On isNaProtection1On() throws Exception {
		BooleanReadChannel c = manager.getChannel(ChannelAddress.fromString(inputNAProtection1));
		if(c.value().get()) {
			return NAProtection_1_On.TRUE;
		} else {
			return NAProtection_1_On.FALSE;
		}
	}

	@Override
	public NAProtection_2_On isNaProtection2On() throws Exception {
		BooleanReadChannel c = manager.getChannel(ChannelAddress.fromString(inputNAProtection2));
		if(c.value().get()) {
			return NAProtection_2_On.TRUE;
		} else {
			return NAProtection_2_On.FALSE;
		}
	}

	@Override
	public GridconCommunicationFailed isGridconCommunicationFailed() throws Exception {
		GridconPCS gridconPCS = manager.getComponent(gridconPcsId);
		if(gridconPCS.isCommunicationBroken()) {
			return GridconCommunicationFailed.TRUE;
		} else {
			return GridconCommunicationFailed.FALSE;
		}
		
	}

	@Override
	public MeterCommunicationFailed isMeterCommunicationFailed() throws Exception {
		AbstractModbusBridge modbusBridge = manager.getComponent(meterModbusId);
		Channel<Boolean> slaveCommunicationFailedChannel = modbusBridge.getSlaveCommunicationFailedChannel();
		Optional<Boolean> communicationFailedOpt = slaveCommunicationFailedChannel.value().asOptional();
		// If the channel value is present and it is set then the communication is
		// broken
		if (communicationFailedOpt.isPresent() && communicationFailedOpt.get()) {
			return MeterCommunicationFailed.TRUE;
		}
		return MeterCommunicationFailed.FALSE;
	}

	@Override
	public VoltageInRange isVoltageInRange() throws Exception {
		SymmetricMeter meter =  manager.getComponent(meterId);
		double voltage = meter.getVoltage().value().get();
		if (voltage > DecisionTableCondition.LOWER_VOLTAGE && voltage < DecisionTableCondition.UPPER_VOLTAGE) {
			return VoltageInRange.TRUE;
		} else {
			return VoltageInRange.FALSE;
		}
	}

	@Override
	public SyncBridgeOn isSyncBridgeOn() throws Exception {
		BooleanReadChannel c = manager.getChannel(ChannelAddress.fromString(inputSyncBridge));
		if(c.value().get()) {
			return SyncBridgeOn.TRUE;
		} else {
			return SyncBridgeOn.FALSE;
		}
	}

}
