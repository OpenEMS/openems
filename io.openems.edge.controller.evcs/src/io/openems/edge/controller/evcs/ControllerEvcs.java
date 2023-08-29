package io.openems.edge.controller.evcs;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.energy.api.schedulable.Schedulable;

public interface ControllerEvcs extends Schedulable {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		AWAITING_HYSTERESIS(Doc.of(OpenemsType.BOOLEAN)) //
		; //

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