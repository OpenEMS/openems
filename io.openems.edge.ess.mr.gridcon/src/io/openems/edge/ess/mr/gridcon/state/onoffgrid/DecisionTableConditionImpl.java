package io.openems.edge.ess.mr.gridcon.state.onoffgrid;

import java.util.Optional;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.ess.mr.gridcon.GridconPCS;
import io.openems.edge.meter.api.SymmetricMeter;

public class DecisionTableConditionImpl implements DecisionTableCondition {

	private ComponentManager manager;
	private String gridconPcsId;
	private String meterId;
	private String inputNAProtection1;
	private String inputNAProtection2;
	private String inputSyncBridge;

	public DecisionTableConditionImpl(ComponentManager manager, String gridconPcsId, 
			String meterId, String inputNAProtection1, String inputNAProtection2, String inputSyncBridge) {
		super();
		this.manager = manager;
		this.gridconPcsId = gridconPcsId;
		this.meterId = meterId;
		this.inputNAProtection1 = inputNAProtection1;
		this.inputNAProtection2 = inputNAProtection2;
		this.inputSyncBridge = inputSyncBridge;
	}

	@Override
	public NAProtection_1_On isNaProtection1On() {
		try {
			BooleanReadChannel c = manager.getChannel(ChannelAddress.fromString(inputNAProtection1));
			if (c.value().get()) {
				return NAProtection_1_On.TRUE;
			} else {
				return NAProtection_1_On.FALSE;
			}
		} catch (Exception e) {
			return NAProtection_1_On.UNSET;
		}

	}

	@Override
	public NAProtection_2_On isNaProtection2On() {
		try {
			BooleanReadChannel c = manager.getChannel(ChannelAddress.fromString(inputNAProtection2));
			if (c.value().get()) {
				return NAProtection_2_On.TRUE;
			} else {
				return NAProtection_2_On.FALSE;
			}
		} catch (Exception e) {
			return NAProtection_2_On.UNSET;
		}
	}

	@Override
	public GridconCommunicationFailed isGridconCommunicationFailed() {
		GridconPCS gridconPCS;
		try {
			gridconPCS = manager.getComponent(gridconPcsId);
			if (gridconPCS.isCommunicationBroken()) {
				return GridconCommunicationFailed.TRUE;
			} else {
				return GridconCommunicationFailed.FALSE;
			}
		} catch (OpenemsNamedException e) {
			return GridconCommunicationFailed.UNSET;
		}
	}

	@Override
	public MeterCommunicationFailed isMeterCommunicationFailed() {

		try {
			AbstractOpenemsModbusComponent meter = manager.getComponent(meterId);

			BridgeModbus modbusBridge = meter.getModbus();

			StateChannel slaveCommunicationFailedChannel = modbusBridge
					.channel(BridgeModbus.ChannelId.SLAVE_COMMUNICATION_FAILED);
			Optional<Boolean> communicationFailedOpt = slaveCommunicationFailedChannel.value().asOptional();
			// If the channel value is present and it is set then the communication is
			// broken
			if (communicationFailedOpt.isPresent()) {
				if (communicationFailedOpt.get()) {
					return MeterCommunicationFailed.TRUE;
				} else {
					return MeterCommunicationFailed.FALSE;
				}
			} else {
				return MeterCommunicationFailed.UNSET;
			}	
		} catch (Exception e) {
			return MeterCommunicationFailed.UNSET;
		}
	}

	@Override
	public VoltageInRange isVoltageInRange() {
		try {
			SymmetricMeter meter = manager.getComponent(meterId);
			double voltage = meter.getVoltage().value().get() / 1000;
			if (voltage > DecisionTableCondition.LOWER_VOLTAGE && voltage < DecisionTableCondition.UPPER_VOLTAGE) {
				return VoltageInRange.TRUE;
			} else {
				return VoltageInRange.FALSE;
			}
		} catch (Exception e) {
			return VoltageInRange.UNSET;
		}

	}

	@Override
	public SyncBridgeOn isSyncBridgeOn() {
		try {
			BooleanReadChannel c = manager.getChannel(ChannelAddress.fromString(inputSyncBridge));
			if (c.value().get()) {
				return SyncBridgeOn.TRUE;
			} else {
				return SyncBridgeOn.FALSE;
			}
		} catch (Exception e) {
			return SyncBridgeOn.UNSET;
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Input NA Protection 1: ");
		sb.append(isNaProtection1On());
		sb.append("\n");
		
		sb.append("Input NA Protection 2: ");
		sb.append(isNaProtection2On());
		sb.append("\n");
		
		sb.append("GridconCommunicationFailed: ");
		sb.append(isGridconCommunicationFailed());
		sb.append("\n");
		
		sb.append("MeterCommunicationFailed: ");
		sb.append(isMeterCommunicationFailed());
		sb.append("\n");
		
		sb.append("Voltage in Range: ");
		sb.append(isVoltageInRange());
		sb.append("\n");
		
		sb.append("Sync Bridge On: ");
		sb.append(isSyncBridgeOn());
		sb.append("\n");
		
		return sb.toString();
	}
}
