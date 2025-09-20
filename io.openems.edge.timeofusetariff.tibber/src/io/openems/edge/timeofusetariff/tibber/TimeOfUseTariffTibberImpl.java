package io.openems.edge.timeofusetariff.tibber;

import static io.openems.edge.timeofusetariff.api.utils.TimeOfUseTariffUtils.generateDebugLog;
import static io.openems.edge.timeofusetariff.tibber.Utils.calculateDelay;

import java.io.IOException;
import java.time.ZonedDateTime;
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
	protected static final int CLIENT_ERROR_CODE = 400;
	protected static final int TOO_MANY_REQUESTS_CODE = 429;

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
		var httpStatusCode = 0;
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

		this.setChannelValues(httpStatusCode, filterIsRequired);

		var delay = calculateDelay(httpStatusCode, filterIsRequired, unableToUpdatePrices);
		if (delay != 0) {
			this.executor.schedule(this.task, delay, TimeUnit.SECONDS);
		}
	};

	/**
	 * Sets the values of specific channels based on the provided parameters.
	 * 
	 * @param httpStatusCode   The HTTP status code received from the API.
	 * @param filterIsRequired A boolean indicating whether filter is required.
	 */
	private void setChannelValues(int httpStatusCode, boolean filterIsRequired) {
		var authenticationFailed = false;
		var serverError = false;
		var timeout = false;

		switch (httpStatusCode) {
		case CLIENT_ERROR_CODE:
			authenticationFailed = true;
			this.logWarn(this.log, "Authentication failed, please try again with valid token.");
			break;

		case TOO_MANY_REQUESTS_CODE:
			timeout = true;
			break;

		default:
			if (httpStatusCode >= 200 && httpStatusCode < 300) {
				// No error
			} else {
				serverError = true;
				this.logWarn(this.log, "An unexpected error occurred on the server. Please try again later");
			}
			break;
		}

		this.channel(TimeOfUseTariffTibber.ChannelId.HTTP_STATUS_CODE).setNextValue(httpStatusCode);
		this.channel(TimeOfUseTariffTibber.ChannelId.FILTER_IS_REQUIRED).setNextValue(filterIsRequired);
		this.channel(TimeOfUseTariffTibber.ChannelId.STATUS_TIMEOUT).setNextValue(timeout);
		this.channel(TimeOfUseTariffTibber.ChannelId.STATUS_AUTHENTICATION_FAILED).setNextValue(authenticationFailed);
		this.channel(TimeOfUseTariffTibber.ChannelId.STATUS_SERVER_ERROR).setNextValue(serverError);
	}

	@Override
	public TimeOfUsePrices getPrices() {
		return TimeOfUsePrices.from(ZonedDateTime.now(), this.prices.get());
	}

	@Override
	public String debugLog() {
		return generateDebugLog(this, this.meta.getCurrency());
	}
}
