package io.openems.edge.weather.openmeteo;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.weather.api.Weather;

public interface WeatherOpenMeteo extends Weather, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		HTTP_STATUS_CODE(Doc.of(OpenemsType.INTEGER) //
				.text("The HTTP status code")), //
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
	 * Gets the Channel for {@link ChannelId#HTTP_STATUS_CODE}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getHttpStatusCodeChannel() {
		return this.channel(ChannelId.HTTP_STATUS_CODE);
	}

	/**
	 * Gets the HttpStatusCode. See {@link ChannelId#HTTP_STATUS_CODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getHttpStatusCode() {
		return this.getHttpStatusCodeChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#HTTP_STATUS_CODE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setHttpStatusCode(Integer value) {
		this.getHttpStatusCodeChannel().setNextValue(value);
	}
}
