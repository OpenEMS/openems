package io.openems.edge.fronius.gen24.batteryinverter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.fronius.enums.PvString;
import io.openems.edge.fronius.gen24.dccharger.FroniusGen24DcChargerImpl;

public class BatteryInverterFroniusGen24ImplTest {

	@Test
	void test() throws Exception {
		new ComponentTest(new BatteryInverterFroniusGen24Impl()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("batteryInverter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setControlMode(ControlMode.INTERNAL) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

	/**
	 * Verifies that {@code getDcPvPower()} sums the {@code ACTUAL_POWER} of all
	 * registered
	 * {@link io.openems.edge.fronius.gen24.dccharger.FroniusGen24DcCharger}s -
	 * matching the GoodWe/FENECON Commercial40 pattern - instead of reading SunSpec
	 * Module 1/2 registers directly. Chargers are registered by calling
	 * addCharger() directly (as OSGi's dynamic Reference would), since Chargers
	 * hold no reference back to the BatteryInverter.
	 */
	@Test
	void testGetDcPvPowerSumsRegisteredChargers() throws Exception {
		var inverter = new BatteryInverterFroniusGen24Impl();
		final ComponentTest inverterTest = new ComponentTest(inverter) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("batteryInverter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setControlMode(ControlMode.INTERNAL) //
						.build());

		// No Chargers registered yet -> no PV power known
		assertNull(inverter.getDcPvPower());

		var charger1 = new FroniusGen24DcChargerImpl();
		final ComponentTest charger1Test = new ComponentTest(charger1) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(io.openems.edge.fronius.gen24.dccharger.MyConfig.create() //
						.setId("charger0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setPvString(PvString.ONE) //
						.build());
		charger1._setActualPower(500);
		// _setActualPower() only sets 'nextValue'; a cycle is needed to shift it
		// into 'value', which is what getActualPower()/getDcPvPower() read.
		charger1Test.next(new TestCase());

		var charger2 = new FroniusGen24DcChargerImpl();
		final ComponentTest charger2Test = new ComponentTest(charger2) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(io.openems.edge.fronius.gen24.dccharger.MyConfig.create() //
						.setId("charger1") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setPvString(PvString.TWO) //
						.build());
		charger2._setActualPower(300);
		charger2Test.next(new TestCase());

		// Register both Chargers, as the OSGi dynamic Reference would
		inverter.addCharger(charger1);
		inverter.addCharger(charger2);

		assertEquals(Integer.valueOf(800), inverter.getDcPvPower());

		// Unregistering a Charger removes its contribution
		inverter.removeCharger(charger2);
		assertEquals(Integer.valueOf(500), inverter.getDcPvPower());

		charger1Test.deactivate();
		charger2Test.deactivate();
		inverterTest.deactivate();
	}
}
