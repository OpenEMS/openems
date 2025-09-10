package io.openems.edge.evcc.weather;

import static com.google.common.collect.ImmutableSortedMap.toImmutableSortedMap;
import static io.openems.common.utils.DateUtils.roundDownToQuarter;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttpFactory;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.weather.api.Weather;
import io.openems.edge.weather.api.WeatherData;
import io.openems.edge.weather.api.WeatherSnapshot;

@Designate(ocd = Config.class)
@Component(name = "Weather.Evcc", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class WeatherEvccImpl extends AbstractOpenemsComponent implements WeatherEvcc, OpenemsComponent, Weather {

	private static final Logger log = LoggerFactory.getLogger(EvccForecastService.class);

	@Reference
	private ComponentManager componentManager;

	@Reference
	private BridgeHttpFactory httpBridgeFactory;

	private Clock clock;

	private EvccForecastService forecastService;
	private BridgeHttp httpBridge;

	public WeatherEvccImpl() throws OpenemsNamedException {
		super(OpenemsComponent.ChannelId.values(), WeatherEvcc.ChannelId.values(), Weather.ChannelId.values());
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());

		this.httpBridge = this.httpBridgeFactory.get();
		this.clock = this.componentManager.getClock();

		if (!config.enabled()) {
			return;
		}

		this.forecastService = new EvccForecastService(config.apiUrl(), this.httpBridge, this.clock);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		if (this.httpBridge != null) {
			this.httpBridgeFactory.unget(this.httpBridge);
			this.httpBridge = null;
		}
		this.forecastService = null;
	}

	@Override
	public WeatherSnapshot getCurrentWeather() {
		log.warn("getCurrentWeather");
		if (this.forecastService == null) {
			throw new IllegalStateException(
					"WeatherForecastService is not available. This may happen if the weather API component is not enabled or configured");
		}
		return this.forecastService.getWeatherForecast()
				.getAtOrElse(roundDownToQuarter(ZonedDateTime.now(this.componentManager.getClock())), null);
	}

	@Override
	public CompletableFuture<WeatherData> getHistoricalWeather(ZonedDateTime dateFrom, ZonedDateTime dateTo) {
		return CompletableFuture.failedFuture(new IllegalStateException("HistoricalWeatherService is not available."));
	}

	@Override
	public WeatherData getWeatherForecast() {
		if (this.forecastService == null) {
			throw new IllegalStateException(
					"WeatherForecastService is not available. This may happen if the weather API component is not enabled or configured");
		}

		final var weatherData = this.forecastService.getWeatherForecast();
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

	/**
	* Returns the underlying {@link EvccForecastService} instance (specially for unit testing)
	*
	* <p>
	* This service is responsible for retrieving and parsing forecast data from
	* the EVCC API. The value may be {@code null} if this component is not
	* activated or has been deactivated.
	*
	* @return the {@link EvccForecastService} instance, or {@code null} if not available
	*/
	public EvccForecastService getForecastService() {
		return this.forecastService;
	}
}
