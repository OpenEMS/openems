package io.openems.edge.energy.task.smart;

import static io.openems.edge.energy.api.simulatable.ExecutionPlan.NO_OF_PERIODS;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import io.jenetics.Genotype;
import io.jenetics.IntegerChromosome;
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

		var gtf = Genotype.of(//
				Stream.of(schedulables) //
						.map(s -> IntegerChromosome.of(0, s.getScheduleHandler().presets.length - 1, NO_OF_PERIODS)) //
						.collect(Collectors.toUnmodifiableList()));

		Optimizer.buildExecutionPlan(schedulables, forecast, gtf);
	}

}
