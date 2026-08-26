package io.openems.edge.fronius.gen24.dccharger;

import org.junit.jupiter.api.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.fronius.enums.PvString;

public class FroniusGen24DcChargerImplTest {

	@Test
	void test() throws Exception {

		// DcCharger is now fully independent - no OSGi @Reference to the
		// BatteryInverter. It performs its own SunSpec discovery against the
		// same physical device (hence same Unit-ID as the inverter would use),
		// via its own Modbus bridge.
		new ComponentTest(new FroniusGen24DcChargerImpl()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("charger0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setPvString(PvString.ONE) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}
}
