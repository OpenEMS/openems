package io.openems.edge.bridge.mqtt;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingRunnable;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.mqtt.api.BridgeMqtt;
import io.openems.edge.bridge.mqtt.api.LogVerbosity;
import io.openems.edge.bridge.mqtt.api.MqttProtocol;
import io.openems.edge.bridge.mqtt.api.Topic;
import io.openems.edge.bridge.mqtt.api.payloads.GenericPayloadImpl;
import io.openems.edge.bridge.mqtt.api.task.MqttPublishTaskImpl;
import io.openems.edge.bridge.mqtt.api.task.MqttSubscribeTaskImpl;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractComponentTest;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyCycle;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.core.timer.DummyTimerManager;

public class BridgeMqttImplTest {

	private static final String MQTT_ID = "mqtt0";

	private static final String MQTT_COMPONENT_DEVICE = "mqttdevice0";

	private static final String DUMMY_DEVICE_ID = "dummy0";
	private static final int CYCLE_TIME = 100;

	@Test
	public void test() throws Exception {
		final ThrowingRunnable<Exception> sleep = () -> Thread.sleep(CYCLE_TIME);
		final var clock = new TimeLeapClock(Instant.ofEpochSecond(1577836800L), ZoneOffset.UTC);
		var dummyCpm = new DummyComponentManager(clock);
		var dummyComponent = new MyDummyOpenEmsComponent(DUMMY_DEVICE_ID);
		dummyCpm.addComponent(dummyComponent);
		var sut = new MqttBridgeImpl();
		var test = new ComponentTest(sut) //

				.addReference("cpm", dummyCpm) //
				.addReference("cycle", new DummyCycle(CYCLE_TIME)) //
				.addReference("tm", new DummyTimerManager(dummyCpm)) //
				.activate(MyConfigMqttBridge.create() //
						.setId(MQTT_ID) //
						.setPassword("guest") //
						.setUser("guest") //
						.setBrokerUrl("tcp://localhost:1883") //
						.setLogVerbosity(LogVerbosity.NONE) //
						.build());
		var device = new MyMqttComponent(MQTT_COMPONENT_DEVICE, sut, dummyComponent);
		test //
				.addComponent(device) //
				.next(new AbstractComponentTest.TestCase() //
						.onAfterProcessImage(sleep)); //

	}

	private static class MyMqttComponent extends DummyMqttComponent {

		public MyMqttComponent(String id, BridgeMqtt bridge, OpenemsComponent referenceComponent)
				throws OpenemsException {
			super(id, bridge, referenceComponent);
		}

		@Override
		protected MqttProtocol defineMqttProtocol() throws OpenemsException {
			var config = new ArrayList<String>();
			var config2 = new ArrayList<String>();
			config.add("TEST=" + MyDummyOpenEmsComponent.ChannelId.DUMMY.id());
			config2.add("TEST=" + MyDummyOpenEmsComponent.ChannelId.DUMMY_2.id());
			return new MqttProtocol(this, new MqttPublishTaskImpl(new Topic("test", //
					0, //
					new GenericPayloadImpl(super.createMap(config), //
							"test0") //
			), 0), new MqttSubscribeTaskImpl(new Topic("test2", //
					new GenericPayloadImpl(super.createMap(config2), ""))));
		}
	}

	private static class MyDummyOpenEmsComponent extends AbstractOpenemsComponent {
		public MyDummyOpenEmsComponent(String id) {
			super(OpenemsComponent.ChannelId.values(), //
					ChannelId.values());
			this.channels().forEach(Channel::nextProcessImage);
			super.activate(null, id, "", true);
		}

		public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
			DUMMY(Doc.of(OpenemsType.INTEGER)), //
			DUMMY_2(Doc.of(OpenemsType.INTEGER)) //
			;

			private final Doc doc;

			private ChannelId(Doc doc) {
				this.doc = doc;
			}

			@Override
			public Doc doc() {
				return this.doc;
			}
		}
	}

}
