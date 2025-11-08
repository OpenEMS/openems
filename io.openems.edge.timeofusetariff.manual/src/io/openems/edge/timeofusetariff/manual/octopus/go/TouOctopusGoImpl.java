package io.openems.edge.timeofusetariff.manual.octopus.go;

import static io.openems.common.jscalendar.JSCalendar.RecurrenceFrequency.DAILY;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.timeofusetariff.api.TouManualHelper.EMPTY_TOU_MANUAL_HELPER;
import static io.openems.edge.timeofusetariff.api.utils.TimeOfUseTariffUtils.generateDebugLog;
import static io.openems.edge.timeofusetariff.manual.octopus.Utils.CENT_PER_KWH_TO_CURRENCY_PER_MWH;

import java.time.Duration;
import java.time.LocalTime;

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
import io.openems.common.jscalendar.JSCalendar;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;
import io.openems.edge.timeofusetariff.api.TouManualHelper;
import io.openems.edge.timeofusetariff.manual.octopus.Utils;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "TimeOfUseTariff.OctopusGo", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class TouOctopusGoImpl extends AbstractOpenemsComponent
		implements TouOctopusGo, TimeOfUseTariff, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(TouOctopusGoImpl.class);

	@Reference
	private Meta meta;

	private Config config = null;

	private TouManualHelper octopusHelper = EMPTY_TOU_MANUAL_HELPER;
	private TouManualHelper ancillaryCostsHelper = EMPTY_TOU_MANUAL_HELPER;

	@Reference
	private ComponentManager componentManager;

	public TouOctopusGoImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				TouOctopusGo.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());

		this.config = config;

		// Cent/kWh -> Currency/MWh
		// Example: 12 Cent/kWh => 0.12 EUR/kWh * 1000 kWh/MWh = 120 EUR/MWh.
		final var standardPrice = config.standardPrice() * CENT_PER_KWH_TO_CURRENCY_PER_MWH;
		final var lowPrice = config.lowPrice() * CENT_PER_KWH_TO_CURRENCY_PER_MWH;

		if (Double.isNaN(standardPrice) || Double.isNaN(lowPrice) || standardPrice < lowPrice) {
			setValue(this, TouOctopusGo.ChannelId.INVALID_PRICE, true);
			return;
		}

		final var clock = this.componentManager.getClock();
		final var goSchedule = JSCalendar.Tasks.<Double>create() //
				.setClock(clock) //
				.add(t -> t //
						.setStart(LocalTime.of(0, 0)) //
						.setDuration(Duration.ofHours(5)) //
						.addRecurrenceRule(b -> b //
								.setFrequency(DAILY)) //
						.setPayload(lowPrice) //
						.build()) //
				.build();

		this.octopusHelper = new TouManualHelper(clock, goSchedule, standardPrice);

		try {
			final var ancillarySchedule = Utils.parseScheduleFromConfig(clock, this.config.ancillaryCosts());
			this.ancillaryCostsHelper = new TouManualHelper(clock, ancillarySchedule, 0.0);
		} catch (OpenemsNamedException e) {
			this.logWarn(this.log, "Unable to parse Schedule: " + e.getMessage());
			this.ancillaryCostsHelper = EMPTY_TOU_MANUAL_HELPER;
		}

		setValue(this, TouOctopusGo.ChannelId.INVALID_PRICE, false);
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
		return Utils.getPrices(//
				this.octopusHelper, //
				this.ancillaryCostsHelper, //
				this.config.ancillaryCosts(), //
				msg -> this.logWarn(this.log, msg));
	}
}
