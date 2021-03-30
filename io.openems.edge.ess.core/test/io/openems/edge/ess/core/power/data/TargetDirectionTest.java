package io.openems.edge.ess.core.power.data;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.core.power.Data;
import io.openems.edge.ess.core.power.solver.LinearConstraintsSolver;
import io.openems.edge.ess.power.api.Inverter;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

public class TargetDirectionTest {

	private static DummyManagedSymmetricEss ess0;
	private static MyData data;

	public static final LinearConstraintsSolver linearConstraintsSolver = new LinearConstraintsSolver();

	@Before
	public void before() {
		ess0 = new DummyManagedSymmetricEss("ess0") //
				.withAllowedChargePower(-9000) //
				.withAllowedDischargePower(9000) //
				.withMaxApparentPower(5000);
		data = new MyData();
		data.addEss(ess0);
		data.initializeCycle();
	}

	@Test
	public void testGetTargetDirection() throws Exception {
		// #1
		data.addSimpleConstraint("", ess0.id(), Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 0);
		assertEquals(TargetDirection.KEEP_ZERO, //
				TargetDirection.from(data.getInverters(), data.getCoefficients(),
						data.getConstraintsForAllInverters()));
		data.initializeCycle();

		// #2
		data.addSimpleConstraint("", ess0.id(), Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -1);
		assertEquals(TargetDirection.CHARGE, //
				TargetDirection.from(data.getInverters(), data.getCoefficients(),
						data.getConstraintsForAllInverters()));
		data.initializeCycle();

		// #3
		data.addSimpleConstraint("", ess0.id(), Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 1);
		assertEquals(TargetDirection.DISCHARGE, //
				TargetDirection.from(data.getInverters(), data.getCoefficients(),
						data.getConstraintsForAllInverters()));
	}

	private static class MyData extends Data {

		@Override
		protected synchronized void addEss(ManagedSymmetricEss ess) {
			super.addEss(ess);
		}

		@Override
		protected List<Inverter> getInverters() {
			return super.getInverters();
		}

		@Override
		protected synchronized void initializeCycle() {
			super.initializeCycle();
		}
	}
}
