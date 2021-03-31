package io.openems.shared.mssql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.osgi.annotation.versioning.ProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.StringUtils;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import com.microsoft.sqlserver.jdbc.SQLServerException;

public class MssqlConnector {
	
	private static final int EXECUTOR_MIN_THREADS = 1;
	private static final int EXECUTOR_MAX_THREADS = 50;
	private static final int EXECUTOR_QUEUE_SIZE = 100;
	
	private final Logger log = LoggerFactory.getLogger(MssqlConnector.class);
	
	private final String server;
	private final String dbname;
	private final int port;
	private final String username;
	private final String password;
	private final boolean isPasswordProtected;
	private final boolean isReadOnly;
	
	private final ThreadPoolExecutor executor = new ThreadPoolExecutor(EXECUTOR_MIN_THREADS, EXECUTOR_MAX_THREADS, 60L,
			TimeUnit.SECONDS, new ArrayBlockingQueue<>(EXECUTOR_QUEUE_SIZE), new ThreadPoolExecutor.DiscardPolicy());
	private final ScheduledExecutorService debugLogExecutor = Executors.newSingleThreadScheduledExecutor();
	
	/**
	 * The constructor for a MS-SQL connection
	 * 
	 * @param server				Server a.k.a host name of the connection 
	 * @param dbname				Database name
	 * @param port					Port number in which the MS-SQL DB is available
	 * @param username				User name for logging in
	 * @param password				Password for logging in
	 * @param isPasswordProtected	Is the DB password protected? then the inputs are handled differently
	 * @param isReadOnly			Is the database supposed to be read only?, then writing would be skipped.
	 */
	public MssqlConnector(String server, String dbname, int port, String username, String password, 
			boolean isPasswordProtected, boolean isReadOnly) {
		super();
		this.server = server;
		this.dbname = dbname;
		this.port = port;
		this.username = username;
		this.password = password;
		this.isPasswordProtected = isPasswordProtected;
		this.isReadOnly = isReadOnly;
		this.debugLogExecutor.scheduleWithFixedDelay(() -> {
			int queueSize = this.executor.getQueue().size();
			this.log.info(String.format("[monitor] Pool: %d, Active: %d, Pending: %d, Completed: %d %s",
					this.executor.getPoolSize(), //
					this.executor.getActiveCount(), //
					this.executor.getQueue().size(), //
					this.executor.getCompletedTaskCount(), //
					(queueSize == EXECUTOR_QUEUE_SIZE) ? "!!!BACKPRESSURE!!!" : "")); //
		}, 10, 10, TimeUnit.SECONDS);
	}
	
	private Connection _mssqlDB = null;
	
	/**
	 * Get the MS-SQL Connection
	 * @return	connection
	 */
	private Connection getConnection() {
		if (this._mssqlDB == null) {
			SQLServerDataSource MssqlDB = new SQLServerDataSource();
			// Source:
			// https://docs.microsoft.com/de-de/sql/connect/jdbc/data-source-sample?view=sql-server-ver15
			MssqlDB.setServerName(this.server);
			MssqlDB.setDatabaseName(this.dbname);
			MssqlDB.setPortNumber(this.port);
			if (this.isPasswordProtected) {
				MssqlDB.setUser(this.username);
				MssqlDB.setPassword(this.password);
			}
			try {
				this._mssqlDB = MssqlDB.getConnection();
			} catch (SQLServerException e) {
				log.warn("Unable to establish connection with MS-SQL DB. Exception: "+e.getMessage());
			}
		}
		return this._mssqlDB;
	}
	
	public void deactivate() {
		// shutdown executor
		if (this.executor != null) {
			try {
				this.executor.shutdown();
				this.executor.awaitTermination(5, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				this.log.warn("tasks interrupted");
			}finally {
				if (!this.executor.isTerminated()) {
					this.log.warn("cancel non-finished tasks");
				}
				this.executor.shutdownNow();
			}
		}
		if (this._mssqlDB != null) {
			try {
				this._mssqlDB.close();
			} catch (SQLException e) {
				log.warn("Unable to close MS-SQL DB. Exception: " + e.getMessage());
			}
		}
	}
	
	/**CODE TO BE CHECKED
	public void write(String query) throws OpenemsException {
		if (this.isReadOnly) {
			log.info("Read-Only-Mode is activated. Not writing measurement: " +
					StringUtils.toShortString(query, 100));
		}
		try {
			this.executor.execute(() -> {
				this.getConnection().createStatement().executeUpdate(query);
				this.getConnection().commit();
						});
		} catch (SQLException e) {
			this.getConnection().rollback();
			throw new OpenemsException("Unable to write point: " + e.getMessage());
		}
	}
	*/
	
	public void testRead() {
		try {
			ResultSet rs = this.getConnection()
							   .createStatement()
							   .executeQuery("SELECT TOP 1 * FROM mda.Wt$LiveData$Test ORDER BY WsDateTime DESC");

			while (rs.next()) {
				String output = "Latest data read from DB: [ID:"+ rs.getString("WsTID") + 
						", Timestamp: "+ rs.getDate("WsDateTime") +
						", Timestamp tick: "+ Integer.toString(rs.getInt("WsDateTimeTicks")) +
						", Ch_000: " + Float.toString(rs.getFloat("WsCh_000")) + 
						", Ch_001: " + Float.toString(rs.getFloat("WsCh_001")) +
						", Ch_002: " + Float.toString(rs.getFloat("WsCh_002")) + 
						", Ch_003: " + Float.toString(rs.getFloat("WsCh_003")) + 
						", Ch_004: " + Float.toString(rs.getFloat("WsCh_004")) +
						", Ch_005: " + Float.toString(rs.getFloat("WsCh_005")) +
						", Ch_006: " + Float.toString(rs.getFloat("WsCh_006")) + 
						", Ch_007: " + Float.toString(rs.getFloat("WsCh_007")) + 
						", Ch_008: " + Float.toString(rs.getFloat("WsCh_008")) +
						", Ch_009: " + Float.toString(rs.getFloat("WsCh_009")) +
						"]";
				log.info(output);
			}
		} catch (SQLException e) {
			log.warn("Oops! Some problem with the connections. Exeption: " + e.getMessage());
		}
	}
}
