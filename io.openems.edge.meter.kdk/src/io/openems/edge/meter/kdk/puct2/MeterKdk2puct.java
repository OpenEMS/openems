package io.openems.edge.meter.kdk.puct2;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.meter.api.ElectricityMeter;

public interface MeterKdk2puct extends ElectricityMeter, OpenemsComponent, ModbusSlave {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		PRIMARY_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE)), //
		SECONDARY_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE)), //
		SOFTWARE_VERSION(Doc.of(OpenemsType.FLOAT)), //
		// It is used in KDK meter to multiply with current and power values read from
		// registers, to get actual current and power.
		CT_RATIO(Doc.of(OpenemsType.INTEGER)
				.text("It is the ratio of primary current input to secondary current output at full load.")), //
		METER_CURRENT_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		METER_CURRENT_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		METER_CURRENT_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		METER_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIWATT)), //
		METER_ACTIVE_POWER_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIWATT)), //
		METER_ACTIVE_POWER_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIWATT)), //
		METER_ACTIVE_POWER_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIWATT)), //
		METER_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIWATT)), //
		METER_REACTIVE_POWER_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIWATT)), //
		METER_REACTIVE_POWER_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIWATT)), //
		METER_REACTIVE_POWER_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIWATT)),
		SOFTWARE_VERSION_CHECK_SUM(Doc.of(OpenemsType.LONG)) //
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
	 * Gets the Channel for {@link ChannelId#PRIMARY_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getPrimaryCurrentChannel() {
		return this.channel(ChannelId.PRIMARY_CURRENT);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SECONDARY_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getSecondaryCurrentChannel() {
		return this.channel(ChannelId.SECONDARY_CURRENT);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CT_RATIO}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getCtRatioChannel() {
		return this.channel(ChannelId.CT_RATIO);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#CT_RATIO} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCtRatio(Integer value) {
		this.getCtRatioChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SOFTWARE_VERSION_CHECK_SUM}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getSofwareVersionCheckSumChannel() {
		return this.channel(ChannelId.SOFTWARE_VERSION_CHECK_SUM);
	}

	/**
	 * Gets the Channel for {@link ChannelId#METER_CURRENT_L1}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getKdkMeterCurrentL1Channel() {
		return this.channel(ChannelId.METER_CURRENT_L1);
	}

	/**
	 * Gets the Channel for {@link ChannelId#METER_CURRENT_L2}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getKdkMeterCurrentL2Channel() {
		return this.channel(ChannelId.METER_CURRENT_L2);
	}

	/**
	 * Gets the Channel for {@link ChannelId#METER_CURRENT_L3}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getKdkMeterCurrentL3Channel() {
		return this.channel(ChannelId.METER_CURRENT_L3);
	}

	/**
	 * Gets the Channel for {@link ChannelId#METER_ACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getKdkMeterActivePowerChannel() {
		return this.channel(ChannelId.METER_ACTIVE_POWER);
	}

	/**
	 * Gets the Channel for {@link ChannelId#METER_ACTIVE_POWER_L1}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getKdkMeterActivePowerL1Channel() {
		return this.channel(ChannelId.METER_ACTIVE_POWER_L1);
	}

	/**
	 * Gets the Channel for {@link ChannelId#METER_ACTIVE_POWER_L2}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getKdkMeterActivePowerL2Channel() {
		return this.channel(ChannelId.METER_ACTIVE_POWER_L2);
	}

	/**
	 * Gets the Channel for {@link ChannelId#METER_ACTIVE_POWER_L3}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getKdkMeterActivePowerL3Channel() {
		return this.channel(ChannelId.METER_ACTIVE_POWER_L3);
	}

	/**
	 * Gets the Channel for {@link ChannelId#METER_REACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getKdkMeterReactivePowerChannel() {
		return this.channel(ChannelId.METER_REACTIVE_POWER);
	}

	/**
	 * Gets the Channel for {@link ChannelId#METER_REACTIVE_POWER_L1}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getKdkMeterReactivePowerL1Channel() {
		return this.channel(ChannelId.METER_REACTIVE_POWER_L1);
	}

	/**
	 * Gets the Channel for {@link ChannelId#METER_REACTIVE_POWER_L2}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getKdkMeterReactivePowerL2Channel() {
		return this.channel(ChannelId.METER_REACTIVE_POWER_L2);
	}

	/**
	 * Gets the Channel for {@link ChannelId#METER_REACTIVE_POWER_L3}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getKdkMeterReactivePowerL3Channel() {
		return this.channel(ChannelId.METER_REACTIVE_POWER_L3);
	}

}
