package io.openems.edge.example.gruenstrom;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface GruenStromReader extends OpenemsComponent {
	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/*
		 * Report 1
		 */
		GREEN_LEVEL(Doc.of(OpenemsType.STRING) //
				.text("Green Level at the current time")), //
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
	 * Gets the Channel for {@link ChannelId#GREEN_LEVEL}.
	 *
	 * @return the Channel
	 */
	public default StringReadChannel getGreenLevelChannel() {
		return this.channel(ChannelId.GREEN_LEVEL);
	}
	
}
