package io.openems.edge.ess.core.power.v2;

import static io.openems.edge.common.type.Phase.SingleOrAllPhase.ALL;
import static io.openems.edge.ess.power.api.Pwr.ACTIVE;
import static io.openems.edge.ess.power.api.Pwr.REACTIVE;
import static io.openems.edge.ess.power.api.Relationship.EQUALS;
import static io.openems.edge.ess.power.api.Relationship.GREATER_OR_EQUALS;
import static io.openems.edge.ess.power.api.Relationship.LESS_OR_EQUALS;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.ess.core.power.EssPowerImpl;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyMetaEss;

public class PowerDistributionHandlerV2Test {

	// ========== Single ESS ==========

	@Test
	public void testEqualsConstraint() throws Exception {
		var p0 = new AtomicInteger();
		final var power = new EssPowerImpl();
		var ess0 = new DummyManagedSymmetricEss("ess0") //
				.setPower(power) //
				.withAllowedChargePower(-50000).withAllowedDischargePower(50000).withMaxApparentPower(50000) //
				.withSymmetricApplyPowerCallback(r -> p0.set(r.activePower()));

		var sut = V2TestUtils.createComponentTest(power, ess0);
		sut.next(new TestCase() //
				.onBeforeWriteCallbacks(() -> {
					power.addConstraint(power.createSimpleConstraint("Set 10kW", ess0, ALL, ACTIVE, EQUALS, 10000));
				}));

		assertEquals(10000, p0.get());
	}

	@Test
	public void testUpperBoundConstraint() throws Exception {
		var p0 = new AtomicInteger();
		final var power = new EssPowerImpl();
		var ess0 = new DummyManagedSymmetricEss("ess0") //
				.setPower(power) //
				.withAllowedChargePower(-50000).withAllowedDischargePower(50000).withMaxApparentPower(50000) //
				.withSymmetricApplyPowerCallback(r -> p0.set(r.activePower()));

		var sut = V2TestUtils.createComponentTest(power, ess0);
		sut.next(new TestCase() //
				.onBeforeWriteCallbacks(() -> {
					power.addConstraint(
							power.createSimpleConstraint("Max 5kW", ess0, ALL, ACTIVE, LESS_OR_EQUALS, 5000));
				}));

		assertEquals(0, p0.get());
	}

	@Test
	public void testLowerBoundConstraint() throws Exception {
		var p0 = new AtomicInteger();
		final var power = new EssPowerImpl();
		var ess0 = new DummyManagedSymmetricEss("ess0") //
				.setPower(power) //
				.withAllowedChargePower(-50000).withAllowedDischargePower(50000).withMaxApparentPower(50000) //
				.withSymmetricApplyPowerCallback(r -> p0.set(r.activePower()));

		var sut = V2TestUtils.createComponentTest(power, ess0);
		sut.next(new TestCase() //
				.onBeforeWriteCallbacks(() -> {
					power.addConstraint(
							power.createSimpleConstraint("Min 3kW", ess0, ALL, ACTIVE, GREATER_OR_EQUALS, 3000));
				}));

		assertEquals(3000, p0.get());
	}

	@Test
	public void testBoundsClampToZero() throws Exception {
		var p0 = new AtomicInteger();
		final var power = new EssPowerImpl();
		var ess0 = new DummyManagedSymmetricEss("ess0") //
				.setPower(power) //
				.withAllowedChargePower(-50000).withAllowedDischargePower(50000).withMaxApparentPower(50000) //
				.withSymmetricApplyPowerCallback(r -> p0.set(r.activePower()));

		var sut = V2TestUtils.createComponentTest(power, ess0);
		sut.next(new TestCase() //
				.onBeforeWriteCallbacks(() -> {
					power.addConstraint(
							power.createSimpleConstraint("Min -5kW", ess0, ALL, ACTIVE, GREATER_OR_EQUALS, -5000));
					power.addConstraint(
							power.createSimpleConstraint("Max 5kW", ess0, ALL, ACTIVE, LESS_OR_EQUALS, 5000));
				}));

		assertEquals(0, p0.get());
	}

