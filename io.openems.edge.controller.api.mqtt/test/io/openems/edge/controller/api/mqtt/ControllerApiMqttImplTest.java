package io.openems.edge.controller.api.mqtt;

import static io.openems.edge.controller.api.mqtt.ControllerApiMqttImpl.createTopicPrefix;
import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.Test;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;

public class ControllerApiMqttImplTest {

	private static final String CTRL_ID = "ctrl0";

	@Test
	public void test() throws Exception {
		final var clock = new TimeLeapClock(Instant.ofEpochSecond(1577836800L) /* starts at 1. January 2020 00:00:00 */,
				ZoneOffset.UTC);
		new ComponentTest(new ControllerApiMqttImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addComponent(new DummySum()) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setClientId("edge0") //
						.setTopicPrefix("") //
						.setUsername("guest") //
						.setPassword("guest") //
						.setUri("ws://localhost:1883") //
						.setPersistencePriority(PersistencePriority.VERY_LOW) //
						.setDebugMode(true) //
						.setCertPem("") //
						.setPrivateKeyPem("") //
						.setTrustStorePath("") //
						.build());
	}

	@Test
	public void testCreateTopicPrefix() throws Exception {
		assertEquals("foo/bar/edge/edge0/", createTopicPrefix(MyConfig.create() //
				.setClientId("edge0") //
				.setTopicPrefix("foo/bar") //
				.build()));
		assertEquals("edge/edge0/", createTopicPrefix(MyConfig.create() //
				.setClientId("edge0") //
				.setTopicPrefix("") //
				.build()));
		assertEquals("edge/edge0/", createTopicPrefix(MyConfig.create() //
				.setClientId("edge0") //
				.setTopicPrefix(null) //
				.build()));
	}
}