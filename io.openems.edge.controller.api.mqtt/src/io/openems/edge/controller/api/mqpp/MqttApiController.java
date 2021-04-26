package io.openems.edge.controller.api.mqpp;

import org.ops4j.pax.logging.spi.PaxAppender;
import org.osgi.service.event.EventHandler;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface MqttApiController extends Controller, OpenemsComponent, PaxAppender, EventHandler {

	public final static String TOPIC_PREFIX = "edge/%s/";
	public final static String TOPIC_CHANNEL_PREFIX = "channel/";
	public final static String TOPIC_CHANNEL_LAST_UPDATE = "lastUpdate";
	public final static String TOPIC_EDGE_CONFIG = "edgeConfig/";

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
