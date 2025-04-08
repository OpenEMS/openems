package io.openems.edge.controller.evse.single;

import static io.openems.common.jscalendar.JSCalendar.RecurrenceFrequency.WEEKLY;
import static io.openems.common.utils.JsonUtils.getAsJsonArray;
import static io.openems.edge.controller.evse.single.EnergyScheduler.buildManualEnergyScheduleHandler;
import static io.openems.edge.controller.evse.single.EnergyScheduler.buildSmartEnergyScheduleHandler;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.apache.commons.math3.optim.nonlinear.scalar.GoalType.MAXIMIZE;
import static org.junit.Assert.assertEquals;

import java.time.LocalTime;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.utils.UuidUtils;
import io.openems.edge.controller.evse.single.EnergyScheduler.Payload;
import io.openems.edge.controller.evse.single.EnergyScheduler.ScheduleContext;
import io.openems.edge.controller.evse.single.EnergyScheduler.SmartOptimizationContext;
import io.openems.edge.controller.evse.single.jsonrpc.GetScheduleResponse;
import io.openems.edge.controller.test.DummyController;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler.Fitness;
import io.openems.edge.energy.api.handler.EshWithDifferentModes;
import io.openems.edge.energy.api.simulation.Coefficient;
import io.openems.edge.energy.api.test.EnergyScheduleTester;
import io.openems.edge.evse.api.Limit;
import io.openems.edge.evse.api.SingleThreePhase;
import io.openems.edge.evse.api.chargepoint.Mode;
import io.openems.edge.evse.api.chargepoint.Mode.Actual;

public class EnergySchedulerTest {

	private static final Limit THREE_PHASE = new Limit(SingleThreePhase.THREE_PHASE, 6000, 32000);
	private static final Limit SINGLE_PHASE = new Limit(SingleThreePhase.SINGLE_PHASE, 6000, 32000);

	@Test
	public void testNull() {
		var esh = buildManualEnergyScheduleHandler(new DummyController("ctrl0"), () -> null);
		assertEquals("", esh.getParentFactoryPid());
		assertEquals("ctrl0", esh.getParentId());

		var t = EnergyScheduleTester.from(esh);
		assertEquals(4000 /* no discharge limitation */,
				(int) t.simulatePeriod().ef().getExtremeCoefficientValue(Coefficient.ESS, MAXIMIZE));
	}

	@Test
	public void testMinimum() throws OpenemsNamedException {
		var esh = buildManualEnergyScheduleHandler(new DummyController("ctrl0"), //
				() -> new EnergyScheduler.Config.ManualOptimizationContext(Mode.Actual.MINIMUM, true, THREE_PHASE, //
						1000, 5_000));

		var t = EnergyScheduleTester.from(esh);
		assertEquals(1035, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(1035, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(1035, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(895, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(0, t.simulatePeriod().ef().getManagedConsumption());

		var r = GetScheduleResponse.from(UuidUtils.getNilUuid(), null, esh);
		var schedule = getAsJsonArray(r.getResult(), "schedule");
		assertEquals(0, schedule.size());
	}

	@Test
	public void testForce() {
		var esh = buildManualEnergyScheduleHandler(new DummyController("ctrl0"), //
				() -> new EnergyScheduler.Config.ManualOptimizationContext(Mode.Actual.FORCE, true, THREE_PHASE, //
						1000, 20_000));

		var t = EnergyScheduleTester.from(esh);
		assertEquals(5520, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(5520, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(5520, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(2440, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(0, t.simulatePeriod().ef().getManagedConsumption());
	}

	@Test
	public void testZero() {
		var esh = buildManualEnergyScheduleHandler(new DummyController("ctrl0"), //
				() -> new EnergyScheduler.Config.ManualOptimizationContext(Mode.Actual.ZERO, true, THREE_PHASE, //
						1000, 20_000));

		var t = EnergyScheduleTester.from(esh);
		assertEquals(0, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(0, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(0, t.simulatePeriod().ef().getManagedConsumption());
	}

	@Test
	public void testSurplus() {
		var esh = buildManualEnergyScheduleHandler(new DummyController("ctrl0"), //
				() -> new EnergyScheduler.Config.ManualOptimizationContext(Mode.Actual.SURPLUS, true, SINGLE_PHASE, //
						1000, 7_000));

		var t = EnergyScheduleTester.from(esh);
		for (var i = 0; i < 29; i++) {
			assertEquals(0, t.simulatePeriod().ef().getManagedConsumption());
		}
		assertEquals(363, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(446, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(568, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(885, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(1114, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(1309, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(1315, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(0, t.simulatePeriod().ef().getManagedConsumption());
	}

	@Test
	public void testSmartZero() {
		var sr = testSmart(Mode.Actual.ZERO);
		assertEquals(10, sr.fitness.getHardConstraintViolations());
	}

	@Test
	public void testSmartSurplus() {
		var sr = testSmart(Mode.Actual.SURPLUS);
		assertEquals(0, (int) sr.fitness.getGridBuyCost());
	}

	@Test
	public void testSmartResponse() throws OpenemsNamedException {
		var esh = createSmartEsh();
		var r = GetScheduleResponse.from(UuidUtils.getNilUuid(), esh, null);
		var schedule = getAsJsonArray(r.getResult(), "schedule");
		assertEquals(0, schedule.size());
	}

	private static record SmartResult(Fitness fitness) {
	}

	private static EshWithDifferentModes<Actual, SmartOptimizationContext, ScheduleContext> createSmartEsh() {
		final var smartConfig = ImmutableList.of(//
				JSCalendar.Task.<Payload>create() //
						.setStart(LocalTime.of(7, 30)) //
						.addRecurrenceRule(b -> b //
								.setFrequency(WEEKLY) //
								.addByDay(TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)) //
						.setPayload(new Payload(10_000)) //
						.build(), //
				JSCalendar.Task.<Payload>create() //
						.setStart(LocalTime.of(7, 30)) //
						.addRecurrenceRule(b -> b //
								.setFrequency(WEEKLY) //
								.addByDay(MONDAY)) //
						.setPayload(new Payload(60_000)) //
						.build());

		return buildSmartEnergyScheduleHandler(new DummyController("ctrl0"), //
				() -> new EnergyScheduler.Config.SmartOptimizationConfig(true, THREE_PHASE, smartConfig));
	}

	private static SmartResult testSmart(Mode.Actual mode) {
		var esh = createSmartEsh();

		var t = EnergyScheduleTester.from(esh);

		var csc = (SmartOptimizationContext) t.perEsh.getFirst().csc();
		assertEquals("2020-01-01T07:30Z", csc.targetTime().toString());
		assertEquals(10000, csc.targetPayload().sessionEnergyMinimum());

		var fitness = new Fitness();
		for (var i = 0; i < 35; i++) {
			t.simulatePeriod(fitness, mode.getValue());
		}
		return new SmartResult(fitness);
	}
}
