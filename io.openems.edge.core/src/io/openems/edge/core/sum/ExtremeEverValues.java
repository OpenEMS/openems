package io.openems.edge.core.sum;

import static io.openems.common.OpenemsConstants.PROPERTY_LAST_CHANGE_AT;
import static io.openems.common.OpenemsConstants.PROPERTY_LAST_CHANGE_BY;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.Sum;

/**
 * Holds maximum/minimum ever experienced values, handles updating the Channel
 * and the Core.Sum configuration for persistence.
 */
public class ExtremeEverValues {

	/**
	 * Create a {@link ExtremeEverValues} builder.
	 *
	 * @param pid the configuration PID
	 * @return a {@link Builder}
	 */
	public static Builder create(String pid) {
		return new Builder(Clock.systemDefaultZone(), pid);
	}

	/**
	 * Create a {@link ExtremeEverValues} builder with a mocked {@link Clock}.
	 *
	 * @param clock a mocked {@link Clock}
	 * @param pid   the configuration PID
	 * @return a {@link Builder}
	 */
	public static Builder create(Clock clock, String pid) {
		return new Builder(clock, pid);
	}

	public static class Builder {
		private final Clock clock;
		private final String pid;
		private final List<ExtremeEverValue> entries = new ArrayList<>();

		public Builder(Clock clock, String pid) {
			this.clock = clock;
			this.pid = pid;
		}

		/**
		 * Adds an Entry.
		 * 
		 * @param targetChannelId the target {@link ChannelId} that always holds the
		 *                        Extreme-Ever-Value.
		 * @param configProperty  the name of the config property that persists this
		 *                        value
		 * @param range           the {@link Range} to identify a extreme value
		 * @param sourceChannelId the source {@link ChannelId} that provides a new value
		 * @return the {@link Builder}
		 */
		public Builder add(ChannelId targetChannelId, String configProperty, Range range, ChannelId sourceChannelId) {
			this.entries.add(new ExtremeEverValue(targetChannelId, configProperty, range, sourceChannelId));
			return this;
		}

		public ExtremeEverValues build() {
			return new ExtremeEverValues(this.clock, this.pid, this.entries.toArray(ExtremeEverValue[]::new));
		}
	}

	private final Logger log = LoggerFactory.getLogger(ExtremeEverValues.class);
	private final Clock clock;
	private final String pid;
	private final ExtremeEverValue[] entries;

	private Instant lastConfigUpdate;

	private ExtremeEverValues(Clock clock, String pid, ExtremeEverValue[] entries) {
		this.clock = clock;
		this.pid = pid;
		this.entries = entries;
		this.lastConfigUpdate = Instant.now(clock);
	}

	/**
	 * Initializes the values from {@link ComponentContext}.
	 * 
	 * @param context the {@link ComponentContext}
	 */
	public void initializeFromContext(ComponentContext context) {
		for (var entry : this.entries) {
			entry.initializeFromContext(context);
		}
	}

	/**
	 * Returns true if 24 hours passed since last config update.
	 * 
	 * @return true if config should be updated
	 */
	private boolean isDueForConfigUpdate() {
		return this.lastConfigUpdate.plus(24, ChronoUnit.HOURS).isBefore(Instant.now(this.clock));
	}

	/**
	 * Updates the values from {@link Sum}-Channels and the Component Configuration
	 * (if due).
	 * 
	 * @param component the {@link OpenemsComponent} that holds source and target
	 *                  Channels.
	 * @param cm        the {@link ConfigurationAdmin} to persist
	 *                  Extreme-Ever-Values
	 */
	public synchronized void update(OpenemsComponent component, ConfigurationAdmin cm) {
		for (var entry : this.entries) {
			entry.updateFromChannel(component);
		}

		if (!this.isDueForConfigUpdate()) {
			return;
		}

		this.lastConfigUpdate = Instant.now(this.clock);
		Map<String, Object> configUpdates = new HashMap<>();
		for (var entry : this.entries) {
			var value = entry.getConfigUpdate();
			if (value != null) {
				configUpdates.put(entry.configProperty, value);
			}
		}
		if (configUpdates.isEmpty()) {
			return;
		}

		try {
			var c = cm.getConfiguration(this.pid, "?");
			var properties = c.getProperties();
			configUpdates.forEach((key, value) -> properties.put(key, value));
			properties.put(PROPERTY_LAST_CHANGE_BY, "Internal: ExtremeEverValues");
			properties.put(PROPERTY_LAST_CHANGE_AT,
					LocalDateTime.now(this.clock).truncatedTo(ChronoUnit.SECONDS).toString());
			c.update(properties);
		} catch (IOException | SecurityException e) {
			this.log.error("ERROR: " + e.getMessage());
		}
	}

	private static class ExtremeEverValue {
		private final ChannelId targetChannelId;
		private final String configProperty;
		private final Range range;
		private final ChannelId sourceChannelId;

		private int configValue = 0;
		private int actualValue = 0;

		private ExtremeEverValue(ChannelId targetChannelId, String configProperty, Range range,
				ChannelId sourceChannelId) {
			this.targetChannelId = targetChannelId;
			this.configProperty = configProperty;
			this.range = range;
			this.sourceChannelId = sourceChannelId;
		}

		private synchronized void initializeFromContext(ComponentContext context) {
			this.configValue = this.actualValue = (int) context.getProperties().get(this.configProperty);
		}

		private synchronized void updateFromChannel(OpenemsComponent component) {
			var source = ((IntegerReadChannel) component.channel(this.sourceChannelId)).getNextValue().get();
			if (source != null && switch (this.range) {
			case NEGATIVE -> source < this.actualValue;
			case POSTIVE -> source > this.actualValue;
			}) {
				// New extreme value appeared
				this.actualValue = source;
			}

			// Update target Channel
			component.channel(this.targetChannelId).setNextValue(this.actualValue);
		}

		private Object getConfigUpdate() {
			if (this.actualValue != this.configValue) {
				return this.actualValue;
			}
			return null;
		}
	}

	public static enum Range {
		NEGATIVE, POSTIVE;
	}
}
