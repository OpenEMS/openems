package io.openems.edge.controller.api.modbus.readwrite;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.controller.api.modbus.readwrite.AbstractModbusReadWriteApi.getChannelNameCamel;
import static io.openems.edge.controller.api.modbus.readwrite.AbstractModbusReadWriteApi.getChannelNameUpper;
import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.utils.DictionaryUtils;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyCycle;
import io.openems.edge.common.test.DummyMeta;
import io.openems.edge.controller.api.common.WriteObject;
import io.openems.edge.controller.api.common.WritePojo;
import io.openems.edge.controller.api.modbus.LogVerbosity;
import io.openems.edge.controller.api.modbus.MyTcpConfig;
import io.openems.edge.controller.api.modbus.readwrite.tcp.ControllerApiModbusTcpReadWriteImpl;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

public class AbstractModbusReadWriteApiTest {

	@Test
	public void test() throws Exception {
		final var clock = createDummyClock();
		final var cm = new DummyConfigurationAdmin();
		final var ess0 = new DummyManagedSymmetricEss("ess0");
		final var ess1 = new DummyManagedSymmetricEss("ess1");
		final var sut = (AbstractModbusReadWriteApi) new ControllerApiModbusTcpReadWriteImpl(); //

		final var test = new ControllerTest(sut) //
				.addComponent(new DummyCycle(1000)) //
				.addReference("cm", cm) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("metaComponent", new DummyMeta()) //
				.addReference("addComponent", ess0) //
				.addReference("addComponent", ess1) //
				.activate(MyTcpConfig.create(io.openems.edge.controller.api.modbus.readonly.tcp.Config.class) //
						.setId("ctrlApiModbusTcp0") //
						.setEnabled(true) // has to be enabled for resetting channel
						.setComponentIds("ess0") //
						.setMaxConcurrentConnections(5) //
						.setPort(12345) // random port not blocking 502
						.setApiTimeout(60) //
						.setLogVerbosity(LogVerbosity.NONE) //
						.build());

		// Initially Channel does not exist
		assertThrows(IllegalArgumentException.class, () -> sut.channel("Ess0SetActivePowerEquals"));
		{
			var c = cm.getConfiguration(sut.id()).getProperties();
			assertNull(DictionaryUtils.getAsString(c, "_lastChangeBy"));
			assertNull(DictionaryUtils.getAsString(c, "_lastChangeAt"));
			assertEquals(0, ((String[]) c.get("writeChannels")).length);
		}

		// Store data in channel and config properties
		handleWrite(sut, ess0, ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS, 123);
		test.next(new TestCase() //
				.output("Ess0SetActivePowerEquals", 123) //
				.output(AbstractModbusReadWriteApi.ChannelId.ESS0_ACTUAL_SET_ACTIVE_POWER_EQUALS, 123));
		{
			var c = cm.getConfiguration(sut.id()).getProperties();
			assertEquals("Internal ModbusReadWriteApi", DictionaryUtils.getAsString(c, "_lastChangeBy"));
			assertNotNull(DictionaryUtils.getAsString(c, "_lastChangeAt"));
			assertEquals("Ess0SetActivePowerEquals", ((String[]) c.get("writeChannels"))[0]);
		}

		// Remaining Channels
		handleWrite(sut, ess1, ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS, 456); // ignore ess1
		handleWrite(sut, ess0, ManagedSymmetricEss.ChannelId.SET_REACTIVE_POWER_EQUALS, 111);
		handleWrite(sut, ess0, ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_LESS_OR_EQUALS, 222);
		handleWrite(sut, ess0, ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_GREATER_OR_EQUALS, 333);
		handleWrite(sut, ess0, ManagedSymmetricEss.ChannelId.SET_REACTIVE_POWER_LESS_OR_EQUALS, 444);
		handleWrite(sut, ess0, ManagedSymmetricEss.ChannelId.SET_REACTIVE_POWER_GREATER_OR_EQUALS, 555);
		test.next(new TestCase() //
				.output("Ess0SetActivePowerEquals", 123) //
				.output(AbstractModbusReadWriteApi.ChannelId.ESS0_ACTUAL_SET_REACTIVE_POWER_EQUALS, 111) //
				.output(AbstractModbusReadWriteApi.ChannelId.ESS0_ACTUAL_SET_ACTIVE_POWER_LESS_OR_EQUALS, 222) //
				.output(AbstractModbusReadWriteApi.ChannelId.ESS0_ACTUAL_SET_ACTIVE_POWER_GREATER_OR_EQUALS, 333) //
				.output(AbstractModbusReadWriteApi.ChannelId.ESS0_ACTUAL_SET_REACTIVE_POWER_LESS_OR_EQUALS, 444) //
				.output(AbstractModbusReadWriteApi.ChannelId.ESS0_ACTUAL_SET_REACTIVE_POWER_GREATER_OR_EQUALS, 555));
	}

	private static void handleWrite(AbstractModbusReadWriteApi sut, OpenemsComponent component, ChannelId channelId,
			Object value) {
		var entry = Map.<WriteChannel<?>, WriteObject>of(component.channel(channelId), new WritePojo(value)).entrySet()
				.iterator().next();
		sut.handleWrites(entry);
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
