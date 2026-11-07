package io.openems.edge.ess.core.power.v2;

import static io.openems.edge.common.type.Phase.SingleOrAllPhase.ALL;
import static io.openems.edge.ess.power.api.Pwr.ACTIVE;
import static io.openems.edge.ess.power.api.Relationship.EQUALS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.ess.core.power.EssPowerImpl;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyMetaEss;

public class PowerDistributionHandlerV2ConservationIntegrationTest {

	@Test
	public void testClusterForcedDischargeRedistributesThroughApplyPower() throws Exception {
		var pa = new AtomicInteger();
		var pc = new AtomicInteger();
		var pb = new AtomicInteger();

		var power = new EssPowerImpl();
		var a = new DummyManagedSymmetricEss("essA") //
				.setPower(power).withSoc(50) //
				.withAllowedChargePower(-5000) //
				.withAllowedDischargePower(5000) //
				.withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> pa.set(r.activePower()));
		var c = new DummyManagedSymmetricEss("essC") //
				.setPower(power).withSoc(50) //
				.withAllowedChargePower(-10000) //
				.withAllowedDischargePower(10000) //
				.withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> pc.set(r.activePower()));
		var b = new DummyManagedSymmetricEss("essB") //
				.setPower(power).withSoc(50) //
				.withAllowedChargePower(4000) //
				.withAllowedDischargePower(10000) //
				.withMaxApparentPower(10000) //
				.withSymmetricApplyPowerCallback(r -> pb.set(r.activePower()));
		var cluster = new DummyMetaEss("cluster", a, c, b);

		var sut = V2TestUtils.createComponentTest(power, cluster, a, c, b);
		sut.next(new TestCase() //
				.onBeforeWriteCallbacks(() -> {
					power.addConstraint(power.createSimpleConstraint("Cluster 7kW", cluster, ALL, ACTIVE, EQUALS, 7000));
				}));

		assertEquals(1000, pa.get());
		assertEquals(2000, pc.get());
		assertEquals(4000, pb.get());
		assertEquals(7000, pa.get() + pc.get() + pb.get());
	}
}
