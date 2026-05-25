package io.openems.edge.sma.ess.sunnyboystorage;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.test.DummyPower;

public class EssSmaSunnyBoyStorageImplTest {

	/**
	 * Smoke test: component activates and deactivates without errors.
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
				.deactivate();
	}

	/**
	 * Verifies that static limits set during activation are correct.
	 */
	@Test
	public void testStaticLimits() throws Exception {
		var ess = new EssSmaSunnyBoyStorageImpl();
		new ComponentTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create().build());

		assertEquals(Integer.valueOf(-2500),
				ess.channel(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER).getNextValue().get());
		assertEquals(Integer.valueOf(2500),
				ess.channel(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER).getNextValue().get());
		assertEquals(Integer.valueOf(2500),
				ess.channel(SymmetricEss.ChannelId.MAX_APPARENT_POWER).getNextValue().get());
		assertEquals(Integer.valueOf(2000),
				ess.channel(SymmetricEss.ChannelId.CAPACITY).getNextValue().get());
	}

	/**
	 * Verifies that applyPower() sets the correct write-channel values for
	 * discharge (activePower > 0).
	 */
	@Test
	public void testApplyPowerDischarge() throws Exception {
		var ess = new EssSmaSunnyBoyStorageImpl();
		new ComponentTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create().build());

		ess.applyPower(1000, 0);

		IntegerWriteChannel bmsModeChannel = ess.channel(EssSmaSunnyBoyStorage.ChannelId.BMS_MODE);
		assertEquals(Integer.valueOf(2424), bmsModeChannel.getNextWriteValue().get());

		IntegerWriteChannel minChargeChannel = ess.channel(EssSmaSunnyBoyStorage.ChannelId.MIN_CHARGE_POWER);
		assertEquals(Integer.valueOf(0), minChargeChannel.getNextWriteValue().get());

		IntegerWriteChannel maxChargeChannel = ess.channel(EssSmaSunnyBoyStorage.ChannelId.MAX_CHARGE_POWER);
		assertEquals(Integer.valueOf(0), maxChargeChannel.getNextWriteValue().get());

		IntegerWriteChannel minDischargeChannel = ess.channel(EssSmaSunnyBoyStorage.ChannelId.MIN_DISCHARGE_POWER);
		assertEquals(Integer.valueOf(0), minDischargeChannel.getNextWriteValue().get());

		IntegerWriteChannel maxDischargeChannel = ess.channel(EssSmaSunnyBoyStorage.ChannelId.MAX_DISCHARGE_POWER);
		assertEquals(Integer.valueOf(1000), maxDischargeChannel.getNextWriteValue().get());

		IntegerWriteChannel gridSetpointChannel = ess.channel(EssSmaSunnyBoyStorage.ChannelId.GRID_POWER_SETPOINT);
		assertEquals(Integer.valueOf(1000), gridSetpointChannel.getNextWriteValue().get());
	}

	/**
	 * Verifies that applyPower() sets the correct write-channel values for
	 * charge (activePower < 0).
	 */
	@Test
	public void testApplyPowerCharge() throws Exception {
		var ess = new EssSmaSunnyBoyStorageImpl();
		new ComponentTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create().build());

		ess.applyPower(-800, 0);

		IntegerWriteChannel minChargeChannel = ess.channel(EssSmaSunnyBoyStorage.ChannelId.MIN_CHARGE_POWER);
		assertEquals(Integer.valueOf(0), minChargeChannel.getNextWriteValue().get());

		IntegerWriteChannel maxChargeChannel = ess.channel(EssSmaSunnyBoyStorage.ChannelId.MAX_CHARGE_POWER);
		assertEquals(Integer.valueOf(800), maxChargeChannel.getNextWriteValue().get());

		IntegerWriteChannel minDischargeChannel = ess.channel(EssSmaSunnyBoyStorage.ChannelId.MIN_DISCHARGE_POWER);
		assertEquals(Integer.valueOf(0), minDischargeChannel.getNextWriteValue().get());

		IntegerWriteChannel maxDischargeChannel = ess.channel(EssSmaSunnyBoyStorage.ChannelId.MAX_DISCHARGE_POWER);
		assertEquals(Integer.valueOf(0), maxDischargeChannel.getNextWriteValue().get());

		IntegerWriteChannel gridSetpointChannel = ess.channel(EssSmaSunnyBoyStorage.ChannelId.GRID_POWER_SETPOINT);
		assertEquals(Integer.valueOf(-800), gridSetpointChannel.getNextWriteValue().get());
	}

	/**
	 * Verifies that read-only mode suppresses all write-channel updates.
	 */
	@Test
	public void testReadOnlyMode() throws Exception {
		var ess = new EssSmaSunnyBoyStorageImpl();
		new ComponentTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create().setReadOnlyMode(true).build());

		ess.applyPower(1500, 0);

		IntegerWriteChannel gridSetpointChannel = ess.channel(EssSmaSunnyBoyStorage.ChannelId.GRID_POWER_SETPOINT);
		assertEquals(false, gridSetpointChannel.getNextWriteValue().isPresent());
	}
}
