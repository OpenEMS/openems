package io.openems.common.tariff.api.spottariff.dummy;

import io.openems.common.tariff.api.TariffProvider;
import io.openems.common.tariff.api.TariffUpdateStatus;
import io.openems.common.tariff.api.spottariff.SpotTariff;
import io.openems.common.types.BiddingZone;
import io.openems.common.types.TariffData;
import io.openems.common.utils.BehaviorSubject;
import io.openems.common.utils.SingletonPool;

public class DummySpotTariff implements SpotTariff, SingletonPool.SingletonPoolImplementation {
	private final BiddingZone biddingZone;
	private final BehaviorSubject<TariffData> tariffData;
	private final BehaviorSubject<TariffUpdateStatus> tariffUpdateStatus;
	private final Runnable onUpdate;

	public DummySpotTariff(BiddingZone biddingZone, BehaviorSubject<TariffData> tariffData,
			BehaviorSubject<TariffUpdateStatus> tariffUpdateStatus, Runnable onUpdate) {
		this.biddingZone = biddingZone;
		this.tariffData = tariffData;
		this.tariffUpdateStatus = tariffUpdateStatus;
		this.onUpdate = onUpdate;
	}

	@Override
	public String getIdentifier() {
		return "DummySpotTariff";
	}

	@Override
	public BehaviorSubject<TariffData> getTariffData() {
		return this.tariffData;
	}

	@Override
	public BehaviorSubject<TariffUpdateStatus> getLastUpdateStatus() {
		return this.tariffUpdateStatus;
	}

	@Override
	public BiddingZone getBiddingZone() {
		return this.biddingZone;
	}

	public TariffProvider.LoadedTariffInfo getInfo() {
		return new TariffProvider.LoadedTariffInfo(this, this.tariffUpdateStatus.getValue());
	}

	@Override
	public void activate() {
		this.onUpdate.run();
	}

	@Override
	public void deactivate() {
		this.onUpdate.run();
	}
}
