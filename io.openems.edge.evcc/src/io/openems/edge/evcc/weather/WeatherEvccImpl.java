package io.openems.edge.evcc.weather;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
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
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttpFactory;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.weather.api.DailyWeatherSnapshot;
import io.openems.edge.weather.api.HourlyWeatherSnapshot;
import io.openems.edge.weather.api.QuarterlyWeatherSnapshot;
import io.openems.edge.weather.api.Weather;

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

		this.forecastService = new EvccForecastService(config.apiUrl(), this.httpBridge, this.clock, config);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		if (this.forecastService != null) {
			this.forecastService.cleanup();
			this.forecastService = null;
		}
		if (this.httpBridge != null) {
			this.httpBridgeFactory.unget(this.httpBridge);
			this.httpBridge = null;
		}
	}

	@Override
	public QuarterlyWeatherSnapshot getCurrentWeather() throws OpenemsException {
		if (this.forecastService == null) {
			throw new OpenemsException(
					"WeatherForecastService is not available. This may happen if the weather API component is not enabled or configured");
		}

		// Validate data freshness
		this.validateForecastData();

		final var now = roundDownToQuarter(ZonedDateTime.now(this.componentManager.getClock()));
		final var forecast = this.forecastService.getWeatherForecast();

		return forecast.stream() //
				.filter(snapshot -> snapshot.datetime().isEqual(now)) //
				.findFirst() //
				.orElse(null);
	}

	@Override
	public CompletableFuture<List<QuarterlyWeatherSnapshot>> getHistoricalWeather(//
			LocalDate dateFrom, //
			LocalDate dateTo, //
			ZoneId zone) {
		return CompletableFuture
				.failedFuture(new OpenemsException("Historical weather data is not available from EVCC API"));
	}

	@Override
	public List<QuarterlyWeatherSnapshot> getQuarterlyWeatherForecast(int forecastQuarters) throws OpenemsException {
		if (this.forecastService == null) {
			throw new OpenemsException(
					"WeatherForecastService is not available. This may happen if the weather API component is not enabled or configured");
		}

		// Validate data freshness
		this.validateForecastData();

		final var now = roundDownToQuarter(ZonedDateTime.now(this.componentManager.getClock()));
		final var forecast = this.forecastService.getWeatherForecast();

		return forecast.stream() //
				.filter(snapshot -> !snapshot.datetime().isBefore(now)) //
				.limit(forecastQuarters) //
				.toList();
	}

	@Override
	public List<HourlyWeatherSnapshot> getHourlyWeatherForecast(int forecastHours) throws OpenemsException {
		// EVCC only provides quarterly data, no hourly aggregation available
		return new ArrayList<>();
	}

	@Override
	public List<DailyWeatherSnapshot> getDailyWeatherForecast() throws OpenemsException {
		// EVCC only provides quarterly data, no daily aggregation available
		return new ArrayList<>();
	}

	/**
	 * Validates that the forecast data is available and not stale.
	 *
	 * @throws OpenemsException if the forecast data is unavailable or outdated
	 */
	private void validateForecastData() throws OpenemsException {
		final var lastUpdate = this.forecastService.getLastUpdate();
		final var forecast = this.forecastService.getWeatherForecast();

		if (forecast.isEmpty()) {
			throw new OpenemsException("Weather forecast data is not available");
		}

		if (lastUpdate == null || lastUpdate.isBefore(Instant.now(this.clock).minus(Duration.ofDays(1)))) {
			throw new OpenemsException("Weather forecast data is outdated (last update: " + lastUpdate + ")");
		}
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
