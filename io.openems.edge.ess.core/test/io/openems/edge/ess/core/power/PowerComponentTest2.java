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
	public void testOnlyOneEssDistribution() throws Exception {
		EssPower powerComponent = new EssPowerImpl();

		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-12000) //
				.withAllowedDischargePower(12000) //
				.withMaxApparentPower(10000) //
				.withSoc(60);

		var ess0 = new DummyMetaEss("ess0", ess1) //
				.setPower(powerComponent);

		final var cm = new DummyConfigurationAdmin();
		cm.getOrCreateEmptyConfiguration(EssPower.SINGLETON_SERVICE_PID);

		final var componentTest = new ComponentTest(powerComponent) //
				.addReference("cm", cm) //
				.addReference("addEss", ess0) //
				.addReference("addEss", ess1) //

				.activate(MyConfig.create() //
						.setStrategy(SolverStrategy.OPTIMIZE_BY_KEEPING_ALL_EQUAL) //
						.setSymmetricMode(true) //
						.setDebugMode(false) //
						.setEnablePid(false) //
						.build()); //

		expect("#1.1", ess1, 10000, 0);

		ess0.setActivePowerEquals(10000);
		componentTest.next(new TestCase("#1"));

		expect("#2.1", ess1, 10000, 0);

		ess0.setActivePowerEquals(12000);
		componentTest.next(new TestCase("#2"));

	}

	/**
	 * Testing near equals strategy.
	 * 
	 * @throws Exception on exception
	 */
	@Test
	public void testNearEqualDistribution() throws Exception {
		EssPower powerComponent = new EssPowerImpl();

		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-12000) //
				.withAllowedDischargePower(12000) //
				.withMaxApparentPower(10000) //
				.withSoc(60);
		var ess2 = new DummyManagedSymmetricEss("ess2") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-12000) //
				.withAllowedDischargePower(12000) //
				.withMaxApparentPower(10000) //
				.withSoc(60);
		var ess3 = new DummyManagedSymmetricEss("ess3") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-12000) //
				.withAllowedDischargePower(12000) //
				.withMaxApparentPower(10000) //
				.withSoc(30);
		var ess4 = new DummyManagedSymmetricEss("ess4") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-12000) //
				.withAllowedDischargePower(12000) //
				.withMaxApparentPower(10000) //
				.withSoc(60);
		var ess0 = new DummyMetaEss("ess0", ess1, ess2, ess3, ess4) //
				.setPower(powerComponent);

		final var cm = new DummyConfigurationAdmin();
		cm.getOrCreateEmptyConfiguration(EssPower.SINGLETON_SERVICE_PID);

		final var componentTest = new ComponentTest(powerComponent) //
				.addReference("cm", cm) //
				.addReference("addEss", ess0) //
				.addReference("addEss", ess1) //
				.addReference("addEss", ess2) //
				.addReference("addEss", ess3) //
				.addReference("addEss", ess4) //
				.activate(MyConfig.create() //
						.setStrategy(SolverStrategy.OPTIMIZE_BY_KEEPING_ALL_EQUAL) //
						.setSymmetricMode(true) //
						.setDebugMode(false) //
						.setEnablePid(false) //
						.build()); //

		// #1 Discharging
		expect("#1.1", ess1, 2500, 0);
		expect("#1.2", ess2, 2500, 0);
		expect("#1.3", ess3, 2500, 0);
		expect("#1.4", ess4, 2500, 0);

		ess0.addPowerConstraint("SetActivePowerEquals1", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 10000);
		ess0.setActivePowerEquals(10000);
		componentTest.next(new TestCase("#1"));

		// #2 Charging
		expect("#2.1", ess1, -2500, 0);
		expect("#2.2", ess2, -2500, 0);
		expect("#2.3", ess3, -2500, 0);
		expect("#2.4", ess4, -2500, 0);

		ess0.addPowerConstraint("SetActivePowerEquals2", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -10000);
		ess0.setActivePowerEquals(10000);
		componentTest.next(new TestCase("#1"));

		// #3 Discharging with lower allowed discharge power
		ess4.withAllowedDischargePower(1900);

		// Should be
		expect("#3.1", ess1, 2701, 0);
		expect("#3.2", ess2, 2701, 0);
		expect("#3.3", ess3, 2701, 0);
		expect("#3.4", ess4, 1897, 0);

		ess0.addPowerConstraint("SetActivePowerEquals3", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 10000);
		componentTest.next(new TestCase("#3"));

		// #4 charging with lower allowed charge power
		ess4.withAllowedDischargePower(12000);
		ess4.withAllowedChargePower(-1900);

		expect("#4.1", ess1, -2700, 0);
		expect("#4.2", ess2, -2700, 0);
		expect("#4.3", ess3, -2703, 0);
		expect("#4.4", ess4, -1896, 0);

		ess0.addPowerConstraint("SetActivePowerEquals4", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -10000);
		componentTest.next(new TestCase("#4"));

		// #5 keeping zero
		expect("#5.1", ess1, 0, 0);
		expect("#5.2", ess2, 0, 0);
		expect("#5.3", ess3, 0, 0);
		expect("#5.4", ess4, 0, 0);

		ess0.addPowerConstraint("SetActivePowerEquals5", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 0);
		componentTest.next(new TestCase("#5"));

		ess4.withAllowedChargePower(1000);

		// #6 keeping zero
		expect("#5.1", ess1, 0, 0);
		expect("#5.2", ess2, 0, 0);
		expect("#5.3", ess3, 0, 0);
		expect("#5.4", ess4, 0, 0);

		ess0.addPowerConstraint("ctrl0", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 0);
		componentTest.next(new TestCase("#5"));

	}

	private static void expect(String description, DummyManagedSymmetricEss ess, int p, int q) {
		openCallbacks.incrementAndGet();
		ess.withSymmetricApplyPowerCallback(record -> {
			openCallbacks.decrementAndGet();
			// System.out.println(description + " for " + ess.id() + ": " + activePower);
			assertEquals(description + " for " + ess.id(), p, record.activePower());
			assertEquals(description + " for " + ess.id(), q, record.reactivePower());
		});
	}

}
