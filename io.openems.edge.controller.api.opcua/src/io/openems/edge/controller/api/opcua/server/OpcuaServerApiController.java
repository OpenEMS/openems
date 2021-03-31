package io.openems.edge.controller.api.opcua.server;

import io.openems.common.channel.Level;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface OpcuaServerApiController extends Controller, OpenemsComponent {

	public static final int DEFAULT_PORT = 4840;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		UNABLE_TO_START(Doc.of(Level.FAULT) //
				.text("Unable to start OPC UA Api Server")), //
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

}
