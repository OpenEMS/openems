package io.openems.edge.evcs.webasto.unite;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.meter.api.ElectricityMeter;

public interface EvcsWebastoUnite extends ElectricityMeter, Evcs, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		SERIAL_NUMBER(Doc.of(OpenemsType.STRING) //
				.accessMode(AccessMode.READ_ONLY)), //
		CHARGE_POINT_ID(Doc.of(OpenemsType.STRING) //
				.accessMode(AccessMode.READ_ONLY)), //
		BRAND(Doc.of(OpenemsType.STRING) //
				.accessMode(AccessMode.READ_ONLY)), //
		MODEL(Doc.of(OpenemsType.STRING) //
				.accessMode(AccessMode.READ_ONLY)), //
		FIRMWARE_VERSION(Doc.of(OpenemsType.STRING) //
				.accessMode(AccessMode.READ_ONLY)), //
		DATE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		TIME(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		CHARGE_POINT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY)), //
		NUMBER_OF_PHASES(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		CHARGE_POINT_STATE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		CHARGING_STATE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		EQUIPMENT_STATE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		CABLE_STATE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		EVSE_FAULT_CODE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		METER_READING(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		SESSION_MAX_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_ONLY)), //
		EVSE_MIN_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_ONLY)), //
		EVSE_MAX_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_ONLY)), //
		CABLE_MAX_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_ONLY)), //
		SESSION_START_TIME(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		SESSION_DURATION(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		SESSION_END_TIME(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		FAILSAFE_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_WRITE)), //
		FAILSAFE_TIMEOUT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		CHARGING_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_WRITE)), //
		ALIVE_REGISTER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)); //

		private final Doc doc;

		ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}

	}

	/**
	 * Gets the Channel for {@link EvcsWebastoUnite.ChannelId#ALIVE_REGISTER}.
	 *
	 * @return the Channel
	 */
	default WriteChannel<Integer> getAliveChannel() {
		return this.channel(ChannelId.ALIVE_REGISTER);
	}

	/**
	 * Gets the Value of {@link EvcsWebastoUnite.ChannelId#ALIVE_REGISTER}.
	 * 
	 * @param value the value
	 */
	default void _setAliveValue(int value) throws OpenemsError.OpenemsNamedException {
		WriteChannel<Integer> channel = this.getAliveChannel();
		channel.setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link EvcsWebastoUnite.ChannelId#CHARGE_POINT_STATE}.
	 *
	 * @return the Channel
	 */
	default Channel<Integer> getChargePointStateChannel() {
		return this.channel(ChannelId.CHARGE_POINT_STATE);
	}

	/**
	 * Gets the Channel for {@link EvcsWebastoUnite.ChannelId#CHARGE_POINT_STATE}.
	 *
	 * @return the Channel
	 */
	default int getChargePointState() {
		Channel<Integer> channel = this.getChargePointStateChannel();
		return channel.value().orElse(channel.getNextValue().orElse(-1));
	}

	/**
	 * Gets the Channel for {@link EvcsWebastoUnite.ChannelId#CHARGING_CURRENT}.
	 *
	 * @return the Channel
	 */
	default WriteChannel<Integer> getCurrentLimitChannel() {
		return this.channel(ChannelId.CHARGING_CURRENT);
	}

	/**
	 * Gets the Value of {@link EvcsWebastoUnite.ChannelId#CHARGING_CURRENT}.
	 *
	 * @return the value
	 */
	default int getCurrentLimit() {
		WriteChannel<Integer> channel = this.getCurrentLimitChannel();
		return channel.value().orElse(channel.getNextValue().orElse(0));
	}

	/**
	 * Sets a value into the CurrentLimit register. See
	 * {@link EvcsWebastoUnite.ChannelId#CHARGING_CURRENT}.
	 *
	 * @param value the next write value
	 * @throws OpenemsError.OpenemsNamedException on error
	 */
	default void setCurrentLimit(int value) throws OpenemsError.OpenemsNamedException {
		WriteChannel<Integer> channel = this.getCurrentLimitChannel();
		channel.setNextWriteValue(value);
	}
}
