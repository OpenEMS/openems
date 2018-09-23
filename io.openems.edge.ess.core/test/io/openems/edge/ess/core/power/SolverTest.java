package io.openems.edge.ess.core.power;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.Relationship;
import org.junit.Test;

import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;

public class SolverTest {

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

	private static ManagedSymmetricEssDummy[] prepareSymmetricEss() {
		return new ManagedSymmetricEssDummy[] { //
				new ManagedSymmetricEssDummy("ess1") //
						.allowedCharge(-9000).allowedDischarge(9000).maxApparentPower(5000),
				new ManagedSymmetricEssDummy("ess2") //
						.allowedCharge(-9000).allowedDischarge(9000).maxApparentPower(5000) };
	}

	private static ManagedSymmetricEss[] prepareEssCluster() {
		ManagedSymmetricEssDummy ess1 = new ManagedSymmetricEssDummy("ess1") //
				.allowedCharge(-9000).allowedDischarge(9000).maxApparentPower(5000);
		ManagedSymmetricEssDummy ess2 = new ManagedSymmetricEssDummy("ess2") //
				.allowedCharge(-9000).allowedDischarge(9000).maxApparentPower(5000);
		EssClusterDummy ess0 = new EssClusterDummy("ess0", ess1, ess2); //
		return new ManagedSymmetricEss[] { ess0, ess1, ess2, };
	}

	@Test
	public void testCommercial40DischargeSymmetricActivePower() throws Exception {
		ManagedSymmetricEssDummy ess0 = new ManagedSymmetricEssDummy("ess0").maxApparentPower(40000)
				.allowedCharge(-26000).allowedDischarge(40000).precision(100).soc(51);
		Data d = prepareData(ess0);
		Solver s = new Solver(d);

		d.addSimpleConstraint(ess0, Phase.ALL, Pwr.ACTIVE, Relationship.EQ, 610);
		ess0.expectP(700);
		ess0.expectP(0);
		s.solve();

		d.initializeCycle();
		ess0.soc(49);
		d.addSimpleConstraint(ess0, Phase.ALL, Pwr.ACTIVE, Relationship.EQ, 590);
		ess0.expectP(500);
		s.solve();

		// case 1:
//					assertEquals(500, activePower);
//					assertEquals(0, reactivePower);
//					break;
//				case 2:
//					assertEquals(-400, activePower);
//					assertEquals(0, reactivePower);
//					break;
//				case 3:
//					assertEquals(-300, activePower);
//					assertEquals(0, reactivePower);
//					break;
//				case 4:
//					assertEquals(-2000, activePower);
//					assertEquals(0, reactivePower);
//					break;
//				}
//			}
//
//		power.initializeNextCycle();
//		runNo.incrementAndGet();

//
//		power.applyPower();
//
//		power.initializeNextCycle();
//		runNo.incrementAndGet();
//
//		ess0.addPowerConstraint(ConstraintType.CYCLE, Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -310);
//
//		power.applyPower();
//
//		power.initializeNextCycle();
//		runNo.incrementAndGet();
//		ess0.soc(51);
//
//		ess0.addPowerConstraint(ConstraintType.CYCLE, Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -310);
//
//		power.applyPower();
//
//		power.initializeNextCycle();
//		runNo.incrementAndGet();
//		ess0.soc(50);
//
//		// force Charge
//		ess0.addPowerConstraint(ConstraintType.CYCLE, Phase.ALL, Pwr.ACTIVE, Relationship.LESS_OR_EQUALS, -2000);
//
//		power.applyPower();
	}

	@Test
	public void testGetConstraints() {
		ManagedSymmetricEss[] esss = prepareEssCluster();
		Data data = prepareData(esss);
		List<LinearConstraint> cs = Solver.getConstraints(data);

		assertTrue(cs.size() > 0);
	}

	@Test
	public void testCreateGenericEssConstraints() {
		ManagedSymmetricEss[] esss = prepareEssCluster();
		Data data = prepareData(esss);
		List<Constraint> cs = data.createGenericEssConstraints();

		assertEquals((esss.length - 1) * 4, cs.size()); // 4 Constraints per Ess, without Cluster
	}

	@Test
	public void testNoOfCoefficients() {
		ManagedSymmetricEss[] esss = prepareEssCluster();
		Data data = prepareData(esss);

		assertEquals(esss.length * 4 /* phases + all */ * 2 /* pwr */, data.getCoefficients().getNoOfCoefficients());
	}
}