	@Test
	public void testClampToEssLimits() throws Exception {
		var p0 = new AtomicInteger();
		final var power = new EssPowerImpl();
		var ess0 = new DummyManagedSymmetricEss("ess0") //
				.setPower(power) //
				.withAllowedChargePower(-50000).withAllowedDischargePower(50000).withMaxApparentPower(50000) //
				.withSymmetricApplyPowerCallback(r -> p0.set(r.activePower()));

		var sut = V2TestUtils.createComponentTest(power, ess0);
		sut.next(new TestCase() //
				.onBeforeWriteCallbacks(() -> {
					power.addConstraint(power.createSimpleConstraint("Set 100kW", ess0, ALL, ACTIVE, EQUALS, 100000));
				}));

		assertEquals(50000, p0.get());
	}

	@Test
	public void testClampToChargeLimits() throws Exception {
		var p0 = new AtomicInteger();
		final var power = new EssPowerImpl();
		var ess0 = new DummyManagedSymmetricEss("ess0") //
				.setPower(power) //
				.withAllowedChargePower(-50000).withAllowedDischargePower(50000).withMaxApparentPower(50000) //
				.withSymmetricApplyPowerCallback(r -> p0.set(r.activePower()));

		var sut = V2TestUtils.createComponentTest(power, ess0);
		sut.next(new TestCase() //
				.onBeforeWriteCallbacks(() -> {
					power.addConstraint(power.createSimpleConstraint("Set -100kW", ess0, ALL, ACTIVE, EQUALS, -100000));
				}));

		assertEquals(-50000, p0.get());
	}

	@Test
	public void testNoConstraints() throws Exception {
		var p0 = new AtomicInteger();
		final var power = new EssPowerImpl();
		var ess0 = new DummyManagedSymmetricEss("ess0") //
				.setPower(power) //
				.withAllowedChargePower(-50000).withAllowedDischargePower(50000).withMaxApparentPower(50000) //
				.withSymmetricApplyPowerCallback(r -> p0.set(r.activePower()));

		var sut = V2TestUtils.createComponentTest(power, ess0);
		sut.next(new TestCase());

		assertEquals(0, p0.get());
	}

	@Test
	public void testConstraintsClearedAfterCycle() throws Exception {
		var p0 = new AtomicInteger();
		final var power = new EssPowerImpl();
		var ess0 = new DummyManagedSymmetricEss("ess0") //
				.setPower(power) //
				.withAllowedChargePower(-50000).withAllowedDischargePower(50000).withMaxApparentPower(50000) //
				.withSymmetricApplyPowerCallback(r -> p0.set(r.activePower()));

		var sut = V2TestUtils.createComponentTest(power, ess0);
		sut.next(new TestCase() //
				.onBeforeWriteCallbacks(() -> {
					power.addConstraint(power.createSimpleConstraint("Set 10kW", ess0, ALL, ACTIVE, EQUALS, 10000));
				}));
		assertEquals(10000, p0.get());

		sut.next(new TestCase()); // second cycle — no constraints → back to 0W
		assertEquals(0, p0.get());
	}

	@Test
	public void testMultipleEss() throws Exception {
		var p0 = new AtomicInteger();
		var p1 = new AtomicInteger();
		final var power = new EssPowerImpl();
		var ess0 = new DummyManagedSymmetricEss("ess0") //
				.setPower(power) //
				.withAllowedChargePower(-50000).withAllowedDischargePower(50000).withMaxApparentPower(50000) //
				.withSymmetricApplyPowerCallback(r -> p0.set(r.activePower()));
		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.setPower(power) //
				.withAllowedChargePower(-30000).withAllowedDischargePower(30000).withMaxApparentPower(30000) //
				.withSymmetricApplyPowerCallback(r -> p1.set(r.activePower()));

		var sut = V2TestUtils.createComponentTest(power, ess0, ess1);
		sut.next(new TestCase() //
				.onBeforeWriteCallbacks(() -> {
					power.addConstraint(power.createSimpleConstraint("ess0: 20kW", ess0, ALL, ACTIVE, EQUALS, 20000));
					power.addConstraint(power.createSimpleConstraint("ess1: 15kW", ess1, ALL, ACTIVE, EQUALS, 15000));
				}));

		assertEquals(20000, p0.get());
		assertEquals(15000, p1.get());
	}

