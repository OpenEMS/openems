package io.openems.edge.timedata.rrd4j.version;

import java.io.IOException;
import java.util.Comparator;

import org.rrd4j.core.RrdBackendFactory;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDbPool;
import org.rrd4j.core.RrdDef;

import io.openems.common.channel.Unit;

/**
 * Represents a data model version of the rrd4j database files.
 * 
 * <p>
 * Note: does not represent the version of the rrd db which you can set via
 * {@link RrdDef#setVersion(int)}.
 * 
 * <p>
 * Versions are built to run after another. e.g. if someone writes a version 4
 * this version need to migrate data from version 3.
 */
public interface Version {

	/**
	 * Creates a {@link Comparator}, which sorts the versions by its version number
	 * ascending. The number can be obtained with {@link Version#getVersion()}.
	 * 
	 * @return the {@link Comparator}
	 */
	public static Comparator<Version> numberComparator() {
		return (o1, o2) -> o1.getVersion() - o2.getVersion();
	}

	public static record CreateDatabaseConfig(//
			String rrdDbId, //
			Unit channelUnit, //
			String path, //
			long startTime, //
			RrdBackendFactory factory, //
			RrdDbPool pool //
	) {

		/**
		 * Returns a new {@link CreateDatabaseConfig} with the given start time and the
		 * other attributes copied from this instance.
		 * 
		 * @param startTime the new start time
		 * @return the new {@link CreateDatabaseConfig}
		 */
		public CreateDatabaseConfig withStartTime(long startTime) {
			return new CreateDatabaseConfig(//
					this.rrdDbId, //
					this.channelUnit, //
					this.path, //
					startTime, //
					this.factory, //
					this.pool //
			);
		}

		/**
		 * Returns a new {@link CreateDatabaseConfig} with the given pool and the other
		 * attributes copied from this instance.
		 * 
		 * @param pool the new pool
		 * @return the new {@link CreateDatabaseConfig}
		 */
		public CreateDatabaseConfig withPool(RrdDbPool pool) {
			return new CreateDatabaseConfig(//
					this.rrdDbId, //
					this.channelUnit, //
					this.path, //
					this.startTime, //
					this.factory, //
					pool //
			);
		}

	}

	/**
	 * Gets the version number of this {@link Version}.
	 * 
	 * @return the version number
	 */
	public int getVersion();

	/**
	 * Creates a new database with the given {@link CreateDatabaseConfig}.
	 * 
	 * @param config the configuration to create the database
	 * @return the created database
	 * @throws IOException on I/O-Error
	 */
	public abstract RrdDb createNewDb(CreateDatabaseConfig config) throws IOException;

	/**
	 * Migrates the old database into a new one by creating a temporary file
	 * database of the {@link CreateDatabaseConfig}.
	 * 
	 * @param oldDb  the old database instance of the previous version
	 * @param config the {@link CreateDatabaseConfig} to create a new database
	 * @return the migrated data in the new database
	 * @throws IOException on I/O-Error
	 */
	public abstract RrdDb migrate(RrdDb oldDb, CreateDatabaseConfig config) throws IOException;
}
