package io.openems.backend.tariff.manager.impl;

import static org.osgi.service.component.annotations.ServiceScope.SINGLETON;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import io.openems.backend.tariff.manager.BackendSpotTariffProvider;
import io.openems.backend.tariff.manager.BackendTariffManager;
import io.openems.backend.tariff.manager.SpotTariffTracker;
import io.openems.common.event.EventEmitter;
import io.openems.common.services.currency.CurrencyExchange;
import io.openems.common.tariff.api.spottariff.SpotTariffProvider;
import io.openems.common.types.BiddingZone;
import io.openems.common.types.Tuple2;

@Component(scope = SINGLETON)
public class BackendTariffManagerImpl implements BackendTariffManager {

	@Reference
	protected CurrencyExchange currencyExchange;

	@Reference(//
			cardinality = ReferenceCardinality.MULTIPLE, //
			policy = ReferencePolicy.DYNAMIC, //
			bind = "bindSpotTariffProvider", //
			unbind = "unbindSpotTariffProvider" //
	)
	protected volatile List<BackendSpotTariffProvider> spotTariffProviders;

	protected void bindSpotTariffProvider(SpotTariffProvider provider) {
		this.updateSpotTariffProviders();
	}

	protected void unbindSpotTariffProvider(SpotTariffProvider provider) {
		this.updateSpotTariffProviders();
	}

	private final EventEmitter<Void> onSpotTariffProvidersChange = new EventEmitter<>();
	private final Map<BiddingZone, SpotTariffTrackerImpl> spotTariffTrackersByBiddingZone = new EnumMap<>(
			BiddingZone.class);

	@Override
	public synchronized SpotTariffTracker getSpotTariff(BiddingZone biddingZone) {
		return this.spotTariffTrackersByBiddingZone.get(biddingZone);
	}

	@Override
	public EventEmitter<Void> getOnSpotTariffProvidersChange() {
		return this.onSpotTariffProvidersChange;
	}

	@Override
	public synchronized List<BiddingZone> getAllSupportedBiddingZones() {
		return new ArrayList<>(this.spotTariffTrackersByBiddingZone.keySet());
	}

	private SpotTariffTrackerImpl createTracker(BiddingZone biddingZone) {
		var tracker = new SpotTariffTrackerImpl(biddingZone, this.currencyExchange);
		tracker.activate();

		return tracker;
	}

	private synchronized void updateSpotTariffProviders() {
		var newProvidersByBiddingZone = this.spotTariffProviders.stream() //
				.flatMap(x -> Arrays.stream(x.getFetchableBiddingZones()) //
						.map(z -> new Tuple2<>(z, x))) //
				.collect(Collectors.groupingBy(Tuple2::a, Collectors.mapping(Tuple2::b, Collectors.toList())));

		for (var entry : newProvidersByBiddingZone.entrySet()) {
			var tracker = this.spotTariffTrackersByBiddingZone.computeIfAbsent(entry.getKey(), this::createTracker);
			tracker.bindProviders(entry.getValue().stream() //
					.map(x -> (SpotTariffProvider) x) //
					.toList());
		}

		// Remove old trackers
		{
			var itr = this.spotTariffTrackersByBiddingZone.entrySet().iterator();
			while (itr.hasNext()) {
				var entry = itr.next();
				if (!newProvidersByBiddingZone.containsKey(entry.getKey())) {
					entry.getValue().deactivate();
					itr.remove();
				}
			}
		}

		this.onSpotTariffProvidersChange.emit(null);
	}

}
