package io.openems.edge.evcc.api.gridtariff;

import io.openems.common.channel.Level;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;

public interface TimeOfUseGridTariffEvcc extends TimeOfUseTariff, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		HTTP_STATUS_CODE(Doc.of(OpenemsType.INTEGER) //
				.text("The HTTP status code")), //
		STATUS_TIMEOUT(Doc.of(Level.WARNING) //
				.text("Unable to update prices: timout while reading from server")), //
		STATUS_SERVER_ERROR(Doc.of(Level.WARNING) //
				.text("Unable to update prices: unexpected server error")), //
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
