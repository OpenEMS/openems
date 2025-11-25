package io.openems.edge.controller.evse.single;

import static io.openems.common.jscalendar.JSCalendar.RecurrenceFrequency.WEEKLY;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static io.openems.edge.controller.evse.single.EnergyScheduler.buildManualEnergyScheduleHandler;
import static io.openems.edge.controller.evse.single.EnergyScheduler.buildSmartEnergyScheduleHandler;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalTime;

import org.junit.Ignore;
import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jscalendar.JSCalendar;
import io.openems.edge.common.type.Phase.SingleOrThreePhase;
import io.openems.edge.controller.evse.single.EnergyScheduler.Config.SmartOptimizationConfig;
import io.openems.edge.controller.evse.single.EnergyScheduler.Payload;
import io.openems.edge.controller.evse.single.EnergyScheduler.ScheduleContext;
import io.openems.edge.controller.evse.single.EnergyScheduler.SmartOptimizationContext;
import io.openems.edge.controller.evse.single.jsonrpc.GetSchedule;
import io.openems.edge.controller.test.DummyController;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler.Fitness;
import io.openems.edge.energy.api.handler.EshWithDifferentModes;
import io.openems.edge.energy.api.test.EnergyScheduleTester;
import io.openems.edge.evse.api.chargepoint.Mode;
import io.openems.edge.evse.api.chargepoint.Mode.Actual;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.common.ApplySetPoint;
import io.openems.edge.evse.api.electricvehicle.Profile.ElectricVehicleAbilities;

public class EnergySchedulerTest {

