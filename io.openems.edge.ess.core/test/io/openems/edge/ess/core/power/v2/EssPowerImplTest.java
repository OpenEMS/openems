package io.openems.edge.ess.core.power.v2;

import static io.openems.edge.common.type.Phase.SingleOrAllPhase.ALL;
import static io.openems.edge.ess.power.api.Pwr.ACTIVE;
import static io.openems.edge.ess.power.api.Pwr.REACTIVE;
import static io.openems.edge.ess.power.api.Relationship.EQUALS;
import static io.openems.edge.ess.power.api.Relationship.GREATER_OR_EQUALS;
import static io.openems.edge.ess.power.api.Relationship.LESS_OR_EQUALS;
import static io.openems.edge.ess.power.api.SolverStrategy.BALANCE;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.ess.core.power.EssPower;
import io.openems.edge.ess.core.power.EssPowerImpl;
import io.openems.edge.ess.core.power.MyConfig;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

public class EssPowerImplTest {

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
	public void test() throws Exception {
		final var power = new EssPowerImpl();
		var ess0 = new DummyManagedSymmetricEss("ess0") //
				.setPower(power) //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(12000) //
				.withSoc(30);

		final var cm = new DummyConfigurationAdmin();
		cm.getOrCreateEmptyConfiguration(EssPower.SINGLETON_SERVICE_PID);

		final var test = new ComponentTest(power) //
				.addReference("cm", cm) //
				.addReference("addEss", ess0) //
				.activate(MyConfig.create() //
						.setStrategy(BALANCE) //
						.setSymmetricMode(true) //
						.setDebugMode(false) //
						.setEnablePid(false) //
						.build()) //
				.next(new TestCase());

		final var constraint = ess0.addPowerConstraint("", ALL, ACTIVE, EQUALS, 5000);
		ess0.addPowerConstraint("", ALL, ACTIVE, LESS_OR_EQUALS, 5000);
		ess0.addPowerConstraint("", ALL, ACTIVE, GREATER_OR_EQUALS, 5000);
		ess0.addPowerConstraint("", ALL, REACTIVE, EQUALS, 3000);
		ess0.addPowerConstraint("", ALL, REACTIVE, LESS_OR_EQUALS, 3000);
		ess0.addPowerConstraintAndValidate("", ALL, REACTIVE, GREATER_OR_EQUALS, 3000);

		assertEquals("ess0Q", power.getCoefficient(ess0, ALL, REACTIVE).toString());

		power.removeConstraint(constraint);

		assertEquals(0, power.getMinPower(ess0, ALL, ACTIVE));
		assertEquals(0, power.getMaxPower(ess0, ALL, ACTIVE));

		test.deactivate();
	}
}