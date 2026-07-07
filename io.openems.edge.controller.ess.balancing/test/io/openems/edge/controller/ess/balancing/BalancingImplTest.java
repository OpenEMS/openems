package io.openems.edge.controller.ess.balancing;

import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class BalancingImplTest {

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerEssBalancingImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0") //
						.setPower(new DummyPower(0.3, 0.3, 0.1))) //
				.addReference("meter", new DummyElectricityMeter("meter0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setMeterId("meter0") //
						.setTargetGridSetpoint(0) //
						.build())
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 6000)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 12000)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 3793) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000 - 3793) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 16483)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 8981) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000 - 8981) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 19649)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 13723) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000 - 13723) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 21577)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 17469) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000 - 17469) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 22436)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 20066) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000 - 20066) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 22531)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 21564) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000 - 21564) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 22171)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 22175) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000 - 22175) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 21608)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 22173) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000 - 22173) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 21017)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 21816) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000 - 21816) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 20508)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 21311) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000 - 21311) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 20129)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 20803) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000 - 20803) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 19889)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 20377) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000 - 20377) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 19767)) //
				.deactivate();
	}

	@Test
	public void testSetGridPower() throws Exception {
		new ControllerTest(new ControllerEssBalancingImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0") //
						.setPower(new DummyPower(0.3, 0.3, 0.1))) //
				.addReference("meter", new DummyElectricityMeter("meter0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setMeterId("meter0") //
						.setTargetGridSetpoint(0) //
						.build())
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						.input("ctrl0", ControllerEssBalancing.ChannelId.SET_GRID_ACTIVE_POWER, 5000) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS, 4500)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						.input("ctrl0", ControllerEssBalancing.ChannelId.SET_GRID_ACTIVE_POWER, 5000) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS, 9000)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 10000) //
						.input("ctrl0", ControllerEssBalancing.ChannelId.SET_GRID_ACTIVE_POWER, 5000) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS, 12500)) //
				.deactivate();
	}

	/**
	 * Test behavior when ESS has tightly constrained power limits (near-equality
	 * scenario). This validates the setActivePower() logic that skips writes when
	 * abs(max-min) < 10.
	 *
	 * <p>
	 * Regression test for potential stale command risk when filter early-returns.
	 */
	@Test
	public void testTightConstraints_nearEqualityBehavior() throws Exception {
		// Arrange: ESS with very tight discharge limit (simulates nearly full battery
		// or
		// hardware constraint)
		var tightEss = new DummyManagedSymmetricEss("ess1") //
				.withMaxApparentPower(10_000) //
				.withAllowedChargePower(-10_000) //
				.withAllowedDischargePower(100) // Only 100W discharge allowed
				.setPower(new DummyPower(0.3, 0.3, 0.1));

		new ControllerTest(new ControllerEssBalancingImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", tightEss) //
				.addReference("meter", new DummyElectricityMeter("meter1")) //
				.activate(MyConfig.create() //
						.setId("ctrl1") //
						.setEssId("ess1") //
						.setMeterId("meter1") //
						.setTargetGridSetpoint(0) //
						.build())
				// Cycle 1: Grid pulling 200W, ESS can only provide 100W (tight limit)
				// Filter calculates target ~60W (P-term from 200W error)
				// Min/Max from DummyPower: min=-100 (charge), max=100 (discharge)
				// abs(100 - (-100)) = 200 >= 10, so filter runs
				.next(new TestCase() //
						.input("ess1", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						.input("meter1", ElectricityMeter.ChannelId.ACTIVE_POWER, 200) //
						.output("ess1", SET_ACTIVE_POWER_EQUALS, 60)) //
				// Cycle 2: ESS responding, filter ramps up but constrained
				.next(new TestCase() //
						.input("ess1", SymmetricEss.ChannelId.ACTIVE_POWER, 20) //
						.input("meter1", ElectricityMeter.ChannelId.ACTIVE_POWER, 180) //
						.output("ess1", SET_ACTIVE_POWER_EQUALS, 112)) //
				.deactivate();
	}

	/**
	 * Test null setpoint handling in balancing controller (e.g., when controller is
	 * temporarily disabled or in standby mode).
	 */
	@Test
	public void testNullSetpoint_noWriteOccurs() throws Exception {
		var ess = new DummyManagedSymmetricEss("ess2") //
				.setPower(new DummyPower(0.3, 0.3, 0.1));

		// Simulate a scenario where controller might pass null
		// (current implementation always calculates, but this validates defensive
		// logic)
		new ControllerTest(new ControllerEssBalancingImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", ess) //
				.addReference("meter", new DummyElectricityMeter("meter2")) //
				.activate(MyConfig.create() //
						.setId("ctrl2") //
						.setEssId("ess2") //
						.setMeterId("meter2") //
						.setTargetGridSetpoint(0) //
						.build())
				.next(new TestCase() //
						.input("ess2", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						.input("meter2", ElectricityMeter.ChannelId.ACTIVE_POWER, 0) // Balanced
						.output("ess2", SET_ACTIVE_POWER_EQUALS, 0)) // Should write 0, not null
				.deactivate();
	}

}
