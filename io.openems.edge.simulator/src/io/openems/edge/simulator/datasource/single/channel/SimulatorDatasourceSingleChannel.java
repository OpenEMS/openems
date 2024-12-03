package io.openems.edge.simulator.datasource.single.channel;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.simulator.datasource.api.SimulatorDatasource;

public interface SimulatorDatasourceSingleChannel extends SimulatorDatasource, OpenemsComponent, EventHandler {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		DATA(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE))
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
	
	public default IntegerReadChannel getDataChannel() {
		return this.channel(ChannelId.DATA);
	}
	
	public default IntegerWriteChannel getDataWriteChannel() {
		return this.channel(ChannelId.DATA);
	}

}
