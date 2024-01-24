package io.openems.edge.core.sum;

import java.util.Dictionary;

import org.osgi.service.component.ComponentContext;

import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.sum.Sum;

/**
 * Holds maximum/minimum ever experienced values, handles updating the Channel
 * and the Core.Sum configuration for persistence.
 */
public class ExtremeEverValue {

	public static enum Range {
		NEGATIVE, POSTIVE;
	}

	private final Sum.ChannelId targetChannelId;
	private final String configProperty;
	private final Range range;
	private final Sum.ChannelId sourceChannelId;

	private int value = 0;

	public ExtremeEverValue(Sum.ChannelId targetChannelId, String configProperty, Range range,
			Sum.ChannelId sourceChannelId) {
		this.targetChannelId = targetChannelId;
		this.configProperty = configProperty;
		this.range = range;
		this.sourceChannelId = sourceChannelId;
	}

	/**
	 * Initializes the {@link #value} from {@link ComponentContext}.
	 * 
	 * @param context the {@link ComponentContext}
	 */
	public synchronized void initializeFromContext(ComponentContext context) {
		this.value = (int) context.getProperties().get(this.configProperty);
	}

	/**
	 * Updates the {@link #value} from {@link Sum}-Channels.
	 * 
	 * @param sum the {@link Sum} component
	 * @return myself if the extreme value was updated
	 */
	public synchronized ExtremeEverValue updateFromChannel(Sum sum) {
		final ExtremeEverValue result;
		var source = ((IntegerReadChannel) sum.channel(this.sourceChannelId)).getNextValue().get();
		if (source != null && switch (this.range) {
		case NEGATIVE -> source < this.value;
		case POSTIVE -> source > this.value;
		}) {
			// New extreme value appeared
			this.value = source;
			// Trigger config update by returning myself
			result = this;

		} else {
			// No new extreme value appeared
			result = null;
		}

		// Update target Channel
		sum.channel(this.targetChannelId).setNextValue(this.value);

		return result;
	}

	/**
	 * Updates a {@link Config} Dictionary of Properties with the extreme value.
	 * 
	 * @param properties the Properties Dictionary
	 */
	public synchronized void updateConfig(Dictionary<String, Object> properties) {
		properties.put(this.configProperty, this.value);
	}
}
