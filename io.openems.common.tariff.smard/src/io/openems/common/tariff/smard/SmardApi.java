package io.openems.common.tariff.smard;

import static io.openems.common.utils.JsonUtils.getAsJsonArray;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.common.utils.JsonUtils.getAsLong;
import static io.openems.common.utils.JsonUtils.parseToJsonObject;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpExecutor;
import io.openems.common.bridge.http.api.HttpBridgeService;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.api.UrlBuilder;
import io.openems.common.bridge.http.time.DelayTimeProvider;
import io.openems.common.bridge.http.time.DelayTimeProviderChain;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.timedata.DurationUnit;
import io.openems.common.types.TariffData;
import io.openems.common.types.TariffType;
import io.openems.common.utils.DateUtils;
import io.openems.common.utils.TimeRangeValues;

public class SmardApi implements HttpBridgeService {
	/**
	 * SMARD is providing new values after this hour of the day. This hour is not
	 * guaranteed. It's not in any documentation, it's just observed behavior.
	 */
	public static final int SMARD_UPDATE_HOUR = 14;

	private static final String URI = "https://www.smard.de/app/";
	private static final String REGION = "DE-LU";
	private static final String RESOLUTION = "quarterhour";
	private static final Currency CURRENCY = Currency.getInstance("EUR");
	private static final DurationUnit PRICES_DURATION_UNIT = DurationUnit.ofMinutes(15);

	private final Logger log = LoggerFactory.getLogger(SmardApi.class);
	private final BridgeHttp bridge;
	private final BridgeHttpExecutor executor;
	private final SmardBiddingZone biddingZone;
	private final Clock clock;

	private ScheduledFuture<?> currentSubscriptionFuture;
	private Instant nextFetchTime;

	public SmardApi(SmardBiddingZone biddingZone, BridgeHttp bridge, BridgeHttpExecutor executor, Clock clock) {
		this.biddingZone = biddingZone;
		this.bridge = bridge;
		this.executor = executor;
		this.clock = clock;
		this.nextFetchTime = Instant.now(clock);
	}

	public Instant getNextFetchTime() {
		return this.nextFetchTime;
	}

	/**
	 * Subscribes to receive prices periodically from SMARD api.
	 *
	 * @param onDataReceived Called when data is received successfully
	 * @param onError        Called when data receiving failed
	 */
	public void subscribePrices(Consumer<TariffData> onDataReceived, Consumer<Throwable> onError) {
		this.currentSubscriptionFuture = this.executor.schedule(() -> {
			this.fetch().whenComplete((data, ex) -> {
				DelayTimeProvider.Delay delay;
				try {
					if (ex != null) {
						onError.accept(ex);

						delay = this.getErrorDelay();
					} else {
						onDataReceived.accept(data);
						delay = this.calculateNextFetchDelay(data.prices().getLastTime());
					}
				} catch (Exception e) {
					if (this.executor.isShutdown()) {
						return;
					}

					this.log.error("Exception occurred while SMARD completion handling", e);
					delay = this.getErrorDelay();
				}

				if (!this.executor.isShutdown()) {
					var durationDelay = (DelayTimeProvider.Delay.DurationDelay) delay;
					this.nextFetchTime = Instant.now(this.clock).plus(durationDelay.getDuration());
					this.currentSubscriptionFuture = this.executor
							.schedule(() -> this.subscribePrices(onDataReceived, onError), durationDelay);
				}
			});
		}, (DelayTimeProvider.Delay.DurationDelay) DelayTimeProvider.Delay.immediate());
	}

	protected CompletableFuture<TariffData> fetch() {
		var endpoint = this.createTimestampEndpoint();
		return this.bridge.request(endpoint) //
				.thenCompose(result -> this.bridge.request(this.createChartDataEndpoint(result))) //
				.thenApply(this::readPricesFromChartData);
	}

	protected BridgeHttp.Endpoint createTimestampEndpoint() {
		var urlBuilder = UrlBuilder.parse(URI) //
				.withAddedPath("/chart_data/{filter}/{region}/index_{resolution}.json") //
				.withPathReplace("{filter}", this.biddingZone.getSmardFilter()) //
				.withPathReplace("{region}", REGION) //
				.withPathReplace("{resolution}", RESOLUTION);

		return BridgeHttp.create(urlBuilder.toEncodedString()).build();
	}

	protected BridgeHttp.Endpoint createChartDataEndpoint(HttpResponse<String> timestampResponse) {
		try {
			var timestamps = this.readTimestampsFromResponse(timestampResponse);
			if (timestamps.isEmpty()) {
				throw new OpenemsException("No timestamps available from SMARD");
			}

			var lastTimestamp = timestamps.stream().max(Comparator.naturalOrder()).get();
			return this.createChartDataEndpoint(lastTimestamp);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to create chart data endpoint", ex);
		}
	}

