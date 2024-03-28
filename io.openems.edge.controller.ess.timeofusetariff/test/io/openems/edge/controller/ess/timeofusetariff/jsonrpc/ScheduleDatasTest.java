package io.openems.edge.controller.ess.timeofusetariff.jsonrpc;

import static io.openems.common.utils.JsonUtils.prettyToString;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.CHARGE_GRID;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DELAY_DISCHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffControllerImplTest.CLOCK;
import static io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffControllerImplTest.callCreateParams;
import static io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffControllerImplTest.getOptimizer;
import static io.openems.edge.controller.ess.timeofusetariff.jsonrpc.ScheduleDatas.fromLogString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;

import io.openems.edge.controller.ess.timeofusetariff.StateMachine;
import io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffControllerImplTest;
import io.openems.edge.controller.ess.timeofusetariff.jsonrpc.ScheduleDatas.ScheduleData;
import io.openems.edge.controller.ess.timeofusetariff.optimizer.EnergyFlow;
import io.openems.edge.controller.ess.timeofusetariff.optimizer.Params.OptimizePeriod;
import io.openems.edge.controller.ess.timeofusetariff.optimizer.Params.QuarterPeriod;
import io.openems.edge.controller.ess.timeofusetariff.optimizer.Simulator;

public class ScheduleDatasTest {

	protected static final ZonedDateTime TIME = ZonedDateTime.of(2000, 1, 1, 0, 15, 0, 0, ZoneId.of("UTC"));

	protected static final ScheduleDatas SCHEDULE_DATAS = new ScheduleDatas(22_000, ImmutableList.of(//
			new ScheduleData(TIME, null, 100, 200, 1234, 222, 333, 78.9, DELAY_DISCHARGE, 987, 654),
			new ScheduleData(TIME.plusMinutes(15), null, 100, 200, 4567, 444, 333, 12.3, CHARGE_GRID, 987, 654)));

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
						OPTIMIZER Time  OptimizeBy EssMaxEnergy MaxBuyFromGrid EssInitial Production Consumption  Price State           EssChargeDischarge  Grid
						OPTIMIZER 00:15 -                   100            200       1234        222         333  78.90 DELAY_DISCHARGE                987   654
						OPTIMIZER 00:30 -                   100            200       4567        444         333  12.30 CHARGE_GRID                    987   654
						""",
				SCHEDULE_DATAS.toLogString("OPTIMIZER "));
	}

	@Test
	public void testToJsonArray() {
		assertEquals("""
				[
				  {
				    "timestamp": "2000-01-01T00:00:00Z",
				    "soc": null,
				    "production": null,
				    "consumption": null,
				    "state": null,
				    "price": null,
				    "ess": null,
				    "grid": null
				  },
				  {
				    "timestamp": "2000-01-01T00:15:00Z",
				    "soc": 6,
				    "production": 222,
				    "consumption": 333,
				    "state": 0,
				    "price": 78.9,
				    "ess": 987,
				    "grid": 654
				  },
				  {
				    "timestamp": "2000-01-01T00:30:00Z",
				    "soc": 21,
				    "production": 444,
				    "consumption": 333,
				    "state": 3,
				    "price": 12.3,
				    "ess": 987,
				    "grid": 654
				  }
				]""", prettyToString(SCHEDULE_DATAS.toJsonArray(TIME.minusMinutes(15))));
	}

	@Test
	public void testFromLogString() {
		var log = SCHEDULE_DATAS.toLogString("");
		assertEquals(log, fromLogString(22_000, log).toLogString(""));
	}

	@Test
	public void testFromSchedule1() throws Exception {
		var optimizer = getOptimizer(TimeOfUseTariffControllerImplTest.create(CLOCK));
		callCreateParams(optimizer);
		var sds = ScheduleDatas.fromSchedule(optimizer);
		assertEquals(
				"""
						Time  OptimizeBy EssMaxEnergy MaxBuyFromGrid EssInitial Production Consumption  Price State           EssChargeDischarge  Grid
						""",
				sds.toLogString(""));
	}

	@Test
	public void testFromSchedule2() throws Exception {
		var sds = ScheduleDatas.fromSchedule(22_000, ImmutableSortedMap.of(//
				TIME, //
				new Simulator.Period(//
						new OptimizePeriod(TIME, 1, 2, 3, 4, 5, 6., ImmutableList.of(//
								new QuarterPeriod(TIME, 1, 2, 3, 4, 5, 6))),
						StateMachine.BALANCING, 10_000,
						new EnergyFlow(0, 0, 1000 /* ess */, 500 /* grid */, 0, 0, 0, 0, 0, 0)) //
		));
		assertEquals(
				"""
						OPTIMIZER Time  OptimizeBy EssMaxEnergy MaxBuyFromGrid EssInitial Production Consumption  Price State           EssChargeDischarge  Grid
						OPTIMIZER 00:15 QUARTER               1              3      10000          4           5   6.00 BALANCING                     1000   500
						""",
				sds.toLogString("OPTIMIZER "));
	}

}
