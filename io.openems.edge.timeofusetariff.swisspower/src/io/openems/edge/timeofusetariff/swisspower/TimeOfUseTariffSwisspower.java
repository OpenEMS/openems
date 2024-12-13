package io.openems.edge.timeofusetariff.swisspower;

import io.openems.common.channel.Level;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;

public interface TimeOfUseTariffSwisspower extends TimeOfUseTariff, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		HTTP_STATUS_CODE(Doc.of(OpenemsType.INTEGER)//
				.text("Displays the HTTP status code")), //
		STATUS_INVALID_FIELDS(Doc.of(Level.WARNING) //
				.text("Unable to update prices: please check your access token and metering code")), //
		/**
		 * Should never happen. Only happens if the request has missing fields or wrong
		 * format of timestamps.
		 */
		STATUS_BAD_REQUEST(Doc.of(Level.FAULT) //
				.text("Unable to update prices: internal error")), //
		STATUS_READ_TIMEOUT(Doc.of(Level.WARNING) //
				.text("Unable to update prices: read timeout error")), //
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
