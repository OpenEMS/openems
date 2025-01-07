package io.openems.edge.controller.evcs;

import static io.openems.edge.controller.evcs.Utils.buildEshManual;
import static io.openems.edge.controller.evcs.Utils.buildEshSmart;
import static org.junit.Assert.assertEquals;

import java.util.function.Supplier;

import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.openems.edge.controller.evcs.Utils.EshContext.EshManualContext;
import io.openems.edge.controller.evcs.Utils.EshContext.EshSmartContext;
import io.openems.edge.energy.api.EnergyScheduleHandler;
import io.openems.edge.energy.api.EnergyScheduleHandler.AbstractEnergyScheduleHandler;
import io.openems.edge.energy.api.simulation.Coefficient;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalSimulationsContext;
import io.openems.edge.energy.api.simulation.OneSimulationContext;
import io.openems.edge.energy.api.test.DummyGlobalSimulationsContext;
import io.openems.edge.evcs.api.ChargeMode;

public class UtilsTest {

	private static Supplier<EshSmartContext> generateEshSmartContext() {
		return () -> new EshSmartContext("evcs0", 2000, ImmutableList.of());
	}

	private static Supplier<EshManualContext> generateEshManualContext(ChargeMode chargeMode) {
		return () -> new EshManualContext(true, chargeMode, 6000, 2300, Priority.CAR, "evcs0", 2000);
	}

	private static int cons(GlobalSimulationsContext gsc, int period) {
		return gsc.periods().get(period).consumption();
	}

	@Test
	public void testManualExcessCar() {
		var esh = buildEshManual(generateEshManualContext(ChargeMode.EXCESS_POWER));
		var gsc = DummyGlobalSimulationsContext.fromHandlers(esh);
		((AbstractEnergyScheduleHandler<?>) esh /* this is safe */).initialize(gsc);
		var osc = OneSimulationContext.from(gsc);

		assertEquals(cons(gsc, 0) + 575, getConsumption(osc, esh, 0));
		assertEquals(cons(gsc, 1) + 575, getConsumption(osc, esh, 1));
		assertEquals(cons(gsc, 2) + 575, getConsumption(osc, esh, 2));
		assertEquals(cons(gsc, 3) + 275, getConsumption(osc, esh, 3));
		assertEquals(cons(gsc, 4), getConsumption(osc, esh, 4));
	}

	@Test
	public void testManualForce() {
		var esh = buildEshManual(generateEshManualContext(ChargeMode.FORCE_CHARGE));
		var gsc = DummyGlobalSimulationsContext.fromHandlers(esh);
		((AbstractEnergyScheduleHandler<?>) esh /* this is safe */).initialize(gsc);
		var osc = OneSimulationContext.from(gsc);

		assertEquals(cons(gsc, 0) + 1500, getConsumption(osc, esh, 0));
		assertEquals(cons(gsc, 1) + 500, getConsumption(osc, esh, 1));
		assertEquals(cons(gsc, 2), getConsumption(osc, esh, 2));
		assertEquals(cons(gsc, 3), getConsumption(osc, esh, 3));
		assertEquals(cons(gsc, 4), getConsumption(osc, esh, 4));
	}

	@Test
	public void testInitialPopulation() {
		var esh = buildEshSmart(generateEshSmartContext());
		var gsc = DummyGlobalSimulationsContext.fromHandlers(esh);
		esh.initialize(gsc);
		var ips = esh.getInitialPopulations(gsc);
		assertEquals(1, ips.size());
		var ip = ips.get(0);
		assertEquals(2, ip.periods().size());
		assertEquals("2020-01-01T11:00Z", ip.periods().get(0).toString());
		assertEquals("2020-01-01T12:00Z", ip.periods().get(1).toString());

		var ps = gsc.periods().stream().filter(p -> ip.periods().contains(p.time())).toList();
		assertEquals(2, ps.size());
		assertEquals(282.9, ps.get(0).price(), 0.001);
		assertEquals(260.7, ps.get(1).price(), 0.001);
	}

	private static int getConsumption(OneSimulationContext osc, EnergyScheduleHandler esh, int periodIndex) {
		var period = osc.global.periods().get(periodIndex);
		var ef = EnergyFlow.Model.from(osc, period);
		((EnergyScheduleHandler.WithOnlyOneState<?>) esh).simulatePeriod(osc, period, ef);
		return ((int) ef.getExtremeCoefficientValue(Coefficient.CONS, GoalType.MINIMIZE));
	}
}
