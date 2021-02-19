package io.openems.edge.controller.api.mqpp;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableTable;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import io.openems.common.channel.AccessMode;
import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.common.component.OpenemsComponent;

public class ChannelValuesWorker extends AbstractCycleWorker {

	private final static int MQTT_QOS = 0; // loss is ok
	private final static boolean MQTT_RETAIN = true; // send last value to subscriber
	private final static MqttProperties MQTT_PROPERTIES;
	static {
		MQTT_PROPERTIES = new MqttProperties();
		MQTT_PROPERTIES.setMessageExpiryInterval(60L); // channel value is only valid for 60 seconds
	}

	private final Logger log = LoggerFactory.getLogger(ChannelValuesWorker.class);
	private final MqttApiControllerImpl parent;

	private ImmutableTable<String, String, JsonElement> lastValues = ImmutableTable.of();

	protected ChannelValuesWorker(MqttApiControllerImpl parent) {
		this.parent = parent;
	}

	@Override
	public void activate(String name) {
		super.activate(name);
	}

	@Override
	public void deactivate() {
		super.deactivate();
	}

	/**
	 * Triggers sending all Channel values once. After executing once, this is reset
	 * automatically to default 'send changed values only' mode.
	 */
	public synchronized void sendValuesOfAllChannelsOnce() {
		this.lastValues = ImmutableTable.of();
		this.triggerNextRun();
	}

	@Override
	protected synchronized void forever() {
		// TODO reset this.lastValues once in a while to trigger resend of all channels
		final List<OpenemsComponent> enabledComponents = this.parent.componentManager.getEnabledComponents();

		// Update the data from ChannelValues
		final ImmutableTable<String, String, JsonElement> allValues = this.collectData(enabledComponents);

		// Get timestamp and round to Cycle-Time
		final int cycleTime = this.getCycleTime();
		final long timestamp = Instant.now(this.parent.componentManager.getClock()).toEpochMilli() / cycleTime
				* cycleTime;

		// Send Changed values
		allValues.rowMap().entrySet().parallelStream() //
				.forEach(row -> {
					row.getValue().entrySet().parallelStream() //
							.forEach(column -> {
								if (!Objects.equals(column.getValue(),
										this.lastValues.get(row.getKey(), column.getKey()))) {
									this.publish(row.getKey() + "/" + column.getKey(), column.getValue().toString());
								}
							});
				});

		// Update disappeared components
		final Set<String> enabledComponentIds = enabledComponents.stream() //
				.map(c -> c.id()) //
				.collect(Collectors.toSet());
		this.lastValues.rowMap().entrySet().stream() //
				.filter(row -> !enabledComponentIds.contains(row.getKey())) //
				.forEach(row -> {
					row.getValue().entrySet().parallelStream() //
							.forEach(column -> {
								this.publish(row.getKey() + "/" + column.getKey(), JsonNull.INSTANCE.toString());
							});
				});

		// Update lastUpdate timestamp
		this.publish(MqttApiController.TOPIC_CHANNEL_LAST_UPDATE, String.valueOf(timestamp));

		// Keep values for next run
		this.lastValues = allValues;

		this.log.info("Sent...");
	}

	/**
	 * Cycles through all Channels and collects the value.
	 * 
	 * @param enabledComponents the enabled components
	 * @return collected data
	 */
	private ImmutableTable<String, String, JsonElement> collectData(List<OpenemsComponent> enabledComponents) {
		return enabledComponents.parallelStream() //
				.flatMap(component -> component.channels().parallelStream()) //
				.filter(channel -> // Ignore WRITE_ONLY Channels
				channel.channelDoc().getAccessMode() == AccessMode.READ_ONLY
						|| channel.channelDoc().getAccessMode() == AccessMode.READ_WRITE)
				.collect(ImmutableTable.toImmutableTable(c -> c.address().getComponentId(),
						c -> c.address().getChannelId(), c -> c.value().asJson()));
	}

	private void publish(String subTopic, String value) {
		if (!this.parent.publish(//
				/* topic */ MqttApiController.TOPIC_CHANNEL_PREFIX + subTopic, //
				/* message */ value.toString(), //
				MQTT_QOS, MQTT_RETAIN, MQTT_PROPERTIES //
		)) {
			this.parent.logWarn(this.log, "Unable to send [" + subTopic + "=" + value + "]");
			// TODO ?
		}
	}

}