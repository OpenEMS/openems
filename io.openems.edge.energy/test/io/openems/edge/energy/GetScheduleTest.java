package io.openems.edge.energy;

import static io.openems.common.utils.JsonUtils.getAsJsonArray;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.common.utils.ReflectionUtils.setAttributeViaReflection;
import static io.openems.edge.energy.GetSchedule.SUM_CONSUMPTION;
import static io.openems.edge.energy.GetSchedule.SUM_ESS_DISCHARGE_POWER;
import static io.openems.edge.energy.GetSchedule.SUM_ESS_SOC;
import static io.openems.edge.energy.GetSchedule.SUM_GRID;
import static io.openems.edge.energy.GetSchedule.SUM_GRID_BUY_PRICE;
import static io.openems.edge.energy.GetSchedule.SUM_GRID_SELL_PRICE;
import static io.openems.edge.energy.GetSchedule.SUM_PRODUCTION;
import static io.openems.edge.energy.GetSchedule.SUM_UNMANAGED_CONSUMPTION;
import static io.openems.edge.energy.optimizer.SimulatorTest.DUMMY_PREVIOUS_RESULT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;
import io.openems.common.test.TestUtils;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.jsonapi.Call;
import io.openems.edge.common.jsonapi.JsonApiBuilder;

class GetScheduleTest {

	private static final ChannelAddress TIME_OF_USE_MODE = new ChannelAddress("ctrlEssTimeOfUseTariff0",
			"StateMachine");
	private static final ChannelAddress EVSE_0_MODE = new ChannelAddress("ctrlEvseSingle0", "ActualMode");
	private static final ChannelAddress EVSE_0_POWER = new ChannelAddress("evseChargePoint0", "ActivePower");
	private static final ChannelAddress EVSE_1_MODE = new ChannelAddress("ctrlEvseSingle1", "ActualMode");
	private static final ChannelAddress EVSE_1_POWER = new ChannelAddress("evseChargePoint1", "ActivePower");

