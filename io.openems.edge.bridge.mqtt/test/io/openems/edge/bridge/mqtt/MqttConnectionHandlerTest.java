package io.openems.edge.bridge.mqtt;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.edge.bridge.mqtt.api.QoS;

/**
 * Tests for {@link MqttConnectionHandler} utility methods.
 */
public class MqttConnectionHandlerTest {

	@Test
	public void testToPahoQosAtMostOnce() {
		assertEquals(0, MqttConnectionHandler.toPahoQos(QoS.AT_MOST_ONCE));
	}

	@Test
	public void testToPahoQosAtLeastOnce() {
		assertEquals(1, MqttConnectionHandler.toPahoQos(QoS.AT_LEAST_ONCE));
	}

	@Test
	public void testToPahoQosExactlyOnce() {
		assertEquals(2, MqttConnectionHandler.toPahoQos(QoS.EXACTLY_ONCE));
	}

	@Test
	public void testFromPahoQosZero() {
		assertEquals(QoS.AT_MOST_ONCE, MqttConnectionHandler.fromPahoQos(0));
	}

	@Test
	public void testFromPahoQosOne() {
		assertEquals(QoS.AT_LEAST_ONCE, MqttConnectionHandler.fromPahoQos(1));
	}

	@Test
	public void testFromPahoQosTwo() {
		assertEquals(QoS.EXACTLY_ONCE, MqttConnectionHandler.fromPahoQos(2));
	}

	@Test
	public void testFromPahoQosInvalidDefaultsToAtMostOnce() {
		assertEquals(QoS.AT_MOST_ONCE, MqttConnectionHandler.fromPahoQos(-1));
		assertEquals(QoS.AT_MOST_ONCE, MqttConnectionHandler.fromPahoQos(3));
		assertEquals(QoS.AT_MOST_ONCE, MqttConnectionHandler.fromPahoQos(100));
	}

	@Test
	public void testQosRoundTrip() {
		for (QoS qos : QoS.values()) {
			int pahoQos = MqttConnectionHandler.toPahoQos(qos);
			QoS result = MqttConnectionHandler.fromPahoQos(pahoQos);
			assertEquals(qos, result);
		}
	}

}
