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
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.RrdMemoryBackendFactory;

import io.openems.common.channel.PersistencePriority;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;

public class TimedataRrd4jImplTest {

	private static final String COMPONENT_ID = "rrd4j0";

	@Test
	public void test() throws Exception {
		final var componentManager = new DummyComponentManager();
		new ComponentTest(new TimedataRrd4jImpl()) //
				.addReference("workerFactory", new DummyRecordWorkerFactory(componentManager)) //
				.addReference("readHandler", new Rrd4jReadHandler()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setPersistencePriority(PersistencePriority.MEDIUM) //
						.build()) //
				.next(new TestCase()) //
		;
	}

	private static final Instant START = Instant.ofEpochSecond(1577836800L); /* starts at 1. January 2020 00:00:00 */

	private static void addSample(RrdDb database, Instant instant, double value) throws IOException {
		var sample = database.createSample(instant.getEpochSecond());
		sample.setValue(0, value);
		sample.update();
	}

	private static RrdDb createRrdDb(int step, int fiveMinutes, int oneHour) throws IOException, URISyntaxException {
		final var rrdDef = new RrdDef("empty-path", START.getEpochSecond() - 1, step);
		rrdDef.addDatasource(//
				new DsDef(Rrd4jConstants.DEFAULT_DATASOURCE_NAME, //
						DsType.GAUGE, //
						Rrd4jConstants.DEFAULT_HEARTBEAT_SECONDS, // Heartbeat in [s], default 300 = 5 minutes
						Double.NaN, Double.NaN));
		// detailed recordings
		rrdDef.addArchive(ConsolFun.AVERAGE, 0.5, 1, fiveMinutes); // 1 step (5 minutes), 8928 rows (31 days)
		rrdDef.addArchive(ConsolFun.AVERAGE, 0.5, 12, oneHour); // 12 steps (60 minutes), 8016 rows (334 days)

		final var database = RrdDb.getBuilder() //
				.setBackendFactory(new RrdMemoryBackendFactory()) // in memory
				.setRrdDef(rrdDef) //
				.build();

		for (var i = 0; i <= 60 /* minutes */ * 4 /* hours */; i++) {
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
		var resolution = 900; // 15 minutes

		var database = createRrdDb(300, 100, 100);
		var request = database.createFetchRequest(//
				ConsolFun.AVERAGE, //
				START.getEpochSecond(), //
				START.plus(3, ChronoUnit.HOURS).getEpochSecond());
		var result = Rrd4jSupplier.postProcessData(request.fetchData(), resolution);
		database.close();

		assertEquals(12, result.length); // 3 hours * 4 entries/per hour (15 minutes) = 12
		assertEquals(8.0, result[0], 0.1);
		assertEquals(23.0, result[1], 0.1);
		assertEquals(38.0, result[2], 0.1);
	}

	/**
	 * Test RRD4j step equals resolution.
	 *
	 * @throws IOException        on error
	 * @throws URISyntaxException on error
	 */
	@Test
	public void testExact() throws IOException, URISyntaxException {
		var resolution = 300; // 5 minutes

		var database = createRrdDb(300, 100, 100);
		var request = database.createFetchRequest(//
				ConsolFun.AVERAGE, //
				START.getEpochSecond(), //
				START.plus(3, ChronoUnit.HOURS).getEpochSecond());
		var result = Rrd4jSupplier.postProcessData(request.fetchData(), resolution);
		database.close();

		assertEquals(36, result.length); // 3 hours * 12 entries/per hour (5 minutes) = 36
		assertEquals(0.0, result[0], 0.1);
		assertEquals(3.0, result[1], 0.1);
		assertEquals(8.0, result[2], 0.1);
		assertEquals(13.0, result[3], 0.1);
	}

	/**
	 * Test RRD4j step bigger than resolution.
	 *
	 * @throws IOException        on error
	 * @throws URISyntaxException on error
	 */
	@Test
	public void testSplit() throws IOException, URISyntaxException {
		var resolution = 60; // 1 minute

		var database = createRrdDb(300, 100, 100);
		var request = database.createFetchRequest(//
				ConsolFun.AVERAGE, //
				START.getEpochSecond(), //
				START.plus(3, ChronoUnit.HOURS).getEpochSecond());
		var result = Rrd4jSupplier.postProcessData(request.fetchData(), resolution);
		database.close();

		assertEquals(180, result.length); // 3 hours * 60 entries/per hour (1 minute) = 180
		for (var i = 0; i < 5; i++) {
			assertEquals(0., result[i], 0.1);
		}
		for (var i = 5; i < 10; i++) {
			assertEquals(3., result[i], 0.1);
		}
		for (var i = 10; i < 15; i++) {
			assertEquals(8., result[i], 0.1);
		}
	}

	/**
	 * Test RRD4j step bigger than resolution, but resolution not divisible.
	 *
	 * @throws IOException        on error
	 * @throws URISyntaxException on error
	 */
	@Test
	public void testSplitUneven() throws IOException, URISyntaxException {
		var resolution = 300; // 1 minute

		var database = createRrdDb(720, 100, 100);
		var request = database.createFetchRequest(//
				ConsolFun.AVERAGE, //
				START.getEpochSecond(), //
				START.plus(3, ChronoUnit.HOURS).getEpochSecond());
		var result = Rrd4jSupplier.postProcessData(request.fetchData(), resolution);
		database.close();

		assertEquals(36, result.length); // 3 hours * 12 entries/per hour (5 minutes) = 36
		assertEquals(Double.NaN, result[0], 0.1);
		assertEquals(6.5, result[3], 0.1);
		assertEquals(6.5, result[4], 0.1);
		assertEquals(18.5, result[5], 0.1);
	}
}
