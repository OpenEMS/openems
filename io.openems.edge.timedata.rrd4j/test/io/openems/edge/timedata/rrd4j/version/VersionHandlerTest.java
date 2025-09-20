package io.openems.edge.timedata.rrd4j.version;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class VersionHandlerTest {

	private VersionHandler versionHandler;

	private Version1 version1;
	private Version2 version2;
	private Version3 version3;

	@Before
	public void setUp() {
		this.versionHandler = new VersionHandler();
		this.version1 = new Version1(VersionTest.createDummyVersionComponentContext(1));
		this.version2 = Version3Test.createDummyVersion2();
		this.version3 = Version3Test.createDummyVersion3();
		this.versionHandler.bindVersion(this.version1);
		this.versionHandler.bindVersion(this.version3);
		this.versionHandler.bindVersion(this.version2);
	}

	@Test
	public void testGetLatestVersion() {
		assertEquals(this.version3, this.versionHandler.getLatestVersion());
	}

	@Test
	public void testGetLatestVersionNumber() {
		assertEquals(this.version3.getVersion(), this.versionHandler.getLatestVersionNumber());
	}

}
