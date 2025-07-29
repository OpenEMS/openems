package io.openems.edge.evcs.goe.http;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.evcs.goe.api.EvcsGoe;

public interface EvcsGoeHttp extends EvcsGoe {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		CURR_USER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
		);

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