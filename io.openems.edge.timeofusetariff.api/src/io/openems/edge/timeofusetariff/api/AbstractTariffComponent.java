package io.openems.edge.timeofusetariff.api;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.tariff.api.Tariff;
import io.openems.common.tariff.api.TariffProvider;
import io.openems.common.tariff.api.TariffUpdateStatus;
import io.openems.common.types.OpenemsType;
import io.openems.common.types.TariffData;
import io.openems.common.utils.BehaviorSubject;
import io.openems.common.utils.DateUtils;
import io.openems.edge.common.channel.ChannelId.ChannelIdImpl;
import io.openems.edge.common.channel.ChannelUtils;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;

public abstract class AbstractTariffComponent extends AbstractOpenemsComponent
		implements TariffComponent, EventHandler {

	protected final Map<Tariff, TariffChannelTracker> channelTrackers = new HashMap<>();

	protected AbstractTariffComponent(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	@Override
	protected void activate(ComponentContext context, String id, String alias, boolean enabled)
			throws IllegalArgumentException {
		super.activate(context, id, alias, enabled);

		this.getInfosAboutLoadedTariffs().subscribe(this::update);
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}

		if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
			this.onBeforeProcessCycle();
		}
	}

	private synchronized void onBeforeProcessCycle() {
		for (var channelTracker : this.channelTrackers.values()) {
			channelTracker.updateChannelValues();
		}
	}

	protected void update(List<TariffProvider.LoadedTariffInfo> tariffInfos) {
		final List<Tariff> tariffs = new ArrayList<>();
		final List<TariffUpdateStatus> updateFailures = new ArrayList<>();
		boolean hasNoPrices = false;
		Instant lastUpdateTime = null;

		for (var tariffInfo : tariffInfos) {
			tariffs.add(tariffInfo.tariff());

			final var tariffData = tariffInfo.tariff().getTariffData().getValue();
			if (tariffData == null || !tariffData.prices().hasDataInFuture(this.getClock())) {
				hasNoPrices = true;
			}

			if (tariffInfo.lastUpdateStatus() != null) {
				switch (tariffInfo.lastUpdateStatus()) {
				case TariffUpdateStatus.FailedWithException(var time, var ex) -> {
					updateFailures.add(tariffInfo.lastUpdateStatus());
					lastUpdateTime = DateUtils.max(lastUpdateTime, time);
				}
				case TariffUpdateStatus.FailedWithHttpError(var time, var status, var body) -> {
					updateFailures.add(tariffInfo.lastUpdateStatus());
					lastUpdateTime = DateUtils.max(lastUpdateTime, time);
				}
				case TariffUpdateStatus.Successful(var time) -> {
					lastUpdateTime = DateUtils.max(lastUpdateTime, time);
				}
				case TariffUpdateStatus.NotFetchedYet(var time) -> {
					hasNoPrices = true;
				}
				}
			}
		}

		ChannelUtils.setValue(this, TariffComponent.ChannelId.NO_PRICES, hasNoPrices);
		ChannelUtils.setValue(this, TariffComponent.ChannelId.NO_TARIFFS_REQUESTED, tariffInfos.isEmpty());
		ChannelUtils.setValue(this, TariffComponent.ChannelId.LAST_UPDATE,
				lastUpdateTime != null ? lastUpdateTime.getEpochSecond() : null);

		this.setChannelsForUpdateFailures(updateFailures);
		this.refreshChannelTrackerInstances(tariffs);
	}

	private synchronized void refreshChannelTrackerInstances(List<Tariff> tariffs) {
		for (var tariff : tariffs) {
			TariffChannelTracker tracker = this.channelTrackers.get(tariff);
			if (tracker == null) {
				tracker = new TariffChannelTracker(tariff);
				tracker.activate();

				this.channelTrackers.put(tariff, tracker);
			}

			tracker.updateChannelValues();
		}

		for (var existingTariff : this.channelTrackers.keySet().toArray(new Tariff[0])) {
			if (!tariffs.contains(existingTariff)) {
				final var tracker = this.channelTrackers.remove(existingTariff);
				tracker.deactivate();
			}
		}
	}

	protected abstract void setChannelsForUpdateFailures(List<TariffUpdateStatus> updateFailures);

	protected abstract String getChannelPrefixForTariff(Tariff tariff);

	/**
	 * Returns subscribable informations about the loaded tariffs and their status.
	 * 
	 * @return BehaviorSubject with information about loaded tariffs, must not be
	 *         null.
	 */
	public abstract BehaviorSubject<List<TariffProvider.LoadedTariffInfo>> getInfosAboutLoadedTariffs();

	/**
	 * Returns the clock used for current time.
	 *
	 * @return Clock instance
	 */
	public abstract Clock getClock();

	protected class TariffChannelTracker {
		private final Tariff tariff;
		private final TariffChannelId channelIds;

		protected TariffChannelTracker(Tariff tariff) {
			this.tariff = tariff;
			this.channelIds = new TariffChannelId(AbstractTariffComponent.this.getChannelPrefixForTariff(tariff));
		}

		public TariffChannelId getChannelIds() {
			return this.channelIds;
		}

		protected void activate() {
			for (var channelId : this.channelIds.getAllChannelIds()) {
				AbstractTariffComponent.this.addChannel(channelId);
			}
		}

		protected void deactivate() {
			for (var channelId : this.channelIds.getAllChannelIds()) {
				var channel = AbstractTariffComponent.this.channel(channelId);
				if (channel != null) {
					AbstractTariffComponent.this.removeChannel(channel);
				}
			}
		}

		public void updateChannelValues() {
			final var tariffData = Optional.ofNullable(this.tariff.getTariffData().getValue());
			final var lastUpdateStatus = this.tariff.getLastUpdateStatus().getValue();

			ChannelUtils.setValue(AbstractTariffComponent.this, this.channelIds.startDate,
					tariffData.map(d -> d.prices().getFirstTime().getEpochSecond()).orElse(null));
			ChannelUtils.setValue(AbstractTariffComponent.this, this.channelIds.endDate,
					tariffData.map(d -> d.prices().getLastTime().getEpochSecond()).orElse(null));
			ChannelUtils.setValue(AbstractTariffComponent.this, this.channelIds.copyright,
					tariffData.map(TariffData::copyright).orElse(null));
			ChannelUtils.setValue(AbstractTariffComponent.this, this.channelIds.currentPrice, //
					tariffData.map(d -> d.prices().getAt(AbstractTariffComponent.this.getClock().instant()))
							.orElse(null));
			ChannelUtils.setValue(AbstractTariffComponent.this, this.channelIds.currency,
					tariffData.map(d -> d.currency().getCurrencyCode()).orElse(null));
			ChannelUtils.setValue(AbstractTariffComponent.this, this.channelIds.fetchStatus,
					lastUpdateStatus.toString());
		}
	}

	protected static class TariffChannelId {

		private final String prefix;
		public final ChannelIdImpl startDate;
		public final ChannelIdImpl endDate;
		public final ChannelIdImpl currentPrice;
		public final ChannelIdImpl copyright;
		public final ChannelIdImpl currency;
		public final ChannelIdImpl fetchStatus;

		public TariffChannelId(String prefix) {
			this.prefix = prefix;

			this.startDate = new ChannelIdImpl(this.n("START_DATE"), //
					Doc.of(OpenemsType.LONG) //
							.unit(Unit.UNIX_TIMESTAMP_SECONDS) //
							.persistencePriority(PersistencePriority.VERY_LOW));
			this.endDate = new ChannelIdImpl(this.n("END_DATE"), //
					Doc.of(OpenemsType.LONG) //
							.unit(Unit.UNIX_TIMESTAMP_SECONDS) //
							.persistencePriority(PersistencePriority.VERY_LOW));

			this.currentPrice = new ChannelIdImpl(this.n("CURRENT_PRICE"), //
					Doc.of(OpenemsType.DOUBLE) //
							.unit(Unit.MONEY_PER_MEGAWATT_HOUR) //
							.persistencePriority(PersistencePriority.VERY_LOW));
			this.copyright = new ChannelIdImpl(this.n("COPYRIGHT"), //
					Doc.of(OpenemsType.STRING) //
							.persistencePriority(PersistencePriority.VERY_LOW));
			this.currency = new ChannelIdImpl(this.n("CURRENCY"), //
					Doc.of(OpenemsType.STRING) //
							.persistencePriority(PersistencePriority.VERY_LOW));
			this.fetchStatus = new ChannelIdImpl(this.n("FETCH_STATUS"), //
					Doc.of(OpenemsType.STRING) //
							.persistencePriority(PersistencePriority.VERY_LOW));
		}

		public ChannelIdImpl[] getAllChannelIds() {
			return new ChannelIdImpl[] { this.startDate, this.endDate, this.currentPrice, this.copyright, this.currency,
					this.fetchStatus };
		}

		private String n(String name) {
			if (this.prefix == null || this.prefix.isEmpty()) {
				return name;
			}
			return this.prefix + "_" + name;
		}
	}
}
