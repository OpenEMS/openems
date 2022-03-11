package io.openems.edge.timedata.api;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.timedata.CommonTimedataService;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

@ProviderType
public interface Timedata extends CommonTimedataService, OpenemsComponent {

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
	 * Gets the latest known value for the given {@link ChannelAddress}.
	 *
	 * @param channelAddress the ChannelAddress to be queried
	 * @return the latest known value or Empty
	 */
	public CompletableFuture<Optional<Object>> getLatestValue(ChannelAddress channelAddress);

}
