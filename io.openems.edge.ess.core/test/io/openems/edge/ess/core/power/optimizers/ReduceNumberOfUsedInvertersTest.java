package io.openems.edge.ess.core.power.optimizers;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.math3.optim.PointValuePair;
import org.junit.Before;
import org.junit.Test;

import io.openems.common.function.ThrowingFunction;
import io.openems.edge.ess.core.power.data.TargetDirectionUtil.TargetDirection;
import io.openems.edge.ess.power.api.Inverter;
import io.openems.edge.ess.power.api.ThreePhaseInverter;

public class ReduceNumberOfUsedInvertersTest {

	private static ReduceNumberOfUsedInverters sut;
	private static List<Inverter> allInverters = new ArrayList<Inverter>();

	private static class ValidateFunction implements ThrowingFunction<List<Inverter>, PointValuePair, Exception> {

		private final List<Inverter> allInverters;
		private final int requiredNumberOfInverters;

		protected ValidateFunction(List<Inverter> allInverters, int requiredNumberOfInverters) {
			this.allInverters = allInverters;
			this.requiredNumberOfInverters = requiredNumberOfInverters;
		}

		@Override
		public PointValuePair apply(List<Inverter> disabledInverters) throws Exception {
			if (allInverters.size() - disabledInverters.size() < this.requiredNumberOfInverters) {
				throw new Exception("Not solved");
			} else {
				return null; // ignored
			}
		}

	}

	@Before
	public void before() {
		sut = new ReduceNumberOfUsedInverters();

		allInverters.add(new ThreePhaseInverter("ess0").setWeight(10));
		allInverters.add(new ThreePhaseInverter("ess1").setWeight(20));
		allInverters.add(new ThreePhaseInverter("ess2").setWeight(30));
		allInverters.add(new ThreePhaseInverter("ess3").setWeight(40));
		allInverters.add(new ThreePhaseInverter("ess4").setWeight(50));
		allInverters.add(new ThreePhaseInverter("ess5").setWeight(60));
		allInverters.add(new ThreePhaseInverter("ess6").setWeight(70));
		allInverters.add(new ThreePhaseInverter("ess7").setWeight(80));
	}

	@Test
	public void testNumberOfUsedInverters() {
		int requiredNumberOfInverters = 3;

		ValidateFunction validateFunction = new ValidateFunction(allInverters, requiredNumberOfInverters);
		List<Inverter> inverters = sut.apply(allInverters, TargetDirection.DISCHARGE, validateFunction);

		if (requiredNumberOfInverters > allInverters.size() || requiredNumberOfInverters <= 0) {
			// no solution possible; keep all Inverters
			assertEquals(allInverters.size(), inverters.size());
		} else {
			assertEquals(requiredNumberOfInverters, inverters.size());
		}
	}

	@Test
	public void testActualInvertersOnDischarge() {
		ValidateFunction validateFunction = new ValidateFunction(allInverters, 2);
		List<Inverter> inverters = sut.apply(allInverters, TargetDirection.DISCHARGE, validateFunction);

		Iterator<Inverter> iter = inverters.iterator();
		Inverter inv;

		inv = iter.next();
		assertEquals("ess6", inv.getEssId());

		inv = iter.next();
		assertEquals("ess7", inv.getEssId());
	}

	@Test
	public void testActualInvertersOnCharge() {
		ValidateFunction validateFunction = new ValidateFunction(allInverters, 3);
		List<Inverter> inverters = sut.apply(allInverters, TargetDirection.CHARGE, validateFunction);

		Iterator<Inverter> iter = inverters.iterator();
		Inverter inv;

		inv = iter.next();
		assertEquals("ess2", inv.getEssId());

		inv = iter.next();
		assertEquals("ess1", inv.getEssId());

		inv = iter.next();
		assertEquals("ess0", inv.getEssId());
	}

}
