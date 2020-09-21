package io.openems.edge.ess.core.power;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.ess.power.api.SolverStrategy;
import io.openems.edge.ess.test.DummyManagedAsymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyMetaEss;

public class PowerComponentTest {

	private static AtomicInteger openCallbacks;

	@Before
	public void before() {
		openCallbacks = new AtomicInteger(0);
	}

	@After
	public void after() {
		assertEquals("Not all Callbacks were actually called", 0, openCallbacks.get());
	}

	@Test
	public void testSymmetricEss() throws Exception {
		PowerComponent powerComponent = new PowerComponentImpl();
		DummyManagedSymmetricEss ess0 = new DummyManagedSymmetricEss("ess0", powerComponent) //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(12000) //
				.withSoc(30);

		ComponentTest componentTest = new ComponentTest(powerComponent) //
				.addReference("addEss", ess0) //
				.activate(MyConfig.create() //
						.setStrategy(SolverStrategy.OPTIMIZE_BY_MOVING_TOWARDS_TARGET) //
						.setSymmetricMode(true) //
						.setDebugMode(false) //
						.setEnablePid(false) //
						.build()); //

		expect("#10", ess0, 5000, 0);
		ess0.addPowerConstraint("", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 5000);
		componentTest.next(new TestCase());
	}

	@Test
	public void testAsymmetricEss() throws Exception {
		PowerComponent powerComponent = new PowerComponentImpl();
		DummyManagedAsymmetricEss ess0 = new DummyManagedAsymmetricEss("ess0", powerComponent); //
		ess0 //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(30000) //
				.withSoc(30);

		final ComponentTest componentTest = new ComponentTest(powerComponent) //
				.addReference("addEss", ess0) //
				.activate(MyConfig.create() //
						.setStrategy(SolverStrategy.OPTIMIZE_BY_MOVING_TOWARDS_TARGET) //
						.setSymmetricMode(false) //
						.setDebugMode(false) //
						.setEnablePid(false) //
						.build()); //

		expect("#1", ess0, 5000, 0, 5000, 0, 5000, 0);
		ess0.addPowerConstraint("", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 15000);
		ess0.addPowerConstraint("", Phase.L1, Pwr.ACTIVE, Relationship.EQUALS, 5000);
		ess0.addPowerConstraint("", Phase.L2, Pwr.ACTIVE, Relationship.EQUALS, 5000);
		componentTest.next(new TestCase());
	}

	@Test
	public void testAsymmetricEssAllEqual() throws Exception {
		PowerComponent powerComponent = new PowerComponentImpl();
		DummyManagedAsymmetricEss ess0 = new DummyManagedAsymmetricEss("ess0", powerComponent); //
		ess0 //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(30000) //
				.withSoc(30);

		ComponentTest componentTest = new ComponentTest(powerComponent) //
				.addReference("addEss", ess0) //
				.activate(MyConfig.create() //
						.setStrategy(SolverStrategy.OPTIMIZE_BY_KEEPING_ALL_EQUAL) //
						.setSymmetricMode(false) //
						.setDebugMode(false) //
						.setEnablePid(false) //
						.build()); //

		expect("#1", ess0, 5000, 0, 5000, 0, 5000, 0);
		ess0.addPowerConstraint("", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 15000);
		componentTest.next(new TestCase());
	}

