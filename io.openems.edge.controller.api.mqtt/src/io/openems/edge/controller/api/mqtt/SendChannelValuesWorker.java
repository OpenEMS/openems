package io.openems.edge.controller.api.mqtt;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonElement;

import io.openems.common.channel.AccessMode;
import io.openems.common.utils.StringUtils;
import io.openems.common.utils.ThreadPoolUtils;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Method {@link #collectData()} is called Synchronously with the Core.Cycle to
 * collect values of Channels. Sending of values is then delegated to an
 * asynchronous task.
 *
 * <p>
 * The logic tries to send changed values once per Cycle and all values once
 * every {@link #SEND_VALUES_OF_ALL_CHANNELS_AFTER_SECONDS}.
 */
public class SendChannelValuesWorker {

	private static final int MQTT_QOS = 0; // loss is ok
	private static final boolean MQTT_RETAIN = true; // send last value to subscriber
	private static final int SEND_VALUES_OF_ALL_CHANNELS_AFTER_SECONDS = 300; /* 5 minutes */
	private static final MqttProperties MQTT_PROPERTIES;

	static {
		MQTT_PROPERTIES = new MqttProperties();
		// channel value is only valid for restricted time
		MQTT_PROPERTIES.setMessageExpiryInterval(Long.valueOf(SEND_VALUES_OF_ALL_CHANNELS_AFTER_SECONDS * 2));
	}

	private final Logger log = LoggerFactory.getLogger(SendChannelValuesWorker.class);
	private final ControllerApiMqttImpl parent;

	private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS,
			new ArrayBlockingQueue<>(1), //
			new ThreadFactoryBuilder().setNameFormat(ControllerApiMqttImpl.COMPONENT_NAME + ":SendWorker-%d").build(), //
			new ThreadPoolExecutor.DiscardOldestPolicy());

	/**
	 * If true: next 'send' sends all channel values.
	 */
	private final AtomicBoolean sendValuesOfAllChannels = new AtomicBoolean(true);

	/**
	 * Keeps the last timestamp when all channel values were sent.
	 */
	private Instant lastSendValuesOfAllChannels = Instant.MIN;

	/**
	 * Keeps the values of last successful send.
	 */
	private Table<String, String, JsonElement> lastAllValues = ImmutableTable.of();

	protected SendChannelValuesWorker(ControllerApiMqttImpl parent) {
		this.parent = parent;
	}

	/**
	 * Triggers sending all Channel values once.
	 */
	public synchronized void sendValuesOfAllChannelsOnce() {
		this.sendValuesOfAllChannels.set(true);
	}

	/**
	 * Stops the {@link SendChannelValuesWorker}.
	 */
	public void deactivate() {
		// Shutdown executor
		ThreadPoolUtils.shutdownAndAwaitTermination(this.executor, 5);
	}

	/**
	 * Called synchronously on AFTER_PROCESS_IMAGE event. Collects all the data and
	 * triggers asynchronous sending.
	 */
	public synchronized void collectData() {
		var now = Instant.now(this.parent.componentManager.getClock());

		// Update the values of all channels
		final var enabledComponents = this.parent.componentManager.getEnabledComponents();
		final var allValues = this.collectData(enabledComponents);

		// Add to send Queue
		this.executor.execute(new SendTask(this, now, allValues));
	}

	/**
	 * Cycles through all Channels and collects the value.
	 *
	 * @param enabledComponents the enabled components
	 * @return collected data
	 */
	private ImmutableTable<String, String, JsonElement> collectData(List<OpenemsComponent> enabledComponents) {
		try {
			return enabledComponents.parallelStream() //
					.flatMap(component -> component.channels().parallelStream()) //
					.filter(channel -> // Ignore WRITE_ONLY Channels
					channel.channelDoc().getAccessMode() != AccessMode.WRITE_ONLY //
							// Ignore Low-Priority Channels
							&& channel.channelDoc().getPersistencePriority()
									.isAtLeast(this.parent.config.persistencePriority()))
					.collect(ImmutableTable.toImmutableTable(c -> c.address().getComponentId(),
							c -> c.address().getChannelId(), c -> c.value().asJson()));
			// TODO remove values for disappeared components
//			final Set<String> enabledComponentIds = enabledComponents.stream() //
//					.map(c -> c.id()) //
//					.collect(Collectors.toSet());
//			this.lastValues.rowMap().entrySet().stream() //
//					.filter(row -> !enabledComponentIds.contains(row.getKey())) //
//					.forEach(row -> {
//						row.getValue().entrySet().parallelStream() //
//								.forEach(column -> {
//									this.publish(row.getKey() + "/" + column.getKey(), JsonNull.INSTANCE.toString());
//								});
//					});
		} catch (Exception e) {
			// ConcurrentModificationException can happen if Channels are dynamically added
			// or removed
			return ImmutableTable.of();
		}
	}

	/*
	 * From here things run asynchronously.
	 */

	private static class SendTask implements Runnable {

		private final SendChannelValuesWorker parent;
		private final Instant timestamp;
		private final ImmutableTable<String, String, JsonElement> allValues;

		public SendTask(SendChannelValuesWorker parent, Instant timestamp,
				ImmutableTable<String, String, JsonElement> allValues) {
			this.parent = parent;
			this.timestamp = timestamp;
			this.allValues = allValues;
		}

		@Override
		public void run() {
			// Holds the data of the last successful send. If the table is empty, it is also
			// used as a marker to send all data.
			final Table<String, String, JsonElement> lastAllValues;

			if (this.parent.sendValuesOfAllChannels.getAndSet(false)) {
				// Send values of all Channels once in a while
				lastAllValues = ImmutableTable.of();

			} else if (Duration.between(this.parent.lastSendValuesOfAllChannels, this.timestamp)
					.getSeconds() > SEND_VALUES_OF_ALL_CHANNELS_AFTER_SECONDS) {
				// Send values of all Channels if explicitly asked for
				lastAllValues = ImmutableTable.of();

			} else {
				// Actually use the kept 'lastSentValues'
				// CHECKSTYLE:OFF
				lastAllValues = this.parent.lastAllValues;
				// CHECKSTYLE:ON
			}

			// Send changed values
			var allSendSuccessful = true;
			List<String> sendTopics = new ArrayList<>();
			for (Entry<String, Map<String, JsonElement>> row : this.allValues.rowMap().entrySet()) {
				for (Entry<String, JsonElement> column : row.getValue().entrySet()) {
					if (!Objects.equals(column.getValue(), lastAllValues.get(row.getKey(), column.getKey()))) {
						var subtopic = row.getKey() + "/" + column.getKey();
						sendTopics.add(subtopic);
						if (!this.publish(row.getKey() + "/" + column.getKey(), column.getValue().toString())) {
							allSendSuccessful = false;
						}
					}
				}
			}

			// Update lastUpdate timestamp
			this.publish(ControllerApiMqtt.TOPIC_CHANNEL_LAST_UPDATE, String.valueOf(this.timestamp));

			// Successful?
			if (allSendSuccessful) {
				this.parent.parent.logInfo(this.parent.log, "Successfully sent MQTT topics: "
						+ StringUtils.toShortString(String.join(", ", sendTopics), 100));

				// update information for next runs
				this.parent.lastAllValues = this.allValues;
				if (lastAllValues.isEmpty()) {
					// 'lastSentValues' was empty, i.e. all values were sent
					this.parent.lastSendValuesOfAllChannels = this.timestamp;
				}
			} else {
				this.parent.parent.logWarn(this.parent.log, "Error while sending MQTT topics: "
						+ StringUtils.toShortString(String.join(", ", sendTopics), 100));
			}
		}

		/**
		 * Publish a Channel value message.
		 *
		 * @param subTopic the Channel Subtopic
		 * @param value    the value Json.toString()
		 * @return true if sent successfully; false otherwise
		 */
		private boolean publish(String subTopic, String value) {
			return this.parent.parent.publish(//
					/* topic */ ControllerApiMqtt.TOPIC_CHANNEL_PREFIX + subTopic, //
					/* message */ value.toString(), //
					MQTT_QOS, MQTT_RETAIN, MQTT_PROPERTIES //
			);
		}

	}

}