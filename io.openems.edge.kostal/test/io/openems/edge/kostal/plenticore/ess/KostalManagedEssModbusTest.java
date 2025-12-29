package io.openems.edge.kostal.plenticore.ess;

import static io.openems.edge.ess.api.SymmetricEss.ChannelId.ACTIVE_POWER;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.MAX_APPARENT_POWER;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.SOC;
import static io.openems.edge.kostal.plenticore.ess.KostalManagedEss.ChannelId.CHARGE_POWER;
import static io.openems.edge.kostal.plenticore.ess.KostalManagedEss.ChannelId.MAX_CHARGE_POWER;
import static io.openems.edge.kostal.plenticore.ess.KostalManagedEss.ChannelId.MAX_DISCHARGE_POWER;
import static io.openems.edge.kostal.plenticore.ess.KostalManagedEss.ChannelId.SET_ACTIVE_POWER;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.kostal.plenticore.enums.ControlMode;

/**
 * Tests ESS with Modbus register patterns according to KOSTAL specification.
 *
 * <p>
 * Key register addresses from KOSTAL Interface Description:
 * - 210 (0xD2): State of charge (Float, %) LSWMSW
 * - 531 (0x213): Max apparent power (UInt16, VA)
 * - 582 (0x246): Active power (Int16, W)
 * - 1034 (0x40A): Charge power (Float, W) LSWMSW [READ/WRITE for power control]
 * - 1038 (0x40E): Max charge power (Float, W) LSWMSW
 * - 1040 (0x410): Max discharge power (Float, W) LSWMSW
 *
 * <p>
 * These tests validate ESS behavior with realistic Modbus data patterns:
 * - testReadFromModbus: Validates component processes realistic register values
 * - testChargingScenario: Validates battery charging state handling
 * - testDischargingScenario: Validates battery discharging state handling
 * - testPowerControlWrite: Validates actual Modbus WRITE to register 1034
 *
 * <p>
 * Note: Register values are based on real data captured from KOSTAL Plenticore Plus
 * device using mbpoll tool.
 */
public class KostalManagedEssModbusTest {

	private static final String ESS_ID = "ess0";
	private static final String MODBUS_ID = "modbus0";

	private static ComponentTest newComponentTest() throws Exception {
		return new ComponentTest(new KostalManagedEssImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID));
	}

	private static MyConfig createConfig(ControlMode controlMode, boolean readOnlyMode) {
		return MyConfig.create() //
				.setId(ESS_ID) //
				.setModbusId(MODBUS_ID) //
				.setModbusUnitId(71) //
				.setCapacity(10000) //
				.setWatchdog(20) //
				.setTolerance(30) //
				.setReadOnlyMode(readOnlyMode) //
				.setControlMode(controlMode) //
				.setDebugMode(false) //
				.build();
	}

	@Test
	public void testReadFromModbus() throws Exception {
		// Real data captured from KOSTAL Plenticore Plus
		newComponentTest() //
				.activate(createConfig(ControlMode.INTERNAL, true)) //
				.next(new TestCase() //
						.input(SOC, 5) //
						.input(MAX_APPARENT_POWER, 10000) //
						.input(ACTIVE_POWER, 0) //
						.input(CHARGE_POWER, 0) //
						.input(MAX_CHARGE_POWER, 4610) //
						.input(MAX_DISCHARGE_POWER, 4610)) //
				.next(new TestCase() //
						.output(SOC, 5) //
						.output(MAX_APPARENT_POWER, 10000) //
						.output(ACTIVE_POWER, 0))
				.deactivate();
	}

	@Test
	public void testChargingScenario() throws Exception {
		newComponentTest() //
				.activate(createConfig(ControlMode.INTERNAL, true)) //
				.next(new TestCase() //
						.input(SOC, 50) //
						.input(ACTIVE_POWER, -4000) // Negative = charging from grid
						.input(CHARGE_POWER, 4000) // Positive = battery charging
						.input(MAX_CHARGE_POWER, 5000) //
						.input(MAX_DISCHARGE_POWER, 5000)) //
				.next(new TestCase() //
						.output(SOC, 50) //
						.output(ACTIVE_POWER, -4000) //
						.output(CHARGE_POWER, 4000))
				.deactivate();
	}

	@Test
	public void testDischargingScenario() throws Exception {
		newComponentTest() //
				.activate(createConfig(ControlMode.INTERNAL, true)) //
				.next(new TestCase() //
						.input(SOC, 90) //
						.input(ACTIVE_POWER, 5000) // Positive = discharging to grid
						.input(CHARGE_POWER, -5000) // Negative = battery discharging
						.input(MAX_CHARGE_POWER, 5000) //
						.input(MAX_DISCHARGE_POWER, 5000)) //
				.next(new TestCase() //
						.output(SOC, 90) //
						.output(ACTIVE_POWER, 5000) //
						.output(CHARGE_POWER, -5000))
				.deactivate();
	}

	@Test
	public void testPowerControlWrite() throws Exception {
		var sut = new KostalManagedEssImpl();
		var modbus = new DummyModbusBridge(MODBUS_ID) //
				.withRegisters(210, 0x0000, 0x4248) //
				.withRegisters(531, 10000) //
				.withRegisters(582, 0) //
				.withRegisters(1034, 0x0000, 0x0000, 0x0000, 0x0000, 0x4000, 0x459C, 0x4000, 0x459C);

		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", modbus) //
				.activate(createConfig(ControlMode.REMOTE, false)) //
				.next(new TestCase(), 3) //
				.next(new TestCase() //
						.input(MAX_CHARGE_POWER, 5000) //
						.input(MAX_DISCHARGE_POWER, 5000) //
						.onExecuteWriteCallbacks(() -> sut.applyPower(1000, 0)) //
						.output(SET_ACTIVE_POWER, 1000)) //
				.next(new TestCase(), 2) //
				.next(new TestCase() //
						.input(MAX_CHARGE_POWER, 5000) //
						.input(MAX_DISCHARGE_POWER, 5000) //
						.onExecuteWriteCallbacks(() -> sut.applyPower(-1000, 0)) //
						.output(SET_ACTIVE_POWER, -1000)) //
				.next(new TestCase(), 2) //
				.deactivate();
	}
}
