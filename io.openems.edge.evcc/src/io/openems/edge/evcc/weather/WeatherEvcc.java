package io.openems.edge.evcc.weather;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.weather.api.Weather;

public interface WeatherEvcc extends Weather, OpenemsComponent {

	enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		HTTP_STATUS_CODE(
				io.openems.edge.common.channel.Doc.of(OpenemsType.INTEGER).text("HTTP status code from EVCC API"));

		private final io.openems.edge.common.channel.Doc doc;

		private ChannelId(io.openems.edge.common.channel.Doc doc) {
			this.doc = doc;
		}

		@Override
		public io.openems.edge.common.channel.Doc doc() {
			return this.doc;
		}
	}

	default Channel<Integer> getHttpStatusCodeChannel() {
		return this.channel(ChannelId.HTTP_STATUS_CODE);
	}

	/**
	* Sets the current HTTP status code retrieved from the EVCC API.
	*
	* @param value the HTTP status code to set, may be {@code null} if unavailable
	*/
	default void _setHttpStatusCode(Integer value) {
		this.getHttpStatusCodeChannel().setNextValue(value);
	}
}
