package io.openems.edge.bridge.mqtt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;

import org.junit.Test;

import io.openems.edge.bridge.mqtt.api.MqttVersion;
import io.openems.edge.bridge.mqtt.api.QoS;

/**
 * Tests for {@link Mqtt3ConnectionHandler}.
 */
public class Mqtt3ConnectionHandlerTest {

	@Test
	public void testConstructor() {
		var config = MyConfig.create() //
				.setId("mqtt0") //
				.setMqttVersion(MqttVersion.V3_1_1) //
				.setUri("tcp://localhost:1883") //
				.setClientId("test-client") //
				.setUsername("") //
				.setPassword("") //
				.setCleanSession(true) //
				.setKeepAliveInterval(60) //
				.build();

		var handler = new Mqtt3ConnectionHandler(config, "localhost", 1883, "test-client", false, MqttVersion.V3_1_1);

		assertNotNull(handler);
		assertFalse(handler.isConnected());
	}

	@Test
	public void testConstructorWithSsl() {
		var config = MyConfig.create() //
				.setId("mqtt0") //
				.setMqttVersion(MqttVersion.V3_1) //
				.setUri("ssl://localhost:8883") //
				.setClientId("test-client-ssl") //
				.build();

		var handler = new Mqtt3ConnectionHandler(config, "localhost", 8883, "test-client-ssl", true, MqttVersion.V3_1);

		assertNotNull(handler);
		assertFalse(handler.isConnected());
	}

	@Test
	public void testIsConnectedWhenNotConnected() {
		var config = MyConfig.create().build();
		var handler = new Mqtt3ConnectionHandler(config, "localhost", 1883, "client", false, MqttVersion.V3_1_1);

		assertFalse(handler.isConnected());
	}

	@Test
	public void testPublishWhenNotConnected() throws Exception {
		var config = MyConfig.create().build();
		var handler = new Mqtt3ConnectionHandler(config, "localhost", 1883, "client", false, MqttVersion.V3_1_1);

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
		var handler = new Mqtt3ConnectionHandler(config, "localhost", 1883, "client", false, MqttVersion.V3_1_1);

		// Should not throw, just return early
		handler.subscribe("test/topic", QoS.AT_LEAST_ONCE, msg -> {
		});

		assertFalse(handler.isConnected());
	}

	@Test
	public void testUnsubscribeWhenNotConnected() throws Exception {
		var config = MyConfig.create().build();
		var handler = new Mqtt3ConnectionHandler(config, "localhost", 1883, "client", false, MqttVersion.V3_1_1);

		var future = handler.unsubscribe("test/topic");

		// Should complete immediately without error
		assertNotNull(future);
		future.get(); // Should not throw
	}

	@Test
	public void testDisconnectWhenNotConnected() {
		var config = MyConfig.create().build();
		var handler = new Mqtt3ConnectionHandler(config, "localhost", 1883, "client", false, MqttVersion.V3_1_1);

		// Should not throw
		handler.disconnect();
		assertFalse(handler.isConnected());
	}

	@Test
	public void testTopicMatchesExact() throws Exception {
		var config = MyConfig.create().build();
		var handler = new Mqtt3ConnectionHandler(config, "localhost", 1883, "client", false, MqttVersion.V3_1_1);

		assertTrue(this.invokeTopicMatches(handler, "sensors/temperature", "sensors/temperature"));
		assertFalse(this.invokeTopicMatches(handler, "sensors/temperature", "sensors/humidity"));
	}

	@Test
	public void testTopicMatchesSingleLevelWildcard() throws Exception {
		var config = MyConfig.create().build();
		var handler = new Mqtt3ConnectionHandler(config, "localhost", 1883, "client", false, MqttVersion.V3_1_1);

		assertTrue(this.invokeTopicMatches(handler, "sensors/+/data", "sensors/temp/data"));
		assertTrue(this.invokeTopicMatches(handler, "sensors/+/data", "sensors/humidity/data"));
		assertFalse(this.invokeTopicMatches(handler, "sensors/+/data", "sensors/temp/value"));
		assertFalse(this.invokeTopicMatches(handler, "sensors/+/data", "devices/temp/data"));
	}

	@Test
	public void testTopicMatchesMultiLevelWildcard() throws Exception {
		var config = MyConfig.create().build();
		var handler = new Mqtt3ConnectionHandler(config, "localhost", 1883, "client", false, MqttVersion.V3_1_1);

		assertTrue(this.invokeTopicMatches(handler, "sensors/#", "sensors/temperature"));
		assertTrue(this.invokeTopicMatches(handler, "sensors/#", "sensors/temperature/value"));
		assertTrue(this.invokeTopicMatches(handler, "sensors/#", "sensors/a/b/c/d"));
		assertFalse(this.invokeTopicMatches(handler, "sensors/#", "devices/temperature"));
	}

	@Test
	public void testTopicMatchesCombinedWildcards() throws Exception {
		var config = MyConfig.create().build();
		var handler = new Mqtt3ConnectionHandler(config, "localhost", 1883, "client", false, MqttVersion.V3_1_1);

		assertTrue(this.invokeTopicMatches(handler, "+/sensors/#", "home/sensors/temp"));
		assertTrue(this.invokeTopicMatches(handler, "+/sensors/#", "office/sensors/temp/value"));
		assertFalse(this.invokeTopicMatches(handler, "+/sensors/#", "home/devices/temp"));
	}

	@Test
	public void testTopicMatchesEdgeCases() throws Exception {
		var config = MyConfig.create().build();
		var handler = new Mqtt3ConnectionHandler(config, "localhost", 1883, "client", false, MqttVersion.V3_1_1);

		// Empty or single segment
		assertTrue(this.invokeTopicMatches(handler, "topic", "topic"));
		assertFalse(this.invokeTopicMatches(handler, "topic", "other"));

		// Different length paths
		assertFalse(this.invokeTopicMatches(handler, "a/b", "a/b/c"));
		assertFalse(this.invokeTopicMatches(handler, "a/b/c", "a/b"));
	}

	/**
	 * Invokes the private topicMatches method using reflection.
	 *
	 * @param handler the handler instance
	 * @param filter  the topic filter
	 * @param topic   the topic to match
	 * @return true if topic matches filter
	 * @throws Exception on reflection error
	 */
	private boolean invokeTopicMatches(Mqtt3ConnectionHandler handler, String filter, String topic) throws Exception {
		Method method = Mqtt3ConnectionHandler.class.getDeclaredMethod("topicMatches", String.class, String.class);
		method.setAccessible(true);
		return (boolean) method.invoke(handler, filter, topic);
	}

}
