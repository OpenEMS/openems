package io.openems.edge.huawei.pvinverter;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

public interface HuaweiPvInverter extends ManagedSymmetricPvInverter {

	enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		VOLTAGE_L1_L2(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIVOLT)), //
		VOLTAGE_L2_L3(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIVOLT)), //
		VOLTAGE_L3_L1(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIVOLT)), //
		INPUT_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)), //

		HUAWEI_ACTIVE_POWER_LIMIT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.accessMode(AccessMode.READ_WRITE)), //
		ACTIVE_POWER_PERCENTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.accessMode(AccessMode.READ_WRITE)) //
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

	public default IntegerWriteChannel getHuaweiActivePowerLimitChannel() {
		return this.channel(ChannelId.HUAWEI_ACTIVE_POWER_LIMIT);
	}

	public default void setHuaweiActivePowerLimit(Integer value) throws OpenemsNamedException {
		this.getHuaweiActivePowerLimitChannel().setNextWriteValue(value);
	}

	public default IntegerWriteChannel getActivePowerPercentageChannel() {
		return this.channel(ChannelId.ACTIVE_POWER_PERCENTAGE);
	}

	public default void setActivePowerPercentage(Integer value) throws OpenemsNamedException {
		this.getActivePowerPercentageChannel().setNextWriteValue(value);
	}

}
