package io.openems.edge.timeofusetariff.spottariff.provider;

import java.util.List;

import io.openems.common.tariff.api.TariffProvider;
import io.openems.common.tariff.api.spottariff.SpotTariff;
import io.openems.common.tariff.api.spottariff.SpotTariffProvider;
import io.openems.common.types.BiddingZone;
import io.openems.common.utils.BehaviorSubject;
import io.openems.edge.timeofusetariff.api.AbstractTariffComponent;
import io.openems.edge.timeofusetariff.api.TariffComponent;

public abstract class AbstractMirroredSpotTariffProviderComponent extends AbstractTariffComponent
		implements TariffComponent, TariffProvider, SpotTariffProvider {

	protected AbstractMirroredSpotTariffProviderComponent(
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	/**
	 * Returns the underlying provider to which all calls are delegated.
	 *
	 * @return Underlying provider instance or null, if not loaded yet.
	 */
	public abstract SpotTariffProvider getUnderlyingProvider();

	@Override
	public boolean isSpotTariffSupported(BiddingZone biddingZone) {
		final var provider = this.getUnderlyingProvider();
		if (provider == null) {
			return false;
		}
		return provider.isSpotTariffSupported(biddingZone);
	}

	@Override
	public SpotTariff getSpotTariff(BiddingZone biddingZone) {
		final var provider = this.getUnderlyingProvider();
		if (provider == null) {
			return null;
		}
		return provider.getSpotTariff(biddingZone);
	}

	@Override
	public void ungetSpotTariff(SpotTariff tariffObj) {
		final var provider = this.getUnderlyingProvider();
		if (provider != null) {
			provider.ungetSpotTariff(tariffObj);
		}
	}

	@Override
	public BehaviorSubject<List<LoadedTariffInfo>> getInfosAboutLoadedTariffs() {
		final var provider = this.getUnderlyingProvider();
		if (provider == null) {
			return new BehaviorSubject<>(List.of());
		}
		return provider.getInfosAboutLoadedTariffs();
	}

	@Override
	public int getRanking() {
		final var provider = this.getUnderlyingProvider();
		if (provider == null) {
			return 0;
		}
		return provider.getRanking();
	}
}
