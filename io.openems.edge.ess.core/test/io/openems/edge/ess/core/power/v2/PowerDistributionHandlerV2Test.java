package io.openems.edge.ess.core.power.v2;

import static io.openems.edge.common.type.Phase.SingleOrAllPhase.ALL;
import static io.openems.edge.ess.power.api.Pwr.ACTIVE;
import static io.openems.edge.ess.power.api.Pwr.REACTIVE;
import static io.openems.edge.ess.power.api.Relationship.EQUALS;
import static io.openems.edge.ess.power.api.Relationship.GREATER_OR_EQUALS;
import static io.openems.edge.ess.power.api.Relationship.LESS_OR_EQUALS;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyMetaEss;

public class PowerDistributionHandlerV2Test {

	private static AtomicInteger openCallbacks;

	private List<ManagedSymmetricEss> esss;
	private PowerDistributionHandlerV2 handler;

	private DummyManagedSymmetricEss ess0;

	@Before
	public void before() {
		openCallbacks = new AtomicInteger(0);

		this.ess0 = new DummyManagedSymmetricEss("ess0") //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(50000);

		this.esss = new ArrayList<>();
		this.esss.add(this.ess0);

		this.handler = new PowerDistributionHandlerV2(() -> this.esss);
		this.handler.onAfterProcessImage();
	}

	@After
	public void after() {
		assertEquals("Not all Callbacks were actually called", 0, openCallbacks.get());
	}

	private static void expect(String description, DummyManagedSymmetricEss ess, int p, int q) {
		openCallbacks.incrementAndGet();
		ess.withSymmetricApplyPowerCallback(record -> {
			openCallbacks.decrementAndGet();
			assertEquals(description + " P for " + ess.id(), p, record.activePower());
			assertEquals(description + " Q for " + ess.id(), q, record.reactivePower());
		});
	}

	// ========== Single ESS ==========

	@Test
	public void testEqualsConstraint() throws Exception {
		expect("#1", this.ess0, 10000, 0);

		this.handler.addConstraint(//
				this.handler.createSimpleConstraint("Set 10kW", this.ess0, ALL, ACTIVE, EQUALS, 10000));
		this.handler.onBeforeWriteEvent();
	}

	@Test
	public void testUpperBoundConstraint() throws Exception {
		expect("#1", this.ess0, 0, 0);

		this.handler.addConstraint(//
				this.handler.createSimpleConstraint("Max 5kW", this.ess0, ALL, ACTIVE, LESS_OR_EQUALS, 5000));
		this.handler.onBeforeWriteEvent();
	}

	@Test
	public void testLowerBoundConstraint() throws Exception {
		expect("#1", this.ess0, 3000, 0);

		this.handler.addConstraint(//
				this.handler.createSimpleConstraint("Min 3kW", this.ess0, ALL, ACTIVE, GREATER_OR_EQUALS, 3000));
		this.handler.onBeforeWriteEvent();
	}

	@Test
	public void testBoundsClampToZero() throws Exception {
		expect("#1", this.ess0, 0, 0);

		this.handler.addConstraint(//
				this.handler.createSimpleConstraint("Min -5kW", this.ess0, ALL, ACTIVE, GREATER_OR_EQUALS, -5000));
		this.handler.addConstraint(//
				this.handler.createSimpleConstraint("Max 5kW", this.ess0, ALL, ACTIVE, LESS_OR_EQUALS, 5000));
		this.handler.onBeforeWriteEvent();
	}

	@Test
	public void testClampToEssLimits() throws Exception {
		expect("#1", this.ess0, 50000, 0);

		this.handler.addConstraint(//
				this.handler.createSimpleConstraint("Set 100kW", this.ess0, ALL, ACTIVE, EQUALS, 100000));
		this.handler.onBeforeWriteEvent();
	}

	@Test
	public void testClampToChargeLimits() throws Exception {
		expect("#1", this.ess0, -50000, 0);

		this.handler.addConstraint(//
				this.handler.createSimpleConstraint("Set -100kW", this.ess0, ALL, ACTIVE, EQUALS, -100000));
		this.handler.onBeforeWriteEvent();
	}

	@Test
	public void testNoConstraints() throws Exception {
		expect("#1", this.ess0, 0, 0);

		this.handler.onBeforeWriteEvent();
	}

