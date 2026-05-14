package io.openems.edge.energy;

import static io.openems.common.utils.JsonUtils.getAsJsonArray;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.common.utils.ReflectionUtils.setAttributeViaReflection;
import static io.openems.edge.energy.GetSchedule.SUM_CONSUMPTION;
import static io.openems.edge.energy.GetSchedule.SUM_ESS_DISCHARGE_POWER;
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
			timedata.add(t, SUM_PRODUCTION, value++);
			timedata.add(t, SUM_CONSUMPTION, value++);
			timedata.add(t, SUM_UNMANAGED_CONSUMPTION, value++);
			timedata.add(t, TIME_OF_USE_MODE, value % 3);
		}

		final var routes = new JsonApiBuilder();
		final var optimizer = EnergySchedulerImplTest.getOptimizer(sut);
		setAttributeViaReflection(optimizer, "latestSimulationResult", DUMMY_PREVIOUS_RESULT);
		sut.buildJsonApiRoutes(routes);

		final var from = ZonedDateTime.now(clock).minusHours(4);
		final var call = new Call<JsonrpcRequest, JsonrpcResponse>(
				GenericJsonrpcRequest.createRequest(new GetSchedule(), new GetSchedule.Request(from)));
		routes.handle(call);

		final var response = call.getResponse().toJsonObject();
		final var result = getAsJsonObject(response, "result");
		final var data = getAsJsonArray(result, "data");

		// First Entry
		assertEquals("""
				{
				  "timestamp": "2019-12-31T20:00:00Z",
				  "type": "HISTORY",
				  "_sum": {
				    "GridBuyPrice": 1200.0,
				    "GridSellPrice": 1201.0,
				    "ProductionActivePower": 1204,
				    "ConsumptionActivePower": 1205,
				    "UnmanagedConsumptionActivePower": 1206,
				    "EssDischargePower": 1203,
				    "GridActivePower": 1202
				  },
				  "eshs": [
				    {
				      "id": "ctrlEssTimeOfUseTariff0",
				      "mode": "DELAY_DISCHARGE"
				    }
				  ]
				}""", JsonUtils.prettyToString(data.get(0)));

		// Second Entry
		assertEquals("""
				{
				  "timestamp": "2019-12-31T20:05:00Z",
				  "type": "HISTORY",
				  "_sum": {
				    "GridBuyPrice": 1205.0,
				    "GridSellPrice": 1206.0,
				    "ProductionActivePower": 1209,
				    "ConsumptionActivePower": 1210,
				    "UnmanagedConsumptionActivePower": 1211,
				    "EssDischargePower": 1208,
				    "GridActivePower": 1207
				  },
				  "eshs": [
				    {
				      "id": "ctrlEssTimeOfUseTariff0",
				      "mode": "BALANCING"
				    }
				  ]
				}""", JsonUtils.prettyToString(data.get(1)));

		// Last History Entry
		assertEquals("""
				{
				  "timestamp": "2019-12-31T23:55:00Z",
				  "type": "HISTORY",
				  "_sum": {
				    "GridBuyPrice": 1435.0,
				    "GridSellPrice": 1436.0,
				    "ProductionActivePower": 1439,
				    "ConsumptionActivePower": 1440,
				    "UnmanagedConsumptionActivePower": 1441,
				    "EssDischargePower": 1438,
				    "GridActivePower": 1437
				  },
				  "eshs": [
				    {
				      "id": "ctrlEssTimeOfUseTariff0",
				      "mode": "CHARGE_GRID"
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
				    "GridActivePower": 424
				  },
				  "eshs": [
				    {
				      "id": "ctrlEssTimeOfUseTariff0",
				      "mode": "DELAY_DISCHARGE"
				    },
				    {
				      "id": "esh2",
				      "mode": "BAR"
				    }
				  ]
				}""", JsonUtils.prettyToString(data.get(48)));

		// Last Entry in SimulationResult
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
				    "GridActivePower": -11944
				  },
				  "eshs": [
				    {
				      "id": "ctrlEssTimeOfUseTariff0",
				      "mode": "BALANCING"
				    },
				    {
				      "id": "esh2",
				      "mode": "FOO"
				    }
				  ]
				}""", JsonUtils.prettyToString(data.get(99)));

		// Last Entry in SimulationResult
		assertEquals("""
				{
				  "timestamp": "2020-01-01T19:45:00Z",
				  "type": "PREDICTION",
				  "_sum": {
				    "GridBuyPrice": 140.01,
				    "ProductionActivePower": 0,
				    "ConsumptionActivePower": 417,
				    "UnmanagedConsumptionActivePower": 417
				  },
				  "eshs": []
				}""", JsonUtils.prettyToString(data.get(data.size() - 1)));
	}

}
