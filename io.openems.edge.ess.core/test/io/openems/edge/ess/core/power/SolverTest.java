package io.openems.edge.ess.core.power;

import static org.junit.Assert.assertEquals;

import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.junit.Test;

import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.ess.power.api.SolverStrategy;

public class SolverTest {

	private static Data prepareDataAndSetSymmetricMode(boolean symmetricMode, ManagedSymmetricEss... esss) {
		PowerComponent c = new PowerComponent();
		Data data = new Data(c);
		data.setSymmetricMode(symmetricMode);
		for (ManagedSymmetricEss ess : esss) {
			c.addEss(ess);
			data.addEss(ess);
		}
		data.initializeCycle();
		return data;
	}

	private static Data prepareData(ManagedSymmetricEss... esss) {
		PowerComponent c = new PowerComponent();
		Data data = new Data(c);
		for (ManagedSymmetricEss ess : esss) {
			c.addEss(ess);
			data.addEss(ess);
		}
		data.initializeCycle();
		return data;
	}

	private static ManagedSymmetricEss[] prepareEssCluster() {
		ManagedSymmetricEssDummy ess1 = new ManagedSymmetricEssDummy("ess1") //
				.allowedChargePower(-9000).allowedDischargePower(9000).maxApparentPower(5000);
		ManagedSymmetricEssDummy ess2 = new ManagedSymmetricEssDummy("ess2") //
				.allowedChargePower(-9000).allowedDischargePower(9000).maxApparentPower(5000);
		EssClusterDummy ess0 = new EssClusterDummy("ess0", ess1, ess2); //
		return new ManagedSymmetricEss[] { ess0, ess1, ess2, };
	}

	@Test
	public void testSymmetricMode() throws Exception {
		ManagedSymmetricEssDummy ess1 = new ManagedSymmetricEssDummy("ess1") //
				.allowedChargePower(-50000).allowedDischargePower(50000).maxApparentPower(12000).soc(30);
		ManagedAsymmetricEssDummy ess2 = new ManagedAsymmetricEssDummy("ess2") //
				.allowedChargePower(-50000).allowedDischargePower(50000).maxApparentPower(12000).soc(60);
		EssClusterDummy ess0 = new EssClusterDummy("ess0", ess1, ess2); //
		Data d = prepareDataAndSetSymmetricMode(false, ess0, ess1, ess2);
		Solver s = new Solver(d);

		d.addSimpleConstraint("", ess0.id(), Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 5000);
		s.solve();
		d.initializeCycle();
		d.addSimpleConstraint("", ess0.id(), Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -9000);
		s.solve();
	}

