package io.openems.edge.ess.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.Test;

import io.openems.edge.ess.test.DummySymmetricEss;

public class CalculateSocTest {

	@Test
	public void testEmpty() {
		var esss = List.<SymmetricEss>of();
		assertNull(new CalculateSoc().add(esss).calculate());
	}

	@Test
	public void testNull() {
		var esss = List.<SymmetricEss>of(//
				new DummySymmetricEss("ess0"), //
				new DummySymmetricEss("ess1"));
		assertNull(new CalculateSoc().add(esss).calculate());
	}

	@Test
	public void testWeightedSoc() {
		var esss = List.<SymmetricEss>of(//
				new DummySymmetricEss("ess0").withCapacity(10_000).withSoc(40), //
				new DummySymmetricEss("ess1").withCapacity(20_000).withSoc(60));
		assertEquals(53, (int) new CalculateSoc().add(esss).calculate());
	}

	@Test
	public void testAverageSoc() {
		var esss = List.<SymmetricEss>of(//
				new DummySymmetricEss("ess0").withCapacity(10_000).withSoc(40), //
				new DummySymmetricEss("ess1"), //
				new DummySymmetricEss("ess2").withSoc(60));
		assertEquals(50, (int) new CalculateSoc().add(esss).calculate());
	}

}
