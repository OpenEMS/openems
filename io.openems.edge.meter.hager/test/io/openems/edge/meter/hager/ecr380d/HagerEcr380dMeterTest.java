package io.openems.edge.meter.hager.ecr380d;


import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

/**
 * Minimaler Smoke-Test mit DummyModbusBridge.
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
//					new Config() {
//				@Override public Class<? extends java.lang.annotation.Annotation> annotationType(){ return HagerMeterImpl.Config.class; }
//				@Override public String id() { return CID; }
//				@Override public String alias() { return ""; }
//				@Override public boolean enabled() { return true; }
//				@Override public MeterType type() { return MeterType.GRID; }
//				@Override public String modbus_id() { return MID; }
//				@Override public int modbusUnitId() { return 1; }
//				@Override public String Modbus_target() { return "(enabled=true)"; }
//				@Override public String webconsole_configurationFactory_nameHint() { return "Meter Hager [{id}]"; }
//			})
			)
			.next(new TestCase())
			.deactivate();

		assertTrue(true);
	}
}