	@Test
	public void testStrSctr() throws Exception {
		ManagedSymmetricEssDummy ess1 = new ManagedSymmetricEssDummy("ess1") //
				.allowedChargePower(-50000).allowedDischargePower(50000).maxApparentPower(12000).soc(30);
		ManagedSymmetricEssDummy ess2 = new ManagedSymmetricEssDummy("ess2") //
				.allowedChargePower(-50000).allowedDischargePower(50000).maxApparentPower(12000).soc(60);
		ManagedSymmetricEssDummy ess3 = new ManagedSymmetricEssDummy("ess3") //
				.allowedChargePower(-50000).allowedDischargePower(50000).maxApparentPower(12000).soc(50);
		ManagedSymmetricEssDummy ess4 = new ManagedSymmetricEssDummy("ess4") //
				.allowedChargePower(-50000).allowedDischargePower(50000).maxApparentPower(12000).soc(10);
		ManagedSymmetricEssDummy ess5 = new ManagedSymmetricEssDummy("ess5") //
				.allowedChargePower(-50000).allowedDischargePower(50000).maxApparentPower(12000).soc(90);
		ManagedSymmetricEssDummy ess6 = new ManagedSymmetricEssDummy("ess6") //
				.allowedChargePower(-50000).allowedDischargePower(50000).maxApparentPower(12000).soc(70);
		EssClusterDummy ess0 = new EssClusterDummy("ess0", ess1, ess2, ess3, ess4, ess5, ess6); //
		Data d = prepareData(ess0, ess1, ess2, ess3, ess4, ess5, ess6);
		final Solver s = new Solver(d);

		// #1
		d.addSimpleConstraint("#1", ess0.id(), Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 30000);
		ess1.expectP(0).expectQ(0);
		ess2.expectP(9864).expectQ(0); // third largest SoC
		ess3.expectP(0).expectQ(0);
		ess4.expectP(0).expectQ(0);
		ess5.expectP(10172).expectQ(0); // largest SoC
		ess6.expectP(9966).expectQ(0);// second largest SoC
		s.solve();

		// #2
		d.initializeCycle();
		d.addSimpleConstraint("#2", ess0.id(), Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 25000);
		ess1.expectP(0).expectQ(0);
		ess2.expectP(8113).expectQ(0);
		ess3.expectP(0).expectQ(0);
		ess4.expectP(0).expectQ(0);
		ess5.expectP(8611).expectQ(0);
		ess6.expectP(8278).expectQ(0);
		s.solve();

		// #3
		d.initializeCycle();
		d.addSimpleConstraint("#3", ess0.id(), Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 5000);
		ess1.expectP(0).expectQ(0);
		ess2.expectP(1569).expectQ(0);
		ess3.expectP(0).expectQ(0);
		ess4.expectP(0).expectQ(0);
		ess5.expectP(1832).expectQ(0);
		ess6.expectP(1601).expectQ(0);
		s.solve();

		// #4 not strictly defined force charge
		d.initializeCycle();
		d.addSimpleConstraint("#4", ess1.id(), Phase.ALL, Pwr.ACTIVE, Relationship.LESS_OR_EQUALS, -2000);
		d.addSimpleConstraint("#4", ess2.id(), Phase.ALL, Pwr.ACTIVE, Relationship.LESS_OR_EQUALS, -2000);
		d.addSimpleConstraint("#4", ess3.id(), Phase.ALL, Pwr.ACTIVE, Relationship.LESS_OR_EQUALS, -2000);
		d.addSimpleConstraint("#4", ess4.id(), Phase.ALL, Pwr.ACTIVE, Relationship.LESS_OR_EQUALS, -2000);
		d.addSimpleConstraint("#4", ess5.id(), Phase.ALL, Pwr.ACTIVE, Relationship.LESS_OR_EQUALS, -2000);
		d.addSimpleConstraint("#4", ess6.id(), Phase.ALL, Pwr.ACTIVE, Relationship.LESS_OR_EQUALS, -2000);
		ess1.expectP(-2000).expectQ(0);
		ess2.expectP(-2000).expectQ(0);
		ess3.expectP(-2000).expectQ(0);
		ess4.expectP(-2000).expectQ(0);
		ess5.expectP(-2000).expectQ(0);
		ess6.expectP(-2000).expectQ(0);
		s.solve();

	}

	@Test
	public void testCommercial40DischargeSymmetricActivePower() throws Exception {
		ManagedSymmetricEssDummy ess0 = new ManagedSymmetricEssDummy("ess0").maxApparentPower(40000)
				.allowedChargePower(-26000).allowedDischargePower(40000).precision(100).soc(51);
		Data d = prepareData(ess0);
		Solver s = new Solver(d);

		// #1
		d.addSimpleConstraint("#1", ess0.id(), Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 610);
		ess0.expectP(700);
		s.solve();

		// #2
		d.initializeCycle();
		ess0.soc(49);
		d.addSimpleConstraint("#2", ess0.id(), Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 590);
		ess0.expectP(500);
		s.solve();

		// #3
		d.initializeCycle();
		ess0.soc(49);
		d.addSimpleConstraint("#3", ess0.id(), Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -310);
		ess0.expectP(-400);
		s.solve();

		// #4
		d.initializeCycle();
		ess0.soc(51);
		d.addSimpleConstraint("#4", ess0.id(), Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -310);
		ess0.expectP(-300);
		s.solve();

		// #5 not strictly defined force charge
		d.initializeCycle();
		ess0.soc(50);
		d.addSimpleConstraint("#5", ess0.id(), Phase.ALL, Pwr.ACTIVE, Relationship.LESS_OR_EQUALS, -2000);
		ess0.expectP(-2000);
		s.solve();
	}

	@Test
	public void testNoOfCoefficients() {
		ManagedSymmetricEss[] esss = prepareEssCluster();
		Data data = prepareData(esss);

		assertEquals(esss.length * 4 /* phases + all */ * 2 /* pwr */, data.getCoefficients().getNoOfCoefficients());
	}

