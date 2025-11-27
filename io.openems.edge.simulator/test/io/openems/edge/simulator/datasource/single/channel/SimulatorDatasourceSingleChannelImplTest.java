package io.openems.edge.simulator.datasource.single.channel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

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
	public void testGetValueReturnsDefaultWhenEmpty() throws Exception {
		var sut = new SimulatorDatasourceSingleChannelImpl();
		new ComponentTest(sut) //
				.addReference("componentManager", new DummyComponentManager(new TimeLeapClock())) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setTimeDelta(-1) //
						.build()) //
				.next(new TestCase()) //
		;

		// getValue() should return default value 0 when no value has been set
		Integer value = sut.getValue(OpenemsType.INTEGER, DATA);
		assertEquals(Integer.valueOf(0), value);
	}

	@Test
	public void testGetValueReturnsSetValue() throws Exception {
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

		// getValue() should return the value set via the channel
		Integer value = sut.getValue(OpenemsType.INTEGER, DATA);
		assertEquals(Integer.valueOf(6000), value);
	}

	@Test
	public void testGetValueUpdatesOnNewValue() throws Exception {
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

	@Test
	public void testGetValuesReturnsDefaultWhenEmpty() throws Exception {
		var sut = new SimulatorDatasourceSingleChannelImpl();
		new ComponentTest(sut) //
				.addReference("componentManager", new DummyComponentManager(new TimeLeapClock())) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setTimeDelta(-1) //
						.build()) //
				.next(new TestCase()) //
		;

		// getValues() should return default value 0 when no value has been set
		List<Integer> values = sut.getValues(OpenemsType.INTEGER, DATA);
		assertEquals(1, values.size());
		assertEquals(Integer.valueOf(0), values.get(0));
	}

	@Test
	public void testGetValuesReturnsSetValue() throws Exception {
		var sut = new SimulatorDatasourceSingleChannelImpl();
		new ComponentTest(sut) //
				.addReference("componentManager", new DummyComponentManager(new TimeLeapClock())) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setTimeDelta(-1) //
						.build()) //
				.next(new TestCase()) //
		;

		// Set the value via the WriteChannel
		sut.getDataWriteChannel().setNextWriteValue(5000);

		// getValues() should return the value set via the channel
		List<Integer> values = sut.getValues(OpenemsType.INTEGER, DATA);
		assertEquals(1, values.size());
		assertEquals(Integer.valueOf(5000), values.get(0));
	}

	@Test
	public void testGetKeysReturnsEmptySet() throws Exception {
		var sut = new SimulatorDatasourceSingleChannelImpl();
		new ComponentTest(sut) //
				.addReference("componentManager", new DummyComponentManager(new TimeLeapClock())) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setTimeDelta(-1) //
						.build()) //
				.next(new TestCase()) //
		;

		// getKeys() should return empty set
		assertTrue(sut.getKeys().isEmpty());
	}

	@Test
	public void testGetTimeDelta() throws Exception {
		var sut = new SimulatorDatasourceSingleChannelImpl();
		new ComponentTest(sut) //
				.addReference("componentManager", new DummyComponentManager(new TimeLeapClock())) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setTimeDelta(60) //
						.build()) //
				.next(new TestCase()) //
		;

		// getTimeDelta() should return configured value
		assertEquals(60, sut.getTimeDelta());
	}

	@Test
	public void testNullValueIsIgnored() throws Exception {
		var sut = new SimulatorDatasourceSingleChannelImpl();
		new ComponentTest(sut) //
				.addReference("componentManager", new DummyComponentManager(new TimeLeapClock())) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setTimeDelta(-1) //
						.build()) //
				.next(new TestCase()) //
		;

		// Set a value first
		sut.getDataWriteChannel().setNextWriteValue(3000);
		assertEquals(Integer.valueOf(3000), sut.getValue(OpenemsType.INTEGER, DATA));

		// Setting null should not change the value (null is ignored in callback)
		sut.getDataWriteChannel().setNextWriteValue(null);
		assertEquals(Integer.valueOf(3000), sut.getValue(OpenemsType.INTEGER, DATA));
	}

	@Test
	public void testGetDataChannel() throws Exception {
		var sut = new SimulatorDatasourceSingleChannelImpl();
		new ComponentTest(sut) //
				.addReference("componentManager", new DummyComponentManager(new TimeLeapClock())) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setTimeDelta(-1) //
						.build()) //
				.next(new TestCase()) //
		;

		// Verify channel accessors work
		var readChannel = sut.getDataChannel();
		var writeChannel = sut.getDataWriteChannel();

		assertEquals(readChannel.channelId(), writeChannel.channelId());
		assertEquals("Data", readChannel.channelId().id());
	}

}
