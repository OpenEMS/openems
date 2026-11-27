package io.openems.edge.core.price;

import java.util.List;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import io.openems.common.services.currency.CurrencyExchange;
import io.openems.common.tariff.api.spottariff.SpotTariffProvider;
import io.openems.common.types.BiddingZone;
import io.openems.common.utils.SingletonPool;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.timeofusetariff.api.SpotTariffTracker;
import io.openems.edge.timeofusetariff.api.TariffGridSell;
import io.openems.edge.timeofusetariff.api.TariffManager;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;

@Component
public class TariffManagerImpl implements TariffManager {

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	private volatile List<TimeOfUseTariff> tariffGridBuyProviders;

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	private volatile List<TariffGridSell> tariffGridSellProviders;

	@Reference(//
			cardinality = ReferenceCardinality.MULTIPLE, //
			policy = ReferencePolicy.DYNAMIC, //
			bind = "bindSpotTariffProvider", //
			unbind = "unbindSpotTariffProvider" //
	)
	protected volatile List<SpotTariffProvider> spotTariffProviders;

	protected void bindSpotTariffProvider(SpotTariffProvider provider) {
		this.updateSpotTariffProviders();
	}

	protected void unbindSpotTariffProvider(SpotTariffProvider provider) {
		this.updateSpotTariffProviders();
	}

	@Reference
	protected CurrencyExchange currencyExchange;

	@Reference
	protected Meta meta;

	private final SpotTariffPool spotTariffPool = new SpotTariffPool();

	@Activate
	public TariffManagerImpl() {
	}

	@Override
	public TimeOfUsePrices getGridBuyDayAheadPrices() {
		return this.tariffGridBuyProviders.stream()//
				.findFirst()//
				.map(TimeOfUseTariff::getPrices)//
				.orElse(TimeOfUsePrices.EMPTY_PRICES);
	}

	@Override
	public TimeOfUsePrices getGridSellDayAheadPrices() {
		return this.tariffGridSellProviders.stream()//
				.findFirst()//
				.map(TariffGridSell::getGridSellPrices)//
				.orElse(TimeOfUsePrices.EMPTY_PRICES);
	}

	@Override
	public SpotTariffTracker getSpotTariff(BiddingZone biddingZone) {
		return this.spotTariffPool.get(biddingZone);
	}

	@Override
	public void ungetSpotTariff(SpotTariffTracker spotTariffTracker) {
		this.spotTariffPool.unget((SpotTariffTrackerImpl) spotTariffTracker);
	}

	private synchronized void updateSpotTariffProviders() {
		for (var instance : this.spotTariffPool.getAllInstances()) {
			instance.bindProviders(this.spotTariffProviders);
		}
	}

	private class SpotTariffPool extends SingletonPool<BiddingZone, SpotTariffTrackerImpl> {
		@Override
		protected SpotTariffTrackerImpl createNewInstance(BiddingZone biddingZone) {
			var tracker = new SpotTariffTrackerImpl(biddingZone, TariffManagerImpl.this.currencyExchange,
					TariffManagerImpl.this.meta);
			tracker.bindProviders(TariffManagerImpl.this.spotTariffProviders);
			return tracker;
		}
	}
}
