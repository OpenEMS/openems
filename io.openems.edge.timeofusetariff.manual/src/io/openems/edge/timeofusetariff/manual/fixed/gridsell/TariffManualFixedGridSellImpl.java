package io.openems.edge.timeofusetariff.manual.fixed.gridsell;

import static io.openems.common.utils.DateUtils.QUARTERS_PER_DAY;
import static io.openems.edge.timeofusetariff.api.utils.TariffGridSellUtils.generateDebugLog;

import java.time.Instant;
import java.util.Arrays;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.timeofusetariff.api.TariffGridSell;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Tariff.Manual.Fixed.GridSell", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class TariffManualFixedGridSellImpl extends AbstractOpenemsComponent
		implements OpenemsComponent, TariffGridSell, TariffManualFixedGridSell {

	@Reference
	private ComponentManager componentManager;

	@Reference
	private Meta meta;

	private double fixedGridSellPrice;

	public TariffManualFixedGridSellImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				TariffManualFixedGridSell.ChannelId.values()//
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());

		if (!config.enabled()) {
			return;
		}

		this.fixedGridSellPrice = config.fixedGridSellPrice();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public TimeOfUsePrices getGridSellPrices() {
		final var now = Instant.now(this.componentManager.getClock());
		final var prices = new Double[QUARTERS_PER_DAY * 2];
		Arrays.fill(prices, this.fixedGridSellPrice);

		return TimeOfUsePrices.from(now, prices);
	}

	@Override
	public String debugLog() {
		return generateDebugLog(this, this.meta.getCurrency());
	}
}
