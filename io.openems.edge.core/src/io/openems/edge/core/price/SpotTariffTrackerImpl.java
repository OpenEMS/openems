package io.openems.edge.core.price;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.services.currency.CurrencyExchange;
import io.openems.common.tariff.api.spottariff.SpotTariff;
import io.openems.common.tariff.api.spottariff.SpotTariffProvider;
import io.openems.common.types.BiddingZone;
import io.openems.common.types.TariffData;
import io.openems.common.utils.BehaviorSubject;
import io.openems.common.utils.SingletonPool;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.timeofusetariff.api.SpotTariffTracker;

public class SpotTariffTrackerImpl implements SpotTariffTracker, SingletonPool.SingletonPoolImplementation {
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private final BiddingZone biddingZone;
	private final CurrencyExchange currencyExchange;
	private final Meta meta;

	private final List<ProviderReference> providers = new ArrayList<>();
	private final BehaviorSubject<TariffData> tariffData = new BehaviorSubject<>(null);

	private final Consumer<TariffData> onPriceUpdateFunc = this::onPriceUpdate;
	private final BiConsumer<Value<Integer>, Value<Integer>> onCurrencyChangeFunc = //
			(o, n) -> this.onCurrencyOrExchangeRateUpdate();
	private final Consumer<Void> onExchangeRatesChanged = v -> this.onCurrencyOrExchangeRateUpdate();

	private boolean isCurrentlyChangingProviders;
	private TariffData lastSetTariffData;
	private volatile boolean deactivated;

	public SpotTariffTrackerImpl(BiddingZone biddingZone, CurrencyExchange currencyExchange, Meta meta) {
		this.biddingZone = biddingZone;
		this.currencyExchange = currencyExchange;
		this.meta = meta;
	}

	@Override
	public void activate() {
		this.meta.getCurrencyChannel().onChange(this.onCurrencyChangeFunc);
		this.currencyExchange.onExchangeRatesChanged().subscribe(this.onExchangeRatesChanged);
	}

	@Override
	public synchronized void deactivate() {
		this.deactivated = true;

		this.currencyExchange.onExchangeRatesChanged().unsubscribe(this.onExchangeRatesChanged);
		this.meta.getCurrencyChannel().removeOnChangeCallback(this.onCurrencyChangeFunc);

		this.providers.forEach(ProviderReference::unload);
		this.providers.clear();
	}

	@Override
	public BehaviorSubject<TariffData> getTariffData() {
		return this.tariffData;
	}

	@Override
	public boolean areProvidersAvailable() {
		return !this.providers.isEmpty();
	}

	public synchronized void bindProviders(List<SpotTariffProvider> providers) {
		if (this.deactivated) {
			return;
		}

		try {
			this.isCurrentlyChangingProviders = true;
			final var oldProviders = this.providers.stream().map(r -> r.provider).collect(Collectors.toSet());

			for (SpotTariffProvider provider : providers) {
				if (!provider.isSpotTariffSupported(this.biddingZone)) {
					continue;
				}

				if (oldProviders.remove(provider)) {
					continue;
				}

				try {
					var tariff = provider.getSpotTariff(this.biddingZone);
					tariff.getTariffData().subscribe(this.onPriceUpdateFunc);
					this.providers.add(new ProviderReference(provider, tariff));
				} catch (Exception ex) {
					this.log.error("Failed to initialize spot tariff provider {} for bidding zone {}",
							provider.getClass().getSimpleName(), this.biddingZone, ex);
				}
			}

			var itr = this.providers.iterator();
			while (itr.hasNext()) {
				final var activeProvider = itr.next();
				if (oldProviders.contains(activeProvider.provider)) {
					try {
						activeProvider.unload();
					} catch (Exception ex) {
						this.log.error("Failed to unload spot tariff provider {} for bidding zone {}",
								activeProvider.provider.getClass().getSimpleName(), this.biddingZone, ex);
					}
					itr.remove();
				}
			}

			this.providers.sort(Comparator.comparingInt((ProviderReference p) -> p.provider.getRanking()).reversed());
		} finally {
			this.isCurrentlyChangingProviders = false;
		}

		this.updatePrice();
	}

	private void onPriceUpdate(TariffData tariffData) {
		if (this.isCurrentlyChangingProviders) {
			return;
		}
		this.updatePrice();
	}

	private synchronized void onCurrencyOrExchangeRateUpdate() {
		this.updatePrice();
	}

	protected synchronized void updatePrice() {
		if (this.deactivated) {
			return;
		}

		var applicableTariffData = this.providers.stream() //
				.map(t -> t.tariff.getTariffData().getValue()) //
				.filter(Objects::nonNull) //
				.toList();

		var newTariffData = TariffData.byCombination(applicableTariffData, this.getSystemCurrency(),
				this.currencyExchange);
		if (this.lastSetTariffData == null || !this.lastSetTariffData.equals(newTariffData)) {
			this.lastSetTariffData = newTariffData;
			this.tariffData.setValue(newTariffData);
		}
	}

	private Currency getSystemCurrency() {
		return this.meta.getCurrency().getJavaCurrency();
	}

	private class ProviderReference {
		private final SpotTariffProvider provider;
		private final SpotTariff tariff;

		private ProviderReference(SpotTariffProvider provider, SpotTariff tariff) {
			this.provider = provider;
			this.tariff = tariff;
		}

		void unload() {
			this.tariff.getTariffData().unsubscribe(SpotTariffTrackerImpl.this.onPriceUpdateFunc);
			this.provider.ungetSpotTariff(this.tariff);
		}
	}
}
