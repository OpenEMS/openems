package io.openems.edge.ess.core.power.v1;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.core.power.EssPower;
import io.openems.edge.ess.core.power.EssPowerImpl;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyMetaEss;

public class DataTest {

	private static Data data;
	private static List<ManagedSymmetricEss> esss;

	@Before
	public void before() {
		EssPower powerComponent = new EssPowerImpl();
		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(12000) //
				.withSoc(30);
		var ess2 = new DummyManagedSymmetricEss("ess2") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(12000) //
				.withSoc(60);
		var ess0 = new DummyMetaEss("ess0", ess1, ess2) //
				.setPower(powerComponent);
		esss = Lists.newArrayList(ess0, ess1, ess2);

		data = new Data(() -> esss);
	}

	@Test
	public void testNoOfCoefficientsSymmetric() {
		data.setSymmetricMode(true);
		assertEquals(esss.size() /* symmetric */ * 2 /* pwr */, data.getCoefficients().getNoOfCoefficients());
	}

	@Test
	public void testNoOfCoefficientsAsymmetric() {
		data.setSymmetricMode(false);
		assertEquals(esss.size() * 4 /* phases + all */ * 2 /* pwr */, data.getCoefficients().getNoOfCoefficients());
	}

	/**
	 * Verifies that a MetaEss (e.g. EssCluster) does not get its own Inverter
	 * entry. Only physical ESS members should have Inverters so that the solver
	 * never tries to call applyPower() on the wrapper.
	 */
	@Test
	public void testNoInverterForMetaEss() {
		data.setSymmetricMode(true);
		// esss = [ess0(MetaEss), ess1, ess2] → only ess1 and ess2 should have inverters
		assertEquals(2, data.getInverters().size());
	}

	/**
	 * Columns exist only for the live cluster plus live children. Config children
	 * that are not bound do not get columns.
	 */
	@Test
	public void testMemberCoefficientsRegisteredWhenOnlyClusterInEsss() {
		EssPower powerComponent = new EssPowerImpl();
		var ess1 = new DummyManagedSymmetricEss("ess1").setPower(powerComponent);
		var ess2 = new DummyManagedSymmetricEss("ess2").setPower(powerComponent);
		var cluster = new DummyMetaEss("essCluster0", ess1, ess2).setPower(powerComponent);

		var clusterOnly = Lists.<ManagedSymmetricEss>newArrayList(cluster);
		var clusterData = new Data(() -> clusterOnly);
		clusterData.setSymmetricMode(true);

		assertEquals(1 * 2, clusterData.getCoefficients().getNoOfCoefficients());
	}

	/**
	 * A live ESS that is not a cluster child gets no inverter when a MetaEss is
	 * present.
	 */
	@Test
	public void testNoInverterForStandaloneWhenClusterPresent() {
		EssPower powerComponent = new EssPowerImpl();
		var ess1 = new DummyManagedSymmetricEss("ess1").setPower(powerComponent);
		var ess2 = new DummyManagedSymmetricEss("ess2").setPower(powerComponent);
		var ess3 = new DummyManagedSymmetricEss("ess3").setPower(powerComponent);
		var ess0 = new DummyMetaEss("ess0", ess1, ess2).setPower(powerComponent);
		var mixed = Lists.<ManagedSymmetricEss>newArrayList(ess0, ess1, ess2, ess3);
		var mixedData = new Data(() -> mixed);
		mixedData.setSymmetricMode(true);

		assertEquals(2, mixedData.getInverters().size());
		assertEquals(3 * 2, mixedData.getCoefficients().getNoOfCoefficients());
	}

	/**
	 * Verifies that getConstraintsForAllInverters() does not throw when the live
	 * esss list is updated (essCluster0 added) but updateInverters() has not yet
	 * been called — the "timing gap" that occurs in OSGi when addEss() stores the
	 * cluster in the list and then calls onUpdateEsss() non-atomically.
	 *
	 * <p>
	 * Without fix: createMetaEssConstraints() sees essCluster0 in the live esss but
	 * its coefficient was never registered --> "Coefficient for
	 * [essCluster0,ALL,ACTIVE] was not found".
	 *
	 * 
	 */
	@Test
	public void testTimingGapClusterAddedBeforeUpdateInverters() throws Exception {
		EssPower powerComponent = new EssPowerImpl();
		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-20000) //
				.withAllowedDischargePower(20000) //
				.withMaxApparentPower(20000);
		var ess2 = new DummyManagedSymmetricEss("ess2") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-20000) //
				.withAllowedDischargePower(20000) //
				.withMaxApparentPower(20000);
		var cluster = new DummyMetaEss("essCluster0", ess1, ess2).setPower(powerComponent);

		// Start with only the physical ESS — no cluster yet
		var liveEsss = Lists.<ManagedSymmetricEss>newArrayList(ess1, ess2);
		var d = new Data(() -> liveEsss);
		d.setSymmetricMode(true);

		liveEsss.add(0, cluster);

		d.getConstraintsForAllInverters();
	}
}
