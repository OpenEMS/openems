package io.openems.edge.solaredge.ess;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.DIRECT_1_TO_1;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TestUtils;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.type.Phase.SingleOrAllPhase;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.solaredge.charger.SolarEdgeChargerImpl;
import io.openems.edge.solaredge.enums.ControlMode;

public class IgnoreMinPowerConverterTest {

	@Test
	public void test() throws Exception {
		var charger = new SolarEdgeChargerImpl();
		new ComponentTest(charger) //
		.addReference("cm", new DummyConfigurationAdmin()) //
		.addReference("essInverter", new SolarEdgeEssImpl())
		.activate(io.openems.edge.solaredge.charger.MyConfig.create() //
				.setId("charger0") //
				.setEssInverterId("ess0") //
				.build());
		
		var solarEdge = new SolarEdgeEssImpl();
		solarEdge.addCharger(charger);
		final var componentTest = new ComponentTest(solarEdge) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("sum", new DummySum()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(charger)
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setControlMode(ControlMode.REMOTE) //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setPhase(SingleOrAllPhase.L1) //
						.build()) //
				.next(new TestCase());
		
		final ElementToChannelConverter ignoreMinPower = IgnoreMinPowerConverter.from(solarEdge, DIRECT_1_TO_1);

		// Test no ignore on channel unavailability
		assertEquals((float) 10, ignoreMinPower.elementToChannel((float) 10));
		
		// Test no ignore while pv production is > 0
		TestUtils.withValue(charger, EssDcCharger.ChannelId.ACTUAL_POWER, 100); // pvProduction
		TestUtils.withValue(solarEdge, HybridEss.ChannelId.DC_DISCHARGE_POWER, 0);
		assertEquals((float) 10, ignoreMinPower.elementToChannel((float) 10));
		
		// Test no ignore while DC discharge power is > 0 
		TestUtils.withValue(charger, EssDcCharger.ChannelId.ACTUAL_POWER, 0); // pvProduction
		TestUtils.withValue(solarEdge, HybridEss.ChannelId.DC_DISCHARGE_POWER, 1000);
		assertEquals((float) 10, ignoreMinPower.elementToChannel((float) 10));
		
		// Test no ignore while DC discharge power is < 0 
		TestUtils.withValue(charger, EssDcCharger.ChannelId.ACTUAL_POWER, 0); // pvProduction
		TestUtils.withValue(solarEdge, HybridEss.ChannelId.DC_DISCHARGE_POWER, 1000);
		assertEquals((float) 10, ignoreMinPower.elementToChannel((float) 10));
		
		// Test ignore while pvProduction and DcDischargePower are zero and value between -50 and 50   
		TestUtils.withValue(charger, EssDcCharger.ChannelId.ACTUAL_POWER, 0); // pvProduction
		TestUtils.withValue(solarEdge, HybridEss.ChannelId.DC_DISCHARGE_POWER, 0);
		assertEquals(0, ignoreMinPower.elementToChannel((float) -25));
		
		// Test ignore while pvProduction and DcDischargePower are zero and value between -50 and 50   
		TestUtils.withValue(charger, EssDcCharger.ChannelId.ACTUAL_POWER, 0); // pvProduction
		TestUtils.withValue(solarEdge, HybridEss.ChannelId.DC_DISCHARGE_POWER, 0);
		assertEquals(0, ignoreMinPower.elementToChannel((float) 25));

		// Test no ignore while pvProduction and DcDischargePower are zero and value < -50  
		TestUtils.withValue(charger, EssDcCharger.ChannelId.ACTUAL_POWER, 0); // pvProduction
		TestUtils.withValue(solarEdge, HybridEss.ChannelId.DC_DISCHARGE_POWER, 0);
		assertEquals((float) -50, ignoreMinPower.elementToChannel((float) -50));

		// Test no ignore while pvProduction and DcDischargePower are zero and value < -50  
		TestUtils.withValue(charger, EssDcCharger.ChannelId.ACTUAL_POWER, 0); // pvProduction
		TestUtils.withValue(solarEdge, HybridEss.ChannelId.DC_DISCHARGE_POWER, 0);
		assertEquals((float) -51, ignoreMinPower.elementToChannel((float) -51));

		// Test no ignore while pvProduction and DcDischargePower are zero and value >= 50  
		TestUtils.withValue(charger, EssDcCharger.ChannelId.ACTUAL_POWER, 0); // pvProduction
		TestUtils.withValue(solarEdge, HybridEss.ChannelId.DC_DISCHARGE_POWER, 0);
		assertEquals((float) 50, ignoreMinPower.elementToChannel((float) 50));
		
		// Test no ignore while pvProduction and DcDischargePower are zero and value >= 50  
		TestUtils.withValue(charger, EssDcCharger.ChannelId.ACTUAL_POWER, 0); // pvProduction
		TestUtils.withValue(solarEdge, HybridEss.ChannelId.DC_DISCHARGE_POWER, 0);
		assertEquals((float) 51, ignoreMinPower.elementToChannel((float) 51));
		
		componentTest.deactivate();
	}
}
