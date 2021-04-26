package io.openems.edge.controller.api.mqpp;

import org.osgi.service.event.EventHandler;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface MqttApiController extends Controller, OpenemsComponent, EventHandler {

	public final static String TOPIC_PREFIX = "edge/%s/";
	public final static String TOPIC_CHANNEL_PREFIX = "channel/";
	public final static String TOPIC_CHANNEL_LAST_UPDATE = "lastUpdate";
	public final static String TOPIC_EDGE_CONFIG = "edgeConfig/";

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
