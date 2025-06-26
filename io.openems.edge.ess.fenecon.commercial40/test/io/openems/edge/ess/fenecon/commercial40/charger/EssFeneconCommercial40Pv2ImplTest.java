package io.openems.edge.ess.fenecon.commercial40.charger;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.fenecon.commercial40.EssFeneconCommercial40Impl;
import io.openems.edge.ess.test.ManagedSymmetricEssTest;

public class EssFeneconCommercial40Pv2ImplTest {

	@Test
	public void test() throws Exception {
		var ess = new EssFeneconCommercial40Impl();
		new ManagedSymmetricEssTest(ess) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(io.openems.edge.ess.fenecon.commercial40.MyConfig.create() //
						.setId("ess0") //
						.setModbusId("modbus0") //
						.setSurplusFeedInSocLimit(90) //
						.setSurplusFeedInAllowedChargePowerLimit(-8000) //
						.setSurplusFeedInIncreasePowerFactor(1.1) //
						.setSurplusFeedInMaxIncreasePowerFactor(2000) //
						.setSurplusFeedInPvLimitOnPowerDecreaseCausedByOvertemperature(5000) //
						.setSurplusFeedInOffTime("17:00:00") //
						.build());

		new ComponentTest(new EssFeneconCommercial40Pv2Impl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", ess) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfigPv2.create() //
						.setId("charger1") //
						.setModbusId("modbus0") //
						.setEssId("ess0") //
						.setMaxActualPower(0) //
						.build()) //
		;
	}
}
