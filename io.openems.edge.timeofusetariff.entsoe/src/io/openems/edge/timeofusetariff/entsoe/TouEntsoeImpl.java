package io.openems.edge.timeofusetariff.entsoe;

import java.io.IOException;
import java.time.Clock;
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

import com.google.common.collect.ImmutableSortedMap;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.ThreadPoolUtils;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.currency.Currency;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;
import io.openems.edge.timeofusetariff.api.utils.TimeOfUseTariffUtils;

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
	private final AtomicReference<ImmutableSortedMap<ZonedDateTime, Float>> prices = new AtomicReference<>(
			ImmutableSortedMap.of());

	@Reference
	private Meta meta;

	private Config config = null;
	private String securityToken = "";
	private ZonedDateTime updateTimeStamp = null;
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

		this.securityToken = Token.parseOrNull(config.securityToken());
		if (this.securityToken == null) {
			this.logError(this.log, "Please configure Security Token to access ENTSO-E");
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
		var areaCode = this.config.biddingZone().code;
		var fromDate = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS);
		var toDate = fromDate.plusDays(1);
		var unableToUpdatePrices = false;

		try {
			final var result = EntsoeApi.query(token, areaCode, fromDate, toDate);
			final var entsoeCurrency = Utils.parseCurrency(result);
			final var globalCurrency = this.meta.getCurrency();
			if (globalCurrency == Currency.UNDEFINED) {
				throw new OpenemsException("Global Currency is UNDEFINED. Please configure it in Core.Meta component");
			}

			final var exchangeRate = globalCurrency.name().equals(entsoeCurrency) //
					? 1 // No need to fetch exchange rate from API.
					: Utils.exchangeRateParser(ExchangeRateApi.getExchangeRates(), globalCurrency);

			// Parse the response for the prices
			this.prices.set(Utils.parsePrices(result, "PT60M", exchangeRate));

			// store the time stamp
			this.updateTimeStamp = ZonedDateTime.now();

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
		// return empty TimeOfUsePrices if data is not yet available.
		if (!this.config.enabled() || this.updateTimeStamp == null) {
			return TimeOfUsePrices.empty(ZonedDateTime.now());
		}

		return TimeOfUseTariffUtils.getNext24HourPrices(Clock.systemDefaultZone() /* can be mocked for testing */,
				this.prices.get(), this.updateTimeStamp);
	}

}
