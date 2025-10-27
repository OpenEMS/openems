package io.openems.edge.controller.api.modbus.readwrite.tcp;

import static io.openems.edge.controller.api.modbus.readwrite.tcp.ControllerApiModbusTcpReadWriteImpl.getChannelNameCamel;
import static io.openems.edge.controller.api.modbus.readwrite.tcp.ControllerApiModbusTcpReadWriteImpl.getChannelNameUpper;
import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyCycle;
import io.openems.edge.common.test.DummyMeta;
import io.openems.edge.controller.api.modbus.ModbusApi;
import io.openems.edge.controller.test.ControllerTest;

public class ControllerApiModbusTcpReadWriteImplTest {

	private static final String CONTROLLER_ID = "ctrlApiModbusTcp0";

	private static final ChannelAddress PROCESS_IMAGE_FAULT = new ChannelAddress(CONTROLLER_ID,
			ModbusApi.ChannelId.PROCESS_IMAGE_FAULT.id());

	private TimeLeapClock clock = new TimeLeapClock(Instant.parse("2024-01-01T01:00:00.00Z"), ZoneOffset.UTC);

	@Test
	public void test() throws Exception {
		var sut = new ControllerApiModbusTcpReadWriteImpl(); //

		new ControllerTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addComponent(new DummyCycle(1000)) //
				.addReference("componentManager", new DummyComponentManager(this.clock)) //
				.addReference("metaComponent", new DummyMeta("_meta")) //
				.activate(MyConfig.create() //
						.setId(CONTROLLER_ID) //
						.setEnabled(true) // has to be enabled for resetting channel
						.setComponentIds() //
						.setMaxConcurrentConnections(5) //
						.setPort(123456) // random port not blocking 502
						.setApiTimeout(60) //
						.build()) //
				.next(new TestCase() //
						.onAfterProcessImage(() -> sut.setProcessImageFault(this.clock)) //
						.output(PROCESS_IMAGE_FAULT, true) //
				) //
				.next(new TestCase() //
						.timeleap(this.clock, 20, ChronoUnit.SECONDS) //
						.onAfterProcessImage(() -> sut.resetProcessImageError(this.clock))
						.output(PROCESS_IMAGE_FAULT, true) //
				) //
				.next(new TestCase() //
						.timeleap(this.clock, 40, ChronoUnit.SECONDS) //
						// after one minute, PROCESS_IMAGE_FAULT is false again
						.output(PROCESS_IMAGE_FAULT, false) //
				) //
				.deactivate();
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
		assertEquals("ESS0_SET_ACTIVE_POWER_EQUALS", getChannelNameUpper("ess0", SET_ACTIVE_POWER_EQUALS));
		assertEquals("ESS0_SET_ACTIVE_POWER_EQUALS", getChannelNameUpper("Ess0", SET_ACTIVE_POWER_EQUALS));
	}

	@Test
	public void testGetChannelNameCamel() {
		assertEquals("Ess0SetActivePowerEquals", getChannelNameCamel("ess0", SET_ACTIVE_POWER_EQUALS));
		assertEquals("Ess0SetActivePowerEquals", getChannelNameCamel("Ess0", SET_ACTIVE_POWER_EQUALS));
	}

}
