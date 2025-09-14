package io.openems.edge.meter.hager.ecr380d;


import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

/**
 * minimal smoke test with {@link DummyModbusBridge}.
 */
public class HagerEcr380dMeterTest {

	private static final String CID = "meter0";
	private static final String MID = "modbus0";

	@Test
	public void testActivate() throws Exception {
		new ComponentTest(new HagerEcr380dMeterImpl())
			.addReference("cm", new io.openems.edge.common.test.DummyConfigurationAdmin())
			.addReference("setModbus", new DummyModbusBridge(MID))
			.activate(MyConfig.create() //
					.setId(CID)
					.setModbusId(MID)
					.setType(MeterType.GRID)
					.build()
			)
			.next(new TestCase())
			.deactivate();

		assertTrue(true);
	}
}
