package io.openems.edge.ess.core.power.v2;

import static io.openems.edge.common.type.Phase.SingleOrAllPhase.ALL;
import static io.openems.edge.ess.power.api.Pwr.ACTIVE;
import static io.openems.edge.ess.power.api.Relationship.EQUALS;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ForceChargeDischargeTest {

	@Test
	public void testEssClusterForceCharge() throws Exception {
		// ess3 [-10000, -2000] must charge even when cluster discharges
		var s = V2TestUtils.createCluster("cluster0", new int[][] { //
				{ 10, -10000, 10000 }, // ess1
				{ 30, -10000, 10000 }, // ess2
				{ 70, -10000, -2000 }, // ess3: force charge
				{ 90, -10000, 10000 }, // ess4
		});

		var handler = V2TestUtils.createHandler(s);
		handler.onAfterProcessImage();

		handler.addConstraint(//
				handler.createSimpleConstraint("Cluster 40kW", s.cluster(), ALL, ACTIVE, EQUALS, 40000));
		handler.onBeforeWriteEvent();

		assertEquals("ess1", 10000, s.powerOf("ess1"));
		assertEquals("ess2", 10000, s.powerOf("ess2"));
		assertEquals("ess3 force charge", -2000, s.powerOf("ess3"));
		assertEquals("ess4", 10000, s.powerOf("ess4"));
		assertEquals("Total", 28000, s.totalPower());
	}

	@Test
	public void testEssClusterForceDischarge() throws Exception {
		// ess3 [2000, 10000] must discharge even when cluster charges
		var s = V2TestUtils.createCluster("cluster0", new int[][] { //
				{ 10, -10000, 10000 }, // ess1
				{ 30, -10000, 10000 }, // ess2
				{ 70, 2000, 10000 }, // ess3: force discharge
				{ 90, -10000, 10000 }, // ess4
		});

		var handler = V2TestUtils.createHandler(s);
		handler.onAfterProcessImage();

		handler.addConstraint(//
				handler.createSimpleConstraint("Cluster -40kW", s.cluster(), ALL, ACTIVE, EQUALS, -40000));
		handler.onBeforeWriteEvent();

		assertEquals("ess1", -10000, s.powerOf("ess1"));
		assertEquals("ess2", -10000, s.powerOf("ess2"));
		assertEquals("ess3 force discharge", 2000, s.powerOf("ess3"));
		assertEquals("ess4", -10000, s.powerOf("ess4"));
		assertEquals("Total", -28000, s.totalPower());
	}

	@Test
	public void testEssClusterNoConstraints() throws Exception {
		// No cluster constraint — ess3 [2000, 10000] must still force-discharge
		var s = V2TestUtils.createCluster("cluster0", new int[][] { //
				{ 10, -10000, 10000 }, // ess1
				{ 30, -10000, 10000 }, // ess2
				{ 70, 2000, 10000 }, // ess3: force discharge
				{ 90, -10000, 10000 }, // ess4
		});

		var handler = V2TestUtils.createHandler(s);
		handler.onAfterProcessImage();
		handler.onBeforeWriteEvent();

		assertEquals("ess1", 0, s.powerOf("ess1"));
		assertEquals("ess2", 0, s.powerOf("ess2"));
		assertEquals("ess3 force discharge", 2000, s.powerOf("ess3"));
		assertEquals("ess4", 0, s.powerOf("ess4"));
		assertEquals("Total", 2000, s.totalPower());
	}
}
