package io.openems.edge.controller.ess.timeofusetariff.optimizer;

import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.BALANCING;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.CHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DELAY_DISCHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.CONSUMPTION_888_20231106;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.PAST_CONSUMPTION_PREDICTION;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.PAST_HOURLY_PRICES;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.PAST_PRODUCTION_PREDICTION;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.PAST_STATES;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.PRICES_888_20231106;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.PRODUCTION_888_20231106;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.SimulatorTest.TIME;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.SimulatorTest.hourlyToQuarterly;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.buildInitialPopulation;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.calculateCharge100;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.calculateStateChargeEnergy;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.handleGetScheduleRequest;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.interpolateArray;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.joinConsumptionPredictions;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.postprocessPeriodState;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.toEnergy;
import static io.openems.edge.timeofusetariff.api.utils.TimeOfUseTariffUtils.getNowRoundedDownToMinutes;
import static java.lang.Integer.MIN_VALUE;
import static java.util.Arrays.stream;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.time.ZonedDateTime;

import org.junit.Ignore;
import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.controller.ess.timeofusetariff.StateMachine;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.timedata.test.DummyTimedata;

public class UtilsTest {

	@Test
	public void testInterpolateArrayFloat() {
		assertArrayEquals(new float[] { 123F, 123F, 234F, 234F, 345F }, //
				interpolateArray(new Float[] { null, 123F, 234F, null, 345F, null }), //
				0.0001F);

		assertArrayEquals(new float[] {}, //
				interpolateArray(new Float[] { null }), //
				0.0001F);
	}

	@Test
	public void testInterpolateArrayInteger() {
		assertArrayEquals(new int[] { 123, 123, 234, 234, 345 }, //
				interpolateArray(new Integer[] { null, 123, 234, null, 345, null }));

		assertArrayEquals(new int[] {}, //
				interpolateArray(new Integer[] { null }));

		assertArrayEquals(new int[] { 123, 123 }, //
				interpolateArray(new Integer[] { null, 123 }));

		assertArrayEquals(new int[] { 123 }, //
				interpolateArray(new Integer[] { 123, null }));
	}

	@Test
	public void testJoinConsumptionPredictions() {
		assertArrayEquals(//
				new Integer[] { 1, 2, 3, 4, 55, 66, 77, 88, 99 }, //
				joinConsumptionPredictions(4, //
						new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, //
						new Integer[] { 11, 22, 33, 44, 55, 66, 77, 88, 99 }));
	}

	@Test
	public void testCalculateStateChargeEnergy() {
		assertEquals(0, calculateStateChargeEnergy(0, 0, 0, 0));

		assertEquals(-1234, calculateStateChargeEnergy(//
				2000 /* grid limit */, 1000 /* consumption */, 2000 /* production */, -1234));

		assertEquals(-1100, calculateStateChargeEnergy(//
				1000 /* grid limit */, 100 /* consumption */, 200 /* production */, MIN_VALUE /* no limit */));

		// 'maxBuyFromGrid' is already completely consumed by predicted consumption
		// (minus production). => simulate charge with 1 Wh to trigger optimizer to
		// consider CHARGE instead of BALANCING mode
		assertEquals(1, calculateStateChargeEnergy(//
				5000 /* grid limit */, 20000 /* consumption */, 0 /* production */, -2500));
	}

	@Test
	public void testCalculateCharge100() {
		assertEquals(-2500, calculateCharge100(//
				new DummyManagedSymmetricEss("ess0").withActivePower(-1000), //
				new DummySum().withGridActivePower(500), //
				/* maxChargePowerFromGrid */ 2000).intValue());

		// Would be 5000, but can never be positive
		assertEquals(0, calculateCharge100(//
				new DummyManagedSymmetricEss("ess0").withActivePower(1000), //
				new DummySum().withGridActivePower(9000), //
				/* maxChargePowerFromGrid */ 5000).intValue());
	}

	@Test
	@Ignore
	public void testHandleScheduleRequest() throws OpenemsNamedException {
		final var now = getNowRoundedDownToMinutes(ZonedDateTime.now(), 15);
		final var fromDate = now.minusHours(3);
		final var channeladdressPrices = new ChannelAddress("", "QuarterlyPrices");
		final var channeladdressStateMachine = new ChannelAddress("", "StateMachine");
		final var channelPredictedProduction = new ChannelAddress("", "PredictedProduction");
		final var channelPredictedConsumption = new ChannelAddress("", "PredictedConsumption");

		var timedata = new DummyTimedata("timedata0");

		// past data is only for 3 hours. in total 12 quarters.
		for (var i = 0; i < 12; i++) {
			timedata.add(fromDate.plusMinutes(i * 15), channeladdressPrices, PAST_HOURLY_PRICES[i]);
			timedata.add(fromDate.plusMinutes(i * 15), channeladdressStateMachine, PAST_STATES[i]);
			timedata.add(fromDate.plusMinutes(i * 15), channelPredictedProduction, PAST_PRODUCTION_PREDICTION[i]);
			timedata.add(fromDate.plusMinutes(i * 15), channelPredictedConsumption, PAST_CONSUMPTION_PREDICTION[i]);
		}

		// Testing only past data. For full data, optimizer has to be created as well.
		var result = handleGetScheduleRequest(new Optimizer(null), randomUUID(), timedata, "", fromDate, now);

		JsonUtils.prettyPrint(result.getResult());
	}

