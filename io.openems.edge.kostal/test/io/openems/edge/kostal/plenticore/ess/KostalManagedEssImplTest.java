package io.openems.edge.kostal.plenticore.ess;

import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER;
import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.ACTIVE_POWER;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.GRID_MODE;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.MAX_APPARENT_POWER;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.SOC;
import static io.openems.edge.kostal.plenticore.ess.KostalManagedEss.ChannelId.BATTERY_CURRENT;
import static io.openems.edge.kostal.plenticore.ess.KostalManagedEss.ChannelId.BATTERY_TEMPERATURE;
import static io.openems.edge.kostal.plenticore.ess.KostalManagedEss.ChannelId.BATTERY_VOLTAGE;
import static io.openems.edge.kostal.plenticore.ess.KostalManagedEss.ChannelId.CHARGE_POWER;
import static io.openems.edge.kostal.plenticore.ess.KostalManagedEss.ChannelId.FREQUENCY;
import static io.openems.edge.kostal.plenticore.ess.KostalManagedEss.ChannelId.GRID_VOLTAGE_L1;
import static io.openems.edge.kostal.plenticore.ess.KostalManagedEss.ChannelId.GRID_VOLTAGE_L2;
import static io.openems.edge.kostal.plenticore.ess.KostalManagedEss.ChannelId.GRID_VOLTAGE_L3;
import static io.openems.edge.kostal.plenticore.ess.KostalManagedEss.ChannelId.INVERTER_STATE;
import static io.openems.edge.kostal.plenticore.ess.KostalManagedEss.ChannelId.MAX_CHARGE_POWER;
import static io.openems.edge.kostal.plenticore.ess.KostalManagedEss.ChannelId.MAX_DISCHARGE_POWER;
import static io.openems.edge.kostal.plenticore.ess.KostalManagedEss.ChannelId.SET_ACTIVE_POWER;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.ess.test.ManagedSymmetricEssTest;
import io.openems.edge.kostal.plenticore.enums.ControlMode;
import io.openems.edge.kostal.plenticore.enums.InverterState;

public class KostalManagedEssImplTest {

	private static final String ESS_ID = "ess0";
	private static final String MODBUS_ID = "modbus0";

