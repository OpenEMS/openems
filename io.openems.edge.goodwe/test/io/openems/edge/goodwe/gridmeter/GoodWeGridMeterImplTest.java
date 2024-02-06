package io.openems.edge.goodwe.gridmeter;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.power.api.Phase;

public class GoodWeGridMeterImplTest {

	private static final String MODBUS_ID = "modbus0";

	private static final String METER_ID = "meter0";

	private static final ChannelAddress METER_CON_CORRECTLY_L1 = new ChannelAddress(METER_ID,
			GoodWeGridMeter.ChannelId.METER_CON_CORRECTLY_L1.id());
	private static final ChannelAddress METER_CON_INCORRECTLY_L1 = new ChannelAddress(METER_ID,
			GoodWeGridMeter.ChannelId.METER_CON_INCORRECTLY_L1.id());
	private static final ChannelAddress METER_CON_REVERSE_L1 = new ChannelAddress(METER_ID,
			GoodWeGridMeter.ChannelId.METER_CON_REVERSE_L1.id());

	@Test
	public void test() throws Exception {
		final var sut = new GoodWeGridMeterImpl();

		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(METER_ID) //
						.setModbusId(MODBUS_ID) //
						.build()) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> sut.convertMeterConnectStatus(null))
						.output(METER_CON_CORRECTLY_L1, false) //
						.output(METER_CON_INCORRECTLY_L1, false) //
						.output(METER_CON_REVERSE_L1, false)) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> sut.convertMeterConnectStatus(1))
						.output(METER_CON_CORRECTLY_L1, true) //
						.output(METER_CON_INCORRECTLY_L1, false) //
						.output(METER_CON_REVERSE_L1, false)) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> sut.convertMeterConnectStatus(2))
						.output(METER_CON_CORRECTLY_L1, false) //
						.output(METER_CON_INCORRECTLY_L1, false) //
						.output(METER_CON_REVERSE_L1, true)) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> sut.convertMeterConnectStatus(4))
						.output(METER_CON_CORRECTLY_L1, false) //
						.output(METER_CON_INCORRECTLY_L1, true) //
						.output(METER_CON_REVERSE_L1, false));
	}

	@Test
	public void testMeterConnectStateConverter() throws Exception {

		var l1Result = GoodWeGridMeterImpl.getPhaseConnectionValue(Phase.L1, 0x0124);
		var l2Result = GoodWeGridMeterImpl.getPhaseConnectionValue(Phase.L2, 0x0124);
		var l3Result = GoodWeGridMeterImpl.getPhaseConnectionValue(Phase.L3, 0x0124);

		assertEquals(4, (int) l1Result);
		assertEquals(2, (int) l2Result);
		assertEquals(1, (int) l3Result);

		l1Result = GoodWeGridMeterImpl.getPhaseConnectionValue(Phase.L1, 0x0524);
		l2Result = GoodWeGridMeterImpl.getPhaseConnectionValue(Phase.L2, 0x0462);
		l3Result = GoodWeGridMeterImpl.getPhaseConnectionValue(Phase.L3, 0x1647);

		assertEquals(4, (int) l1Result);
		assertEquals(6, (int) l2Result);
		assertEquals(6, (int) l3Result);

		var l1NoResult = GoodWeGridMeterImpl.getPhaseConnectionValue(Phase.L1, 0x000);
		var l2NoResult = GoodWeGridMeterImpl.getPhaseConnectionValue(Phase.L2, 0x000);
		var l3NoResult = GoodWeGridMeterImpl.getPhaseConnectionValue(Phase.L3, 0x000);

		assertEquals(0, (int) l1NoResult);
		assertEquals(0, (int) l2NoResult);
		assertEquals(0, (int) l3NoResult);

		var noResult = GoodWeGridMeterImpl.getPhaseConnectionValue(Phase.L3, 0x000);

		assert noResult == 0x000;
	}
}
