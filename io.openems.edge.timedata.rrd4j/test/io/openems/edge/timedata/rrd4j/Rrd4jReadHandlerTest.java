package io.openems.edge.timedata.rrd4j;

import static org.junit.Assert.assertTrue;

import java.time.Instant;

import org.junit.Before;
import org.junit.Test;
import org.rrd4j.core.RrdBackendFactory;
import org.rrd4j.core.RrdMemoryBackendFactory;

import io.openems.common.channel.Unit;
import io.openems.edge.timedata.rrd4j.version.Version.CreateDatabaseConfig;
import io.openems.edge.timedata.rrd4j.version.Version3Test;

public class Rrd4jReadHandlerTest {
	// starts at 1. January 2020 00:00:00
	private static final Instant START = Instant.ofEpochSecond(1577836800L);

	private RrdBackendFactory factory;

	@Before
	public void setUp() throws Exception {
		this.factory = new RrdMemoryBackendFactory();
	}

	@Test
	public void testGetArchivesSortedByArcStep() throws Exception {
		final var version3 = Version3Test.createDummyVersion3();

		final var db = version3.createNewDb(new CreateDatabaseConfig(//
				"rrdbId", //
				Unit.WATT_HOURS, //
				"path", //
				START.getEpochSecond(), //
				this.factory, //
				null //
		));

		final var sorted = Rrd4jReadHandler.getArchivesSortedByArcStep(db);
		long lastStepSize = 0L;
		for (var archive : sorted) {
			assertTrue("The last step size should be lower than the next step size.",
					lastStepSize < archive.getArcStep());
			lastStepSize = archive.getArcStep();
		}

	}

}
