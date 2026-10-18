package io.openems.edge.timeofusetariff.spottariff.provider.smard;

import java.time.Clock;
import java.util.List;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.tariff.api.Tariff;
import io.openems.common.tariff.api.TariffProvider;
import io.openems.common.tariff.api.TariffUpdateStatus;
import io.openems.common.tariff.api.spottariff.SpotTariff;
import io.openems.common.tariff.api.spottariff.SpotTariffProvider;
import io.openems.common.tariff.smard.SmardTariffConfig;
import io.openems.common.tariff.smard.SmardTariffProvider;
import io.openems.edge.common.channel.ChannelUtils;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.timeofusetariff.api.TariffComponent;
import io.openems.edge.timeofusetariff.spottariff.provider.AbstractMirroredSpotTariffProviderComponent;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Tariff.SpotTariff.SMARD", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class EdgeSmardTariffProviderImpl extends AbstractMirroredSpotTariffProviderComponent
		implements EdgeSmardTariffProvider, SpotTariffProvider, TariffProvider, OpenemsComponent, EventHandler {

	@Reference
	private BridgeHttpFactory bridgeHttpFactory;

	@Reference
	private ComponentManager componentManager;

	private SmardTariffProvider provider;

	public EdgeSmardTariffProviderImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				TariffComponent.ChannelId.values(), //
				EdgeSmardTariffProvider.ChannelId.values() //
		);
	}

	@Activate
	protected synchronized void activate(ComponentContext context, Config config) {
		if (!config.enabled()) {
			super.activate(context, config.id(), config.alias(), false);
			return;
		}

		final var providerConfig = new SmardTariffConfig(config.service_ranking());
		this.provider = new SmardTariffProvider(providerConfig, this.bridgeHttpFactory,
				this.componentManager.getClock(), config.id());

		super.activate(context, config.id(), config.alias(), true);

		this.provider.activate();
	}

	@Deactivate
	@Override
	protected synchronized void deactivate() {
		if (this.provider != null) {
			this.provider.deactivate();
			this.provider = null;
		}
		super.deactivate();
	}

	@Override
	public SpotTariffProvider getUnderlyingProvider() {
		return this.provider;
	}

	@Override
	protected void setChannelsForUpdateFailures(List<TariffUpdateStatus> updateFailures) {
		ChannelUtils.setValue(this, EdgeSmardTariffProvider.ChannelId.UPDATE_FAILURE, !updateFailures.isEmpty());
	}

	@Override
	protected String getChannelPrefixForTariff(Tariff tariff) {
		return ((SpotTariff) tariff).getBiddingZone().name();
	}

	@Override
	public Clock getClock() {
		return this.componentManager.getClock();
	}
}
