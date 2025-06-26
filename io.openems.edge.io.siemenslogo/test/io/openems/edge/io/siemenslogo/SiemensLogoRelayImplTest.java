package io.openems.edge.io.siemenslogo;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.channel.AccessMode;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class SiemensLogoRelayImplTest {

	@Test
	public void test() throws Exception {
		var sut = new SiemensLogoRelayImpl();
		new ComponentTest(new SiemensLogoRelayImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("io0") //
						.setModbusId("modbus0") //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
		assertEquals("Output:????????|Input:????????????", sut.debugLog());

		var mst = sut.getModbusSlaveTable(AccessMode.READ_WRITE);
		assertEquals(180, mst.getLength());

		assertEquals(8, sut.digitalOutputChannels().length);
		assertEquals(12, sut.digitalInputChannels().length);
	}

}
