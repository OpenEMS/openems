package io.openems.edge.phoenixcontact.plcnext;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;

public interface PlcNextDevice extends ElectricityMeter, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		VOLTAGE_LINE_L12(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIVOLT)//
				.persistencePriority(PersistencePriority.HIGH)),
		VOLTAGE_LINE_L23(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIVOLT)//
				.persistencePriority(PersistencePriority.HIGH)),
		VOLTAGE_LINE_L31(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIVOLT)//
				.persistencePriority(PersistencePriority.HIGH)),
		CURRENT_NEUTRAL(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIAMPERE)//
				.persistencePriority(PersistencePriority.HIGH)),
		APPARENT_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)),
		APPARENT_POWER_L1(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)),
		APPARENT_POWER_L2(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)),
		APPARENT_POWER_L3(Doc.of(OpenemsType.INTEGER)//
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