	private static CombinedAbilities createAbilities(SingleOrThreePhase phase, boolean isReadyForCharging) {
		var chargePointAbilities = ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(phase, 6000, 16000)) //
				.setIsReadyForCharging(isReadyForCharging) //
				.build();
		var electricVehicleAbilities = ElectricVehicleAbilities.create() //
				.setSinglePhaseLimitInMilliAmpere(6000, 32000) //
				.setThreePhaseLimitInMilliAmpere(6000, 16000) //
				.build();
		return CombinedAbilities.createFrom(chargePointAbilities, electricVehicleAbilities) //
				.build();
	}

	@Test
	public void testNull() {
		var esh = buildManualEnergyScheduleHandler(new DummyController("ctrl0"), () -> null);
		assertEquals("", esh.getParentFactoryPid());
		assertEquals("ctrl0", esh.getParentId());

		var t = EnergyScheduleTester.from(esh);
		assertEquals(4000 /* no discharge limitation */, t.simulatePeriod().ef().setEss(4000));
	}

	@Test
	public void testMinimum() throws OpenemsNamedException {
		var esh = buildManualEnergyScheduleHandler(new DummyController("ctrl0"), //
				() -> new EnergyScheduler.Config.ManualOptimizationContext(Mode.Actual.MINIMUM,
						createAbilities(THREE_PHASE, true), //
						false /* appearsToBeFullyCharged */, //
						1000, /* sessionEnergy */ //
						5000 /* sessionEnergyLimit */));

		var t = EnergyScheduleTester.from(esh);
		assertEquals(1035, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(1035, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(1035, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(895, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(0, t.simulatePeriod().ef().getManagedConsumption());

		var r = GetSchedule.Response.create(null, esh);
		assertTrue(r.schedule().isEmpty());
	}

	@Test
	public void testForce() {
		var esh = buildManualEnergyScheduleHandler(new DummyController("ctrl0"), //
				() -> new EnergyScheduler.Config.ManualOptimizationContext(Mode.Actual.FORCE,
						createAbilities(THREE_PHASE, true), //
						false /* appearsToBeFullyCharged */, //
						1000, /* sessionEnergy */ //
						10000 /* sessionEnergyLimit */));

		var t = EnergyScheduleTester.from(esh);
		assertEquals(2760, t.simulatePeriod().ef().getManagedConsumption()); // 16 A = 11040 W
		assertEquals(2760, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(2760, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(720, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(0, t.simulatePeriod().ef().getManagedConsumption());
	}

	@Test
	public void testZero() {
		var esh = buildManualEnergyScheduleHandler(new DummyController("ctrl0"), //
				() -> new EnergyScheduler.Config.ManualOptimizationContext(Mode.Actual.ZERO,
						createAbilities(THREE_PHASE, true), //
						false /* appearsToBeFullyCharged */, //
						1000, /* sessionEnergy */ //
						20000 /* sessionEnergyLimit */));

		var t = EnergyScheduleTester.from(esh);
		assertEquals(0, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(0, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(0, t.simulatePeriod().ef().getManagedConsumption());
	}

	@Test
	public void testSurplus() {
		var esh = buildManualEnergyScheduleHandler(new DummyController("ctrl0"), //
				() -> new EnergyScheduler.Config.ManualOptimizationContext(Mode.Actual.SURPLUS,
						createAbilities(THREE_PHASE, true), //
						false /* appearsToBeFullyCharged */, //
						1000, /* sessionEnergy */ //
						4500 /* sessionEnergyLimit */));

		var t = EnergyScheduleTester.from(esh);
		for (var i = 0; i < 33; i++) {
			assertEquals(0, t.simulatePeriod().ef().getManagedConsumption());
		}
		assertEquals(1114, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(1309, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(1077, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(0, t.simulatePeriod().ef().getManagedConsumption());
	}

	@Test
	public void testSmartZero() {
		var sr = testSmart(Mode.Actual.ZERO);
		assertEquals(0, sr.fitness.getHardConstraintViolations());
	}

	@Test
	public void testSmartSurplus() {
		var sr = testSmart(Mode.Actual.SURPLUS);
		assertEquals(0, (int) sr.fitness.getGridBuyCost());
	}

	@Test
	public void testSmartResponse() throws OpenemsNamedException {
		var esh = createSmartEsh();
		var r = GetSchedule.Response.create(esh, null);
		assertTrue(r.schedule().isEmpty());
	}

	@Test
	public void testSerializerManual() throws OpenemsNamedException {
		final var serializer = EnergyScheduler.Config.ManualOptimizationContext.serializer();
		var obj = new EnergyScheduler.Config.ManualOptimizationContext(Mode.Actual.MINIMUM,
				createAbilities(THREE_PHASE, true), //
				false /* appearsToBeFullyCharged */, //
				1000, /* sessionEnergy */ //
				5000 /* sessionEnergyLimit */);
		var json = serializer.serialize(obj);
		assertEquals(serializer.deserialize(json), obj);
	}

	// TODO
	@Ignore
	@Test
	public void testSerializerSmart() throws OpenemsNamedException {
		final var serializer = EnergyScheduler.Config.SmartOptimizationConfig.serializer();
		var obj = createSmartOptimizationConfig();
		var json = serializer.serialize(obj);
		assertEquals(serializer.deserialize(json), obj);
	}

	private static record SmartResult(Fitness fitness) {
	}

	private static SmartOptimizationConfig createSmartOptimizationConfig() {
		final var smartConfig = JSCalendar.Tasks.<Payload>create() //
				.add(t -> t //
						.setStart(LocalTime.of(7, 30)) //
						.addRecurrenceRule(b -> b //
								.setFrequency(WEEKLY) //
								.addByDay(TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)) //
						.setPayload(new Payload(10_000)) //
						.build()) //
				.add(t -> t //
						.setStart(LocalTime.of(7, 30)) //
						.addRecurrenceRule(b -> b //
								.setFrequency(WEEKLY) //
								.addByDay(MONDAY)) //
						.setPayload(new Payload(60_000)) //
						.build()) //
				.build();
		return new EnergyScheduler.Config.SmartOptimizationConfig(createAbilities(THREE_PHASE, true), //
				false /* appearsToBeFullyCharged */, //
				smartConfig);
	}

	private static EshWithDifferentModes<Actual, SmartOptimizationContext, ScheduleContext> createSmartEsh() {
		return buildSmartEnergyScheduleHandler(new DummyController("ctrl0"), () -> createSmartOptimizationConfig());
	}

	private static SmartResult testSmart(Mode.Actual mode) {
		var esh = createSmartEsh();

		var t = EnergyScheduleTester.from(esh);

		var csc = (SmartOptimizationContext) t.perEsh.getFirst().csc();
		assertNull(csc.targetTime());
		assertNull(csc.targetPayload());
		// assertEquals("2020-01-01T07:30Z", csc.targetTime().toString());
		// assertEquals(10000, csc.targetPayload().sessionEnergyMinimum());

		var fitness = new Fitness();
		for (var i = 0; i < 35; i++) {
			t.simulatePeriod(fitness, mode.getValue());
		}
		return new SmartResult(fitness);
	}
}
