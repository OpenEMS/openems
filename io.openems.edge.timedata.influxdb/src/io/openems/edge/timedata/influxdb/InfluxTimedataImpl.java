package io.openems.edge.timedata.influxdb;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.cycle.Cycle;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.timedata.api.Timedata;
import io.openems.shared.influxdb.InfluxConnector;

/**
 * Provides read and write access to InfluxDB.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Timedata.InfluxDB", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class InfluxTimedataImpl extends AbstractOpenemsComponent
		implements InfluxTimedata, Timedata, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(InfluxTimedataImpl.class);

	@Reference
	private Cycle cycle;

	private InfluxConnector influxConnector = null;

	// Counts the number of Cycles till data is written to InfluxDB.
	private int cycleCount = 0;

	private Config config;

	public InfluxTimedataImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Timedata.ChannelId.values(), //
				InfluxTimedata.ChannelId.values() //
		);
	}

	@Reference
	protected ComponentManager componentManager;

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		if (!this.isEnabled()) {
			return;
		}

		this.influxConnector = new InfluxConnector(config.queryLanguage(), URI.create(config.url()), config.org(),
				config.apiKey(), config.bucket(), config.isReadOnly(), 5, config.maxQueueSize(), //
				(throwable) -> {
					this.logError(this.log, "Unable to write to InfluxDB: " + throwable.getMessage());
				});
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		if (this.influxConnector != null) {
			this.influxConnector.deactivate();
		}
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.collectAndWriteChannelValues();
			break;
		}
	}

	protected synchronized void collectAndWriteChannelValues() {
		var cycleTime = this.cycle.getCycleTime(); // [ms]
		var timestamp = System.currentTimeMillis() / cycleTime * cycleTime; // Round value to Cycle-Time in [ms]

		if (++this.cycleCount >= this.config.noOfCycles()) {
			this.cycleCount = 0;
			final var point = Point.measurement(InfluxConnector.MEASUREMENT).time(timestamp, WritePrecision.MS);
			final var addedAtLeastOneChannelValue = new AtomicBoolean(false);

			this.componentManager.getEnabledComponents().stream().filter(OpenemsComponent::isEnabled)
					.forEach(component -> {
						component.channels().forEach(channel -> {
							switch (channel.channelDoc().getAccessMode()) {
							case WRITE_ONLY:
								// ignore Write-Only-Channels
								return;
							case READ_ONLY:
							case READ_WRITE:
								break;
							}

							Optional<?> valueOpt = channel.value().asOptional();
							if (!valueOpt.isPresent()) {
								// ignore not available channels
								return;
							}
							Object value = valueOpt.get();
							var address = channel.address().toString();
							try {
								switch (channel.getType()) {
								case BOOLEAN:
									point.addField(address, (Boolean) value ? 1 : 0);
									break;
								case SHORT:
									point.addField(address, (Short) value);
									break;
								case INTEGER:
									point.addField(address, (Integer) value);
									break;
								case LONG:
									point.addField(address, (Long) value);
									break;
								case FLOAT:
									point.addField(address, (Float) value);
									break;
								case DOUBLE:
									point.addField(address, (Double) value);
									break;
								case STRING:
									point.addField(address, (String) value);
									break;
								}
							} catch (IllegalArgumentException e) {
								this.log.warn("Unable to add Channel [" + address + "] value [" + value + "]: "
										+ e.getMessage());
								return;
							}
							addedAtLeastOneChannelValue.set(true);
						});
					});

			if (addedAtLeastOneChannelValue.get()) {
				this.influxConnector.write(point);
			}
		}
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
			throws OpenemsNamedException {
		// ignore edgeId as Points are also written without Edge-ID
		Optional<Integer> influxEdgeId = Optional.empty();
		return this.influxConnector.queryHistoricData(influxEdgeId, fromDate, toDate, channels, resolution);
	}

	@Override
	public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(String edgeId, ZonedDateTime fromDate,
			ZonedDateTime toDate, Set<ChannelAddress> channels) throws OpenemsNamedException {
		// ignore edgeId as Points are also written without Edge-ID
		Optional<Integer> influxEdgeId = Optional.empty();
		return this.influxConnector.queryHistoricEnergy(influxEdgeId, fromDate, toDate, channels);
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricEnergyPerPeriod(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
			throws OpenemsNamedException {
		// ignore edgeId as Points are also written without Edge-ID
		Optional<Integer> influxEdgeId = Optional.empty();
		return this.influxConnector.queryHistoricEnergyPerPeriod(influxEdgeId, fromDate, toDate, channels, resolution);
	}

	@Override
	public CompletableFuture<Optional<Object>> getLatestValue(ChannelAddress channelAddress) {
		// TODO implement this method
		return CompletableFuture.completedFuture(Optional.empty());
	}

}
