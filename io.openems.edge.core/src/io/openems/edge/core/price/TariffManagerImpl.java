package io.openems.edge.core.price;

import java.util.List;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

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
}
