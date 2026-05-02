package io.openems.edge.huawei.pvinverter.smartlogger;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.LongDoc;
import io.openems.edge.common.channel.LongWriteChannel;
import io.openems.edge.common.channel.StringDoc;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

public interface HuaweiSmartloggerPvInverter extends ElectricityMeter, ManagedSymmetricPvInverter {

	enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		MODEL(new StringDoc().accessMode(AccessMode.READ_ONLY)),
		SERIAL_NUMBER(new StringDoc().accessMode(AccessMode.READ_ONLY)),

		TOTAL_ENERGY(new LongDoc().accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.LOW)//
		), //
		DAILY_ENERGY(new LongDoc().accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.LOW)//
		), //
		POWER_GENERATION_TIME(new LongDoc().accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.HOUR)//
				.persistencePriority(PersistencePriority.LOW)//
		), //
		VOLTAGE_L1_L2(new LongDoc().accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.MILLIVOLT)//
				.persistencePriority(PersistencePriority.LOW)//
		), //
		VOLTAGE_L2_L3(new LongDoc().accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.MILLIVOLT)//
				.persistencePriority(PersistencePriority.LOW)//
		), //
		VOLTAGE_L1_L3(new LongDoc().accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.MILLIVOLT)//
				.persistencePriority(PersistencePriority.LOW)//
		), //
		LOCKED(new BooleanDoc().accessMode(AccessMode.READ_ONLY)//
				.persistencePriority(PersistencePriority.MEDIUM)),
		CAPACITY(new LongDoc().accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.LOW)), //
		ACTIVE_POWER_LIMIT_PERCENT(new LongDoc().accessMode(AccessMode.READ_WRITE)//
				.unit(Unit.PERCENT)//
				.persistencePriority(PersistencePriority.MEDIUM)//
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
