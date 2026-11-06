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

	@Test
	public void testNoInverterForMetaEss() {
		data.setSymmetricMode(true);
		assertEquals(2, data.getInverters().size());
	}

	@Test
	public void testMemberCoefficientsRegisteredWhenOnlyClusterInEsss() {
		EssPower powerComponent = new EssPowerImpl();
		var ess1 = new DummyManagedSymmetricEss("ess1").setPower(powerComponent);
		var ess2 = new DummyManagedSymmetricEss("ess2").setPower(powerComponent);
		var cluster = new DummyMetaEss("essCluster0", ess1, ess2).setPower(powerComponent);

		var clusterOnly = Lists.<ManagedSymmetricEss>newArrayList(cluster);
		var clusterData = new Data(() -> clusterOnly);
		clusterData.setSymmetricMode(true);

		// Expected: coefficients for essCluster0, ess1, ess2 = 3 IDs × 2 pwr = 6
		assertEquals(3 * 2, clusterData.getCoefficients().getNoOfCoefficients());
	}

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

		assertEquals(3, mixedData.getInverters().size());
		assertEquals(4 * 2, mixedData.getCoefficients().getNoOfCoefficients());
	}

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

		var liveEsss = Lists.<ManagedSymmetricEss>newArrayList(ess1, ess2);
		var d = new Data(() -> liveEsss);
		d.setSymmetricMode(true);

		liveEsss.add(0, cluster);

		d.getConstraintsForAllInverters();
	}
}
