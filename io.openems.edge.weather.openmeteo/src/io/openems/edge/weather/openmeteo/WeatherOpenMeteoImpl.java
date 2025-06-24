package io.openems.edge.weather.openmeteo;

import static com.google.common.collect.ImmutableSortedMap.toImmutableSortedMap;
import static io.openems.common.utils.DateUtils.roundDownToQuarter;

import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttpFactory;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.meta.Coordinates;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.weather.api.Weather;
import io.openems.edge.weather.api.WeatherData;
import io.openems.edge.weather.api.WeatherSnapshot;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Weather.OpenMeteo", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class WeatherOpenMeteoImpl extends AbstractOpenemsComponent
		implements WeatherOpenMeteo, OpenemsComponent, Weather {

	private static final long FORECAST_DAYS = 3;
	private static final String[] WEATHER_VARIABLES = { //
			"shortwave_radiation", //
			"direct_normal_irradiance", //
			"temperature_2m", //
			"weather_code", //
	};

	private Config config = null;

	@Reference
	private ComponentManager componentManager;

	@Reference
	private BridgeHttpFactory httpBridgeFactory;

	private BridgeHttp httpBridge;
	private HistoricalWeatherService historicalWeatherService;
	private WeatherForecastService weatherForecastService;

	private Meta meta;
	private Optional<Coordinates> coordinates;

	/**
	 * Binds the {@link Meta} object.
	 * 
	 * @param meta the Meta object to bind
	 */
	@Reference
	public void bindMeta(Meta meta) {
		this.meta = meta;
		this.coordinates = meta.getCoordinates();
	}

	/**
	 * Updates the bound {@link Meta} object and resubscribes to the weather
	 * forecast if coordinates changed.
	 * 
	 * @param updatedMeta the updated Meta object
	 */
	public void updatedMeta(Meta updatedMeta) {
		if (!this.coordinates.equals(updatedMeta.getCoordinates())) {
			// Subscribe to weather forecast
			this.weatherForecastService.subscribeToWeatherForecast(//
					new OpenMeteoDelayTimeProvider(this.componentManager.getClock()), //
					this.channel(WeatherOpenMeteo.ChannelId.HTTP_STATUS_CODE), //
					updatedMeta.getCoordinates(), //
					this.componentManager.getClock().getZone()//
			);

			this.coordinates = updatedMeta.getCoordinates();
		}
	}

	/**
	 * Unbinds the {@link Meta} object.
	 * 
	 * @param meta the Meta object to unbind
	 */
	public void unbindMeta(Meta meta) {
		this.meta = null;
	}

	public WeatherOpenMeteoImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				WeatherOpenMeteo.ChannelId.values(), //
				Weather.ChannelId.values()//
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;

		if (!this.config.enabled()) {
			return;
		}

		this.httpBridge = this.httpBridgeFactory.get();
		this.historicalWeatherService = new HistoricalWeatherService(this.httpBridge, WEATHER_VARIABLES);
		this.weatherForecastService = new WeatherForecastService(this.httpBridge, WEATHER_VARIABLES, FORECAST_DAYS);

		// Subscribe to weather forecast
		this.weatherForecastService.subscribeToWeatherForecast(//
				new OpenMeteoDelayTimeProvider(this.componentManager.getClock()), //
				this.channel(WeatherOpenMeteo.ChannelId.HTTP_STATUS_CODE), //
				this.meta.getCoordinates(), //
				this.componentManager.getClock().getZone()//
		);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.httpBridgeFactory.unget(this.httpBridge);
		this.httpBridge = null;
	}

	@Override
	public WeatherSnapshot getCurrentWeather() {
		if (this.weatherForecastService == null) {
			throw new IllegalStateException(
					"WeatherForecastService is not available. This may happen if the weather API component is not enabled or configured");
		}

		return this.weatherForecastService.getWeatherForecast()
				.getAtOrElse(roundDownToQuarter(ZonedDateTime.now(this.componentManager.getClock())), null);
	}

	@Override
	public CompletableFuture<WeatherData> getHistoricalWeather(ZonedDateTime dateFrom, ZonedDateTime dateTo) {
		if (this.historicalWeatherService == null) {
			return CompletableFuture.failedFuture(new IllegalStateException(
					"HistoricalWeatherService is not available. This may happen if the weather API component is not enabled or configured"));
		}

		return this.historicalWeatherService.getWeatherData(//
				this.meta.getCoordinates(), //
				dateFrom, //
				dateTo, //
				this.componentManager.getClock().getZone()//
		);
	}

	@Override
	public WeatherData getWeatherForecast() {
		if (this.weatherForecastService == null) {
			throw new IllegalStateException(
					"WeatherForecastService is not available. This may happen if the weather API component is not enabled or configured");
		}

		final var weatherData = this.weatherForecastService.getWeatherForecast();
		if (weatherData.isEmpty()) {
			return WeatherData.EMPTY_WEATHER_DATA;
		}

		final var now = roundDownToQuarter(ZonedDateTime.now(this.componentManager.getClock()));
		if (weatherData.getFirstTime().isEqual(now)) {
			return weatherData;
		}

		final var newMap = weatherData.toMapWithAllQuarters().entrySet().stream() //
				.filter(e -> !now.isAfter(e.getKey())) //
				.collect(toImmutableSortedMap(Comparator.naturalOrder(), Entry::getKey, Entry::getValue));

		if (newMap.isEmpty()) {
			return WeatherData.EMPTY_WEATHER_DATA;
		}

		return WeatherData.from(newMap);
	}

}
