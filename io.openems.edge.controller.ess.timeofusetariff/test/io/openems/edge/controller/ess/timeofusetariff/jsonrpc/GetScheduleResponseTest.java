package io.openems.edge.controller.ess.timeofusetariff.jsonrpc;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;
import static io.openems.common.utils.JsonUtils.getAsJsonArray;
import static io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffControllerImpl.applyBalancing;
import static io.openems.edge.controller.ess.timeofusetariff.UtilsTest.CLOCK;
import static io.openems.edge.controller.ess.timeofusetariff.jsonrpc.TestData.CONSUMPTION_PREDICTION_QUARTERLY;
import static io.openems.edge.controller.ess.timeofusetariff.jsonrpc.TestData.PAST_HOURLY_PRICES;
import static io.openems.edge.controller.ess.timeofusetariff.jsonrpc.TestData.PAST_SOC;
import static io.openems.edge.controller.ess.timeofusetariff.jsonrpc.TestData.PAST_STATES;
import static io.openems.edge.controller.ess.timeofusetariff.jsonrpc.TestData.PRODUCTION_888_20231106;
import static io.openems.edge.controller.ess.timeofusetariff.jsonrpc.TestData.PRODUCTION_PREDICTION_QUARTERLY;
import static org.junit.Assert.assertEquals;

import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.UuidUtils;
import io.openems.edge.controller.ess.timeofusetariff.StateMachine;
import io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffControllerImplTest;
import io.openems.edge.controller.ess.timeofusetariff.Utils;
import io.openems.edge.energy.api.EnergyScheduleHandler.WithDifferentStates.Period;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalSimulationsContext;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.timedata.test.DummyTimedata;

public class GetScheduleResponseTest {

