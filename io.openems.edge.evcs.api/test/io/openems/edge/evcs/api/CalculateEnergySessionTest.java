package io.openems.edge.evcs.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.function.Supplier;

import org.junit.Test;

import io.openems.edge.common.filter.DisabledRampFilter;
import io.openems.edge.evcs.test.DummyEvcsPower;
import io.openems.edge.evcs.test.DummyManagedEvcs;

public class CalculateEnergySessionTest {

	@Test
	public void test() {
		var evcs = new DummyManagedEvcs("evcs0", new DummyEvcsPower(new DisabledRampFilter()));
		Supplier<Integer> energy = () -> evcs.getEnergySessionChannel().getNextValue().get();
		var sut = new CalculateEnergySession(evcs);

		sut.update(false);
		assertNull(energy.get());

		sut.update(true);
		assertNull(energy.get());

		evcs.withActiveProductionEnergy(null);
		sut.update(true);
		assertNull(energy.get());

		evcs.withActiveProductionEnergy(100);
		sut.update(true);
		assertEquals(0L, energy.get().intValue());

		evcs.withActiveProductionEnergy(101);
		sut.update(true);
		assertEquals(1L, energy.get().intValue());
	}
}