	@Test
	public void testPostprocessPeriodState() {
		var p = Params.create() //
				.time(TIME) //
				.essAvailableEnergy(0) //
				.essCapacity(22000) //
				.essMaxEnergyPerPeriod(0) //
				.maxBuyFromGrid(0) //
				.productions(new int[0]) //
				.consumptions(new int[0]) //
				.prices(new float[] { 123F }) //
				.states(new StateMachine[0]) //
				.build();

		assertEquals("BALANCING stays BALANCING", //
				BALANCING, postprocessPeriodState(p, 0, 0, 0, 0, BALANCING));

		assertEquals("DELAY_DISCHARGE and would discharge in balancing", //
				DELAY_DISCHARGE, postprocessPeriodState(p, 0, 0, 0, 0, DELAY_DISCHARGE));
		assertEquals("DELAY_DISCHARGE and would charge from PV in balancing", //
				BALANCING, postprocessPeriodState(p, 0, 0, 0, -1000, DELAY_DISCHARGE));
		assertEquals("DELAY_DISCHARGE but price is the max price", //
				BALANCING, postprocessPeriodState(p, 0, 0, 123F, 0, DELAY_DISCHARGE));
		assertEquals("DELAY_DISCHARGE and price is NOT the max price", //
				DELAY_DISCHARGE, postprocessPeriodState(p, 0, 0, 122.9F, 0, DELAY_DISCHARGE));

		assertEquals("CHARGE actually from grid", //
				CHARGE, postprocessPeriodState(p, 0, 1, 0, 0, CHARGE));
		assertEquals("CHARGE but fully supplied by excess PV", //
				BALANCING, postprocessPeriodState(p, 0, -1000, 0, 0, CHARGE));
		assertEquals("CHARGE but battery is full (>= 90 %)", //
				DELAY_DISCHARGE, postprocessPeriodState(p, 19800, 1, 0, 0, CHARGE));
		assertEquals("CHARGE and battery is NOT full (89 %)", //
				CHARGE, postprocessPeriodState(p, 19799, 1, 0, 0, CHARGE));
		assertEquals("CHARGE but price is close to max", //
				DELAY_DISCHARGE, postprocessPeriodState(p, 0, 1, 102.6F, 0, CHARGE));
		assertEquals("CHARGE and price is NOT close to max", //
				CHARGE, postprocessPeriodState(p, 0, 1, 102.5F, 0, CHARGE));
	}

	@Test
	public void testBuildInitialPopulation() {
		{
			var gt = buildInitialPopulation(Params.create() //
					.productions(stream(interpolateArray(PRODUCTION_888_20231106)).map(v -> toEnergy(v)).toArray()) //
					.consumptions(stream(interpolateArray(CONSUMPTION_888_20231106)).map(v -> toEnergy(v)).toArray()) //
					.prices(hourlyToQuarterly(interpolateArray(PRICES_888_20231106))) //
					.states(BALANCING, DELAY_DISCHARGE, CHARGE) //
					.existingSchedule(CHARGE, DELAY_DISCHARGE, CHARGE, DELAY_DISCHARGE, BALANCING) //
					.build()).get(0);
			assertEquals(2 /* CHARGE */, gt.get(0).get(0).intValue());
			assertEquals(1 /* DELAY_DISCHARGE */, gt.get(1).get(0).intValue());
			assertEquals(2 /* CHARGE */, gt.get(2).get(0).intValue());
			assertEquals(1 /* DELAY_DISCHARGE */, gt.get(3).get(0).intValue());
			assertEquals(0 /* BALANCING */, gt.get(4).get(0).intValue());
			assertEquals(0 /* BALANCING */, gt.get(5).get(0).intValue()); // default
		}
		{
			var gt = buildInitialPopulation(Params.create() //
					.productions(stream(interpolateArray(PRODUCTION_888_20231106)).map(v -> toEnergy(v)).toArray()) //
					.consumptions(stream(interpolateArray(CONSUMPTION_888_20231106)).map(v -> toEnergy(v)).toArray()) //
					.prices(hourlyToQuarterly(interpolateArray(PRICES_888_20231106))) //
					.states(BALANCING, DELAY_DISCHARGE) //
					.existingSchedule(CHARGE, DELAY_DISCHARGE, CHARGE, DELAY_DISCHARGE, BALANCING) //
					.build()).get(0);
			assertEquals(0 /* fallback to BALANCING */, gt.get(0).get(0).intValue());
			assertEquals(1 /* DELAY_DISCHARGE */, gt.get(1).get(0).intValue());
			assertEquals(0 /* fallback to BALANCING */, gt.get(2).get(0).intValue());
			assertEquals(1 /* DELAY_DISCHARGE */, gt.get(3).get(0).intValue());
			assertEquals(0 /* BALANCING */, gt.get(4).get(0).intValue());
			assertEquals(0 /* BALANCING */, gt.get(5).get(0).intValue()); // default
		}

	}
}
