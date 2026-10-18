package io.openems.edge.timeofusetariff.manual.eeg2025.gridsell;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.tariff.api.Tariff;
import io.openems.common.tariff.api.TariffProvider;
import io.openems.common.tariff.api.TariffUpdateStatus;
import io.openems.common.tariff.api.spottariff.SpotTariff;
import io.openems.common.tariff.entsoe.EntsoeTariffConfig;
import io.openems.common.tariff.entsoe.EntsoeTariffProvider;
import io.openems.common.timedata.DurationUnit;
import io.openems.common.types.BiddingZone;
import io.openems.common.types.TariffData;
import io.openems.common.types.TariffType;
import io.openems.common.utils.BehaviorSubject;
import io.openems.common.utils.TimeRangeValues;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.timeofusetariff.api.AbstractTariffComponent;
import io.openems.edge.timeofusetariff.api.SpotTariffTracker;
import io.openems.edge.timeofusetariff.api.TariffComponent;
import io.openems.edge.timeofusetariff.api.TariffGridSell;
import io.openems.edge.timeofusetariff.api.TariffManager;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Tariff.Manual.EEG2025.GridSell", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class TariffManualEeg2025GridSellImpl extends AbstractTariffComponent implements TariffManualEeg2025GridSell,
		OpenemsComponent, TariffGridSell, TariffComponent, TariffProvider, Tariff, EventHandler {

	private final Logger log = LoggerFactory.getLogger(TariffManualEeg2025GridSellImpl.class);

	@Reference
	private ComponentManager componentManager;

	@Reference
	private Meta meta;

	@Reference
	private TariffManager tariffManager;

	@Reference
	private OpenemsEdgeOem oem;

	@Reference
	private BridgeHttpFactory bridgeHttpFactory;

	private final Consumer<TariffData> onNewSpotTariffDataFunc = this::onNewSpotTariffData;

	private final BehaviorSubject<TariffData> tariffData = new BehaviorSubject<>(null);
	private final BehaviorSubject<TariffUpdateStatus> updateStatus = new BehaviorSubject<>(null);
	private final BehaviorSubject<List<TariffProvider.LoadedTariffInfo>> infosAboutLoadedTariffs = new BehaviorSubject<>(
			List.of());

	private Config config;
	private SpotTariffTracker spotTariffTracker;
	private boolean activated;

	private EntsoeTariffProvider deprecatedProvider;
	private SpotTariff deprecatedSpotTariff;

	public TariffManualEeg2025GridSellImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				TariffComponent.ChannelId.values(), //
				TariffManualEeg2025GridSell.ChannelId.values()//
		);
	}

	@Activate
	private synchronized void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.updateStatus.setValue(new TariffUpdateStatus.NotFetchedYet(this.getClock().instant()));

		if (!config.enabled()) {
			return;
		}

		this.config = config;

		this.spotTariffTracker = this.tariffManager.getSpotTariff(BiddingZone.GERMANY);
		this.spotTariffTracker.getTariffData().subscribe(this.onNewSpotTariffDataFunc);

		this.updateInfosAboutLoadedTariffs();
		this.activated = true;

		if (!this.spotTariffTracker.areProvidersAvailable()) {
			CompletableFuture.delayedExecutor(30, TimeUnit.SECONDS).execute(() -> {
				synchronized (this) {
					if (this.activated && !this.spotTariffTracker.areProvidersAvailable()) {
						this.logInfo(this.log,
								"No spot tariff providers available, enabling deprecated direct entso-e provider");
						this.enableDeprecatedDirectEntsoeProvider();
					}
				}
			});
		}
	}

	@Deactivate
	@Override
	protected synchronized void deactivate() {
		this.activated = false;
		super.deactivate();

		if (this.deprecatedSpotTariff != null) {
			this.deprecatedSpotTariff.getTariffData().unsubscribe(this.onNewSpotTariffDataFunc);
			if (this.deprecatedProvider != null) {
				this.deprecatedProvider.ungetSpotTariff(this.deprecatedSpotTariff);
			}
			this.deprecatedSpotTariff = null;
		}
		if (this.deprecatedProvider != null) {
			this.deprecatedProvider.deactivate();
			this.deprecatedProvider = null;
		}
		if (this.spotTariffTracker != null) {
			this.spotTariffTracker.getTariffData().unsubscribe(this.onNewSpotTariffDataFunc);
			this.tariffManager.ungetSpotTariff(this.spotTariffTracker);
			this.spotTariffTracker = null;
		}
	}

	private void enableDeprecatedDirectEntsoeProvider() {
		this.deprecatedProvider = new EntsoeTariffProvider(
				new EntsoeTariffConfig(this.getSecurityToken(this.config), this.config.service_ranking()),
				this.bridgeHttpFactory, this.getClock(), this.id());
		this.deprecatedProvider.activate();

		this.deprecatedSpotTariff = this.deprecatedProvider.getSpotTariff(BiddingZone.GERMANY);
		this.deprecatedSpotTariff.getTariffData().subscribe(this.onNewSpotTariffDataFunc);
	}

	private String getSecurityToken(Config config) {
		if (config.securityToken() != null && !config.securityToken().isEmpty()) {
			return config.securityToken();
		}

		if (this.oem.getEntsoeToken() != null && !this.oem.getEntsoeToken().isEmpty()) {
			return this.oem.getEntsoeToken();
		}

		this.logError(this.log, "No entso-e security token available");
		return null;
	}

	protected void onNewSpotTariffData(TariffData spotTariffData) {
		if (spotTariffData == null) {
			this.tariffData.setValue(null);
			this.updateStatus.setValue(new TariffUpdateStatus.NotFetchedYet(this.getClock().instant()));
		} else {
			final var processedPrices = this.processPrices(spotTariffData.prices());
			this.tariffData
					.setValue(new TariffData(new TariffType.SellGross(), processedPrices, spotTariffData.currency(),
							spotTariffData.fetchTime(), spotTariffData.lastUpdate(), spotTariffData.copyright()));
			this.updateStatus.setValue(new TariffUpdateStatus.Successful(this.getClock().instant()));
		}
		this.updateInfosAboutLoadedTariffs();
	}

	private TimeRangeValues<Double> processPrices(TimeRangeValues<Double> spotPrices) {
		var timeSpan = spotPrices.getTimeSpan();
		if (timeSpan == null) {
			return null;
		}

		var pricesBuilder = TimeRangeValues.builder(timeSpan, DurationUnit.ofMinutes(15), Double.class);
		final var fixedPrice = this.getFixedGridSellPriceInEuroPerMWh();

		Instant time = timeSpan.getStartInclusive();
		while (time.isBefore(timeSpan.getEndExclusive())) {
			var spotPrice = spotPrices.getAtOrElse(time, 0.0);
			if (spotPrice <= 0) {
				pricesBuilder.setByTime(time, 0.0);
			} else {
				pricesBuilder.setByTime(time, fixedPrice);
			}

			time = time.plus(DurationUnit.ofMinutes(15).getDuration());
		}

		return pricesBuilder.build();
	}

	public double getFixedGridSellPriceInEuroPerMWh() {
		// Config is in Cent/kWh, channel unit is in Euro/MWh
		return this.config.fixedGridSellPrice() * 10;
	}

	@Override
	public TimeOfUsePrices getGridSellPrices() {
		final var data = this.tariffData.getValue();
		if (data == null) {
			return TimeOfUsePrices.EMPTY_PRICES;
		}

		return TimeOfUsePrices.from(Instant.now(this.getClock()), data.prices());
	}

	@Override
	protected void setChannelsForUpdateFailures(List<TariffUpdateStatus> updateFailures) {
		// No error channels
	}

	@Override
	protected String getChannelPrefixForTariff(Tariff tariff) {
		return "";
	}

	@Override
	public BehaviorSubject<List<TariffProvider.LoadedTariffInfo>> getInfosAboutLoadedTariffs() {
		return this.infosAboutLoadedTariffs;
	}

	protected void updateInfosAboutLoadedTariffs() {
		this.infosAboutLoadedTariffs.setValue(List.of(new LoadedTariffInfo(this, this.updateStatus.getValue())));
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
	public String getIdentifier() {
		return this.id();
	}

	@Override
	public BehaviorSubject<TariffData> getTariffData() {
		return this.tariffData;
	}

	@Override
	public BehaviorSubject<TariffUpdateStatus> getLastUpdateStatus() {
		return this.updateStatus;
	}
}
