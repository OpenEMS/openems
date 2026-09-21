package io.openems.edge.sma.ess.sunnyboystorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.openems.common.channel.AccessMode;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.test.DummyPower;

public class EssSmaSunnyBoyStorageImplTest {

	/**
	 * Smoke test: component activates, runs one cycle, and deactivates without
	 * errors.
	 */
	@Test
	public void testActivateDeactivate() throws Exception {
		new ComponentTest(new EssSmaSunnyBoyStorageImpl()) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(3) //
						.setCapacity(2000) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

	/**
	 * Verifies that static limits set during activation are correct.
	 */
	@Test
	public void testStaticLimits() throws Exception {
		new ComponentTest(new EssSmaSunnyBoyStorageImpl()) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.build()) //
				.next(new TestCase() //
						.output(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER, -2500) //
						.output(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER, 2500) //
						.output(SymmetricEss.ChannelId.MAX_APPARENT_POWER, 2500) //
						.output(SymmetricEss.ChannelId.CAPACITY, 2000)) //
				.deactivate();
	}

	/**
	 * Verifies that applyPower() sets the correct write-channel values for
	 * discharge (activePower > 0). min=max=activePower forces the exact power rate.
	 */
	@Test
	public void testApplyPowerDischarge() throws Exception {
		var ess = new EssSmaSunnyBoyStorageImpl();
		var test = new ComponentTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.build());

		ess.applyPower(1000, 0);

		test //
				.next(new TestCase() //
						.output(EssSmaSunnyBoyStorage.ChannelId.BMS_MODE, 2290)
						.output(EssSmaSunnyBoyStorage.ChannelId.MIN_CHARGE_POWER, 0)
						.output(EssSmaSunnyBoyStorage.ChannelId.MAX_CHARGE_POWER, 0)
						.output(EssSmaSunnyBoyStorage.ChannelId.MIN_DISCHARGE_POWER, 1000)
						.output(EssSmaSunnyBoyStorage.ChannelId.MAX_DISCHARGE_POWER, 1000)
						.output(EssSmaSunnyBoyStorage.ChannelId.GRID_POWER_SETPOINT, 0))
				.deactivate();
	}

	/**
	 * Verifies that applyPower() sets the correct write-channel values for charge
	 * (activePower < 0). min=max=abs(activePower) forces the exact rate.
	 */
	@Test
	public void testApplyPowerCharge() throws Exception {
		var ess = new EssSmaSunnyBoyStorageImpl();
		var test = new ComponentTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.build());

		ess.applyPower(-800, 0);

		test //
				.next(new TestCase() //
						.output(EssSmaSunnyBoyStorage.ChannelId.BMS_MODE, 2289)
						.output(EssSmaSunnyBoyStorage.ChannelId.MIN_CHARGE_POWER, 800)
						.output(EssSmaSunnyBoyStorage.ChannelId.MAX_CHARGE_POWER, 800)
						.output(EssSmaSunnyBoyStorage.ChannelId.MIN_DISCHARGE_POWER, 0)
						.output(EssSmaSunnyBoyStorage.ChannelId.MAX_DISCHARGE_POWER, 0)
						.output(EssSmaSunnyBoyStorage.ChannelId.GRID_POWER_SETPOINT, 0))
				.deactivate();
	}

	/**
	 * Verifies that applyPower(0) sets BMS mode to Presetting (self-consumption)
	 * and allows full charge/discharge range for internal BMS control.
	 */
	@Test
	public void testApplyPowerStandby() throws Exception {
		var ess = new EssSmaSunnyBoyStorageImpl();
		var test = new ComponentTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.build());

		ess.applyPower(0, 0);

		test //
				.next(new TestCase() //
						.output(EssSmaSunnyBoyStorage.ChannelId.BMS_MODE, 2424)
						.output(EssSmaSunnyBoyStorage.ChannelId.MIN_CHARGE_POWER, 0)
						.output(EssSmaSunnyBoyStorage.ChannelId.MAX_CHARGE_POWER, 0)
						.output(EssSmaSunnyBoyStorage.ChannelId.MIN_DISCHARGE_POWER, 0)
						.output(EssSmaSunnyBoyStorage.ChannelId.MAX_DISCHARGE_POWER, 0)
						.output(EssSmaSunnyBoyStorage.ChannelId.GRID_POWER_SETPOINT, 0))
				.deactivate();
	}

	/**
	 * Verifies that read-only mode suppresses all write-channel updates.
	 */
	@Test
	public void testReadOnlyMode() throws Exception {
		var ess = new EssSmaSunnyBoyStorageImpl();
		var test = new ComponentTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setReadOnlyMode(true) //
						.build());

		ess.applyPower(1500, 0);

		test //
				.next(new TestCase() //
						.output(EssSmaSunnyBoyStorage.ChannelId.GRID_POWER_SETPOINT, null))
				.deactivate();
	}

	/**
	 * Verifies debugLog() returns a non-null string in the expected format.
	 */
	@Test
	public void testDebugLog() throws Exception {
		var ess = new EssSmaSunnyBoyStorageImpl();
		new ComponentTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.build()) //
				.next(new TestCase());

		var log = ess.debugLog();
		assertNotNull(log);
		assertTrue(log.startsWith("SoC:"));
	}

	/**
	 * Verifies getPowerPrecision() returns 1 W resolution.
	 */
	@Test
	public void testGetPowerPrecision() throws Exception {
		var ess = new EssSmaSunnyBoyStorageImpl();
		new ComponentTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.build());

		assertEquals(1, ess.getPowerPrecision());
	}

	/**
	 * Verifies getPower() returns the injected Power instance.
	 */
	@Test
	public void testGetPower() throws Exception {
		var ess = new EssSmaSunnyBoyStorageImpl();
		new ComponentTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.build());

		assertNotNull(ess.getPower());
	}

	/**
	 * Verifies getModbusSlaveTable() returns a valid table for all access modes.
	 */
	@Test
	public void testGetModbusSlaveTable() throws Exception {
		var ess = new EssSmaSunnyBoyStorageImpl();
		new ComponentTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.build());

		assertNotNull(ess.getModbusSlaveTable(AccessMode.READ_WRITE));
		assertNotNull(ess.getModbusSlaveTable(AccessMode.READ_ONLY));
		assertNotNull(ess.getModbusSlaveTable(AccessMode.WRITE_ONLY));
	}
}
