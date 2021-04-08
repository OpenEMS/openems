package io.openems.edge.timedata.mssql;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import com.microsoft.sqlserver.jdbc.SQLServerException;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Timedata.Mssql", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
)
public class TimedataMssqlImpl extends AbstractOpenemsComponent 
		implements TimedataMssql, Timedata, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(TimedataMssqlImpl.class);
	
	private Connection mssqlConnection = null;
	
	// Counts the number of Cycles till data is written to MS-SQL DB.
	private int cycleCount = 0;
	
	// setting up the config variable
	private Config config = null;
	
	// variables for thread pool executor
	private static final int EXECUTOR_MIN_THREADS = 1;
	private static final int EXECUTOR_MAX_THREADS = 50;
	private static final int EXECUTOR_QUEUE_SIZE = 100;
	private ThreadPoolExecutor executor = null;
	private ScheduledExecutorService debugLogExecutor = null;
	
	// Initializing the prepared statement object list 
	private List<PreparedStatement> allPreparedStatements = new ArrayList<PreparedStatement>();
		
	public TimedataMssqlImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				TimedataMssql.ChannelId.values() //
		);
	}
	
	@Reference
	protected ComponentManager componentManager;
	
	@Activate
	void activate(ComponentContext context, Config config) throws SQLException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		
		// 1. setting up the thread pool executor
		this.executor = new ThreadPoolExecutor(EXECUTOR_MIN_THREADS, EXECUTOR_MAX_THREADS, 60L,
				TimeUnit.SECONDS, new ArrayBlockingQueue<>(EXECUTOR_QUEUE_SIZE), new ThreadPoolExecutor.DiscardPolicy());
		this.debugLogExecutor = Executors.newSingleThreadScheduledExecutor();
		
		// 2. Thread pool monitor monitor
		this.debugLogExecutor.scheduleWithFixedDelay(() -> {
			int queueSize = this.executor.getQueue().size();
			this.log.info(String.format("[monitor] Pool: %d, Active: %d, Pending: %d, Completed: %d %s",
					this.executor.getPoolSize(), //
					this.executor.getActiveCount(), //
					this.executor.getQueue().size(), //
					this.executor.getCompletedTaskCount(), //
					(queueSize == EXECUTOR_QUEUE_SIZE) ? "!!!BACKPRESSURE!!!" : "")); //
		}, 10, 10, TimeUnit.SECONDS);
		
		// 3. setting up the connection
		if (this.mssqlConnection == null) {
			SQLServerDataSource MssqlDB = new SQLServerDataSource();
			// Source:
			// https://docs.microsoft.com/de-de/sql/connect/jdbc/data-source-sample?view=sql-server-ver15
			MssqlDB.setServerName(config.server());
			MssqlDB.setDatabaseName(config.dbname());
			MssqlDB.setPortNumber(config.port());
			if (config.isPasswordProtected()) {
				MssqlDB.setUser(config.username());
				MssqlDB.setPassword(config.password());
			}
			try {
				this.mssqlConnection = MssqlDB.getConnection();
			} catch (SQLServerException e) {
				log.warn("Unable to establish connection with MS-SQL DB. Exception: "+e.getMessage());
			}
		}
		
		// now check the what kind of connection it is and initiate the prepared statements
		// Please note: this would be a better method of implementation if this is intended as a one-long
		// living connection. 
		
		// sample prepared statement for testing 
		String query =  "INSERT INTO mda.Wt$LiveData$Test"+
				   		"(WsDateTime, WsDateTimeTicks, "+
				   		"WsCh_000, WsCh_001, WsCh_002, WsCh_003,"+
				   		"WsCh_004, WsCh_005, WsCh_006, WsCh_007,"+
				   		"WsCh_008, WsCh_009) " +
				   		" VALUES(?,"+// COLUMN: WsDateTime
				   		"?,"+// COLUMN: WsDateTimeTicks
				   		"?,"+// COLUMN: WsCh_000
				   		"?,"+// COLUMN: WsCh_001
				   		"?,"+// COLUMN: WsCh_002
				   		"?,"+// COLUMN: WsCh_003
				   		"?,"+// COLUMN: WsCh_004
				   		"?,"+// COLUMN: WsCh_005
				   		"?,"+// COLUMN: WsCh_006
				   		"?,"+// COLUMN: WsCh_007
				   		"?,"+// COLUMN: WsCh_008
				   		"?)";// COLUMN: WsCh_009
		
		this.allPreparedStatements.add(this.mssqlConnection.prepareStatement(query)); 
		
		this.config = config;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		
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
		if (this.mssqlConnection!= null) {
			try {
				this.mssqlConnection.close();
				// must also close all the prepared statements here
				
			} catch (SQLException e) {
				log.warn("Unable to close MS-SQL DB. Exception: " + e.getMessage());
			}
		}
	}
	
	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			if (this.config.isReadOnly()) {
				log.info("This connection is Read-Only mode. Skipping measurements");
			} else {
				this.collectAndWriteChannelValues();
				this.testRead();
			}
			break;
		}
	}
	
	protected synchronized void collectAndWriteChannelValues() {
		
		if (this.cycleCount >= this.config.noOfCycles()) {
			this.cycleCount = 0;  // resetting the cycle count value
		}
		
		int count = 0;
		Iterator<PreparedStatement> preparedStatementsIterator = this.allPreparedStatements.iterator();
		Random random = new Random();
		while (preparedStatementsIterator.hasNext()) {
			count ++;
			try {
				this.mssqlConnection.setAutoCommit(false);
				PreparedStatement currentStatement = preparedStatementsIterator.next();
				currentStatement.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now(ZoneOffset.UTC)));
				currentStatement.setLong(2, Timestamp.valueOf(LocalDateTime.now(ZoneOffset.UTC)).toInstant().toEpochMilli());
				for (int n=1; n<=10; n++) {
					currentStatement.setFloat(n+2, random.nextFloat());
				}
				currentStatement.execute();
				this.mssqlConnection.commit();
				
			} catch (SQLException e){
				try {
					this.mssqlConnection.rollback();
				} catch (SQLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				log.warn("Unable to write measurement" + e.getMessage());
			}
		}
		log.info("Executed " + count + " prepared statements successfully!");
		//this.mssqlConnector.write(query);
		
		//Test read
		//this.mssqlConnector.testRead();
	}
	
	public void testRead() {
		try {
			ResultSet rs = this.mssqlConnection
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

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, int resolution)
			throws OpenemsNamedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(String edgeId, ZonedDateTime fromDate,
			ZonedDateTime toDate, Set<ChannelAddress> channels) throws OpenemsNamedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricEnergyPerPeriod(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, int resolution)
			throws OpenemsNamedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<Optional<Object>> getLatestValue(ChannelAddress channelAddress) {
		// TODO Auto-generated method stub
		return null;
	}
}