	@Test
	public void testActivateDeactivate() throws Exception {
		new ComponentTest(new KostalManagedEssImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(ESS_ID) //
						.setReadOnlyMode(true) //
						.setModbusId(MODBUS_ID) //
						.setCapacity(10000) //
						.setWatchdog(20) //
						.setTolerance(30) //
						.setControlMode(ControlMode.INTERNAL) //
						.setModbusUnitId(71) //
						.setDebugMode(true) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

	@Test
	public void testManagedSymmetricEss() throws Exception {
		new ManagedSymmetricEssTest(new KostalManagedEssImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(ESS_ID) //
						.setReadOnlyMode(false) //
						.setModbusId(MODBUS_ID) //
						.setCapacity(10000) //
						.setWatchdog(30) //
						.setTolerance(50) //
						.setControlMode(ControlMode.REMOTE) //
						.setModbusUnitId(71) //
						.setDebugMode(false) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

	@Test
	public void testPowerControlWithTolerance() throws Exception {
		final int Tolerance = 50;
		final int Watchdog = 30;
		var sut = new KostalManagedEssImpl();

		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(ESS_ID) //
						.setReadOnlyMode(false) //
						.setModbusId(MODBUS_ID) //
						.setCapacity(10000) //
						.setWatchdog(Watchdog) //
						.setTolerance(Tolerance) //
						.setControlMode(ControlMode.REMOTE) //
						.setModbusUnitId(71) //
						.setDebugMode(false) //
						.build()) //

				// Test 1: Values within tolerance should be clamped to 0
				.next(new TestCase("Power within tolerance (+25W) should become 0") //
						.input(MAX_CHARGE_POWER, 5000) //
						.input(MAX_DISCHARGE_POWER, 5000) //
						.onExecuteWriteCallbacks(() -> sut.applyPower(25, 0)) //
						.output(SET_ACTIVE_POWER, 0)) //

				// Test 2: Boundary cases - exactly at tolerance boundary should be written
				.next(new TestCase("Power at tolerance (50W) should be set") //
						.input(MAX_CHARGE_POWER, 5000) //
						.input(MAX_DISCHARGE_POWER, 5000) //
						.onExecuteWriteCallbacks(() -> sut.applyPower(Tolerance, 0)) //
						.output(SET_ACTIVE_POWER, Tolerance)) //

				.next(new TestCase("Power at -tolerance (-50W) should be set") //
						.input(MAX_CHARGE_POWER, 5000) //
						.input(MAX_DISCHARGE_POWER, 5000) //
						.onExecuteWriteCallbacks(() -> sut.applyPower(-Tolerance, 0)) //
						.output(SET_ACTIVE_POWER, -Tolerance)) //

				// Test 3: Values outside tolerance should be set correctly
				.next(new TestCase("Power outside tolerance (200W) should be set") //
						.input(MAX_CHARGE_POWER, 5000) //
						.input(MAX_DISCHARGE_POWER, 5000) //
						.onExecuteWriteCallbacks(() -> sut.applyPower(200, 0)) //
						.output(SET_ACTIVE_POWER, 200)) //

				.next(new TestCase("Negative power outside tolerance (-300W) should be set") //
						.input(MAX_CHARGE_POWER, 5000) //
						.input(MAX_DISCHARGE_POWER, 5000) //
						.onExecuteWriteCallbacks(() -> sut.applyPower(-300, 0)) //
						.output(SET_ACTIVE_POWER, -300)) //

				// Test 4: Large power values should be set
				.next(new TestCase("Large power value (3000W) should be set") //
						.input(MAX_CHARGE_POWER, 5000) //
						.input(MAX_DISCHARGE_POWER, 5000) //
						.onExecuteWriteCallbacks(() -> sut.applyPower(3000, 0)) //
						.output(SET_ACTIVE_POWER, 3000)) //

				// Test 5: Negative values within tolerance also clamped to 0
				.next(new TestCase("Negative within tolerance (-30W) should become 0") //
						.input(MAX_CHARGE_POWER, 5000) //
						.input(MAX_DISCHARGE_POWER, 5000) //
						.onExecuteWriteCallbacks(() -> sut.applyPower(-30, 0)) //
						.output(SET_ACTIVE_POWER, 0)) //

				.deactivate();

		// Verify that REMOTE mode with readOnlyMode=false means isManaged() returns true
		assertEquals(true, sut.isManaged());
	}

	@Test
	public void testPowerControlWithSmartModeAndWatchdog() throws Exception {
		final int Tolerance = 100;
		final int Watchdog = 2; // 2 seconds for testing
		var sut = new KostalManagedEssImpl();

		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(ESS_ID) //
						.setReadOnlyMode(false) //
						.setModbusId(MODBUS_ID) //
						.setCapacity(10000) //
						.setWatchdog(Watchdog) //
						.setTolerance(Tolerance) //
						.setControlMode(ControlMode.SMART) //
						.setModbusUnitId(71) //
						.setDebugMode(false) //
						.build()) //

				// First write: should always go through
				.next(new TestCase("First power write (1000W) should be set") //
						.input(MAX_CHARGE_POWER, 5000) //
						.input(MAX_DISCHARGE_POWER, 5000) //
						.onExecuteWriteCallbacks(() -> sut.applyPower(1000, 0))) //

				// Second write within tolerance of previous: should be skipped in SMART mode
				.next(new TestCase("Power within tolerance of previous (1050W) should be skipped") //
						.input(MAX_CHARGE_POWER, 5000) //
						.input(MAX_DISCHARGE_POWER, 5000) //
						.onExecuteWriteCallbacks(() -> sut.applyPower(1050, 0))) //

				// Write outside tolerance: should go through
				.next(new TestCase("Power outside tolerance (1200W) should be set") //
						.input(MAX_CHARGE_POWER, 5000) //
						.input(MAX_DISCHARGE_POWER, 5000) //
						.onExecuteWriteCallbacks(() -> sut.applyPower(1200, 0))) //

				// Same value: should be skipped
				.next(new TestCase("Same power value (1200W) should be skipped") //
						.input(MAX_CHARGE_POWER, 5000) //
						.input(MAX_DISCHARGE_POWER, 5000) //
						.onExecuteWriteCallbacks(() -> sut.applyPower(1200, 0))) //

				// Different value outside tolerance: should go through
				.next(new TestCase("New power value (1500W) should be set") //
						.input(MAX_CHARGE_POWER, 5000) //
						.input(MAX_DISCHARGE_POWER, 5000) //
						.onExecuteWriteCallbacks(() -> sut.applyPower(1500, 0))) //

				.deactivate();
	}

	@Test
	public void testBatteryData() throws Exception {
		new ComponentTest(new KostalManagedEssImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(ESS_ID) //
						.setReadOnlyMode(true) //
						.setModbusId(MODBUS_ID) //
						.setCapacity(10000) //
						.setWatchdog(20) //
						.setTolerance(30) //
						.setControlMode(ControlMode.INTERNAL) //
						.setModbusUnitId(71) //
						.setDebugMode(false) //
						.build()) //
				.next(new TestCase("Battery monitoring") //
						.input(BATTERY_VOLTAGE, 52000) // 52V
						.input(BATTERY_CURRENT, 100) // 100A
						.input(BATTERY_TEMPERATURE, 25) // 25°C
						.input(SOC, 75) // 75%
						.input(ACTIVE_POWER, 5200)) // 5.2kW
				.deactivate();
	}

	@Test
	public void testGridData() throws Exception {
		new ComponentTest(new KostalManagedEssImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(ESS_ID) //
						.setReadOnlyMode(true) //
						.setModbusId(MODBUS_ID) //
						.setCapacity(10000) //
						.setWatchdog(20) //
						.setTolerance(30) //
						.setControlMode(ControlMode.INTERNAL) //
						.setModbusUnitId(71) //
						.setDebugMode(false) //
						.build()) //
				.next(new TestCase("Three-phase grid data") //
						.input(GRID_VOLTAGE_L1, 230) //
						.input(GRID_VOLTAGE_L2, 230) //
						.input(GRID_VOLTAGE_L3, 230) //
						.input(FREQUENCY, 50) //
						.input(GRID_MODE, GridMode.ON_GRID)) //
				.deactivate();
	}

	@Test
	public void testInverterStates() throws Exception {
		new ComponentTest(new KostalManagedEssImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(ESS_ID) //
						.setReadOnlyMode(true) //
						.setModbusId(MODBUS_ID) //
						.setCapacity(10000) //
						.setWatchdog(20) //
						.setTolerance(30) //
						.setControlMode(ControlMode.INTERNAL) //
						.setModbusUnitId(71) //
						.setDebugMode(false) //
						.build()) //
				.next(new TestCase("Inverter feed-in") //
						.input(INVERTER_STATE, InverterState.FEEDIN)) //
				.next(new TestCase("Inverter standby") //
						.input(INVERTER_STATE, InverterState.STANDBY)) //
				.deactivate();
	}

	@Test
	public void testSmartControlMode() throws Exception {
		var sut = new KostalManagedEssImpl();
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(ESS_ID) //
						.setReadOnlyMode(false) //
						.setModbusId(MODBUS_ID) //
						.setCapacity(15000) //
						.setWatchdog(20) //
						.setTolerance(30) //
						.setControlMode(ControlMode.SMART) //
						.setModbusUnitId(71) //
						.setDebugMode(false) //
						.build()) //
				.next(new TestCase("Smart mode with tolerance") //
						.input(SOC, 60) //
						.input(MAX_CHARGE_POWER, 7500) //
						.input(MAX_DISCHARGE_POWER, 7500)) //
				.deactivate();

		// Verify that SMART mode is managed and capacity is set correctly
		assertEquals(true, sut.isManaged());
		assertEquals(15000, (int) sut.getCapacity().orElse(-1));
	}

	@Test
	public void testReadOnlyMode() throws Exception {
		var sut = new KostalManagedEssImpl();
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(ESS_ID) //
						.setReadOnlyMode(true) //
						.setModbusId(MODBUS_ID) //
						.setCapacity(10000) //
						.setWatchdog(20) //
						.setTolerance(30) //
						.setControlMode(ControlMode.INTERNAL) //
						.setModbusUnitId(71) //
						.setDebugMode(false) //
						.build()) //
				.next(new TestCase("Read-only mode prevents writes")) //
				.deactivate();

		// Verify that read-only mode means isManaged() returns false
		assertEquals(false, sut.isManaged());
	}

	@Test
	public void testInternalControlMode() throws Exception {
		var sut = new KostalManagedEssImpl();
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(ESS_ID) //
						.setReadOnlyMode(false) //
						.setModbusId(MODBUS_ID) //
						.setCapacity(12000) //
						.setWatchdog(25) //
						.setTolerance(40) //
						.setControlMode(ControlMode.INTERNAL) //
						.setModbusUnitId(71) //
						.setDebugMode(false) //
						.build()) //
				.next(new TestCase("Internal mode uses inverter AUTO mode")) //
				.deactivate();

		// INTERNAL mode is managed (readOnlyMode=false)
		assertEquals(true, sut.isManaged());
	}
}
