package io.openems.edge.energy.v1.optimizer;

import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.BALANCING;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.CHARGE_GRID;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DELAY_DISCHARGE;
import static io.openems.edge.energy.api.EnergyUtils.interpolateArray;
import static io.openems.edge.energy.api.EnergyUtils.toEnergy;
import static io.openems.edge.energy.v1.optimizer.InitialPopulationUtils.buildInitialPopulation;
import static io.openems.edge.energy.v1.optimizer.SimulatorTest.hourlyToQuarterly;
import static io.openems.edge.energy.v1.optimizer.TestData.CONSUMPTION_888_20231106;
import static io.openems.edge.energy.v1.optimizer.TestData.PRICES_888_20231106;
import static io.openems.edge.energy.v1.optimizer.TestData.PRODUCTION_888_20231106;
import static io.openems.edge.energy.v1.optimizer.UtilsTest.prepareExistingSchedule;
import static io.openems.edge.energy.v1.optimizer.UtilsV1.interpolateDoubleArray;
import static java.util.Arrays.stream;
import static org.junit.Assert.assertEquals;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Test;

import io.openems.edge.controller.ess.timeofusetariff.ControlMode;

public class InitialPopulationUtilsTest {

	public static final ZonedDateTime TIME = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));

	@Test
	public void testBuildInitialPopulation() {
		{
			var lgt = buildInitialPopulation(Params.create() //
					.setTime(TIME) //
					.setProductions(stream(interpolateArray(PRODUCTION_888_20231106)).map(v -> toEnergy(v)).toArray()) //
					.setConsumptions(stream(interpolateArray(CONSUMPTION_888_20231106)).map(v -> toEnergy(v)).toArray()) //
					.setPrices(hourlyToQuarterly(interpolateDoubleArray(PRICES_888_20231106))) //
					.setStates(ControlMode.CHARGE_CONSUMPTION.states) //
					.setExistingSchedule(prepareExistingSchedule(TIME)) //
					.build());
			assertEquals(5, lgt.size()); // No Schedule -> only pure BALANCING + CHARGE_GRID
		}
		{
			var lgt = buildInitialPopulation(Params.create() //
					.setTime(TIME) //
					.setProductions(stream(interpolateArray(PRODUCTION_888_20231106)).map(v -> toEnergy(v)).toArray()) //
					.setConsumptions(stream(interpolateArray(CONSUMPTION_888_20231106)).map(v -> toEnergy(v)).toArray()) //
					.setPrices(hourlyToQuarterly(interpolateDoubleArray(PRICES_888_20231106))) //
					.setStates(ControlMode.CHARGE_CONSUMPTION.states) //
					.setExistingSchedule(prepareExistingSchedule(TIME, BALANCING, BALANCING)) //
					.build());
			assertEquals(5, lgt.size()); // Existing Schedule is only BALANCING -> only pure BALANCING + CHARGE_GRID
		}
		{
			var gt = buildInitialPopulation(Params.create() //
					.setTime(TIME) //
					.setProductions(stream(interpolateArray(PRODUCTION_888_20231106)).map(v -> toEnergy(v)).toArray()) //
					.setConsumptions(stream(interpolateArray(CONSUMPTION_888_20231106)).map(v -> toEnergy(v)).toArray()) //
					.setPrices(hourlyToQuarterly(interpolateDoubleArray(PRICES_888_20231106))) //
					.setStates(ControlMode.CHARGE_CONSUMPTION.states) //
					.setExistingSchedule(prepareExistingSchedule(TIME, //
							CHARGE_GRID, DELAY_DISCHARGE, CHARGE_GRID, DELAY_DISCHARGE, BALANCING)) //
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
					.setTime(TIME) //
					.setProductions(stream(interpolateArray(PRODUCTION_888_20231106)).map(v -> toEnergy(v)).toArray()) //
					.setConsumptions(stream(interpolateArray(CONSUMPTION_888_20231106)).map(v -> toEnergy(v)).toArray()) //
					.setPrices(hourlyToQuarterly(interpolateDoubleArray(PRICES_888_20231106))) //
					.setStates(ControlMode.DELAY_DISCHARGE.states) //
					.setExistingSchedule(prepareExistingSchedule(TIME, //
							CHARGE_GRID, DELAY_DISCHARGE, CHARGE_GRID, DELAY_DISCHARGE, BALANCING)) //
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