	@Test
	public void testConstraintsClearedAfterCycle() throws Exception {
		expect("#1", this.ess0, 10000, 0);
		this.handler.addConstraint(//
				this.handler.createSimpleConstraint("Set 10kW", this.ess0, ALL, ACTIVE, EQUALS, 10000));
		this.handler.onBeforeWriteEvent();

		this.handler.onAfterWriteEvent();
		this.handler.onAfterProcessImage();

		expect("#2", this.ess0, 0, 0);
		this.handler.onBeforeWriteEvent();
	}

	@Test
	public void testMultipleEss() throws Exception {
		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.withAllowedChargePower(-30000) //
				.withAllowedDischargePower(30000) //
				.withMaxApparentPower(30000);

		this.esss.add(ess1);
		this.handler.onUpdateEsss();
		this.handler.onAfterProcessImage();

		expect("#1", this.ess0, 20000, 0);
		expect("#1", ess1, 15000, 0);

		this.handler.addConstraint(//
				this.handler.createSimpleConstraint("ess0: 20kW", this.ess0, ALL, ACTIVE, EQUALS, 20000));
		this.handler.addConstraint(//
				this.handler.createSimpleConstraint("ess1: 15kW", ess1, ALL, ACTIVE, EQUALS, 15000));
		this.handler.onBeforeWriteEvent();
	}

	@Test
	public void testTightestUpperBoundWins() throws Exception {
		expect("#1", this.ess0, 3000, 0);

		this.handler.addConstraint(//
				this.handler.createSimpleConstraint("Max 10kW", this.ess0, ALL, ACTIVE, LESS_OR_EQUALS, 10000));
		this.handler.addConstraint(//
				this.handler.createSimpleConstraint("Max 5kW", this.ess0, ALL, ACTIVE, LESS_OR_EQUALS, 5000));
		this.handler.addConstraint(//
				this.handler.createSimpleConstraint("Min 3kW", this.ess0, ALL, ACTIVE, GREATER_OR_EQUALS, 3000));
		this.handler.onBeforeWriteEvent();
	}

	@Test
	public void testReactivePower() throws Exception {
		expect("#1", this.ess0, 10000, 2000);

		this.handler.addConstraint(//
				this.handler.createSimpleConstraint("P=10kW", this.ess0, ALL, ACTIVE, EQUALS, 10000));
		this.handler.addConstraint(//
				this.handler.createSimpleConstraint("Q=2kvar", this.ess0, ALL, REACTIVE, EQUALS, 2000));
		this.handler.onBeforeWriteEvent();
	}

	// ========== Cluster ==========

	@Test
	public void testClusterSingleMember() throws Exception {
		var s = V2TestUtils.createCluster("cluster0", new int[][] { //
				{ 50, -10000, 10000 }, //
		});

		var handler = V2TestUtils.createHandler(s);
		handler.onAfterProcessImage();
		handler.addConstraint(//
				handler.createSimpleConstraint("Cluster 5kW", s.cluster(), ALL, ACTIVE, EQUALS, 5000));
		handler.onBeforeWriteEvent();

		assertEquals("ess1 single member", 5000, s.powerOf("ess1"));
	}

	@Test
	public void testClusterSingleMember_NoConstraint() throws Exception {
		var s = V2TestUtils.createCluster("cluster0", new int[][] { //
				{ 50, -10000, 10000 }, //
		});

		var handler = V2TestUtils.createHandler(s);
		handler.onAfterProcessImage();
		handler.onBeforeWriteEvent();

		assertEquals("ess1 no power", 0, s.powerOf("ess1"));
	}

	@Test
	public void testNoConstraint_AllZero() throws Exception {
		var s = V2TestUtils.createCluster("cluster0", new int[][] { //
				{ 20, -10000, 10000 }, //
				{ 50, -10000, 10000 }, //
				{ 80, -10000, 10000 }, //
		});

		var handler = V2TestUtils.createHandler(s);
		handler.onAfterProcessImage();
		handler.onBeforeWriteEvent();

		assertEquals("ess1", 0, s.powerOf("ess1"));
		assertEquals("ess2", 0, s.powerOf("ess2"));
		assertEquals("ess3", 0, s.powerOf("ess3"));
	}

