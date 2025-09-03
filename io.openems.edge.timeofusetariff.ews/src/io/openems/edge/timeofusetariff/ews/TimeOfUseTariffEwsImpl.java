package io.openems.edge.timeofusetariff.ews;

import static io.openems.edge.timeofusetariff.api.utils.TimeOfUseTariffUtils.generateDebugLog;
import static io.openems.edge.timeofusetariff.ews.Utils.calculateDelay;

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
		name = "TimeOfUseTariff.Ews", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class TimeOfUseTariffEwsImpl extends AbstractOpenemsComponent
		implements TimeOfUseTariff, OpenemsComponent, TimeOfUseTariffEws {

	private static final String EWS_API_URL = "https://api.ews-schoenau.de/v1/dynamicprices/EWS-OEKO-DYN";
	protected static final int CLIENT_ERROR_CODE = 401;
	
	private final Logger log = LoggerFactory.getLogger(TimeOfUseTariffEwsImpl.class);
	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	private final AtomicReference<TimeOfUsePrices> prices = new AtomicReference<>(TimeOfUsePrices.EMPTY_PRICES);

	@Reference
	private Meta meta;

	@Reference
	private ComponentManager componentManager;

	private Config config = null;

	public TimeOfUseTariffEwsImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				TimeOfUseTariffEws.ChannelId.values() //
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
				.url(EWS_API_URL) //
				.header("X-API-KEY", this.config.accessToken()) //
				.get() //
				.build();
		var httpStatusCode = 0;
		var unableToUpdatePrices = false;

		try (var response = client.newCall(request).execute()) {
			httpStatusCode = response.code();

			if (!response.isSuccessful()) {
				throw new IOException("Unexpected code " + response);
			}

			// Parse the response for the prices
			this.prices.set(Utils.parsePrices(response.body().string()));

		} catch (IOException | OpenemsNamedException e) {
			unableToUpdatePrices = true;
			e.printStackTrace();
		}

		this.setChannelValues(httpStatusCode);

		var delay = calculateDelay(httpStatusCode, unableToUpdatePrices);
		if (delay != 0) {
			this.executor.schedule(this.task, delay, TimeUnit.SECONDS);
		}
	};
	
	/**
	 * Sets the values of specific channels based on the provided parameters.
	 * 
	 * @param httpStatusCode   The HTTP status code received from the API.
	 */
	private void setChannelValues(int httpStatusCode) {
		var authenticationFailed = false;
		var serverError = false;
		var timeout = false;

		switch (httpStatusCode) {
		case CLIENT_ERROR_CODE:
			authenticationFailed = true;
			this.logWarn(this.log, "Authentication failed, please try again with valid token.");
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

		this.channel(TimeOfUseTariffEws.ChannelId.HTTP_STATUS_CODE).setNextValue(httpStatusCode);
		this.channel(TimeOfUseTariffEws.ChannelId.STATUS_TIMEOUT).setNextValue(timeout);
		this.channel(TimeOfUseTariffEws.ChannelId.STATUS_AUTHENTICATION_FAILED).setNextValue(authenticationFailed);
		this.channel(TimeOfUseTariffEws.ChannelId.STATUS_SERVER_ERROR).setNextValue(serverError);
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
