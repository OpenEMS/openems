package io.openems.edge.timeofusetariff.tibber;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import com.google.common.collect.ImmutableSortedMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.ThreadPoolUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;
import io.openems.edge.timeofusetariff.api.utils.TimeOfUseTariffUtils;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "TimeOfUseTariff.Tibber", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class TibberImpl extends AbstractOpenemsComponent implements TimeOfUseTariff, OpenemsComponent, Tibber {

	private static final String TIBBER_API_URL = "https://api.tibber.com/v1-beta/gql";

	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	private Config config = null;

	private final AtomicReference<ImmutableSortedMap<ZonedDateTime, Float>> prices = new AtomicReference<>(
			ImmutableSortedMap.of());

	private ZonedDateTime updateTimeStamp = null;

	private final Runnable task = () -> {

		/*
		 * Update Map of prices
		 */
		var client = new OkHttpClient();
		var mediaType = MediaType.parse("application/json");
		var body = RequestBody.create(mediaType, JsonUtils.buildJsonObject() //
				.addProperty("query", //
						"{\n" + "  viewer {\n" + "    homes {\n" + "      currentSubscription{\n"
								+ "        priceInfo{\n" + "          today {\n" + "            total\n"
								+ "            startsAt\n" + "          }\n" + "          tomorrow {\n"
								+ "            total\n" + "            startsAt\n" + "          }\n" + "        }\n"
								+ "      }\n" + "    }\n" + "  }\n" + "}" + "") //
				.build().toString());
		var request = new Request.Builder() //
				.url(TIBBER_API_URL) //
				.header("Authorization", this.config.accessToken()).post(body) //
				.build();
		int httpStatusCode;
		try (var response = client.newCall(request).execute()) {
			httpStatusCode = response.code();

			if (!response.isSuccessful()) {
				throw new IOException("Unexpected code " + response);
			}

			// Parse the response for the prices
			this.prices.set(TibberImpl.parsePrices(response.body().string()));

			// store the time stamp
			this.updateTimeStamp = ZonedDateTime.now();

		} catch (IOException | OpenemsNamedException e) {
			e.printStackTrace();
			httpStatusCode = 0;
		}

		this.channel(Tibber.ChannelId.HTTP_STATUS_CODE).setNextValue(httpStatusCode);

		/*
		 * Schedule next price update for 2 pm
		 */
		var now = ZonedDateTime.now();
		var nextRun = now.withHour(14).truncatedTo(ChronoUnit.HOURS);
		if (now.isAfter(nextRun)) {
			nextRun = nextRun.plusDays(1);
		}

		var duration = Duration.between(now, nextRun);
		var delay = duration.getSeconds();

		this.executor.schedule(this.task, delay, TimeUnit.SECONDS);
	};

	@Reference
	private ComponentManager componentManager;

	public TibberImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Tibber.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());

		if (!config.enabled()) {
			return;
		}
		this.config = config;
		this.executor.schedule(this.task, 0, TimeUnit.SECONDS);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		ThreadPoolUtils.shutdownAndAwaitTermination(this.executor, 0);
	}

	@Override
	public TimeOfUsePrices getPrices() {
		// return null if data is not yet available.
		if (this.updateTimeStamp == null) {
			return null;
		}

		return TimeOfUseTariffUtils.getNext24HourPrices(Clock.systemDefaultZone() /* can be mocked for testing */,
				this.prices.get(), this.updateTimeStamp);
	}

	/**
	 * Parse the Tibber JSON to the Price Map.
	 *
	 * @param jsonData the Tibber JSON
	 * @return the Price Map
	 * @throws OpenemsNamedException on error
	 */
	public static ImmutableSortedMap<ZonedDateTime, Float> parsePrices(String jsonData) throws OpenemsNamedException {
		var result = new TreeMap<ZonedDateTime, Float>();

		if (!jsonData.isEmpty()) {

			var line = JsonUtils.parseToJsonObject(jsonData);
			var homes = JsonUtils.getAsJsonObject(line, "data") //
					.getAsJsonObject("viewer") //
					.getAsJsonArray("homes");

			for (JsonElement home : homes) {

				var priceInfo = JsonUtils.getAsJsonObject(home, "currentSubscription") //
						.getAsJsonObject("priceInfo");

				// Price info for today and tomorrow.
				var today = JsonUtils.getAsJsonArray(priceInfo, "today");
				var tomorrow = JsonUtils.getAsJsonArray(priceInfo, "tomorrow");

				// Adding to an array to avoid individual variables for individual for loops.
				JsonArray[] days = { today, tomorrow };

				// parse the arrays for price and time stamps.
				for (JsonArray day : days) {
					for (JsonElement element : day) {
						var marketPrice = JsonUtils.getAsFloat(element, "total") * 1000;
						var startTime = ZonedDateTime
								.parse(JsonUtils.getAsString(element, "startsAt"), DateTimeFormatter.ISO_DATE_TIME)
								.withZoneSameInstant(ZoneId.systemDefault());
						// Adding the values in the Map.
						result.put(startTime, marketPrice);
					}
				}
			}
		}
		return ImmutableSortedMap.copyOf(result);
	}
}