	protected BridgeHttp.Endpoint createChartDataEndpoint(long timestamp) {
		var urlBuilder = UrlBuilder.parse(URI) //
				.withAddedPath("/chart_data/{filter}/{region}/{filter}_{region}_{resolution}_{timestamp}.json")
				.withPathReplace("{filter}", this.biddingZone.getSmardFilter()) //
				.withPathReplace("{region}", REGION) //
				.withPathReplace("{resolution}", RESOLUTION) //
				.withPathReplace("{timestamp}", String.valueOf(timestamp));

		return BridgeHttp.create(urlBuilder.toEncodedString()).build();
	}

	protected List<Long> readTimestampsFromResponse(HttpResponse<String> response) throws OpenemsNamedException {
		var data = parseToJsonObject(response.data());
		var timestamps = getAsJsonArray(data, "timestamps");

		var builder = ImmutableList.<Long>builder();
		for (var element : timestamps) {
			builder.add(element.getAsLong());
		}

		return builder.build();
	}

	protected TariffData readPricesFromChartData(HttpResponse<String> response) {
		try {
			var data = parseToJsonObject(response.data());
			var series = getAsJsonArray(data, "series");

			var pricesByTimestamp = series.asList().stream() //
					.map(JsonElement::getAsJsonArray) //
					.filter(x -> !x.get(1).isJsonNull()) //
					.collect(Collectors.toMap(x -> x.get(0).getAsLong(), x -> x.get(1).getAsDouble()));

			var sortedTimestamps = pricesByTimestamp.keySet().stream().sorted().toList();
			var firstTimestamp = decodeTimestamp(sortedTimestamps.getFirst());
			var lastTimestamp = decodeTimestamp(sortedTimestamps.getLast()).plus(PRICES_DURATION_UNIT.getDuration());

			var builder = TimeRangeValues.builder(firstTimestamp, lastTimestamp, PRICES_DURATION_UNIT, Double.class);
			for (var entry : pricesByTimestamp.entrySet()) {
				builder.setByTime(decodeTimestamp(entry.getKey()), entry.getValue());
			}
			var prices = builder.build();

			var metadata = getAsJsonObject(data, "meta_data");
			var created = decodeTimestamp(getAsLong(metadata, "created"));

			return new TariffData(new TariffType.DayAheadEpexSpot(this.biddingZone.getBiddingZone()), prices, CURRENCY,
					Instant.now(), created, null);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to read prices from SMARD data. Content: '" + response.data() + "'", ex);
		}
	}

	/**
	 * Calculates when the data should be fetched the next time.
	 *
	 * @param lastReceivedDate The last timepoint that is currently available
	 * @return The delay until the next run
	 */
	protected DelayTimeProvider.Delay calculateNextFetchDelay(Instant lastReceivedDate) {
		final var now = Instant.now(this.clock);
		final var nextMidnight = now.truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.DAYS);
		final var todayAtExecuteHour = now.truncatedTo(ChronoUnit.DAYS).plus(SMARD_UPDATE_HOUR, ChronoUnit.HOURS);

		// Case 1: No prices at all or data doesn't extend to next day (incomplete)
		if (lastReceivedDate == null || lastReceivedDate.isBefore(nextMidnight)) {
			var nextRun = DateUtils.roundDownToQuarter(now.plus(1, ChronoUnit.HOURS));
			return DelayTimeProvider.Delay.of(Duration.between(now, nextRun));
		}

		// Case 2: Complete data available, but before 14:00
		if (now.isBefore(todayAtExecuteHour)) {
			return DelayTimeProviderChain.fixedDelay(Duration.between(now, todayAtExecuteHour)) //
					.plusRandomDelay(60, ChronoUnit.SECONDS).getDelay();
		}

		// Case 3: After 14:00, schedule for next day at 14:00
		return DelayTimeProviderChain.fixedAtEveryFull(this.clock, DurationUnit.ofHours(24)) //
				.plusFixedAmount(Duration.ofHours(SMARD_UPDATE_HOUR)) //
				.plusRandomDelay(60, ChronoUnit.SECONDS) //
				.getDelay();
	}

	protected DelayTimeProvider.Delay getErrorDelay() {
		return DelayTimeProviderChain.fixedDelay(Duration.ofMinutes(2)).plusRandomDelay(60, ChronoUnit.SECONDS)
				.getDelay();
	}

	private static Instant decodeTimestamp(long timestamp) {
		return Instant.ofEpochMilli(timestamp);
	}

	@Override
	public void close() throws Exception {
		if (this.currentSubscriptionFuture != null) {
			this.currentSubscriptionFuture.cancel(false);
			this.currentSubscriptionFuture = null;
		}
	}
}
