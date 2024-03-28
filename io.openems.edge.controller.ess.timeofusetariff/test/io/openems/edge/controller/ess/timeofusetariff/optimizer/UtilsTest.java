package io.openems.edge.controller.ess.timeofusetariff.optimizer;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;
import static io.openems.common.utils.JsonUtils.getAsFloat;
import static io.openems.common.utils.JsonUtils.getAsInt;
import static io.openems.common.utils.JsonUtils.getAsJsonArray;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.common.utils.UuidUtils.getNilUuid;
import static io.openems.edge.common.test.TestUtils.withValue;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.BALANCING;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.CHARGE_GRID;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DELAY_DISCHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.CONSUMPTION_PREDICTION_QUARTERLY;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.PAST_HOURLY_PRICES;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.PAST_SOC;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.PAST_STATES;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.PRODUCTION_888_20231106;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.PRODUCTION_PREDICTION_QUARTERLY;
import static io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffControllerImplTest.CLOCK;
import static io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffControllerImplTest.callCreateParams;
import static io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffControllerImplTest.getContext;
import static io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffControllerImplTest.getOptimizer;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.EnergyFlowTest.NO_FLOW;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.SimulatorTest.TIME;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.SimulatorTest.createParams888d20231106;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.ESS_MAX_SOC;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.SUM_CONSUMPTION;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.SUM_ESS_DISCHARGE_POWER;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.SUM_ESS_SOC;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.SUM_GRID;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.SUM_PRODUCTION;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.calculateChargeGridPower;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.calculateDelayDischargePower;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.calculateEssChargeInChargeGridPowerFromParams;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.calculateExecutionLimitSeconds;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.calculateMaxChargeProductionPower;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.createSimulatorParams;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.findFirstPeakIndex;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.findFirstValleyIndex;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.generateProductionPrediction;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.getEssMinSocEnergy;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.handleGetScheduleRequest;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.interpolateArray;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.joinConsumptionPredictions;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.paramsAreValid;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.postprocessRunState;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.postprocessSimulatorState;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.toEnergy;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.toPower;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.updateSchedule;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.IntStream;

import org.junit.Test;

import com.google.common.collect.ImmutableSortedMap;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.controller.ess.emergencycapacityreserve.ControllerEssEmergencyCapacityReserve;
import io.openems.edge.controller.ess.limittotaldischarge.ControllerEssLimitTotalDischarge;
import io.openems.edge.controller.ess.timeofusetariff.ControlMode;
import io.openems.edge.controller.ess.timeofusetariff.StateMachine;
import io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffControllerImplTest;
import io.openems.edge.controller.ess.timeofusetariff.optimizer.Simulator.Period;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.test.DummyHybridEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.timedata.test.DummyTimedata;

public class UtilsTest {

	protected static ImmutableSortedMap<ZonedDateTime, StateMachine> prepareExistingSchedule(ZonedDateTime fromDate,
			StateMachine... existingSchedule) {
		return IntStream.range(0, existingSchedule.length) //
				.mapToObj(Integer::valueOf) //
				.collect(ImmutableSortedMap.<Integer, ZonedDateTime, StateMachine>toImmutableSortedMap(
						ZonedDateTime::compareTo, //
						i -> fromDate.plusMinutes(i * 15), //
						i -> existingSchedule[i]));
	}

	@Test
	public void testCreateSimulatorParams() throws Exception {
		var context = getContext(TimeOfUseTariffControllerImplTest.create(CLOCK));
		var p = createSimulatorParams(context, ImmutableSortedMap.of());
		assertEquals(4, p.optimizePeriods().size());
		assertEquals(10000, p.essTotalEnergy());
		assertEquals(0, p.essMinSocEnergy());
		assertEquals(250, p.optimizePeriods().get(0).essMaxEnergy());
		assertEquals(6000, p.essInitialEnergy());
		assertEquals(434, p.optimizePeriods().get(0).essChargeInChargeGrid());
		assertEquals(2500, p.optimizePeriods().get(0).maxBuyFromGrid());
		assertArrayEquals(ControlMode.CHARGE_CONSUMPTION.states, p.states());
	}

