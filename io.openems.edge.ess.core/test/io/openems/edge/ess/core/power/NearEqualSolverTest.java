package io.openems.edge.ess.core.power;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import io.openems.edge.ess.core.power.solver.nearequal.SolveNearEqual;

public class NearEqualSolverTest {

	@Test
	public void solverTest() {
		double[] essUpperLimit = { 10000, 10000, 10000, 1900 };
		double[] essLowerLimit = { 0, 0, 0, 1800 };
		double setValue = 50000;
		var model = new SolveNearEqual();
		model.setUpperBound(essUpperLimit);
		model.setLowerBound(essLowerLimit);
		model.setpowerSetValue(setValue);

		var result = model.solve(4).getPoint();
		assertEquals(Arrays.stream(essUpperLimit).sum(), Arrays.stream(result).sum(), 1e-6);
	}

}
