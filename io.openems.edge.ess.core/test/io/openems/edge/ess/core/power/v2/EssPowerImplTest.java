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

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.ess.core.power.EssPower;
import io.openems.edge.ess.core.power.EssPowerImpl;
import io.openems.edge.ess.core.power.MyConfig;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyMetaEss;

public class EssPowerImplTest {

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

		assertEquals(5000, power.getMinPower(ess0, ALL, ACTIVE));
		assertEquals(5000, power.getMaxPower(ess0, ALL, ACTIVE));

		test.deactivate();
	}

	/**
	 * ess0 = [ess1, ess2], ess3 not in cluster. cluster EQUALS +10000W, ess3 EQUALS
	 * -5000W.
	 */
	@Test
	public void testStandaloneEssAndCluster() throws Exception {
		var p1 = new AtomicInteger();
		var p2 = new AtomicInteger();
		var p3 = new AtomicInteger();

		final var power = new EssPowerImpl();
		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.setPower(power) //
				.withSoc(50) //
				.withAllowedChargePower(-10000) //
				.withAllowedDischargePower(10000) //
				.withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p1.set(r.activePower()));
		var ess2 = new DummyManagedSymmetricEss("ess2") //
				.setPower(power) //
				.withSoc(50) //
				.withAllowedChargePower(-10000) //
				.withAllowedDischargePower(10000) //
				.withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p2.set(r.activePower()));
		var ess3 = new DummyManagedSymmetricEss("ess3") //
				.setPower(power) //
				.withSoc(50) //
				.withAllowedChargePower(-10000) //
				.withAllowedDischargePower(10000) //
				.withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p3.set(r.activePower()));
		var cluster0 = new DummyMetaEss("cluster0", ess1, ess2);

		final var cm = new DummyConfigurationAdmin();
		cm.getOrCreateEmptyConfiguration(EssPower.SINGLETON_SERVICE_PID);

		var sut = new ComponentTest(power) //
				.addReference("cm", cm) //
				.addReference("addEss", cluster0) //
				.addReference("addEss", ess1) //
				.addReference("addEss", ess2) //
				.addReference("addEss", ess3) //
				.activate(MyConfig.create() //
						.setStrategy(BALANCE) //
						.setSymmetricMode(true) //
						.setDebugMode(false) //
						.setEnablePid(false) //
						.build()); //

		sut.next(new TestCase() //
				.onBeforeWriteCallbacks(() -> {
					power.addConstraint(power.createSimpleConstraint("", cluster0, ALL, ACTIVE, EQUALS, 10000));
					power.addConstraint(power.createSimpleConstraint("", ess3, ALL, ACTIVE, EQUALS, -5000));
				}));

		assertEquals(5000, p1.get());
		assertEquals(5000, p2.get());
		assertEquals(-5000, p3.get());
	}

	/**
	 * ess0 = [ess1, ess2, ess3, ess4], all ess [-10000..+10000] W. ess0 EQUALS
	 * -50000W — exceeds total capacity of -40000W, capped, each gets -10000W.
	 */
	@Test
	public void testClusterOverCapacityCharge() throws Exception {
		var p1 = new AtomicInteger();
		var p2 = new AtomicInteger();
		var p3 = new AtomicInteger();
		var p4 = new AtomicInteger();

		final var power = new EssPowerImpl();
		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.setPower(power) //
				.withSoc(50) //
				.withAllowedChargePower(-10000) //
				.withAllowedDischargePower(10000) //
				.withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p1.set(r.activePower()));
		var ess2 = new DummyManagedSymmetricEss("ess2") //
				.setPower(power) //
				.withSoc(50) //
				.withAllowedChargePower(-10000) //
				.withAllowedDischargePower(10000) //
				.withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p2.set(r.activePower()));
		var ess3 = new DummyManagedSymmetricEss("ess3") //
				.setPower(power) //
				.withSoc(50) //
				.withAllowedChargePower(-10000) //
				.withAllowedDischargePower(10000) //
				.withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p3.set(r.activePower()));
		var ess4 = new DummyManagedSymmetricEss("ess4") //
				.setPower(power) //
				.withSoc(50) //
				.withAllowedChargePower(-10000) //
				.withAllowedDischargePower(10000) //
				.withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p4.set(r.activePower()));
		var ess0 = new DummyMetaEss("ess0", ess1, ess2, ess3, ess4);

		final var cm = new DummyConfigurationAdmin();
		cm.getOrCreateEmptyConfiguration(EssPower.SINGLETON_SERVICE_PID);

		var sut = new ComponentTest(power) //
				.addReference("cm", cm) //
				.addReference("addEss", ess0) //
				.addReference("addEss", ess1) //
				.addReference("addEss", ess2) //
				.addReference("addEss", ess3) //
				.addReference("addEss", ess4) //
				.activate(MyConfig.create() //
						.setStrategy(BALANCE) //
						.setSymmetricMode(true) //
						.setDebugMode(false) //
						.setEnablePid(false) //
						.build());//
		sut.next(new TestCase() //
				.onBeforeWriteCallbacks(() -> {
					power.addConstraint(power.createSimpleConstraint("", ess0, ALL, ACTIVE, EQUALS, -50000));
				}));

		assertEquals(-10000, p1.get());
		assertEquals(-10000, p2.get());
		assertEquals(-10000, p3.get());
		assertEquals(-10000, p4.get());
	}

	/**
	 * ess0 = [ess1, ess2, ess3, ess4], all [-10000..+10000] W. ess0 EQUALS -10000W,
	 * ess1/ess2/ess3 GREATER_OR_EQUALS -2000W (charge limited). Solver distributes
	 * proportionally by charge capacity.
	 */
	@Test
	public void testClusterWithPerEssChargeLimits() throws Exception {
		var p1 = new AtomicInteger();
		var p2 = new AtomicInteger();
		var p3 = new AtomicInteger();
		var p4 = new AtomicInteger();

		final var power = new EssPowerImpl();
		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.setPower(power) //
				.withSoc(50) //
				.withAllowedChargePower(-10000) //
				.withAllowedDischargePower(10000) //
				.withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p1.set(r.activePower()));
		var ess2 = new DummyManagedSymmetricEss("ess2") //
				.setPower(power) //
				.withSoc(50) //
				.withAllowedChargePower(-10000) //
				.withAllowedDischargePower(10000) //
				.withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p2.set(r.activePower()));
		var ess3 = new DummyManagedSymmetricEss("ess3") //
				.setPower(power) //
				.withSoc(50) //
				.withAllowedChargePower(-10000) //
				.withAllowedDischargePower(10000) //
				.withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p3.set(r.activePower()));
		var ess4 = new DummyManagedSymmetricEss("ess4") //
				.setPower(power) //
				.withSoc(50) //
				.withAllowedChargePower(-10000) //
				.withAllowedDischargePower(10000) //
				.withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p4.set(r.activePower()));
		var ess0 = new DummyMetaEss("ess0", ess1, ess2, ess3, ess4);

		final var cm = new DummyConfigurationAdmin();
		cm.getOrCreateEmptyConfiguration(EssPower.SINGLETON_SERVICE_PID);

		var sut = new ComponentTest(power) //
				.addReference("cm", cm) //
				.addReference("addEss", ess0) //
				.addReference("addEss", ess1) //
				.addReference("addEss", ess2) //
				.addReference("addEss", ess3) //
				.addReference("addEss", ess4) //
				.activate(MyConfig.create() //
						.setStrategy(BALANCE) //
						.setSymmetricMode(true) //
						.setDebugMode(false) //
						.setEnablePid(false) //
						.build()); //
		sut.next(new TestCase() //
				.onBeforeWriteCallbacks(() -> {
					power.addConstraint(power.createSimpleConstraint("", ess0, ALL, ACTIVE, EQUALS, -10000));
					power.addConstraint(power.createSimpleConstraint("", ess1, ALL, ACTIVE, GREATER_OR_EQUALS, -2000));
					power.addConstraint(power.createSimpleConstraint("", ess2, ALL, ACTIVE, GREATER_OR_EQUALS, -2000));
					power.addConstraint(power.createSimpleConstraint("", ess3, ALL, ACTIVE, GREATER_OR_EQUALS, -2000));
				}));

		// totalCapacity=16000; ess1/2/3: -10000*2000/16000=-1250; ess4 gets remainder
		assertEquals(-1250, p1.get());
		assertEquals(-1250, p2.get());
		assertEquals(-1250, p3.get());
		assertEquals(-6250, p4.get());
	}

	/**
	 * ess0 = [ess1, ess2, ess3, ess4], all [-10000..+10000] W. ess0 EQUALS -10000W,
	 * ess1/ess2/ess3 GREATER_OR_EQUALS +2000W (force discharge — fully charged).
	 * Infeasible: solver does best it can, net = -4000W.
	 */
	@Test
	public void testClusterWithForceDischargeConflict() throws Exception {
		var p1 = new AtomicInteger();
		var p2 = new AtomicInteger();
		var p3 = new AtomicInteger();
		var p4 = new AtomicInteger();

		final var power = new EssPowerImpl();
		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.setPower(power) //
				.withSoc(50) //
				.withAllowedChargePower(-10000) //
				.withAllowedDischargePower(10000) //
				.withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p1.set(r.activePower()));
		var ess2 = new DummyManagedSymmetricEss("ess2") //
				.setPower(power) //
				.withSoc(50) //
				.withAllowedChargePower(-10000) //
				.withAllowedDischargePower(10000) //
				.withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p2.set(r.activePower()));
		var ess3 = new DummyManagedSymmetricEss("ess3") //
				.setPower(power) //
				.withSoc(50) //
				.withAllowedChargePower(-10000) //
				.withAllowedDischargePower(10000) //
				.withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p3.set(r.activePower()));
		var ess4 = new DummyManagedSymmetricEss("ess4") //
				.setPower(power) //
				.withSoc(50) //
				.withAllowedChargePower(-10000) //
				.withAllowedDischargePower(10000) //
				.withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p4.set(r.activePower()));
		var ess0 = new DummyMetaEss("ess0", ess1, ess2, ess3, ess4);

		final var cm = new DummyConfigurationAdmin();
		cm.getOrCreateEmptyConfiguration(EssPower.SINGLETON_SERVICE_PID);

		var sut = new ComponentTest(power) //
				.addReference("cm", cm) //
				.addReference("addEss", ess0) //
				.addReference("addEss", ess1) //
				.addReference("addEss", ess2) //
				.addReference("addEss", ess3) //
				.addReference("addEss", ess4) //
				.activate(MyConfig.create() //
						.setStrategy(BALANCE) //
						.setSymmetricMode(true) //
						.setDebugMode(false) //
						.setEnablePid(false) //
						.build()); //
		sut.next(new TestCase() //
				.onBeforeWriteCallbacks(() -> {
					power.addConstraint(power.createSimpleConstraint("", ess0, ALL, ACTIVE, EQUALS, -10000));
					power.addConstraint(power.createSimpleConstraint("", ess1, ALL, ACTIVE, GREATER_OR_EQUALS, 2000));
					power.addConstraint(power.createSimpleConstraint("", ess2, ALL, ACTIVE, GREATER_OR_EQUALS, 2000));
					power.addConstraint(power.createSimpleConstraint("", ess3, ALL, ACTIVE, GREATER_OR_EQUALS, 2000));
				}));

		// ess1/2/3 clamped to +2000; ess4 at max charge -10000 → net = -4000W
		assertEquals(2000, p1.get());
		assertEquals(2000, p2.get());
		assertEquals(2000, p3.get());
		assertEquals(-10000, p4.get());
	}
}
