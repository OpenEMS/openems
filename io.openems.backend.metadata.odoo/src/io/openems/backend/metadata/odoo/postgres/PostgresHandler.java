package io.openems.backend.metadata.odoo.postgres;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.slf4j.Logger;

import io.openems.backend.metadata.odoo.Config;
import io.openems.backend.metadata.odoo.EdgeCache;
import io.openems.backend.metadata.odoo.MetadataOdoo;
import io.openems.backend.metadata.odoo.MyEdge;
import io.openems.backend.metadata.odoo.postgres.task.InsertOrUpdateDeviceState;
import io.openems.backend.metadata.odoo.postgres.task.UpdateEdgeStatesSum;
import io.openems.common.channel.Level;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.EdgeConfig.Component.Channel;
import io.openems.common.types.EdgeConfig.Component.Channel.ChannelDetail;
import io.openems.common.types.EdgeConfig.Component.Channel.ChannelDetailState;

public class PostgresHandler {

	protected final EdgeCache edgeCache;

	private final MetadataOdoo parent;
	private final Credentials credentials;
	private final InitializeEdgesWorker initializeEdgesWorker;
	private final PeriodicWriteWorker periodicWriteWorker;
	private final QueueWriteWorker queueWriteWorker;

	public PostgresHandler(MetadataOdoo parent, EdgeCache edgeCache, Config config) {
		this.parent = parent;
		this.edgeCache = edgeCache;
		this.credentials = Credentials.fromConfig(config);
		this.initializeEdgesWorker = new InitializeEdgesWorker(this, this.credentials);
		this.initializeEdgesWorker.start();
		this.periodicWriteWorker = new PeriodicWriteWorker(this, this.credentials);
		this.periodicWriteWorker.start();
		this.queueWriteWorker = new QueueWriteWorker(this, this.credentials);
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
			InsertOrUpdateDeviceState task = new InsertOrUpdateDeviceState(edge.getOdooId(), channelAddress, level,
					stateChannelName, Timestamp.valueOf(LocalDateTime.now(ZoneOffset.UTC)));

			// Add this StateChannel to the write queue
			this.queueWriteWorker.addTask(task);
		}

		// Add "UpdateEdgeStates" task to write queue
		this.queueWriteWorker.addTask(new UpdateEdgeStatesSum(edge.getOdooId()));
	}

	public PeriodicWriteWorker getPeriodicWriteWorker() {
		return this.periodicWriteWorker;
	}

	public QueueWriteWorker getQueueWriteWorker() {
		return this.queueWriteWorker;
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
