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

public class PowerDistributionHandlerV2ReactivePowerTest {

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

	// ========== Standalone ESS ==========

	@Test
	public void testReactiveEquals() throws Exception {
		expect("#1", this.ess0, 10000, 2000);

		this.handler.addConstraint(//
				this.handler.createSimpleConstraint("P=10kW", this.ess0, ALL, ACTIVE, EQUALS, 10000));
		this.handler.addConstraint(//
				this.handler.createSimpleConstraint("Q=2kvar", this.ess0, ALL, REACTIVE, EQUALS, 2000));
		this.handler.onBeforeWriteEvent();
	}

	@Test
	public void testReactiveOnly_NoActiveConstraint() throws Exception {
		expect("#1", this.ess0, 0, 5000);

		this.handler.addConstraint(//
				this.handler.createSimpleConstraint("Q=5kvar", this.ess0, ALL, REACTIVE, EQUALS, 5000));
		this.handler.onBeforeWriteEvent();
	}

	@Test
	public void testNoReactiveConstraint_QStaysZero() throws Exception {
		expect("#1", this.ess0, 5000, 0);

		this.handler.addConstraint(//
				this.handler.createSimpleConstraint("P=5kW", this.ess0, ALL, ACTIVE, EQUALS, 5000));
		this.handler.onBeforeWriteEvent();
	}

	@Test
	public void testApparentPowerClamping() throws Exception {
		// S=50000, P=40000 -> qMax=30000, Q=35000 clamped to 30000
		expect("#1", this.ess0, 40000, 30000);

		this.handler.addConstraint(//
				this.handler.createSimpleConstraint("P=40kW", this.ess0, ALL, ACTIVE, EQUALS, 40000));
		this.handler.addConstraint(//
				this.handler.createSimpleConstraint("Q=35kvar", this.ess0, ALL, REACTIVE, EQUALS, 35000));
		this.handler.onBeforeWriteEvent();
	}

	@Test
	public void testApparentPowerClamping_NegativeReactive() throws Exception {
		// S=50000, P=30000 -> qMax=40000, Q=-45000 clamped to -40000
		expect("#1", this.ess0, 30000, -40000);

		this.handler.addConstraint(//
				this.handler.createSimpleConstraint("P=30kW", this.ess0, ALL, ACTIVE, EQUALS, 30000));
		this.handler.addConstraint(//
				this.handler.createSimpleConstraint("Q=-45kvar", this.ess0, ALL, REACTIVE, EQUALS, -45000));
		this.handler.onBeforeWriteEvent();
	}

	@Test
	public void testReactiveLessOrEquals() throws Exception {
		expect("#1", this.ess0, 0, 0);

		this.handler.addConstraint(//
				this.handler.createSimpleConstraint("Q<=3kvar", this.ess0, ALL, REACTIVE, LESS_OR_EQUALS, 3000));
		this.handler.onBeforeWriteEvent();
	}

	@Test
	public void testReactiveGreaterOrEquals() throws Exception {
		expect("#1", this.ess0, 0, 3000);

		this.handler.addConstraint(//
				this.handler.createSimpleConstraint("Q>=3kvar", this.ess0, ALL, REACTIVE, GREATER_OR_EQUALS, 3000));
		this.handler.onBeforeWriteEvent();
	}

	@Test
	public void testReactiveBounds() throws Exception {
		expect("#1", this.ess0, 0, 0);

		this.handler.addConstraint(//
				this.handler.createSimpleConstraint("Q>=-2kvar", this.ess0, ALL, REACTIVE, GREATER_OR_EQUALS, -2000));
		this.handler.addConstraint(//
				this.handler.createSimpleConstraint("Q<=2kvar", this.ess0, ALL, REACTIVE, LESS_OR_EQUALS, 2000));
		this.handler.onBeforeWriteEvent();
	}

	@Test
	public void testReactiveClearedAfterCycle() throws Exception {
		expect("#1", this.ess0, 10000, 5000);
		this.handler.addConstraint(//
				this.handler.createSimpleConstraint("P=10kW", this.ess0, ALL, ACTIVE, EQUALS, 10000));
		this.handler.addConstraint(//
				this.handler.createSimpleConstraint("Q=5kvar", this.ess0, ALL, REACTIVE, EQUALS, 5000));
		this.handler.onBeforeWriteEvent();

		this.handler.onAfterWriteEvent();
		this.handler.onAfterProcessImage();

		expect("#2", this.ess0, 0, 0);
		this.handler.onBeforeWriteEvent();
	}

