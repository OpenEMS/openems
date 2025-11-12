package io.openems.edge.sma.ess.sunnyisland;

import static io.openems.edge.common.type.Phase.SingleOrAllPhase.L1;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.ess.api.SymmetricEss;
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
						.setPhase(L1) //
						.setCapacity(10_000) //
						.build()) //
				.next(new TestCase() //
						.output(SymmetricEss.ChannelId.CAPACITY, 10_000)) //
				.deactivate();
	}

}
