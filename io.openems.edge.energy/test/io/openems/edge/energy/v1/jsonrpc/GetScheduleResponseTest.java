package io.openems.edge.energy.v1.jsonrpc;

import static io.openems.common.utils.JsonUtils.prettyToString;
import static io.openems.common.utils.UuidUtils.getNilUuid;
import static io.openems.edge.energy.v1.optimizer.ScheduleDatasTest.SCHEDULE_DATAS;
import static io.openems.edge.energy.v1.optimizer.SimulatorTest.TIME;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

public class GetScheduleResponseTest {

	@Test
	public void testToJsonObject() throws OpenemsNamedException {
		var response = new GetScheduleResponse(getNilUuid(), TIME, TIME.plusMinutes(30), SCHEDULE_DATAS);

		assertEquals("""
				{
				  "jsonrpc": "2.0",
				  "id": "00000000-0000-0000-0000-000000000000",
				  "result": {
				    "schedule": [
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
				    ]
				  }
				}""", prettyToString(response.toJsonObject()));
	}

}
