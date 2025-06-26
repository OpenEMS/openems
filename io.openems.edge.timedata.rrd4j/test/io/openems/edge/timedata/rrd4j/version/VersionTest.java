package io.openems.edge.timedata.rrd4j.version;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;

import com.google.common.collect.Lists;

import io.openems.edge.common.test.DummyComponentContext;

public class VersionTest {

	private Version1 version1;
	private Version2 version2;
	private Version3 version3;

	@Before
	public void setUp() throws Exception {
		this.version1 = new Version1(VersionTest.createDummyVersionComponentContext(1));
		this.version2 = new Version2(VersionTest.createDummyVersionComponentContext(2));
		this.version3 = new Version3(VersionTest.createDummyVersionComponentContext(3));
	}

	@Test
	public void testNumberComparator() {
		final var versions = Lists.newArrayList(this.version3, this.version1, this.version2);
		Collections.sort(versions, Version.numberComparator());
		assertEquals(versions.get(0), this.version1);
		assertEquals(versions.get(1), this.version2);
		assertEquals(versions.get(2), this.version3);
	}

	/**
	 * Creates a dummy {@link ComponentContext} for a {@link Version}.
	 * 
	 * @param version the number of the version
	 * @return the dummy {@link ComponentContext}
	 */
	public static ComponentContext createDummyVersionComponentContext(int version) {
		final var context = new DummyComponentContext();
		context.addProperty("version", version);
		return context;
	}

}
