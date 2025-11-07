package io.openems.edge.weather.openmeteo;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.weather.api.Weather;

public interface WeatherOpenMeteo extends Weather, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		HTTP_STATUS_CODE(Doc.of(OpenemsType.INTEGER)//
				.text("The HTTP status code")), //

		CURRENT_TEMPERATURE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Current temperature in degrees Celsius.")), //

		CURRENT_WEATHER_CODE(Doc.of(OpenemsType.INTEGER)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Current weather condition code.")), //

		TODAYS_SUNSHINE_DURATION(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.HOUR)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Today's sunshine duration in hours.")), //

		TODAYS_MIN_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Today's minimum temperature in degrees Celsius.")), //

		TODAYS_MAX_TEMPERATURE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Today's maximum temperature in degrees Celsius.")), //

		TEMPERATURE_IN_4H(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Temperature in 4 hours in degrees Celsius.")), //

		WEATHER_CODE_IN_4H(Doc.of(OpenemsType.INTEGER)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Weather condition code in 4 hours.")), //

		TEMPERATURE_IN_8H(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Temperature in 8 hours in degrees Celsius.")), //

		WEATHER_CODE_IN_8H(Doc.of(OpenemsType.INTEGER)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Weather condition code in 8 hours.")), //

		TEMPERATURE_IN_12H(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Temperature in 12 hours in degrees Celsius.")), //

		WEATHER_CODE_IN_12H(Doc.of(OpenemsType.INTEGER)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Weather condition code in 12 hours.")), //

		TEMPERATURE_IN_16H(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Temperature in 16 hours in degrees Celsius.")), //

		WEATHER_CODE_IN_16H(Doc.of(OpenemsType.INTEGER)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Weather condition code in 16 hours.")), //

		TEMPERATURE_IN_20H(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Temperature in 20 hours in degrees Celsius.")), //

		WEATHER_CODE_IN_20H(Doc.of(OpenemsType.INTEGER)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Weather condition code in 20 hours.")), //

		TEMPERATURE_IN_24H(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Temperature in 24 hours in degrees Celsius.")), //

		WEATHER_CODE_IN_24H(Doc.of(OpenemsType.INTEGER)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Weather condition code in 24 hours.")), //

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

	/**
	 * Gets the Channel for {@link ChannelId#CURRENT_TEMPERATURE}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getCurrentTemperatureChannel() {
		return this.channel(ChannelId.CURRENT_TEMPERATURE);
	}

	/**
	 * Gets the CurrentTemperature. See {@link ChannelId#CURRENT_TEMPERATURE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCurrentTemperature() {
		return this.getCurrentTemperatureChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CURRENT_TEMPERATURE} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCurrentTemperature(Integer value) {
		this.getCurrentTemperatureChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CURRENT_WEATHER_CODE}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getCurrentWeatherCodeChannel() {
		return this.channel(ChannelId.CURRENT_WEATHER_CODE);
	}

	/**
	 * Gets the CurrentWeatherCode. See {@link ChannelId#CURRENT_WEATHER_CODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCurrentWeatherCode() {
		return this.getCurrentWeatherCodeChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CURRENT_WEATHER_CODE} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCurrentWeatherCode(Integer value) {
		this.getCurrentWeatherCodeChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#TODAYS_SUNSHINE_DURATION}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getTodaysSunshineDurationChannel() {
		return this.channel(ChannelId.TODAYS_SUNSHINE_DURATION);
	}

	/**
	 * Gets the TodaysSunshineDuration. See
	 * {@link ChannelId#TODAYS_SUNSHINE_DURATION}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTodaysSunshineDuration() {
		return this.getTodaysSunshineDurationChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#TODAYS_SUNSHINE_DURATION} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTodaysSunshineDuration(Integer value) {
		this.getTodaysSunshineDurationChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#TODAYS_MIN_TEMPERATURE}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getTodaysMinTemperatureChannel() {
		return this.channel(ChannelId.TODAYS_MIN_TEMPERATURE);
	}

	/**
	 * Gets the TodaysMinTemperature. See {@link ChannelId#TODAYS_MIN_TEMPERATURE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTodaysMinTemperature() {
		return this.getTodaysMinTemperatureChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#TODAYS_MIN_TEMPERATURE} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTodaysMinTemperature(Integer value) {
		this.getTodaysMinTemperatureChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#TODAYS_MAX_TEMPERATURE}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getTodaysMaxTemperatureChannel() {
		return this.channel(ChannelId.TODAYS_MAX_TEMPERATURE);
	}

	/**
	 * Gets the TodaysMaxTemperature. See {@link ChannelId#TODAYS_MAX_TEMPERATURE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTodaysMaxTemperature() {
		return this.getTodaysMaxTemperatureChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#TODAYS_MAX_TEMPERATURE} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTodaysMaxTemperature(Integer value) {
		this.getTodaysMaxTemperatureChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#TEMPERATURE_IN_4H}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getTemperatureIn4hChannel() {
		return this.channel(ChannelId.TEMPERATURE_IN_4H);
	}

	/**
	 * Gets the TemperatureIn4h. See {@link ChannelId#TEMPERATURE_IN_4H}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTemperatureIn4h() {
		return this.getTemperatureIn4hChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#TEMPERATURE_IN_4H}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTemperatureIn4h(Integer value) {
		this.getTemperatureIn4hChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#WEATHER_CODE_IN_4H}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getWeatherCodeIn4hChannel() {
		return this.channel(ChannelId.WEATHER_CODE_IN_4H);
	}

	/**
	 * Gets the WeatherCodeIn4h. See {@link ChannelId#WEATHER_CODE_IN_4H}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getWeatherCodeIn4h() {
		return this.getWeatherCodeIn4hChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#WEATHER_CODE_IN_4H} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setWeatherCodeIn4h(Integer value) {
		this.getWeatherCodeIn4hChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#TEMPERATURE_IN_8H}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getTemperatureIn8hChannel() {
		return this.channel(ChannelId.TEMPERATURE_IN_8H);
	}

	/**
	 * Gets the TemperatureIn8h. See {@link ChannelId#TEMPERATURE_IN_8H}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTemperatureIn8h() {
		return this.getTemperatureIn8hChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#TEMPERATURE_IN_8H}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTemperatureIn8h(Integer value) {
		this.getTemperatureIn8hChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#WEATHER_CODE_IN_8H}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getWeatherCodeIn8hChannel() {
		return this.channel(ChannelId.WEATHER_CODE_IN_8H);
	}

	/**
	 * Gets the WeatherCodeIn8h. See {@link ChannelId#WEATHER_CODE_IN_8H}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getWeatherCodeIn8h() {
		return this.getWeatherCodeIn8hChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#WEATHER_CODE_IN_8H} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setWeatherCodeIn8h(Integer value) {
		this.getWeatherCodeIn8hChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#TEMPERATURE_IN_12H}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getTemperatureIn12hChannel() {
		return this.channel(ChannelId.TEMPERATURE_IN_12H);
	}

	/**
	 * Gets the TemperatureIn12h. See {@link ChannelId#TEMPERATURE_IN_12H}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTemperatureIn12h() {
		return this.getTemperatureIn12hChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#TEMPERATURE_IN_12H} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTemperatureIn12h(Integer value) {
		this.getTemperatureIn12hChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#WEATHER_CODE_IN_12H}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getWeatherCodeIn12hChannel() {
		return this.channel(ChannelId.WEATHER_CODE_IN_12H);
	}

	/**
	 * Gets the WeatherCodeIn12h. See {@link ChannelId#WEATHER_CODE_IN_12H}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getWeatherCodeIn12h() {
		return this.getWeatherCodeIn12hChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#WEATHER_CODE_IN_12H} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setWeatherCodeIn12h(Integer value) {
		this.getWeatherCodeIn12hChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#TEMPERATURE_IN_16H}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getTemperatureIn16hChannel() {
		return this.channel(ChannelId.TEMPERATURE_IN_16H);
	}

	/**
	 * Gets the TemperatureIn16h. See {@link ChannelId#TEMPERATURE_IN_16H}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTemperatureIn16h() {
		return this.getTemperatureIn16hChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#TEMPERATURE_IN_16H} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTemperatureIn16h(Integer value) {
		this.getTemperatureIn16hChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#WEATHER_CODE_IN_16H}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getWeatherCodeIn16hChannel() {
		return this.channel(ChannelId.WEATHER_CODE_IN_16H);
	}

	/**
	 * Gets the WeatherCodeIn16h. See {@link ChannelId#WEATHER_CODE_IN_16H}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getWeatherCodeIn16h() {
		return this.getWeatherCodeIn16hChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#WEATHER_CODE_IN_16H} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setWeatherCodeIn16h(Integer value) {
		this.getWeatherCodeIn16hChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#TEMPERATURE_IN_20H}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getTemperatureIn20hChannel() {
		return this.channel(ChannelId.TEMPERATURE_IN_20H);
	}

	/**
	 * Gets the TemperatureIn20h. See {@link ChannelId#TEMPERATURE_IN_20H}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTemperatureIn20h() {
		return this.getTemperatureIn20hChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#TEMPERATURE_IN_20H} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTemperatureIn20h(Integer value) {
		this.getTemperatureIn20hChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#WEATHER_CODE_IN_20H}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getWeatherCodeIn20hChannel() {
		return this.channel(ChannelId.WEATHER_CODE_IN_20H);
	}

	/**
	 * Gets the WeatherCodeIn20h. See {@link ChannelId#WEATHER_CODE_IN_20H}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getWeatherCodeIn20h() {
		return this.getWeatherCodeIn20hChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#WEATHER_CODE_IN_20H} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setWeatherCodeIn20h(Integer value) {
		this.getWeatherCodeIn20hChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#TEMPERATURE_IN_24H}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getTemperatureIn24hChannel() {
		return this.channel(ChannelId.TEMPERATURE_IN_24H);
	}

	/**
	 * Gets the TemperatureIn24h. See {@link ChannelId#TEMPERATURE_IN_24H}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTemperatureIn24h() {
		return this.getTemperatureIn24hChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#TEMPERATURE_IN_24H} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTemperatureIn24h(Integer value) {
		this.getTemperatureIn24hChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#WEATHER_CODE_IN_24H}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getWeatherCodeIn24hChannel() {
		return this.channel(ChannelId.WEATHER_CODE_IN_24H);
	}

	/**
	 * Gets the WeatherCodeIn24h. See {@link ChannelId#WEATHER_CODE_IN_24H}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getWeatherCodeIn24h() {
		return this.getWeatherCodeIn24hChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#WEATHER_CODE_IN_24H} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setWeatherCodeIn24h(Integer value) {
		this.getWeatherCodeIn24hChannel().setNextValue(value);
	}
}
