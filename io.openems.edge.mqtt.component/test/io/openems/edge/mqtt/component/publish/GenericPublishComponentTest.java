package io.openems.edge.mqtt.component.publish;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;

import org.junit.Test;

import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.mqtt.test.DummyMqttBridgeImpl;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.mqtt.component.enums.TimeIntervalSeconds;

public class GenericPublishComponentTest {

	private static final String BRIDGE_ID = "mqtt0";
	private static final String MQTT_COMPONENT_ID = "mqttComponent0";
	private static final String DUMMY_DEVICE_ID = "dummy0";

	@Test
	public void test() throws Exception {
		final var clock = new TimeLeapClock(Instant.ofEpochSecond(1577836800L), ZoneOffset.UTC);
		@SuppressWarnings("unused")
		var dummyCpm = new DummyComponentManager(clock);
		var sut = new GenericPublishImpl();
		var dummyComponent = new MyDummyOpenEmsComponent(DUMMY_DEVICE_ID);
		var cNames = new ArrayList<String>();
		dummyComponent.channels().forEach(channel -> cNames.add(channel.channelId().id()));

		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setMqtt", new DummyMqttBridgeImpl(BRIDGE_ID)) //
				.addReference("setReferencedComponent", dummyComponent) //
				// .addComponent(dummyCpm)
				.activate(MyConfigGenericPublish //
						.create() //
						.setId(MQTT_COMPONENT_ID) //
						.setChannels(cNames.toArray(new String[0])) //
						.setReferencedComponentId(DUMMY_DEVICE_ID) //
						.setMqttId(BRIDGE_ID) //
						.setPubInterval(TimeIntervalSeconds.CYCLE) //
						.setTopic("test") //
						.setKeyToChanel(new String[] { "TEST=Dummy" })//
						.build() //
				)

		;
		//
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
