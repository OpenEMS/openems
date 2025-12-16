package io.openems.edge.controller.evse.cluster;

import static io.openems.edge.energy.api.test.DummyGlobalOptimizationContext.CLOCK;
import static io.openems.edge.energy.api.test.DummyGlobalOptimizationContext.TIME;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZonedDateTime;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jscalendar.JSCalendar;
import io.openems.edge.controller.evse.cluster.EnergyScheduler.ClusterEshConfig;
import io.openems.edge.controller.evse.single.Params;
import io.openems.edge.controller.evse.single.Types.Payload;
import io.openems.edge.energy.api.RiskLevel;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.evse.api.chargepoint.Mode;

public class EshUtilsTest {

	@Test
	public void test() throws OpenemsNamedException {
		var tasks = JSCalendar.Tasks.serializer(Payload.serializer()).deserialize("""
				[
				   {
				      "@type":"Task",
				      "uid":"175578bd-2c24-40f6-abbd-a41c8b126da8",
				      "start":"01:30:00",
				      "duration":"PT4H",
				      "recurrenceRules":[
				         {
				            "frequency":"daily"
				         }
				      ],
				      "openems.io:payload":{
				         "class":"Manual",
				         "mode":"FORCE"
				      }
				   }
				]
				""");
		var params = new Params("ctrl0", null, null, 0, 0, null, null, null, false, null, tasks);
		var clusterEshConfig = new ClusterEshConfig(null, ImmutableMap.of("ctrl0", params));
		var goc = new GlobalOptimizationContext(CLOCK, RiskLevel.MEDIUM, TIME, ImmutableList.of(), ImmutableList.of(), //
				new GlobalOptimizationContext.Grid(0, 20000), //
				new GlobalOptimizationContext.Ess(0, 12223, 5000, 5000), //
				ImmutableList.of(//
						GlobalOptimizationContext.Period.Quarter.from(0, TIME.plusMinutes(0), 0, 700, 123.), //
						GlobalOptimizationContext.Period.Quarter.from(1, TIME.plusMinutes(15), 100, 600, 123.), //
						GlobalOptimizationContext.Period.Quarter.from(2, TIME.plusMinutes(30), 200, 500, 125.), //
						GlobalOptimizationContext.Period.Quarter.from(3, TIME.plusMinutes(45), 300, 400, 126.), //
						GlobalOptimizationContext.Period.Quarter.from(4, TIME.plusMinutes(60), 400, 300, 123.), //
						GlobalOptimizationContext.Period.Quarter.from(5, TIME.plusMinutes(75), 500, 200, 122.), //
						GlobalOptimizationContext.Period.Quarter.from(6, TIME.plusMinutes(90), 600, 100, 121.), //
						GlobalOptimizationContext.Period.Quarter.from(7, TIME.plusMinutes(105), 700, 0, 121.)));

		var t = EshUtils.parseTasks(goc, clusterEshConfig);
		assertEquals(Mode.FORCE, t.a().get("ctrl0", ZonedDateTime.parse("2020-01-01T01:30Z")));
		assertTrue(t.b().isEmpty());
	}

}
