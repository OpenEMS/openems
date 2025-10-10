package io.openems.edge.controller.levl.balancing;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.controller.levl.common.LogVerbosity.NONE;

import java.time.Instant;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.DummyCycle;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class ControllerLevlEssBalancingImplTest {

	@Test
	public void testWithoutLevlRequest() throws Exception {
		final var clock = createDummyClock();
		new ControllerTest(new ControllerLevlEssBalancingImpl(clock)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0") //
						.setPower(new DummyPower(0.3, 0.3, 0.1)) //
						.withCapacity(500000) // 1.800.000.000 Ws
						.withSoc(50) // 900.000.000 Ws
						.withMaxApparentPower(500000)) //
				.addReference("meter", new DummyElectricityMeter("meter0")) //
				.addReference("cycle", new DummyCycle(1000)) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setMeterId("meter0") //
						.setLogVerbosity(NONE) //
						.build()) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20_000) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID, 20_000) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS, 6_000)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20_000) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS, 12_000)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 3_793) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20_000 - 3_793) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS, 16_483)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 8_981) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20_000 - 8_981) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS, 19_649)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 13723) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000 - 13723) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS, 21577)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 17469) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000 - 17469) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS, 22436)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 20066) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000 - 20066) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS, 22531)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 21564) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000 - 21564) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS, 22171)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 22175) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000 - 22175) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS, 21608)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 22173) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000 - 22173) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS, 21017)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 21816) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000 - 21816) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS, 20508)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 21311) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000 - 21311) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS, 20129)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 20803) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000 - 20803) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS, 19889)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 20377) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000 - 20377) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS, 19767)); //
	}

	@Test
	public void testWithLevlDischargeRequest() throws Exception {
		final var clock = createDummyClock();
		new ControllerTest(new ControllerLevlEssBalancingImpl(clock)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0") //
						.setPower(new DummyPower(0.3, 0.3, 0.1)) //
						.withCapacity(500000) // 1.800.000.000 Ws
						.withSoc(50) // 900.000.000 Ws
						.withMaxApparentPower(500000)) //
				.addReference("meter", new DummyElectricityMeter("meter0")) //
				.addReference("cycle", new DummyCycle(1000)) //
				.addReference("currentRequest", new LevlControlRequest(0, 100, Instant.now(clock))) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setMeterId("meter0") //
						.setLogVerbosity(NONE) //
						.build()) //
				.next(new TestCase() //
						// following values have to be initialized in the first cycle
						.input(ControllerLevlEssBalancing.ChannelId.SELL_TO_GRID_LIMIT, -100_000) //
						.input(ControllerLevlEssBalancing.ChannelId.BUY_FROM_GRID_LIMIT, 100_000) //
						.input(ControllerLevlEssBalancing.ChannelId.STATE_OF_CHARGE_LOWER_BOUND_LEVL, 0) //
						.input(ControllerLevlEssBalancing.ChannelId.STATE_OF_CHARGE_UPPER_BOUND_LEVL, 100) //
						.input(ControllerLevlEssBalancing.ChannelId.INFLUENCE_SELL_TO_GRID, true) //
						.input(ControllerLevlEssBalancing.ChannelId.ESS_EFFICIENCY, 80.0) //
						.input(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, 10_000) //
						.input(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, 100_000) //
						// following values have to be updated each cycle
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20_000) //
						.input("ess0", ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER, 30_000) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID, 30_000) //
						.output(ControllerLevlEssBalancing.ChannelId.PRIMARY_USE_CASE_BATTERY_POWER, 20_000L) //
						.output(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, 0L) //
						.output(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, 87_500L)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 30_000) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, -10_000) //
						.input("ess0", ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER, 20_000) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID, 20_000) //
						.output(ControllerLevlEssBalancing.ChannelId.PRIMARY_USE_CASE_BATTERY_POWER, 20_000L) //
						.output(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, 0L) //
						.output(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, 87_500L)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 20_000) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.input("ess0", ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER, 20_000) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID, 20_000) //
						.output(ControllerLevlEssBalancing.ChannelId.PRIMARY_USE_CASE_BATTERY_POWER, 20_000L) //
						.output(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, 0L) //
						.output(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, 87_500L)); //
	}

	@Test
	public void testWithLevlChargeRequest() throws Exception {
		final var clock = createDummyClock();
		var now = Instant.now(clock);

		new ControllerTest(new ControllerLevlEssBalancingImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0") //
						.setPower(new DummyPower(0.3, 0.3, 0.1)) //
						.withCapacity(500000) // 1.800.000.000 Ws
						.withSoc(50) // 900.000.000 Ws
						.withMaxApparentPower(500000)) //
				.addReference("meter", new DummyElectricityMeter("meter0")) //
				.addReference("cycle", new DummyCycle(1000)) //
				.addReference("currentRequest", new LevlControlRequest(0, 100, now)) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setMeterId("meter0") //
						.setLogVerbosity(NONE) //
						.build()) //
				.next(new TestCase() //
						// following values have to be initialized in the first cycle
						.input(ControllerLevlEssBalancing.ChannelId.SELL_TO_GRID_LIMIT, -100_000) //
						.input(ControllerLevlEssBalancing.ChannelId.BUY_FROM_GRID_LIMIT, 100_000) //
						.input(ControllerLevlEssBalancing.ChannelId.STATE_OF_CHARGE_LOWER_BOUND_LEVL, 0) //
						.input(ControllerLevlEssBalancing.ChannelId.STATE_OF_CHARGE_UPPER_BOUND_LEVL, 100) //
						.input(ControllerLevlEssBalancing.ChannelId.INFLUENCE_SELL_TO_GRID, true) //
						.input(ControllerLevlEssBalancing.ChannelId.ESS_EFFICIENCY, 80.0) //
						.input(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, -10000) //
						.input(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, 100_000) //
						// following values have to be updated each cycle
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000) //
						.input("ess0", ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER, 10000) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID, 10000) //
						.output(ControllerLevlEssBalancing.ChannelId.PRIMARY_USE_CASE_BATTERY_POWER, 20000L) //
						.output(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, 0L) //
						.output(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, 108000L)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 10000) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 10000) //
						.input("ess0", ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER, 20000) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID, 20000) //
						.output(ControllerLevlEssBalancing.ChannelId.PRIMARY_USE_CASE_BATTERY_POWER, 20000L) //
						.output(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, 0L) //
						.output(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, 108000L)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 20000) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.input("ess0", ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER, 20000) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID, 20000) //
						.output(ControllerLevlEssBalancing.ChannelId.PRIMARY_USE_CASE_BATTERY_POWER, 20000L) //
						.output(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, 0L) //
						.output(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, 108000L)); //
	}

	// Test with discharge request (ws) > MAX_INT. Constrained by sell to grid
	// limit.
	@Test
	public void testWithLargeLevlDischargeRequest() throws Exception {
		final var clock = createDummyClock();
		var now = Instant.now(clock);

		new ControllerTest(new ControllerLevlEssBalancingImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0") //
						.setPower(new DummyPower(0.3, 0.3, 0.1)) //
						.withCapacity(500000) // 1.800.000.000 Ws
						.withSoc(50) // 900.000.000 Ws
						.withMaxApparentPower(500000)) //
				.addReference("meter", new DummyElectricityMeter("meter0")) //
				.addReference("cycle", new DummyCycle(1000)) //
				.addReference("currentRequest", new LevlControlRequest(0, 100, now)) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setMeterId("meter0") //
						.setLogVerbosity(NONE) //
						.build()) //
				.next(new TestCase() //
						// following values have to be initialized in the first cycle
						.input(ControllerLevlEssBalancing.ChannelId.SELL_TO_GRID_LIMIT, -100_000) //
						.input(ControllerLevlEssBalancing.ChannelId.BUY_FROM_GRID_LIMIT, 100_000) //
						.input(ControllerLevlEssBalancing.ChannelId.STATE_OF_CHARGE_LOWER_BOUND_LEVL, 0) //
						.input(ControllerLevlEssBalancing.ChannelId.STATE_OF_CHARGE_UPPER_BOUND_LEVL, 100) //
						.input(ControllerLevlEssBalancing.ChannelId.INFLUENCE_SELL_TO_GRID, true) //
						.input(ControllerLevlEssBalancing.ChannelId.ESS_EFFICIENCY, 80.0) //
						.input(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, 100_000) //
						.input(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, 2_500_000_000L) //
						// following values have to be updated each cycle
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000) //
						.input("ess0", ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER, 120000) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID, 120000) //
						.output(ControllerLevlEssBalancing.ChannelId.PRIMARY_USE_CASE_BATTERY_POWER, 20000L) //
						.output(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, 2_499_900_000L) //
						.output(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, -25000L)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 120000) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, -100000) //
						.input("ess0", ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER, 120000) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID, 120000) //
						.output(ControllerLevlEssBalancing.ChannelId.PRIMARY_USE_CASE_BATTERY_POWER, 20000L) //
						.output(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, 2_499_800_000L) //
						.output(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, -150000L)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 120000) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, -100000) //
						.input("ess0", ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER, 120000) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID, 120000) //
						.output(ControllerLevlEssBalancing.ChannelId.PRIMARY_USE_CASE_BATTERY_POWER, 20000L) //
						.output(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, 2_499_700_000L) //
						.output(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, -275000L)); //
	}

	@Test
	public void testWithReservedChargeCapacityLevlChargesPucMustNotCharge() throws Exception {
		final var clock = createDummyClock();
		new ControllerTest(new ControllerLevlEssBalancingImpl(clock)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0") //
						.setPower(new DummyPower(0.3, 0.3, 0.1)) //
						.withCapacity(500000) // 1.800.000.000 Ws
						.withMaxApparentPower(500000)) //
				.addReference("meter", new DummyElectricityMeter("meter0")) //
				.addReference("cycle", new DummyCycle(1000)) //
				.addReference("currentRequest", new LevlControlRequest(0, 100, Instant.now(clock))) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setMeterId("meter0") //
						.setLogVerbosity(NONE) //
						.build()) //
				.next(new TestCase() //
						// following values have to be initialized in the first cycle
						.input(ControllerLevlEssBalancing.ChannelId.SELL_TO_GRID_LIMIT, -800000) //
						.input(ControllerLevlEssBalancing.ChannelId.BUY_FROM_GRID_LIMIT, 800000) //
						.input(ControllerLevlEssBalancing.ChannelId.STATE_OF_CHARGE_LOWER_BOUND_LEVL, 0) //
						.input(ControllerLevlEssBalancing.ChannelId.STATE_OF_CHARGE_UPPER_BOUND_LEVL, 100) //
						.input(ControllerLevlEssBalancing.ChannelId.INFLUENCE_SELL_TO_GRID, true) //
						.input(ControllerLevlEssBalancing.ChannelId.ESS_EFFICIENCY, 80.0) //
						.input(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, -10_000_000) //
						// 10% of total capacity
						.input(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, -180_000_000) //
						// following values have to be updated each cycle
						.input("ess0", SymmetricEss.ChannelId.SOC, 90) // 90% = 450,000 Wh = 1,620,000.000 Ws
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						// grid power w/o Levl --> sell to grid
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, -20_000) //
						.input("ess0", ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER, -500_000) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID, -500_000) //
						// puc should not do anything because capacity is completely reserved for Levl
						.output(ControllerLevlEssBalancing.ChannelId.PRIMARY_USE_CASE_BATTERY_POWER, 0L) //
						// 500,000 Ws should be realized, therefore 9,500,000 Ws are remaining
						.output(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, -9_500_000L) //
						// Levl soc increases by 500,000 Ws * 80% efficiency = 400,000 Ws
						.output(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, -179_600_000L)) //
				.next(new TestCase() //
						// 90% = 450,000 Wh = 1,620,000,000 Ws | should be 1,620,400,000 Ws but only
						// full percent values can be read
						.input("ess0", SymmetricEss.ChannelId.SOC, 90)
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, -500_000) //
						// grid power ceteris paribus w/ Levl
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 480_000) //
						.input("ess0", ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER, -500_000) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID, -500_000) //
						// since reserved capacity decreased by 400,000 Ws in the previous cycle but ess
						// soc value remains the same, puc can charge again
						.output(ControllerLevlEssBalancing.ChannelId.PRIMARY_USE_CASE_BATTERY_POWER, -20_000L) //
						// 480,000 Ws can be realized for Levl, therefore 9,020,00 are remaining
						.output(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, -9_020_000L) //
						// Levl soc increases by 480,000 Ws * 80% efficiency = 384,000 Ws
						.output(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, -179_216_000L)); //
	}

	@Test
	public void testWithReservedChargeCapacityLevlDischargesPucMustNotCharge() throws Exception {
		final var clock = createDummyClock();
		new ControllerTest(new ControllerLevlEssBalancingImpl(clock)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0") //
						.setPower(new DummyPower(0.3, 0.3, 0.1)) //
						.withCapacity(500_000) // 1,800,000,000 Ws
						.withMaxApparentPower(500_000)) //
				.addReference("meter", new DummyElectricityMeter("meter0")) //
				.addReference("cycle", new DummyCycle(1000)) //
				.addReference("currentRequest", new LevlControlRequest(0, 100, Instant.now(clock))) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setMeterId("meter0") //
						.setLogVerbosity(NONE) //
						.build()) //
				.next(new TestCase() //
						// following values have to be initialized in the first cycle
						.input(ControllerLevlEssBalancing.ChannelId.SELL_TO_GRID_LIMIT, -800_000) //
						.input(ControllerLevlEssBalancing.ChannelId.BUY_FROM_GRID_LIMIT, 800_000) //
						.input(ControllerLevlEssBalancing.ChannelId.STATE_OF_CHARGE_LOWER_BOUND_LEVL, 0) //
						.input(ControllerLevlEssBalancing.ChannelId.STATE_OF_CHARGE_UPPER_BOUND_LEVL, 100) //
						.input(ControllerLevlEssBalancing.ChannelId.INFLUENCE_SELL_TO_GRID, true) //
						.input(ControllerLevlEssBalancing.ChannelId.ESS_EFFICIENCY, 80.0) //
						.input(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, 10_000_000) //
						// 10% of total capacity
						.input(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, -180_000_000) //
						// following values have to be updated each cycle
						.input("ess0", SymmetricEss.ChannelId.SOC, 90) // 90% = 450,000 Wh = 1,620,000,000 Ws
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						// grid power w/o Levl --> sell to grid
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, -20_000) //
						// max discharge power of 500,000 W should be applied
						.input("ess0", ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER, 500_000) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID, 500_000) //
						// puc should not do anything because capacity is completely reserved for Levl
						.output(ControllerLevlEssBalancing.ChannelId.PRIMARY_USE_CASE_BATTERY_POWER, 0L) //
						// 500,000 Ws should be realized, therefore 9,500,000 Ws are remaining
						.output(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, 9_500_000L) //
						// Levl soc decreases by 500,000 Ws / 80% efficiency = 625,000 Ws
						.output(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, -180_625_000L)) //
				.next(new TestCase() //
						// 90% = 450,000 Wh = 1,620,000,000 Ws | should be 1,619,375,000 Ws but only
						// full percent values can be read
						.input("ess0", SymmetricEss.ChannelId.SOC, 90) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 500_000) //
						// grid power ceteris paribus w/ Levl
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, -520_000) //
						// max discharge power of 500,000 W should be applied
						.input("ess0", ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER, 500_000) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID, 500_000) //
						// puc should not do anything because capacity is still completely reserved for
						// Levl
						.output(ControllerLevlEssBalancing.ChannelId.PRIMARY_USE_CASE_BATTERY_POWER, 0L) //
						// 500,000 Ws should be realized, therefore 9,000,000 Ws are remaining
						.output(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, 9_000_000L) //
						// Levl soc decreases by 500,000 Ws / 80% efficiency = 625,000 Ws
						.output(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, -181_250_000L)); //
	}

	@Test
	public void testWithReservedChargeCapacityLevlChargesPucMayCharge() throws Exception {
		final var clock = createDummyClock();
		new ControllerTest(new ControllerLevlEssBalancingImpl(clock)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0") //
						.setPower(new DummyPower(0.3, 0.3, 0.1)) //
						.withCapacity(500_000) // 1,800,000,000 Ws
						.withMaxApparentPower(500_000)) //
				.addReference("meter", new DummyElectricityMeter("meter0")) //
				.addReference("cycle", new DummyCycle(1000)) //
				.addReference("currentRequest", new LevlControlRequest(0, 100, Instant.now(clock))) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setMeterId("meter0") //
						.setLogVerbosity(NONE) //
						.build()) //
				.next(new TestCase() //
						// following values have to be initialized in the first cycle
						.input(ControllerLevlEssBalancing.ChannelId.SELL_TO_GRID_LIMIT, -800_000) //
						.input(ControllerLevlEssBalancing.ChannelId.BUY_FROM_GRID_LIMIT, 800_000) //
						.input(ControllerLevlEssBalancing.ChannelId.STATE_OF_CHARGE_LOWER_BOUND_LEVL, 0) //
						.input(ControllerLevlEssBalancing.ChannelId.STATE_OF_CHARGE_UPPER_BOUND_LEVL, 100) //
						.input(ControllerLevlEssBalancing.ChannelId.INFLUENCE_SELL_TO_GRID, true) //
						.input(ControllerLevlEssBalancing.ChannelId.ESS_EFFICIENCY, 80.0) //
						.input(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, -10_000_000) //
						// 10% of total capacity
						.input(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, -180_000_000) //
						// following values have to be updated each cycle
						.input("ess0", SymmetricEss.ChannelId.SOC, 85) // 85% = 425,000 Wh = 1,530,000,000 Ws
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						// grid power w/o Levl --> sell to grid
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, -10_000) //
						// max charge power of 500,000 W should be applied
						.input("ess0", ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER, -500_000) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID, -500_000) //
						// puc should charge 10,000 Ws since 5% capacity is available
						.output(ControllerLevlEssBalancing.ChannelId.PRIMARY_USE_CASE_BATTERY_POWER, -10_000L) //
						// 490,000 Ws should be realized, therefore 9,510,000 Ws are remaining
						.output(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, -9_510_000L) //
						// Levl soc increases by 490,000 Ws * 80% efficiency = 392,000 Ws
						.output(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, -179_608_000L)) //
				.next(new TestCase() //
						// 85% = 425,000 Wh = 1,530,000,000 Ws | should be 1,530,400,000 Ws but only
						// full percent values can be read
						.input("ess0", SymmetricEss.ChannelId.SOC, 85) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, -500_000) //
						// grid power ceteris paribus w/ Levl
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 490_000) //
						// max charge power of 500,000 W should be applied
						.input("ess0", ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER, -500_000) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID, -500_000) //
						// puc should still charge 10,000 Ws
						.output(ControllerLevlEssBalancing.ChannelId.PRIMARY_USE_CASE_BATTERY_POWER, -10_000L) //
						// 490,000 Ws should be realized, therefore 9,020,000 Ws are remaining
						.output(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, -9_020_000L) //
						// Levl soc increases by 490,000 Ws * 80% efficiency = 392,000 Ws
						.output(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, -179_216_000L)); //
	}

	@Test
	public void testWithReservedChargeCapacityLevlChargesPucMayDischarge() throws Exception {
		final var clock = createDummyClock();
		new ControllerTest(new ControllerLevlEssBalancingImpl(clock)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0") //
						.setPower(new DummyPower(0.3, 0.3, 0.1)) //
						.withCapacity(500_000) // 1,800,000,000 Ws
						.withMaxApparentPower(500_000)) //
				.addReference("meter", new DummyElectricityMeter("meter0")) //
				.addReference("cycle", new DummyCycle(1000)) //
				.addReference("currentRequest", new LevlControlRequest(0, 100, Instant.now(clock))) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setMeterId("meter0") //
						.setLogVerbosity(NONE) //
						.build()) //
				.next(new TestCase() //
						// following values have to be initialized in the first cycle
						.input(ControllerLevlEssBalancing.ChannelId.SELL_TO_GRID_LIMIT, -800_000) //
						.input(ControllerLevlEssBalancing.ChannelId.BUY_FROM_GRID_LIMIT, 800_000) //
						.input(ControllerLevlEssBalancing.ChannelId.STATE_OF_CHARGE_LOWER_BOUND_LEVL, 0) //
						.input(ControllerLevlEssBalancing.ChannelId.STATE_OF_CHARGE_UPPER_BOUND_LEVL, 100) //
						.input(ControllerLevlEssBalancing.ChannelId.INFLUENCE_SELL_TO_GRID, true) //
						.input(ControllerLevlEssBalancing.ChannelId.ESS_EFFICIENCY, 80.0) //
						.input(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, -10_000_000) //
						// 10% of total capacity
						.input(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, -180_000_000) //
						// following values have to be updated each cycle
						.input("ess0", SymmetricEss.ChannelId.SOC, 50) // 50% = 250,000 Wh = 900,000,000 Ws
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						// grid power w/o Levl --> buy from grid
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 10_000) //
						// max charge power of 500,000 W should be applied
						.input("ess0", ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER, -500_000) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID, -500_000) //
						// puc should discharge 10,000 Ws since Levl has reserved charge not discharge
						// energy
						.output(ControllerLevlEssBalancing.ChannelId.PRIMARY_USE_CASE_BATTERY_POWER, 10_000L) //
						// 510,000 Ws can be realized, because puc discharges 10,000 Ws
						.output(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, -9_490_000L) //
						// Levl soc increases by 510,000 Ws * 80% efficiency = 408,000 Ws
						.output(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, -179_592_000L))
				.next(new TestCase() //
						// 50% = 250,000 Wh = 900,000,000 Ws | should be 900,400,000 Ws but only full
						// percent values can be read
						.input("ess0", SymmetricEss.ChannelId.SOC, 50) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, -500_000) //
						// grid power ceteris paribus w/ Levl
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 510_000) //
						// max charge power of 500,000 W should be applied
						.input("ess0", ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER, -500_000) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID, -500_000) //
						// puc should still charge 10,000 Ws
						.output(ControllerLevlEssBalancing.ChannelId.PRIMARY_USE_CASE_BATTERY_POWER, 10_000L) //
						// 510,000 Ws can be realized again, therefore 8,980,000 Ws are remaining
						.output(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, -8_980_000L) //
						// Levl soc increases by 510,000 Ws * 80% efficiency = 408,000 Ws
						.output(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, -179_184_000L)); //
	}

	@Test
	public void testInfluenceSellToGrid_PucSellToGrid_LevlChargeForbidden() throws Exception {
		final var clock = createDummyClock();
		new ControllerTest(new ControllerLevlEssBalancingImpl(clock)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0") //
						.setPower(new DummyPower(0.3, 0.3, 0.1)) //
						.withCapacity(500000) // 1.800.000.000 Ws
						.withMaxApparentPower(500000)) //
				.addReference("meter", new DummyElectricityMeter("meter0")) //
				.addReference("cycle", new DummyCycle(1000)) //
				.addReference("currentRequest", new LevlControlRequest(0, 100, Instant.now(clock))) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setMeterId("meter0") //
						.setLogVerbosity(NONE) //
						.build()) //
				.next(new TestCase("puc sell to grid, levl charge not allowed") //
						.input(ControllerLevlEssBalancing.ChannelId.SELL_TO_GRID_LIMIT, -100_000) //
						.input(ControllerLevlEssBalancing.ChannelId.BUY_FROM_GRID_LIMIT, 100_000) //
						.input(ControllerLevlEssBalancing.ChannelId.STATE_OF_CHARGE_LOWER_BOUND_LEVL, 0) //
						.input(ControllerLevlEssBalancing.ChannelId.STATE_OF_CHARGE_UPPER_BOUND_LEVL, 100) //
						.input(ControllerLevlEssBalancing.ChannelId.INFLUENCE_SELL_TO_GRID, false) //
						.input(ControllerLevlEssBalancing.ChannelId.ESS_EFFICIENCY, 80.0) //
						.input(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, -10000L) //
						.input(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, -180_000_000) //
						.input("ess0", SymmetricEss.ChannelId.SOC, 90) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, -20000) //
						.input("ess0", ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER, 0) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID, 0) //
						.output(ControllerLevlEssBalancing.ChannelId.PRIMARY_USE_CASE_BATTERY_POWER, 0L) //
						.output(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, -10000L) //
						.output(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, -180_000_000L)); //
	}

	@Test
	public void testInfluenceSellToGrid_PucSellToGrid_LevlDischargeForbidden() throws Exception {
		final var clock = createDummyClock();
		new ControllerTest(new ControllerLevlEssBalancingImpl(clock)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0") //
						.setPower(new DummyPower(0.3, 0.3, 0.1)) //
						.withCapacity(500000) // 1.800.000.000 Ws
						.withMaxApparentPower(500000)) //
				.addReference("meter", new DummyElectricityMeter("meter0")) //
				.addReference("cycle", new DummyCycle(1000)) //
				.addReference("currentRequest", new LevlControlRequest(0, 100, Instant.now(clock))) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setMeterId("meter0") //
						.setLogVerbosity(NONE) //
						.build()) //
				.next(new TestCase("puc sell to grid, levl discharge not allowed") //
						.input(ControllerLevlEssBalancing.ChannelId.SELL_TO_GRID_LIMIT, -100_000) //
						.input(ControllerLevlEssBalancing.ChannelId.BUY_FROM_GRID_LIMIT, 100_000) //
						.input(ControllerLevlEssBalancing.ChannelId.STATE_OF_CHARGE_LOWER_BOUND_LEVL, 0) //
						.input(ControllerLevlEssBalancing.ChannelId.STATE_OF_CHARGE_UPPER_BOUND_LEVL, 100) //
						.input(ControllerLevlEssBalancing.ChannelId.INFLUENCE_SELL_TO_GRID, false) //
						.input(ControllerLevlEssBalancing.ChannelId.ESS_EFFICIENCY, 80.0) //
						.input(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, 10000L) //
						.input(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, -180_000_000) //
						.input("ess0", SymmetricEss.ChannelId.SOC, 90) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, -20000) //
						.input("ess0", ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER, 0) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID, 0) //
						.output(ControllerLevlEssBalancing.ChannelId.PRIMARY_USE_CASE_BATTERY_POWER, 0L) //
						.output(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, 10000L) //
						.output(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, -180_000_000L)); //
	}

	@Test
	public void testInfluenceSellToGrid_PucBuyFromGrid_LevlChargeAllowed() throws Exception {
		final var clock = createDummyClock();
		new ControllerTest(new ControllerLevlEssBalancingImpl(clock)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0") //
						.setPower(new DummyPower(0.3, 0.3, 0.1)) //
						.withCapacity(500000) // 1.800.000.000 Ws
						.withMaxApparentPower(500000)) //
				.addReference("meter", new DummyElectricityMeter("meter0")) //
				.addReference("cycle", new DummyCycle(1000)) //
				.addReference("currentRequest", new LevlControlRequest(0, 100, Instant.now(clock))) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setMeterId("meter0") //
						.setLogVerbosity(NONE) //
						.build()) //
				.next(new TestCase("puc buy from grid, levl charge is allowed") //
						.input(ControllerLevlEssBalancing.ChannelId.SELL_TO_GRID_LIMIT, -100_000) //
						.input(ControllerLevlEssBalancing.ChannelId.BUY_FROM_GRID_LIMIT, 100_000) //
						.input(ControllerLevlEssBalancing.ChannelId.STATE_OF_CHARGE_LOWER_BOUND_LEVL, 0) //
						.input(ControllerLevlEssBalancing.ChannelId.STATE_OF_CHARGE_UPPER_BOUND_LEVL, 100) //
						.input(ControllerLevlEssBalancing.ChannelId.INFLUENCE_SELL_TO_GRID, false) //
						.input(ControllerLevlEssBalancing.ChannelId.ESS_EFFICIENCY, 80.0) //
						.input(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, -30000L) //
						.input(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, 0) //
						.input("ess0", SymmetricEss.ChannelId.SOC, 0) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000) //
						.input("ess0", ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER, -30000) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID, -30000) //
						.output(ControllerLevlEssBalancing.ChannelId.PRIMARY_USE_CASE_BATTERY_POWER, 0L) //
						.output(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, 0L) //
						.output(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, 24000L)); //
	}

	@Test
	public void testInfluenceSellToGrid_PucBuyFromGrid_LevlDischargeLimited() throws Exception {
		final var clock = createDummyClock();
		new ControllerTest(new ControllerLevlEssBalancingImpl(clock)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0") //
						.setPower(new DummyPower(0.3, 0.3, 0.1)) //
						.withCapacity(500000) // 1.800.000.000 Ws
						.withMaxApparentPower(500000)) //
				.addReference("meter", new DummyElectricityMeter("meter0")) //
				.addReference("cycle", new DummyCycle(1000)) //
				.addReference("currentRequest", new LevlControlRequest(0, 100, Instant.now(clock))) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setMeterId("meter0") //
						.setLogVerbosity(NONE) //
						.build()) //
				.next(new TestCase("puc buy from grid, levl discharge is limited to grid limit 0") //
						.input(ControllerLevlEssBalancing.ChannelId.SELL_TO_GRID_LIMIT, -100_000) //
						.input(ControllerLevlEssBalancing.ChannelId.BUY_FROM_GRID_LIMIT, 100_000) //
						.input(ControllerLevlEssBalancing.ChannelId.STATE_OF_CHARGE_LOWER_BOUND_LEVL, 0) //
						.input(ControllerLevlEssBalancing.ChannelId.STATE_OF_CHARGE_UPPER_BOUND_LEVL, 100) //
						.input(ControllerLevlEssBalancing.ChannelId.INFLUENCE_SELL_TO_GRID, false) //
						.input(ControllerLevlEssBalancing.ChannelId.ESS_EFFICIENCY, 100.0) //
						.input(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, 30000L) //
						.input(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, 180_000_000L) //
						.input("ess0", SymmetricEss.ChannelId.SOC, 10) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000) //
						.input("ess0", ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER, 20000) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID, 20000) //
						.output(ControllerLevlEssBalancing.ChannelId.PRIMARY_USE_CASE_BATTERY_POWER, 0L) //
						.output(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, 10000L) //
						.output(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, 179_980_000L)); //
	}

	@Test
	public void testUpperSocLimit() throws Exception {
		final var clock = createDummyClock();
		new ControllerTest(new ControllerLevlEssBalancingImpl(clock)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0") //
						.setPower(new DummyPower(0.3, 0.3, 0.1)) //
						.withCapacity(500000) // 1.800.000.000 Ws
						.withMaxApparentPower(20_000_000)) //
				.addReference("meter", new DummyElectricityMeter("meter0")) //
				.addReference("cycle", new DummyCycle(1000)) //
				.addReference("currentRequest", new LevlControlRequest(0, 100, Instant.now(clock))) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setMeterId("meter0") //
						.setLogVerbosity(NONE) //
						.build()) //
				.next(new TestCase() //
						// following values have to be initialized in the first cycle
						.input(ControllerLevlEssBalancing.ChannelId.SELL_TO_GRID_LIMIT, -40_000_000) //
						.input(ControllerLevlEssBalancing.ChannelId.BUY_FROM_GRID_LIMIT, 40_000_000) //
						.input(ControllerLevlEssBalancing.ChannelId.STATE_OF_CHARGE_LOWER_BOUND_LEVL, 20) //
						.input(ControllerLevlEssBalancing.ChannelId.STATE_OF_CHARGE_UPPER_BOUND_LEVL, 80) //
						.input(ControllerLevlEssBalancing.ChannelId.INFLUENCE_SELL_TO_GRID, true) //
						.input(ControllerLevlEssBalancing.ChannelId.ESS_EFFICIENCY, 100.0) //
						.input(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, -100_000_000) //
						.input(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, 0) //
						// following values have to be updated each cycle
						.input("ess0", SymmetricEss.ChannelId.SOC, 79) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.input("ess0", ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER, -18_000_000) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID, -18_000_000) //
						.output(ControllerLevlEssBalancing.ChannelId.PRIMARY_USE_CASE_BATTERY_POWER, -0L) //
						.output(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, -82_000_000L) //
						.output(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, 18_000_000L)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.SOC, 80) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, -18_000_000) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 18_000_000) //
						.input("ess0", ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER, 0) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID, 0) //
						.output(ControllerLevlEssBalancing.ChannelId.PRIMARY_USE_CASE_BATTERY_POWER, 0L) //
						.output(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, -82_000_000L) //
						.output(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, 18_000_000L)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.SOC, 80) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.input("ess0", ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER, 0) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID, 0) //
						.output(ControllerLevlEssBalancing.ChannelId.PRIMARY_USE_CASE_BATTERY_POWER, 0L) //
						.output(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, -82_000_000L) //
						.output(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, 18_000_000L)); //
	}

	@Test
	public void testUpperSocLimit_levlHasCharged() throws Exception {
		final var clock = createDummyClock();
		new ControllerTest(new ControllerLevlEssBalancingImpl(clock)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0") //
						.setPower(new DummyPower(0.3, 0.3, 0.1)) //
						.withCapacity(500000) // 1.800.000.000 Ws
						.withMaxApparentPower(30_000_000)) //
				.addReference("meter", new DummyElectricityMeter("meter0")) //
				.addReference("cycle", new DummyCycle(1000)) //
				.addReference("currentRequest", new LevlControlRequest(0, 100, Instant.now(clock))) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setMeterId("meter0") //
						.setLogVerbosity(NONE) //
						.build()) //
				.next(new TestCase() //
						// following values have to be initialized in the first cycle
						.input(ControllerLevlEssBalancing.ChannelId.SELL_TO_GRID_LIMIT, -40_000_000) //
						.input(ControllerLevlEssBalancing.ChannelId.BUY_FROM_GRID_LIMIT, 40_000_000) //
						.input(ControllerLevlEssBalancing.ChannelId.STATE_OF_CHARGE_LOWER_BOUND_LEVL, 5) //
						.input(ControllerLevlEssBalancing.ChannelId.STATE_OF_CHARGE_UPPER_BOUND_LEVL, 95) //
						.input(ControllerLevlEssBalancing.ChannelId.INFLUENCE_SELL_TO_GRID, true) //
						.input(ControllerLevlEssBalancing.ChannelId.ESS_EFFICIENCY, 100.0) //
						.input(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, -100_000_000) //
						.input(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, 36_000_000) // 2%
						// following values have to be updated each cycle
						.input("ess0", SymmetricEss.ChannelId.SOC, 94) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, -18_000_000) //
						.input("ess0", ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER, -18_000_000) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID, -18_000_000) //
						.output(ControllerLevlEssBalancing.ChannelId.PRIMARY_USE_CASE_BATTERY_POWER, -18_000_000L) //
						.output(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, -100_000_000L) //
						.output(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, 36_000_000L)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.SOC, 95) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, -18_000_000) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.input("ess0", ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER, 0) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID, 0) //
						.output(ControllerLevlEssBalancing.ChannelId.PRIMARY_USE_CASE_BATTERY_POWER, -18_000_000L) //
						.output(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, -118_000_000L) //
						.output(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, 18_000_000L)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.SOC, 95) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, -18_000_000) //
						.input("ess0", ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER, 0) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID, 0) //
						.output(ControllerLevlEssBalancing.ChannelId.PRIMARY_USE_CASE_BATTERY_POWER, -18_000_000L) //
						.output(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, -136_000_000L) //
						.output(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, 0L)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.SOC, 95) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, -18_000_000) //
						.input("ess0", ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER, -18_000_000) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID, -18_000_000) //
						.output(ControllerLevlEssBalancing.ChannelId.PRIMARY_USE_CASE_BATTERY_POWER, -18_000_000L) //
						.output(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, -136_000_000L) //
						.output(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, 0L)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.SOC, 96) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, -18_000_000) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.input("ess0", ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER, -18_000_000) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID, -18_000_000) //
						.output(ControllerLevlEssBalancing.ChannelId.PRIMARY_USE_CASE_BATTERY_POWER, -18_000_000L) //
						.output(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, -136_000_000L) //
						.output(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, 0L)); //
	}

	@Test
	public void testLowerSocLimit() throws Exception {
		final var clock = createDummyClock();
		new ControllerTest(new ControllerLevlEssBalancingImpl(clock)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0") //
						.setPower(new DummyPower(0.3, 0.3, 0.1)) //
						.withCapacity(500000) // 1.800.000.000 Ws
						.withMaxApparentPower(20_000_000)) //
				.addReference("meter", new DummyElectricityMeter("meter0")) //
				.addReference("cycle", new DummyCycle(1000)) //
				.addReference("currentRequest", new LevlControlRequest(0, 100, Instant.now(clock))) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setMeterId("meter0") //
						.setLogVerbosity(NONE) //
						.build()) //
				.next(new TestCase() //
						// following values have to be initialized in the first cycle
						.input(ControllerLevlEssBalancing.ChannelId.SELL_TO_GRID_LIMIT, -40_000_000) //
						.input(ControllerLevlEssBalancing.ChannelId.BUY_FROM_GRID_LIMIT, 40_000_000) //
						.input(ControllerLevlEssBalancing.ChannelId.STATE_OF_CHARGE_LOWER_BOUND_LEVL, 20) //
						.input(ControllerLevlEssBalancing.ChannelId.STATE_OF_CHARGE_UPPER_BOUND_LEVL, 80) //
						.input(ControllerLevlEssBalancing.ChannelId.INFLUENCE_SELL_TO_GRID, true) //
						.input(ControllerLevlEssBalancing.ChannelId.ESS_EFFICIENCY, 100.0) //
						.input(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, 100_000_000) //
						.input(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, 0) //
						// following values have to be updated each cycle
						.input("ess0", SymmetricEss.ChannelId.SOC, 21) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.input("ess0", ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER, 18_000_000) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID, 18_000_000) //
						.output(ControllerLevlEssBalancing.ChannelId.PRIMARY_USE_CASE_BATTERY_POWER, -0L) //
						.output(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, 82_000_000L) //
						.output(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, -18_000_000L)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.SOC, 20) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 18_000_000) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, -18_000_000) //
						.input("ess0", ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER, 0) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID, 0) //
						.output(ControllerLevlEssBalancing.ChannelId.PRIMARY_USE_CASE_BATTERY_POWER, 0L) //
						.output(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, 82_000_000L) //
						.output(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, -18_000_000L)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.SOC, 20) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.input("ess0", ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER, 0) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID, 0) //
						.output(ControllerLevlEssBalancing.ChannelId.PRIMARY_USE_CASE_BATTERY_POWER, 0L) //
						.output(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, 82_000_000L) //
						.output(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, -18_000_000L)); //
	}

	@Test
	public void testLowerSocLimit_levlHasDischarged() throws Exception {
		final var clock = createDummyClock();
		new ControllerTest(new ControllerLevlEssBalancingImpl(clock)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0") //
						.setPower(new DummyPower(0.3, 0.3, 0.1)) //
						.withCapacity(500000) // 1.800.000.000 Ws
						.withMaxApparentPower(30_000_000)) //
				.addReference("meter", new DummyElectricityMeter("meter0")) //
				.addReference("cycle", new DummyCycle(1000)) //
				.addReference("currentRequest", new LevlControlRequest(0, 100, Instant.now(clock))) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setMeterId("meter0") //
						.setLogVerbosity(NONE) //
						.build()) //
				.next(new TestCase() //
						// following values have to be initialized in the first cycle
						.input(ControllerLevlEssBalancing.ChannelId.SELL_TO_GRID_LIMIT, -40_000_000) //
						.input(ControllerLevlEssBalancing.ChannelId.BUY_FROM_GRID_LIMIT, 40_000_000) //
						.input(ControllerLevlEssBalancing.ChannelId.STATE_OF_CHARGE_LOWER_BOUND_LEVL, 5) //
						.input(ControllerLevlEssBalancing.ChannelId.STATE_OF_CHARGE_UPPER_BOUND_LEVL, 95) //
						.input(ControllerLevlEssBalancing.ChannelId.INFLUENCE_SELL_TO_GRID, true) //
						.input(ControllerLevlEssBalancing.ChannelId.ESS_EFFICIENCY, 100.0) //
						.input(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, 100_000_000) //
						.input(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, -36_000_000) // 2%
						// following values have to be updated each cycle
						.input("ess0", SymmetricEss.ChannelId.SOC, 6) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 18_000_000) //
						.input("ess0", ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER, 18_000_000) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID, 18_000_000) //
						.output(ControllerLevlEssBalancing.ChannelId.PRIMARY_USE_CASE_BATTERY_POWER, 18_000_000L) //
						.output(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, 100_000_000L) //
						.output(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, -36_000_000L)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.SOC, 5) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 18_000_000) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.input("ess0", ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER, 0) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID, 0) //
						.output(ControllerLevlEssBalancing.ChannelId.PRIMARY_USE_CASE_BATTERY_POWER, 18_000_000L) //
						.output(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, 118_000_000L) //
						.output(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, -18_000_000L)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.SOC, 5) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 18_000_000) //
						.input("ess0", ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER, 0) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID, 0) //
						.output(ControllerLevlEssBalancing.ChannelId.PRIMARY_USE_CASE_BATTERY_POWER, 18_000_000L) //
						.output(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, 136_000_000L) //
						.output(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, 0L)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.SOC, 5) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 18_000_000) //
						.input("ess0", ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER, 18_000_000) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID, 18_000_000) //
						.output(ControllerLevlEssBalancing.ChannelId.PRIMARY_USE_CASE_BATTERY_POWER, 18_000_000L) //
						.output(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, 136_000_000L) //
						.output(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, 0L)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.SOC, 4) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 18_000_000) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.input("ess0", ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER, 18_000_000) //
						.output("ess0", ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID, 18_000_000) //
						.output(ControllerLevlEssBalancing.ChannelId.PRIMARY_USE_CASE_BATTERY_POWER, 18_000_000L) //
						.output(ControllerLevlEssBalancing.ChannelId.REMAINING_LEVL_ENERGY, 136_000_000L) //
						.output(ControllerLevlEssBalancing.ChannelId.LEVL_STATE_OF_CHARGE, 0L)); //
	}
}
