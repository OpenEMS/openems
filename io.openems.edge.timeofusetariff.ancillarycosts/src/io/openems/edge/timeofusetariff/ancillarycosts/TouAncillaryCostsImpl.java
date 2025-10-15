package io.openems.edge.timeofusetariff.ancillarycosts;

import static com.google.common.collect.ImmutableSortedMap.toImmutableSortedMap;
import static io.openems.edge.timeofusetariff.api.AncillaryCosts.parseForGermany;
import static io.openems.edge.timeofusetariff.api.TouManualHelper.EMPTY_TOU_MANUAL_HELPER;
import static io.openems.edge.timeofusetariff.api.utils.TimeOfUseTariffUtils.generateDebugLog;

import java.util.Comparator;
import java.util.Map.Entry;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;
import io.openems.edge.timeofusetariff.api.TouManualHelper;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "TimeOfUseTariff.AncillaryCosts", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class TouAncillaryCostsImpl extends AbstractOpenemsComponent implements TimeOfUseTariff, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(TouAncillaryCostsImpl.class);

	@Reference
	private Meta meta;

	@Reference
	private ComponentManager componentManager;

	private TouManualHelper helper = null;
	private double fixedTariff;

	public TouAncillaryCostsImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				TouAncillaryCosts.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());

		this.applyConfig(config);
	}

	private void applyConfig(Config config) {
		this.fixedTariff = config.fixedTariff();
		try {
			var schedule = parseForGermany(config.ancillaryCosts());
			this.helper = new TouManualHelper(this.componentManager.getClock(), schedule, 0.0);
		} catch (OpenemsNamedException e) {
			this.logWarn(this.log, "Unable to parse Schedule:" + e.getMessage());
			this.helper = EMPTY_TOU_MANUAL_HELPER;
			this.channel(TouAncillaryCosts.ChannelId.INVALID_PRICE).setNextValue(true);
		}

	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public String debugLog() {
		return generateDebugLog(this, this.meta.getCurrency());
	}

	@Override
	public TimeOfUsePrices getPrices() {
		var helper = this.helper;
		if (helper == null) {
			return TimeOfUsePrices.EMPTY_PRICES;
		}

		final var ancillaryPrices = this.helper.getPrices();

		if (ancillaryPrices.isEmpty()) {
			return ancillaryPrices;
		}

		var finalPriceMap = ancillaryPrices.toMap() //
				.entrySet() //
				.stream() //
				.collect(toImmutableSortedMap(//
						Comparator.naturalOrder(), //
						// ct/KWh -> EUR/MWh
						Entry::getKey, e -> (e.getValue() + this.fixedTariff) * 10 //
				));

		return TimeOfUsePrices.from(finalPriceMap);
	}
}
