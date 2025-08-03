package io.openems.common.types;

import static io.openems.common.types.SemanticVersion.fromString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SemanticVersionTest {

	@Test
	public void testFromString() {
		var version2018110Snapshot = SemanticVersion.fromString("2018.11.0-SNAPSHOT");
		var version2018100Snapshot = SemanticVersion.fromString("2018.10.0-SNAPSHOT");
		var version2018100 = SemanticVersion.fromString("2018.10.0");
		var version2018101 = SemanticVersion.fromString("2018.10.1");
		var versionEmtpy = SemanticVersion.fromString("");

		assertTrue(version2018110Snapshot.isAtLeast(versionEmtpy));
		assertTrue(version2018110Snapshot.isAtLeast(version2018101));

		assertTrue(version2018100.isAtLeast(version2018100Snapshot));

		assertTrue(version2018101.isAtLeast(version2018100Snapshot));
		assertTrue(version2018101.isAtLeast(version2018100));

		var version201810 = SemanticVersion.fromString("2018.10");
		var version2018 = SemanticVersion.fromString("2018");

		assertTrue(version201810.isAtLeast(version2018100));
		assertTrue(version2018.isAtLeast(version2018));

		assertEquals(SemanticVersion.ZERO, SemanticVersion.fromStringOrZero(""));
		assertEquals(SemanticVersion.ZERO, SemanticVersion.fromStringOrZero(null));
		assertEquals(SemanticVersion.ZERO, SemanticVersion.fromStringOrZero("xyz"));
	}

	@Test
	public void testIsAtLeast() {
		assertTrue(fromString("6").isAtLeast(fromString("5")));
		assertFalse(fromString("4").isAtLeast(fromString("5")));

		assertTrue(fromString("5.7").isAtLeast(fromString("5.6")));
		assertFalse(fromString("5.5").isAtLeast(fromString("5.6")));

		assertTrue(fromString("5.6.7").isAtLeast(fromString("5.6.6")));
		assertFalse(fromString("5.6.5").isAtLeast(fromString("5.6.6")));

		assertTrue(fromString("5.6.7").isAtLeast(fromString("5.6.7-SNAPSHOT")));

		assertTrue(fromString("5.6.7-abc").isAtLeast(fromString("5.6.7-abb")));
		assertFalse(fromString("5.6.7-aba").isAtLeast(fromString("5.6.7-abb")));
	}

	@Test
	public void testToString() {
		assertEquals("2018.11.0-SNAPSHOT", SemanticVersion.fromString("2018.11.0-SNAPSHOT").toString());
		assertEquals("2018.11.0", SemanticVersion.fromString("2018.11.0").toString());
	}

}
