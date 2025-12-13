package io.openems.edge.evcc.gridtariff;

import static io.openems.edge.timeofusetariff.api.utils.TimeOfUseTariffUtils.generateDebugLog;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.time.HttpBridgeTimeServiceDefinition;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "TimeOfUseTariff.Grid.Evcc", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)

public class TimeOfUseGridTariffEvccImpl extends AbstractOpenemsComponent
		implements TimeOfUseTariff, OpenemsComponent, TimeOfUseGridTariffEvcc {

	@Reference
	private ComponentManager componentManager;

	@Reference
	private BridgeHttpFactory httpBridgeFactory;

	@Reference
	private Meta meta;

	private TimeOfUseGridTariffEvccApi apiClient;

	private BridgeHttp httpBridge;

	public TimeOfUseGridTariffEvccImpl() {
		super(OpenemsComponent.ChannelId.values(), TimeOfUseGridTariffEvcc.ChannelId.values());
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());

		if (!config.enabled()) {
			return;
		}

		this.httpBridge = this.httpBridgeFactory.get();
		final var timeService = this.httpBridge.createService(HttpBridgeTimeServiceDefinition.INSTANCE);

		this.apiClient = new TimeOfUseGridTariffEvccApi(config.apiUrl(), timeService,
				this.componentManager.getClock());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.httpBridgeFactory.unget(this.httpBridge);
	}

	/**
	 * Retrieves the current time-of-use prices.
	 *
	 * <p>
	 * This method checks if the API client is available. If so, it fetches the
	 * prices from the API client and returns a TimeOfUsePrices instance with the
	 * current timestamp. If the API client is not available, it returns an empty
	 * TimeOfUsePrices object.
	 *
	 * @return the current time-of-use prices or an empty instance if the API client
	 *         is unavailable.
	 */
	public TimeOfUsePrices getPrices() {
		if (this.apiClient != null) {
			return this.apiClient.getPrices();
		}
		return TimeOfUsePrices.EMPTY_PRICES;
	}

	@Override
	public String debugLog() {
		return generateDebugLog(this, this.meta.getCurrency());
	}
}
