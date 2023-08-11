package io.openems.edge.timeofusetariff.awattar;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
import okhttp3.OkHttpClient;
import okhttp3.Request;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "TimeOfUseTariff.Awattar", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class TimeOfUseTariffAwattarImpl extends AbstractOpenemsComponent
		implements TimeOfUseTariff, OpenemsComponent, TimeOfUseTariffAwattar {

	private static final String AWATTAR_API_URL = "https://api.awattar.de/v1/marketdata";

	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	private final AtomicReference<ImmutableSortedMap<ZonedDateTime, Float>> prices = new AtomicReference<>(
			ImmutableSortedMap.of());

	@Reference
	private ComponentManager componentManager;

	private ZonedDateTime updateTimeStamp = null;

	public TimeOfUseTariffAwattarImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				TimeOfUseTariffAwattar.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());

		if (!config.enabled()) {
			return;
		}

		this.executor.schedule(this.task, 0, TimeUnit.SECONDS);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		ThreadPoolUtils.shutdownAndAwaitTermination(this.executor, 0);
	}

	private final Runnable task = () -> {

		/*
		 * Update Map of prices
		 */
		var client = new OkHttpClient();
		var request = new Request.Builder() //
				.url(AWATTAR_API_URL) //
				// aWATTar currently does not anymore require an Apikey.
				// .header("Authorization", Credentials.basic(apikey, "")) //
				.build();
		int httpStatusCode;
		try (var response = client.newCall(request).execute()) {
			httpStatusCode = response.code();

			if (!response.isSuccessful()) {
				throw new IOException("Unexpected code " + response);
			}

			// Parse the response for the prices
			this.prices.set(TimeOfUseTariffAwattarImpl.parsePrices(response.body().string()));

			// store the time stamp
			this.updateTimeStamp = ZonedDateTime.now();

		} catch (IOException | OpenemsNamedException e) {
			e.printStackTrace();
			httpStatusCode = 0;
			// TODO Try again in x minutes
		}

		this.channel(TimeOfUseTariffAwattar.ChannelId.HTTP_STATUS_CODE).setNextValue(httpStatusCode);

		/*
		 * Schedule next price update every hour
		 */
		var now = ZonedDateTime.now();
		// We query every hour since Awattar gives the prices for only next 24 hours
		// instead of 96.
		var nextRun = now.plusHours(1).truncatedTo(ChronoUnit.HOURS);
		var delay = Duration.between(now, nextRun).getSeconds();

		this.executor.schedule(this.task, delay, TimeUnit.SECONDS);
	};

	@Override
	public TimeOfUsePrices getPrices() {
		// return empty TimeOfUsePrices if data is not yet available.
		if (this.updateTimeStamp == null) {
			return TimeOfUsePrices.empty(ZonedDateTime.now());
		}

		return TimeOfUseTariffUtils.getNext24HourPrices(Clock.systemDefaultZone() /* can be mocked for testing */,
				this.prices.get(), this.updateTimeStamp);
	}

	/**
	 * Parse the aWATTar JSON to the Price Map.
	 *
	 * @param jsonData the aWATTar JSON
	 * @return the Price Map
	 * @throws OpenemsNamedException on error
	 */
	public static ImmutableSortedMap<ZonedDateTime, Float> parsePrices(String jsonData) throws OpenemsNamedException {
		var result = new TreeMap<ZonedDateTime, Float>();

		var line = JsonUtils.parseToJsonObject(jsonData);
		var data = JsonUtils.getAsJsonArray(line, "data");

		for (JsonElement element : data) {

			var marketPrice = JsonUtils.getAsFloat(element, "marketprice");
			var startTimestampLong = JsonUtils.getAsLong(element, "start_timestamp");

			// Converting Long time stamp to ZonedDateTime.
			var startTimeStamp = ZonedDateTime //
					.ofInstant(Instant.ofEpochMilli(startTimestampLong), ZoneId.systemDefault())
					.truncatedTo(ChronoUnit.HOURS);

			// Adding the values in the Map.
			result.put(startTimeStamp, marketPrice);
			result.put(startTimeStamp.plusMinutes(15), marketPrice);
			result.put(startTimeStamp.plusMinutes(30), marketPrice);
			result.put(startTimeStamp.plusMinutes(45), marketPrice);
		}
		return ImmutableSortedMap.copyOf(result);
	}

}
