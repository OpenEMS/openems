package io.openems.edge.levl.controller;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.DummyCycle;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class BalancingImplTest {

	private static final String CTRL_ID = "ctrl0";

	private static final String ESS_ID = "ess0";
	private static final ChannelAddress ESS_ACTIVE_POWER = new ChannelAddress(ESS_ID, "ActivePower");
	private static final ChannelAddress ESS_SOC = new ChannelAddress(ESS_ID, "Soc");
	private static final ChannelAddress ESS_SET_ACTIVE_POWER_EQUALS = new ChannelAddress(ESS_ID, "SetActivePowerEquals");
	private static final ChannelAddress ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID = new ChannelAddress(ESS_ID, "SetActivePowerEqualsWithPid");
	private static final ChannelAddress DEBUG_SET_ACTIVE_POWER = new ChannelAddress(ESS_ID, "DebugSetActivePower");
	
	private static final String METER_ID = "meter0";
	private static final ChannelAddress METER_ACTIVE_POWER = new ChannelAddress(METER_ID, "ActivePower");
	private static final ChannelAddress LEVL_REMAINING_LEVL_ENERGY = new ChannelAddress(CTRL_ID, "RemainingLevlEnergy");
	private static final ChannelAddress LEVL_SOC = new ChannelAddress(CTRL_ID, "LevlSoc");
	private static final ChannelAddress LEVL_SELL_TO_GRID_LIMIT = new ChannelAddress(CTRL_ID, "SellToGridLimit");
	private static final ChannelAddress LEVL_BUY_FROM_GRID_LIMIT = new ChannelAddress(CTRL_ID, "BuyFromGridLimit");
	private static final ChannelAddress SOC_LOWER_BOUND_LEVL = new ChannelAddress(CTRL_ID, "SocLowerBoundLevl");
	private static final ChannelAddress SOC_UPPER_BOUND_LEVL = new ChannelAddress(CTRL_ID, "SocUpperBoundLevl");
	private static final ChannelAddress LEVL_INFLUENCE_SELL_TO_GRID = new ChannelAddress(CTRL_ID, "InfluenceSellToGrid");
	private static final ChannelAddress LEVL_EFFICIENCY = new ChannelAddress(CTRL_ID, "Efficiency");
	private static final ChannelAddress PUC_BATTERY_POWER = new ChannelAddress(CTRL_ID, "PucBatteryPower");
	
	@Test
	public void testWithoutLevlRequest() throws Exception {
		new ControllerTest(new ControllerEssBalancingImpl())
				.addReference("cm", new DummyConfigurationAdmin())
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)
						.setPower(new DummyPower(0.3, 0.3, 0.1))
						.withCapacity(500000) // 1.800.000.000 Ws
						.withSoc(50) // 900.000.000 Ws
						.withMaxApparentPower(500000))
				.addReference("meter", new DummyElectricityMeter(METER_ID))
				.addReference("cycle", new DummyCycle(1000))
				.activate(MyConfig.create()
						.setId(CTRL_ID)
						.setEssId(ESS_ID)
						.setMeterId(METER_ID)
						.build())
				.next(new TestCase()
						.input(ESS_ACTIVE_POWER, 0)
						.input(METER_ACTIVE_POWER, 20000)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, 20000)
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 6000))
				.next(new TestCase()
						.input(ESS_ACTIVE_POWER, 0)
						.input(METER_ACTIVE_POWER, 20000)
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 12000))
				.next(new TestCase()
						.input(ESS_ACTIVE_POWER, 3793)
						.input(METER_ACTIVE_POWER, 20000 - 3793)
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 16483))
				.next(new TestCase()
						.input(ESS_ACTIVE_POWER, 8981)
						.input(METER_ACTIVE_POWER, 20000 - 8981)
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 19649))
				.next(new TestCase()
						.input(ESS_ACTIVE_POWER, 13723)
						.input(METER_ACTIVE_POWER, 20000 - 13723)
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 21577))
				.next(new TestCase()
						.input(ESS_ACTIVE_POWER, 17469)
						.input(METER_ACTIVE_POWER, 20000 - 17469)
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 22436))
				.next(new TestCase()
						.input(ESS_ACTIVE_POWER, 20066)
						.input(METER_ACTIVE_POWER, 20000 - 20066)
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 22531))
				.next(new TestCase()
						.input(ESS_ACTIVE_POWER, 21564)
						.input(METER_ACTIVE_POWER, 20000 - 21564)
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 22171))
				.next(new TestCase()
						.input(ESS_ACTIVE_POWER, 22175)
						.input(METER_ACTIVE_POWER, 20000 - 22175)
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 21608))
				.next(new TestCase()
						.input(ESS_ACTIVE_POWER, 22173)
						.input(METER_ACTIVE_POWER, 20000 - 22173)
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 21017))
				.next(new TestCase()
						.input(ESS_ACTIVE_POWER, 21816)
						.input(METER_ACTIVE_POWER, 20000 - 21816)
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 20508))
				.next(new TestCase()
						.input(ESS_ACTIVE_POWER, 21311)
						.input(METER_ACTIVE_POWER, 20000 - 21311)
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 20129))
				.next(new TestCase()
						.input(ESS_ACTIVE_POWER, 20803)
						.input(METER_ACTIVE_POWER, 20000 - 20803)
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 19889))
				.next(new TestCase()
						.input(ESS_ACTIVE_POWER, 20377)
						.input(METER_ACTIVE_POWER, 20000 - 20377)
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 19767));
	}
	
	@Test
	public void testWithLevlDischargeRequest() throws Exception {
		new ControllerTest(new ControllerEssBalancingImpl())
				.addReference("cm", new DummyConfigurationAdmin())
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)
						.setPower(new DummyPower(0.3, 0.3, 0.1))
						.withCapacity(500000) // 1.800.000.000 Ws
						.withSoc(50) // 900.000.000 Ws
						.withMaxApparentPower(500000))
				.addReference("meter", new DummyElectricityMeter(METER_ID))
				.addReference("cycle", new DummyCycle(1000))
				.addReference("currentRequest", new LevlControlRequest(0, 100))
				.activate(MyConfig.create()
						.setId(CTRL_ID)
						.setEssId(ESS_ID)
						.setMeterId(METER_ID)
						.build())
				.next(new TestCase()
						// following values have to be initialized in the first cycle
						.input(LEVL_SELL_TO_GRID_LIMIT, -100_000)
						.input(LEVL_BUY_FROM_GRID_LIMIT, 100_000)
						.input(SOC_LOWER_BOUND_LEVL, 0)
						.input(SOC_UPPER_BOUND_LEVL, 100)
						.input(LEVL_INFLUENCE_SELL_TO_GRID, true)
						.input(LEVL_EFFICIENCY, 80.0)
						.input(LEVL_REMAINING_LEVL_ENERGY, 10000)
						.input(LEVL_SOC, 100_000)
						// following values have to be updated each cycle
						.input(ESS_ACTIVE_POWER, 0)
						.input(METER_ACTIVE_POWER, 20000)
						.input(DEBUG_SET_ACTIVE_POWER, 30000)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, 30000)
						.output(PUC_BATTERY_POWER, 20000L)
						.output(LEVL_REMAINING_LEVL_ENERGY, 0L)
						.output(LEVL_SOC, 87500L))
				.next(new TestCase()
						.input(ESS_ACTIVE_POWER, 30000)
						.input(METER_ACTIVE_POWER, -10000)
						.input(DEBUG_SET_ACTIVE_POWER, 20000)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, 20000)
						.output(PUC_BATTERY_POWER, 20000L)
						.output(LEVL_REMAINING_LEVL_ENERGY, 0L)
						.output(LEVL_SOC, 87500L))
				.next(new TestCase()
						.input(ESS_ACTIVE_POWER, 20000)
						.input(METER_ACTIVE_POWER, 0)
						.input(DEBUG_SET_ACTIVE_POWER, 20000)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, 20000)
						.output(PUC_BATTERY_POWER, 20000L)
						.output(LEVL_REMAINING_LEVL_ENERGY, 0L)
						.output(LEVL_SOC, 87500L));
	}

	@Test
	public void testWithLevlChargeRequest() throws Exception {
		new ControllerTest(new ControllerEssBalancingImpl())
				.addReference("cm", new DummyConfigurationAdmin())
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)
						.setPower(new DummyPower(0.3, 0.3, 0.1))
						.withCapacity(500000) // 1.800.000.000 Ws
						.withSoc(50) // 900.000.000 Ws
						.withMaxApparentPower(500000))
				.addReference("meter", new DummyElectricityMeter(METER_ID))
				.addReference("cycle", new DummyCycle(1000))
				.addReference("currentRequest", new LevlControlRequest(0, 100))
				.activate(MyConfig.create()
						.setId(CTRL_ID)
						.setEssId(ESS_ID)
						.setMeterId(METER_ID)
						.build())
				.next(new TestCase()
						// following values have to be initialized in the first cycle
						.input(LEVL_SELL_TO_GRID_LIMIT, -100_000)
						.input(LEVL_BUY_FROM_GRID_LIMIT, 100_000)
						.input(SOC_LOWER_BOUND_LEVL, 0)
						.input(SOC_UPPER_BOUND_LEVL, 100)
						.input(LEVL_INFLUENCE_SELL_TO_GRID, true)
						.input(LEVL_EFFICIENCY, 80.0)
						.input(LEVL_REMAINING_LEVL_ENERGY, -10000)
						.input(LEVL_SOC, 100_000)
						// following values have to be updated each cycle
						.input(ESS_ACTIVE_POWER, 0)
						.input(METER_ACTIVE_POWER, 20000)
						.input(DEBUG_SET_ACTIVE_POWER, 10000)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, 10000)
						.output(PUC_BATTERY_POWER, 20000L)
						.output(LEVL_REMAINING_LEVL_ENERGY, 0L)
						.output(LEVL_SOC, 108000L))
				.next(new TestCase()
						.input(ESS_ACTIVE_POWER, 10000)
						.input(METER_ACTIVE_POWER, 10000)
						.input(DEBUG_SET_ACTIVE_POWER, 20000)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, 20000)
						.output(PUC_BATTERY_POWER, 20000L)
						.output(LEVL_REMAINING_LEVL_ENERGY, 0L)
						.output(LEVL_SOC, 108000L))
				.next(new TestCase()
						.input(ESS_ACTIVE_POWER, 20000)
						.input(METER_ACTIVE_POWER, 0)
						.input(DEBUG_SET_ACTIVE_POWER, 20000)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, 20000)
						.output(PUC_BATTERY_POWER, 20000L)
						.output(LEVL_REMAINING_LEVL_ENERGY, 0L)
						.output(LEVL_SOC, 108000L));
	}
	
	// Test with discharge request (ws) > MAX_INT. Constrained by sell to grid limit.
	@Test
	public void testWithLargeLevlDischargeRequest() throws Exception {
		new ControllerTest(new ControllerEssBalancingImpl())
				.addReference("cm", new DummyConfigurationAdmin())
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)
						.setPower(new DummyPower(0.3, 0.3, 0.1))
						.withCapacity(500000) // 1.800.000.000 Ws
						.withSoc(50) // 900.000.000 Ws
						.withMaxApparentPower(500000))
				.addReference("meter", new DummyElectricityMeter(METER_ID))
				.addReference("cycle", new DummyCycle(1000))
				.addReference("currentRequest", new LevlControlRequest(0, 100))
				.activate(MyConfig.create()
						.setId(CTRL_ID)
						.setEssId(ESS_ID)
						.setMeterId(METER_ID)
						.build())
				.next(new TestCase()
						// following values have to be initialized in the first cycle
						.input(LEVL_SELL_TO_GRID_LIMIT, -100_000)
						.input(LEVL_BUY_FROM_GRID_LIMIT, 100_000)
						.input(SOC_LOWER_BOUND_LEVL, 0)
						.input(SOC_UPPER_BOUND_LEVL, 100)
						.input(LEVL_INFLUENCE_SELL_TO_GRID, true)
						.input(LEVL_EFFICIENCY, 80.0)
						.input(LEVL_SOC, 100_000)
						.input(LEVL_REMAINING_LEVL_ENERGY, 2_500_000_000L)
						// following values have to be updated each cycle
						.input(ESS_ACTIVE_POWER, 0)
						.input(METER_ACTIVE_POWER, 20000)
						.input(DEBUG_SET_ACTIVE_POWER, 120000)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, 120000)
						.output(PUC_BATTERY_POWER, 20000L)
						.output(LEVL_REMAINING_LEVL_ENERGY, 2_499_900_000L)
						.output(LEVL_SOC, -25000L))
				.next(new TestCase()
						.input(ESS_ACTIVE_POWER, 120000)
						.input(METER_ACTIVE_POWER, -100000)
						.input(DEBUG_SET_ACTIVE_POWER, 120000)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, 120000)
						.output(PUC_BATTERY_POWER, 20000L)
						.output(LEVL_REMAINING_LEVL_ENERGY, 2_499_800_000L)
						.output(LEVL_SOC, -150000L))
				.next(new TestCase()
						.input(ESS_ACTIVE_POWER, 120000)
						.input(METER_ACTIVE_POWER, -100000)
						.input(DEBUG_SET_ACTIVE_POWER, 120000)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, 120000)
						.output(PUC_BATTERY_POWER, 20000L)
						.output(LEVL_REMAINING_LEVL_ENERGY, 2_499_700_000L)
						.output(LEVL_SOC, -275000L));
	}

	@Test
	public void testWithReservedChargeCapacityLevlChargesPucMustNotCharge() throws Exception {
		new ControllerTest(new ControllerEssBalancingImpl())
				.addReference("cm", new DummyConfigurationAdmin())
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)
						.setPower(new DummyPower(0.3, 0.3, 0.1))
						.withCapacity(500000) // 1.800.000.000 Ws
						.withMaxApparentPower(500000))
				.addReference("meter", new DummyElectricityMeter(METER_ID))
				.addReference("cycle", new DummyCycle(1000))
				.addReference("currentRequest", new LevlControlRequest(0, 100))
				.activate(MyConfig.create()
						.setId(CTRL_ID)
						.setEssId(ESS_ID)
						.setMeterId(METER_ID)
						.build())
				.next(new TestCase()
						// following values have to be initialized in the first cycle
						.input(LEVL_SELL_TO_GRID_LIMIT, -800000)
						.input(LEVL_BUY_FROM_GRID_LIMIT, 800000)
						.input(SOC_LOWER_BOUND_LEVL, 0)
						.input(SOC_UPPER_BOUND_LEVL, 100)
						.input(LEVL_INFLUENCE_SELL_TO_GRID, true)
						.input(LEVL_EFFICIENCY, 80.0)
						.input(LEVL_REMAINING_LEVL_ENERGY, -10_000_000) 
						.input(LEVL_SOC, -180_000_000) // 10% of total capacity
						// following values have to be updated each cycle
						.input(ESS_SOC, 90) // 90% = 450,000 Wh = 1,620,000.000 Ws
						.input(ESS_ACTIVE_POWER, 0)
						.input(METER_ACTIVE_POWER, -20_000) // grid power w/o Levl --> sell to grid
						.input(DEBUG_SET_ACTIVE_POWER, -500_000)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, -500_000)
						.output(PUC_BATTERY_POWER, 0L) // puc should not do anything because capacity is completely reserved for Levl
						.output(LEVL_REMAINING_LEVL_ENERGY, -9_500_000L) // 500,000 Ws should be realized, therefore 9,500,000 Ws are remaining
						.output(LEVL_SOC, -179_600_000L))  // Levl soc increases by 500,000 Ws * 80% efficiency = 400,000 Ws
				.next(new TestCase()
						.input(ESS_SOC, 90) // 90% = 450,000 Wh = 1,620,000,000 Ws | should be 1,620,400,000 Ws but only full percent values can be read
						.input(ESS_ACTIVE_POWER, -500_000)
						.input(METER_ACTIVE_POWER, 480_000) // grid power ceteris paribus w/ Levl
						.input(DEBUG_SET_ACTIVE_POWER, -500_000) 
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, -500_000)
						.output(PUC_BATTERY_POWER, -20_000L) // since reserved capacity decreased by 400,000 Ws in the previous cycle but ess soc value remains the same, puc can charge again
						.output(LEVL_REMAINING_LEVL_ENERGY, -9_020_000L) // 480,000 Ws can be realized for Levl, therefore 9,020,00 are remaining
						.output(LEVL_SOC, -179_216_000L)); // Levl soc increases by 480,000 Ws * 80% efficiency = 384,000 Ws
	}
	
	@Test
	public void testWithReservedChargeCapacityLevlDischargesPucMustNotCharge() throws Exception {
		new ControllerTest(new ControllerEssBalancingImpl())
				.addReference("cm", new DummyConfigurationAdmin())
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)
						.setPower(new DummyPower(0.3, 0.3, 0.1))
						.withCapacity(500_000) // 1,800,000,000 Ws
						.withMaxApparentPower(500_000))
				.addReference("meter", new DummyElectricityMeter(METER_ID))
				.addReference("cycle", new DummyCycle(1000))
				.addReference("currentRequest", new LevlControlRequest(0, 100))
				.activate(MyConfig.create()
						.setId(CTRL_ID)
						.setEssId(ESS_ID)
						.setMeterId(METER_ID)
						.build())
				.next(new TestCase()
						// following values have to be initialized in the first cycle
						.input(LEVL_SELL_TO_GRID_LIMIT, -800_000)
						.input(LEVL_BUY_FROM_GRID_LIMIT, 800_000)
						.input(SOC_LOWER_BOUND_LEVL, 0)
						.input(SOC_UPPER_BOUND_LEVL, 100)
						.input(LEVL_INFLUENCE_SELL_TO_GRID, true)
						.input(LEVL_EFFICIENCY, 80.0)
						.input(LEVL_REMAINING_LEVL_ENERGY, 10_000_000)
						.input(LEVL_SOC, -180_000_000) // 10% of total capacity
						// following values have to be updated each cycle
						.input(ESS_SOC, 90) // 90% = 450,000 Wh = 1,620,000,000 Ws
						.input(ESS_ACTIVE_POWER, 0)
						.input(METER_ACTIVE_POWER, -20_000) // grid power w/o Levl --> sell to grid
						.input(DEBUG_SET_ACTIVE_POWER, 500_000) // max discharge power of 500,000 W should be applied
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, 500_000)
						.output(PUC_BATTERY_POWER, 0L) // puc should not do anything because capacity is completely reserved for Levl
						.output(LEVL_REMAINING_LEVL_ENERGY, 9_500_000L) // 500,000 Ws should be realized, therefore 9,500,000 Ws are remaining
						.output(LEVL_SOC, -180_625_000L)) // Levl soc decreases by 500,000 Ws / 80% efficiency = 625,000 Ws
				.next(new TestCase()
						.input(ESS_SOC, 90) // 90% = 450,000 Wh = 1,620,000,000 Ws | should be 1,619,375,000 Ws but only full percent values can be read
						.input(ESS_ACTIVE_POWER, 500_000)
						.input(METER_ACTIVE_POWER, -520_000) // grid power ceteris paribus w/ Levl
						.input(DEBUG_SET_ACTIVE_POWER, 500_000) // max discharge power of 500,000 W should be applied
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, 500_000)
						.output(PUC_BATTERY_POWER, 0L) // // puc should not do anything because capacity is still completely reserved for Levl
						.output(LEVL_REMAINING_LEVL_ENERGY, 9_000_000L) // 500,000 Ws should be realized, therefore 9,000,000 Ws are remaining
						.output(LEVL_SOC, -181_250_000L)); // Levl soc decreases by 500,000 Ws / 80% efficiency = 625,000 Ws
	}
	
	
	@Test
	public void testWithReservedChargeCapacityLevlChargesPucMayCharge() throws Exception {
		new ControllerTest(new ControllerEssBalancingImpl())
				.addReference("cm", new DummyConfigurationAdmin())
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)
						.setPower(new DummyPower(0.3, 0.3, 0.1))
						.withCapacity(500_000) // 1,800,000,000 Ws
						.withMaxApparentPower(500_000))
				.addReference("meter", new DummyElectricityMeter(METER_ID))
				.addReference("cycle", new DummyCycle(1000))
				.addReference("currentRequest", new LevlControlRequest(0, 100))
				.activate(MyConfig.create()
						.setId(CTRL_ID)
						.setEssId(ESS_ID)
						.setMeterId(METER_ID)
						.build())
				.next(new TestCase()
						// following values have to be initialized in the first cycle
						.input(LEVL_SELL_TO_GRID_LIMIT, -800_000)
						.input(LEVL_BUY_FROM_GRID_LIMIT, 800_000)
						.input(SOC_LOWER_BOUND_LEVL, 0)
						.input(SOC_UPPER_BOUND_LEVL, 100)
						.input(LEVL_INFLUENCE_SELL_TO_GRID, true)
						.input(LEVL_EFFICIENCY, 80.0)
						.input(LEVL_REMAINING_LEVL_ENERGY, -10_000_000)
						.input(LEVL_SOC, -180_000_000) // 10% of total capacity
						// following values have to be updated each cycle
						.input(ESS_SOC, 85) // 85% = 425,000 Wh = 1,530,000,000 Ws
						.input(ESS_ACTIVE_POWER, 0)
						.input(METER_ACTIVE_POWER, -10_000) // grid power w/o Levl --> sell to grid
						.input(DEBUG_SET_ACTIVE_POWER, -500_000) // max charge power of 500,000 W should be applied
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, -500_000)
						.output(PUC_BATTERY_POWER, -10_000L) // puc should charge 10,000 Ws since 5% capacity is available
						.output(LEVL_REMAINING_LEVL_ENERGY, -9_510_000L) // 490,000 Ws should be realized, therefore 9,510,000 Ws are remaining
						.output(LEVL_SOC, -179_608_000L)) // Levl soc increases by 490,000 Ws * 80% efficiency = 392,000 Ws
				.next(new TestCase()
						.input(ESS_SOC, 85) // 85% = 425,000 Wh = 1,530,000,000 Ws | should be 1,530,400,000 Ws but only full percent values can be read
						.input(ESS_ACTIVE_POWER, -500_000)
						.input(METER_ACTIVE_POWER, 490_000) // grid power ceteris paribus w/ Levl
						.input(DEBUG_SET_ACTIVE_POWER, -500_000) // max charge power of 500,000 W should be applied
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, -500_000)
						.output(PUC_BATTERY_POWER, -10_000L) // puc should still charge 10,000 Ws
						.output(LEVL_REMAINING_LEVL_ENERGY, -9_020_000L) // 490,000 Ws should be realized, therefore 9,020,000 Ws are remaining
						.output(LEVL_SOC, -179_216_000L)); // Levl soc increases by 490,000 Ws * 80% efficiency = 392,000 Ws
	}
	
	@Test
	public void testWithReservedChargeCapacityLevlChargesPucMayDischarge() throws Exception {
		new ControllerTest(new ControllerEssBalancingImpl())
				.addReference("cm", new DummyConfigurationAdmin())
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)
						.setPower(new DummyPower(0.3, 0.3, 0.1))
						.withCapacity(500_000) // 1,800,000,000 Ws
						.withMaxApparentPower(500_000))
				.addReference("meter", new DummyElectricityMeter(METER_ID))
				.addReference("cycle", new DummyCycle(1000))
				.addReference("currentRequest", new LevlControlRequest(0, 100))
				.activate(MyConfig.create()
						.setId(CTRL_ID)
						.setEssId(ESS_ID)
						.setMeterId(METER_ID)
						.build())
				.next(new TestCase()
						// following values have to be initialized in the first cycle
						.input(LEVL_SELL_TO_GRID_LIMIT, -800_000)
						.input(LEVL_BUY_FROM_GRID_LIMIT, 800_000)
						.input(SOC_LOWER_BOUND_LEVL, 0)
						.input(SOC_UPPER_BOUND_LEVL, 100)
						.input(LEVL_INFLUENCE_SELL_TO_GRID, true)
						.input(LEVL_EFFICIENCY, 80.0)
						.input(LEVL_REMAINING_LEVL_ENERGY, -10_000_000)
						.input(LEVL_SOC, -180_000_000) // 10% of total capacity
						// following values have to be updated each cycle
						.input(ESS_SOC, 50) // 50% = 250,000 Wh = 900,000,000 Ws
						.input(ESS_ACTIVE_POWER, 0)
						.input(METER_ACTIVE_POWER, 10_000) // grid power w/o Levl --> buy from grid
						.input(DEBUG_SET_ACTIVE_POWER, -500_000) // max charge power of 500,000 W should be applied
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, -500_000)
						.output(PUC_BATTERY_POWER, 10_000L) // puc should discharge 10,000 Ws since Levl has reserved charge not discharge energy
						.output(LEVL_REMAINING_LEVL_ENERGY, -9_490_000L) // 510,000 Ws can be realized, because puc discharges 10,000 Ws
						.output(LEVL_SOC, -179_592_000L)) // Levl soc increases by 510,000 Ws * 80% efficiency = 408,000 Ws
				.next(new TestCase()
						.input(ESS_SOC, 50) // 50% = 250,000 Wh = 900,000,000 Ws | should be 900,400,000 Ws but only full percent values can be read
						.input(ESS_ACTIVE_POWER, -500_000)
						.input(METER_ACTIVE_POWER, 510_000) // grid power ceteris paribus w/ Levl
						.input(DEBUG_SET_ACTIVE_POWER, -500_000) // max charge power of 500,000 W should be applied
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, -500_000)
						.output(PUC_BATTERY_POWER, 10_000L) // puc should still charge 10,000 Ws
						.output(LEVL_REMAINING_LEVL_ENERGY, -8_980_000L) // 510,000 Ws can be realized again, therefore 8,980,000 Ws are remaining
						.output(LEVL_SOC, -179_184_000L)); // Levl soc increases by 510,000 Ws * 80% efficiency = 408,000 Ws
	}
	
	
	@Test
	public void testInfluenceSellToGrid_PucSellToGrid_LevlChargeForbidden() throws Exception {
		new ControllerTest(new ControllerEssBalancingImpl())
				.addReference("cm", new DummyConfigurationAdmin())
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)
						.setPower(new DummyPower(0.3, 0.3, 0.1))
						.withCapacity(500000) // 1.800.000.000 Ws
						.withMaxApparentPower(500000))
				.addReference("meter", new DummyElectricityMeter(METER_ID))
				.addReference("cycle", new DummyCycle(1000))
				.addReference("currentRequest", new LevlControlRequest(0, 100))
				.activate(MyConfig.create()
						.setId(CTRL_ID)
						.setEssId(ESS_ID)
						.setMeterId(METER_ID)
						.build())
				.next(new TestCase("puc sell to grid, levl charge not allowed")
						.input(LEVL_SELL_TO_GRID_LIMIT, -100_000)
						.input(LEVL_BUY_FROM_GRID_LIMIT, 100_000)
						.input(SOC_LOWER_BOUND_LEVL, 0)
						.input(SOC_UPPER_BOUND_LEVL, 100)
						.input(LEVL_INFLUENCE_SELL_TO_GRID, false)
						.input(LEVL_EFFICIENCY, 80.0)
						.input(LEVL_REMAINING_LEVL_ENERGY, -10000L)
						.input(LEVL_SOC, -180_000_000)
						.input(ESS_SOC, 90)
						.input(ESS_ACTIVE_POWER, 0)
						.input(METER_ACTIVE_POWER, -20000)
						.input(DEBUG_SET_ACTIVE_POWER, 0)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, 0)
						.output(PUC_BATTERY_POWER, 0L)
						.output(LEVL_REMAINING_LEVL_ENERGY, -10000L)
						.output(LEVL_SOC, -180_000_000L));
	}
	
	@Test
	public void testInfluenceSellToGrid_PucSellToGrid_LevlDischargeForbidden() throws Exception {
		new ControllerTest(new ControllerEssBalancingImpl())
				.addReference("cm", new DummyConfigurationAdmin())
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)
						.setPower(new DummyPower(0.3, 0.3, 0.1))
						.withCapacity(500000) // 1.800.000.000 Ws
						.withMaxApparentPower(500000))
				.addReference("meter", new DummyElectricityMeter(METER_ID))
				.addReference("cycle", new DummyCycle(1000))
				.addReference("currentRequest", new LevlControlRequest(0, 100))
				.activate(MyConfig.create()
						.setId(CTRL_ID)
						.setEssId(ESS_ID)
						.setMeterId(METER_ID)
						.build())
				.next(new TestCase("puc sell to grid, levl discharge not allowed")
						.input(LEVL_SELL_TO_GRID_LIMIT, -100_000)
						.input(LEVL_BUY_FROM_GRID_LIMIT, 100_000)
						.input(SOC_LOWER_BOUND_LEVL, 0)
						.input(SOC_UPPER_BOUND_LEVL, 100)
						.input(LEVL_INFLUENCE_SELL_TO_GRID, false)
						.input(LEVL_EFFICIENCY, 80.0)
						.input(LEVL_REMAINING_LEVL_ENERGY, 10000L)
						.input(LEVL_SOC, -180_000_000)
						.input(ESS_SOC, 90)
						.input(ESS_ACTIVE_POWER, 0)
						.input(METER_ACTIVE_POWER, -20000)
						.input(DEBUG_SET_ACTIVE_POWER, 0)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, 0)
						.output(PUC_BATTERY_POWER, 0L)
						.output(LEVL_REMAINING_LEVL_ENERGY, 10000L)
						.output(LEVL_SOC, -180_000_000L));
	}

	@Test
	public void testInfluenceSellToGrid_PucBuyFromGrid_LevlChargeAllowed() throws Exception {
		new ControllerTest(new ControllerEssBalancingImpl())
				.addReference("cm", new DummyConfigurationAdmin())
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)
						.setPower(new DummyPower(0.3, 0.3, 0.1))
						.withCapacity(500000) // 1.800.000.000 Ws
						.withMaxApparentPower(500000))
				.addReference("meter", new DummyElectricityMeter(METER_ID))
				.addReference("cycle", new DummyCycle(1000))
				.addReference("currentRequest", new LevlControlRequest(0, 100))
				.activate(MyConfig.create()
						.setId(CTRL_ID)
						.setEssId(ESS_ID)
						.setMeterId(METER_ID)
						.build())
				.next(new TestCase("puc buy from grid, levl charge is allowed")
						.input(LEVL_SELL_TO_GRID_LIMIT, -100_000)
						.input(LEVL_BUY_FROM_GRID_LIMIT, 100_000)
						.input(SOC_LOWER_BOUND_LEVL, 0)
						.input(SOC_UPPER_BOUND_LEVL, 100)
						.input(LEVL_INFLUENCE_SELL_TO_GRID, false)
						.input(LEVL_EFFICIENCY, 80.0)
						.input(LEVL_REMAINING_LEVL_ENERGY, -30000L)
						.input(LEVL_SOC, 0)
						.input(ESS_SOC, 0)
						.input(ESS_ACTIVE_POWER, 0)
						.input(METER_ACTIVE_POWER, 20000)
						.input(DEBUG_SET_ACTIVE_POWER, -30000)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, -30000)
						.output(PUC_BATTERY_POWER, 0L)
						.output(LEVL_REMAINING_LEVL_ENERGY, 0L)
						.output(LEVL_SOC, 24000L));
	}	
	
	@Test
	public void testInfluenceSellToGrid_PucBuyFromGrid_LevlDischargeLimited() throws Exception {
		new ControllerTest(new ControllerEssBalancingImpl())
				.addReference("cm", new DummyConfigurationAdmin())
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)
						.setPower(new DummyPower(0.3, 0.3, 0.1))
						.withCapacity(500000) // 1.800.000.000 Ws
						.withMaxApparentPower(500000))
				.addReference("meter", new DummyElectricityMeter(METER_ID))
				.addReference("cycle", new DummyCycle(1000))
				.addReference("currentRequest", new LevlControlRequest(0, 100))
				.activate(MyConfig.create()
						.setId(CTRL_ID)
						.setEssId(ESS_ID)
						.setMeterId(METER_ID)
						.build())
				.next(new TestCase("puc buy from grid, levl discharge is limited to grid limit 0")
						.input(LEVL_SELL_TO_GRID_LIMIT, -100_000)
						.input(LEVL_BUY_FROM_GRID_LIMIT, 100_000)
						.input(SOC_LOWER_BOUND_LEVL, 0)
						.input(SOC_UPPER_BOUND_LEVL, 100)
						.input(LEVL_INFLUENCE_SELL_TO_GRID, false)
						.input(LEVL_EFFICIENCY, 100.0)
						.input(LEVL_REMAINING_LEVL_ENERGY, 30000L)
						.input(LEVL_SOC, 180_000_000L)
						.input(ESS_SOC, 10)
						.input(ESS_ACTIVE_POWER, 0)
						.input(METER_ACTIVE_POWER, 20000)
						.input(DEBUG_SET_ACTIVE_POWER, 20000)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, 20000)
						.output(PUC_BATTERY_POWER, 0L)
						.output(LEVL_REMAINING_LEVL_ENERGY, 10000L)
						.output(LEVL_SOC, 179_980_000L));
	}
	
	@Test
	public void testUpperSocLimit() throws Exception {
		new ControllerTest(new ControllerEssBalancingImpl())
				.addReference("cm", new DummyConfigurationAdmin())
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)
						.setPower(new DummyPower(0.3, 0.3, 0.1))
						.withCapacity(500000) // 1.800.000.000 Ws
						.withMaxApparentPower(20_000_000))
				.addReference("meter", new DummyElectricityMeter(METER_ID))
				.addReference("cycle", new DummyCycle(1000))
				.addReference("currentRequest", new LevlControlRequest(0, 100))
				.activate(MyConfig.create()
						.setId(CTRL_ID)
						.setEssId(ESS_ID)
						.setMeterId(METER_ID)
						.build())
				.next(new TestCase()
						// following values have to be initialized in the first cycle
						.input(LEVL_SELL_TO_GRID_LIMIT, -40_000_000)
						.input(LEVL_BUY_FROM_GRID_LIMIT, 40_000_000)
						.input(SOC_LOWER_BOUND_LEVL, 20)
						.input(SOC_UPPER_BOUND_LEVL, 80)
						.input(LEVL_INFLUENCE_SELL_TO_GRID, true)
						.input(LEVL_EFFICIENCY, 100.0)
						.input(LEVL_REMAINING_LEVL_ENERGY, -100_000_000)
						.input(LEVL_SOC, 0)
						// following values have to be updated each cycle
						.input(ESS_SOC, 79)
						.input(ESS_ACTIVE_POWER, 0)
						.input(METER_ACTIVE_POWER, 0)
						.input(DEBUG_SET_ACTIVE_POWER, -18_000_000)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, -18_000_000)
						.output(PUC_BATTERY_POWER, -0L)
						.output(LEVL_REMAINING_LEVL_ENERGY, -82_000_000L)
						.output(LEVL_SOC, 18_000_000L))
				.next(new TestCase()
						.input(ESS_SOC, 80)
						.input(ESS_ACTIVE_POWER, -18_000_000)
						.input(METER_ACTIVE_POWER, 18_000_000)
						.input(DEBUG_SET_ACTIVE_POWER, 0)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, 0)
						.output(PUC_BATTERY_POWER, 0L)
						.output(LEVL_REMAINING_LEVL_ENERGY, -82_000_000L)
						.output(LEVL_SOC, 18_000_000L))
				.next(new TestCase()
						.input(ESS_SOC, 80)
						.input(ESS_ACTIVE_POWER, 0)
						.input(METER_ACTIVE_POWER, 0)
						.input(DEBUG_SET_ACTIVE_POWER, 0)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, 0)
						.output(PUC_BATTERY_POWER, 0L)
						.output(LEVL_REMAINING_LEVL_ENERGY, -82_000_000L)
						.output(LEVL_SOC, 18_000_000L));
	}
	
	@Test
	public void testUpperSocLimit_levlHasCharged() throws Exception {
		new ControllerTest(new ControllerEssBalancingImpl())
				.addReference("cm", new DummyConfigurationAdmin())
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)
						.setPower(new DummyPower(0.3, 0.3, 0.1))
						.withCapacity(500000) // 1.800.000.000 Ws
						.withMaxApparentPower(30_000_000))
				.addReference("meter", new DummyElectricityMeter(METER_ID))
				.addReference("cycle", new DummyCycle(1000))
				.addReference("currentRequest", new LevlControlRequest(0, 100))
				.activate(MyConfig.create()
						.setId(CTRL_ID)
						.setEssId(ESS_ID)
						.setMeterId(METER_ID)
						.build())
				.next(new TestCase()
						// following values have to be initialized in the first cycle
						.input(LEVL_SELL_TO_GRID_LIMIT, -40_000_000)
						.input(LEVL_BUY_FROM_GRID_LIMIT, 40_000_000)
						.input(SOC_LOWER_BOUND_LEVL, 5)
						.input(SOC_UPPER_BOUND_LEVL, 95)
						.input(LEVL_INFLUENCE_SELL_TO_GRID, true)
						.input(LEVL_EFFICIENCY, 100.0)
						.input(LEVL_REMAINING_LEVL_ENERGY, -100_000_000)
						.input(LEVL_SOC, 36_000_000) // 2%
						// following values have to be updated each cycle
						.input(ESS_SOC, 94)
						.input(ESS_ACTIVE_POWER, 0)
						.input(METER_ACTIVE_POWER, -18_000_000)
						.input(DEBUG_SET_ACTIVE_POWER, -18_000_000)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, -18_000_000)
						.output(PUC_BATTERY_POWER, -18_000_000L)
						.output(LEVL_REMAINING_LEVL_ENERGY, -100_000_000L)
						.output(LEVL_SOC, 36_000_000L))
				.next(new TestCase()
						.input(ESS_SOC, 95)
						.input(ESS_ACTIVE_POWER, -18_000_000)
						.input(METER_ACTIVE_POWER, 0)
						.input(DEBUG_SET_ACTIVE_POWER, 0)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, 0)
						.output(PUC_BATTERY_POWER, -18_000_000L)
						.output(LEVL_REMAINING_LEVL_ENERGY, -118_000_000L)
						.output(LEVL_SOC, 18_000_000L))
				.next(new TestCase()
						.input(ESS_SOC, 95)
						.input(ESS_ACTIVE_POWER, 0)
						.input(METER_ACTIVE_POWER, -18_000_000)
						.input(DEBUG_SET_ACTIVE_POWER, 0)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, 0)
						.output(PUC_BATTERY_POWER, -18_000_000L)
						.output(LEVL_REMAINING_LEVL_ENERGY, -136_000_000L)
						.output(LEVL_SOC, 0L))
				.next(new TestCase()
						.input(ESS_SOC, 95)
						.input(ESS_ACTIVE_POWER, 0)
						.input(METER_ACTIVE_POWER, -18_000_000)
						.input(DEBUG_SET_ACTIVE_POWER, -18_000_000)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, -18_000_000)
						.output(PUC_BATTERY_POWER, -18_000_000L)
						.output(LEVL_REMAINING_LEVL_ENERGY, -136_000_000L)
						.output(LEVL_SOC, 0L))
				.next(new TestCase()
						.input(ESS_SOC, 96)
						.input(ESS_ACTIVE_POWER, -18_000_000)
						.input(METER_ACTIVE_POWER, 0)
						.input(DEBUG_SET_ACTIVE_POWER, -18_000_000)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, -18_000_000)
						.output(PUC_BATTERY_POWER, -18_000_000L)
						.output(LEVL_REMAINING_LEVL_ENERGY, -136_000_000L)
						.output(LEVL_SOC, 0L));
	}

	@Test
	public void testLowerSocLimit() throws Exception {
		new ControllerTest(new ControllerEssBalancingImpl())
				.addReference("cm", new DummyConfigurationAdmin())
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)
						.setPower(new DummyPower(0.3, 0.3, 0.1))
						.withCapacity(500000) // 1.800.000.000 Ws
						.withMaxApparentPower(20_000_000))
				.addReference("meter", new DummyElectricityMeter(METER_ID))
				.addReference("cycle", new DummyCycle(1000))
				.addReference("currentRequest", new LevlControlRequest(0, 100))
				.activate(MyConfig.create()
						.setId(CTRL_ID)
						.setEssId(ESS_ID)
						.setMeterId(METER_ID)
						.build())
				.next(new TestCase()
						// following values have to be initialized in the first cycle
						.input(LEVL_SELL_TO_GRID_LIMIT, -40_000_000)
						.input(LEVL_BUY_FROM_GRID_LIMIT, 40_000_000)
						.input(SOC_LOWER_BOUND_LEVL, 20)
						.input(SOC_UPPER_BOUND_LEVL, 80)
						.input(LEVL_INFLUENCE_SELL_TO_GRID, true)
						.input(LEVL_EFFICIENCY, 100.0)
						.input(LEVL_REMAINING_LEVL_ENERGY, 100_000_000)
						.input(LEVL_SOC, 0)
						// following values have to be updated each cycle
						.input(ESS_SOC, 21)
						.input(ESS_ACTIVE_POWER, 0)
						.input(METER_ACTIVE_POWER, 0)
						.input(DEBUG_SET_ACTIVE_POWER, 18_000_000)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, 18_000_000)
						.output(PUC_BATTERY_POWER, -0L)
						.output(LEVL_REMAINING_LEVL_ENERGY, 82_000_000L)
						.output(LEVL_SOC, -18_000_000L))
				.next(new TestCase()
						.input(ESS_SOC, 20)
						.input(ESS_ACTIVE_POWER, 18_000_000)
						.input(METER_ACTIVE_POWER, -18_000_000)
						.input(DEBUG_SET_ACTIVE_POWER, 0)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, 0)
						.output(PUC_BATTERY_POWER, 0L)
						.output(LEVL_REMAINING_LEVL_ENERGY, 82_000_000L)
						.output(LEVL_SOC, -18_000_000L))
				.next(new TestCase()
						.input(ESS_SOC, 20)
						.input(ESS_ACTIVE_POWER, 0)
						.input(METER_ACTIVE_POWER, 0)
						.input(DEBUG_SET_ACTIVE_POWER, 0)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, 0)
						.output(PUC_BATTERY_POWER, 0L)
						.output(LEVL_REMAINING_LEVL_ENERGY, 82_000_000L)
						.output(LEVL_SOC, -18_000_000L));
	}

	@Test
	public void testLowerSocLimit_levlHasDischarged() throws Exception {
		new ControllerTest(new ControllerEssBalancingImpl())
				.addReference("cm", new DummyConfigurationAdmin())
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)
						.setPower(new DummyPower(0.3, 0.3, 0.1))
						.withCapacity(500000) // 1.800.000.000 Ws
						.withMaxApparentPower(30_000_000))
				.addReference("meter", new DummyElectricityMeter(METER_ID))
				.addReference("cycle", new DummyCycle(1000))
				.addReference("currentRequest", new LevlControlRequest(0, 100))
				.activate(MyConfig.create()
						.setId(CTRL_ID)
						.setEssId(ESS_ID)
						.setMeterId(METER_ID)
						.build())
				.next(new TestCase()
						// following values have to be initialized in the first cycle
						.input(LEVL_SELL_TO_GRID_LIMIT, -40_000_000)
						.input(LEVL_BUY_FROM_GRID_LIMIT, 40_000_000)
						.input(SOC_LOWER_BOUND_LEVL, 5)
						.input(SOC_UPPER_BOUND_LEVL, 95)
						.input(LEVL_INFLUENCE_SELL_TO_GRID, true)
						.input(LEVL_EFFICIENCY, 100.0)
						.input(LEVL_REMAINING_LEVL_ENERGY, 100_000_000)
						.input(LEVL_SOC, -36_000_000) // 2%
						// following values have to be updated each cycle
						.input(ESS_SOC, 6)
						.input(ESS_ACTIVE_POWER, 0)
						.input(METER_ACTIVE_POWER, 18_000_000)
						.input(DEBUG_SET_ACTIVE_POWER, 18_000_000)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, 18_000_000)
						.output(PUC_BATTERY_POWER, 18_000_000L)
						.output(LEVL_REMAINING_LEVL_ENERGY, 100_000_000L)
						.output(LEVL_SOC, -36_000_000L))
				.next(new TestCase()
						.input(ESS_SOC, 5)
						.input(ESS_ACTIVE_POWER, 18_000_000)
						.input(METER_ACTIVE_POWER, 0)
						.input(DEBUG_SET_ACTIVE_POWER, 0)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, 0)
						.output(PUC_BATTERY_POWER, 18_000_000L)
						.output(LEVL_REMAINING_LEVL_ENERGY, 118_000_000L)
						.output(LEVL_SOC, -18_000_000L))
				.next(new TestCase()
						.input(ESS_SOC, 5)
						.input(ESS_ACTIVE_POWER, 0)
						.input(METER_ACTIVE_POWER, 18_000_000)
						.input(DEBUG_SET_ACTIVE_POWER, 0)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, 0)
						.output(PUC_BATTERY_POWER, 18_000_000L)
						.output(LEVL_REMAINING_LEVL_ENERGY, 136_000_000L)
						.output(LEVL_SOC, 0L))
				.next(new TestCase()
						.input(ESS_SOC, 5)
						.input(ESS_ACTIVE_POWER, 0)
						.input(METER_ACTIVE_POWER, 18_000_000)
						.input(DEBUG_SET_ACTIVE_POWER, 18_000_000)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, 18_000_000)
						.output(PUC_BATTERY_POWER, 18_000_000L)
						.output(LEVL_REMAINING_LEVL_ENERGY, 136_000_000L)
						.output(LEVL_SOC, 0L))
				.next(new TestCase()
						.input(ESS_SOC, 4)
						.input(ESS_ACTIVE_POWER, 18_000_000)
						.input(METER_ACTIVE_POWER, 0)
						.input(DEBUG_SET_ACTIVE_POWER, 18_000_000)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, 18_000_000)
						.output(PUC_BATTERY_POWER, 18_000_000L)
						.output(LEVL_REMAINING_LEVL_ENERGY, 136_000_000L)
						.output(LEVL_SOC, 0L));
	}
}
