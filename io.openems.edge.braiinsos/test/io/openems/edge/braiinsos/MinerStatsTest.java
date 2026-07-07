package io.openems.edge.braiinsos;

import static io.openems.common.utils.JsonUtils.prettyToString;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.braiinsos.api.MinerStats;

public class MinerStatsTest {

	@Test
	public void testDeserialize() throws OpenemsNamedException {
		var ms = MinerStats.serializer().deserialize("""
				{
				   "pool_stats":{
				      "accepted_shares":336,
				      "rejected_shares":0,
				      "stale_shares":0,
				      "last_difficulty":65536,
				      "best_share":4252170,
				      "generated_work":18614888,
				      "last_share_time":{
				         "seconds":1757155477,
				         "nanos":966307530
				      }
				   },
				   "miner_stats":{
				      "real_hashrate":{
				         "last_5s":{
				            "gigahash_per_second":83458.9122481965
				         },
				         "last_15s":{
				            "gigahash_per_second":79149.9208918426
				         },
				         "last_30s":{
				            "gigahash_per_second":79691.95987182492
				         },
				         "last_1m":{
				            "gigahash_per_second":80901.22286652516
				         },
				         "last_5m":{
				            "gigahash_per_second":66310.95355058069
				         },
				         "last_15m":{
				            "gigahash_per_second":34589.41413026074
				         },
				         "last_30m":{
				            "gigahash_per_second":17294.70706513037
				         },
				         "last_1h":{
				            "gigahash_per_second":8647.353532565185
				         },
				         "last_24h":{
				            "gigahash_per_second":360.306397190216
				         },
				         "since_restart":{
				            "gigahash_per_second":47578.57980428093
				         }
				      },
				      "nominal_hashrate":{
				         "gigahash_per_second":82297.73339894401
				      },
				      "error_hashrate":{
				         "megahash_per_second":0.0
				      },
				      "found_blocks":0,
				      "best_share":4252170
				   },
				   "power_stats":{
				      "approximated_consumption":{
				         "watt":2288
				      },
				      "efficiency":{
				         "joule_per_terahash":28.280590419961825
				      }
				   }
				}""");

		assertEquals(2288, ms.approximatedConsumption());
	}

	@Test
	public void testSerialize() throws OpenemsNamedException {
		var j = MinerStats.serializer().serialize(new MinerStats(6789., 1234, 56.));

		assertEquals("""
				{
				  "miner_stats": {
				    "real_hashrate": {
				      "last_15s": {
				        "gigahash_per_second": 6789.0
				      }
				    }
				  },
				  "power_stats": {
				    "approximated_consumption": {
				      "watt": 1234
				    },
				    "efficiency": {
				      "joule_per_terahash": 56.0
				    }
				  }
				}""", prettyToString(j));
	}
}