	@Test
	public void testFullApparentPower_PAtMax() throws Exception {
		// P=50000 -> qMax=0, Q clamped to 0
		expect("#1", this.ess0, 50000, 0);

		this.handler.addConstraint(//
				this.handler.createSimpleConstraint("P=50kW", this.ess0, ALL, ACTIVE, EQUALS, 50000));
		this.handler.addConstraint(//
				this.handler.createSimpleConstraint("Q=10kvar", this.ess0, ALL, REACTIVE, EQUALS, 10000));
		this.handler.onBeforeWriteEvent();
	}

	@Test
	public void testZeroActivePower_FullReactiveAvailable() throws Exception {
		// P=0 -> qMax=50000
		expect("#1", this.ess0, 0, 50000);

		this.handler.addConstraint(//
				this.handler.createSimpleConstraint("Q=50kvar", this.ess0, ALL, REACTIVE, EQUALS, 50000));
		this.handler.onBeforeWriteEvent();
	}

	// ========== Multiple standalone ESS ==========

	@Test
	public void testMultipleEss_IndependentReactive() throws Exception {
		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.withAllowedChargePower(-30000) //
				.withAllowedDischargePower(30000) //
				.withMaxApparentPower(30000);

		this.esss.add(ess1);
		this.handler.onUpdateEsss();
		this.handler.onAfterProcessImage();

		expect("#1", this.ess0, 10000, 5000);
		expect("#1", ess1, 20000, 3000);

		this.handler.addConstraint(//
				this.handler.createSimpleConstraint("ess0 P=10kW", this.ess0, ALL, ACTIVE, EQUALS, 10000));
		this.handler.addConstraint(//
				this.handler.createSimpleConstraint("ess0 Q=5kvar", this.ess0, ALL, REACTIVE, EQUALS, 5000));
		this.handler.addConstraint(//
				this.handler.createSimpleConstraint("ess1 P=20kW", ess1, ALL, ACTIVE, EQUALS, 20000));
		this.handler.addConstraint(//
				this.handler.createSimpleConstraint("ess1 Q=3kvar", ess1, ALL, REACTIVE, EQUALS, 3000));
		this.handler.onBeforeWriteEvent();
	}

	// ========== Cluster ==========

	@Test
	public void testCluster_ReactiveDistributedEqually() throws Exception {
		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.withSoc(50).withAllowedChargePower(-10000) //
				.withAllowedDischargePower(10000).withMaxApparentPower(10000);
		var ess2 = new DummyManagedSymmetricEss("ess2") //
				.withSoc(50).withAllowedChargePower(-10000) //
				.withAllowedDischargePower(10000).withMaxApparentPower(10000);

		var cluster = new DummyMetaEss("cluster0", ess1, ess2);

		var allEsss = new ArrayList<ManagedSymmetricEss>();
		allEsss.add(cluster);
		allEsss.add(ess1);
		allEsss.add(ess2);

		var powers = new HashMap<String, int[]>();
		for (var ess : List.of(ess1, ess2)) {
			var pq = new int[2];
			powers.put(ess.id(), pq);
			ess.withSymmetricApplyPowerCallback(r -> {
				pq[0] = r.activePower();
				pq[1] = r.reactivePower();
			});
		}

		var handler = new PowerDistributionHandlerV2(() -> allEsss);
		handler.onAfterProcessImage();

		handler.addConstraint(//
				handler.createSimpleConstraint("Q=6kvar", cluster, ALL, REACTIVE, EQUALS, 6000));
		handler.onBeforeWriteEvent();

		assertEquals("ess1 P", 0, powers.get("ess1")[0]);
		assertEquals("ess2 P", 0, powers.get("ess2")[0]);
		assertEquals("ess1 Q", 3000, powers.get("ess1")[1]);
		assertEquals("ess2 Q", 3000, powers.get("ess2")[1]);
	}

