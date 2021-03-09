package io.openems.edge.timedata.rrd4j;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.Test;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.DsDef;
import org.rrd4j.core.FetchRequest;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.RrdMemoryBackendFactory;
import org.rrd4j.core.Sample;

public class Rrd4jTimedataImplTest {

	private final static Instant START = Instant.ofEpochSecond(1577836800L); /* starts at 1. January 2020 00:00:00 */

	private static void addSample(RrdDb database, Instant instant, double value) throws IOException {
		Sample sample = database.createSample(instant.getEpochSecond());
		sample.setValue(0, value);
		sample.update();
	}

	private static RrdDb createRrdDb(int oneMinute, int fiveMinutes) throws IOException, URISyntaxException {
		final RrdDef rrdDef = new RrdDef("empty-path", START.getEpochSecond(), Rrd4jTimedataImpl.DEFAULT_STEP_SECONDS);
		rrdDef.addDatasource(//
				new DsDef(Rrd4jTimedataImpl.DEFAULT_DATASOURCE_NAME, //
						DsType.GAUGE, //
						Rrd4jTimedataImpl.DEFAULT_HEARTBEAT_SECONDS, // Heartbeat in [s], default 60 = 1 minute
						Double.NaN, Double.NaN));
		// detailed recordings
		rrdDef.addArchive(ConsolFun.AVERAGE, 0.5, 1, oneMinute); // 1 step (1 minute), 1440 rows (1 day)
		rrdDef.addArchive(ConsolFun.AVERAGE, 0.5, 5, fiveMinutes); // 5 steps (5 minutes), 2880 rows (10 days)
		// hourly values for a very long time
		rrdDef.addArchive(ConsolFun.AVERAGE, 0.5, 60, 87_600); // 60 steps (1 hour), 87600 rows (10 years)
		final RrdDb database = RrdDb.getBuilder() //
				.setBackendFactory(new RrdMemoryBackendFactory()) // in memory
				.setRrdDef(rrdDef) //
				.build();

		for (int i = 1; i <= 120; i++) {
			addSample(database, START.plus(i, ChronoUnit.MINUTES), i);
		}
		return database;
	}

	/**
	 * Test RRD4j step smaller than resolution.
	 * 
	 * @throws IOException        on error
	 * @throws URISyntaxException on error
	 */
	@Test
	public void testMerge() throws IOException, URISyntaxException {
		int resolution = 300; // 5 minutes

		RrdDb database = createRrdDb(1000, 2000);
		FetchRequest request = database.createFetchRequest(ConsolFun.AVERAGE, START.getEpochSecond(),
				START.plus(3, ChronoUnit.HOURS).getEpochSecond());
		double[] result = Rrd4jTimedataImpl.postProcessData(request, resolution);
		database.close();

		assertEquals(36, result.length); // 3 hours * 12 entries/per hour (5 minutes) = 36
		assertEquals(3.0, result[0], 0.001);
		assertEquals(8.0, result[1], 0.001);
		assertEquals(13.0, result[2], 0.001);
	}

	/**
	 * Test RRD4j step equals resolution.
	 * 
	 * @throws IOException        on error
	 * @throws URISyntaxException on error
	 */
	@Test
	public void testExact() throws IOException, URISyntaxException {
		int resolution = 300; // 5 minutes

		RrdDb database = createRrdDb(10, 200);
		FetchRequest request = database.createFetchRequest(ConsolFun.AVERAGE, START.getEpochSecond(),
				START.plus(3, ChronoUnit.HOURS).getEpochSecond());
		double[] result = Rrd4jTimedataImpl.postProcessData(request, resolution);
		database.close();

		assertEquals(36, result.length); // 3 hours * 12 entries/per hour (5 minutes) = 36
		assertEquals(3.0, result[0], 0.001);
		assertEquals(8.0, result[1], 0.001);
		assertEquals(13.0, result[2], 0.001);
	}

	/**
	 * Test RRD4j step bigger than resolution.
	 * 
	 * @throws IOException        on error
	 * @throws URISyntaxException on error
	 */
	@Test
	public void testSplit() throws IOException, URISyntaxException {
		int resolution = 300; // 5 minutes

		RrdDb database = createRrdDb(10, 20);
		FetchRequest request = database.createFetchRequest(ConsolFun.AVERAGE, START.getEpochSecond(),
				START.plus(3, ChronoUnit.HOURS).getEpochSecond());
		double[] result = Rrd4jTimedataImpl.postProcessData(request, resolution);
		database.close();

		assertEquals(36, result.length); // 3 hours * 12 entries/per hour (5 minutes) = 36
		assertEquals(30.5, result[0], 0.001);
		assertEquals(30.5, result[1], 0.001);
		assertEquals(30.5, result[2], 0.001);
		assertEquals(90.5, result[12], 0.001);
	}
}
