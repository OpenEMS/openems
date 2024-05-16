package io.openems.edge.controller.fnnstb;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface ControllerFnnstb extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		CONNECTION_STATUS(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)) //
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

	public default BooleanWriteChannel getConnectionStatusChannel() {
		return this.channel(ChannelId.CONNECTION_STATUS);
	}

	public default void setConnectionStatus(boolean value) {
		this.getConnectionStatusChannel().setNextValue(value);
	}

}
