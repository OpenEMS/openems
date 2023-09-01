package io.openems.edge.timedata.rrd4j.version;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.rrd4j.ConsolFun;
import org.rrd4j.core.RrdBackendFactory;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdMemoryBackendFactory;

import io.openems.common.channel.Unit;
import io.openems.edge.timedata.rrd4j.version.Version.CreateDatabaseConfig;

public class Version3Test {

	// starts at 1. January 2020 00:00:00
	private static final Instant START = Instant.ofEpochSecond(1577836800L);

	/**
	 * Creates a dummy {@link Version2}.
	 * 
	 * @return the dummy version
	 */
	public static Version2 createDummyVersion2() {
		return new Version2(VersionTest.createDummyVersionComponentContext(2), //
				new Version2.StaticConfigurationConstants(//
						144, // 1 steps (5 minutes), 144 rows (1/2 day)
						24 // 1 step (60 minutes), 24 rows (1 day)
				));
	}

	/**
	 * Creates a dummy {@link Version3}.
	 * 
	 * @return the dummy version
	 */
	public static Version3 createDummyVersion3() {
		return new Version3(VersionTest.createDummyVersionComponentContext(3), //
				new Version3.StaticConfigurationConstants(// s
						24, // 1 steps (60 minutes), 24 rows (1 day)
						144 // 1 step (5 minutes), 144 rows (1/2 day)
				));
	}

	private RrdBackendFactory factory;
	private Version3 version3;

	@Before
	public void setUp() throws Exception {
		this.factory = new RrdMemoryBackendFactory();
		this.version3 = createDummyVersion3();
	}

	private RrdDb createDb() throws IOException {
		return this.version3.createNewDb(this.createDefaultDatabaseConfigForCumulatedValues());
	}

	private CreateDatabaseConfig createDefaultDatabaseConfigForCumulatedValues() {
		return new CreateDatabaseConfig(//
				"rrd4j0", //
				Unit.CUMULATED_WATT_HOURS, //
				"path", //
				START.getEpochSecond(), //
				this.factory, //
				null //
		);
	}

	private CreateDatabaseConfig createDefaultDatabaseConfigForAverageValues() {
		return new CreateDatabaseConfig(//
				"rrd4j0", //
				Unit.WATT_HOURS, //
				"path", //
				START.getEpochSecond(), //
				this.factory, //
				null //
		);
	}

	@Test
	public void testCreateNewCumulatedDb() throws Exception {
		try (var database = this.createDb()) {
			assertNotNull(database);
		}
	}

	@Test
	public void testMigrateAverageDb() throws Exception {
		final var version2 = createDummyVersion2();
		final var config = this.createDefaultDatabaseConfigForAverageValues();
		try (final var db = version2.createNewDb(config)) {
			// write dummy values
			final var end = START.plusSeconds(60 * 60).getEpochSecond();
			for (long i = START.getEpochSecond() + 1; i < end; i++) {
				db.createSample(i) //
						.setValue(0, i) //
						.update();
			}

			final var migratedDb = this.version3.migrate(db, config);

			final var dbv2Result = db.createFetchRequest(ConsolFun.AVERAGE, START.getEpochSecond(), end).fetchData();
			final var dbv3Result = migratedDb.createFetchRequest(ConsolFun.AVERAGE, START.getEpochSecond(), end)
					.fetchData();

			assertArrayEquals(dbv2Result.getValues()[0], dbv3Result.getValues()[0], 0.0);
			assertArrayEquals(dbv2Result.getTimestamps(), dbv3Result.getTimestamps());
		}
	}

	@Test
	public void testMigrateCumulatedDb() throws Exception {
		final var version2 = createDummyVersion2();
		final var config = this.createDefaultDatabaseConfigForCumulatedValues();
		try (final var db = version2.createNewDb(config)) {
			// writing test data from 04:00 to 06:00
			for (var hour : List.of(4, 5, 6)) {
				for (int i = 60 * 60 * hour + 1; i <= 60 * 60 * (hour + 1); i++) {
					db.createSample(START.getEpochSecond() + i) //
							.setValue(0, hour) //
							.update();
				}
			}
			final var migratedDb = this.version3.migrate(db, config);

			// check if data got applied correctly
			final var request = migratedDb.createFetchRequest(ConsolFun.MAX, START.getEpochSecond(),
					START.getEpochSecond() + 60 * 60 * 12);
			final var requestData = request.fetchData();

			final var values = requestData.getValues()[0];
			final var timestamps = requestData.getTimestamps();

			var hour4Value = Double.NaN;
			var hour5Value = Double.NaN;
			var hour6Value = Double.NaN;
			for (int i = 0; i < values.length; i++) {
				final var data = values[i];
				final var timestamp = timestamps[i];

				if ((timestamp - START.getEpochSecond()) / 60 / 60 == 4) {
					assertTrue(Double.isNaN(hour4Value));
					hour4Value = data;
					continue;
				}
				if ((timestamp - START.getEpochSecond()) / 60 / 60 == 5) {
					assertTrue(Double.isNaN(hour5Value));
					hour5Value = data;
					continue;
				}
				if ((timestamp - START.getEpochSecond()) / 60 / 60 == 6) {
					assertTrue(Double.isNaN(hour6Value));
					hour6Value = data;
					continue;
				}

				assertTrue("Got: " + data + ", expected NaN.", Double.isNaN(data));
			}
			// check if the written hour values match the expected hour values
			assertEquals(4.0, hour4Value, 0.0);
			assertEquals(5.0, hour5Value, 0.0);
			assertEquals(6.0, hour6Value, 0.0);
		}
	}

}
