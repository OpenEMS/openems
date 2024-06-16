package io.openems.edge.timeofusetariff.entsoe;

import static io.openems.common.utils.StringUtils.definedOrElse;
import static io.openems.edge.timeofusetariff.api.utils.ExchangeRateApi.getExchangeRate;
import static io.openems.edge.timeofusetariff.api.utils.TimeOfUseTariffUtils.generateDebugLog;
import static io.openems.edge.timeofusetariff.entsoe.Utils.parseCurrency;
import static io.openems.edge.timeofusetariff.entsoe.Utils.parsePrices;

import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import javax.xml.parsers.ParserConfigurationException;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.utils.ThreadPoolUtils;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.currency.Currency;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "TimeOfUseTariff.ENTSO-E", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class TouEntsoeImpl extends AbstractOpenemsComponent implements TouEntsoe, OpenemsComponent, TimeOfUseTariff {

	private static final int API_EXECUTE_HOUR = 14;

	private final Logger log = LoggerFactory.getLogger(TouEntsoeImpl.class);
	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	private final AtomicReference<TimeOfUsePrices> prices = new AtomicReference<>(TimeOfUsePrices.EMPTY_PRICES);

	@Reference
	private Meta meta;

	@Reference
	private OpenemsEdgeOem oem;

	private Config config = null;
	private String securityToken = null;
	private String exchangerateAccesskey = null;
	private ScheduledFuture<?> future = null;

	public TouEntsoeImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				TouEntsoe.ChannelId.values() //
		);
	}

	private final BiConsumer<Value<Integer>, Value<Integer>> onCurrencyChange = (a, b) -> {
		this.scheduleTask(0);
	};

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());

		if (!config.enabled()) {
			return;
		}

		this.securityToken = definedOrElse(config.securityToken(), this.oem.getEntsoeToken());
		if (this.securityToken == null) {
			this.logError(this.log, "Please configure Security Token to access ENTSO-E");
			return;
		}

		this.exchangerateAccesskey = definedOrElse(config.exchangerateAccesskey(), this.oem.getExchangeRateAccesskey());
		if (this.exchangerateAccesskey == null) {
			this.logError(this.log, "Please configure personal Access key to access Exchange rate host API");
			return;
		}
		this.config = config;

		// React on updates to Currency.
		this.meta.getCurrencyChannel().onChange(this.onCurrencyChange);

		// Schedule once
		this.scheduleTask(0);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.meta.getCurrencyChannel().removeOnChangeCallback(this.onCurrencyChange);
		ThreadPoolUtils.shutdownAndAwaitTermination(this.executor, 0);
	}

	/**
	 * Schedules execution the the update Task.
	 * 
	 * @param seconds execute task in seconds
	 */
	private synchronized void scheduleTask(long seconds) {
		if (this.future != null) {
			this.future.cancel(false);
		}
		this.future = this.executor.schedule(this.task, seconds, TimeUnit.SECONDS);
	}

	private final Runnable task = () -> {
		var token = this.securityToken;
		var exchangerateAccesskey = this.exchangerateAccesskey;
		var areaCode = this.config.biddingZone().code;
		var fromDate = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS);
		var toDate = fromDate.plusDays(1);
		var unableToUpdatePrices = false;

		try {
			final var result = EntsoeApi.query(token, areaCode, fromDate, toDate);
			final var entsoeCurrency = parseCurrency(result);
			final var globalCurrency = this.meta.getCurrency();
			if (globalCurrency == Currency.UNDEFINED) {
				throw new OpenemsException("Global Currency is UNDEFINED. Please configure it in Core.Meta component");
			}

			final var exchangeRate = globalCurrency.name().equals(entsoeCurrency) //
					? 1. // No need to fetch exchange rate from API.
					: getExchangeRate(exchangerateAccesskey, entsoeCurrency, globalCurrency);
			// Parse the response for the prices
			this.prices.set(parsePrices(result, "PT60M", exchangeRate));

		} catch (IOException | ParserConfigurationException | SAXException | OpenemsNamedException e) {
			this.logWarn(this.log, "Unable to Update Entsoe Time-Of-Use Price: " + e.getMessage());
			e.printStackTrace();
			unableToUpdatePrices = true;
		}

		this.channel(TouEntsoe.ChannelId.UNABLE_TO_UPDATE_PRICES).setNextValue(unableToUpdatePrices);

		/*
		 * Schedule next price update at 2 o clock every day.
		 */
		var now = ZonedDateTime.now();
		var nextRun = now.withHour(API_EXECUTE_HOUR).truncatedTo(ChronoUnit.HOURS);
		if (unableToUpdatePrices) {
			// If the prices are not updated, try again in next minute.
			nextRun = now.plusMinutes(1).truncatedTo(ChronoUnit.MINUTES);
			this.logWarn(this.log, "Unable to Update the prices, Trying again at: " + nextRun);
		} else if (now.isAfter(nextRun)) {
			nextRun = nextRun.plusDays(1);
		}

		var delay = Duration.between(now, nextRun).getSeconds();
		this.scheduleTask(delay);
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