	@Test
	public void testInterpolateArrayFloat() {
		assertArrayEquals(new double[] { 123, 123, 234, 234, 345 }, //
				interpolateArray(new Double[] { null, 123., 234., null, 345., null }), //
				0.0001F);

		assertArrayEquals(new double[] {}, //
				interpolateArray(new Double[] { null }), //
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
	public void testToPower() {
		assertEquals(2000, (int) toPower(500));
		assertNull(toPower(null));
	}

	@Test
	public void testGenerateProductionPrediction() {
		final var arr = new Integer[] { 1, 2, 3 };
		assertArrayEquals(arr, generateProductionPrediction(arr, 2));
		assertArrayEquals(new Integer[] { 1, 2, 3, 0 }, generateProductionPrediction(arr, 4));
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
	public void testFindFirstPeakIndex() {
		assertEquals(0, findFirstPeakIndex(0, new double[0]));
		assertEquals(0, findFirstPeakIndex(0, new double[] { 1 }));
		assertEquals(0, findFirstPeakIndex(0, new double[] { 1, 0 }));
		assertEquals(1, findFirstPeakIndex(0, new double[] { 0, 1, 0 }));
		assertEquals(1, findFirstPeakIndex(0, new double[] { 0, 1, 0, 1 }));
		assertEquals(5, findFirstPeakIndex(5, new double[0]));
	}

	@Test
	public void testFindFirstValleyIndex() {
		assertEquals(0, findFirstValleyIndex(0, new double[0]));
		assertEquals(0, findFirstValleyIndex(0, new double[] { 1 }));
		assertEquals(1, findFirstValleyIndex(0, new double[] { 1, 0 }));
		assertEquals(0, findFirstValleyIndex(0, new double[] { 0, 1, 0 }));
		assertEquals(2, findFirstValleyIndex(1, new double[] { 0, 1, 0, 1 }));
		assertEquals(5, findFirstValleyIndex(5, new double[0]));
	}

	@Test
	public void testCalculateChargeGridPower() {
		var params = createParams888d20231106(ControlMode.CHARGE_CONSUMPTION.states);
		assertNull(calculateChargeGridPower(params, //
				new DummyManagedSymmetricEss("ess0"), //
				new DummySum(), //
				/* maxChargePowerFromGrid */ 24_000));

		assertEquals(-10000, calculateChargeGridPower(null, //
				new DummyManagedSymmetricEss("ess0") //
						.withCapacity(20_000) //
						.withActivePower(-6_000), //
				new DummySum() //
						.withGridActivePower(10_000), //
				/* maxChargePowerFromGrid */ 20_000).intValue());

		assertEquals(-4200, calculateChargeGridPower(null, //
				new DummyManagedSymmetricEss("ess0") //
						.withCapacity(20_000) //
						.withActivePower(-6_000), //
				new DummySum() //
						.withGridActivePower(10_000), //
				/* maxChargePowerFromGrid */ 20_000, //
				/* limitChargePowerFor14aEnWG */ true).intValue());
		
		assertEquals(-11000, calculateChargeGridPower(null, //
				new DummyManagedSymmetricEss("ess0") //
						.withCapacity(20_000) //
						.withActivePower(-6_000), //
				new DummySum() //
						.withGridActivePower(5_000), //
				/* maxChargePowerFromGrid */ 20_000).intValue());

		assertEquals(-5860, calculateChargeGridPower(params, //
				new DummyManagedSymmetricEss("ess0") //
						.withActivePower(-1000), //
				new DummySum() //
						.withGridActivePower(500), //
				/* maxChargePowerFromGrid */ 24_000).intValue());

		// Would be -3584, but limited to 5000 which is already surpassed
		// TODO if this should actually serve as blackout-protection, a positive value
		// would have to be returned
		assertEquals(0, calculateChargeGridPower(params, //
				new DummyManagedSymmetricEss("ess0") //
						.withActivePower(1000), //
				new DummySum() //
						.withGridActivePower(9000), //
				/* maxChargePowerFromGrid */ 5_000).intValue());

		assertEquals(-8360, calculateChargeGridPower(params, //
				new DummyHybridEss("ess0") //
						.withActivePower(-1000) //
						.withDcDischargePower(-1500), //
				new DummySum() //
						.withGridActivePower(-2000), //
				/* maxChargePowerFromGrid */ 24_000).intValue());
	}

	@Test
	public void testCalculateChargeProduction() {
		assertEquals(-500, calculateMaxChargeProductionPower(//
				new DummySum() //
						.withProductionAcActivePower(500)) //
				.intValue());

		assertEquals(0, calculateMaxChargeProductionPower(//
				new DummySum()) //
				.intValue());

		assertEquals(0, calculateMaxChargeProductionPower(//
				new DummySum() //
						.withProductionAcActivePower(-100 /* wrong */)) //
				.intValue());
	}

	@Test
	public void testCalculateDelayDischarge() {
		// DC-PV
		assertEquals(500, calculateDelayDischargePower(//
				new DummyHybridEss("ess0") //
						.withActivePower(-500) //
						.withDcDischargePower(-1000))
				.intValue());

		// Never negative
		assertEquals(0, calculateDelayDischargePower(//
				new DummyHybridEss("ess0") //
						.withActivePower(-1500) //
						.withDcDischargePower(-1000))
				.intValue());

		// AC-PV
		assertEquals(0, calculateDelayDischargePower(//
				new DummyManagedSymmetricEss("ess0") //
						.withActivePower(-1500)) //
				.intValue());
	}

	@Test
	public void testParamsAreValid() throws Exception {
		var builder = Params.create() //
				.setTime(TIME) //
				.setEssInitialEnergy(0) //
				.setEssTotalEnergy(22000) //
				.setEssMinSocEnergy(2_000) //
				.setEssMaxSocEnergy(20_000) //
				.seMaxBuyFromGrid(toEnergy(24_000)) //
				.seMaxBuyFromGrid(0) //
				.setStates(new StateMachine[0]);

		// No periods are available
		assertFalse(paramsAreValid(builder //
				.setProductions() //
				.setConsumptions() //
				.setPrices() //
				.build()));

		// Production and Consumption predictions are all zero
		assertFalse(paramsAreValid(builder //
				.setProductions(0, 0, 0) //
				.setConsumptions(0, 0) //
				.setPrices(123F) //
				.build()));

		// Prices are all the same
		assertFalse(paramsAreValid(builder //
				.setProductions(0, 1, 3) //
				.setConsumptions(0, 2) //
				.setPrices(123F, 123F) //
				.build()));

		// Finally got it right...
		assertTrue(paramsAreValid(builder //
				.setProductions(0, 1, 3) //
				.setConsumptions(0, 2) //
				.setPrices(123F, 124F) //
				.build()));
		assertEquals(2, builder.build().optimizePeriods().size());
	}

	private static class MyControllerEssLimitTotalDischarge
			extends AbstractDummyOpenemsComponent<MyControllerEssLimitTotalDischarge>
			implements ControllerEssLimitTotalDischarge {

		protected MyControllerEssLimitTotalDischarge(Integer minSoc) {
			super("ctrl0", //
					OpenemsComponent.ChannelId.values(), //
					ControllerEssLimitTotalDischarge.ChannelId.values() //
			);
			withValue(this.getMinSocChannel(), minSoc);
		}

		@Override
		public void run() throws OpenemsNamedException {
		}

		@Override
		protected MyControllerEssLimitTotalDischarge self() {
			return this;
		}
	}

	private static class MyControllerEssEmergencyCapacityReserve
			extends AbstractDummyOpenemsComponent<MyControllerEssEmergencyCapacityReserve>
			implements ControllerEssEmergencyCapacityReserve {

		protected MyControllerEssEmergencyCapacityReserve(Integer reserveSoc) {
			super("ctrl0", //
					OpenemsComponent.ChannelId.values(), //
					ControllerEssEmergencyCapacityReserve.ChannelId.values() //
			);
			withValue(this.getActualReserveSocChannel(), reserveSoc);
		}

		@Override
		public void run() throws OpenemsNamedException {
		}

		@Override
		protected MyControllerEssEmergencyCapacityReserve self() {
			return this;
		}
	}

	@Test
	public void testGetEssMinSocEnergy() {
		var t1 = new MyControllerEssLimitTotalDischarge(50);
		var t2 = new MyControllerEssLimitTotalDischarge(null);
		var t3 = new MyControllerEssEmergencyCapacityReserve(30);
		assertEquals(5000, getEssMinSocEnergy(new Context(//
				null, null, null, null, null, //
				List.of(t3), List.of(t1, t2), //
				null, 0), //
				10000));
	}

	@Test
	public void testHandleScheduleRequest() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-03-04T14:19:00.00Z"), ZoneOffset.UTC);
		final var ctrl = TimeOfUseTariffControllerImplTest.create(clock);

		// Simulate historic data
		var now = roundDownToQuarter(ZonedDateTime.now(clock));
		final var fromDate = now.minusHours(3);
		var timedata = new DummyTimedata("timedata0");
		for (var i = 0; i < 12; i++) {
			var quarter = fromDate.plusMinutes(i * 15);
			timedata.add(quarter, new ChannelAddress("ctrl0", "QuarterlyPrices"), PAST_HOURLY_PRICES[i]);
			timedata.add(quarter, new ChannelAddress("ctrl0", "StateMachine"), PAST_STATES[i]);
			timedata.add(quarter, SUM_PRODUCTION, PRODUCTION_PREDICTION_QUARTERLY[i]);
			timedata.add(quarter, SUM_CONSUMPTION, CONSUMPTION_PREDICTION_QUARTERLY[i]);
			timedata.add(quarter, SUM_ESS_SOC, PAST_SOC[i]);
			timedata.add(quarter, SUM_ESS_DISCHARGE_POWER, PRODUCTION_888_20231106[i]);
			timedata.add(quarter, SUM_GRID, PRODUCTION_888_20231106[i]);
		}

		var optimizer = getOptimizer(ctrl);
		callCreateParams(optimizer);

		// Testing only past data. For full data, optimizer has to be created as well.
		var result = handleGetScheduleRequest(optimizer, getNilUuid(), timedata, "ctrl0", clock.now()).getResult();

		// JsonUtils.prettyPrint(result);

		var schedule = getAsJsonArray(result, "schedule");
		assertEquals(12, schedule.size());
		{
			var period = getAsJsonObject(schedule.get(0));
			assertEquals(PAST_HOURLY_PRICES[0], getAsFloat(period, "price"), 0.00F);
			assertEquals(PRODUCTION_PREDICTION_QUARTERLY[0] / 4, getAsInt(period, "production"));
		}
	}

	@Test
	public void testPostprocessPeriodState() {
		var p = Params.create() //
				.setTime(TIME) //
				.setEssInitialEnergy(0) //
				.setEssTotalEnergy(22000) //
				.setEssMinSocEnergy(2_000) //
				.setEssMaxSocEnergy(20_000) //
				.setEssMaxEnergy(0) //
				.seMaxBuyFromGrid(toEnergy(24_000)) //
				.setProductions() //
				.setConsumptions() //
				.setPrices(new double[] { 123 }) //
				.setStates(new StateMachine[0]) //
				.build();

		assertEquals("BALANCING stays BALANCING", //
				BALANCING, postprocessSimulatorState(p, 0, BALANCING, NO_FLOW));

		assertEquals("DELAY_DISCHARGE but battery is empty", //
				BALANCING, postprocessSimulatorState(p, 2000, DELAY_DISCHARGE, NO_FLOW));

		assertEquals("DELAY_DISCHARGE and would discharge in balancing", //
				DELAY_DISCHARGE, postprocessSimulatorState(p, 2001, DELAY_DISCHARGE, NO_FLOW));
		assertEquals("DELAY_DISCHARGE and would charge from PV in balancing", //
				BALANCING, postprocessSimulatorState(p, 2001, DELAY_DISCHARGE,
						new EnergyFlow(0, 0, 0, 0, 0, 0, 1 /* productionToEss */, 0, 0, 0)));

		assertEquals("CHARGE_GRID actually from grid", //
				CHARGE_GRID, postprocessSimulatorState(p, 0, CHARGE_GRID,
						new EnergyFlow(0, 0, 0, 0, 0, 0, 0, 0, 0, 1 /* gridToEss */)));
		assertEquals("CHARGE_GRID but actually not charging", //
				BALANCING, postprocessSimulatorState(p, 0, CHARGE_GRID, NO_FLOW));
		assertEquals("CHARGE_GRID but battery is full", //
				DELAY_DISCHARGE, postprocessSimulatorState(p, 20_001, CHARGE_GRID,
						new EnergyFlow(0, 0, 0, 0, 0, 0, 0, 0, 0, 1 /* gridToEss */)));
	}

	@Test
	public void testPostprocessRunState() {
		// SoC undefined -> all stay
		assertEquals(BALANCING, postprocessRunState(0, null, 0, BALANCING));
		assertEquals(DELAY_DISCHARGE, postprocessRunState(0, null, 0, DELAY_DISCHARGE));
		assertEquals(CHARGE_GRID, postprocessRunState(0, null, 0, CHARGE_GRID));

		assertEquals("BALANCING stays BALANCING", //
				BALANCING, postprocessRunState(10, 10, 0, BALANCING));

		assertEquals("DELAY_DISCHARGE but SoC is at Min-SoC", //
				BALANCING, postprocessRunState(10, 10, 0, DELAY_DISCHARGE));
		assertEquals("DELAY_DISCHARGE and SoC is above Min-SoC", //
				DELAY_DISCHARGE, postprocessRunState(10, 11, 0, DELAY_DISCHARGE));

		assertEquals("CHARGE_GRID but SoC is at Max-SoC", //
				DELAY_DISCHARGE, postprocessRunState((int) ESS_MAX_SOC, (int) ESS_MAX_SOC + 1, 0, CHARGE_GRID));
		assertEquals("CHARGE_GRID and SoC is below or equal Max-SoC", //
				CHARGE_GRID, postprocessRunState((int) ESS_MAX_SOC, (int) ESS_MAX_SOC, 0, CHARGE_GRID));
	}

	@Test
	public void testCalculateExecutionLimitSeconds() {
		final var clock = new TimeLeapClock(Instant.parse("2022-01-01T00:00:00.00Z"), ZoneOffset.UTC);
		assertEquals(Duration.ofMinutes(14).plusSeconds(30).toSeconds(), calculateExecutionLimitSeconds(clock));

		clock.leap(11, ChronoUnit.MINUTES);
		assertEquals(Duration.ofMinutes(3).plusSeconds(30).toSeconds(), calculateExecutionLimitSeconds(clock));

		clock.leap(150, ChronoUnit.SECONDS);
		assertEquals(60, calculateExecutionLimitSeconds(clock));

		clock.leap(1, ChronoUnit.SECONDS);
		assertEquals(Duration.ofMinutes(15).plusSeconds(59).toSeconds(), calculateExecutionLimitSeconds(clock));
	}

	@Test
	public void testCalculateMaxChargeGridPowerFromParams() {
		final var params = createParams888d20231106(ControlMode.CHARGE_CONSUMPTION.states);
		final var ess = new DummyManagedSymmetricEss("ess0");

		// No params, initial ESS
		assertEquals(0, calculateEssChargeInChargeGridPowerFromParams(null, ess));

		// No params, ESS with MaxApparentPower
		withValue(ess, SymmetricEss.ChannelId.MAX_APPARENT_POWER, 1000);
		assertEquals(250, calculateEssChargeInChargeGridPowerFromParams(null, ess));

		// No params, ESS with Capacity
		withValue(ess, SymmetricEss.ChannelId.CAPACITY, 15000);
		assertEquals(7500, calculateEssChargeInChargeGridPowerFromParams(null, ess));

		// With params (22 kWh; but few Consumption)
		assertEquals(5360, calculateEssChargeInChargeGridPowerFromParams(params, ess));
	}

	@Test
	public void testUpdateSchedule() {
		final ZonedDateTime t = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
		final Period pOld = new Period(null, DELAY_DISCHARGE, 0, NO_FLOW);
		final Period pNew = new Period(null, BALANCING, 0, NO_FLOW);

		var schedule = new TreeMap<ZonedDateTime, Period>();
		schedule.put(t.minusMinutes(15), pOld); // old entry is removed
		schedule.put(t, pOld); // current entry stays
		schedule.put(t.plusMinutes(15), pOld); // is overridden
		schedule.put(t.plusMinutes(30), pOld); // is overridden
		schedule.put(t.plusMinutes(45), pOld); // timestamp is missing in new Schedule -> remove

		var newSchedule = ImmutableSortedMap.<ZonedDateTime, Period>naturalOrder() //
				.put(t, pNew) //
				.put(t.plusMinutes(15), pNew) //
				.put(t.plusMinutes(30), pNew) //
				.build();

		updateSchedule(t, schedule, newSchedule);

		// One old entry
		assertEquals(1, schedule.values().stream().filter(v -> v == pOld).count());

		// Two new entries
		assertEquals(2, schedule.values().stream().filter(v -> v == pNew).count());

		// No old entry
		assertEquals(0, schedule.keySet().stream().filter(tz -> tz.isBefore(t)).count());

		// Details
		assertEquals(pOld, schedule.get(t));
		assertEquals(pNew, schedule.get(t.plusMinutes(15)));
		assertEquals(pNew, schedule.get(t.plusMinutes(30)));

		// No current entry -> handle null
		schedule.remove(t);
		updateSchedule(t, schedule, newSchedule);
	}
}
