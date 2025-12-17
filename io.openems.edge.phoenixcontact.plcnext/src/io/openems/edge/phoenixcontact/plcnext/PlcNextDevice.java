package io.openems.edge.phoenixcontact.plcnext;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;

public interface PlcNextDevice extends ElectricityMeter, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		LINE_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIVOLT)//
				.persistencePriority(PersistencePriority.HIGH)),
		LINE_VOLTAGE_L1(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIVOLT)//
				.persistencePriority(PersistencePriority.HIGH)),
		LINE_VOLTAGE_L2(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIVOLT)//
				.persistencePriority(PersistencePriority.HIGH)),
		LINE_VOLTAGE_L3(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIVOLT)//
				.persistencePriority(PersistencePriority.HIGH)),
		NEUTRAL_CURRENT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIAMPERE)//
				.persistencePriority(PersistencePriority.HIGH)),
		APPARENT_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)),
		POWER_FACTOR(Doc.of(OpenemsType.DOUBLE)//
				.unit(Unit.NONE)//
				.persistencePriority(PersistencePriority.HIGH));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

}
