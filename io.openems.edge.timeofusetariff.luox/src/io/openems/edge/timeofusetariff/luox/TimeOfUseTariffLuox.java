package io.openems.edge.timeofusetariff.luox;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.oauth.ConnectionState;

public interface TimeOfUseTariffLuox extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		STATUS_SERVER_ERROR(Doc.of(Level.WARNING) //
				.translationKey(TimeOfUseTariffLuox.class, "TimeOfUseTariffLuox.StatusServerError")), //
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
	 * Internal method to set the 'nextValue' on {@link ConnectionState} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setStatusServerError(boolean value) {
		this.getStatusServerErrorChannel().setNextValue(value);
	}

	/**
	 * Gets the HttpStatusCode value. See {@link ConnectionState}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getStatusServerError() {
		return this.getStatusServerErrorChannel().value();
	}

	/**
	 * Gets the Channel for {@link ConnectionState}.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getStatusServerErrorChannel() {
		return this.channel(ChannelId.STATUS_SERVER_ERROR);
	}

}
