package io.openems.edge.energy.task.smart;

import org.junit.Test;

import io.openems.edge.energy.api.schedulable.Schedulable;
import io.openems.edge.energy.dummy.controller.DummyEvcsController;
import io.openems.edge.energy.dummy.controller.DummyFixActivePowerController;
import io.openems.edge.energy.dummy.device.DummyEss;
import io.openems.edge.energy.dummy.device.DummyEvcs;
import io.openems.edge.energy.dummy.forecast.DummyForecast;

public class OptimizerTest {

	@Test
	public void testBuildExecutionPlan() {
		var evcs0 = new DummyEvcs("evcs0");
		var ctrlEvcs0 = new DummyEvcsController("ctrlEvcs0", evcs0);
		var ess0 = new DummyEss("ess0");
		var ctrlFixAP0 = new DummyFixActivePowerController("ctrlFixAP0", ess0);
		var schedulables = new Schedulable[] { ctrlEvcs0, ctrlFixAP0 };

		var forecast = DummyForecast.autumn24();

		var gtf = Optimizer.generateGenotypeFactory(schedulables);
		var gt = gtf.newInstance();
		var ep = Optimizer.buildExecutionPlan(schedulables, forecast, gt);
		System.out.println(ep);
	}

}