	@Test
	void test() throws Exception {
		final var clock = TestUtils.createDummyClock();
		final var now = ZonedDateTime.now(clock);
		final var sut = EnergySchedulerImplTest.create(clock);

		// Simulate historic data
		final var timedata = EnergySchedulerImplTest.getTimedata(sut);
		for (var t = now.minusHours(24); t.isBefore(now); t = t.plusMinutes(5)) {
			var value = t.getHour() * 60 + t.getMinute(); // just some reproducible, unique value
			timedata.add(t, SUM_GRID_BUY_PRICE, value++);
			timedata.add(t, SUM_GRID_SELL_PRICE, value++);
			timedata.add(t, SUM_GRID, value++);
			timedata.add(t, SUM_ESS_DISCHARGE_POWER, value++);
			timedata.add(t, SUM_ESS_SOC, Math.round(value++ / 100f));
			timedata.add(t, SUM_PRODUCTION, value++);
			timedata.add(t, SUM_CONSUMPTION, value++);
			timedata.add(t, SUM_UNMANAGED_CONSUMPTION, value++);
			timedata.add(t, TIME_OF_USE_MODE, value++ % 4);
			timedata.add(t, EVSE_0_MODE, value++ % 4);
			timedata.add(t, EVSE_1_MODE, value++ % 4);
			timedata.add(t, EVSE_0_POWER, value++);
			timedata.add(t, EVSE_1_POWER, value++);
		}

		final var routes = new JsonApiBuilder();
		final var optimizer = EnergySchedulerImplTest.getOptimizer(sut);
		setAttributeViaReflection(optimizer, "latestSimulationResult", DUMMY_PREVIOUS_RESULT);
		sut.buildJsonApiRoutes(routes);

		final var from = ZonedDateTime.now(clock) //
				.minusHours(4) //
				.plusMinutes(9); // fake non-even request
		final var call = new Call<JsonrpcRequest, JsonrpcResponse>(
				GenericJsonrpcRequest.createRequest(new GetSchedule(), new GetSchedule.Request(from)));
		routes.handle(call);

		final var response = call.getResponse().toJsonObject();
		final var result = getAsJsonObject(response, "result");
		final var data = getAsJsonArray(result, "data");

		// First Entry
		assertEquals("""
				{
				  "timestamp": "2019-12-31T00:00:00Z",
				  "type": "HISTORY",
				  "_sum": {
				    "GridBuyPrice": 0.0,
				    "GridSellPrice": 1.0,
				    "ProductionActivePower": 5,
				    "ConsumptionActivePower": 6,
				    "UnmanagedConsumptionActivePower": 7,
				    "EssDischargePower": 3,
				    "EssSoc": 0,
				    "GridActivePower": 2
				  },
				  "eshs": [
				    {
				      "id": "ctrlEssTimeOfUseTariff0",
				      "mode": 0
				    },
				    {
				      "id": "ctrlEvseSingle0",
				      "mode": 1,
				      "managedConsumption": 11
				    },
				    {
				      "id": "ctrlEvseSingle1",
				      "mode": 2,
				      "managedConsumption": 12
				    }
				  ]
				}""", JsonUtils.prettyToString(data.get(0)));

		// Second Entry
		assertEquals("""
				{
				  "timestamp": "2019-12-31T00:05:00Z",
				  "type": "HISTORY",
				  "_sum": {
				    "GridBuyPrice": 5.0,
				    "GridSellPrice": 6.0,
				    "ProductionActivePower": 10,
				    "ConsumptionActivePower": 11,
				    "UnmanagedConsumptionActivePower": 12,
				    "EssDischargePower": 8,
				    "EssSoc": 0,
				    "GridActivePower": 7
				  },
				  "eshs": [
				    {
				      "id": "ctrlEssTimeOfUseTariff0",
				      "mode": 1
				    },
				    {
				      "id": "ctrlEvseSingle0",
				      "mode": 2,
				      "managedConsumption": 16
				    },
				    {
				      "id": "ctrlEvseSingle1",
				      "mode": 3,
				      "managedConsumption": 17
				    }
				  ]
				}""", JsonUtils.prettyToString(data.get(1)));

		// Last History Entry
		assertEquals("""
				{
				  "timestamp": "2019-12-31T03:55:00Z",
				  "type": "HISTORY",
				  "_sum": {
				    "GridBuyPrice": 235.0,
				    "GridSellPrice": 236.0,
				    "ProductionActivePower": 240,
				    "ConsumptionActivePower": 241,
				    "UnmanagedConsumptionActivePower": 242,
				    "EssDischargePower": 238,
				    "EssSoc": 2,
				    "GridActivePower": 237
				  },
				  "eshs": [
				    {
				      "id": "ctrlEssTimeOfUseTariff0",
				      "mode": 3
				    },
				    {
				      "id": "ctrlEvseSingle0",
				      "mode": 0,
				      "managedConsumption": 246
				    },
				    {
				      "id": "ctrlEvseSingle1",
				      "mode": 1,
				      "managedConsumption": 247
				    }
				  ]
				}""", JsonUtils.prettyToString(data.get(47)));

		// First Prediction Entry
		assertEquals("""
				{
				  "timestamp": "2020-01-01T00:00:00Z",
				  "type": "PREDICTION",
				  "_sum": {
				    "GridBuyPrice": 293.7,
				    "ProductionActivePower": 0,
				    "ConsumptionActivePower": 424,
				    "UnmanagedConsumptionActivePower": 424,
				    "EssDischargePower": 0,
				    "EssSoc": 23,
				    "GridActivePower": 424
				  },
				  "eshs": [
				    {
				      "id": "ctrlEssTimeOfUseTariff0",
				      "mode": 0
				    },
				    {
				      "id": "ctrlEvseSingle0",
				      "mode": 3
				    },
				    {
				      "id": "ctrlEvseSingle1",
				      "mode": 2
				    },
				    {
				      "id": "esh2",
				      "mode": 2
				    }
				  ]
				}""", JsonUtils.prettyToString(data.get(288)));

		// Last Prediction Entry
		assertEquals("""
				{
				  "timestamp": "2020-01-01T12:45:00Z",
				  "type": "PREDICTION",
				  "_sum": {
				    "GridBuyPrice": 260.7,
				    "ProductionActivePower": 12180,
				    "ConsumptionActivePower": 236,
				    "UnmanagedConsumptionActivePower": 236,
				    "EssDischargePower": 0,
				    "EssSoc": 100,
				    "GridActivePower": -11944
				  },
				  "eshs": [
				    {
				      "id": "ctrlEssTimeOfUseTariff0",
				      "mode": 1
				    },
				    {
				      "id": "ctrlEvseSingle0",
				      "mode": 3
				    },
				    {
				      "id": "ctrlEvseSingle1",
				      "mode": 2
				    },
				    {
				      "id": "esh2",
				      "mode": 1
				    }
				  ]
				}""", JsonUtils.prettyToString(data.get(339)));

		// Last Entry in SimulationResult
		assertEquals("""
				{
				  "timestamp": "2020-01-01T23:45:00Z",
				  "type": "PREDICTION",
				  "_sum": {
				    "GridBuyPrice": 120.14,
				    "ProductionActivePower": 0,
				    "ConsumptionActivePower": 660,
				    "UnmanagedConsumptionActivePower": 660
				  },
				  "eshs": []
				}""", JsonUtils.prettyToString(data.get(data.size() - 1)));
	}

}
