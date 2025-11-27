package io.openems.edge.simulator.datasource.single.channel;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;

public class SimulatorDatasourceSingleChannelImplTest {

	private static final String COMPONENT_ID = "datasource0";
	private static final ChannelAddress DATA = new ChannelAddress(COMPONENT_ID, "Data");

	@Test
	public void test() throws Exception {
		new ComponentTest(new SimulatorDatasourceSingleChannelImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setTimeDelta(-1) //
						.build()) //
				.next(new TestCase()) //
		;
	}

	@Test
	public void testGetDataReturnsDefaultValueWhenEmpty() throws Exception {
		var sut = new SimulatorDatasourceSingleChannelImpl();
		new ComponentTest(sut) //
				.addReference("componentManager", new DummyComponentManager(new TimeLeapClock())) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setTimeDelta(-1) //
						.build()) //
				.next(new TestCase()) //
		;

		// getData() should return container with default value 0 when no value has been set
		Integer value = sut.getValue(OpenemsType.INTEGER, DATA);
		assertEquals(Integer.valueOf(0), value);
	}

	@Test
	public void testGetDataReturnsSetValue() throws Exception {
		var sut = new SimulatorDatasourceSingleChannelImpl();
		new ComponentTest(sut) //
				.addReference("componentManager", new DummyComponentManager(new TimeLeapClock())) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setTimeDelta(-1) //
						.build()) //
				.next(new TestCase()) //
		;

		// Directly set the value via the WriteChannel
		sut.getDataWriteChannel().setNextWriteValue(6000);

		// getData() should return the value set via the channel
		Integer value = sut.getValue(OpenemsType.INTEGER, DATA);
		assertEquals(Integer.valueOf(6000), value);
	}

	@Test
	public void testGetDataUpdatesOnNewValue() throws Exception {
		var sut = new SimulatorDatasourceSingleChannelImpl();
		new ComponentTest(sut) //
				.addReference("componentManager", new DummyComponentManager(new TimeLeapClock())) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setTimeDelta(-1) //
						.build()) //
				.next(new TestCase()) //
		;

		// Set initial value
		sut.getDataWriteChannel().setNextWriteValue(1000);
		assertEquals(Integer.valueOf(1000), sut.getValue(OpenemsType.INTEGER, DATA));

		// Update to new value
		sut.getDataWriteChannel().setNextWriteValue(2000);
		assertEquals(Integer.valueOf(2000), sut.getValue(OpenemsType.INTEGER, DATA));
	}

}
