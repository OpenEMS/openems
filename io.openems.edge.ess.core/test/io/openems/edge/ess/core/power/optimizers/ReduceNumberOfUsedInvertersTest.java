package io.openems.edge.ess.core.power.optimizers;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.optim.PointValuePair;
import org.junit.Before;
import org.junit.Test;

import io.openems.common.function.ThrowingFunction;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.core.power.Solver;
import io.openems.edge.ess.core.power.data.TargetDirection;
import io.openems.edge.ess.core.power.data.WeightsUtil;
import io.openems.edge.ess.power.api.Inverter;
import io.openems.edge.ess.power.api.ThreePhaseInverter;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

public class ReduceNumberOfUsedInvertersTest {

	private static ReduceNumberOfUsedInverters sut;

	private static List<ManagedSymmetricEss> esss;
	private static List<Inverter> allInverters;
	private static DummyManagedSymmetricEss ess0;
	private static DummyManagedSymmetricEss ess1;
	private static DummyManagedSymmetricEss ess2;
	private static DummyManagedSymmetricEss ess3;
	private static ThreePhaseInverter inverter0;
	private static ThreePhaseInverter inverter1;
	private static ThreePhaseInverter inverter2;
	private static ThreePhaseInverter inverter3;

	@Before
	public void before() {
		ess0 = new DummyManagedSymmetricEss("ess0").withSoc(10);
		ess1 = new DummyManagedSymmetricEss("ess1").withSoc(20);
		ess2 = new DummyManagedSymmetricEss("ess2").withSoc(30);
		ess3 = new DummyManagedSymmetricEss("ess3").withSoc(40);
		esss = Arrays.asList(ess0, ess1, ess2, ess3);

		inverter0 = new ThreePhaseInverter(ess0.id());
		inverter1 = new ThreePhaseInverter(ess1.id());
		inverter2 = new ThreePhaseInverter(ess2.id());
		inverter3 = new ThreePhaseInverter(ess3.id());
		allInverters = Arrays.asList(inverter0, inverter1, inverter2, inverter3);

		WeightsUtil.updateWeightsFromSoc(allInverters, esss);
		WeightsUtil.sortByWeights(allInverters);

		sut = new ReduceNumberOfUsedInverters();
	}

	@Test
	public void testNumberOfUsedInverters() {
		var requiredNumberOfInverters = 3;

		var validateFunction = new ValidateFunction(allInverters, requiredNumberOfInverters);
		var inverters = sut.apply(allInverters, TargetDirection.DISCHARGE, validateFunction);

		if (requiredNumberOfInverters > allInverters.size() || requiredNumberOfInverters <= 0) {
			// no solution possible; keep all Inverters
			assertEquals(allInverters.size(), inverters.size());
		} else {
			assertEquals(requiredNumberOfInverters, inverters.size());
		}
	}

	@Test
	public void testActualInvertersOnDischarge() {
		var validateFunction = new ValidateFunction(allInverters, 2);
		var inverters = sut.apply(allInverters, TargetDirection.DISCHARGE, validateFunction);

		var iter = inverters.iterator();
		Inverter inv;

		inv = iter.next();
		assertEquals("ess3", inv.getEssId());

		inv = iter.next();
		assertEquals("ess2", inv.getEssId());
	}

	@Test
	public void testActualInvertersOnCharge() {
		var validateFunction = new ValidateFunction(allInverters, 3);
		var inverters = sut.apply(allInverters, TargetDirection.CHARGE, validateFunction);

		var iter = inverters.iterator();
		Inverter inv;

		inv = iter.next();
		assertEquals("ess0", inv.getEssId());

		inv = iter.next();
		assertEquals("ess1", inv.getEssId());

		inv = iter.next();
		assertEquals("ess2", inv.getEssId());
	}

	/**
	 * Dummy ValidateFunction. In reality this is done by
	 * 'solveWithDisabledInverters' in {@link Solver}.
	 */
	private static class ValidateFunction implements ThrowingFunction<List<Inverter>, PointValuePair, Exception> {

		private final List<Inverter> allInverters;
		private final int requiredNumberOfInverters;

		protected ValidateFunction(List<Inverter> allInverters, int requiredNumberOfInverters) {
			this.allInverters = allInverters;
			this.requiredNumberOfInverters = requiredNumberOfInverters;
		}

		@Override
		public PointValuePair apply(List<Inverter> disabledInverters) throws Exception {
			if (this.allInverters.size() - disabledInverters.size() < this.requiredNumberOfInverters) {
				throw new Exception("Not solved");
			}
			return null; // ignored
		}

	}

}
