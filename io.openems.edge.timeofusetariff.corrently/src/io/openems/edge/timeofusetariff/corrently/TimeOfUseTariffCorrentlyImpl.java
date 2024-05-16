package io.openems.edge.timeofusetariff.corrently;

import static io.openems.common.utils.JsonUtils.getAsDouble;
import static io.openems.common.utils.JsonUtils.getAsJsonArray;
import static io.openems.common.utils.JsonUtils.getAsLong;
import static io.openems.common.utils.JsonUtils.parseToJsonObject;
import static io.openems.edge.timeofusetariff.api.utils.TimeOfUseTariffUtils.generateDebugLog;

import java.io.IOException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.ThreadPoolUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;
import okhttp3.OkHttpClient;
import okhttp3.Request;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "TimeOfUseTariff.Corrently", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class TimeOfUseTariffCorrentlyImpl extends AbstractOpenemsComponent
		implements TimeOfUseTariff, OpenemsComponent, TimeOfUseTariffCorrently {

	private static final String CORRENTLY_API_URL = "https://api.corrently.io/v2.0/gsi/marketdata?zip=";

	private final Logger log = LoggerFactory.getLogger(TimeOfUseTariffCorrentlyImpl.class);
	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	private final AtomicReference<TimeOfUsePrices> prices = new AtomicReference<>(TimeOfUsePrices.EMPTY_PRICES);

	@Reference
	private Meta meta;

	@Reference
	private ComponentManager componentManager;

	private Config config = null;

	public TimeOfUseTariffCorrentlyImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				TimeOfUseTariffCorrently.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
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

	private final Runnable task = () -> {

		/*
		 * Update Map of prices
		 */
		var client = new OkHttpClient();
		var request = new Request.Builder() //
				.url(CORRENTLY_API_URL + this.config.zipcode() + "&resolution=900") //
				.build();
		int httpStatusCode;
		try (var response = client.newCall(request).execute()) {
			httpStatusCode = response.code();

			if (!response.isSuccessful()) {
				throw new IOException("Unexpected code " + response);
			}

			// Parse the response for the prices
			this.prices.set(parsePrices(response.body().string()));

		} catch (IOException | OpenemsNamedException e) {
			this.logWarn(this.log, "Unable to Update Corrently Time-Of-Use Price: " + e.getMessage());
			httpStatusCode = 0;
		}

		this.channel(TimeOfUseTariffCorrently.ChannelId.HTTP_STATUS_CODE).setNextValue(httpStatusCode);

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

	@Override
	public TimeOfUsePrices getPrices() {
		return TimeOfUsePrices.from(ZonedDateTime.now(), this.prices.get());
	}

	/**
	 * Parse the Corrently JSON to {@link TimeOfUsePrices}.
	 *
	 * @param jsonData the Corrently JSON
	 * @return the Price Map
	 * @throws OpenemsNamedException on error
	 */
	public static TimeOfUsePrices parsePrices(String jsonData) throws OpenemsNamedException {
		var result = new TreeMap<ZonedDateTime, Double>();
		var data = getAsJsonArray(parseToJsonObject(jsonData), "data");
		for (var element : data) {
			var marketPrice = getAsDouble(element, "marketprice");

			// Converting Long time stamp to ZonedDateTime.
			var startTimeStamp = ZonedDateTime //
					.ofInstant(Instant.ofEpochMilli(getAsLong(element, "start_timestamp")), ZoneId.systemDefault())
					.truncatedTo(ChronoUnit.MINUTES);
			// Adding the values in the Map.
			result.put(startTimeStamp, marketPrice);
		}
		return TimeOfUsePrices.from(result);
	}

	@Override
	public String debugLog() {
		return generateDebugLog(this, this.meta.getCurrency());
	}
}
