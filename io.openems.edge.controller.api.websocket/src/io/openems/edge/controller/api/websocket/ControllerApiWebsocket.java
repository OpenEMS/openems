package io.openems.edge.controller.api.websocket;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface ControllerApiWebsocket extends Controller, OpenemsComponent, EventHandler {

	public static final String EDGE_ID = "0";
	public static final String EDGE_COMMENT = "";
	public static final String EDGE_PRODUCT_TYPE = "";
	public static final Level SUM_STATE = Level.OK;
	public static final int DEFAULT_PORT = 8075;

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
