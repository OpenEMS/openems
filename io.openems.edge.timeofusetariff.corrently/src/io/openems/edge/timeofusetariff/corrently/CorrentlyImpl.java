package io.openems.edge.timeofusetariff.corrently;

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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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
import okhttp3.Response;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "TimeOfUseTariff.Corrently", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class CorrentlyImpl extends AbstractOpenemsComponent implements TimeOfUseTariff, OpenemsComponent, Corrently {

	private static final String CORRENTLY_API_URL = "https://api.corrently.io/v2.0/gsi/marketdata?zipcode=";

	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	private Config config = null;

	private final AtomicReference<ImmutableSortedMap<ZonedDateTime, Float>> prices = new AtomicReference<ImmutableSortedMap<ZonedDateTime, Float>>(
			ImmutableSortedMap.of());

	private Clock updateTimeStamp = Clock.fixed(Instant.MIN, ZoneId.systemDefault());

	private final Runnable task = () -> {

		/*
		 * Update Map of prices
		 */
		ImmutableSortedMap<ZonedDateTime, Float> prices;
		try {
			Integer zipcode = this.config.zipcode();
			OkHttpClient client = new OkHttpClient();
			Request request = new Request.Builder() //
					.url(CORRENTLY_API_URL.concat(zipcode.toString())) //
					// .header("Authorization", Credentials.basic(apikey, "")) //
					.build();

			Response response = client.newCall(request).execute();
			this.channel(Corrently.ChannelId.HTTP_STATUS_CODE).setNextValue(response.code());

			if (!response.isSuccessful()) {
				throw new IOException("Unexpected code " + response);
			}

			// Parse the response for the prices
			prices = CorrentlyImpl.parsePrices(response.body().toString());

			// store the time stamp
			this.updateTimeStamp = Clock.systemDefaultZone();

		} catch (IOException | OpenemsNamedException e) {
			e.printStackTrace();
			prices = ImmutableSortedMap.of();
		}

		this.prices.set(prices);

		/*
		 * Schedule next price update for 2 pm
		 */
		ZonedDateTime now = ZonedDateTime.now();
		ZonedDateTime nextRun = now.withHour(14).truncatedTo(ChronoUnit.HOURS);
		if (now.isAfter(nextRun)) {
			nextRun = nextRun.plusDays(1);
		}

		Duration duration = Duration.between(now, nextRun);
		long delay = duration.getSeconds();

		this.executor.schedule(this.task, delay, TimeUnit.SECONDS);
	};

	@Reference
	private ComponentManager componentManager;

	public CorrentlyImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Corrently.ChannelId.values() //
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

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		ThreadPoolUtils.shutdownAndAwaitTermination(this.executor, 0);
	}

	@Override
	public TimeOfUsePrices getPrices() {
		return TimeOfUseTariffUtils.getNext24HourPrices(Clock.systemDefaultZone() /* can be mocked for testing */,
				this.prices.get(), this.updateTimeStamp);
	}

	/**
	 * Parse the Corrently JSON to the Price Map.
	 * 
	 * @param jsonData the Corrently JSON
	 * @return the Price Map
	 * @throws OpenemsNamedException on error
	 */
	public static ImmutableSortedMap<ZonedDateTime, Float> parsePrices(String jsonData) throws OpenemsNamedException {
		TreeMap<ZonedDateTime, Float> result = new TreeMap<>();

		if (!jsonData.isEmpty()) {

			JsonObject line = JsonUtils.getAsJsonObject(JsonUtils.parse(jsonData));
			JsonArray data = JsonUtils.getAsJsonArray(line, "data");

			for (JsonElement element : data) {

				float marketPrice = JsonUtils.getAsFloat(element, "marketprice");
				long startTimestampLong = JsonUtils.getAsLong(element, "start_timestamp");

				// Converting Long time stamp to ZonedDateTime.
				ZonedDateTime startTimeStamp = ZonedDateTime //
						.ofInstant(Instant.ofEpochMilli(startTimestampLong), ZoneId.systemDefault())
						.truncatedTo(ChronoUnit.HOURS);

				// Adding the values in the Map.
				result.put(startTimeStamp, marketPrice);
				result.put(startTimeStamp.plusMinutes(15), marketPrice);
				result.put(startTimeStamp.plusMinutes(30), marketPrice);
				result.put(startTimeStamp.plusMinutes(45), marketPrice);
			}
		}
		return ImmutableSortedMap.copyOf(result);
	}

}