	@Test
	public void test() throws Exception {
		final var now = roundDownToQuarter(ZonedDateTime.now(CLOCK));
		final var ess = new DummyManagedSymmetricEss("ess0") //
				.withCapacity(10000);
		final var model = new EnergyFlow.Model(//
				/* production */ 2500, //
				/* consumption */ 500, //
				/* essMaxCharge */ 5000, //
				/* essMaxDischarge */ 0, //
				/* gridMaxBuy */ 4000, //
				/* gridMaxSell */ 10000);
		applyBalancing(model);
		final var energyFlow = model.solve();

		// Simulate historic data
		final var timedata = new DummyTimedata("timedata0");
		final var fromDate = now.minusHours(3);
		for (var i = 0; i < 12; i++) {
			var quarter = fromDate.plusMinutes(i * 15);
			timedata.add(quarter, new ChannelAddress("ctrl0", "QuarterlyPrices"), PAST_HOURLY_PRICES[i]);
			timedata.add(quarter, new ChannelAddress("ctrl0", "StateMachine"), PAST_STATES[i]);
			timedata.add(quarter, Utils.SUM_PRODUCTION, PRODUCTION_PREDICTION_QUARTERLY[i]);
			timedata.add(quarter, Utils.SUM_CONSUMPTION, CONSUMPTION_PREDICTION_QUARTERLY[i]);
			timedata.add(quarter, Utils.SUM_ESS_SOC, PAST_SOC[i]);
			timedata.add(quarter, Utils.SUM_ESS_DISCHARGE_POWER, PRODUCTION_888_20231106[i]);
			timedata.add(quarter, Utils.SUM_GRID, PRODUCTION_888_20231106[i]);
		}

		// Simulate future Schedule
		var ctrl = TimeOfUseTariffControllerImplTest.create(CLOCK, ess, timedata);
		var esh = TimeOfUseTariffControllerImplTest.getEnergyScheduleHandler(ctrl);
		esh.onBeforeSimulation(new GlobalSimulationsContext(CLOCK, new AtomicInteger(), null, null, null,
				new GlobalSimulationsContext.Ess(0, 0, 0, 0), ImmutableList.of()));
		esh.applySchedule(ImmutableSortedMap.<ZonedDateTime, Period.Transition>naturalOrder() //
				.put(now.plusMinutes(0), new Period.Transition(1, 0.1, energyFlow, 5000)) //
				.put(now.plusMinutes(15), new Period.Transition(0, 0.2, energyFlow, 6000)) //
				.put(now.plusMinutes(30), new Period.Transition(0, 0.3, energyFlow, 7000)) //
				.build());

		final var gsr = GetScheduleResponse.from(UuidUtils.getNilUuid(), "ctrl0", CLOCK, ess, timedata, esh);

		var schedule = getAsJsonArray(gsr.getResult(), "schedule");

		assertEquals("""
				[
				  {
				    "timestamp": "1999-12-31T21:00:00Z",
				    "price": 158.0,
				    "state": 1,
				    "grid": 0,
				    "production": 0,
				    "consumption": 1021,
				    "ess": 0,
				    "soc": 60
				  },
				  {
				    "timestamp": "1999-12-31T21:15:00Z",
				    "price": 160.0,
				    "state": 1,
				    "grid": 0,
				    "production": 0,
				    "consumption": 1208,
				    "ess": 0,
				    "soc": 62
				  },
				  {
				    "timestamp": "1999-12-31T21:30:00Z",
				    "price": 171.0,
				    "state": 1,
				    "grid": 0,
				    "production": 0,
				    "consumption": 713,
				    "ess": 0,
				    "soc": 64
				  },
				  {
				    "timestamp": "1999-12-31T21:45:00Z",
				    "price": 174.0,
				    "state": 1,
				    "grid": 0,
				    "production": 0,
				    "consumption": 931,
				    "ess": 0,
				    "soc": 66
				  },
				  {
				    "timestamp": "1999-12-31T22:00:00Z",
				    "price": 161.0,
				    "state": 1,
				    "grid": 0,
				    "production": 0,
				    "consumption": 2847,
				    "ess": 0,
				    "soc": 65
				  },
				  {
				    "timestamp": "1999-12-31T22:15:00Z",
				    "price": 152.0,
				    "state": 3,
				    "grid": 0,
				    "production": 0,
				    "consumption": 2551,
				    "ess": 0,
				    "soc": 67
				  },
				  {
				    "timestamp": "1999-12-31T22:30:00Z",
				    "price": 120.0,
				    "state": 3,
				    "grid": 0,
				    "production": 0,
				    "consumption": 1558,
				    "ess": 0,
				    "soc": 70
				  },
				  {
				    "timestamp": "1999-12-31T22:45:00Z",
				    "price": 111.0,
				    "state": 1,
				    "grid": 0,
				    "production": 0,
				    "consumption": 1234,
				    "ess": 0,
				    "soc": 73
				  },
				  {
				    "timestamp": "1999-12-31T23:00:00Z",
				    "price": 105.0,
				    "state": 2,
				    "grid": 0,
				    "production": 0,
				    "consumption": 433,
				    "ess": 0,
				    "soc": 76
				  },
				  {
				    "timestamp": "1999-12-31T23:15:00Z",
				    "price": 105.0,
				    "state": 1,
				    "grid": 0,
				    "production": 0,
				    "consumption": 633,
				    "ess": 0,
				    "soc": 79
				  },
				  {
				    "timestamp": "1999-12-31T23:30:00Z",
				    "price": 74.0,
				    "state": 2,
				    "grid": 0,
				    "production": 0,
				    "consumption": 1355,
				    "ess": 0,
				    "soc": 83
				  },
				  {
				    "timestamp": "1999-12-31T23:45:00Z",
				    "price": 73.0,
				    "state": 2,
				    "grid": 0,
				    "production": 0,
				    "consumption": 606,
				    "ess": 0,
				    "soc": 87
				  },
				  {
				    "timestamp": "2000-01-01T00:00:00Z",
				    "price": 0.1,
				    "state": 0,
				    "grid": 0,
				    "production": 10000,
				    "consumption": 2000,
				    "ess": -8000,
				    "soc": 50
				  },
				  {
				    "timestamp": "2000-01-01T00:15:00Z",
				    "price": 0.2,
				    "state": 1,
				    "grid": 0,
				    "production": 10000,
				    "consumption": 2000,
				    "ess": -8000,
				    "soc": 60
				  },
				  {
				    "timestamp": "2000-01-01T00:30:00Z",
				    "price": 0.3,
				    "state": 1,
				    "grid": 0,
				    "production": 10000,
				    "consumption": 2000,
				    "ess": -8000,
				    "soc": 70
				  }
				]""", JsonUtils.prettyToString(schedule));
	}

	@Test
	public void testEmpty() throws OpenemsNamedException {
		var response = GetScheduleResponse.empty(CLOCK, StateMachine.BALANCING).toList().get(0);
		assertEquals(StateMachine.BALANCING.getValue(), JsonUtils.getAsInt(response, "state"));
	}
}
