package io.openems.edge.controller.evse.cluster;

import static io.openems.common.jscalendar.JSCalendar.RecurrenceFrequency.DAILY;
import static io.openems.edge.energy.api.test.DummyGlobalOptimizationContext.CLOCK;
import static io.openems.edge.energy.api.test.DummyGlobalOptimizationContext.TIME;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
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
		assertEquals(Mode.FORCE, t.manualModes().get("ctrl0", ZonedDateTime.parse("2020-01-01T01:30Z")));
		assertTrue(t.smartPayloads().isEmpty());
		assertTrue(t.smartDeadlines().isEmpty());
	}

	@Test
	public void testSmartTaskDeadline() throws OpenemsNamedException {
		// Smart task: 01:30 for 45 min (→ end 02:15), minimum 5000 Wh.
		// Periods cover 00:00–02:15 (lastTime = 02:15).
		// Loop adds Smart payloads for 01:30, 01:45, 02:00; last slot = 02:00.
		// Expected deadline: ("ctrl0", 02:00) → 5000
		var tasks = JSCalendar.Tasks.<Payload>create() //
				.add(t -> t //
						.setStart("01:30") //
						.setDuration(Duration.ofMinutes(45)) //
						.addRecurrenceRule(r -> r.setFrequency(DAILY)) //
						.setPayload(new Payload.Smart(5000))) //
				.build();
		var params = new Params("ctrl0", null, null, 0, 0, null, null, null, false, null, tasks);
		var clusterEshConfig = new ClusterEshConfig(null, ImmutableMap.of("ctrl0", params));
		var goc = new GlobalOptimizationContext(CLOCK, Environment.PRODUCTION, TIME, ImmutableList.of(),
				ImmutableList.of(), //
				new GlobalOptimizationContext.Grid(0, 20000, JSCalendar.Tasks.empty()), //
				new GlobalOptimizationContext.Ess(0, 12223, 5000, 5000), //
				GlobalOptimizationContext.Periods.create(Environment.PRODUCTION) //
						.add(TIME.plusMinutes(0), null, 0, 700, 123.) //   00:00
						.add(TIME.plusMinutes(15), null, 0, 700, 123.) //  00:15
						.add(TIME.plusMinutes(30), null, 0, 700, 123.) //  00:30
						.add(TIME.plusMinutes(45), null, 0, 700, 123.) //  00:45
						.add(TIME.plusMinutes(60), null, 0, 700, 123.) //  01:00
						.add(TIME.plusMinutes(75), null, 0, 700, 123.) //  01:15
						.add(TIME.plusMinutes(90), null, 0, 700, 123.) //  01:30 — Smart window start
						.add(TIME.plusMinutes(105), null, 0, 700, 123.) // 01:45
						.add(TIME.plusMinutes(120), null, 0, 700, 123.) // 02:00
						.add(TIME.plusMinutes(135), null, 0, 700, 123.) // 02:15 — lastTime
						.build());

		var t = EshUtils.parseTasks(goc, clusterEshConfig);

		// No manual overrides
		assertTrue(t.manualModes().isEmpty());

		// Smart payloads cover exactly the 3 periods inside the window
		assertEquals(3, t.smartPayloads().size());
		assertNotNull(t.smartPayloads().get("ctrl0", TIME.plusMinutes(90)));  // 01:30
		assertNotNull(t.smartPayloads().get("ctrl0", TIME.plusMinutes(105))); // 01:45
		assertNotNull(t.smartPayloads().get("ctrl0", TIME.plusMinutes(120))); // 02:00

		// Deadline is the last period in the window with the required minimum energy
		assertEquals(1, t.smartDeadlines().size());
		assertEquals(Integer.valueOf(5000), t.smartDeadlines().get("ctrl0", TIME.plusMinutes(120)));
	}

	@Test
	public void testSmartTaskNoDeadlineWhenMinimumIsZero() throws OpenemsNamedException {
		// Smart task with sessionEnergyMinimum = 0 must NOT register any deadline.
		var tasks = JSCalendar.Tasks.<Payload>create() //
				.add(t -> t //
						.setStart("01:30") //
						.setDuration(Duration.ofMinutes(45)) //
						.addRecurrenceRule(r -> r.setFrequency(DAILY)) //
						.setPayload(new Payload.Smart(0))) //
				.build();
		var params = new Params("ctrl0", null, null, 0, 0, null, null, null, false, null, tasks);
		var clusterEshConfig = new ClusterEshConfig(null, ImmutableMap.of("ctrl0", params));
		var goc = new GlobalOptimizationContext(CLOCK, Environment.PRODUCTION, TIME, ImmutableList.of(),
				ImmutableList.of(), //
				new GlobalOptimizationContext.Grid(0, 20000, JSCalendar.Tasks.empty()), //
				new GlobalOptimizationContext.Ess(0, 12223, 5000, 5000), //
				GlobalOptimizationContext.Periods.create(Environment.PRODUCTION) //
						.add(TIME.plusMinutes(0), null, 0, 700, 123.) //
						.add(TIME.plusMinutes(15), null, 0, 700, 123.) //
						.add(TIME.plusMinutes(30), null, 0, 700, 123.) //
						.add(TIME.plusMinutes(45), null, 0, 700, 123.) //
						.add(TIME.plusMinutes(60), null, 0, 700, 123.) //
						.add(TIME.plusMinutes(75), null, 0, 700, 123.) //
						.add(TIME.plusMinutes(90), null, 0, 700, 123.) //
						.add(TIME.plusMinutes(105), null, 0, 700, 123.) //
						.add(TIME.plusMinutes(120), null, 0, 700, 123.) //
						.add(TIME.plusMinutes(135), null, 0, 700, 123.) //
						.build());

		var t = EshUtils.parseTasks(goc, clusterEshConfig);

		// Smart payloads are still populated (window is valid)
		assertEquals(3, t.smartPayloads().size());
		// But no deadline because sessionEnergyMinimum = 0
		assertTrue(t.smartDeadlines().isEmpty());
		assertNull(t.smartDeadlines().get("ctrl0", TIME.plusMinutes(120)));
	}

}
