package io.openems.edge.weather.openmeteo;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import com.google.common.annotations.VisibleForTesting;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.time.HttpBridgeTimeServiceDefinition;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Role;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.EdgeGuards;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.meta.types.Coordinates;
import io.openems.edge.weather.api.DailyWeatherSnapshot;
import io.openems.edge.weather.api.HourlyWeatherSnapshot;
import io.openems.edge.weather.api.QuarterlyWeatherSnapshot;
import io.openems.edge.weather.api.Weather;
import io.openems.edge.weather.openmeteo.data.DefaultWeatherDataParser;
import io.openems.edge.weather.openmeteo.forecast.WeatherForecastDelayTimeProvider;
import io.openems.edge.weather.openmeteo.forecast.WeatherForecastPersistenceService;
import io.openems.edge.weather.openmeteo.forecast.WeatherForecastService;
import io.openems.edge.weather.openmeteo.historical.HistoricalWeatherService;
import io.openems.edge.weather.openmeteo.jsonrpc.DailyWeatherForecastEndpoint;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Weather.OpenMeteo", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class WeatherOpenMeteoImpl extends AbstractOpenemsComponent
		implements WeatherOpenMeteo, OpenemsComponent, Weather, ComponentJsonApi {

	private static final long MINUTES_PER_QUARTER = 15L;

	public static final int FORECAST_DAYS = 7;
	public static final int PAST_DAYS = 1;
	public static final int MAX_FORECAST_AGE_DAYS = 1;

	@Reference
	private ComponentManager componentManager;

	@Reference
	private BridgeHttpFactory httpBridgeFactory;

	@Reference
	private OpenemsEdgeOem oem;

	private Config config;
	private BridgeHttp httpBridge;

	private HistoricalWeatherService historicalWeatherService;
	private WeatherForecastService weatherForecastService;
	private WeatherForecastPersistenceService weatherForecastPersistenceService;

	private Meta meta;
	private Coordinates coordinates;

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
		if (!Objects.equals(this.coordinates, updatedMeta.getCoordinates())) {
			this.weatherForecastService.subscribeToWeatherForecast(//
					new WeatherForecastDelayTimeProvider(this.componentManager.getClock()), //
					updatedMeta.getCoordinates(), //
					() -> this.componentManager.getClock(), //
					() -> this.onFetchWeatherForecastSuccess());

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
		final var timeService = this.httpBridge.createService(HttpBridgeTimeServiceDefinition.INSTANCE);
		final var weatherDataParser = new DefaultWeatherDataParser();

		this.historicalWeatherService = new HistoricalWeatherService(//
				this.httpBridge, //
				this.oem.getOpenMeteoApiKey(), //
				weatherDataParser);

		this.weatherForecastService = new WeatherForecastService(//
				this, //
				timeService, //
				this.oem.getOpenMeteoApiKey(), //
				FORECAST_DAYS, //
				PAST_DAYS, //
				weatherDataParser);

		this.weatherForecastPersistenceService = new WeatherForecastPersistenceService(//
				this, //
				() -> this.componentManager.getClock());

		this.weatherForecastService.subscribeToWeatherForecast(//
				new WeatherForecastDelayTimeProvider(this.componentManager.getClock()), //
				this.meta.getCoordinates(), //
				() -> this.componentManager.getClock(), //
				() -> this.onFetchWeatherForecastSuccess());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.weatherForecastService.deactivateForecastSubscription();
		this.httpBridgeFactory.unget(this.httpBridge);
		this.httpBridge = null;
		this.weatherForecastPersistenceService.deactivateHourlyPersistenceJob();
		this.weatherForecastPersistenceService = null;
	}

	@Override
	public QuarterlyWeatherSnapshot getCurrentWeather() throws OpenemsException {
		this.assertWeatherForecastServiceAvailable();
		this.assertWeatherForecastValid(//
				this.weatherForecastService.getQuarterlyWeatherForecast(), //
				this.weatherForecastService.getLastUpdate());

		var now = roundDownToQuarter(ZonedDateTime.now(this.componentManager.getClock()));
		return this.weatherForecastService.getQuarterlyWeatherForecast().stream()
				.filter(snapshot -> snapshot.datetime().equals(now))//
				.findFirst()//
				.orElse(null);
	}

	@Override
	public CompletableFuture<List<QuarterlyWeatherSnapshot>> getHistoricalWeather(//
			LocalDate dateFrom, //
			LocalDate dateTo, //
			ZoneId zone) {
		if (this.historicalWeatherService == null) {
			return CompletableFuture.failedFuture(new IllegalStateException(
					"HistoricalWeatherService is not available. This may happen if the weather API component is not enabled or configured"));
		}

		return this.historicalWeatherService.getWeatherData(//
				this.meta.getCoordinates(), //
				dateFrom, //
				dateTo, //
				zone);
	}

	@Override
	public List<QuarterlyWeatherSnapshot> getQuarterlyWeatherForecast(int forecastQuarters) throws OpenemsException {
		this.assertWeatherForecastServiceAvailable();
		this.assertWeatherForecastValid(//
				this.weatherForecastService.getQuarterlyWeatherForecast(), //
				this.weatherForecastService.getLastUpdate());

		final var quarterlyWeatherForecast = this.weatherForecastService.getQuarterlyWeatherForecast();

		var now = roundDownToQuarter(ZonedDateTime.now(this.componentManager.getClock()));
		var forecastUntil = now.plusMinutes(forecastQuarters * MINUTES_PER_QUARTER);

		return quarterlyWeatherForecast.stream()//
				.filter(snapshot -> !snapshot.datetime().isBefore(now) && !snapshot.datetime().isAfter(forecastUntil))//
				.sorted(Comparator.comparing(QuarterlyWeatherSnapshot::datetime))//
				.toList();
	}

	@Override
	public List<HourlyWeatherSnapshot> getHourlyWeatherForecast(int forecastHours) throws OpenemsException {
		this.assertWeatherForecastServiceAvailable();
		this.assertWeatherForecastValid(//
				this.weatherForecastService.getHourlyWeatherForecast(), //
				this.weatherForecastService.getLastUpdate());

		final var hourlyWeatherForecast = this.weatherForecastService.getHourlyWeatherForecast();
		var now = ZonedDateTime.now(this.componentManager.getClock()).truncatedTo(ChronoUnit.HOURS);
		var forecastUntil = now.plusHours(forecastHours);

		return hourlyWeatherForecast.stream()//
				.filter(snapshot -> !snapshot.datetime().isBefore(now) && !snapshot.datetime().isAfter(forecastUntil))//
				.sorted(Comparator.comparing(HourlyWeatherSnapshot::datetime))//
				.toList();
	}

	@Override
	public List<DailyWeatherSnapshot> getDailyWeatherForecast() throws OpenemsException {
		this.assertWeatherForecastServiceAvailable();
		this.assertWeatherForecastValid(//
				this.weatherForecastService.getDailyWeatherForecast(), //
				this.weatherForecastService.getLastUpdate());

		final var dailyWeatherForecast = this.weatherForecastService.getDailyWeatherForecast();
		var today = LocalDate.now(this.componentManager.getClock());

		return dailyWeatherForecast.stream()//
				.filter(snapshot -> !snapshot.date().isBefore(today))//
				.sorted(Comparator.comparing(DailyWeatherSnapshot::date))//
				.toList();
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.handleRequest(new DailyWeatherForecastEndpoint(), endpoint -> {
			endpoint.setGuards(EdgeGuards.roleIsAtleast(Role.GUEST));
		}, call -> {
			return new DailyWeatherForecastEndpoint.Response(//
					this.getDailyWeatherForecast());
		});
	}

	private void onFetchWeatherForecastSuccess() {
		this.weatherForecastPersistenceService.deactivateHourlyPersistenceJob();
		this.weatherForecastPersistenceService.startHourlyPersistenceJob();
	}

	private void assertWeatherForecastServiceAvailable() {
		if (this.weatherForecastService == null) {
			throw new IllegalStateException(
					"WeatherForecastService is not available. This may happen if the weather API component is not enabled or configured");
		}
	}

	private <T> void assertWeatherForecastValid(List<T> weatherForecast, Instant lastUpdate) throws OpenemsException {
		if (weatherForecast == null || lastUpdate == null || lastUpdate.isBefore(
				Instant.now(this.componentManager.getClock()).minus(Duration.ofDays(MAX_FORECAST_AGE_DAYS)))) {
			throw new OpenemsException("Weather forecast data is unavailable or outdated");
		}
	}

	@VisibleForTesting
	void setHistoricalWeatherService(HistoricalWeatherService historicalWeatherService) {
		this.historicalWeatherService = historicalWeatherService;
	}

	@VisibleForTesting
	void setWeatherForecastService(WeatherForecastService weatherForecastService) {
		this.weatherForecastService = weatherForecastService;
	}
}
