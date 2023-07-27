package io.openems.edge.timeofusetariff.entsoe;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

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

	private final Logger log = LoggerFactory.getLogger(TouEntsoeImpl.class);
	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	private final AtomicReference<ImmutableSortedMap<ZonedDateTime, Float>> prices = new AtomicReference<>(
			ImmutableSortedMap.of());

	private Config config = null;
	private Currency currency;
	private ZonedDateTime updateTimeStamp = null;
	private static final int EUR_EXHANGE_RATE = 1;

	@Reference
	private CurrencyProvider currencyProvider;

	public TouEntsoeImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				TouEntsoe.ChannelId.values() //
		);
	}

	private final Consumer<Value<Integer>> callback = t -> {
		this.currency = t.asEnum();
		this.executor.schedule(this.task, 0, TimeUnit.SECONDS);
	};

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());

		if (!config.enabled()) {
			return;
		}
		this.config = config;
		this.currency = this.currencyProvider.getCurrent();
		this.executor.schedule(this.task, 0, TimeUnit.SECONDS);

		// Trigger to activate when the currency from Meta is updated.
		this.currencyProvider.subscribe(this.callback);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.currencyProvider.unsubscribe(this.callback);
		ThreadPoolUtils.shutdownAndAwaitTermination(this.executor, 0);
	}

	@Component
	public static class CurrencyProviderOsgi implements CurrencyProvider {

		@Reference
		private Meta meta;

		public Currency getCurrent() {
			return this.meta.getCurrencyChannel().getNextValue().asEnum();
		}

		/**
		 * Subscribes to the Currency channel to trigger on update.
		 * 
		 * @param consumer The callback {@link Consumer}.
		 */
		public void subscribe(Consumer<Value<Integer>> consumer) {
			this.meta.getCurrencyChannel().onSetNextValue(consumer);
		}

		/**
		 * Unsubscribe from the Currency channel.
		 * 
		 * @param consumer The callback {@link Consumer}.
		 */
		public void unsubscribe(Consumer<Value<Integer>> consumer) {
			this.meta.getCurrencyChannel().removeOnSetNextValueCallback(consumer);
		}
	}

	private final Runnable task = () -> {
		var token = this.config.securityToken();
		var areaCode = this.config.biddingZone().getName();
		var fromDate = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS);
		var toDate = fromDate.plusDays(1);
		var unableToUpdatePrices = false;

		try {
			var result = EntsoeApi.query(token, areaCode, fromDate, toDate);

			final double exchangeRate;
			if (this.currency == Currency.EUR) {
				// No need to fetch from API.
				exchangeRate = EUR_EXHANGE_RATE;
			} else {
				exchangeRate = Utils.exchangeRateParser(ExchangeRateApi.getExchangeRate(), this.currency);
			}

			// Parse the response for the prices
			this.prices.set(Utils.parse(result, "PT60M", exchangeRate));

			// store the time stamp
			this.updateTimeStamp = ZonedDateTime.now();
		} catch (IOException | ParserConfigurationException | SAXException | OpenemsNamedException e) {
			e.printStackTrace();
			if (e instanceof OpenemsNamedException) {
				this.logWarn(this.log, "Unable to get the currency exchange rate " + e.getMessage());
			} else {
				this.logWarn(this.log, "Unable to Update Entsoe Time-Of-Use Price: " + e.getMessage());
			}
			unableToUpdatePrices = true;
		}

		this.channel(TouEntsoe.ChannelId.UNABLE_TO_UPDATE_PRICES).setNextValue(unableToUpdatePrices);

		/*
		 * Schedule next price update at 2 o clock every day.
		 */
		var now = ZonedDateTime.now();
		var nextRun = now.withHour(14).truncatedTo(ChronoUnit.HOURS);
		if (unableToUpdatePrices) {
			// If the prices are not updated, try again in next minute.
			nextRun = now.plusMinutes(1).truncatedTo(ChronoUnit.MINUTES);
			this.logWarn(this.log, "Unable to Update the prices, Trying again at: " + nextRun);
		} else if (now.isAfter(nextRun)) {
			nextRun = nextRun.plusDays(1);
		}

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

}