	@Test
	public void testCluster() throws Exception {
		PowerComponent powerComponent = new PowerComponentImpl();
		DummyManagedSymmetricEss ess1 = new DummyManagedSymmetricEss("ess1", powerComponent) //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(12000) //
				.withSoc(30);
		DummyManagedSymmetricEss ess2 = new DummyManagedSymmetricEss("ess2", powerComponent) //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(12000) //
				.withSoc(60);
		DummyMetaEss ess0 = new DummyMetaEss("ess0", powerComponent, ess1, ess2); //

		final ComponentTest componentTest = new ComponentTest(powerComponent) //
				.addReference("addEss", ess0) //
				.addReference("addEss", ess1) //
				.addReference("addEss", ess2) //
				.activate(MyConfig.create() //
						.setStrategy(SolverStrategy.OPTIMIZE_BY_MOVING_TOWARDS_TARGET) //
						.setSymmetricMode(true) //
						.setDebugMode(false) //
						.setEnablePid(false) //
						.build()); //

		// #1
		expect("#1", ess1, -5000, 0);
		expect("#1", ess2, -0, 0);
		ess0.addPowerConstraint("#1", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -5000);
		ess1.withSoc(80); // this is for test #2
		componentTest.next(new TestCase("#1"));

		// #2
		expect("#2", ess1, -4697, 0);
		expect("#2", ess2, -302, 0);
		ess0.addPowerConstraint("#2", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -5000);
		componentTest.next(new TestCase("#2"));

		// #3
		expect("#3", ess1, -4429, 0);
		expect("#3", ess2, -570, 0);
		ess0.addPowerConstraint("#3", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -5000);
		componentTest.next(new TestCase("#3"));

		// #4
		expect("#4", ess1, -4190, 0);
		expect("#4", ess2, -809, 0);
		ess0.addPowerConstraint("#4", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -5000);
		componentTest.next(new TestCase("#4"));

		// #5
		expect("#5", ess1, -3976, 0);
		expect("#5", ess2, -1023, 0);
		ess0.addPowerConstraint("#5", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -5000);
		componentTest.next(new TestCase("#5"));

		// #6
		expect("#6", ess1, -3782, 0);
		expect("#6", ess2, -1217, 0);
		ess0.addPowerConstraint("#6", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -5000);
		componentTest.next(new TestCase("#6"));

		// #7
		expect("#7", ess1, -3606, 0);
		expect("#7", ess2, -1393, 0);
		ess0.addPowerConstraint("#7", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -5000);
		componentTest.next(new TestCase("#7"));

		// #8
		expect("#8", ess1, -3446, 0);
		expect("#8", ess2, -1553, 0);
		ess0.addPowerConstraint("#8", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -5000);
		componentTest.next(new TestCase("#8"));

		// #9
		expect("#9", ess1, -3300, 0);
		expect("#9", ess2, -1699, 0);
		ess0.addPowerConstraint("#9", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -5000);
		componentTest.next(new TestCase("#9"));

		// #10
		expect("#10", ess1, -3165, 0);
		expect("#10", ess2, -1834, 0);
		ess0.addPowerConstraint("#10", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -5000);
		componentTest.next(new TestCase("#10"));

		ess1.withSymmetricApplyPowerCallback(null);
		ess2.withSymmetricApplyPowerCallback(null);
		componentTest.next(new TestCase("#11"));
		componentTest.next(new TestCase("#11"));
		componentTest.next(new TestCase("#12"));
		componentTest.next(new TestCase("#13"));
		componentTest.next(new TestCase("#14"));
		componentTest.next(new TestCase("#15"));
		componentTest.next(new TestCase("#16"));
		componentTest.next(new TestCase("#17"));
		componentTest.next(new TestCase("#18"));
		componentTest.next(new TestCase("#19"));

		// #20
		expect("#20", ess1, -0, 0);
		expect("#20", ess2, -5000, 0);
		ess0.addPowerConstraint("#10", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -5000);
		componentTest.next(new TestCase("#10"));
	}

