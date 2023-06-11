package io.openems.edge.ess.core.power;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyMetaEss;

public class DataTest {

	private static Data data;
	private static List<ManagedSymmetricEss> esss;

	@Before
	public void before() {
		EssPower powerComponent = new EssPowerImpl();
		var ess1 = new DummyManagedSymmetricEss("ess1", powerComponent) //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(12000) //
				.withSoc(30);
		var ess2 = new DummyManagedSymmetricEss("ess2", powerComponent) //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(12000) //
				.withSoc(60);
		var ess0 = new DummyMetaEss("ess0", powerComponent, ess1, ess2); //
		esss = Lists.newArrayList(ess0, ess1, ess2);

		data = new Data();
		for (ManagedSymmetricEss ess : esss) {
			data.addEss(ess);
		}
		data.initializeCycle();
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
}
