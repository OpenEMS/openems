package io.openems.edge.edge2edge.ess;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface Edge2EdgeEss extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		MINIMUM_POWER_SET_POINT(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.WATT)), //
		MAXIMUM_POWER_SET_POINT(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.WATT)), //
		REMOTE_NO_OPENEMS(Doc.of(Level.FAULT)), //
		REMOTE_COMPONENT_ID_NOT_FOUND(Doc.of(Level.FAULT)), //

		REMOTE_FAULT(Doc.of(Level.FAULT)), //
		REMOTE_WARNING(Doc.of(Level.WARNING)), //
		REMOTE_INFO(Doc.of(Level.INFO)), //

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

}
