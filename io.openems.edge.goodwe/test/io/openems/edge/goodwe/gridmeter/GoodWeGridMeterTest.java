package io.openems.edge.goodwe.gridmeter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.power.api.Phase;

public class GoodWeGridMeterTest {

	private static final String MODBUS_ID = "modbus0";

	private static final String METER_ID = "meter0";

	@Test
	public void test() throws Exception {
		final GoodWeGridMeter gridMeterComponent = new GoodWeGridMeterImpl();

		new ComponentTest(gridMeterComponent) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(METER_ID) //
						.setModbusId(MODBUS_ID) //
						.build());
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

		var noResult = GoodWeGridMeterImpl.getPhaseConnectionValue(Phase.L3, null);

		assertNull(noResult);
	}
}
