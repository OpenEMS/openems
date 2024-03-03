package io.openems.edge.evcs.spelsberg.smart;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface EvcsSpelsbergSmart extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/* Integration of Modbus register set: TQ-DM100 */

		EVSE_STATE(Doc.of(EvseState.values()) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("State of the charging station")),

		CABLE_STATE(Doc.of(CableState.values()) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("State of the cable socket connection")),

		CHARGE_POINT_STATE(Doc.of(ChargePointState.values()) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("State of the charging device")),

		LIFE_BIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE) //
				.text("Heartbeat toggle bit")),

		MIN_HARDWARE_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY) //
				.text("Minimum charging current of the hardware")),

		MAX_HARDWARE_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY) //
				.text("Maximal charging current of the hardware")),

		CHARGE_SAVE_CURRENT_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE).accessMode(AccessMode.READ_WRITE) //
				.text("Maximum charging current under communication failure")),

		CHARGE_START_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY) //
				.text("Start time of charging process")),

		CHARGE_STOP_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY) //
				.text("Stop time of charging process")),

		CHARGE_DURATION_SESSION(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS) //
				.text("Duration of the current session in Wh")),

		CHARGE_ENERGY_SESSION(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.text("Sum of charged energy for the current session in Wh")),

		CHARGE_SIGNALED_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Maximum current signaled to the EV for charging")),

		CURRENT_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE).accessMode(AccessMode.READ_ONLY) //
				.text("Current on L1")),

		CURRENT_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE).accessMode(AccessMode.READ_ONLY) //
				.text("Current on L2")),

		CURRENT_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE).accessMode(AccessMode.READ_ONLY) //
				.text("Current on L3")),

		POWER_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY) //
				.text("Charging power on L1")),

		POWER_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY) //
				.text("Charging power on L2")),

		POWER_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY) //
				.text("Charging power on L3")),

		POWER_TOTAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY) //
				.text("Sum of active charging power")),

		APPLY_CHARGE_POWER_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.WRITE_ONLY) //
				.text("Maximum charging power limit")),

		APPLY_CHARGE_CURRENT_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE).accessMode(AccessMode.WRITE_ONLY) //
				.text("Maximum charging current limit")),;

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
	 * Gets the Channel for {@link ChannelId#APPLY_CHARGE_POWER_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getApplyChargePowerLimitChannel() {
		return this.channel(ChannelId.APPLY_CHARGE_POWER_LIMIT);
	}

	/**
	 * Sets the charge power limit of the EVCS in [W] on
	 * {@link ChannelId#APPLY_CHARGE_POWER_LIMIT} Channel.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setApplyChargePowerLimit(Integer value) throws OpenemsNamedException {
		this.getApplyChargePowerLimitChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#POWER_L1}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getChargePowerL1Channel() {
		return this.channel(ChannelId.POWER_L1);
	}

	/**
	 * Gets the Power on phase L1 in [W]. See {@link ChannelId#POWER_L1}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getChargePowerL1() {
		return this.getChargePowerL1Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#POWER_L2}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getChargePowerL2Channel() {
		return this.channel(ChannelId.POWER_L2);
	}

	/**
	 * Gets the Power on phase L2 in [W]. See {@link ChannelId#POWER_L2}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getChargePowerL2() {
		return this.getChargePowerL2Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#POWER_L3}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getChargePowerL3Channel() {
		return this.channel(ChannelId.POWER_L3);
	}

	/**
	 * Gets the Power on phase L3 in [W]. See {@link ChannelId#POWER_L3}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getChargePowerL3() {
		return this.getChargePowerL3Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#POWER_TOTAL}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getChargePowerTotalChannel() {
		return this.channel(ChannelId.POWER_TOTAL);
	}

	/**
	 * Gets the total charge power on all phases in [W]. See
	 * {@link ChannelId#POWER_TOTAL}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getChargePowerTotal() {
		return this.getChargePowerTotalChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#CHARGE_ENERGY_SESSION}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getChargeEnergySessionChannel() {
		return this.channel(ChannelId.CHARGE_ENERGY_SESSION);
	}

	/**
	 * Gets sum of charged energy for the current session. See
	 * {@link ChannelId#CHARGE_ENERGY_SESSION}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getChargeEnergySession() {
		return this.getChargeEnergySessionChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#LIFE_BIT}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getLifeBitChannel() {
		return this.channel(ChannelId.LIFE_BIT);
	}

	/**
	 * Gets the Life-Bit. See {@link ChannelId#LIFE_BIT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getLifeBit() {
		return this.getLifeBitChannel().value();
	}

	/**
	 * Sets Life-Bit. See {@link ChannelId#LIFE_BIT}.
	 * 
	 * @param value {@link Integer}
	 * @throws OpenemsNamedException on error.
	 */
	public default void setLifeBit(Integer value) throws OpenemsNamedException {
		this.getLifeBitChannel().setNextWriteValue(value);
	}
}
