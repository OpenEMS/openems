package io.openems.edge.controller.ess.gridoptimizedcharge;

import static io.openems.edge.controller.ess.gridoptimizedcharge.EnergyScheduler.buildEnergyScheduleHandler;
import static org.junit.Assert.assertEquals;

import java.time.LocalTime;
import java.util.OptionalInt;

import org.junit.Test;

import io.openems.edge.controller.ess.gridoptimizedcharge.EnergyScheduler.OptimizationContext;
import io.openems.edge.controller.test.DummyController;
import io.openems.edge.energy.api.test.EnergyScheduleTester;

public class EnergySchedulerTest {

		@Test
		public void testNull() {
			var esh = buildEnergyScheduleHandler(new DummyController("ctrl0"), () -> null);
			var t = EnergyScheduleTester.from(esh);
			assertEquals(-3894 /* no charge limitation */, t.simulatePeriod().ef().setEss(-4000));
		}
	
		@Test
		public void testManual() {
			var esh = buildEnergyScheduleHandler(new DummyController("ctrl0"),
					() -> new EnergyScheduler.Config.Manual(LocalTime.of(10, 00)));
			assertEquals("", esh.getParentFactoryPid());
			assertEquals("ctrl0", esh.getParentId());
	
			var t = EnergyScheduleTester.from(esh);
			var csc = (OptimizationContext) t.perEsh.get(0).csc();
			var limits = csc.limits().values().toArray(OptionalInt[]::new);
			assertEquals(3, limits.length);
			assertEquals(OptionalInt.empty(), limits[0]);
			assertEquals(OptionalInt.of(1214), limits[1]);
			assertEquals(OptionalInt.empty(), limits[2]);
	
			assertEquals(-3894, t.simulatePeriod().ef().setEss(-4000));
			assertEquals(-1214, t.simulatePeriodIndex(26).ef().setEss(-4000));
			assertEquals(-4000,t.simulatePeriodIndex(40).ef().setEss(-4000));
		}
}
