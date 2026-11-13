package io.openems.edge.timeofusetariff.energypriceforecast;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.tariff.api.Tariff;
import io.openems.common.tariff.api.TariffProvider;
import io.openems.common.tariff.api.TariffUpdateStatus;
import io.openems.common.tariff.api.spottariff.SpotTariff;
import io.openems.common.tariff.api.spottariff.SpotTariffProvider;
import io.openems.common.types.BiddingZone;
import io.openems.common.utils.BehaviorSubject;
import io.openems.common.utils.SingletonPool;
import io.openems.edge.common.channel.ChannelUtils;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.timeofusetariff.api.AbstractTariffComponent;
import io.openems.edge.timeofusetariff.api.TariffComponent;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Tariff.SpotTariff.EnergyPriceForecast", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
@EventTopics(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)
public class EnergyPriceForecastImpl extends AbstractTariffComponent implements EnergyPriceForecast, OpenemsComponent,
		TariffComponent, TariffProvider, SpotTariffProvider, EventHandler {

	@Reference
	private ComponentManager componentManager;

	@Reference
	private BridgeHttpFactory bridgeHttpFactory;

	private final ForecastPool forecasts = new ForecastPool();
	private final BehaviorSubject<List<LoadedTariffInfo>> infos = new BehaviorSubject<>(List.of());
	private Config config;

	public EnergyPriceForecastImpl() {
		super(OpenemsComponent.ChannelId.values(), TariffComponent.ChannelId.values(),
				EnergyPriceForecast.ChannelId.values());
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		if (config.refreshIntervalMinutes() < 5 || config.refreshIntervalMinutes() > 1440
				|| config.maxForecastAgeHours() < 1 || config.maxForecastAgeHours() > 48) {
			throw new IllegalArgumentException("Refresh interval or maximum forecast age is out of range");
		}
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		this.forecasts.dispose();
		this.updateInfos();
		super.deactivate();
	}

	@Override
	public boolean isSpotTariffSupported(BiddingZone biddingZone) {
		return this.isEnabled() && ForecastMarket.forBiddingZone(biddingZone) != null;
	}

	@Override
	public SpotTariff getSpotTariff(BiddingZone biddingZone) {
		if (!this.isSpotTariffSupported(biddingZone)) {
			throw new IllegalArgumentException("Energy Price Forecast is disabled or does not support " + biddingZone);
		}
		return this.forecasts.get(biddingZone);
	}

	@Override
	public void ungetSpotTariff(SpotTariff tariff) {
		if (tariff instanceof ForecastSpotTariff forecast) {
			this.forecasts.unget(forecast);
		}
	}

	@Override
	public int getRanking() {
		return this.config.service_ranking();
	}

	@Override
	public Clock getClock() {
		return this.componentManager.getClock();
	}

	@Override
	public BehaviorSubject<List<LoadedTariffInfo>> getInfosAboutLoadedTariffs() {
		return this.infos;
	}

	@Override
	protected void setChannelsForUpdateFailures(List<TariffUpdateStatus> failures) {
		ChannelUtils.setValue(this, EnergyPriceForecast.ChannelId.UPDATE_FAILURE, !failures.isEmpty());
	}

	@Override
	protected String getChannelPrefixForTariff(Tariff tariff) {
		return ((SpotTariff) tariff).getBiddingZone().name();
	}

	@Override
	public void handleEvent(Event event) {
		if (this.isEnabled() && event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
			this.forecasts.getAllInstances().forEach(ForecastSpotTariff::expireOutdatedPrices);
		}
		super.handleEvent(event);
	}

	private void updateInfos() {
		this.infos.setValue(this.forecasts.getAllInstances().stream()
				.map(f -> new LoadedTariffInfo(f, f.getLastUpdateStatus().getValue())).toList());
	}

	private class ForecastPool extends SingletonPool<BiddingZone, ForecastSpotTariff> {
		@Override
		protected ForecastSpotTariff createNewInstance(BiddingZone biddingZone) {
			return new ForecastSpotTariff(biddingZone, EnergyPriceForecastImpl.this.id(),
					EnergyPriceForecastImpl.this.bridgeHttpFactory, EnergyPriceForecastImpl.this.getClock(),
					Duration.ofMinutes(EnergyPriceForecastImpl.this.config.refreshIntervalMinutes()),
					Duration.ofHours(EnergyPriceForecastImpl.this.config.maxForecastAgeHours()));
		}

		@Override
		protected void onPostLoadInstance(ForecastSpotTariff forecast) {
			forecast.getLastUpdateStatus().subscribe(s -> EnergyPriceForecastImpl.this.updateInfos());
		}

		@Override
		protected void onPostUnloadInstance(ForecastSpotTariff forecast) {
			EnergyPriceForecastImpl.this.updateInfos();
		}
	}
}
