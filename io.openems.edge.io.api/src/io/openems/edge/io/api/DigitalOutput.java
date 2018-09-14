package io.openems.edge.io.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.component.OpenemsComponent;

@ProviderType
public interface DigitalOutput extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		;
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets all Output Channels
	 */
	public WriteChannel<Boolean>[] digitalOutputChannels();
}
