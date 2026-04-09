package io.openems.edge.timeofusetariff.manual.eeg2025.gridsell;

import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.timeofusetariff.api.utils.TariffGridSellUtils.generateDebugLog;
import static io.openems.edge.timeofusetariff.manual.eeg2025.gridsell.Utils.processPrices;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import com.google.common.annotations.VisibleForTesting;

import io.openems.common.types.MarketPriceData;
import io.openems.common.utils.TimeRangeValues;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.timeofusetariff.api.TariffGridSell;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.entsoe.priceprovider.EntsoeConfiguration;
import io.openems.edge.timeofusetariff.entsoe.priceprovider.EntsoeMarketPriceProvider;
import io.openems.edge.timeofusetariff.entsoe.priceprovider.EntsoeMarketPriceProviderPool;
import io.openems.edge.timeofusetariff.entsoe.priceprovider.MarketPriceUpdateEvent;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Tariff.Manual.EEG2025.GridSell", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class TariffManualEeg2025GridSellImpl extends AbstractOpenemsComponent
		implements OpenemsComponent, TariffGridSell, TariffManualEeg2025GridSell {

	private static final int INTERNAL_ERROR = -1;

	@Reference
	private EntsoeMarketPriceProviderPool marketPriceProviderPool;

	@Reference
	private ComponentManager componentManager;

	@Reference
	private Meta meta;

	private final Consumer<MarketPriceData> onNewMarketPrices = this::setPrices;
	private final Consumer<MarketPriceUpdateEvent> onUpdateEvent = this::onUpdateEvent;
	private final AtomicReference<TimeRangeValues<Double>> prices = new AtomicReference<>(null);

	private double fixedGridSellPrice;
	private EntsoeMarketPriceProvider marketPriceProvider;

	public TariffManualEeg2025GridSellImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				TariffManualEeg2025GridSell.ChannelId.values()//
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());

		if (!config.enabled()) {
			return;
		}

		this.fixedGridSellPrice = config.fixedGridSellPrice();
		this.marketPriceProvider = this.marketPriceProviderPool
				.get(new EntsoeConfiguration(config.biddingZone(), config.securityToken()));

		this.marketPriceProvider.getMarketPrices().subscribe(this.onNewMarketPrices);
		this.marketPriceProvider.getUpdateState().subscribe(this.onUpdateEvent);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();

		if (this.marketPriceProvider != null) {
			this.marketPriceProvider.getMarketPrices().unsubscribe(this.onNewMarketPrices);
			this.marketPriceProvider.getUpdateState().unsubscribe(this.onUpdateEvent);
			this.marketPriceProviderPool.unget(this.marketPriceProvider);
			this.marketPriceProvider = null;
		}
	}

	@Override
	public TimeOfUsePrices getGridSellPrices() {
		var currentPrices = this.prices.get();
		if (currentPrices == null) {
			return TimeOfUsePrices.EMPTY_PRICES;
		}

		return TimeOfUsePrices.from(Instant.now(this.componentManager.getClock()), currentPrices);
	}

	private void setPrices(MarketPriceData marketPriceData) {
		final var prices = marketPriceData == null //
				? null //
				: processPrices(this.fixedGridSellPrice, marketPriceData.getValues());
		this.prices.set(prices);
	}

	private void onUpdateEvent(MarketPriceUpdateEvent event) {
		switch (event) {
		case null -> doNothing();
		case MarketPriceUpdateEvent.Successful(var data) -> {
			setValue(this, TariffManualEeg2025GridSell.ChannelId.HTTP_STATUS_CODE, 200);
			setValue(this, TariffManualEeg2025GridSell.ChannelId.UNABLE_TO_FETCH_MARKET_PRICES, false);
		}
		case MarketPriceUpdateEvent.FailedWithHttpError(var httpStatus, var httpBody) -> {
			setValue(this, TariffManualEeg2025GridSell.ChannelId.HTTP_STATUS_CODE, httpStatus.code());
			setValue(this, TariffManualEeg2025GridSell.ChannelId.UNABLE_TO_FETCH_MARKET_PRICES, true);
		}
		case MarketPriceUpdateEvent.FailedWithException(var exception) -> {
			setValue(this, TariffManualEeg2025GridSell.ChannelId.HTTP_STATUS_CODE, INTERNAL_ERROR);
			setValue(this, TariffManualEeg2025GridSell.ChannelId.UNABLE_TO_FETCH_MARKET_PRICES, true);
		}
		}
	}

	@Override
	public String debugLog() {
		return generateDebugLog(this, this.meta.getCurrency());
	}

	@VisibleForTesting
	void triggerPriceUpdate() {
		if (this.marketPriceProvider != null) {
			this.marketPriceProvider.triggerPriceUpdate();
		}
	}
}