	@Test
	public void testCluster_ReactiveClampedByApparentPower() throws Exception {
		// P=8000 per member -> qMax=6000, cluster Q=20000 clamped
		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.withSoc(50).withAllowedChargePower(-10000) //
				.withAllowedDischargePower(10000).withMaxApparentPower(10000);
		var ess2 = new DummyManagedSymmetricEss("ess2") //
				.withSoc(50).withAllowedChargePower(-10000) //
				.withAllowedDischargePower(10000).withMaxApparentPower(10000);

		var cluster = new DummyMetaEss("cluster0", ess1, ess2);

		var allEsss = new ArrayList<ManagedSymmetricEss>();
		allEsss.add(cluster);
		allEsss.add(ess1);
		allEsss.add(ess2);

		var powers = new HashMap<String, int[]>();
		for (var ess : List.of(ess1, ess2)) {
			var pq = new int[2];
			powers.put(ess.id(), pq);
			ess.withSymmetricApplyPowerCallback(r -> {
				pq[0] = r.activePower();
				pq[1] = r.reactivePower();
			});
		}

		var handler = new PowerDistributionHandlerV2(() -> allEsss);
		handler.onAfterProcessImage();

		handler.addConstraint(//
				handler.createSimpleConstraint("P=16kW", cluster, ALL, ACTIVE, EQUALS, 16000));
		handler.addConstraint(//
				handler.createSimpleConstraint("Q=20kvar", cluster, ALL, REACTIVE, EQUALS, 20000));
		handler.onBeforeWriteEvent();

		assertEquals("ess1 P", 8000, powers.get("ess1")[0]);
		assertEquals("ess2 P", 8000, powers.get("ess2")[0]);
		assertEquals("ess1 Q", 6000, powers.get("ess1")[1]);
		assertEquals("ess2 Q", 6000, powers.get("ess2")[1]);
	}

	@Test
	public void testCluster_NoReactiveConstraint_QZero() throws Exception {
		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.withSoc(50).withAllowedChargePower(-10000) //
				.withAllowedDischargePower(10000).withMaxApparentPower(10000);
		var ess2 = new DummyManagedSymmetricEss("ess2") //
				.withSoc(50).withAllowedChargePower(-10000) //
				.withAllowedDischargePower(10000).withMaxApparentPower(10000);

		var cluster = new DummyMetaEss("cluster0", ess1, ess2);

		var allEsss = new ArrayList<ManagedSymmetricEss>();
		allEsss.add(cluster);
		allEsss.add(ess1);
		allEsss.add(ess2);

		var powers = new HashMap<String, int[]>();
		for (var ess : List.of(ess1, ess2)) {
			var pq = new int[2];
			powers.put(ess.id(), pq);
			ess.withSymmetricApplyPowerCallback(r -> {
				pq[0] = r.activePower();
				pq[1] = r.reactivePower();
			});
		}

		var handler = new PowerDistributionHandlerV2(() -> allEsss);
		handler.onAfterProcessImage();

		handler.addConstraint(//
				handler.createSimpleConstraint("P=10kW", cluster, ALL, ACTIVE, EQUALS, 10000));
		handler.onBeforeWriteEvent();

		assertEquals("ess1 P", 5000, powers.get("ess1")[0]);
		assertEquals("ess2 P", 5000, powers.get("ess2")[0]);
		assertEquals("ess1 Q", 0, powers.get("ess1")[1]);
		assertEquals("ess2 Q", 0, powers.get("ess2")[1]);
	}

	@Test
	public void testCluster_UnequalApparentPower_ProportionalReactive() throws Exception {
		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.withSoc(50).withAllowedChargePower(-10000) //
				.withAllowedDischargePower(10000).withMaxApparentPower(10000);
		var ess2 = new DummyManagedSymmetricEss("ess2") //
				.withSoc(50).withAllowedChargePower(-20000) //
				.withAllowedDischargePower(20000).withMaxApparentPower(20000);

		var cluster = new DummyMetaEss("cluster0", ess1, ess2);

		var allEsss = new ArrayList<ManagedSymmetricEss>();
		allEsss.add(cluster);
		allEsss.add(ess1);
		allEsss.add(ess2);

		var powers = new HashMap<String, int[]>();
		for (var ess : List.of(ess1, ess2)) {
			var pq = new int[2];
			powers.put(ess.id(), pq);
			ess.withSymmetricApplyPowerCallback(r -> {
				pq[0] = r.activePower();
				pq[1] = r.reactivePower();
			});
		}

		var handler = new PowerDistributionHandlerV2(() -> allEsss);
		handler.onAfterProcessImage();

		handler.addConstraint(//
				handler.createSimpleConstraint("Q=15kvar", cluster, ALL, REACTIVE, EQUALS, 15000));
		handler.onBeforeWriteEvent();

		assertEquals("ess1 P", 0, powers.get("ess1")[0]);
		assertEquals("ess2 P", 0, powers.get("ess2")[0]);
		assertEquals("ess1 Q", 5000, powers.get("ess1")[1]);
		assertEquals("ess2 Q", 10000, powers.get("ess2")[1]);
	}
}
