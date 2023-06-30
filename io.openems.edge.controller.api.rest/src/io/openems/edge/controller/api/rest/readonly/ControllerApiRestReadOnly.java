package io.openems.edge.controller.api.rest.readonly;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface ControllerApiRestReadOnly extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
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
