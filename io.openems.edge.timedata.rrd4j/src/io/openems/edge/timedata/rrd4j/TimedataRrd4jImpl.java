package io.openems.edge.timedata.rrd4j;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.CompletableFuture;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import com.google.gson.JsonElement;

import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.Timeranges;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Timedata.Rrd4j", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public final class TimedataRrd4jImpl extends AbstractOpenemsComponent
		implements TimedataRrd4j, Timedata, OpenemsComponent, EventHandler {

	@Reference
	private RecordWorkerFactory workerFactory;
	private RecordWorker worker;

	@Reference
	private Rrd4jReadHandler readHandler;

	private boolean debugMode = false;

	public TimedataRrd4jImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Timedata.ChannelId.values(), //
				TimedataRrd4j.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws Exception {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.debugMode = config.debugMode();

		this.worker = this.workerFactory.get();
		this.worker.setConfig(new RecordWorker.Config(//
				this.id(), //
				config.isReadOnly(), //
				this.debugMode, //
				config.persistencePriority(), //
				isFull -> this._setQueueIsFull(isFull), //
				unableToInsert -> this._setUnableToInsertSample(unableToInsert) //
		));
		if (config.enabled()) {
			this.worker.activate(this.id());
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.workerFactory.unget(this.worker);
		this.worker = null;
	}

	@Override
	public Timeranges getResendTimeranges(//
			final ChannelAddress notSendChannel, //
			final long lastResendTimestamp //
	) throws OpenemsNamedException {
		return this.readHandler.getResendTimeranges(this.id(), notSendChannel, lastResendTimestamp, this.debugMode);
	}

	@Override
	public SortedMap<Long, SortedMap<ChannelAddress, JsonElement>> queryResendData(//
			final ZonedDateTime fromDate, //
			final ZonedDateTime toDate, //
			final Set<ChannelAddress> channels //
	) throws OpenemsNamedException {
		return this.readHandler.queryResendData(this.id(), fromDate, toDate, channels, this.debugMode);
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(//
			final String edgeId, //
			final ZonedDateTime fromDate, //
			final ZonedDateTime toDate, //
			final Set<ChannelAddress> channels, //
			final Resolution resolution //
	) throws OpenemsNamedException {
		return this.readHandler.queryHistoricData(this.id(), fromDate, toDate, channels, resolution, this.debugMode);
	}

	@Override
	public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(//
			final String edgeId, //
			final ZonedDateTime fromDate, //
			final ZonedDateTime toDate, //
			final Set<ChannelAddress> channels //
	) throws OpenemsNamedException {
		return this.readHandler.queryHistoricEnergy(this.id(), fromDate, toDate, channels, this.debugMode);
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricEnergyPerPeriod(//
			final String edgeId, //
			final ZonedDateTime fromDate, //
			final ZonedDateTime toDate, //
			final Set<ChannelAddress> channels, //
			final Resolution resolution //
	) throws OpenemsNamedException {
		return this.readHandler.queryHistoricEnergyPerPeriod(this.id(), fromDate, toDate, channels, resolution,
				this.debugMode);
	}

	@Override
	public CompletableFuture<Optional<Object>> getLatestValue(ChannelAddress channelAddress) {
		return this.readHandler.getLatestValue(this.id(), channelAddress);
	}

	@Override
	public CompletableFuture<Optional<Object>> getLatestValueOfNotExistingChannel(ChannelAddress channelAddress,
			Unit unit) {
		return this.readHandler.getLatestValueOfNotExistingChannel(this.id(), channelAddress, unit);
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.worker.collectData();
			break;
		}
	}

}
