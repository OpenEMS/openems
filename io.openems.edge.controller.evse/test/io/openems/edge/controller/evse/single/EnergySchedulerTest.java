package io.openems.edge.controller.evse.single;

import static io.openems.edge.controller.evse.single.EnergyScheduler.buildManualEnergyScheduleHandler;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.junit.Test;

import io.openems.edge.energy.api.simulation.Coefficient;
import io.openems.edge.energy.api.test.EnergyScheduleTester;

public class EnergySchedulerTest {

	@Test
	public void testNull() {
		var esh = buildManualEnergyScheduleHandler(() -> null, () -> null);
		assertTrue(esh.getId().startsWith("ESH.WithOnlyOneMode."));

		var t = EnergyScheduleTester.from(esh);
		var t0 = t.simulatePeriod(0);
		assertEquals(4000 /* no discharge limitation */,
				(int) t0.ef().getExtremeCoefficientValue(Coefficient.ESS, GoalType.MAXIMIZE));
	}
}

//private static int cons(GlobalOptimizationContext goc, int period) {
//	return goc.periods().get(period).consumption();
//}

//@Test
//public void testManualExcessCar() {
//	var esh = buildEshManual(generateEshManualContext(ChargeMode.EXCESS_POWER));
//	var goc = DummyGlobalOptimizationContext.fromHandlers(esh);
//	((AbstractEnergyScheduleHandler<?>) esh /* this is safe */).initialize(gsc);
//	var gsc = GlobalScheduleContext.from(gsc);
//
//	assertEquals(cons(gsc, 0) + 575, getConsumption(osc, esh, 0));
//	assertEquals(cons(gsc, 1) + 575, getConsumption(osc, esh, 1));
//	assertEquals(cons(gsc, 2) + 575, getConsumption(osc, esh, 2));
//	assertEquals(cons(gsc, 3) + 275, getConsumption(osc, esh, 3));
//	assertEquals(cons(gsc, 4), getConsumption(osc, esh, 4));
//}

//@Test
//public void testManualForce() {
//	var esh = buildEshManual(generateEshManualContext(Mode.Actual.FORCE));
//	var goc = DummyGlobalOptimizationContext.fromHandlers(esh);
//	((AbstractEnergyScheduleHandler<?>) esh /* this is safe */).initialize(goc);
//	var gsc = GlobalScheduleContext.from(goc);
//
//	assertEquals(cons(goc, 0) + 1500, getConsumption(gsc, esh, 0));
//	assertEquals(cons(goc, 1) + 500, getConsumption(gsc, esh, 1));
//	assertEquals(cons(goc, 2), getConsumption(gsc, esh, 2));
//	assertEquals(cons(goc, 3), getConsumption(gsc, esh, 3));
//	assertEquals(cons(goc, 4), getConsumption(gsc, esh, 4));
//}

//@Test
//public void testInitialPopulation() {
//	// NOTE: not yet implemented
//	var esh = buildEshSmart(generateEshSmartContext());
//	var goc = DummyGlobalOptimizationContext.fromHandlers(esh);
//	esh.initialize(goc);
//	var ips = esh.getInitialPopulations(goc);
//	assertEquals(0, ips.size());
//	// var ip = ips.get(0);
//	// assertEquals(2, ip.periods().size());
//	// assertEquals("2020-01-01T11:00Z", ip.periods().get(0).toString());
//	// assertEquals("2020-01-01T12:00Z", ip.periods().get(1).toString());
//	//
//	// var ps = gsc.periods().stream().filter(p ->
//	// ip.periods().contains(p.time())).toList();
//	// assertEquals(2, ps.size());
//	// assertEquals(282.9, ps.get(0).price(), 0.001);
//	// assertEquals(260.7, ps.get(1).price(), 0.001);
//}

//private static int getConsumption(GlobalScheduleContext gsc, EnergyScheduleHandler esh, int periodIndex) {
//	var period = gsc.goc.periods().get(periodIndex);
//	var ef = EnergyFlow.Model.from(gsc, period);
//	((EnergyScheduleHandler.WithOnlyOneState<?, ?>) esh).simulatePeriod(gsc, period, ef);
//	return ((int) ef.getExtremeCoefficientValue(Coefficient.CONS, GoalType.MINIMIZE));
//}