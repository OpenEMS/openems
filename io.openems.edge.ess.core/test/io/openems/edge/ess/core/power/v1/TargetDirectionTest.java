package io.openems.edge.ess.core.power.v1;

import static io.openems.edge.common.type.Phase.SingleOrAllPhase.ALL;
import static io.openems.edge.ess.power.api.Pwr.ACTIVE;
import static io.openems.edge.ess.power.api.Relationship.EQUALS;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import io.openems.edge.ess.core.power.v1.data.TargetDirection;
import io.openems.edge.ess.core.power.v1.solver.LinearConstraintsSolver;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

public class TargetDirectionTest {

	private static DummyManagedSymmetricEss ess0;
	private static Data data;

	public static final LinearConstraintsSolver linearConstraintsSolver = new LinearConstraintsSolver();

	@Before
	public void before() {
		ess0 = new DummyManagedSymmetricEss("ess0") //
				.withAllowedChargePower(-9000) //
				.withAllowedDischargePower(9000) //
				.withMaxApparentPower(5000);
		data = new Data(() -> List.of(ess0));
	}

	@Test
	public void testGetTargetDirection() throws Exception {
		// #1
		data.addSimpleConstraint("", ess0.id(), ALL, ACTIVE, EQUALS, 0);
		assertEquals(TargetDirection.KEEP_ZERO, //
				TargetDirection.from(data.getInverters(), data.getCoefficients(),
						data.getConstraintsForAllInverters()));
		data.initializeCycle();

		// #2
		data.addSimpleConstraint("", ess0.id(), ALL, ACTIVE, EQUALS, -1);
		assertEquals(TargetDirection.CHARGE, //
				TargetDirection.from(data.getInverters(), data.getCoefficients(),
						data.getConstraintsForAllInverters()));
		data.initializeCycle();

		// #3
		data.addSimpleConstraint("", ess0.id(), ALL, ACTIVE, EQUALS, 1);
		assertEquals(TargetDirection.DISCHARGE, //
				TargetDirection.from(data.getInverters(), data.getCoefficients(),
						data.getConstraintsForAllInverters()));
	}
}
