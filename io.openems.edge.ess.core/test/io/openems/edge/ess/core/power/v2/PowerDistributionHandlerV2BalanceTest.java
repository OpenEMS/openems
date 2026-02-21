package io.openems.edge.ess.core.power.v2;

import static io.openems.edge.common.type.Phase.SingleOrAllPhase.ALL;
import static io.openems.edge.ess.power.api.Pwr.ACTIVE;
import static io.openems.edge.ess.power.api.Relationship.EQUALS;
import static io.openems.edge.ess.power.api.Relationship.GREATER_OR_EQUALS;
import static io.openems.edge.ess.power.api.Relationship.LESS_OR_EQUALS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PowerDistributionHandlerV2BalanceTest {

	@Test
	public void testDischarge_EqualSoc() throws Exception {
		var s = V2TestUtils.createCluster("cluster0", new int[][] { //
				{ 50, -10000, 10000 }, //
				{ 50, -10000, 10000 }, //
				{ 50, -10000, 10000 }, //
				{ 50, -10000, 10000 }, //
		});

		var handler = V2TestUtils.createHandler(s);
		handler.onAfterProcessImage();
		handler.addConstraint(//
				handler.createSimpleConstraint("Cluster 20kW", s.cluster(), ALL, ACTIVE, EQUALS, 20000));
		handler.onBeforeWriteEvent();

		assertEquals("Total", 20000, s.totalPower());
		assertEquals("ess1", 5000, s.powerOf("ess1"));
		assertEquals("ess2", 5000, s.powerOf("ess2"));
		assertEquals("ess3", 5000, s.powerOf("ess3"));
		assertEquals("ess4", 5000, s.powerOf("ess4"));
	}

	@Test
	public void testSingleMember_ExceedsLimit() throws Exception {
		var s = V2TestUtils.createCluster("cluster0", new int[][] { //
				{ 50, -5000, 8000 }, //
		});

		var handler = V2TestUtils.createHandler(s);
		handler.onAfterProcessImage();
		handler.addConstraint(//
				handler.createSimpleConstraint("Cluster 15kW", s.cluster(), ALL, ACTIVE, EQUALS, 15000));
		handler.onBeforeWriteEvent();

		assertEquals("ess1 clamped to 8kW", 8000, s.powerOf("ess1"));
	}

	@Test
	public void testDischarge_UnequalCapacities() throws Exception {
		var s = V2TestUtils.createCluster("cluster0", new int[][] { //
				{ 50, -10000, 5000 }, // ess1: 5kW max
				{ 50, -10000, 15000 }, // ess2: 15kW max
		});

		var handler = V2TestUtils.createHandler(s);
		handler.onAfterProcessImage();
		handler.addConstraint(//
				handler.createSimpleConstraint("Cluster 20kW", s.cluster(), ALL, ACTIVE, EQUALS, 20000));
		handler.onBeforeWriteEvent();

		assertEquals("Total", 20000, s.totalPower());
		assertEquals("ess1", 5000, s.powerOf("ess1"));
		assertEquals("ess2", 15000, s.powerOf("ess2"));
	}

	@Test
	public void testPerEssConstraint_LimitActivePower() throws Exception {
		var s = V2TestUtils.createCluster("cluster0", new int[][] { //
				{ 10, -10000, 10000 }, // ess1: will be constrained
				{ 50, -10000, 10000 }, // ess2
				{ 70, -10000, 10000 }, // ess3
				{ 80, -10000, 10000 }, // ess4
		});

		var handler = V2TestUtils.createHandler(s);
		handler.onAfterProcessImage();

		handler.addConstraint(//
				handler.createSimpleConstraint("Limit ess1", s.members().get(0), ALL, ACTIVE, LESS_OR_EQUALS, 5000));
		handler.addConstraint(//
				handler.createSimpleConstraint("Cluster 40kW", s.cluster(), ALL, ACTIVE, EQUALS, 40000));
		handler.onBeforeWriteEvent();

		assertTrue("ess1 <= 5000W", s.powerOf("ess1") <= 5000);
		assertEquals("Total = 35kW", 35000, s.totalPower());
	}

	@Test
	public void testPerEssConstraint_LimitCharge() throws Exception {
		var s = V2TestUtils.createCluster("cluster0", new int[][] { //
				{ 10, -10000, 10000 }, // ess1
				{ 30, -10000, 10000 }, // ess2
				{ 70, -10000, 10000 }, // ess3
				{ 90, -10000, 10000 }, // ess4: will be constrained
		});

		var handler = V2TestUtils.createHandler(s);
		handler.onAfterProcessImage();

		handler.addConstraint(//
				handler.createSimpleConstraint("Limit ess4", s.members().get(3), ALL, ACTIVE, GREATER_OR_EQUALS,
						-3000));
		handler.addConstraint(//
				handler.createSimpleConstraint("Cluster -40kW", s.cluster(), ALL, ACTIVE, EQUALS, -40000));
		handler.onBeforeWriteEvent();

		assertTrue("ess4 >= -3000W", s.powerOf("ess4") >= -3000);
		assertEquals("Total = -33kW", -33000, s.totalPower());
	}

	@Test
	public void testDischarge_PartialCapacity() throws Exception {
		var s = V2TestUtils.createCluster("cluster0", new int[][] { //
				{ 50, -10000, 5000 }, //
				{ 50, -10000, 5000 }, //
		});

		var handler = V2TestUtils.createHandler(s);
		handler.onAfterProcessImage();
		handler.addConstraint(//
				handler.createSimpleConstraint("Cluster 20kW", s.cluster(), ALL, ACTIVE, EQUALS, 20000));
		handler.onBeforeWriteEvent();

		assertEquals("Total clamped to 10kW", 10000, s.totalPower());
		assertEquals("ess1", 5000, s.powerOf("ess1"));
		assertEquals("ess2", 5000, s.powerOf("ess2"));
	}

	@Test
	public void testClusterCharge() throws Exception {
		var s = V2TestUtils.createCluster("cluster0", new int[][] { //
				{ 10, -10000, 10000 }, //
				{ 30, -10000, 10000 }, //
				{ 70, -10000, 10000 }, //
				{ 90, -10000, 10000 }, //
		});

		var handler = V2TestUtils.createHandler(s);
		handler.onAfterProcessImage();

		handler.addConstraint(//
				handler.createSimpleConstraint("Cluster -3kW", s.cluster(), ALL, ACTIVE, EQUALS, -3000));
		handler.onBeforeWriteEvent();

		assertEquals("Total = -3kW", -3000, s.totalPower());

		for (var entry : s.appliedPowers().entrySet()) {
			assertTrue(entry.getKey() + " must charge", entry.getValue().get() <= 0);
		}
	}
}
