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
	 * Verifies that a MetaEss (e.g. EssCluster) does not get its own Inverter entry.
	 * Only physical ESS members should have Inverters so that the solver never tries
	 * to call applyPower() on the wrapper.
	 */
	@Test
	public void testNoInverterForMetaEss() {
		data.setSymmetricMode(true);
		// esss = [ess0(MetaEss), ess1, ess2] → only ess1 and ess2 should have inverters
		assertEquals(2, data.getInverters().size());
	}

	/**
	 * Verifies that member ESS IDs of a MetaEss are registered in the Coefficients
	 * even when only the cluster itself is listed in esss. This is the core of
	 * issue #3752: createMetaEssConstraints() requires member IDs in the coefficient
	 * set to build the cluster = ess0 + ess1 constraint.
	 */
	@Test
	public void testMemberCoefficientsRegisteredWhenOnlyClusterInEsss() {
		EssPower powerComponent = new EssPowerImpl();
		var ess1 = new DummyManagedSymmetricEss("ess1").setPower(powerComponent);
		var ess2 = new DummyManagedSymmetricEss("ess2").setPower(powerComponent);
		var cluster = new DummyMetaEss("essCluster0", ess1, ess2).setPower(powerComponent);

		// Only the cluster in esss — mirrors the real OSGi scenario where members may
		// not be collected before the cluster processes its cycle
		var clusterOnly = Lists.<ManagedSymmetricEss>newArrayList(cluster);
		var clusterData = new Data(() -> clusterOnly);
		clusterData.setSymmetricMode(true);

		// Expected: coefficients for essCluster0, ess1, ess2 = 3 IDs × 2 pwr = 6
		assertEquals(3 * 2, clusterData.getCoefficients().getNoOfCoefficients());
	}
}
