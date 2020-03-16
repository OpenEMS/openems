package io.openems.backend.metadata.odoo.postgres;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.postgresql.Driver;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;

import com.zaxxer.hikari.HikariDataSource;

import io.openems.backend.metadata.odoo.Config;
import io.openems.backend.metadata.odoo.EdgeCache;
import io.openems.backend.metadata.odoo.MetadataOdoo;
import io.openems.backend.metadata.odoo.MyEdge;
import io.openems.backend.metadata.odoo.postgres.task.InsertOrUpdateDeviceStates;
import io.openems.common.channel.Level;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.EdgeConfig.Component.Channel;
import io.openems.common.types.EdgeConfig.Component.Channel.ChannelDetail;
import io.openems.common.types.EdgeConfig.Component.Channel.ChannelDetailState;

public class PostgresHandler {

	protected final EdgeCache edgeCache;

	private final MetadataOdoo parent;
	private final HikariDataSource dataSource;
	private final InitializeEdgesWorker initializeEdgesWorker;
	private final PeriodicWriteWorker periodicWriteWorker;
	private final QueueWriteWorker queueWriteWorker;

	public PostgresHandler(MetadataOdoo parent, EdgeCache edgeCache, Config config, Runnable onInitialized)
			throws SQLException {
		this.parent = parent;
		this.edgeCache = edgeCache;
		this.dataSource = this.getDataSource(config);
		this.initializeEdgesWorker = new InitializeEdgesWorker(this, dataSource, () -> {
			onInitialized.run();
		});
		this.initializeEdgesWorker.start();
		this.periodicWriteWorker = new PeriodicWriteWorker(this, dataSource);
		this.periodicWriteWorker.start();
		this.queueWriteWorker = new QueueWriteWorker(this, dataSource);
		this.queueWriteWorker.start();
	}

	public void deactivate() {
		this.initializeEdgesWorker.stop();
		this.periodicWriteWorker.stop();
		this.queueWriteWorker.stop();
	}

	/**
	 * Gets the Edge for an API-Key, i.e. authenticates the API-Key.
	 * 
	 * @param apikey the API-Key
	 * @return the Edge or Empty
	 */
	public Optional<MyEdge> getEdgeForApikey(String apikey) {
		return Optional.ofNullable(this.edgeCache.getEdgeForApikey(apikey));
	}

	/**
	 * Updates the Device States table.
	 * 
	 * @param edge                the Edge
	 * @param activeStateChannels the active State-Channels
	 */
	public synchronized void updateDeviceStates(MyEdge edge, Map<ChannelAddress, Channel> activeStateChannels) {
		/*
		 * Update the EdgeDeviceState table
		 */
		List<InsertOrUpdateDeviceStates.DeviceState> deviceStates = new ArrayList<>();
		for (Entry<ChannelAddress, Channel> entry : activeStateChannels.entrySet()) {
			ChannelDetail detail = entry.getValue().getDetail();
			if (!(detail instanceof ChannelDetailState)) {
				continue;
			}
			Level level = ((ChannelDetailState) detail).getLevel();
			ChannelAddress channelAddress = entry.getKey();
			Channel channel = entry.getValue();
			String stateChannelName;
			if (!channel.getText().isEmpty()) {
				stateChannelName = channel.getText();
			} else {
				stateChannelName = channel.getId();
			}
			deviceStates.add(new InsertOrUpdateDeviceStates.DeviceState(channelAddress, level, stateChannelName));
		}

		// Add this Task to the write queue
		InsertOrUpdateDeviceStates task = new InsertOrUpdateDeviceStates(edge.getOdooId(),
				Timestamp.valueOf(LocalDateTime.now(ZoneOffset.UTC)), deviceStates);
		this.queueWriteWorker.addTask(task);

		// Update Sum-State from time to time
		this.periodicWriteWorker.triggerUpdateEdgeStatesSum(edge);
	}

	public PeriodicWriteWorker getPeriodicWriteWorker() {
		return this.periodicWriteWorker;
	}

	public QueueWriteWorker getQueueWriteWorker() {
		return this.queueWriteWorker;
	}

	/**
	 * Creates a {@link HikariDataSource} connection pool.
	 * 
	 * @param config the configuration
	 * @return the HikariDataSource
	 * @throws SQLException on error
	 */
	private HikariDataSource getDataSource(Config config) throws SQLException {
		if (!Driver.isRegistered()) {
			Driver.register();
		}
		PGSimpleDataSource pgds = new PGSimpleDataSource();
		pgds.setServerNames(new String[] { config.pgHost() });
		pgds.setPortNumbers(new int[] { config.pgPort() });
		pgds.setDatabaseName(config.database());
		pgds.setUser(config.pgUser());
		if (config.pgPassword() != null) {
			pgds.setPassword(config.pgPassword());
		}
		HikariDataSource result = new HikariDataSource();
		result.setDataSource(pgds);
		return result;
	}

	protected void logInfo(Logger log, String message) {
		this.parent.logInfo(log, message);
	}

	protected void logWarn(Logger log, String message) {
		this.parent.logWarn(log, message);
	}

	protected void logError(Logger log, String message) {
		this.parent.logError(log, message);
	}
}
