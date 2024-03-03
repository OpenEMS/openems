package io.openems.edge.timeofusetariff.tibber;

import static io.openems.edge.timeofusetariff.api.utils.TimeOfUseTariffUtils.generateDebugLog;

import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;
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
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.ThreadPoolUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;
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
public class TimeOfUseTariffTibberImpl extends AbstractOpenemsComponent
		implements TimeOfUseTariff, OpenemsComponent, TimeOfUseTariffTibber {

	private static final String TIBBER_API_URL = "https://api.tibber.com/v1-beta/gql";

	private final Logger log = LoggerFactory.getLogger(TimeOfUseTariffTibberImpl.class);
	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	private final AtomicReference<TimeOfUsePrices> prices = new AtomicReference<>(TimeOfUsePrices.EMPTY_PRICES);

	@Reference
	private Meta meta;

	@Reference
	private ComponentManager componentManager;

	private Config config = null;

	public TimeOfUseTariffTibberImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				TimeOfUseTariffTibber.ChannelId.values() //
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

	protected final Runnable task = () -> {
		/*
		 * Update Map of prices
		 */
		var client = new OkHttpClient();
		var request = new Request.Builder() //
				.url(TIBBER_API_URL) //
				.header("Authorization", this.config.accessToken()) //
				.post(RequestBody.create(JsonUtils.buildJsonObject() //
						.addProperty("query", Utils.generateGraphQl()) //
						.build().toString(), MediaType.parse("application/json"))) //
				.build();
		int httpStatusCode = 0;
		var filterIsRequired = false;
		var unableToUpdatePrices = false;

		try (var response = client.newCall(request).execute()) {
			httpStatusCode = response.code();

			if (!response.isSuccessful()) {
				throw new IOException("Unexpected code " + response);
			}

			// Initialize status channel to false
			this.channel(TimeOfUseTariffTibber.ChannelId.FILTER_IS_REQUIRED).setNextValue(false);

			// Parse the response for the prices
			this.prices.set(Utils.parsePrices(response.body().string(), this.config.filter()));

		} catch (IOException | OpenemsNamedException e) {
			if (e instanceof FoundMultipleHomesException) {
				filterIsRequired = true;
			} else {
				unableToUpdatePrices = true;
			}
			e.printStackTrace();
		}

		this.channel(TimeOfUseTariffTibber.ChannelId.HTTP_STATUS_CODE).setNextValue(httpStatusCode);
		this.channel(TimeOfUseTariffTibber.ChannelId.FILTER_IS_REQUIRED).setNextValue(filterIsRequired);
		this.channel(TimeOfUseTariffTibber.ChannelId.UNABLE_TO_UPDATE_PRICES).setNextValue(unableToUpdatePrices);

		/*
		 * Schedule next price update at next hour; or try again after 5 minutes
		 */
		var now = ZonedDateTime.now();
		final ZonedDateTime nextRun;
		if (unableToUpdatePrices) {
			// If the prices are not updated, try again in 5 minutes.
			nextRun = now.plusMinutes(5).truncatedTo(ChronoUnit.MINUTES);
			this.logWarn(this.log, "Unable to Update the prices, Trying again at: " + nextRun);
		} else {
			nextRun = now.truncatedTo(ChronoUnit.HOURS).plusHours(1);
		}

		this.executor.schedule(this.task, //
				Duration.between(now, nextRun.plusSeconds(new Random().nextInt(60))) // randomly add a few seconds
						.getSeconds(),
				TimeUnit.SECONDS);
	};

	@Override
	public TimeOfUsePrices getPrices() {
		return TimeOfUsePrices.from(ZonedDateTime.now(), this.prices.get());
	}

	@Override
	public String debugLog() {
		return generateDebugLog(this, this.meta.getCurrency());
	}
}
