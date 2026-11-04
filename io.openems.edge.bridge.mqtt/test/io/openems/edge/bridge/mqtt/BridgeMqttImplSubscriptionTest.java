package io.openems.edge.bridge.mqtt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;

import io.openems.edge.bridge.mqtt.api.MqttMessage;
import io.openems.edge.bridge.mqtt.api.QoS;
import io.openems.edge.common.test.ComponentTest;

/**
 * Several subscriptions to the same topic filter, e.g. two Components sharing
 * an acknowledgement topic.
 */
public class BridgeMqttImplSubscriptionTest {

	/** Records broker (un)subscriptions and keeps the dispatchers. */
	private static class FakeConnectionHandler implements MqttConnectionHandler {

		private final Map<String, Consumer<MqttMessage>> dispatchers = new HashMap<>();
		private final List<String> subscribed = new ArrayList<>();
		private final List<String> unsubscribed = new ArrayList<>();

		@Override
		public void connect(ConnectionCallback callback) {
		}

		@Override
		public void disconnect() {
		}

		@Override
		public boolean isConnected() {
			return true;
		}

		@Override
		public CompletableFuture<Void> publish(String topic, byte[] payload, QoS qos, boolean retained) {
			return CompletableFuture.completedFuture(null);
		}

		@Override
		public void subscribe(String topicFilter, QoS qos, Consumer<MqttMessage> callback) {
			this.subscribed.add(topicFilter);
			this.dispatchers.put(topicFilter, callback);
		}

		@Override
		public CompletableFuture<Void> unsubscribe(String topicFilter) {
			this.unsubscribed.add(topicFilter);
			this.dispatchers.remove(topicFilter);
			return CompletableFuture.completedFuture(null);
		}

		private void deliver(String topicFilter, String topic, String payload) {
			var dispatcher = this.dispatchers.get(topicFilter);
			if (dispatcher != null) {
				dispatcher.accept(MqttMessage.of(topic, payload, QoS.AT_LEAST_ONCE, false));
			}
		}
	}

	private BridgeMqttImpl bridge;
	private FakeConnectionHandler broker;

	@Before
	public void setUp() throws Exception {
		this.bridge = new BridgeMqttImpl();
		// Disabled: activates without connecting to a real broker.
		new ComponentTest(this.bridge) //
				.activate(MyConfig.create() //
						.setId("mqtt0") //
						.setEnabled(false) //
						.build());
		this.broker = new FakeConnectionHandler();
		var field = BridgeMqttImpl.class.getDeclaredField("connectionHandler");
		field.setAccessible(true);
		field.set(this.bridge, this.broker);
	}

	@Test
	public void bothSubscribersToTheSameFilterReceiveItsMessages() {
		var first = new ArrayList<String>();
		var second = new ArrayList<String>();
		this.bridge.subscribe("device/ack", QoS.AT_LEAST_ONCE, msg -> first.add(msg.payloadAsString()));
		this.bridge.subscribe("device/ack", QoS.AT_LEAST_ONCE, msg -> second.add(msg.payloadAsString()));

		this.broker.deliver("device/ack", "device/ack", "ok");

		assertEquals(List.of("ok"), first);
		assertEquals(List.of("ok"), second);
		assertEquals("one broker subscription is shared", List.of("device/ack"), this.broker.subscribed);
	}

	@Test
	public void unsubscribingOneKeepsTheOtherAndTheBrokerSubscription() throws Exception {
		var second = new ArrayList<String>();
		var firstSubscription = this.bridge.subscribe("device/ack", QoS.AT_LEAST_ONCE, msg -> {
		});
		final var secondSubscription = this.bridge.subscribe("device/ack", QoS.AT_LEAST_ONCE,
				msg -> second.add(msg.payloadAsString()));

		firstSubscription.unsubscribe().get();
		assertTrue(this.broker.unsubscribed.isEmpty());
		this.broker.deliver("device/ack", "device/ack", "ok");
		assertEquals(List.of("ok"), second);

		secondSubscription.unsubscribe().get();
		assertEquals("the last one removes the broker subscription", List.of("device/ack"),
				this.broker.unsubscribed);
	}

	@Test
	public void overlappingFiltersEachGetTheirOwnMessagesOnce() {
		var wildcard = new ArrayList<String>();
		var exact = new ArrayList<String>();
		this.bridge.subscribe("device/#", QoS.AT_LEAST_ONCE, msg -> wildcard.add(msg.topic()));
		this.bridge.subscribe("device/ack", QoS.AT_LEAST_ONCE, msg -> exact.add(msg.topic()));

		// Paho calls the listener of every subscribed filter the topic matches.
		this.broker.deliver("device/#", "device/ack", "ok");
		this.broker.deliver("device/ack", "device/ack", "ok");

		assertEquals(List.of("device/ack"), wildcard);
		assertEquals(List.of("device/ack"), exact);
	}
}
