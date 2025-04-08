package io.openems.edge.evse.chargepoint.keba;

import static io.openems.common.channel.AccessMode.WRITE_ONLY;
import static io.openems.common.channel.Unit.MILLIAMPERE;
import static io.openems.common.types.OpenemsType.FLOAT;
import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.common.types.OpenemsType.STRING;

import io.openems.common.channel.Unit;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.chargepoint.keba.enums.CableState;
import io.openems.edge.evse.chargepoint.keba.enums.ChargingState;
import io.openems.edge.evse.chargepoint.keba.enums.PhaseSwitchSource;
import io.openems.edge.evse.chargepoint.keba.enums.PhaseSwitchState;
import io.openems.edge.evse.chargepoint.keba.enums.ProductTypeAndFeatures;
import io.openems.edge.evse.chargepoint.keba.enums.SetEnable;
import io.openems.edge.evse.chargepoint.keba.enums.SetUnlock;
import io.openems.edge.meter.api.ElectricityMeter;

public interface EvseChargePointKeba extends EvseChargePoint, ElectricityMeter, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		CHARGING_STATE(Doc.of(ChargingState.values())), //
		CABLE_STATE(Doc.of(CableState.values())), //
		ERROR_CODE(Doc.of(INTEGER)), //
		SERIAL_NUMBER(Doc.of(INTEGER)), //
		FIRMWARE(Doc.of(STRING)), //
		ENERGY_SESSION(Doc.of(INTEGER)), //
		POWER_FACTOR(Doc.of(FLOAT)), //
		MAX_CHARGING_CURRENT(Doc.of(INTEGER)), //
		RFID(Doc.of(STRING)), //
		PHASE_SWITCH_SOURCE(Doc.of(PhaseSwitchSource.values())), //
		PHASE_SWITCH_STATE(Doc.of(PhaseSwitchState.values())), //
		FAILSAFE_CURRENT_SETTING(Doc.of(INTEGER)), //
		FAILSAFE_TIMEOUT_SETTING(Doc.of(INTEGER)), //

		PTAF_PRODUCT_TYPE(Doc.of(ProductTypeAndFeatures.ProductType.values())), //
		PTAF_CABLE_OR_SOCKET(Doc.of(ProductTypeAndFeatures.CableOrSocket.values())), //
		PTAF_SUPPORTED_CURRENT(Doc.of(ProductTypeAndFeatures.SupportedCurrent.values())), //
		PTAF_DEVICE_SERIES(Doc.of(ProductTypeAndFeatures.DeviceSeries.values())), //
		PTAF_ENERGY_METER(Doc.of(ProductTypeAndFeatures.EnergyMeter.values())), //
		PTAF_AUTHORIZATION(Doc.of(ProductTypeAndFeatures.Authorization.values())), //

		/*
		 * Write Registers
		 */
		DEBUG_SET_CHARGING_CURRENT(Doc.of(INTEGER) //
				.unit(MILLIAMPERE)), //
		SET_CHARGING_CURRENT(Doc.of(INTEGER) //
				.unit(MILLIAMPERE) //
				.accessMode(WRITE_ONLY) //
				.onChannelSetNextWriteMirrorToDebugChannel(DEBUG_SET_CHARGING_CURRENT)),
		SET_ENERGY_LIMIT(Doc.of(INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.accessMode(WRITE_ONLY)), //
		SET_UNLOCK_PLUG(Doc.of(SetUnlock.values()) //
				.accessMode(WRITE_ONLY)), //
		DEBUG_SET_ENABLE(Doc.of(SetEnable.values())), //
		SET_ENABLE(Doc.of(SetEnable.values()) //
				.accessMode(WRITE_ONLY) //
				.onChannelSetNextWriteMirrorToDebugChannel(DEBUG_SET_ENABLE)),
		SET_PHASE_SWITCH_SOURCE(Doc.of(PhaseSwitchSource.values()) //
				.accessMode(WRITE_ONLY)), //
		SET_PHASE_SWITCH_STATE(Doc.of(PhaseSwitchState.values()) //
				.accessMode(WRITE_ONLY)), //
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the Channel for {@link ChannelId#CHARGING_STATE}.
	 *
	 * @return the Channel
	 */
	public default Channel<ChargingState> getChargingStateChannel() {
		return this.channel(ChannelId.CHARGING_STATE);
	}

	/**
	 * Gets the Status of the Charge Point. See {@link ChannelId#CHARGING_STATE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default ChargingState getChargingState() {
		return this.getChargingStateChannel().value().asEnum();
	}

	/**
	 * Gets the Channel for {@link ChannelId#PHASE_SWITCH_SOURCE}.
	 *
	 * @return the Channel
	 */
	public default EnumReadChannel getPhaseSwitchSourceChannel() {
		return this.channel(ChannelId.PHASE_SWITCH_SOURCE);
	}

	/**
	 * Gets the {@link PhaseSwitchSource}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default PhaseSwitchSource getPhaseSwitchSource() {
		return this.getPhaseSwitchSourceChannel().value().asEnum();
	}

	/**
	 * Gets the Channel for {@link ChannelId#PHASE_SWITCH_STATE}.
	 *
	 * @return the Channel
	 */
	public default EnumReadChannel getPhaseSwitchStateChannel() {
		return this.channel(ChannelId.PHASE_SWITCH_STATE);
	}

	/**
	 * Gets the {@link PhaseSwitchState}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default PhaseSwitchState getPhaseSwitchState() {
		return this.getPhaseSwitchStateChannel().value().asEnum();
	}

	/**
	 * Gets the Channel for {@link ChannelId#CABLE_STATE}.
	 *
	 * @return the Channel
	 */
	public default Channel<CableState> getCableStateChannel() {
		return this.channel(ChannelId.CABLE_STATE);
	}

	/**
	 * Gets the Cable-State of the Charge Point. See {@link ChannelId#CABLE_STATE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default CableState getCableState() {
		return this.getCableStateChannel().value().asEnum();
	}
}
