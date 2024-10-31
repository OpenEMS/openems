package io.openems.edge.levl.controller;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.DummyCycle;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.levl.controller.ControllerEssBalancingImpl;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class BalancingImplTest {
	
	

	private static final String CTRL_ID = "ctrl0";

	private static final String ESS_ID = "ess0";
	private static final ChannelAddress ESS_ACTIVE_POWER = new ChannelAddress(ESS_ID, "ActivePower");
	private static final ChannelAddress ESS_SOC = new ChannelAddress(ESS_ID, "Soc");
	private static final ChannelAddress ESS_SET_ACTIVE_POWER_EQUALS = new ChannelAddress(ESS_ID,
			"SetActivePowerEquals");
	private static final ChannelAddress ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID = new ChannelAddress(ESS_ID,
			"SetActivePowerEqualsWithPid");
	private static final ChannelAddress DEBUG_SET_ACTIVE_POWER = new ChannelAddress(ESS_ID, "DebugSetActivePower");

	private static final String METER_ID = "meter0";
	private static final ChannelAddress METER_ACTIVE_POWER = new ChannelAddress(METER_ID, "ActivePower");
	
	private static final ChannelAddress LEVL_REMAINING_LEVL_ENERGY = new ChannelAddress(CTRL_ID, "RemainingLevlEnergy");
	private static final ChannelAddress LEVL_SOC = new ChannelAddress(CTRL_ID, "LevlSoc");
	private static final ChannelAddress LEVL_SELL_TO_GRID_LIMIT = new ChannelAddress(CTRL_ID, "SellToGridLimit");
	private static final ChannelAddress LEVL_BUY_FROM_GRID_LIMIT = new ChannelAddress(CTRL_ID, "BuyFromGridLimit");
	private static final ChannelAddress LEVL_SOC_LOWER_BOUND = new ChannelAddress(CTRL_ID, "SocLowerBoundLevl");
	private static final ChannelAddress LEVL_SOC_UPPER_BOUND = new ChannelAddress(CTRL_ID, "SocUpperBoundLevl");
	private static final ChannelAddress LEVL_INFLUENCE_SELL_TO_GRID = new ChannelAddress(CTRL_ID, "InfluenceSellToGrid");
	private static final ChannelAddress LEVL_EFFICIENCY = new ChannelAddress(CTRL_ID, "Efficiency");
	private static final ChannelAddress LEVL_PUC_BATTERY_POWER = new ChannelAddress(CTRL_ID, "PucBatteryPower");
	private static final ChannelAddress LEVL_LAST_REQUEST_REALIZED_ENERGY_GRID = new ChannelAddress(CTRL_ID, "LastRequestRealizedEnergyGrid");
	private static final ChannelAddress LEVL_LAST_REQUEST_TIMESTAMP = new ChannelAddress(CTRL_ID, "LastRequestTimestamp");
	
	@Test
	public void testWithoutLevlRequest() throws Exception {
		new ControllerTest(new ControllerEssBalancingImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID) //
						.setPower(new DummyPower(0.3, 0.3, 0.1)) //
						.withCapacity(500000) // 1.800.000.000 Ws
						.withSoc(50) // 900.000.000 Ws
						.withMaxApparentPower(500000)
						.withAllowedChargePower(500000) //TODO: Die Werte werden in Component nicht verwendet! Herausfinden, ob wir diese berücksichtigen müssen.
						.withAllowedDischargePower(500000))
				.addReference("meter", new DummyElectricityMeter(METER_ID)) //
				.addReference("cycle", new DummyCycle(1000))
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setMeterId(METER_ID) //
						.build())
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 0) //
						.input(METER_ACTIVE_POWER, 20000) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, 20000)
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 6000))
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 0) //
						.input(METER_ACTIVE_POWER, 20000) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 12000)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 3793) //
						.input(METER_ACTIVE_POWER, 20000 - 3793) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 16483)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 8981) //
						.input(METER_ACTIVE_POWER, 20000 - 8981) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 19649)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 13723) //
						.input(METER_ACTIVE_POWER, 20000 - 13723) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 21577)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 17469) //
						.input(METER_ACTIVE_POWER, 20000 - 17469) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 22436)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 20066) //
						.input(METER_ACTIVE_POWER, 20000 - 20066) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 22531)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 21564) //
						.input(METER_ACTIVE_POWER, 20000 - 21564) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 22171)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 22175) //
						.input(METER_ACTIVE_POWER, 20000 - 22175) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 21608)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 22173) //
						.input(METER_ACTIVE_POWER, 20000 - 22173) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 21017)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 21816) //
						.input(METER_ACTIVE_POWER, 20000 - 21816) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 20508)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 21311) //
						.input(METER_ACTIVE_POWER, 20000 - 21311) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 20129)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 20803) //
						.input(METER_ACTIVE_POWER, 20000 - 20803) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 19889)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 20377) //
						.input(METER_ACTIVE_POWER, 20000 - 20377) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 19767));
	}
	
	@Test
	public void testWithLevlDischargeRequest() throws Exception {
		new ControllerTest(new ControllerEssBalancingImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID) //
						.setPower(new DummyPower(0.3, 0.3, 0.1)) //
						.withCapacity(500000) // 1.800.000.000 Ws
						.withSoc(50) // 900.000.000 Ws
						.withMaxApparentPower(500000))
				.addReference("meter", new DummyElectricityMeter(METER_ID)) //
				.addReference("cycle", new DummyCycle(1000))
				.addReference("currentRequest", new LevlControlRequest(0, 100))
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setMeterId(METER_ID) //
						.build())
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 0) //
						.input(METER_ACTIVE_POWER, 20000) //
						.input(LEVL_REMAINING_LEVL_ENERGY, 10000)
						.input(LEVL_SOC, 100_000)
						.input(LEVL_SELL_TO_GRID_LIMIT, -100_000)
						.input(LEVL_BUY_FROM_GRID_LIMIT, 100_000)
						.input(LEVL_SOC_LOWER_BOUND, 0)
						.input(LEVL_SOC_UPPER_BOUND, 100)
						.input(LEVL_INFLUENCE_SELL_TO_GRID, true)
						.input(LEVL_EFFICIENCY, 80.0)
						//TODO: Prüfen ob richtiger Channel. Wurde bisher genutzt, aber ggf. nicht korrekt, da "DEBUG"?
						.input(DEBUG_SET_ACTIVE_POWER, 30000)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, 30000)
						.output(LEVL_PUC_BATTERY_POWER, 20000L)
						.output(LEVL_REMAINING_LEVL_ENERGY, 0L)
						.output(LEVL_SOC, 87500L))
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 30000) //
						.input(METER_ACTIVE_POWER, -10000) //
						.input(DEBUG_SET_ACTIVE_POWER, 20000)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, 20000)
						.output(LEVL_PUC_BATTERY_POWER, 20000L)
						.output(LEVL_REMAINING_LEVL_ENERGY, 0L)
						.output(LEVL_SOC, 87500L))
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 20000) //
						.input(METER_ACTIVE_POWER, 0) //
						.input(DEBUG_SET_ACTIVE_POWER, 20000)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, 20000)
						.output(LEVL_PUC_BATTERY_POWER, 20000L)
						.output(LEVL_REMAINING_LEVL_ENERGY, 0L)
						.output(LEVL_SOC, 87500L));
	}

	@Test
	public void testWithLevlChargeRequest() throws Exception {
		new ControllerTest(new ControllerEssBalancingImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID) //
						.setPower(new DummyPower(0.3, 0.3, 0.1)) //
						.withCapacity(500000) // 1.800.000.000 Ws
						.withSoc(50) // 900.000.000 Ws
						.withMaxApparentPower(500000))
				.addReference("meter", new DummyElectricityMeter(METER_ID)) //
				.addReference("cycle", new DummyCycle(1000))
				.addReference("currentRequest", new LevlControlRequest(0, 100))
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setMeterId(METER_ID) //
						.build())
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 0) //
						.input(METER_ACTIVE_POWER, 20000) //
						.input(LEVL_REMAINING_LEVL_ENERGY, -10000)
						.input(LEVL_SOC, 100_000)
						.input(LEVL_SELL_TO_GRID_LIMIT, -100_000)
						.input(LEVL_BUY_FROM_GRID_LIMIT, 100_000)
						.input(LEVL_SOC_LOWER_BOUND, 0)
						.input(LEVL_SOC_UPPER_BOUND, 100)
						.input(LEVL_INFLUENCE_SELL_TO_GRID, true)
						.input(LEVL_EFFICIENCY, 80.0)
						.input(DEBUG_SET_ACTIVE_POWER, 10000)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, 10000)
						.output(LEVL_PUC_BATTERY_POWER, 20000L)
						.output(LEVL_REMAINING_LEVL_ENERGY, 0L)
						.output(LEVL_SOC, 108000L))
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 10000) //
						.input(METER_ACTIVE_POWER, 10000) //
						.input(DEBUG_SET_ACTIVE_POWER, 20000)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, 20000)
						.output(LEVL_PUC_BATTERY_POWER, 20000L)
						.output(LEVL_REMAINING_LEVL_ENERGY, 0L)
						.output(LEVL_SOC, 108000L))
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 20000) //
						.input(METER_ACTIVE_POWER, 0) //
						.input(DEBUG_SET_ACTIVE_POWER, 20000)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, 20000)
						.output(LEVL_PUC_BATTERY_POWER, 20000L)
						.output(LEVL_REMAINING_LEVL_ENERGY, 0L)
						.output(LEVL_SOC, 108000L));
	}
	
	// Test with discharge request (ws) > MAX_INT. Constrained by sell to grid limit.
	@Test
	public void testWithLargeLevlDischargeRequest() throws Exception {
		new ControllerTest(new ControllerEssBalancingImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID) //
						.setPower(new DummyPower(0.3, 0.3, 0.1)) //
						.withCapacity(500000) // 1.800.000.000 Ws
						.withSoc(50) // 900.000.000 Ws
						.withMaxApparentPower(500000))
				.addReference("meter", new DummyElectricityMeter(METER_ID)) //
				.addReference("cycle", new DummyCycle(1000))
				.addReference("currentRequest", new LevlControlRequest(0, 100))
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setMeterId(METER_ID) //
						.build())
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 0) //
						.input(METER_ACTIVE_POWER, 20000) //
						.input(LEVL_REMAINING_LEVL_ENERGY, 2_500_000_000L)
						.input(LEVL_SOC, 100_000)
						.input(LEVL_SELL_TO_GRID_LIMIT, -100_000)
						.input(LEVL_BUY_FROM_GRID_LIMIT, 100_000)
						.input(LEVL_SOC_LOWER_BOUND, 0)
						.input(LEVL_SOC_UPPER_BOUND, 100)
						.input(LEVL_INFLUENCE_SELL_TO_GRID, true)
						.input(LEVL_EFFICIENCY, 80.0)
						.input(DEBUG_SET_ACTIVE_POWER, 120000)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, 120000)
						.output(LEVL_PUC_BATTERY_POWER, 20000L)
						.output(LEVL_REMAINING_LEVL_ENERGY, 2_499_900_000L)
						.output(LEVL_SOC, -25000L))
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 120000) //
						.input(METER_ACTIVE_POWER, -100000) //
						.input(DEBUG_SET_ACTIVE_POWER, 120000)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, 120000)
						.output(LEVL_PUC_BATTERY_POWER, 20000L)
						.output(LEVL_REMAINING_LEVL_ENERGY, 2_499_800_000L)
						.output(LEVL_SOC, -150000L))
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 120000) //
						.input(METER_ACTIVE_POWER, -100000) //
						.input(DEBUG_SET_ACTIVE_POWER, 120000)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, 120000)
						.output(LEVL_PUC_BATTERY_POWER, 20000L)
						.output(LEVL_REMAINING_LEVL_ENERGY, 2_499_700_000L)
						.output(LEVL_SOC, -275000L));
	}

	// Physical SoC is 90%, Levl SoC is -10%, means PUC must not charge because Levl reserved the remaining 10%. 
	@Test
	public void testWithReservedCapacity() throws Exception {
		new ControllerTest(new ControllerEssBalancingImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID) //
						.setPower(new DummyPower(0.3, 0.3, 0.1)) //
						.withCapacity(500000) // 1.800.000.000 Ws
						.withMaxApparentPower(500000))
				.addReference("meter", new DummyElectricityMeter(METER_ID)) //
				.addReference("cycle", new DummyCycle(1000))
				.addReference("currentRequest", new LevlControlRequest(0, 100))
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setMeterId(METER_ID) //
						.build())
				.next(new TestCase() //
						.input(ESS_SOC, 90) // 1.620.000.000 Ws
						.input(ESS_ACTIVE_POWER, 0) //
						.input(METER_ACTIVE_POWER, -20000) //
						.input(LEVL_REMAINING_LEVL_ENERGY, -10_000_000)
						.input(LEVL_SOC, -180_000_000)
						.input(LEVL_SELL_TO_GRID_LIMIT, -800000)
						.input(LEVL_BUY_FROM_GRID_LIMIT, 800000)
						.input(LEVL_SOC_LOWER_BOUND, 0)
						.input(LEVL_SOC_UPPER_BOUND, 100)
						.input(LEVL_INFLUENCE_SELL_TO_GRID, true)
						.input(LEVL_EFFICIENCY, 80.0)
						.input(DEBUG_SET_ACTIVE_POWER, -500000)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, -500000)
						.output(LEVL_PUC_BATTERY_POWER, 0L)
						.output(LEVL_REMAINING_LEVL_ENERGY, -9_500_000L)
						.output(LEVL_SOC, -179_600_000L))
				.next(new TestCase() //
						.input(ESS_SOC, 90) // 1.620.000.000 Ws, aber eigentlich 1.620.400.000 Ws => TODO: dadurch darf der PUC wieder was machen, obwohl eigentlich noch immer alles für Levl reserviert ist.
						.input(ESS_ACTIVE_POWER, -500000) //
						.input(METER_ACTIVE_POWER, 480000) //
						.input(DEBUG_SET_ACTIVE_POWER, -500000)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, -500000)
						.output(LEVL_PUC_BATTERY_POWER, 0L)
						.output(LEVL_REMAINING_LEVL_ENERGY, -9_000_000L)
						.output(LEVL_SOC, -179_200_000L))
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, -500000) //
						.input(METER_ACTIVE_POWER, 480000) //
						.input(DEBUG_SET_ACTIVE_POWER, -500000)
						.output(ESS_SET_ACTIVE_POWER_EQUALS_WITH_PID, -500000)
						.output(LEVL_PUC_BATTERY_POWER, 0L)
						.output(LEVL_REMAINING_LEVL_ENERGY, -8_500_000L)
						.output(LEVL_SOC, -178_800_000L));
	}

	
	// Erläuterung: Channels die innerhalb des Controllers sowie in einem Testcase gesetzt werden, bleiben erhalten.
	// Channels die jedoch im Anschluss durch andere Controller gesetzt werden, werden nicht gesetzt. Ebenso ändert sich z.B. nicht die ESS_ACTIVE_POWER zwischen den Zyklen, da das ESS lediglich ein Mock ist.
	
	//TODO: AllowedChargePower vs. ess.getPower.Max/Min. Was verwenden? Kurze Analyse: GetPower.Min/Max() wird gesetzt, wenn ApparentPower des Ess gesetzt ist. Allowed Charge/DischargePower muss nochmals irgendwo seperat gesetzt werden können.
	//TODO: Müssen wir noch irgendwo die Production berücksichtigen? Z.B. an der Batterie? Eigentlich nicht, müsste alles über den NAP abgedeckt sein.
}
