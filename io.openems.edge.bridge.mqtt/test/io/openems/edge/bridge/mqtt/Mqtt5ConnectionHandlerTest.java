package io.openems.edge.bridge.mqtt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutionException;

import org.junit.Test;

import io.openems.edge.bridge.mqtt.api.MqttVersion;
import io.openems.edge.bridge.mqtt.api.QoS;

/**
 * Tests for {@link Mqtt5ConnectionHandler}.
 */
public class Mqtt5ConnectionHandlerTest {

	@Test
	public void testConstructor() {
		var config = MyConfig.create() //
				.setId("mqtt0") //
				.setMqttVersion(MqttVersion.V5) //
				.setHost("localhost") //
				.setPort(1883) //
				.setSecureConnect(false) //
				.setClientId("test-client") //
				.setUsername("") //
				.setPassword("") //
				.setCleanSession(true) //
				.setKeepAliveInterval(60) //
				.build();

		var handler = new Mqtt5ConnectionHandler(config, "localhost", 1883, "test-client", false);

		assertNotNull(handler);
		assertFalse(handler.isConnected());
	}

	@Test
	public void testConstructorWithSsl() {
		var config = MyConfig.create() //
				.setId("mqtt0") //
				.setMqttVersion(MqttVersion.V5) //
				.setHost("localhost") //
				.setPort(8883) //
				.setSecureConnect(true) //
				.setClientId("test-client-ssl") //
				.build();

		var handler = new Mqtt5ConnectionHandler(config, "localhost", 8883, "test-client-ssl", true);

		assertNotNull(handler);
		assertFalse(handler.isConnected());
	}

	@Test
	public void testIsConnectedWhenNotConnected() {
		var config = MyConfig.create().build();
		var handler = new Mqtt5ConnectionHandler(config, "localhost", 1883, "client", false);

		assertFalse(handler.isConnected());
	}

	@Test
	public void testPublishWhenNotConnected() throws Exception {
		var config = MyConfig.create().build();
		var handler = new Mqtt5ConnectionHandler(config, "localhost", 1883, "client", false);

		var future = handler.publish("test/topic", "payload".getBytes(), QoS.AT_LEAST_ONCE, false);

		assertTrue(future.isCompletedExceptionally());
		try {
			future.get();
		} catch (ExecutionException e) {
			assertTrue(e.getCause() instanceof IllegalStateException);
			assertEquals("Not connected", e.getCause().getMessage());
		}
	}

	@Test
	public void testSubscribeWhenNotConnected() {
		var config = MyConfig.create().build();
		var handler = new Mqtt5ConnectionHandler(config, "localhost", 1883, "client", false);

		// Should not throw, just return early
		handler.subscribe("test/topic", QoS.AT_LEAST_ONCE, msg -> {
		});

		assertFalse(handler.isConnected());
	}

	@Test
	public void testUnsubscribeWhenNotConnected() throws Exception {
		var config = MyConfig.create().build();
		var handler = new Mqtt5ConnectionHandler(config, "localhost", 1883, "client", false);

		var future = handler.unsubscribe("test/topic");

		// Should complete immediately without error
		assertNotNull(future);
		future.get(); // Should not throw
	}

	@Test
	public void testDisconnectWhenNotConnected() {
		var config = MyConfig.create().build();
		var handler = new Mqtt5ConnectionHandler(config, "localhost", 1883, "client", false);

		// Should not throw
		handler.disconnect();
		assertFalse(handler.isConnected());
	}

	@Test
	public void testTopicMatchesExact() {
		assertTrue(MqttConnectionHandler.topicMatchesFilter("sensors/temperature", "sensors/temperature"));
		assertFalse(MqttConnectionHandler.topicMatchesFilter("sensors/humidity", "sensors/temperature"));
	}

	@Test
	public void testTopicMatchesSingleLevelWildcard() {
		assertTrue(MqttConnectionHandler.topicMatchesFilter("sensors/temp/data", "sensors/+/data"));
		assertTrue(MqttConnectionHandler.topicMatchesFilter("sensors/humidity/data", "sensors/+/data"));
		assertFalse(MqttConnectionHandler.topicMatchesFilter("sensors/temp/value", "sensors/+/data"));
		assertFalse(MqttConnectionHandler.topicMatchesFilter("devices/temp/data", "sensors/+/data"));
	}

	@Test
	public void testTopicMatchesMultiLevelWildcard() {
		assertTrue(MqttConnectionHandler.topicMatchesFilter("sensors/temperature", "sensors/#"));
		assertTrue(MqttConnectionHandler.topicMatchesFilter("sensors/temperature/value", "sensors/#"));
		assertTrue(MqttConnectionHandler.topicMatchesFilter("sensors/a/b/c/d", "sensors/#"));
		assertFalse(MqttConnectionHandler.topicMatchesFilter("devices/temperature", "sensors/#"));
	}

	@Test
	public void testTopicMatchesCombinedWildcards() {
		assertTrue(MqttConnectionHandler.topicMatchesFilter("home/sensors/temp", "+/sensors/#"));
		assertTrue(MqttConnectionHandler.topicMatchesFilter("office/sensors/temp/value", "+/sensors/#"));
		assertFalse(MqttConnectionHandler.topicMatchesFilter("home/devices/temp", "+/sensors/#"));
	}

	@Test
	public void testTopicMatchesEdgeCases() {
		// Empty or single segment
		assertTrue(MqttConnectionHandler.topicMatchesFilter("topic", "topic"));
		assertFalse(MqttConnectionHandler.topicMatchesFilter("other", "topic"));

		// Different length paths
		assertFalse(MqttConnectionHandler.topicMatchesFilter("a/b/c", "a/b"));
		assertFalse(MqttConnectionHandler.topicMatchesFilter("a/b", "a/b/c"));
	}

}
