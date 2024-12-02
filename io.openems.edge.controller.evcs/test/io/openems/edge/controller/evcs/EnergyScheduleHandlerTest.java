package io.openems.edge.controller.evcs;

import static org.junit.Assert.assertEquals;

import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.junit.Test;

import io.openems.edge.controller.evcs.ControllerEvcsImpl.EshContext;
import io.openems.edge.energy.api.EnergyScheduleHandler;
import io.openems.edge.energy.api.EnergyScheduleHandler.AbstractEnergyScheduleHandler;
import io.openems.edge.energy.api.simulation.Coefficient;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalSimulationsContext;
import io.openems.edge.energy.api.simulation.OneSimulationContext;
import io.openems.edge.energy.api.test.DummyGlobalSimulationsContext;
import io.openems.edge.evcs.api.ChargeMode;

public class EnergyScheduleHandlerTest {

	private static EnergyScheduleHandler buildEsh(ChargeMode chargeMode) {
		return ControllerEvcsImpl.buildEnergyScheduleHandler(//
				() -> new EshContext("evcs0", true, chargeMode, Priority.CAR, 2300, 6000, 2000));
	}

	private static int cons(GlobalSimulationsContext gsc, int period) {
		return gsc.periods().get(period).consumption();
	}

	@Test
	public void testManualExcessCar() {
		var esh = buildEsh(ChargeMode.EXCESS_POWER);
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
		var esh = buildEsh(ChargeMode.FORCE_CHARGE);
		var gsc = DummyGlobalSimulationsContext.fromHandlers(esh);
		((AbstractEnergyScheduleHandler<?>) esh /* this is safe */).initialize(gsc);
		var osc = OneSimulationContext.from(gsc);

		assertEquals(cons(gsc, 0) + 1500, getConsumption(osc, esh, 0));
		assertEquals(cons(gsc, 1) + 500, getConsumption(osc, esh, 1));
		assertEquals(cons(gsc, 2), getConsumption(osc, esh, 2));
		assertEquals(cons(gsc, 3), getConsumption(osc, esh, 3));
		assertEquals(cons(gsc, 4), getConsumption(osc, esh, 4));
	}

	private static int getConsumption(OneSimulationContext osc, EnergyScheduleHandler esh, int periodIndex) {
		var period = osc.global.periods().get(periodIndex);
		var ef = EnergyFlow.Model.from(osc, period);
		((EnergyScheduleHandler.WithOnlyOneState<?>) esh).simulatePeriod(osc, period, ef);
		return ((int) ef.getExtremeCoefficientValue(Coefficient.CONS, GoalType.MINIMIZE));
	}
}
