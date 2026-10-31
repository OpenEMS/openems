package io.openems.edge.timeofusetariff.spottariff.gridsell;

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
import io.openems.edge.timeofusetariff.api.SpotTariffTracker;
import io.openems.edge.timeofusetariff.api.TariffComponent;
import io.openems.edge.timeofusetariff.api.TariffGridSell;
import io.openems.edge.timeofusetariff.api.TariffManager;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.api.utils.calculator.PriceCalculatorX;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Tariff.BySpotTariff.GridSell", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class TariffGridSellSpotTariffImpl extends AbstractTariffComponent implements TariffGridSellSpotTariff,
		OpenemsComponent, TariffGridSell, TariffComponent, TariffProvider, Tariff, EventHandler {

	@Reference
	private TariffManager tariffManager;

	@Reference
	private ComponentManager componentManager;

	private final Consumer<TariffData> onNewSpotTariffDataFunc = this::onNewSpotTariffData;

	private final BehaviorSubject<TariffData> tariffData = new BehaviorSubject<>(null);
	private final BehaviorSubject<TariffUpdateStatus> updateStatus = new BehaviorSubject<>(null);
	private final BehaviorSubject<List<LoadedTariffInfo>> infosAboutLoadedTariffs = new BehaviorSubject<>(List.of());

	private Config config;
	private PriceCalculatorX priceCalculator;
	private SpotTariffTracker spotTariffTracker;

	public TariffGridSellSpotTariffImpl() {
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
		this.priceCalculator = PriceCalculatorX.fromExpression(config.calculateExpression());

		this.spotTariffTracker = this.tariffManager.getSpotTariff(config.biddingZone());
		this.spotTariffTracker.getTariffData().subscribe(this.onNewSpotTariffDataFunc);

		this.updateInfosAboutLoadedTariffs();
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

		var resultBuilder = TimeRangeValues.builder(timeSpan, DurationUnit.ofMinutes(15), Double.class);

		Instant time = timeSpan.getStartInclusive();
		while (time.isBefore(timeSpan.getEndExclusive())) {
			var spotPrice = spotPrices.getAtOrElse(time, 0.0);

			var priceWithFee = this.priceCalculator.calculate(spotPrice); // y = Ancillary costs

			resultBuilder.setByTime(time, priceWithFee);
			time = time.plus(DurationUnit.ofMinutes(15).getDuration());
		}

		return resultBuilder.build();
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