	// ========== Standalone ESS + Cluster ==========

	@Test
	public void testStandaloneEssAndCluster() throws Exception {
		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.withSoc(50).withAllowedChargePower(-10000) //
				.withAllowedDischargePower(10000).withMaxApparentPower(10000);
		var ess2 = new DummyManagedSymmetricEss("ess2") //
				.withSoc(50).withAllowedChargePower(-10000) //
				.withAllowedDischargePower(10000).withMaxApparentPower(10000);
		var ess3 = new DummyManagedSymmetricEss("ess3") //
				.withSoc(50).withAllowedChargePower(-10000) //
				.withAllowedDischargePower(10000).withMaxApparentPower(10000);

		var cluster0 = new DummyMetaEss("cluster0", ess1, ess2);

		var allEsss = new ArrayList<ManagedSymmetricEss>();
		allEsss.add(cluster0);
		allEsss.add(ess1);
		allEsss.add(ess2);
		allEsss.add(ess3);

		var powers = new HashMap<String, AtomicInteger>();
		for (var ess : List.of(ess1, ess2, ess3)) {
			var p = new AtomicInteger(0);
			powers.put(ess.id(), p);
			ess.withSymmetricApplyPowerCallback(r -> p.set(r.activePower()));
		}

		var handler = new PowerDistributionHandlerV2(() -> allEsss);
		handler.onAfterProcessImage();

		handler.addConstraint(//
				handler.createSimpleConstraint("Cluster 10kW", cluster0, ALL, ACTIVE, EQUALS, 10000));
		handler.addConstraint(//
				handler.createSimpleConstraint("ess3 -5kW", ess3, ALL, ACTIVE, EQUALS, -5000));

		handler.onBeforeWriteEvent();

		assertEquals("ess1 discharge", 5000, powers.get("ess1").get());
		assertEquals("ess2 discharge", 5000, powers.get("ess2").get());
		assertEquals("ess3 charge", -5000, powers.get("ess3").get());
	}

	// ========== Nested Cluster ==========

	@Test
	public void testNestedCluster() throws Exception {
		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.withSoc(50).withAllowedChargePower(-10000) //
				.withAllowedDischargePower(10000).withMaxApparentPower(10000);
		var ess2 = new DummyManagedSymmetricEss("ess2") //
				.withSoc(50).withAllowedChargePower(-10000) //
				.withAllowedDischargePower(10000).withMaxApparentPower(10000);
		var ess3 = new DummyManagedSymmetricEss("ess3") //
				.withSoc(50).withAllowedChargePower(-10000) //
				.withAllowedDischargePower(10000).withMaxApparentPower(10000);
		var ess4 = new DummyManagedSymmetricEss("ess4") //
				.withSoc(50).withAllowedChargePower(-10000) //
				.withAllowedDischargePower(10000).withMaxApparentPower(10000);

		var cluster1 = new DummyMetaEss("cluster1", ess1, ess2);
		var cluster2 = new DummyMetaEss("cluster2", ess3, ess4);
		var cluster0 = new DummyMetaEss("cluster0", cluster1, cluster2);

		var allEsss = new ArrayList<ManagedSymmetricEss>();
		allEsss.add(cluster0);
		allEsss.add(cluster1);
		allEsss.add(cluster2);
		allEsss.add(ess1);
		allEsss.add(ess2);
		allEsss.add(ess3);
		allEsss.add(ess4);

		var powers = new HashMap<String, AtomicInteger>();
		for (var ess : List.of(ess1, ess2, ess3, ess4)) {
			var p = new AtomicInteger(0);
			powers.put(ess.id(), p);
			ess.withSymmetricApplyPowerCallback(r -> p.set(r.activePower()));
		}

		var handler = new PowerDistributionHandlerV2(() -> allEsss);
		handler.onAfterProcessImage();

		handler.addConstraint(//
				handler.createSimpleConstraint("Cluster0 20kW", cluster0, ALL, ACTIVE, EQUALS, 20000));

		handler.onBeforeWriteEvent();

		assertEquals("ess1", 5000, powers.get("ess1").get());
		assertEquals("ess2", 5000, powers.get("ess2").get());
		assertEquals("ess3", 5000, powers.get("ess3").get());
		assertEquals("ess4", 5000, powers.get("ess4").get());
	}
}
