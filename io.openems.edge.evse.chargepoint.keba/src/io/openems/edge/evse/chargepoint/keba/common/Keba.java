package io.openems.edge.evse.chargepoint.keba.common;

import static io.openems.common.channel.AccessMode.WRITE_ONLY;
import static io.openems.common.channel.Unit.MILLIAMPERE;
import static io.openems.common.types.OpenemsType.INTEGER;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.chargepoint.keba.common.enums.CableState;
import io.openems.edge.evse.chargepoint.keba.common.enums.ChargingState;
import io.openems.edge.evse.chargepoint.keba.common.enums.PhaseSwitchSource;
import io.openems.edge.evse.chargepoint.keba.common.enums.PhaseSwitchState;
import io.openems.edge.evse.chargepoint.keba.common.enums.SetEnable;
import io.openems.edge.evse.chargepoint.keba.common.enums.SetUnlock;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.TimedataProvider;

public interface Keba extends OpenemsComponent, ElectricityMeter, TimedataProvider {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		CHARGING_STATE(Doc.of(ChargingState.values())), //
		CABLE_STATE(Doc.of(CableState.values())), //
		// TODO use COS_PHI to calculate ReactivePower
		POWER_FACTOR(Doc.of(OpenemsType.INTEGER)), //
		PHASE_SWITCH_SOURCE(Doc.of(PhaseSwitchSource.values())), //
		PHASE_SWITCH_STATE(Doc.of(PhaseSwitchState.values())), //

		DEBUG_SET_ENABLE(Doc.of(SetEnable.values())), //
		SET_ENABLE(Doc.of(SetEnable.values())//
				.accessMode(WRITE_ONLY)//
				.onChannelSetNextWriteMirrorToDebugChannel(Keba.ChannelId.DEBUG_SET_ENABLE)),

		DEBUG_SET_CHARGING_CURRENT(Doc.of(INTEGER)//
				.unit(MILLIAMPERE)), //
		SET_CHARGING_CURRENT(Doc.of(INTEGER)//
				.unit(MILLIAMPERE)//
				.accessMode(WRITE_ONLY)//
				.onChannelSetNextWriteMirrorToDebugChannel(Keba.ChannelId.DEBUG_SET_CHARGING_CURRENT)),

		SET_UNLOCK_PLUG(Doc.of(SetUnlock.values())//
				.accessMode(WRITE_ONLY)), //

		SET_PHASE_SWITCH_SOURCE(Doc.of(PhaseSwitchSource.values())//
				.accessMode(WRITE_ONLY)), //
		SET_PHASE_SWITCH_STATE(Doc.of(PhaseSwitchState.values())//
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
	 * Gets the Channel for {@link ChannelId#SET_ENABLE}.
	 *
	 * @return the Channel
	 */
	public default EnumWriteChannel getSetEnableChannel() {
		return this.channel(ChannelId.SET_ENABLE);
	}

	/**
	 * Sets the next Write Value for {@link ChannelId#SET_ENABLE}.
	 * 
	 * @param setEnable the {@link SetEnable}
	 * @throws OpenemsNamedException on error
	 */
	public default void setSetEnable(SetEnable setEnable) throws OpenemsNamedException {
		this.getSetEnableChannel().setNextWriteValue(setEnable);
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
	 * Sets the next Write Value for {@link ChannelId#SET_CHARGING_CURRENT}.
	 * 
	 * @param current current to be set
	 * @throws OpenemsNamedException on error
	 */
	public default void setSetChargingCurrent(int current) throws OpenemsNamedException {
		this.getSetChargingCurrentChannel().setNextWriteValue(current);
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
	 * Gets the Channel for {@link ChannelId#SET_PHASE_SWITCH_SOURCE}.
	 *
	 * @return the Channel
	 */
	public default EnumWriteChannel getSetPhaseSwitchSourceChannel() {
		return this.channel(ChannelId.SET_PHASE_SWITCH_SOURCE);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_PHASE_SWITCH_STATE}.
	 *
	 * @return the Channel
	 */
	public default EnumWriteChannel getSetPhaseSwitchStateChannel() {
		return this.channel(ChannelId.SET_PHASE_SWITCH_STATE);
	}

	/**
	 * Is this {@link Keba} read-only or read-write?.
	 *
	 * @return true for read-only
	 */
	public boolean isReadOnly();
}
