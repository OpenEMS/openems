package io.openems.edge.timeofusetariff.manual.octopus.heat;

import static io.openems.common.jscalendar.JSCalendar.RecurrenceFrequency.DAILY;
import static io.openems.edge.timeofusetariff.api.utils.TimeOfUseTariffUtils.generateDebugLog;

import java.time.Duration;
import java.time.LocalTime;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import com.google.common.collect.ImmutableList;

import io.openems.common.jscalendar.JSCalendar;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;
import io.openems.edge.timeofusetariff.manual.TouManualHelper;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "TimeOfUseTariff.OctopusHeat", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class TouOctopusHeatImpl extends AbstractOpenemsComponent
		implements TouOctopusHeat, TimeOfUseTariff, OpenemsComponent {

	@Reference
	private Meta meta;

	private TouManualHelper helper = null;

	public TouOctopusHeatImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				TouOctopusHeat.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());

		// Cent/kWh -> Currency/MWh
		// Example: 12 Cent/kWh => 0.12 EUR/kWh * 1000 kWh/MWh = 120 EUR/MWh.
		final var standardPrice = config.standardPrice() * 10;
		final var lowPrice = config.lowPrice() * 10;
		final var highPrice = config.highPrice() * 10;

		if (Double.isNaN(standardPrice) || Double.isNaN(lowPrice) || Double.isNaN(highPrice)) {
			this.channel(TouOctopusHeat.ChannelId.INVALID_PRICE).setNextValue(true);
			return;
		}

		final var schedule = ImmutableList.of(
				// Lower price from 02:00 to 06:00
				JSCalendar.Task.<Double>create() //
						.setStart(LocalTime.of(2, 0)) //
						.setDuration(Duration.ofHours(4))//
						.addRecurrenceRule(b -> b.setFrequency(DAILY))//
						.setPayload(lowPrice) //
						.build(),

				// Lower price from 12:00 to 16:00
				JSCalendar.Task.<Double>create() //
						.setStart(LocalTime.of(12, 0)) //
						.setDuration(Duration.ofHours(4)) //
						.addRecurrenceRule(b -> b.setFrequency(DAILY)) //
						.setPayload(lowPrice) //
						.build(),
				// Higher price from 18:00 to 21:00
				JSCalendar.Task.<Double>create() //
						.setStart(LocalTime.of(18, 0)) //
						.setDuration(Duration.ofHours(3)) //
						.addRecurrenceRule(b -> b.setFrequency(DAILY)) //
						.setPayload(highPrice).build());

		this.helper = new TouManualHelper(schedule, standardPrice);
		this.channel(TouOctopusHeat.ChannelId.INVALID_PRICE).setNextValue(false);
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
		return this.helper.getPrices();
	}
}
