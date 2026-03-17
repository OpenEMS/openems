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
import io.openems.edge.energy.api.Environment;
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
		var goc = new GlobalOptimizationContext(CLOCK, Environment.PRODUCTION, TIME, ImmutableList.of(), ImmutableList.of(), //
				new GlobalOptimizationContext.Grid(0, 20000, JSCalendar.Tasks.empty()), //
				new GlobalOptimizationContext.Ess(0, 12223, 5000, 5000), //
				GlobalOptimizationContext.Periods.create(Environment.PRODUCTION) //
						.add(TIME.plusMinutes(0), null, 0, 700, 123.) //
						.add(TIME.plusMinutes(15), null, 100, 600, 123.) //
						.add(TIME.plusMinutes(30), null, 200, 500, 125.) //
						.add(TIME.plusMinutes(45), null, 300, 400, 126.) //
						.add(TIME.plusMinutes(60), null, 400, 300, 123.) //
						.add(TIME.plusMinutes(75), null, 500, 200, 122.) //
						.add(TIME.plusMinutes(90), null, 600, 100, 121.) //
						.add(TIME.plusMinutes(105), null, 700, 0, 121.) //
						.build());

		var t = EshUtils.parseTasks(goc, clusterEshConfig);
		assertEquals(Mode.FORCE, t.a().get("ctrl0", ZonedDateTime.parse("2020-01-01T01:30Z")));
		assertTrue(t.b().isEmpty());
	}

}
