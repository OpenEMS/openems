package io.openems.edge.ess.core.power;

import static org.junit.Assert.assertArrayEquals;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.math3.optim.PointValuePair;
import org.junit.Test;

import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.core.power.optimizers.KeepAllNearEqual;
import io.openems.edge.ess.power.api.Coefficients;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyMetaEss;

public class MergeTest {

	@Test
	public void testRegularEss() {
		Set<String> essIds = Set.of("ess1", "ess2");

		List<ManagedSymmetricEss> essList = List.of(//
				new DummyManagedSymmetricEss("ess1"), //
				new DummyManagedSymmetricEss("ess2"), //
				new DummyMetaEss("ess0"));

		Coefficients coefficients = new Coefficients();
		coefficients.initialize(true, essIds);

		var defaultAPower = new double[] { 100.0, 100.0 };
		var activePower = new PointValuePair(defaultAPower, 0);

		var defaultResult = new double[essList.size()];
		var reactivePower = new PointValuePair(defaultResult, 0);

		var result = KeepAllNearEqual.mergeResults(coefficients, essList, activePower, reactivePower);
		var expected = new double[] { 100.0, 0, 100.0, 0 };
		assertArrayEquals(expected, result, 0);
	}

	@Test
	public void testIrregularEss() {
		Set<String> essIds = new TreeSet<>(Set.of("ess1", "ess2", "ess3"));

		List<ManagedSymmetricEss> essList = List.of(//
				new DummyManagedSymmetricEss("ess1"), //
				new DummyManagedSymmetricEss("ess2"), //
				new DummyMetaEss("ess0"));

		Coefficients coefficients = new Coefficients();
		coefficients.initialize(true, essIds);

		var defaultAPower = new double[] { 100.0, 100.0 };
		var activePower = new PointValuePair(defaultAPower, 0);

		var defaultResult = new double[essList.size()];
		var reactivePower = new PointValuePair(defaultResult, 0);

		var result = KeepAllNearEqual.mergeResults(coefficients, essList, activePower, reactivePower);
		var expected = new double[] { 100.0, 0.0, 100.0, 0.0, 0.0, 0.0 };
		assertArrayEquals(expected, result, 0);
	}
}
