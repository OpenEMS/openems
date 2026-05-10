package io.openems.edge.controller.api.modbus.readwrite.tcp;

import static io.openems.common.test.TestUtils.createDummyClock;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyCycle;
import io.openems.edge.common.test.DummyMeta;
import io.openems.edge.controller.api.modbus.LogVerbosity;
import io.openems.edge.controller.api.modbus.ModbusApi;
import io.openems.edge.controller.api.modbus.MyTcpConfig;
import io.openems.edge.controller.test.ControllerTest;

public class ControllerApiModbusTcpReadWriteImplTest {

	@Test
	public void test() throws Exception {
		final var clock = createDummyClock();
		var sut = new ControllerApiModbusTcpReadWriteImpl(); //

		new ControllerTest(sut) //
				.addComponent(new DummyCycle(1000)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("metaComponent", new DummyMeta()) //
				.activate(MyTcpConfig.create(io.openems.edge.controller.api.modbus.readonly.tcp.Config.class) //
						.setId("ctrlApiModbusTcp0") //
						.setEnabled(true) // has to be enabled for resetting channel
						.setComponentIds() //
						.setMaxConcurrentConnections(5) //
						.setPort(12345) // random port not blocking 502
						.setApiTimeout(60) //
						.setLogVerbosity(LogVerbosity.NONE) //
						.build()) //
				.next(new TestCase() //
						.onAfterProcessImage(() -> sut.setProcessImageFault(clock)) //
						.output(ModbusApi.ChannelId.PROCESS_IMAGE_FAULT, true)) //
				.next(new TestCase() //
						.timeleap(clock, 20, ChronoUnit.SECONDS) //
						.onAfterProcessImage(() -> sut.resetProcessImageError(clock)) //
						.output(ModbusApi.ChannelId.PROCESS_IMAGE_FAULT, true)) //
				.next(new TestCase() //
						.timeleap(clock, 40, ChronoUnit.SECONDS) //
						// after one minute, PROCESS_IMAGE_FAULT is false again
						.output(ModbusApi.ChannelId.PROCESS_IMAGE_FAULT, false)) //
				.deactivate();

		assertNull(sut.debugLog());
	}

	@Test
	public void testTimedataChannels() {
		var controller = new ControllerApiModbusTcpReadWriteImpl(); //
		boolean channelNotFound = controller.channels().stream().noneMatch(//
				ch -> ch.channelId().id().equals("CumulatedActiveTime") //
						|| ch.channelId().id().equals("CumulatedInactiveTime")); //
		assertFalse(channelNotFound);
	}

	@Test
	public void testAddFalseComponents() {
		var controller = new ControllerApiModbusTcpReadWriteImpl(); //
		controller.addComponent(new DummyCycle(1000)); //
		controller.getComponentNoModbusApiFaultChannel().nextProcessImage(); //
		assertTrue(controller.getComponentNoModbusApiFault().get()); //
	}
}
