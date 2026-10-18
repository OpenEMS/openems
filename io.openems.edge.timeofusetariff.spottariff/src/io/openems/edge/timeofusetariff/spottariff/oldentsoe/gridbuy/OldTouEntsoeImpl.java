package io.openems.edge.timeofusetariff.spottariff.oldentsoe.gridbuy;

import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.timeofusetariff.api.TouManualHelper.EMPTY_TOU_MANUAL_HELPER;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.tariff.api.Tariff;
import io.openems.common.tariff.api.TariffProvider;
import io.openems.common.tariff.api.TariffUpdateStatus;
import io.openems.common.tariff.api.spottariff.SpotTariff;
import io.openems.common.tariff.api.spottariff.SpotTariffProvider;
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
import io.openems.edge.common.meta.Meta;
import io.openems.edge.timeofusetariff.api.AbstractTariffComponent;
import io.openems.edge.timeofusetariff.api.AncillaryCosts;
import io.openems.edge.timeofusetariff.api.TariffComponent;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;
import io.openems.edge.timeofusetariff.api.TouManualHelper;
import io.openems.edge.timeofusetariff.api.utils.calculator.PriceCalculatorXY;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "TimeOfUseTariff.ENTSO-E", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class OldTouEntsoeImpl extends AbstractTariffComponent implements OldTouEntsoe, OpenemsComponent,
		TimeOfUseTariff, TariffComponent, TariffProvider, Tariff, SpotTariffProvider {

	private final Logger log = LoggerFactory.getLogger(OldTouEntsoeImpl.class);

	@Reference
	private Meta meta;

	@Reference
	private ComponentManager componentManager;

	@Reference
	private BridgeHttpFactory bridgeHttpFactory;

	@Reference
	private OpenemsEdgeOem oem;

	private final Consumer<TariffData> onNewSpotTariffDataFunc = this::onNewSpotTariffData;
	private final Consumer<TariffUpdateStatus> onNewTariffUpdateStatusFunc = status -> this
			.updateInfosAboutLoadedTariffs();

	private final BehaviorSubject<TariffData> tariffData = new BehaviorSubject<>(null);
	private final BehaviorSubject<TariffUpdateStatus> updateStatus = new BehaviorSubject<>(null);
	private final BehaviorSubject<List<TariffProvider.LoadedTariffInfo>> infosAboutLoadedTariffs = new BehaviorSubject<>(
			List.of());

	private PriceCalculatorXY priceCalculator;
	private EntsoeTariffProvider provider;
	private SpotTariff spotTariff;
	private TouManualHelper helper = TouManualHelper.EMPTY_TOU_MANUAL_HELPER;

	public OldTouEntsoeImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				TariffComponent.ChannelId.values(), //
				OldTouEntsoe.ChannelId.values() //
		);
	}

	@Activate
	protected void activate(ComponentContext context, Config config) {
		final var providerConfig = new EntsoeTariffConfig(this.getSecurityToken(config), 20);
		this.provider = new EntsoeTariffProvider(providerConfig, this.bridgeHttpFactory,
				this.componentManager.getClock(), config.id());

		super.activate(context, config.id(), config.alias(), config.enabled());

		if (!config.enabled()) {
			return;
		}

		this.applySchedule(config);
		this.priceCalculator = PriceCalculatorXY.fromExpression(config.calculateExpression());
		this.provider.activate();

		this.spotTariff = this.provider.getSpotTariff(config.biddingZone());
		this.spotTariff.getTariffData().subscribe(this.onNewSpotTariffDataFunc);
		this.spotTariff.getLastUpdateStatus().subscribe(this.onNewTariffUpdateStatusFunc);
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

	private void applySchedule(Config config) {
		final var clock = this.componentManager.getClock();

		try {
			if (config.ancillaryCosts() == null || config.ancillaryCosts().isBlank()) {
				this.helper = new TouManualHelper(clock, JSCalendar.Tasks.empty(), 0.0);
			} else {
				final var schedule = switch (config.biddingZone()) {
				case GERMANY //
					-> AncillaryCosts.parseForGermany(clock, config.ancillaryCosts());

				case AUSTRIA, SWEDEN_SE1, SWEDEN_SE2, SWEDEN_SE3, SWEDEN_SE4, BELGIUM, NETHERLANDS, CZECHIA, LITHUANIA,
						GREECE -> {
					throw new OpenemsException(
							"Parser for bidding zone " + config.biddingZone().name() + " is not implemented");
				}
				};
				this.helper = new TouManualHelper(clock, schedule, 0.0);
			}
		} catch (OpenemsError.OpenemsNamedException e) {
			this.logWarn(this.log, "Unable to parse Schedule: " + e.getMessage());
			this.helper = EMPTY_TOU_MANUAL_HELPER;
		}
	}

	@Deactivate
	@Override
	protected synchronized void deactivate() {
		if (this.spotTariff != null) {
			this.spotTariff.getLastUpdateStatus().unsubscribe(this.onNewTariffUpdateStatusFunc);
			this.spotTariff.getTariffData().unsubscribe(this.onNewSpotTariffDataFunc);
			if (this.provider != null) {
				this.provider.ungetSpotTariff(this.spotTariff);
			}
			this.spotTariff = null;
		}

		if (this.provider != null) {
			this.provider.deactivate();
			this.provider = null;
		}
		super.deactivate();
	}

	protected void onNewSpotTariffData(TariffData spotTariffData) {
		if (spotTariffData == null) {
			this.tariffData.setValue(null);
			this.updateStatus
					.setValue(new TariffUpdateStatus.NotFetchedYet(this.componentManager.getClock().instant()));
		} else {
			final var gridFees = this.helper.getPrices();
			final var processedPrices = this.processPrices(spotTariffData.prices(), gridFees);
			this.tariffData
					.setValue(new TariffData(new TariffType.BuyGross(), processedPrices, spotTariffData.currency(),
							spotTariffData.fetchTime(), spotTariffData.lastUpdate(), spotTariffData.copyright()));
			this.updateStatus.setValue(new TariffUpdateStatus.Successful(this.componentManager.getClock().instant()));
		}
		this.updateInfosAboutLoadedTariffs();
	}

	private TimeRangeValues<Double> processPrices(TimeRangeValues<Double> spotPrices, TimeOfUsePrices gridFees) {
		var timeSpan = spotPrices.getTimeSpan() //
				.getOverlappingTime(gridFees.getTimeSpan()) //
				.flatMap(x -> x.narrowDownToStartDate(Instant.now(this.componentManager.getClock()))) //
				.orElse(null);
		if (timeSpan == null) {
			return null;
		}

		var resultBuilder = TimeRangeValues.builder(timeSpan, DurationUnit.ofMinutes(15), Double.class);

		Instant time = timeSpan.getStartInclusive();
		while (time.isBefore(timeSpan.getEndExclusive())) {
			var spotPrice = spotPrices.getAt(time);
			var gridFee = gridFees.getAtOrElse(time, 0.0);

			if (spotPrice == null) {
				// Missing spot prices are filled by the fillMissingDataWithPreviousData() call
				// later.
				time = time.plus(DurationUnit.ofMinutes(15).getDuration());
				continue;
			}

			// converting grid fees from ct/KWh -> EUR/MWh
			var gridFeesPerMwh = gridFee * 10;

			var priceWithFee = this.priceCalculator.calculate(//
					spotPrice, // x = EPEX Spot price
					gridFeesPerMwh); // y = Ancillary costs

			resultBuilder.setByTime(time, priceWithFee);
			time = time.plus(DurationUnit.ofMinutes(15).getDuration());
		}

		return resultBuilder //
				.fillMissingDataWithPreviousData() //
				.build();
	}

	@Override
	public TimeOfUsePrices getPrices() {
		final var data = this.tariffData.getValue();
		if (data == null) {
			return TimeOfUsePrices.EMPTY_PRICES;
		}

		return TimeOfUsePrices.from(Instant.now(this.componentManager.getClock()), data.prices());
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
		if (this.spotTariff == null) {
			return new BehaviorSubject<>(
					new TariffUpdateStatus.NotFetchedYet(this.componentManager.getClock().instant()));
		}
		return this.spotTariff.getLastUpdateStatus();
	}

	@Override
	protected void setChannelsForUpdateFailures(List<TariffUpdateStatus> updateFailures) {
		setValue(this, OldTouEntsoe.ChannelId.UNABLE_TO_UPDATE_PRICES, !updateFailures.isEmpty());
	}

	@Override
	protected String getChannelPrefixForTariff(Tariff tariff) {
		if (tariff == this) {
			return "";
		}
		return "SPOT_";
	}

	@Override
	public BehaviorSubject<List<TariffProvider.LoadedTariffInfo>> getInfosAboutLoadedTariffs() {
		return this.infosAboutLoadedTariffs;
	}

	@Override
	public Clock getClock() {
		return this.componentManager.getClock();
	}

	protected void updateInfosAboutLoadedTariffs() {
		this.infosAboutLoadedTariffs.setValue(List.of(//
				new LoadedTariffInfo(this, this.spotTariff.getLastUpdateStatus().getValue()), //
				new LoadedTariffInfo(this.spotTariff, this.spotTariff.getLastUpdateStatus().getValue()) //
		));
	}

	@Override
	public int getRanking() {
		return 5;
	}

	@Override
	public SpotTariff getSpotTariff(BiddingZone biddingZone) {
		return this.provider.getSpotTariff(biddingZone);
	}

	@Override
	public void ungetSpotTariff(SpotTariff tariffObj) {
		this.provider.ungetSpotTariff(tariffObj);
	}

	@Override
	public boolean isSpotTariffSupported(BiddingZone biddingZone) {
		return this.provider.isSpotTariffSupported(biddingZone);
	}
}
