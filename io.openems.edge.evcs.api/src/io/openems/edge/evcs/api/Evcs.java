package io.openems.edge.evcs.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.component.OpenemsComponent;

@ProviderType
public interface Evcs extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
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
