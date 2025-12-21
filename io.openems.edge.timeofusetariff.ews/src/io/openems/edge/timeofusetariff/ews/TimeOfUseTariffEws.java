package io.openems.edge.timeofusetariff.ews;

import io.openems.common.channel.Level;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;

public interface TimeOfUseTariffEws extends TimeOfUseTariff, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		HTTP_STATUS_CODE(Doc.of(OpenemsType.INTEGER) //
				.text("The HTTP status code")), //
		STATUS_TIMEOUT(Doc.of(Level.WARNING) //
				.text("Unable to update prices from Ews: timout while reading from server")), //
		STATUS_AUTHENTICATION_FAILED(Doc.of(Level.WARNING) //
				.text("Unable to update prices from Ews: access token authentication failed")), //
		STATUS_SERVER_ERROR(Doc.of(Level.WARNING) //
				.text("Unable to update prices from Ews: unexpected server error")) //
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
