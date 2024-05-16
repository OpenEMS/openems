package io.openems.edge.energy.optimizer;

import static io.openems.common.utils.JsonUtils.getAsInt;
import static io.openems.common.utils.JsonUtils.prettyToString;
import static io.openems.common.utils.JsonUtils.toJson;
import static io.openems.common.utils.JsonUtils.toJsonArray;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.CHARGE_GRID;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DELAY_DISCHARGE;
import static io.openems.edge.energy.EnergySchedulerImplTest.CLOCK;
import static io.openems.edge.energy.EnergySchedulerImplTest.getOptimizer;
import static io.openems.edge.energy.optimizer.ScheduleDatas.fromLogString;
import static io.openems.edge.energy.optimizer.ScheduleDatas.ScheduleData.fromHistoricDataQuery;
import static io.openems.edge.energy.optimizer.Utils.SUM_CONSUMPTION;
import static io.openems.edge.energy.optimizer.Utils.SUM_ESS_DISCHARGE_POWER;
import static io.openems.edge.energy.optimizer.Utils.SUM_ESS_SOC;
import static io.openems.edge.energy.optimizer.Utils.SUM_GRID;
import static io.openems.edge.energy.optimizer.Utils.SUM_PRODUCTION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.SortedMap;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.gson.JsonElement;

import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.DateUtils;
import io.openems.edge.controller.ess.timeofusetariff.StateMachine;
import io.openems.edge.energy.EnergySchedulerImplTest;
import io.openems.edge.energy.optimizer.Params.Length;
import io.openems.edge.energy.optimizer.Params.OptimizePeriod;
import io.openems.edge.energy.optimizer.Params.QuarterPeriod;
import io.openems.edge.energy.optimizer.ScheduleDatas.ScheduleData;

public class ScheduleDatasTest {

	protected static final ZonedDateTime TIME = ZonedDateTime.of(2000, 1, 1, 0, 15, 0, 0, ZoneId.of("UTC"));

	public static final ScheduleDatas SCHEDULE_DATAS = new ScheduleDatas(22_000, ImmutableList.of(//
			new ScheduleData(TIME, null, 100, 200, 300, 1234, 222, 333, 78.9, DELAY_DISCHARGE, 987, 654),
			new ScheduleData(TIME.plusMinutes(15), null, 100, 200, 300, 4567, 444, 333, 12.3, CHARGE_GRID, 987, 654)));

	@Test
	public void testIsEmpty() {
		assertFalse(SCHEDULE_DATAS.isEmpty());
		assertTrue(new ScheduleDatas(22_000, ImmutableList.of()).isEmpty());
	}

	@Test
	public void testStream() {
		assertEquals(2, SCHEDULE_DATAS.stream().count());
	}

	@Test
	public void testToLogString() {
		assertEquals(
				"""
						OPTIMIZER Time  OptimizeBy EssMaxChargeEnergy EssMaxDischargeEnergy MaxBuyFromGrid EssInitial Production Consumption  Price State           EssChargeDischarge  Grid
						OPTIMIZER 00:15 -                         100                   200            300       1234        222         333  78.90 DELAY_DISCHARGE                987   654
						OPTIMIZER 00:30 -                         100                   200            300       4567        444         333  12.30 CHARGE_GRID                    987   654
						""",
				SCHEDULE_DATAS.toLogString("OPTIMIZER "));
	}

	@Test
	public void testToJsonArray() {
		assertEquals("""
				[
				  {
				    "timestamp": "2000-01-01T00:15:00Z",
				    "soc": 6,
				    "production": 888,
				    "consumption": 1332,
				    "state": 0,
				    "price": 78.9,
				    "ess": 3948,
				    "grid": 2616
				  },
				  {
				    "timestamp": "2000-01-01T00:30:00Z",
				    "soc": 21,
				    "production": 1776,
				    "consumption": 1332,
				    "state": 3,
				    "price": 12.3,
				    "ess": 3948,
				    "grid": 2616
				  }
				]""", prettyToString(SCHEDULE_DATAS.toJsonObjects().values().stream().collect(toJsonArray())));
	}

