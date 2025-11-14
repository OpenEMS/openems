package io.openems.edge.battery.fenecon.home;

import static io.openems.edge.battery.fenecon.home.TwoPartVersion.fromString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TwoPartVersionTest {

	@Test
	public void testFromString() {
		var version11 = TwoPartVersion.fromString("1.1");
		var version12 = TwoPartVersion.fromString("1.2");
		var version117 = TwoPartVersion.fromString("1.17");
		var version5 = TwoPartVersion.fromString("5");
		var versionEmpty = TwoPartVersion.fromString("");

		assertTrue(version5.isAtLeast(version117));
		assertTrue(version11.isAtLeast(versionEmpty));
		assertTrue(version12.isAtLeast(version11));
		assertTrue(version11.isAtLeast(version11));
		assertFalse(version11.isAtLeast(version117));

		assertEquals(TwoPartVersion.ZERO, TwoPartVersion.fromStringOrZero(""));
		assertEquals(TwoPartVersion.ZERO, TwoPartVersion.fromStringOrZero(null));
		assertEquals(TwoPartVersion.ZERO, TwoPartVersion.fromStringOrZero("abc"));
	}

	@Test
	public void testIsAtLeast() {
		assertTrue(fromString("6").isAtLeast(fromString("5")));
		assertFalse(fromString("4").isAtLeast(fromString("5")));

		assertTrue(fromString("1.2").isAtLeast(fromString("1.1")));
		assertTrue(fromString("1.1").isAtLeast(fromString("1.1")));
		assertTrue(fromString("1.1").isAtLeast(fromString("0.2")));
	}

	@Test
	public void testToString() {
		assertEquals("1.2", TwoPartVersion.fromString("1.2").toString());
		assertEquals("2.0", TwoPartVersion.fromString("2").toString());
		assertEquals("1.9", TwoPartVersion.fromRegisterValue(0x109).toString());
	}

}
