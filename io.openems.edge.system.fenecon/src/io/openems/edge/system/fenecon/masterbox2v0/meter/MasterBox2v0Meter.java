package io.openems.edge.system.fenecon.masterbox2v0.meter;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.system.fenecon.masterbox2v0.enums.StateEnergyMeter;

public interface MasterBox2v0Meter extends OpenemsComponent, ElectricityMeter {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		TIME_STAMP(Doc.of(OpenemsType.LONG) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH)),

		STATUS(Doc.of(StateEnergyMeter.values()) //
				.accessMode(AccessMode.READ_ONLY) //
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
