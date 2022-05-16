package io.openems.edge.ess.core.power;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.ess.power.api.SolverStrategy;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyMetaEss;

public class PowerComponentTest2 {

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
	public void testCluster() throws Exception {
		PowerComponent powerComponent = new PowerComponentImpl();
		var ess1 = new DummyManagedSymmetricEss("ess1", powerComponent) //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(12000) //
				.withSoc(15);
		var ess2 = new DummyManagedSymmetricEss("ess2", powerComponent) //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(12000) //
				.withSoc(20);
		var ess3 = new DummyManagedSymmetricEss("ess3", powerComponent) //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(12000) //
				.withSoc(60);
		var ess4 = new DummyManagedSymmetricEss("ess4", powerComponent) //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(12000) //
				.withSoc(70);
		var ess5 = new DummyManagedSymmetricEss("ess5", powerComponent) //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(12000) //
				.withSoc(80);
		var ess0 = new DummyMetaEss("ess0", powerComponent, ess1, ess2, ess3, ess4, ess5); //

		final var cm = new DummyConfigurationAdmin();
		cm.getOrCreateEmptyConfiguration(PowerComponent.SINGLETON_SERVICE_PID);

		final var componentTest = new ComponentTest(powerComponent) //
				.addReference("cm", cm) //
				.addReference("addEss", ess0) //
				.addReference("addEss", ess1) //
				.addReference("addEss", ess2) //
				.addReference("addEss", ess3) //
				.addReference("addEss", ess4) //
				.addReference("addEss", ess5) //
				.activate(MyConfig.create() //
						.setStrategy(SolverStrategy.OPERATE_CLUSTER_AT_MAX_EFFICIENCY) //
						.setSymmetricMode(true) //
						.setDebugMode(false) //
						.setEnablePid(false) //
						.build()); //

		// #1
		expect("#1", ess1, -5000, -3000);
		expect("#1", ess2, -0, 0);
		ess0.addPowerConstraint("#1", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -5000);
		ess0.addPowerConstraint("#1", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, -3000);
		//ess1.withSoc(15); // this is for test #2
		componentTest.next(new TestCase("#1"));

//		// #2
		expect("#2", ess1, -4697, -2818);
		expect("#2", ess2, -302, -181);
		ess0.addPowerConstraint("#2", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -5000);
		ess0.addPowerConstraint("#2", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, -3000);
		componentTest.next(new TestCase("#2"));
//
//		// #3
//		expect("#3", ess1, -4429, -2657);
//		expect("#3", ess2, -570, -342);
//		ess0.addPowerConstraint("#3", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -5000);
//		ess0.addPowerConstraint("#3", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, -3000);
//		componentTest.next(new TestCase("#3"));
//
//		// #4
//		expect("#4", ess1, -4190, -2514);
//		expect("#4", ess2, -809, -485);
//		ess0.addPowerConstraint("#4", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -5000);
//		ess0.addPowerConstraint("#4", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, -3000);
//		componentTest.next(new TestCase("#4"));
//
//		// #5
//		expect("#5", ess1, -3976, -2385);
//		expect("#5", ess2, -1023, -614);
//		ess0.addPowerConstraint("#5", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -5000);
//		ess0.addPowerConstraint("#5", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, -3000);
//		componentTest.next(new TestCase("#5"));
//
//		// #6
//		expect("#6", ess1, -3782, -2269);
//		expect("#6", ess2, -1217, -730);
//		ess0.addPowerConstraint("#6", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -5000);
//		ess0.addPowerConstraint("#6", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, -3000);
//		componentTest.next(new TestCase("#6"));
//
//		// #7
//		expect("#7", ess1, -3606, -2164);
//		expect("#7", ess2, -1393, -835);
//		ess0.addPowerConstraint("#7", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -5000);
//		ess0.addPowerConstraint("#7", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, -3000);
//		componentTest.next(new TestCase("#7"));
//
//		// #8
//		expect("#8", ess1, -3446, -2067);
//		expect("#8", ess2, -1553, -932);
//		ess0.addPowerConstraint("#8", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -5000);
//		ess0.addPowerConstraint("#8", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, -3000);
//		componentTest.next(new TestCase("#8"));
//
//		// #9
//		expect("#9", ess1, -3300, -1980);
//		expect("#9", ess2, -1699, -1019);
//		ess0.addPowerConstraint("#9", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -5000);
//		ess0.addPowerConstraint("#9", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, -3000);
//		componentTest.next(new TestCase("#9"));
//
//		// #10
//		expect("#10", ess1, -3165, -1899);
//		expect("#10", ess2, -1834, -1100);
//		ess0.addPowerConstraint("#10", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -5000);
//		ess0.addPowerConstraint("#10", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, -3000);
//		componentTest.next(new TestCase("#10"));
//
//		ess1.withSymmetricApplyPowerCallback(null);
//		ess2.withSymmetricApplyPowerCallback(null);
//		componentTest.next(new TestCase("#11"));
//		componentTest.next(new TestCase("#11"));
//		componentTest.next(new TestCase("#12"));
//		componentTest.next(new TestCase("#13"));
//		componentTest.next(new TestCase("#14"));
//		componentTest.next(new TestCase("#15"));
//		componentTest.next(new TestCase("#16"));
//		componentTest.next(new TestCase("#17"));
//		componentTest.next(new TestCase("#18"));
//		componentTest.next(new TestCase("#19"));
//
//		// #20
//		expect("#20", ess1, -0, 0);
//		expect("#20", ess2, -5000, -3000);
//		ess0.addPowerConstraint("#20", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -5000);
//		ess0.addPowerConstraint("#20", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, -3000);
//		componentTest.next(new TestCase("#20"));
	}

	private static void expect(String description, DummyManagedSymmetricEss ess, int p, int q) {
		openCallbacks.incrementAndGet();
		ess.withSymmetricApplyPowerCallback(record -> {
			openCallbacks.decrementAndGet();
			// System.out.println(description + " for " + ess.id() + ": " + activePower);
			assertEquals(description + " for " + ess.id(), p, record.activePower);
			assertEquals(description + " for " + ess.id(), q, record.reactivePower);
		});
	}
}