	@Test
	public void testTightestUpperBoundWins() throws Exception {
		var p0 = new AtomicInteger();
		final var power = new EssPowerImpl();
		var ess0 = new DummyManagedSymmetricEss("ess0") //
				.setPower(power) //
				.withAllowedChargePower(-50000).withAllowedDischargePower(50000).withMaxApparentPower(50000) //
				.withSymmetricApplyPowerCallback(r -> p0.set(r.activePower()));

		var sut = V2TestUtils.createComponentTest(power, ess0);
		sut.next(new TestCase() //
				.onBeforeWriteCallbacks(() -> {
					power.addConstraint(
							power.createSimpleConstraint("Max 10kW", ess0, ALL, ACTIVE, LESS_OR_EQUALS, 10000));
					power.addConstraint(
							power.createSimpleConstraint("Max 5kW", ess0, ALL, ACTIVE, LESS_OR_EQUALS, 5000));
					power.addConstraint(
							power.createSimpleConstraint("Min 3kW", ess0, ALL, ACTIVE, GREATER_OR_EQUALS, 3000));
				}));

		assertEquals(3000, p0.get());
	}

	@Test
	public void testReactivePower() throws Exception {
		var p0 = new AtomicInteger();
		var q0 = new AtomicInteger();
		final var power = new EssPowerImpl();
		var ess0 = new DummyManagedSymmetricEss("ess0") //
				.setPower(power) //
				.withAllowedChargePower(-50000).withAllowedDischargePower(50000).withMaxApparentPower(50000) //
				.withSymmetricApplyPowerCallback(r -> {
					p0.set(r.activePower());
					q0.set(r.reactivePower());
				});

		var sut = V2TestUtils.createComponentTest(power, ess0);
		sut.next(new TestCase() //
				.onBeforeWriteCallbacks(() -> {
					power.addConstraint(power.createSimpleConstraint("P=10kW", ess0, ALL, ACTIVE, EQUALS, 10000));
					power.addConstraint(power.createSimpleConstraint("Q=2kvar", ess0, ALL, REACTIVE, EQUALS, 2000));
				}));

		assertEquals(10000, p0.get());
		assertEquals(2000, q0.get());
	}

	// ========== Cluster ==========

	@Test
	public void testClusterSingleMember() throws Exception {
		var p1 = new AtomicInteger();
		final var power = new EssPowerImpl();
		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.setPower(power).withSoc(50) //
				.withAllowedChargePower(-10000).withAllowedDischargePower(10000).withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p1.set(r.activePower()));
		var cluster0 = new DummyMetaEss("cluster0", ess1);

		var sut = V2TestUtils.createComponentTest(power, cluster0, ess1);
		sut.next(new TestCase() //
				.onBeforeWriteCallbacks(() -> {
					power.addConstraint(
							power.createSimpleConstraint("Cluster 5kW", cluster0, ALL, ACTIVE, EQUALS, 5000));
				}));

