package io.openems.edge.huawei.pvinverter.smartlogger;

import static io.openems.common.channel.AccessMode.READ_WRITE;
import static io.openems.common.channel.PersistencePriority.MEDIUM;
import static io.openems.common.channel.Unit.HOUR;
import static io.openems.common.channel.Unit.MILLIVOLT;
import static io.openems.common.channel.Unit.PERCENT;
import static io.openems.common.channel.Unit.WATT;
import static io.openems.common.types.OpenemsType.BOOLEAN;
import static io.openems.common.types.OpenemsType.LONG;
import static io.openems.common.types.OpenemsType.STRING;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.LongWriteChannel;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

public interface HuaweiSmartloggerPvInverter extends ElectricityMeter, ManagedSymmetricPvInverter {

	enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		MODEL(Doc.of(STRING)), //
		SERIAL_NUMBER(Doc.of(STRING)),

		TOTAL_ENERGY(Doc.of(LONG)//
				.unit(WATT)), //
		DAILY_ENERGY(Doc.of(LONG)//
				.unit(WATT)), //
		POWER_GENERATION_TIME(Doc.of(LONG)//
				.unit(HOUR)), //
		VOLTAGE_L1_L2(Doc.of(LONG)//
				.unit(MILLIVOLT)), //
		VOLTAGE_L2_L3(Doc.of(LONG)//
				.unit(MILLIVOLT)), //
		VOLTAGE_L1_L3(Doc.of(LONG)//
				.unit(MILLIVOLT)), //
		LOCKED(Doc.of(BOOLEAN)//
				.persistencePriority(MEDIUM)),
		CAPACITY(Doc.of(LONG)//
				.unit(WATT)), //
		ACTIVE_POWER_LIMIT_PERCENT(Doc.of(LONG)//
				.accessMode(READ_WRITE)//
				.unit(PERCENT)//
				.persistencePriority(MEDIUM)//
				.onInit(channel -> {
					// on each Write to the channel -> set the value
					((LongWriteChannel) channel).onSetNextWrite(channel::setNextValue);
				}));

		private final Doc doc;

		ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}

	}

}
