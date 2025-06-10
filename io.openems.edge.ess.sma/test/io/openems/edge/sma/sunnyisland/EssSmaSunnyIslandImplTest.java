package io.openems.edge.sma.sunnyisland;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.test.ManagedSymmetricEssTest;

public class EssSmaSunnyIslandImplTest {

	@Test
	public void test() throws Exception {
		new ManagedSymmetricEssTest(new EssSmaSunnyIslandImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setModbusId("modbus0") //
						.setPhase(Phase.L1) //
						.build()) //
		;
	}

}
