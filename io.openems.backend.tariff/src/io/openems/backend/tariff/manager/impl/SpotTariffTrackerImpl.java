package io.openems.backend.tariff.manager.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.tariff.manager.SpotTariffTracker;
import io.openems.common.services.currency.CurrencyExchange;
import io.openems.common.tariff.api.spottariff.SpotTariff;
import io.openems.common.tariff.api.spottariff.SpotTariffProvider;
import io.openems.common.types.BiddingZone;
import io.openems.common.types.TariffData;
import io.openems.common.utils.BehaviorSubject;

public class SpotTariffTrackerImpl implements SpotTariffTracker {

	private final Logger log = LoggerFactory.getLogger(SpotTariffTrackerImpl.class);
	private final BiddingZone biddingZone;
	private final CurrencyExchange currencyExchange;

	private final BehaviorSubject<TariffData> tariffData = new BehaviorSubject<>(null);
	private final List<ProviderReference> providers = new ArrayList<>();

	private final Consumer<TariffData> onPriceUpdateFunc = this::onPriceUpdate;
	private final Consumer<Void> onExchangeRatesChanged = v -> this.onExchangeRateUpdate();

	private volatile boolean deactivated;
	private boolean isCurrentlyChangingProviders;
	private TariffData lastSetTariffData;

	public SpotTariffTrackerImpl(BiddingZone biddingZone, CurrencyExchange currencyExchange) {
		this.biddingZone = biddingZone;
		this.currencyExchange = currencyExchange;
	}

	public void activate() {
		this.currencyExchange.onExchangeRatesChanged().subscribe(this.onExchangeRatesChanged);
	}

	public synchronized void deactivate() {
		this.deactivated = true;
		this.currencyExchange.onExchangeRatesChanged().unsubscribe(this.onExchangeRatesChanged);
	}

	private void onExchangeRateUpdate() {
		this.updatePrice();
	}

	private void onPriceUpdate(TariffData newTariffData) {
		if (this.isCurrentlyChangingProviders) {
			return;
		}
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

		var newTariffData = TariffData.byCombination(applicableTariffData, this.biddingZone.getDefaultCurrency(),
				this.currencyExchange);
		if (this.lastSetTariffData == null || !this.lastSetTariffData.equals(newTariffData)) {
			this.lastSetTariffData = newTariffData;
			this.tariffData.setValue(newTariffData);
		}
	}

	public synchronized void bindProviders(List<SpotTariffProvider> providers) {
		if (this.deactivated) {
			return;
		}

		try {
			this.isCurrentlyChangingProviders = true;
			final var oldProviders = new HashSet<>(this.providers.stream().map(r -> r.provider).toList());

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

	@Override
	public BehaviorSubject<TariffData> getTariffData() {
		return this.tariffData;
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