	@Test
	public void testCommercial40Cluster() throws Exception {
		ManagedSymmetricEssDummy ess1 = new ManagedSymmetricEssDummy("ess1").maxApparentPower(40000)
				.allowedChargePower(-500).allowedDischargePower(500).precision(100).soc(1);
		ManagedSymmetricEssDummy ess2 = new ManagedSymmetricEssDummy("ess2").maxApparentPower(40000)
				.allowedChargePower(-500).allowedDischargePower(500).precision(100).soc(97);
		EssClusterDummy ess0 = new EssClusterDummy("ess0", ess1, ess2);
		Data d = prepareData(ess0, ess1, ess2);
		Solver s = new Solver(d);
		s.setStrategy(SolverStrategy.OPTIMIZE_BY_KEEPING_TARGET_DIRECTION_AND_MAXIMIZING_IN_ORDER);

		// #1
		int p = Math.max(-5000, (int) s.getActivePowerExtrema("ess0", Phase.ALL, Pwr.ACTIVE, GoalType.MINIMIZE));
		d.addSimpleConstraint("#1", ess0.id(), Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, p);
		ess1.expectP(-500);
		ess2.expectP(-500);
		// TODO: Q should not be -1000 & 1000!
		s.solve();
		d.initializeCycle();

		// #2
		ess1.allowedChargePower(-1000);
		ess2.allowedChargePower(-1000);
		p = Math.max(-5000, (int) s.getActivePowerExtrema("ess0", Phase.ALL, Pwr.ACTIVE, GoalType.MINIMIZE));
		d.addSimpleConstraint("#1", ess0.id(), Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, p);
		ess1.expectP(-1000);
		ess2.expectP(-1000);
		s.solve();
		d.initializeCycle();

		// #3
		ess1.allowedChargePower(-2000);
		ess2.allowedChargePower(-2000);
		p = Math.max(-5000, (int) s.getActivePowerExtrema("ess0", Phase.ALL, Pwr.ACTIVE, GoalType.MINIMIZE));
		d.addSimpleConstraint("#1", ess0.id(), Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, p);
		ess1.expectP(-2000);
		ess2.expectP(-2000);
		s.solve();
		d.initializeCycle();

		// #4
		ess1.allowedChargePower(-3000);
		ess2.allowedChargePower(-3000);
		p = Math.max(-5000, (int) s.getActivePowerExtrema("ess0", Phase.ALL, Pwr.ACTIVE, GoalType.MINIMIZE));
		d.addSimpleConstraint("#1", ess0.id(), Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, p);
		ess1.expectP(-3000);
		ess2.expectP(-2000);
		s.solve();
		d.initializeCycle();

		// #5
		ess1.allowedChargePower(-3500);
		ess2.allowedChargePower(-3500);
		p = Math.max(-5000, (int) s.getActivePowerExtrema("ess0", Phase.ALL, Pwr.ACTIVE, GoalType.MINIMIZE));
		d.addSimpleConstraint("#1", ess0.id(), Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, p);
		// TODO: should prefer ess1, because it is empty!
		ess1.expectP(-3500);
		ess2.expectP(-1500);
		s.solve();
		d.initializeCycle();

		// #6
		ess1.allowedChargePower(-4000);
		ess2.allowedChargePower(-4000);
		p = Math.max(-5000, (int) s.getActivePowerExtrema("ess0", Phase.ALL, Pwr.ACTIVE, GoalType.MINIMIZE));
		d.addSimpleConstraint("#1", ess0.id(), Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, p);
		// TODO: should prefer ess1, because it is empty!
		ess1.expectP(-4000);
		ess2.expectP(-1000);
		s.solve();
		d.initializeCycle();

		// #6
		ess1.allowedChargePower(-5000);
		ess2.allowedChargePower(-5000);
		p = Math.max(-5000, (int) s.getActivePowerExtrema("ess0", Phase.ALL, Pwr.ACTIVE, GoalType.MINIMIZE));
		d.addSimpleConstraint("#1", ess0.id(), Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, p);
		ess1.expectP(-5000);
		ess2.expectP(0);
		s.solve();
		d.initializeCycle();

	}
}