		assertEquals("ess1 single member", 5000, p1.get());
	}

	@Test
	public void testClusterSingleMember_NoConstraint() throws Exception {
		var p1 = new AtomicInteger();
		final var power = new EssPowerImpl();
		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.setPower(power).withSoc(50) //
				.withAllowedChargePower(-10000).withAllowedDischargePower(10000).withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p1.set(r.activePower()));
		var cluster0 = new DummyMetaEss("cluster0", ess1);

		var sut = V2TestUtils.createComponentTest(power, cluster0, ess1);
		sut.next(new TestCase());

		assertEquals("ess1 no power", 0, p1.get());
	}

	@Test
	public void testNoConstraint_AllZero() throws Exception {
		var p1 = new AtomicInteger();
		var p2 = new AtomicInteger();
		var p3 = new AtomicInteger();
		final var power = new EssPowerImpl();
		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.setPower(power).withSoc(20) //
				.withAllowedChargePower(-10000).withAllowedDischargePower(10000).withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p1.set(r.activePower()));
		var ess2 = new DummyManagedSymmetricEss("ess2") //
				.setPower(power).withSoc(50) //
				.withAllowedChargePower(-10000).withAllowedDischargePower(10000).withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p2.set(r.activePower()));
		var ess3 = new DummyManagedSymmetricEss("ess3") //
				.setPower(power).withSoc(80) //
				.withAllowedChargePower(-10000).withAllowedDischargePower(10000).withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p3.set(r.activePower()));
		var cluster0 = new DummyMetaEss("cluster0", ess1, ess2, ess3);

		var sut = V2TestUtils.createComponentTest(power, cluster0, ess1, ess2, ess3);
		sut.next(new TestCase());

		assertEquals("ess1", 0, p1.get());
		assertEquals("ess2", 0, p2.get());
		assertEquals("ess3", 0, p3.get());
	}

	@Test
	public void testDischarge_UnequalCapacities() throws Exception {
		var p1 = new AtomicInteger();
		var p2 = new AtomicInteger();
		final var power = new EssPowerImpl();
		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.setPower(power).withSoc(50) //
				.withAllowedChargePower(-10000).withAllowedDischargePower(5000).withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p1.set(r.activePower()));
		var ess2 = new DummyManagedSymmetricEss("ess2") //
				.setPower(power).withSoc(50) //
				.withAllowedChargePower(-10000).withAllowedDischargePower(15000).withMaxApparentPower(15000) //
				.withSymmetricApplyPowerCallback(r -> p2.set(r.activePower()));
		var cluster0 = new DummyMetaEss("cluster0", ess1, ess2);

		var sut = V2TestUtils.createComponentTest(power, cluster0, ess1, ess2);
		sut.next(new TestCase() //
				.onBeforeWriteCallbacks(() -> {
					power.addConstraint(
							power.createSimpleConstraint("Cluster 20kW", cluster0, ALL, ACTIVE, EQUALS, 20000));
				}));

		assertEquals("Total", 20000, p1.get() + p2.get());
		assertEquals("ess1", 5000, p1.get());
		assertEquals("ess2", 15000, p2.get());
	}

	/**
	 * Cluster requests 20kW but total discharge capacity is only 10kW — each ESS
	 * delivers its maximum.
	 */
	@Test
	public void testDischarge_PartialCapacity() throws Exception {
		var p1 = new AtomicInteger();
		var p2 = new AtomicInteger();
		final var power = new EssPowerImpl();
		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.setPower(power).withSoc(50) //
				.withAllowedChargePower(-10000).withAllowedDischargePower(5000).withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p1.set(r.activePower()));
		var ess2 = new DummyManagedSymmetricEss("ess2") //
				.setPower(power).withSoc(50) //
				.withAllowedChargePower(-10000).withAllowedDischargePower(5000).withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p2.set(r.activePower()));
		var cluster0 = new DummyMetaEss("cluster0", ess1, ess2);

		var sut = V2TestUtils.createComponentTest(power, cluster0, ess1, ess2);
		sut.next(new TestCase() //
				.onBeforeWriteCallbacks(() -> {
					power.addConstraint(
							power.createSimpleConstraint("Cluster 20kW", cluster0, ALL, ACTIVE, EQUALS, 20000));
				}));

		assertEquals("Total clamped to 10kW", 10000, p1.get() + p2.get());
		assertEquals("ess1", 5000, p1.get());
		assertEquals("ess2", 5000, p2.get());
	}

	// ========== Nested Cluster ==========

	@Test
	public void testNestedCluster() throws Exception {
		var p1 = new AtomicInteger();
		var p2 = new AtomicInteger();
		var p3 = new AtomicInteger();
		var p4 = new AtomicInteger();

		final var power = new EssPowerImpl();
		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.setPower(power).withSoc(50) //
				.withAllowedChargePower(-10000).withAllowedDischargePower(10000).withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p1.set(r.activePower()));
		var ess2 = new DummyManagedSymmetricEss("ess2") //
				.setPower(power).withSoc(50) //
				.withAllowedChargePower(-10000).withAllowedDischargePower(10000).withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p2.set(r.activePower()));
		var ess3 = new DummyManagedSymmetricEss("ess3") //
				.setPower(power).withSoc(50) //
				.withAllowedChargePower(-10000).withAllowedDischargePower(10000).withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p3.set(r.activePower()));
		var ess4 = new DummyManagedSymmetricEss("ess4") //
				.setPower(power).withSoc(50) //
				.withAllowedChargePower(-10000).withAllowedDischargePower(10000).withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p4.set(r.activePower()));

		var cluster1 = new DummyMetaEss("cluster1", ess1, ess2);
		var cluster2 = new DummyMetaEss("cluster2", ess3, ess4);
		var cluster0 = new DummyMetaEss("cluster0", cluster1, cluster2);

		var sut = V2TestUtils.createComponentTest(power, cluster0, cluster1, cluster2, ess1, ess2, ess3, ess4);
		sut.next(new TestCase() //
				.onBeforeWriteCallbacks(() -> {
					power.addConstraint(
							power.createSimpleConstraint("Cluster0 20kW", cluster0, ALL, ACTIVE, EQUALS, 20000));
				}));

		assertEquals("ess1", 5000, p1.get());
		assertEquals("ess2", 5000, p2.get());
		assertEquals("ess3", 5000, p3.get());
		assertEquals("ess4", 5000, p4.get());
	}

	@Test
	public void testNestedClusterDifferentLimits() throws Exception {
		var p1 = new AtomicInteger();
		var p2 = new AtomicInteger();
		var p3 = new AtomicInteger();
		var p4 = new AtomicInteger();

		final var power = new EssPowerImpl();
		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.setPower(power).withSoc(50) //
				.withAllowedChargePower(-10000).withAllowedDischargePower(10000).withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p1.set(r.activePower()));
		var ess2 = new DummyManagedSymmetricEss("ess2") //
				.setPower(power).withSoc(50) //
				.withAllowedChargePower(-10000).withAllowedDischargePower(10000).withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p2.set(r.activePower()));
		var ess3 = new DummyManagedSymmetricEss("ess3") //
				.setPower(power).withSoc(50) //
				.withAllowedChargePower(-10000).withAllowedDischargePower(10000).withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p3.set(r.activePower()));
		var ess4 = new DummyManagedSymmetricEss("ess4") //
				.setPower(power).withSoc(50) //
				.withAllowedChargePower(-10000).withAllowedDischargePower(10000).withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p4.set(r.activePower()));

		var cluster1 = new DummyMetaEss("cluster1", ess1, ess2);
		var cluster2 = new DummyMetaEss("cluster2", ess3, ess4);
		var cluster0 = new DummyMetaEss("cluster0", cluster1, cluster2);

		var sut = V2TestUtils.createComponentTest(power, cluster0, cluster1, cluster2, ess1, ess2, ess3, ess4);

		// Cycle 1: bounded range with EQUALS -10000 → each gets -2500W
		sut.next(new TestCase() //
				.onBeforeWriteCallbacks(() -> {
					power.addConstraint(
							power.createSimpleConstraint("GEQ", cluster0, ALL, ACTIVE, GREATER_OR_EQUALS, -20000));
					power.addConstraint(power.createSimpleConstraint("EQ", cluster0, ALL, ACTIVE, EQUALS, -10000));
					power.addConstraint(
							power.createSimpleConstraint("LEQ", cluster0, ALL, ACTIVE, LESS_OR_EQUALS, 35000));
				}));

		assertEquals("ess1", -2500, p1.get());
		assertEquals("ess2", -2500, p2.get());
		assertEquals("ess3", -2500, p3.get());
		assertEquals("ess4", -2500, p4.get());

		// Cycle 2: only bounds, no EQUALS → solver picks 0W
		sut.next(new TestCase() //
				.onBeforeWriteCallbacks(() -> {
					power.addConstraint(
							power.createSimpleConstraint("GEQ", cluster0, ALL, ACTIVE, GREATER_OR_EQUALS, -20000));
					power.addConstraint(
							power.createSimpleConstraint("LEQ", cluster0, ALL, ACTIVE, LESS_OR_EQUALS, 35000));
				}));

		assertEquals("ess1", 0, p1.get());
		assertEquals("ess2", 0, p2.get());
		assertEquals("ess3", 0, p3.get());
		assertEquals("ess4", 0, p4.get());
	}

	@Test
	public void testGetPowerExtrema_RespectsConstraint() throws Exception {
		var ess1 = new DummyManagedSymmetricEss("ess1")//
				.withAllowedDischargePower(10000)//
				.withAllowedChargePower(-10000)//
				.withMaxApparentPower(10000);
		ess1.withSymmetricApplyPowerCallback(r -> {
		});

		var handler = new PowerDistributionHandlerV2(() -> List.of(ess1), notSolved -> {
			/* ignored in tests */ });
		handler.onAfterProcessImage();

		// Add a constraint capping discharge to 3000 W
		handler.addConstraint(//
				handler.createSimpleConstraint("P<=3000", ess1, ALL, ACTIVE, LESS_OR_EQUALS, 3000));

		handler.addConstraint(
				handler.createSimpleConstraint("P<=3000", ess1, ALL, ACTIVE, Relationship.GREATER_OR_EQUALS, -3000));

		handler.addConstraint(
				handler.createSimpleConstraint("P>=4000", ess1, ALL, ACTIVE, Relationship.GREATER_OR_EQUALS, -2000));

		// New: should return 3000 (constraint respected)
		assertEquals(3000, handler.getPowerExtrema(ess1, ALL, ACTIVE, GoalType.MAXIMIZE));

		assertEquals(-2000, handler.getPowerExtrema(ess1, ALL, ACTIVE, GoalType.MINIMIZE));
	}

	/**
	 * ess3 has allowedDischargePower=-2000 (negative = BMS forcing charge). Even
	 * though the cluster target is +40kW discharge, ess3 must charge at -2000W.
	 * Total delivered = 10+10-2+10 = 28kW.
	 */
	@Test
	public void testEssClusterForceCharge() throws Exception {
		var p1 = new AtomicInteger();
		var p2 = new AtomicInteger();
		var p3 = new AtomicInteger();
		var p4 = new AtomicInteger();

		final var power = new EssPowerImpl();
		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.setPower(power)//
				.withSoc(10) //
				.withAllowedChargePower(-10000)//
				.withAllowedDischargePower(10000)//
				.withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p1.set(r.activePower()));
		var ess2 = new DummyManagedSymmetricEss("ess2") //
				.setPower(power)//
				.withSoc(30) //
				.withAllowedChargePower(-10000)//
				.withAllowedDischargePower(10000)//
				.withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p2.set(r.activePower()));
		var ess3 = new DummyManagedSymmetricEss("ess3") //
				.setPower(power)//
				.withSoc(70) //
				.withAllowedChargePower(-10000)//
				.withAllowedDischargePower(-2000) // BMS forcing charge
				.withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p3.set(r.activePower()));
		var ess4 = new DummyManagedSymmetricEss("ess4") //
				.setPower(power)//
				.withSoc(90) //
				.withAllowedChargePower(-10000)//
				.withAllowedDischargePower(10000)//
				.withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p4.set(r.activePower()));
		var cluster0 = new DummyMetaEss("cluster0", ess1, ess2, ess3, ess4);

		var sut = V2TestUtils.createComponentTest(power, cluster0, ess1, ess2, ess3, ess4);
		sut.next(new TestCase() //
				.onBeforeWriteCallbacks(() -> {
					power.addConstraint(
							power.createSimpleConstraint("Cluster 40kW", cluster0, ALL, ACTIVE, EQUALS, 40000));
				}));

		assertEquals("ess1", 10000, p1.get());
		assertEquals("ess2", 10000, p2.get());
		assertEquals("ess3 force charge", -2000, p3.get());
		assertEquals("ess4", 10000, p4.get());
		assertEquals("Total", 28000, p1.get() + p2.get() + p3.get() + p4.get());
	}

	/**
	 * ess3 has allowedChargePower=+2000 (positive = BMS forcing discharge). Even
	 * though the cluster target is -40kW charge, ess3 must discharge at +2000W.
	 * Total delivered = -10-10+2-10 = -28kW.
	 */
	@Test
	public void testEssClusterForceDischarge() throws Exception {
		var p1 = new AtomicInteger();
		var p2 = new AtomicInteger();
		var p3 = new AtomicInteger();
		var p4 = new AtomicInteger();

		final var power = new EssPowerImpl();
		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.setPower(power).withSoc(10) //
				.withAllowedChargePower(-10000).withAllowedDischargePower(10000).withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p1.set(r.activePower()));
		var ess2 = new DummyManagedSymmetricEss("ess2") //
				.setPower(power).withSoc(30) //
				.withAllowedChargePower(-10000).withAllowedDischargePower(10000).withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p2.set(r.activePower()));
		var ess3 = new DummyManagedSymmetricEss("ess3") //
				.setPower(power).withSoc(70) //
				.withAllowedChargePower(2000) // BMS forcing discharge
				.withAllowedDischargePower(10000).withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p3.set(r.activePower()));
		var ess4 = new DummyManagedSymmetricEss("ess4") //
				.setPower(power).withSoc(90) //
				.withAllowedChargePower(-10000).withAllowedDischargePower(10000).withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p4.set(r.activePower()));
		var cluster0 = new DummyMetaEss("cluster0", ess1, ess2, ess3, ess4);

		var sut = V2TestUtils.createComponentTest(power, cluster0, ess1, ess2, ess3, ess4);
		sut.next(new TestCase() //
				.onBeforeWriteCallbacks(() -> {
					power.addConstraint(
							power.createSimpleConstraint("Cluster -40kW", cluster0, ALL, ACTIVE, EQUALS, -40000));
				}));

		assertEquals("ess1", -10000, p1.get());
		assertEquals("ess2", -10000, p2.get());
		assertEquals("ess3 force discharge", 2000, p3.get());
		assertEquals("ess4", -10000, p4.get());
		assertEquals("Total", -28000, p1.get() + p2.get() + p3.get() + p4.get());
	}

	/**
	 * No cluster constraint. ess3 has allowedChargePower=+2000 (BMS forcing
	 * discharge) — it must still reach its minimum of +2000W. All others settle at
	 * 0W.
	 */
	@Test
	public void testEssClusterNoConstraints() throws Exception {
		var p1 = new AtomicInteger();
		var p2 = new AtomicInteger();
		var p3 = new AtomicInteger();
		var p4 = new AtomicInteger();

		final var power = new EssPowerImpl();
		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.setPower(power).withSoc(10) //
				.withAllowedChargePower(-10000).withAllowedDischargePower(10000).withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p1.set(r.activePower()));
		var ess2 = new DummyManagedSymmetricEss("ess2") //
				.setPower(power).withSoc(30) //
				.withAllowedChargePower(-10000).withAllowedDischargePower(10000).withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p2.set(r.activePower()));
		var ess3 = new DummyManagedSymmetricEss("ess3") //
				.setPower(power).withSoc(70) //
				.withAllowedChargePower(2000) // BMS forcing discharge
				.withAllowedDischargePower(10000).withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p3.set(r.activePower()));
		var ess4 = new DummyManagedSymmetricEss("ess4") //
				.setPower(power).withSoc(90) //
				.withAllowedChargePower(-10000).withAllowedDischargePower(10000).withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> p4.set(r.activePower()));
		var cluster0 = new DummyMetaEss("cluster0", ess1, ess2, ess3, ess4);

		var sut = V2TestUtils.createComponentTest(power, cluster0, ess1, ess2, ess3, ess4);
		sut.next(new TestCase());

		assertEquals("ess1", 0, p1.get());
		assertEquals("ess2", 0, p2.get());
		assertEquals("ess3 force discharge", 2000, p3.get());
		assertEquals("ess4", 0, p4.get());
		assertEquals("Total", 2000, p1.get() + p2.get() + p3.get() + p4.get());
	}
}
