package io.openems.edge.timeofusetariff.entsoe;

import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.timeofusetariff.api.TouManualHelper.EMPTY_TOU_MANUAL_HELPER;
import static io.openems.edge.timeofusetariff.api.utils.ExchangeRateApi.getExchangeRateOrElse;
import static io.openems.edge.timeofusetariff.api.utils.TimeOfUseTariffUtils.generateDebugLog;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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
import io.openems.common.types.MarketPriceData;
import io.openems.common.utils.TimeRangeValues;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.timeofusetariff.entsoe.priceprovider.MarketPriceUpdateEvent;
import io.openems.edge.timeofusetariff.entsoe.priceprovider.EntsoeConfiguration;
import io.openems.edge.timeofusetariff.entsoe.priceprovider.EntsoeMarketPriceProvider;
import io.openems.edge.timeofusetariff.entsoe.priceprovider.EntsoeMarketPriceProviderPool;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;
import io.openems.edge.timeofusetariff.api.TouManualHelper;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "TimeOfUseTariff.ENTSO-E", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class TouEntsoeImpl extends AbstractOpenemsComponent implements TouEntsoe, OpenemsComponent, TimeOfUseTariff {
	private static final int INTERNAL_ERROR = -1;

	private final Logger log = LoggerFactory.getLogger(TouEntsoeImpl.class);
	private final AtomicReference<TimeRangeValues<Double>> prices = new AtomicReference<>(null);

	@Reference
	private EntsoeMarketPriceProviderPool entsoeMarketPriceProviderPool;

	@Reference
	private Meta meta;

	@Reference
	private ComponentManager componentManager;

	private TouManualHelper helper = TouManualHelper.EMPTY_TOU_MANUAL_HELPER;
	private EntsoeMarketPriceProvider priceProvider;

	private final Consumer<MarketPriceUpdateEvent> onUpdateEvent = this::onUpdateEvent;
	private final Consumer<MarketPriceData> onNewPrices = this::setPrices;
	private final BiConsumer<Value<Integer>, Value<Integer>> onCurrencyChange = (a, b) -> this
			.reloadPricesDueToCurrencyChange();

	public TouEntsoeImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				TouEntsoe.ChannelId.values() //
		);
	}

	private void reloadPricesDueToCurrencyChange() {
		if (this.priceProvider != null) {
			this.logInfo(this.log, "Triggering price update due to currency change ...");
			this.setPrices(this.priceProvider.getMarketPrices().getValue());
		}
	}

	@Override
	public void triggerPriceUpdate() {
		if (this.priceProvider != null) {
			this.priceProvider.triggerPriceUpdate();
		}
	}

	@Activate
	private synchronized void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());

		if (!config.enabled()) {
			return;
		}

		this.applySchedule(config);

		this.priceProvider = this.entsoeMarketPriceProviderPool
				.get(new EntsoeConfiguration(config.biddingZone(), config.securityToken()));

		this.priceProvider.getMarketPrices().subscribe(this.onNewPrices);
		this.priceProvider.getUpdateState().subscribe(this.onUpdateEvent);

		// React on updates to Currency.
		this.meta.getCurrencyChannel().onChange(this.onCurrencyChange);
	}

	private void applySchedule(Config config) {
		final var clock = this.componentManager.getClock();

		try {
			final var schedule = Utils.parseToSchedule(clock, config.biddingZone(), config.ancillaryCosts(),
					msg -> this.logWarn(this.log, msg));
			this.helper = new TouManualHelper(clock, schedule, 0.0);

		} catch (OpenemsNamedException e) {
			this.logWarn(this.log, "Unable to parse Schedule: " + e.getMessage());
			this.helper = EMPTY_TOU_MANUAL_HELPER;
		}
	}

	@Deactivate
	protected synchronized void deactivate() {
		super.deactivate();

		this.meta.getCurrencyChannel().removeOnChangeCallback(this.onCurrencyChange);

		if (this.priceProvider != null) {
			this.priceProvider.getMarketPrices().unsubscribe(this.onNewPrices);
			this.priceProvider.getUpdateState().unsubscribe(this.onUpdateEvent);
			this.entsoeMarketPriceProviderPool.unget(this.priceProvider);
			this.priceProvider = null;
		}
	}

	protected void onUpdateEvent(MarketPriceUpdateEvent event) {
		if (event == null) {
			return;
		}

		switch (event) {
		case MarketPriceUpdateEvent.Successful(var data) -> {
			setValue(this, TouEntsoe.ChannelId.HTTP_STATUS_CODE, 200);
			setValue(this, TouEntsoe.ChannelId.UNABLE_TO_UPDATE_PRICES, false);
		}
		case MarketPriceUpdateEvent.FailedWithHttpError(var httpStatus, var httpBody) -> {
			setValue(this, TouEntsoe.ChannelId.HTTP_STATUS_CODE, httpStatus.code());
			setValue(this, TouEntsoe.ChannelId.UNABLE_TO_UPDATE_PRICES, true);
		}
		case MarketPriceUpdateEvent.FailedWithException(var exception) -> {
			setValue(this, TouEntsoe.ChannelId.HTTP_STATUS_CODE, INTERNAL_ERROR);
			setValue(this, TouEntsoe.ChannelId.UNABLE_TO_UPDATE_PRICES, true);
		}
		}
	}

	protected void setPrices(MarketPriceData marketPriceData) {
		if (marketPriceData == null) {
			this.prices.set(null);
			return;
		}

		final var globalCurrency = this.meta.getCurrency();
		final double exchangeRate = getExchangeRateOrElse(marketPriceData.getCurrency(), globalCurrency, 1.);
		final var gridFees = this.helper.getPrices();

		final var processedPrices = Utils.processPrices(this.componentManager.getClock(), marketPriceData.getValues(),
				exchangeRate, gridFees);
		this.prices.set(processedPrices);
	}

	@Override
	public TimeOfUsePrices getPrices() {
		var currentPrices = this.prices.get();
		if (currentPrices == null) {
			return TimeOfUsePrices.EMPTY_PRICES;
		}

		return TimeOfUsePrices.from(Instant.now(this.componentManager.getClock()), currentPrices);
	}

	@Override
	public String debugLog() {
		return generateDebugLog(this, this.meta.getCurrency());
	}
}
