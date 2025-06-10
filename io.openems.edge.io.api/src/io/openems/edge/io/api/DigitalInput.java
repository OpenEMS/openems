package io.openems.edge.io.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

@ProviderType
public interface DigitalInput extends OpenemsComponent {

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

	/**
	 * Gets all Output Channels.
	 * 
	 * @return array of {@link BooleanReadChannel}
	 */
	public BooleanReadChannel[] digitalInputChannels();
}
