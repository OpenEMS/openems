package io.openems.edge.timeofusetariff.spottariff.gridbuy;

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
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.tariff.api.Tariff;
import io.openems.common.tariff.api.TariffProvider;
import io.openems.common.tariff.api.TariffUpdateStatus;
import io.openems.common.timedata.DurationUnit;
import io.openems.common.types.TariffData;
import io.openems.common.types.TariffType;
import io.openems.common.utils.BehaviorSubject;
import io.openems.common.utils.TimeRangeValues;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.timeofusetariff.api.AbstractTariffComponent;
import io.openems.edge.timeofusetariff.api.AncillaryCosts;
import io.openems.edge.timeofusetariff.api.SpotTariffTracker;
import io.openems.edge.timeofusetariff.api.TariffComponent;
import io.openems.edge.timeofusetariff.api.TariffManager;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;
import io.openems.edge.timeofusetariff.api.TouManualHelper;
import io.openems.edge.timeofusetariff.api.utils.calculator.PriceCalculatorXY;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Tariff.BySpotTariff.GridBuy", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class TouSpotTariffImpl extends AbstractTariffComponent implements TouSpotTariff, OpenemsComponent,
		TimeOfUseTariff, TariffComponent, TariffProvider, Tariff, EventHandler {

	@Reference
	private TariffManager tariffManager;

	@Reference
	private ComponentManager componentManager;

	private final Logger log = LoggerFactory.getLogger(TouSpotTariffImpl.class);
	private final Consumer<TariffData> onNewSpotTariffDataFunc = this::onNewSpotTariffData;

	private final BehaviorSubject<TariffData> tariffData = new BehaviorSubject<>(null);
	private final BehaviorSubject<TariffUpdateStatus> updateStatus = new BehaviorSubject<>(null);
	private final BehaviorSubject<List<TariffProvider.LoadedTariffInfo>> infosAboutLoadedTariffs = new BehaviorSubject<>(
			List.of());

	private Config config;
	private TouManualHelper helper = TouManualHelper.EMPTY_TOU_MANUAL_HELPER;
	private PriceCalculatorXY priceCalculator;
	private SpotTariffTracker spotTariffTracker;

	public TouSpotTariffImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				TariffComponent.ChannelId.values() //
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
		this.applySchedule(config);
		this.priceCalculator = PriceCalculatorXY.fromExpression(config.calculateExpression());

		this.spotTariffTracker = this.tariffManager.getSpotTariff(config.biddingZone());
		this.spotTariffTracker.getTariffData().subscribe(this.onNewSpotTariffDataFunc);

		this.updateInfosAboutLoadedTariffs();
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
		super.deactivate();

		if (this.spotTariffTracker != null) {
			this.spotTariffTracker.getTariffData().unsubscribe(this.onNewSpotTariffDataFunc);
			this.tariffManager.ungetSpotTariff(this.spotTariffTracker);
			this.spotTariffTracker = null;
		}
	}

	protected void onNewSpotTariffData(TariffData spotTariffData) {
		if (spotTariffData == null) {
			this.tariffData.setValue(null);
			this.updateStatus.setValue(new TariffUpdateStatus.NotFetchedYet(this.getClock().instant()));
		} else {
			final var gridFees = this.helper.getPrices();
			final var processedPrices = this.processPrices(spotTariffData.prices(), gridFees);
			this.tariffData
					.setValue(new TariffData(new TariffType.BuyGross(), processedPrices, spotTariffData.currency(),
							spotTariffData.fetchTime(), spotTariffData.lastUpdate(), spotTariffData.copyright()));
			this.updateStatus.setValue(new TariffUpdateStatus.Successful(this.getClock().instant()));
		}
		this.updateInfosAboutLoadedTariffs();
	}

	private TimeRangeValues<Double> processPrices(TimeRangeValues<Double> spotPrices, TimeOfUsePrices gridFees) {
		var timeSpan = spotPrices.getTimeSpan() //
				.getOverlappingTime(gridFees.getTimeSpan()) //
				.flatMap(x -> x.narrowDownToStartDate(Instant.now(this.getClock()))) //
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
