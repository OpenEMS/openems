package io.openems.edge.controller.api.modbus.readwrite;

import static io.openems.edge.controller.api.modbus.readwrite.ControllerApiModbusTcpReadWriteImpl.getChannelNameCamel;
import static io.openems.edge.controller.api.modbus.readwrite.ControllerApiModbusTcpReadWriteImpl.getChannelNameUpper;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.DummyCycle;
import io.openems.edge.controller.api.modbus.AbstractModbusTcpApi;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.api.ManagedSymmetricEss;

public class ControllerApiModbusTcpReadWriteImplTest {

	private static final String CTRL_ID = "ctrl0";

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerApiModbusTcpReadWriteImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEnabled(false) // do not actually start server
						.setComponentIds() //
						.setMaxConcurrentConnections(5) //
						.setPort(AbstractModbusTcpApi.DEFAULT_PORT) //
						.setApiTimeout(60) //
						.build()) //
				.next(new TestCase()) //
		;
	}

	@Test
	public void testTimedataChannels() throws Exception {
		var controller = new ControllerApiModbusTcpReadWriteImpl(); //
		boolean channelNotFound = controller.channels().stream().noneMatch(//
				ch -> ch.channelId().id().equals("CumulatedActiveTime") //
						|| ch.channelId().id().equals("CumulatedInactiveTime")); //
		assertFalse(channelNotFound);
	}

	@Test
	public void testAddFalseComponents() throws Exception {
		var controller = new ControllerApiModbusTcpReadWriteImpl(); //
		controller.addComponent(new DummyCycle(1000)); //
		controller.getComponentNoModbusApiFaultChannel().nextProcessImage(); //
		assertTrue(controller.getComponentNoModbusApiFault().get()); //
	}

	@Test
	public void testGetChannelNameUpper() {
		assertEquals("ESS0_SET_ACTIVE_POWER_EQUALS",
				getChannelNameUpper("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS));
		assertEquals("ESS0_SET_ACTIVE_POWER_EQUALS",
				getChannelNameUpper("Ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS));
	}

	@Test
	public void testGetChannelNameCamel() {
		assertEquals("Ess0SetActivePowerEquals",
				getChannelNameCamel("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS));
		assertEquals("Ess0SetActivePowerEquals",
				getChannelNameCamel("Ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS));
	}
}
