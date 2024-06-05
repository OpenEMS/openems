package io.openems.edge.timedata.influxdb;

import static java.util.Collections.emptySortedMap;

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
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.cycle.Cycle;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.Timeranges;
import io.openems.shared.influxdb.InfluxConnector;

/**
 * Provides read and write access to InfluxDB.
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Timedata.InfluxDB", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class TimedataInfluxDbImpl extends AbstractOpenemsComponent
		implements TimedataInfluxDb, Timedata, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(TimedataInfluxDbImpl.class);

	@Reference
	private Cycle cycle;

	@Reference
	private ComponentManager componentManager;

	@Reference
	private OpenemsEdgeOem oem;

	private InfluxConnector influxConnector = null;

	/** Counts the number of Cycles till data is written to InfluxDB. */
	private int cycleCount = 0;

	private Config config;

	public TimedataInfluxDbImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Timedata.ChannelId.values(), //
				TimedataInfluxDb.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		if (!this.isEnabled()) {
			return;
		}

		this.influxConnector = new InfluxConnector(config.id(), config.queryLanguage(), URI.create(config.url()),
				config.org(), config.apiKey(), config.bucket(), this.oem.getInfluxdbTag(), config.isReadOnly(), 5,
				config.maxQueueSize(), //
				(e) -> {
					// ignore
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
			final var point = Point.measurement(this.config.measurement()).time(timestamp, WritePrecision.MS);
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
							
							// ignore channel with lower priority than configured
							if (!channel.channelDoc().getPersistencePriority().isAtLeast(this.config.persistencePriority())) {
								return;
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
		return this.influxConnector.queryHistoricData(influxEdgeId, fromDate, toDate, channels, resolution,
				this.config.measurement());
	}

	@Override
	public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(String edgeId, ZonedDateTime fromDate,
			ZonedDateTime toDate, Set<ChannelAddress> channels) throws OpenemsNamedException {
		// ignore edgeId as Points are also written without Edge-ID
		Optional<Integer> influxEdgeId = Optional.empty();
		return this.influxConnector.queryHistoricEnergy(influxEdgeId, fromDate, toDate, channels,
				this.config.measurement());
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricEnergyPerPeriod(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
			throws OpenemsNamedException {
		// ignore edgeId as Points are also written without Edge-ID
		Optional<Integer> influxEdgeId = Optional.empty();
		return this.influxConnector.queryHistoricEnergyPerPeriod(influxEdgeId, fromDate, toDate, channels, resolution,
				this.config.measurement());
	}

	@Override
	public SortedMap<Long, SortedMap<ChannelAddress, JsonElement>> queryResendData(ZonedDateTime fromDate,
			ZonedDateTime toDate, Set<ChannelAddress> channels) throws OpenemsNamedException {
		// TODO implement this method
		return emptySortedMap();
	}

	@Override
	public CompletableFuture<Optional<Object>> getLatestValue(ChannelAddress channelAddress) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				SortedMap<ChannelAddress, JsonElement> sortedMap = this.influxConnector.queryLastData(Optional.empty(),
						channelAddress, this.config.measurement());

				if (sortedMap != null && !sortedMap.isEmpty() && sortedMap.containsKey(channelAddress)) {
					JsonElement latestValue = sortedMap.get(channelAddress);

					// Check if it´s a number and can be converted to long
					if (latestValue.isJsonPrimitive()) {
						if (latestValue.getAsJsonPrimitive().isNumber()) {
							return Optional.of(latestValue.getAsLong());
						}
					}
				} else {
					// No data found
					return Optional.empty();
				}
			} catch (Exception e) {
				this.log.error("Error getting latest value", e);
			}
			return Optional.empty();
		});
	}

	@Override
	public Timeranges getResendTimeranges(ChannelAddress notSendChannel, long lastResendTimestamp)
			throws OpenemsNamedException {
		// TODO implement this method
		return new Timeranges();
	}
}
