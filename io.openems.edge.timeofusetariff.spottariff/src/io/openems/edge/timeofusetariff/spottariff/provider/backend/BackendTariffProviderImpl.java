package io.openems.edge.timeofusetariff.spottariff.provider.backend;

import java.time.Clock;
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
import io.openems.common.tariff.api.spottariff.SpotTariff;
import io.openems.common.tariff.api.spottariff.SpotTariffProvider;
import io.openems.common.types.BiddingZone;
import io.openems.common.utils.BehaviorSubject;
import io.openems.common.utils.SingletonPool;
import io.openems.edge.common.channel.ChannelUtils;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.services.syncable.SyncableRepository;
import io.openems.edge.controller.api.backend.services.BackendConnectionState;
import io.openems.edge.controller.api.backend.services.BackendConnectionStateListener;
import io.openems.edge.timeofusetariff.api.AbstractTariffComponent;
import io.openems.edge.timeofusetariff.api.TariffComponent;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Tariff.SpotTariff.FromBackend", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class BackendTariffProviderImpl extends AbstractTariffComponent
		implements BackendTariffProvider, SpotTariffProvider, TariffProvider, OpenemsComponent, EventHandler {

	@Reference
	private ComponentManager componentManager;

	@Reference
	private SyncableRepository syncableRepository;

	@Reference
	private BackendConnectionStateListener backendConnectionStateListener;

	private final Consumer<BackendConnectionState> updateBackendConnectionStateFunc = this::updateBackendConnectionState;

	private final BackendSpotPool spotPool = new BackendSpotPool();
	private final BehaviorSubject<List<LoadedTariffInfo>> infosAboutLoadedTariffs = new BehaviorSubject<>(List.of());

	private Config config;

	public BackendTariffProviderImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				TariffComponent.ChannelId.values(), //
				BackendTariffProvider.ChannelId.values() //
		);
	}

	@Activate
	protected synchronized void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;

		if (this.isEnabled()) {
			this.backendConnectionStateListener.getConnectionState().subscribe(this.updateBackendConnectionStateFunc);
		}
	}

	@Deactivate
	@Override
	protected synchronized void deactivate() {
		super.deactivate();

		this.backendConnectionStateListener.getConnectionState().unsubscribe(this.updateBackendConnectionStateFunc);
	}

	@Override
	protected void setChannelsForUpdateFailures(List<TariffUpdateStatus> updateFailures) {
		// Not required here because there are no error channels
	}

	@Override
	protected String getChannelPrefixForTariff(Tariff tariff) {
		return ((SpotTariff) tariff).getBiddingZone().name();
	}

	@Override
	public BehaviorSubject<List<LoadedTariffInfo>> getInfosAboutLoadedTariffs() {
		return this.infosAboutLoadedTariffs;
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
	public boolean isSpotTariffSupported(BiddingZone biddingZone) {
		return true;
	}

	@Override
	public SpotTariff getSpotTariff(BiddingZone biddingZone) {
		return this.spotPool.get(biddingZone);
	}

	@Override
	public void ungetSpotTariff(SpotTariff tariffObj) {
		this.spotPool.unget((BackendSpotTariff) tariffObj);
	}

	private void updateBackendConnectionState(BackendConnectionState state) {
		switch (state) {
		case BackendConnectionState.Connected() ->
			ChannelUtils.setValue(this, BackendTariffProvider.ChannelId.NO_BACKEND_CONNECTION, false);
		default -> //
			ChannelUtils.setValue(this, BackendTariffProvider.ChannelId.NO_BACKEND_CONNECTION, true);
		}
	}

	protected synchronized void updateLoadedTariffInfos() {
		var infos = this.spotPool.getAllInstances().stream() //
				.map(BackendSpotTariff::getInfo) //
				.toList();
		this.infosAboutLoadedTariffs.setValue(infos);
	}

	private class BackendSpotPool extends SingletonPool<BiddingZone, BackendSpotTariff> {

		@Override
		protected BackendSpotTariff createNewInstance(BiddingZone biddingZone) {
			return new BackendSpotTariff(biddingZone, BackendTariffProviderImpl.this.syncableRepository,
					BackendTariffProviderImpl.this.componentManager.getClock(),
					BackendTariffProviderImpl.this.config.id());
		}

		@Override
		protected void onPostLoadInstance(BackendSpotTariff tariff) {
			tariff.getLastUpdateStatus().subscribe(s -> BackendTariffProviderImpl.this.updateLoadedTariffInfos());
		}

		@Override
		protected void onPostUnloadInstance(BackendSpotTariff instance) {
			BackendTariffProviderImpl.this.updateLoadedTariffInfos();
		}
	}
}
