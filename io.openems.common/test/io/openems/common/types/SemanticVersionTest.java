package io.openems.common.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SemanticVersionTest {

	@Test
	public void testIsAtLeast() {
		SemanticVersion version2018110Snapshot = SemanticVersion.fromString("2018.11.0-SNAPSHOT");
		SemanticVersion version2018100Snapshot = SemanticVersion.fromString("2018.10.0-SNAPSHOT");
		SemanticVersion version2018100 = SemanticVersion.fromString("2018.10.0");
		SemanticVersion version2018101 = SemanticVersion.fromString("2018.10.1");
		SemanticVersion versionEmtpy = SemanticVersion.fromString("");

		assertTrue(version2018110Snapshot.isAtLeast(versionEmtpy));
		assertTrue(version2018110Snapshot.isAtLeast(version2018101));

		assertTrue(version2018100.isAtLeast(version2018100Snapshot));

		assertTrue(version2018101.isAtLeast(version2018100Snapshot));
		assertTrue(version2018101.isAtLeast(version2018100));

		SemanticVersion version201810 = SemanticVersion.fromString("2018.10");
		SemanticVersion version2018 = SemanticVersion.fromString("2018");

		assertTrue(version201810.isAtLeast(version2018100));
		assertTrue(version2018.isAtLeast(version2018));
	}

	@Test
	public void testToString() {
		assertEquals("2018.11.0-SNAPSHOT", SemanticVersion.fromString("2018.11.0-SNAPSHOT").toString());
		assertEquals("2018.11.0", SemanticVersion.fromString("2018.11.0").toString());
	}

}
