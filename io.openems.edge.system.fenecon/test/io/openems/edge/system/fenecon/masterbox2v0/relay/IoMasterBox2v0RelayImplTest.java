package io.openems.edge.system.fenecon.masterbox2v0.relay;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.openems.common.channel.Level;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.system.fenecon.masterbox2v0.DummyMasterBox2v0;

public class IoMasterBox2v0RelayImplTest {

	@Test
	public void testReadMapping() throws Exception {
		var ioc = new DummyMasterBox2v0("ioc0") //
				.withRelay1(true) //
				.withRelay2(true) //
				.withRelay3(true) //
				.withRelay4(true) //
				.withRelay5(true) //
				.withRelay6(true);

		var relay = new IoMasterBox2v0RelayImpl();

		new ComponentTest(relay) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ioc", ioc) //
				.activate(MyConfig.create() //
						.setId("io0") //
						.setIocId("ioc0") //
						.build()) //
				.next(new TestCase()) //
				.next(new TestCase());

		assertEquals(true, relay.getRelay1Channel().value().get());
		assertEquals(true, relay.getRelay2Channel().value().get());
		assertEquals(true, relay.getRelay3Channel().value().get());
		assertEquals(true, relay.getRelay4Channel().value().get());
		assertEquals(true, relay.getRelay5Channel().value().get());
		assertEquals(true, relay.getRelay6Channel().value().get());
	}

	@Test
	public void testWriteMapping() throws Exception {
		var ioc = new DummyMasterBox2v0("ioc0");

		new ComponentTest(new IoMasterBox2v0RelayImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ioc", ioc) //
				.activate(MyConfig.create() //
						.setId("io0") //
						.setIocId("ioc0") //
						.build()) //
				.next(new TestCase() //
						.activateStrictMode() //
						.input(IoMasterBox2v0Relay.ChannelId.RELAY_1, true) //
						.input(IoMasterBox2v0Relay.ChannelId.RELAY_2, true) //
						.input(IoMasterBox2v0Relay.ChannelId.RELAY_3, true) //
						.input(IoMasterBox2v0Relay.ChannelId.RELAY_4, true) //
						.input(IoMasterBox2v0Relay.ChannelId.RELAY_5, true) //
						.input(IoMasterBox2v0Relay.ChannelId.RELAY_6, true) //
						.output(OpenemsComponent.ChannelId.STATE, Level.OK) //
						.output(IoMasterBox2v0Relay.ChannelId.RELAY_1, true) //
						.output(IoMasterBox2v0Relay.ChannelId.RELAY_2, true) //
						.output(IoMasterBox2v0Relay.ChannelId.RELAY_3, true) //
						.output(IoMasterBox2v0Relay.ChannelId.RELAY_4, true) //
						.output(IoMasterBox2v0Relay.ChannelId.RELAY_5, true) //
						.output(IoMasterBox2v0Relay.ChannelId.RELAY_6, true) //
						.output(IoMasterBox2v0Relay.ChannelId.DEBUG_RELAY_1, true) //
						.output(IoMasterBox2v0Relay.ChannelId.DEBUG_RELAY_2, true) //
						.output(IoMasterBox2v0Relay.ChannelId.DEBUG_RELAY_3, true) //
						.output(IoMasterBox2v0Relay.ChannelId.DEBUG_RELAY_4, true) //
						.output(IoMasterBox2v0Relay.ChannelId.DEBUG_RELAY_5, true) //
						.output(IoMasterBox2v0Relay.ChannelId.DEBUG_RELAY_6, true) //
				);

		assertEquals(true, ioc.getRelay1Channel().getNextWriteValue().get());
		assertEquals(true, ioc.getRelay2Channel().getNextWriteValue().get());
		assertEquals(true, ioc.getRelay3Channel().getNextWriteValue().get());
		assertEquals(true, ioc.getRelay4Channel().getNextWriteValue().get());
		assertEquals(true, ioc.getRelay5Channel().getNextWriteValue().get());
		assertEquals(true, ioc.getRelay6Channel().getNextWriteValue().get());
	}
}
