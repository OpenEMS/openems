package io.openems.edge.controller.api.rest.readwrite;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface ControllerApiRestReadWrite extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		API_WORKER_LOG(Doc.of(OpenemsType.STRING) //
				.text("Logs Write-Commands via ApiWorker")); //

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
	 * Gets the Channel for {@link ChannelId#API_WORKER_LOG}.
	 *
	 * @return the Channel
	 */
	public default StringReadChannel getApiWorkerLogChannel() {
		return this.channel(ChannelId.API_WORKER_LOG);
	}

}
