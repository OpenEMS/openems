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
	public void testSymmetricEss() throws Exception {
		PowerComponent powerComponent = new PowerComponentImpl();
		var ess0 = new DummyManagedSymmetricEss("ess0", powerComponent) //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(12000) //
				.withSoc(30);

		final var cm = new DummyConfigurationAdmin();
		cm.getOrCreateEmptyConfiguration(PowerComponent.SINGLETON_SERVICE_PID);

		var componentTest = new ComponentTest(powerComponent) //
				.addReference("cm", cm) //
				.addReference("addEss", ess0) //
				.activate(MyConfig.create() //
						.setStrategy(SolverStrategy.KEEP_MAXIMUM_INVERTERS_AT_MAX_EFFICIENCY) //
						.setSymmetricMode(true) //
						.setDebugMode(false) //
						.setEnablePid(false) //
						.build()); //

		expect("#10", ess0, 5000, 3000);
		ess0.addPowerConstraint("", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 10000);
		ess0.addPowerConstraint("", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 3000);
		componentTest.next(new TestCase());
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