	@Test
	public void testFromLogString() {
		var log = SCHEDULE_DATAS.toLogString("");
		assertEquals(log, fromLogString(22_000, log).toLogString(""));
	}

	@Test
	public void testFromSchedule1() throws Exception {
		var optimizer = getOptimizer(EnergySchedulerImplTest.create(CLOCK));
		var p = optimizer.getParams();
		var s = optimizer.getSchedule();

		assertNull(p);
		assertTrue(s.isEmpty());
	}

	@Test
	public void testFromSchedule2() throws Exception {
		var sds = ScheduleDatas.fromSchedule(22_000, ImmutableSortedMap.of(//
				TIME, //
				new Simulator.Period(//
						new OptimizePeriod(TIME, Length.QUARTER, 1, 2, 3, 4, 5, 6, 7., ImmutableList.of(//
								new QuarterPeriod(TIME, 1, 2, 3, 4, 5, 6, 7))),
						StateMachine.BALANCING, 10_000,
						new EnergyFlow(0, 0, 1000 /* ess */, 500 /* grid */, 0, 0, 0, 0, 0, 0)) //
		));
		assertEquals(
				"""
						OPTIMIZER Time  OptimizeBy EssMaxChargeEnergy EssMaxDischargeEnergy MaxBuyFromGrid EssInitial Production Consumption  Price State           EssChargeDischarge  Grid
						OPTIMIZER 00:15 QUARTER                     1                     2              4      10000          5           6   7.00 BALANCING                     1000   500
						""",
				sds.toLogString("OPTIMIZER "));
	}

	@Test
	public void testFromHistoricDataQuery() throws Exception {
		final var price = ChannelAddress.fromString("_sum/GridBuyPrice");
		final var state = ChannelAddress.fromString("ctrl0/StateMachine");
		final var time = DateUtils.roundDownToQuarter(ZonedDateTime.now());

		var sd = fromHistoricDataQuery(10000 /* [Wh] */, price, state,
				ImmutableSortedMap.<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>>naturalOrder() //
						.put(time, ImmutableSortedMap.<ChannelAddress, JsonElement>naturalOrder() //
								.put(SUM_ESS_SOC, toJson(50)) //
								.put(SUM_PRODUCTION, toJson(123)) //
								.put(SUM_CONSUMPTION, toJson(234)) //
								.put(price, toJson(100)) //
								.put(state, toJson(CHARGE_GRID.getValue())) //
								.put(SUM_ESS_DISCHARGE_POWER, toJson(345)) //
								.put(SUM_GRID, toJson(456)) //
								.build()) //
						.build()) //
				.findFirst().get();

		assertEquals(5000, sd.essInitial());
		assertEquals(123 / 4, sd.production());
		assertEquals(234 / 4, sd.consumption());
		assertEquals(100., sd.price(), 0.001);
		assertEquals(CHARGE_GRID, sd.state());
		assertEquals(345 / 4, sd.essChargeDischarge());
		assertEquals(456 / 4, sd.grid());

		var j = sd.toJsonObject(10000);

		assertEquals(50, getAsInt(j, "soc"));
		assertEquals(120 /* rounding */, getAsInt(j, "production"));
		assertEquals(232, getAsInt(j, "consumption"));
		assertEquals(CHARGE_GRID.getValue(), getAsInt(j, "state"));
		assertEquals(100., getAsInt(j, "price"), 0.001);
		assertEquals(344 /* rounding */, getAsInt(j, "ess"));
		assertEquals(456, getAsInt(j, "grid"));
	}

	@Test
	public void testToJsonObjects() throws Exception {
		// Simulate duplicated timestamp
		var sds = new ScheduleDatas(SCHEDULE_DATAS.essTotalEnergy(), ImmutableList.<ScheduleData>builder() //
				.addAll(SCHEDULE_DATAS.entries()) //
				.add(SCHEDULE_DATAS.entries().get(0)) //
				.build());
		var j = sds.toJsonObjects();
		assertEquals(2, j.size());
	}

}
