package io.openems.edge.system.fenecon.masterbox2v0.ao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.openems.edge.io.api.AnalogOutput;
import org.junit.Test;

import io.openems.common.channel.Level;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.io.api.AnalogVoltageOutput;
import io.openems.edge.system.fenecon.masterbox2v0.DummyMasterBox2v0;

public class MasterBox2v0MeterImplTest {

	@Test
	public void testReadMapping() throws Exception {
		var ioc = new DummyMasterBox2v0("ioc0") //
				.withAnalogOutVoltage(5000) //
				.withAnalogOutControl(true);

		var ao = new IoMasterBox2v0AoImpl();

		new ComponentTest(ao).addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ioc", ioc) //
				.activate(MyConfig.create() //
						.setId("ao0") //
						.setIocId("ioc0") //
						.build()) //
				.next(new TestCase()) //
				.next(new TestCase());

		assertEquals(5000, (int) ao.getSetOutputVoltageChannel().value().get());
		assertEquals(true, ao.getAnalogOutControlChannel().value().get());
	}

	@Test
	public void testWriteMapping() throws Exception {
		var ioc = new DummyMasterBox2v0("ioc0");

		new ComponentTest(new IoMasterBox2v0AoImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ioc", ioc) //
				.activate(MyConfig.create() //
						.setId("ao0") //
						.setIocId("ioc0") //
						.build()) //
				.next(new TestCase() //
						.activateStrictMode() //
						.input(AnalogVoltageOutput.ChannelId.SET_OUTPUT_VOLTAGE, 5000) //
						.input(IoMasterBox2v0Ao.ChannelId.ANALOG_OUT_CONTROL, true) //
						.input(AnalogOutput.ChannelId.SET_OUTPUT_PERCENT, 50F) //
						.output(OpenemsComponent.ChannelId.STATE, Level.OK) //
						.output(AnalogVoltageOutput.ChannelId.SET_OUTPUT_VOLTAGE, 5000) //
						.output(IoMasterBox2v0Ao.ChannelId.ANALOG_OUT_CONTROL, true) //
						.output(AnalogOutput.ChannelId.SET_OUTPUT_PERCENT, 50F) //
						.output(IoMasterBox2v0Ao.ChannelId.DEBUG_ANALOG_OUT_CONTROL, true) //
						.output(AnalogVoltageOutput.ChannelId.DEBUG_SET_OUTPUT_VOLTAGE, 5000) //
						.output(AnalogOutput.ChannelId.DEBUG_SET_OUTPUT_PERCENT, 50F) //
				);

		assertEquals(5000, (int) ioc.getAnalogOutVoltageChannel().getNextWriteValue().get());
		assertEquals(true, ioc.getAnalogOutControlChannel().getNextWriteValue().get());
	}
}