	@Test
	public void testStrSctr() throws Exception {
		PowerComponent powerComponent = new PowerComponentImpl();
		DummyManagedSymmetricEss ess1 = new DummyManagedSymmetricEss("ess1", powerComponent) //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(12000) //
				.withSoc(30);
		DummyManagedSymmetricEss ess2 = new DummyManagedSymmetricEss("ess2", powerComponent) //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(12000) //
				.withSoc(60);
		DummyManagedSymmetricEss ess3 = new DummyManagedSymmetricEss("ess3", powerComponent) //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(12000) //
				.withSoc(50);
		DummyManagedSymmetricEss ess4 = new DummyManagedSymmetricEss("ess4", powerComponent) //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(12000) //
				.withSoc(10);
		DummyManagedSymmetricEss ess5 = new DummyManagedSymmetricEss("ess5", powerComponent) //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(12000) //
				.withSoc(90);
		DummyManagedSymmetricEss ess6 = new DummyManagedSymmetricEss("ess6", powerComponent) //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(12000) //
				.withSoc(70);
		DummyMetaEss ess0 = new DummyMetaEss("ess0", powerComponent, ess1, ess2, ess3, ess4, ess5, ess6); //

		final ComponentTest componentTest = new ComponentTest(powerComponent) //
				.addReference("addEss", ess0) //
				.addReference("addEss", ess1) //
				.addReference("addEss", ess2) //
				.addReference("addEss", ess3) //
				.addReference("addEss", ess4) //
				.addReference("addEss", ess5) //
				.addReference("addEss", ess6) //
				.activate(MyConfig.create() //
						.setStrategy(SolverStrategy.OPTIMIZE_BY_MOVING_TOWARDS_TARGET) //
						.setSymmetricMode(true) //
						.setDebugMode(false) //
						.setEnablePid(false) //
						.build()); //

		// #1
		expect("#1", ess1, 0, 0);
		expect("#1", ess2, 9954, 0); // third largest SoC
		expect("#1", ess3, 0, 0);
		expect("#1", ess4, 0, 0);
		expect("#1", ess5, 10062, 0); // largest SoC
		expect("#1", ess6, 9986, 0); // second largest SoC
		ess0.addPowerConstraint("#1", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 30000);
		componentTest.next(new TestCase("#1"));

		// #2
		expect("#2", ess1, 0, 0);
		expect("#2", ess2, 8257, 0); // third largest SoC
		expect("#2", ess3, 0, 0);
		expect("#2", ess4, 0, 0);
		expect("#2", ess5, 8435, 0); // largest SoC
		expect("#2", ess6, 8310, 0); // second largest SoC
		ess0.addPowerConstraint("#2", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 25000);
		componentTest.next(new TestCase("#2"));

		// #3
		expect("#3", ess1, 0, 0);
		expect("#3", ess2, 1634, 0); // third largest SoC
		expect("#3", ess3, 0, 0);
		expect("#3", ess4, 0, 0);
		expect("#3", ess5, 1723, 0); // largest SoC
		expect("#3", ess6, 1644, 0); // second largest SoC
		ess0.addPowerConstraint("#3", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 5000);
		componentTest.next(new TestCase("#3"));

		// #4 not strictly defined force charge
		expect("#4", ess1, -2000, 0);
		expect("#4", ess2, -2000, 0);
		expect("#4", ess3, -2000, 0);
		expect("#4", ess4, -2000, 0);
		expect("#4", ess5, -2000, 0); // largest SoC
		expect("#4", ess6, -2000, 0); // second largest SoC
		ess1.addPowerConstraint("#4", Phase.ALL, Pwr.ACTIVE, Relationship.LESS_OR_EQUALS, -2000);
		ess2.addPowerConstraint("#4", Phase.ALL, Pwr.ACTIVE, Relationship.LESS_OR_EQUALS, -2000);
		ess3.addPowerConstraint("#4", Phase.ALL, Pwr.ACTIVE, Relationship.LESS_OR_EQUALS, -2000);
		ess4.addPowerConstraint("#4", Phase.ALL, Pwr.ACTIVE, Relationship.LESS_OR_EQUALS, -2000);
		ess5.addPowerConstraint("#4", Phase.ALL, Pwr.ACTIVE, Relationship.LESS_OR_EQUALS, -2000);
		ess6.addPowerConstraint("#4", Phase.ALL, Pwr.ACTIVE, Relationship.LESS_OR_EQUALS, -2000);
		componentTest.next(new TestCase("#4"));
	}

