package io.openems.backend.metadata.odoo.postgres;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.TreeMultimap;

import io.openems.backend.metadata.api.Metadata;
import io.openems.backend.metadata.odoo.Config;
import io.openems.backend.metadata.odoo.EdgeCache;
import io.openems.backend.metadata.odoo.Field.EdgeDeviceStatus;
import io.openems.backend.metadata.odoo.MetadataOdoo;
import io.openems.backend.metadata.odoo.MyEdge;
import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.EdgeConfig.Component.Channel;
import io.openems.common.types.EdgeConfig.Component.Channel.ChannelDetail;
import io.openems.common.types.EdgeConfig.Component.Channel.ChannelDetailState;

public class PostgresHandler {

	private final Logger log = LoggerFactory.getLogger(PostgresHandler.class);
	private final MetadataOdoo parent;
	private final EdgeCache edgeCache;
	private final Credentials credentials;
	private final CompletableFuture<Void> initializeEdgesTask;
	private final MyConnection connection;

	public PostgresHandler(MetadataOdoo parent, EdgeCache edgeCache, Config config) {
		this.parent = parent;
		this.edgeCache = edgeCache;
		this.credentials = Credentials.fromConfig(config);
		this.connection = new MyConnection(credentials);

		// Initialize EdgeCache
		this.initializeEdgesTask = CompletableFuture.runAsync(() -> {

			/**
			 * Reads all Edges from Postgres and puts them in a local Cache.
			 */
			try {
				this.parent.logInfo(this.log, "Caching Edges from Postgres");
				ResultSet rs = this.connection.psQueryAllEdges().executeQuery();
				for (int i = 0; rs.next(); i++) {
					if (i % 100 == 0) {
						this.parent.logInfo(this.log, "Caching Edges from Postgres. Finished [" + i + "]");
					}
					try {
						this.edgeCache.addOrUpate(rs);
					} catch (Exception e) {
						this.parent.logError(this.log,
								"Unable to read Edge: " + e.getClass().getSimpleName() + ". " + e.getMessage());
						e.printStackTrace();
					}
				}

			} catch (Exception e) {
				this.parent.logError(this.log,
						"Unable to initialize Edges: " + e.getClass().getSimpleName() + ". " + e.getMessage());
				e.printStackTrace();
			}

			this.parent.logInfo(this.log, "Caching Edges from Postgres finished");
		});
	}

	public void deactivate() {
		this.initializeEdgesTask.cancel(true);
		this.connection.deactivate();
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

	private final Map<MyEdge, LocalDateTime> lastWriteDeviceStates = new HashMap<>();

	/**
	 * Updates the Device States table.
	 * 
	 * @param edge                the Edge
	 * @param activeStateChannels the active State-Channels
	 */
	public synchronized void updateDeviceStates(MyEdge edge, Map<ChannelAddress, Channel> activeStateChannels) {
		LocalDateTime lastWriteDeviceStates = this.lastWriteDeviceStates.get(edge);
		if (lastWriteDeviceStates != null && lastWriteDeviceStates.isAfter(LocalDateTime.now().minusMinutes(1))) {
			// do not write more often than once per minute
			return;
		}
		this.lastWriteDeviceStates.put(edge, LocalDateTime.now());

		try {
			/*
			 * Update the EdgeDeviceState table
			 */
			{
				PreparedStatement ps = this.connection.psInsertOrUpdateDeviceState();
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
					// device_id
					ps.setInt(1, edge.getOdooId());
					// channel_address
					ps.setString(2, channelAddress.toString());
					// level
					ps.setString(3, level.name().toLowerCase());
					// component_id
					ps.setString(4, channelAddress.getComponentId());
					// channel_name
					ps.setString(5, stateChannelName);
					// last_appearance
					ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now(ZoneOffset.UTC)));

					ps.execute();
				}
			}
			/*
			 * Query non-acknowledged states
			 */
			Level highestLevel = Level.OK;
			String stateChannels;
			{
				PreparedStatement ps = this.connection.psQueryNotAcknowledgedDeviceStates();
				ps.setInt(1, edge.getOdooId());
				ResultSet rs = ps.executeQuery();
				TreeMap<Level, TreeMultimap<String, String>> levels = new TreeMap<>(
						(l1, l2) -> Integer.compare(l1.getValue(), l2.getValue() * -1));
				while (rs.next()) {
					// Parse ResultSet
					Level level = Level.valueOf(PgUtils.getAsString(rs, EdgeDeviceStatus.LEVEL).toUpperCase());

					if (level == Level.OK) {
						// ignore OK-Channels; no need to acknowledge them
						continue;
					}

					String componentId = PgUtils.getAsString(rs, EdgeDeviceStatus.COMPONENT_ID);
					String channelName = PgUtils.getAsString(rs, EdgeDeviceStatus.CHANNEL_NAME);

					// Update highest level
					if (level.getValue() > highestLevel.getValue()) {
						highestLevel = level;
					}

					// Add StateChannel to Map
					TreeMultimap<String, String> componentIds = levels.get(level);
					if (componentIds == null) {
						componentIds = TreeMultimap.create();
						levels.put(level, componentIds);
					}
					componentIds.put(componentId, channelName);
				}

				// Generate State-Channels-String
				stateChannels = levels.entrySet().stream().map(levelsEntry -> {
					return levelsEntry.getKey().name().toUpperCase() + " " //
							+ levelsEntry.getValue().asMap().entrySet().stream().map(componentIdsEntry -> {
								return componentIdsEntry.getKey() + ": "
										+ String.join(", ", componentIdsEntry.getValue());
							}).collect(Collectors.joining(";"));
				}).collect(Collectors.joining(" "));
			}
			/*
			 * Update Edge openems_sum_state_level and openems_sum_state_text
			 */
			{
				PreparedStatement ps = this.connection.psUpdateEdgeState();
				// openems_sum_state_level
				ps.setString(1, highestLevel.name().toLowerCase());
				// openems_sum_state_text
				ps.setString(2, stateChannels);
				// device_id
				ps.setInt(3, edge.getOdooId());

				ps.execute();
			}
		} catch (SQLException | OpenemsException e) {
			this.log.error("Unable to update Device-States: " + e.getMessage() + "; for: "
					+ Metadata.activeStateChannelsToString(activeStateChannels));
		}
	}
}
