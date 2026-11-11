package io.openems.edge.timeofusetariff.octopus.dynamic.gridbuy;

import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;

public interface TariffOctopusDynamicGridBuy extends TimeOfUseTariff, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		HTTP_STATUS_CODE(Doc.of(OpenemsType.INTEGER) //
				.text("The HTTP status code of the last request; -1 if no HTTP status is available")), //
		LAST_SUCCESSFUL_UPDATE(Doc.of(OpenemsType.LONG) //
				.unit(Unit.UNIX_TIMESTAMP_SECONDS) //
				.text("Time of the last successful price update")), //
		FORECAST_VALID_UNTIL(Doc.of(OpenemsType.LONG) //
				.unit(Unit.UNIX_TIMESTAMP_SECONDS) //
				.text("End of the last price period of the current forecast")), //
		STATUS_AUTHENTICATION_FAILED(Doc.of(Level.FAULT) //
				.text("Authentication at Octopus Energy failed; no further requests until the configuration is changed")), //
		STATUS_RATE_LIMITED(Doc.of(Level.WARNING) //
				.text("Octopus Energy API rate limit reached; retrying later")), //
		STATUS_UPDATE_FAILED(Doc.of(Level.WARNING) //
				.text("Unable to update prices from Octopus Energy; retrying shortly")), //
		STATUS_CONFIGURATION_INCOMPLETE(Doc.of(Level.FAULT) //
				.text("Email, password or account number is not configured")), //
		STATUS_COUNTRY_NOT_SUPPORTED(Doc.of(Level.FAULT) //
				.text("Configured country is not yet supported/tested; no requests are sent")), //
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
