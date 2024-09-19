package io.openems.edge.controller.ess.gridoptimizedcharge;

import static org.junit.Assert.assertEquals;

import java.time.LocalTime;

import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.junit.Test;

import io.openems.edge.energy.api.EnergyScheduleHandler;
import io.openems.edge.energy.api.EnergyScheduleHandler.AbstractEnergyScheduleHandler;
import io.openems.edge.energy.api.simulation.Coefficient;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalSimulationsContext;
import io.openems.edge.energy.api.simulation.OneSimulationContext;
import io.openems.edge.energy.api.test.DummyGlobalSimulationsContext;

public class EnergyScheduleHandlerTest {

	@Test
	public void testManual() {
		var esh = ControllerEssGridOptimizedChargeImpl.buildEnergyScheduleHandler(//
				() -> Mode.MANUAL, //
				() -> LocalTime.of(10, 00));
		var gsc = DummyGlobalSimulationsContext.fromHandlers(esh);
		((AbstractEnergyScheduleHandler<?>) esh /* this is safe */).initialize(gsc);

		assertEquals(3894, getEssMaxCharge(gsc, esh, 0));
		assertEquals(1214, getEssMaxCharge(gsc, esh, 26));
		assertEquals(4000, getEssMaxCharge(gsc, esh, 40));
	}

	private static int getEssMaxCharge(GlobalSimulationsContext gsc, EnergyScheduleHandler esh, int periodIndex) {
		var osc = OneSimulationContext.from(gsc);
		var period = gsc.periods().get(periodIndex);
		var ef = EnergyFlow.Model.from(osc, period);
		((EnergyScheduleHandler.WithOnlyOneState<?>) esh).simulatePeriod(OneSimulationContext.from(gsc), period, ef);
		return ((int) ef.getExtremeCoefficientValue(Coefficient.ESS, GoalType.MINIMIZE)) * -1;
	}
}
