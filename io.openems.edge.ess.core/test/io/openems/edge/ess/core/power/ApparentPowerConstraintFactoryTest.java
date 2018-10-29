package io.openems.edge.ess.core.power;

import java.util.List;

import org.junit.Test;

import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Phase;

public class ApparentPowerConstraintFactoryTest {

	private static Data prepareData(ManagedSymmetricEss... esss) {
		Data data = new Data();
		PowerComponent c = new PowerComponent();
		for (ManagedSymmetricEss ess : esss) {
			data.addEss(ess);
			c.addEss(ess);
		}
		data.initializeCycle();
		return data;
	}

	@Test
	public void testOneSymmetric() throws Exception {
		ManagedSymmetricEssDummy ess0 = new ManagedSymmetricEssDummy("ess0");
		Data data = prepareData(ess0);

		ApparentPowerConstraintFactory f = new ApparentPowerConstraintFactory(data);
		List<Constraint> cs = f.getConstraints(ess0, Phase.ALL, 10000);

		for (Constraint c : cs) {
			System.out.println(c);
		}
	}
}
