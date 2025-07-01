package io.openems.edge.evse.chargepoint.keba.common;

import static io.openems.common.channel.AccessMode.WRITE_ONLY;
import static io.openems.common.channel.Unit.MILLIAMPERE;
import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.common.types.OpenemsType.STRING;

import io.openems.common.channel.Unit;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.chargepoint.keba.common.enums.CableState;
import io.openems.edge.evse.chargepoint.keba.common.enums.ChargingState;
import io.openems.edge.evse.chargepoint.keba.common.enums.PhaseSwitchSource;
import io.openems.edge.evse.chargepoint.keba.common.enums.PhaseSwitchState;
import io.openems.edge.evse.chargepoint.keba.common.enums.SetEnable;
import io.openems.edge.evse.chargepoint.keba.common.enums.SetUnlock;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.TimedataProvider;

public interface EvseChargePointKeba extends EvseChargePoint, ElectricityMeter, TimedataProvider, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		FIRMWARE(Doc.of(STRING)), //
		CHARGING_STATE(Doc.of(ChargingState.values())), //
		CABLE_STATE(Doc.of(CableState.values())), //

		ERROR_CODE(Doc.of(INTEGER)), //
		SERIAL_NUMBER(Doc.of(INTEGER)), //
		ENERGY_SESSION(Doc.of(INTEGER)), //
		POWER_FACTOR(Doc.of(INTEGER)), //
		MAX_CHARGING_CURRENT(Doc.of(INTEGER)), //
		PHASE_SWITCH_SOURCE(Doc.of(PhaseSwitchSource.values())), //
		PHASE_SWITCH_STATE(Doc.of(PhaseSwitchState.values())), //
		FAILSAFE_CURRENT_SETTING(Doc.of(INTEGER)), //
		FAILSAFE_TIMEOUT_SETTING(Doc.of(INTEGER)), //

		/*
		 * Write Registers
		 */

		DEBUG_SET_ENABLE(Doc.of(SetEnable.values())), //
		SET_ENABLE(Doc.of(SetEnable.values()) //
				.accessMode(WRITE_ONLY) //
				.onChannelSetNextWriteMirrorToDebugChannel(DEBUG_SET_ENABLE)),

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
	 * Gets the Channel for {@link ChannelId#SET_ENABLE}.
	 *
	 * @return the Channel
	 */
	public default EnumWriteChannel getSetEnableChannel() {
		return this.channel(ChannelId.SET_ENABLE);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_CHARGING_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetChargingCurrentChannel() {
		return this.channel(ChannelId.SET_CHARGING_CURRENT);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_PHASE_SWITCH_SOURCE}.
	 *
	 * @return the Channel
	 */
	public default EnumWriteChannel getSetPhaseSwitchSourceChannel() {
		return this.channel(ChannelId.SET_PHASE_SWITCH_SOURCE);
	}

	/**
	 * Gets the required {@link PhaseSwitchSource} for this implementation.
	 * 
	 * @return the {@link PhaseSwitchSource}
	 */
	public PhaseSwitchSource getRequiredPhaseSwitchSource();

	/**
	 * Gets the Channel for {@link ChannelId#SET_PHASE_SWITCH_STATE}.
	 *
	 * @return the Channel
	 */
	public default EnumWriteChannel getSetPhaseSwitchStateChannel() {
		return this.channel(ChannelId.SET_PHASE_SWITCH_STATE);
	}
}
