package io.openems.common.types;

import org.junit.Assert;
import org.junit.Test;

public class SemanticVersionTest {

	@Test
	public void testIsAtLeast() {
		var version2018110Snapshot = SemanticVersion.fromString("2018.11.0-SNAPSHOT");
		var version2018100Snapshot = SemanticVersion.fromString("2018.10.0-SNAPSHOT");
		var version2018100 = SemanticVersion.fromString("2018.10.0");
		var version2018101 = SemanticVersion.fromString("2018.10.1");
		var versionEmtpy = SemanticVersion.fromString("");

		Assert.assertTrue(version2018110Snapshot.isAtLeast(versionEmtpy));
		Assert.assertTrue(version2018110Snapshot.isAtLeast(version2018101));

		Assert.assertTrue(version2018100.isAtLeast(version2018100Snapshot));

		Assert.assertTrue(version2018101.isAtLeast(version2018100Snapshot));
		Assert.assertTrue(version2018101.isAtLeast(version2018100));

		var version201810 = SemanticVersion.fromString("2018.10");
		var version2018 = SemanticVersion.fromString("2018");

		Assert.assertTrue(version201810.isAtLeast(version2018100));
		Assert.assertTrue(version2018.isAtLeast(version2018));
	}

	@Test
	public void testToString() {
		Assert.assertEquals("2018.11.0-SNAPSHOT", SemanticVersion.fromString("2018.11.0-SNAPSHOT").toString());
		Assert.assertEquals("2018.11.0", SemanticVersion.fromString("2018.11.0").toString());
	}

}
