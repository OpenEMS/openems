package io.openems.edge.controller.ess.timeofusetariff.jsonrpc;

import static io.openems.common.utils.JsonUtils.prettyToString;
import static io.openems.common.utils.UuidUtils.getNilUuid;
import static io.openems.edge.controller.ess.timeofusetariff.jsonrpc.ScheduleDatasTest.SCHEDULE_DATAS;
import static io.openems.edge.controller.ess.timeofusetariff.jsonrpc.ScheduleDatasTest.TIME;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

public class GetScheduleResponseTest {

	@Test
	public void testToJsonObject() throws OpenemsNamedException {
		var response = new GetScheduleResponse(getNilUuid(), TIME, SCHEDULE_DATAS);

		assertEquals("""
				{
				  "jsonrpc": "2.0",
				  "id": "00000000-0000-0000-0000-000000000000",
				  "result": {
				    "schedule": [
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
				    ]
				  }
				}""", prettyToString(response.toJsonObject()));
	}

}
