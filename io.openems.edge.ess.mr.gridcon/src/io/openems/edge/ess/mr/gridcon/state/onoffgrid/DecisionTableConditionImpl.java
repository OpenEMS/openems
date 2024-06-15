package io.openems.edge.ess.mr.gridcon.state.onoffgrid;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.ess.mr.gridcon.GridconPcs;
import io.openems.edge.meter.api.ElectricityMeter;

public class DecisionTableConditionImpl implements DecisionTableCondition {

	private ComponentManager manager;
	private String gridconPcsId;
	private String meterId;
	private String inputNaProtection1;
	private String inputNaProtection2;
	private String inputSyncBridge;
	private boolean nA1Inverted;
	private boolean nA2Inverted;
	private boolean isSyncBridgeInverted;

	public DecisionTableConditionImpl(ComponentManager manager, String gridconPcsId, String meterId,
			String inputNaProtection1, String inputNaProtection2, String inputSyncBridge, boolean isNa1Inverted,
			boolean isNa2Inverted, boolean isSyncBridgeInverted) {
		super();
		this.manager = manager;
		this.gridconPcsId = gridconPcsId;
		this.meterId = meterId;
		this.inputNaProtection1 = inputNaProtection1;
		this.inputNaProtection2 = inputNaProtection2;
		this.inputSyncBridge = inputSyncBridge;
		this.nA1Inverted = isNa1Inverted;
		this.nA2Inverted = isNa2Inverted;
		this.isSyncBridgeInverted = isSyncBridgeInverted;
	}

	@Override
	public NaProtection1On isNaProtection1On() {
		try {
			BooleanReadChannel c = this.manager.getChannel(ChannelAddress.fromString(this.inputNaProtection1));

			boolean value = c.value().get();

			if (this.nA1Inverted) {
				value = !value;
			}

			if (value) {
				return NaProtection1On.TRUE;
			} else {
				return NaProtection1On.FALSE;
			}
		} catch (Exception e) {
			return NaProtection1On.UNSET;
		}

	}

	@Override
	public NaProtection2On isNaProtection2On() {
		try {
			BooleanReadChannel c = this.manager.getChannel(ChannelAddress.fromString(this.inputNaProtection2));
			boolean value = c.value().get();

			if (this.nA2Inverted) {
				value = !value;
			}

			if (value) {
				return NaProtection2On.TRUE;
			} else {
				return NaProtection2On.FALSE;
			}
		} catch (Exception e) {
			return NaProtection2On.UNSET;
		}
	}

	@Override
	public GridconCommunicationFailed isGridconCommunicationFailed() {
		GridconPcs gridconPcs;
		try {
			gridconPcs = this.manager.getComponent(this.gridconPcsId);
			if (gridconPcs.isCommunicationBroken()) {
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
			ModbusComponent meter = this.manager.getComponent(this.meterId);
			if (meter.getModbusCommunicationFailed()) {
				return MeterCommunicationFailed.TRUE;
			} else {
				return MeterCommunicationFailed.FALSE;
			}
		} catch (Exception e) {
			return MeterCommunicationFailed.UNSET;
		}
	}

	@Override
	public VoltageInRange isVoltageInRange() {
		try {
			ElectricityMeter meter = this.manager.getComponent(this.meterId);
			double voltage = meter.getVoltage().get() / 1000;
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
			BooleanReadChannel c = this.manager.getChannel(ChannelAddress.fromString(this.inputSyncBridge));

			boolean value = c.value().get();

			if (this.isSyncBridgeInverted) {
				value = !value;
			}

			if (value) {

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
		sb.append(this.isNaProtection1On());
		sb.append("\n");

		sb.append("Input NA Protection 2: ");
		sb.append(this.isNaProtection2On());
		sb.append("\n");

		sb.append("GridconCommunicationFailed: ");
		sb.append(this.isGridconCommunicationFailed());
		sb.append("\n");

		sb.append("MeterCommunicationFailed: ");
		sb.append(this.isMeterCommunicationFailed());
		sb.append("\n");

		sb.append("Voltage in Range: ");
		sb.append(this.isVoltageInRange());
		sb.append("\n");

		sb.append("Sync Bridge On: ");
		sb.append(this.isSyncBridgeOn());
		sb.append("\n");

		return sb.toString();
	}
}
