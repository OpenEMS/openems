package io.openems.edge.phoenixcontact.plcnext.ess;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;

public interface PlcNextEss extends ManagedSymmetricEss, SymmetricEss, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		// IntegerWriteChannels
		SET_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.WRITE_ONLY)//
				.unit(Unit.WATT)), //
		SET_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.WRITE_ONLY)//
				.unit(Unit.VOLT_AMPERE)); //

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
