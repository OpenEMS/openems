package io.openems.edge.solaredge.charger;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.type.Phase.SingleOrAllPhase;
import io.openems.edge.solaredge.enums.ControlMode;
import io.openems.edge.solaredge.ess.SolarEdgeEssImpl;
import io.openems.edge.timedata.test.DummyTimedata;

public class SolarEdgeChargerImplTest {

	@Test
	public void test() throws Exception {
		var ess = new SolarEdgeEssImpl();
		new ComponentTest(ess) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(io.openems.edge.solaredge.ess.MyConfig.create() //
						.setId("ess0") //
						.setControlMode(ControlMode.SMART) //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setPhase(SingleOrAllPhase.L1) //
						.build());

		var charger = new SolarEdgeChargerImpl();
		new ComponentTest(charger) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("essInverter", ess) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.activate(MyConfig.create() //
						.setId("charger0") //
						.setEssInverterId("ess0") //
						.build())
				.next(new TestCase())
				.deactivate();
	}

	@Test
	public void testDebugLog() throws Exception {
		var charger = new SolarEdgeChargerImpl();
		assertNotNull(charger.debugLog());
	}

	@Test
	public void testChannelIds() {
		var channelIds = SolarEdgeCharger.ChannelId.values();
		for (var channelId : channelIds) {
			assertNotNull("ChannelId " + channelId.name() + " should have a doc", channelId.doc());
		}
	}
}
