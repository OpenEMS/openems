package io.openems.edge.controller.ess.timeofusetariff.optimizer;

import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.BALANCING;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.CHARGE_GRID;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DELAY_DISCHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.CONSUMPTION_888_20231106;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.PRICES_888_20231106;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.PRODUCTION_888_20231106;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.InitialPopulationUtils.buildInitialPopulation;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.SimulatorTest.hourlyToQuarterly;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.interpolateArray;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.toEnergy;
import static java.util.Arrays.stream;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.edge.controller.ess.timeofusetariff.ControlMode;

public class InitialPopulationUtilsTest {

	@Test
	public void testBuildInitialPopulation() {
		{
			var lgt = buildInitialPopulation(Params.create() //
					.productions(stream(interpolateArray(PRODUCTION_888_20231106)).map(v -> toEnergy(v)).toArray()) //
					.consumptions(stream(interpolateArray(CONSUMPTION_888_20231106)).map(v -> toEnergy(v)).toArray()) //
					.prices(hourlyToQuarterly(interpolateArray(PRICES_888_20231106))) //
					.states(ControlMode.CHARGE_CONSUMPTION.states) //
					.existingSchedule() //
					.build());
			assertEquals(5, lgt.size()); // No Schedule -> only pure BALANCING + CHARGE_GRID
		}
		{
			var lgt = buildInitialPopulation(Params.create() //
					.productions(stream(interpolateArray(PRODUCTION_888_20231106)).map(v -> toEnergy(v)).toArray()) //
					.consumptions(stream(interpolateArray(CONSUMPTION_888_20231106)).map(v -> toEnergy(v)).toArray()) //
					.prices(hourlyToQuarterly(interpolateArray(PRICES_888_20231106))) //
					.states(ControlMode.CHARGE_CONSUMPTION.states) //
					.existingSchedule(BALANCING, BALANCING) //
					.build());
			assertEquals(5, lgt.size()); // Existing Schedule is only BALANCING -> only pure BALANCING + CHARGE_GRID
		}
		{
			var gt = buildInitialPopulation(Params.create() //
					.productions(stream(interpolateArray(PRODUCTION_888_20231106)).map(v -> toEnergy(v)).toArray()) //
					.consumptions(stream(interpolateArray(CONSUMPTION_888_20231106)).map(v -> toEnergy(v)).toArray()) //
					.prices(hourlyToQuarterly(interpolateArray(PRICES_888_20231106))) //
					.states(ControlMode.CHARGE_CONSUMPTION.states) //
					.existingSchedule(CHARGE_GRID, DELAY_DISCHARGE, CHARGE_GRID, DELAY_DISCHARGE, BALANCING) //
					.build()).get(1);
			assertEquals(2 /* CHARGE_GRID */, gt.get(0).get(0).intValue());
			assertEquals(1 /* DELAY_DISCHARGE */, gt.get(1).get(0).intValue());
			assertEquals(2 /* CHARGE_GRID */, gt.get(2).get(0).intValue());
			assertEquals(1 /* DELAY_DISCHARGE */, gt.get(3).get(0).intValue());
			assertEquals(0 /* BALANCING */, gt.get(4).get(0).intValue());
			assertEquals(0 /* BALANCING */, gt.get(5).get(0).intValue()); // default
		}
		{
			var gt = buildInitialPopulation(Params.create() //
					.productions(stream(interpolateArray(PRODUCTION_888_20231106)).map(v -> toEnergy(v)).toArray()) //
					.consumptions(stream(interpolateArray(CONSUMPTION_888_20231106)).map(v -> toEnergy(v)).toArray()) //
					.prices(hourlyToQuarterly(interpolateArray(PRICES_888_20231106))) //
					.states(ControlMode.DELAY_DISCHARGE.states) //
					.existingSchedule(CHARGE_GRID, DELAY_DISCHARGE, CHARGE_GRID, DELAY_DISCHARGE, BALANCING) //
					.build()).get(1);
			assertEquals(0 /* fallback to BALANCING */, gt.get(0).get(0).intValue());
			assertEquals(1 /* DELAY_DISCHARGE */, gt.get(1).get(0).intValue());
			assertEquals(0 /* fallback to BALANCING */, gt.get(2).get(0).intValue());
			assertEquals(1 /* DELAY_DISCHARGE */, gt.get(3).get(0).intValue());
			assertEquals(0 /* BALANCING */, gt.get(4).get(0).intValue());
			assertEquals(0 /* BALANCING */, gt.get(5).get(0).intValue()); // default
		}
	}

}