	@Test
	public void testCommercial40Cluster() throws Exception {
		PowerComponent powerComponent = new PowerComponentImpl();
		DummyManagedSymmetricEss ess1 = new DummyManagedSymmetricEss("ess1", powerComponent) //
				.withMaxApparentPower(40000) //
				.withSoc(1) //
				.withPowerPrecision(100);
		DummyManagedSymmetricEss ess2 = new DummyManagedSymmetricEss("ess2", powerComponent) //
				.withMaxApparentPower(40000) //
				.withSoc(97) //
				.withPowerPrecision(100);
		DummyMetaEss ess0 = new DummyMetaEss("ess0", powerComponent, ess1, ess2); //

		final ComponentTest componentTest = new ComponentTest(powerComponent) //
				.addReference("addEss", ess0) //
				.addReference("addEss", ess1) //
				.addReference("addEss", ess2) //
				.activate(MyConfig.create() //
						.setStrategy(SolverStrategy.OPTIMIZE_BY_MOVING_TOWARDS_TARGET) //
						.setSymmetricMode(true) //
						.setDebugMode(false) //
						.setEnablePid(false) //
						.build()); //

		// #1
		ess1.withAllowedChargePower(-500).withAllowedDischargePower(500);
		ess2.withAllowedChargePower(-500).withAllowedDischargePower(500);
		assertEquals(1000, ess0.getPower().getMaxPower(ess0, Phase.ALL, Pwr.ACTIVE));
		assertEquals(-1000, ess0.getPower().getMinPower(ess0, Phase.ALL, Pwr.ACTIVE));
		expect("#1", ess1, -500, 0);
		expect("#1", ess2, -500, 0);
		ess0.addPowerConstraint("#1", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -1000);
		componentTest.next(new TestCase("#1"));

		// #2
		ess1.withAllowedChargePower(-1000);
		ess2.withAllowedChargePower(-1000);
		assertEquals(-2000, ess0.getPower().getMinPower(ess0, Phase.ALL, Pwr.ACTIVE));
		expect("#2", ess1, -1000, 0);
		expect("#2", ess2, -1000, 0);
		ess0.addPowerConstraint("#2", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -2000);
		componentTest.next(new TestCase("#2"));

		// #3
		ess1.withAllowedChargePower(-2000);
		ess2.withAllowedChargePower(-2000);
		assertEquals(-4000, ess0.getPower().getMinPower(ess0, Phase.ALL, Pwr.ACTIVE));
		expect("#3", ess1, -2000, 0);
		expect("#3", ess2, -2000, 0);
		ess0.addPowerConstraint("#3", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -4000);
		componentTest.next(new TestCase("#3"));

		// #4
		ess1.withAllowedChargePower(-3000);
		ess2.withAllowedChargePower(-3000);
		assertEquals(-6000, ess0.getPower().getMinPower(ess0, Phase.ALL, Pwr.ACTIVE));
		expect("#4", ess1, -2700, 0); // move towards ess1 because it is empty
		expect("#4", ess2, -2300, 0);
		ess0.addPowerConstraint("#4", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -5000);
		componentTest.next(new TestCase("#4"));

		// #5
		ess1.withAllowedChargePower(-3500);
		ess2.withAllowedChargePower(-3500);
		assertEquals(-7000, ess0.getPower().getMinPower(ess0, Phase.ALL, Pwr.ACTIVE));
		expect("#5", ess1, -2900, 0);
		expect("#5", ess2, -2100, 0);
		ess0.addPowerConstraint("#5", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -5000);
		componentTest.next(new TestCase("#5"));

		// #6
		ess1.withAllowedChargePower(-4000);
		ess2.withAllowedChargePower(-4000);
		assertEquals(-8000, ess0.getPower().getMinPower(ess0, Phase.ALL, Pwr.ACTIVE));
		expect("#6", ess1, -3100, 0); // move towards ess1 because it is empty
		expect("#6", ess2, -1900, 0);
		ess0.addPowerConstraint("#6", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -5000);
		componentTest.next(new TestCase("#6"));

		// #7
		ess1.withAllowedChargePower(-6000);
		ess2.withAllowedChargePower(-6000);
		assertEquals(-12000, ess0.getPower().getMinPower(ess0, Phase.ALL, Pwr.ACTIVE));
		expect("#7", ess1, -3300, 0); // move towards ess1 because it is empty
		expect("#7", ess2, -1700, 0);
		ess0.addPowerConstraint("#7", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -5000);
		componentTest.next(new TestCase("#7"));
	}

	private static void expect(String description, DummyManagedSymmetricEss ess, int p, int q) {
		openCallbacks.incrementAndGet();
		ess.withSymmetricApplyPowerCallback((record) -> {
			openCallbacks.decrementAndGet();
			// System.out.println(description + " for " + ess.id() + ": " + activePower);
			assertEquals(description + " for " + ess.id(), p, record.activePower);
			assertEquals(description + " for " + ess.id(), q, record.reactivePower);
		});
	}

	private static void expect(String description, DummyManagedAsymmetricEss ess, int pL1, int qL1, int pL2, int qL2,
			int pL3, int qL3) {
		openCallbacks.incrementAndGet();
		ess.withAsymmetricApplyPowerCallback((record) -> {
			openCallbacks.decrementAndGet();
			assertEquals(description + " for " + ess.id(), pL1, record.activePowerL1);
			assertEquals(description + " for " + ess.id(), qL1, record.reactivePowerL1);
			assertEquals(description + " for " + ess.id(), pL2, record.activePowerL2);
			assertEquals(description + " for " + ess.id(), qL2, record.reactivePowerL2);
			assertEquals(description + " for " + ess.id(), pL3, record.activePowerL3);
			assertEquals(description + " for " + ess.id(), qL3, record.reactivePowerL3);
		});
	}
}
