package io.openems.edge.ess.core.power.data;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Inverter;
import io.openems.edge.ess.power.api.ThreePhaseInverter;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

public class WeightsUtilTest {

	private static List<ManagedSymmetricEss> esss;
	private static List<Inverter> inverters;
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
		ess0 = new DummyManagedSymmetricEss("ess0").withSoc(50);
		ess1 = new DummyManagedSymmetricEss("ess1").withSoc(70);
		ess2 = new DummyManagedSymmetricEss("ess2").withSoc(40);
		ess3 = new DummyManagedSymmetricEss("ess3").withSoc(70);
		esss = Arrays.asList(ess0, ess1, ess2, ess3);

		inverter0 = new ThreePhaseInverter(ess0.id());
		inverter1 = new ThreePhaseInverter(ess1.id());
		inverter2 = new ThreePhaseInverter(ess2.id());
		inverter3 = new ThreePhaseInverter(ess3.id());
		inverters = Arrays.asList(inverter0, inverter1, inverter2, inverter3);
	}

	@Test
	public void testSortByWeight() {
		WeightsUtil.updateWeightsFromSoc(inverters, esss);
		WeightsUtil.sortByWeights(inverters);

		assertEquals("ess1", inverters.get(0).toString());
		assertEquals("ess3", inverters.get(1).toString());
		assertEquals("ess0", inverters.get(2).toString());
		assertEquals("ess2", inverters.get(3).toString());
	}

	@Test
	public void testAdjustSortingByWeights() {
		WeightsUtil.updateWeightsFromSoc(inverters, esss);
		WeightsUtil.sortByWeights(inverters);

		assertEquals("ess1", inverters.get(0).toString());
		assertEquals("ess3", inverters.get(1).toString());
		assertEquals("ess0", inverters.get(2).toString());
		assertEquals("ess2", inverters.get(3).toString());

		// #1 ess3 weight is slightly below ess0 -> no resorting
		inverter3.setWeight(49);
		WeightsUtil.adjustSortingByWeights(inverters);

		assertEquals("ess1", inverters.get(0).toString());
		assertEquals("ess3", inverters.get(1).toString());
		assertEquals("ess0", inverters.get(2).toString());
		assertEquals("ess2", inverters.get(3).toString());

		// #2 ess3 weight is clearly below ess0 -> resort
		inverter3.setWeight(35);
		WeightsUtil.adjustSortingByWeights(inverters);

		assertEquals("ess1", inverters.get(0).toString());
		assertEquals("ess0", inverters.get(1).toString());
		assertEquals("ess3", inverters.get(2).toString());
		assertEquals("ess2", inverters.get(3).toString());

		// #3 ess3 weight is slightly above ess0 -> no resorting
		inverter3.setWeight(51);
		WeightsUtil.adjustSortingByWeights(inverters);

		assertEquals("ess1", inverters.get(0).toString());
		assertEquals("ess0", inverters.get(1).toString());
		assertEquals("ess3", inverters.get(2).toString());
		assertEquals("ess2", inverters.get(3).toString());

		// #4 ess3 weight is clearly above ess0 -> resort
		inverter3.setWeight(69);
		WeightsUtil.adjustSortingByWeights(inverters);

		assertEquals("ess1", inverters.get(0).toString());
		assertEquals("ess3", inverters.get(1).toString());
		assertEquals("ess0", inverters.get(2).toString());
		assertEquals("ess2", inverters.get(3).toString());
	}

}
