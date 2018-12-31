package io.openems.edge.common.channel;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.doc.ChannelId;
import io.openems.edge.common.channel.doc.Level;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Collects the values of all {@link StateChannel}s. This class is used for the
 * "State" Channel of every OpenEMS Component.
 */
public class StateCollectorChannel extends AbstractReadChannel<Integer> {

	/**
	 * Holds all Channels that are connected to and collected by this
	 * StateCollectorChannel
	 */
	private final Map<io.openems.edge.common.channel.doc.ChannelId, Channel<?>> channels = Collections
			.synchronizedMap(new HashMap<>());

	/**
	 * Holds Channels that have an active (true) value.
	 */
	private final Multimap<Level, io.openems.edge.common.channel.doc.ChannelId> activeStates = HashMultimap.create();

	public StateCollectorChannel(OpenemsComponent parent, ChannelId channelId) {
		super(OpenemsType.INTEGER, parent, channelId);
	}
	
	@Override
	public Value<Integer> value() {
		Value<Integer> result = super.value();
		return result;
	}

	private final BiConsumer<StateChannel, Value<Boolean>> onChangeFunction = (channel, value) -> {
		/*
		 * update activeStates
		 */
		Level channelLevel = channel.channelDoc().getLevel();
		if (value != null && value.orElse(false)) {
			// Value is true -> add to activeStates
			this.activeStates.put(channelLevel, channel.channelId());
		} else {
			// Value is false or unknown -> remove from activeStates
			this.activeStates.remove(channelLevel, channel.channelId());
		}

		/*
		 * Set my own next value according to activeStates.
		 * 
		 * Higher value of Level beats lower value.
		 */
		int nextValue = 0;
		for (Level level : Level.values()) {
			if (this.activeStates.get(level).size() > 0) {
				nextValue = Math.max(nextValue, level.getValue());
			}
		}
		this.setNextValue(nextValue);
	};

	/**
	 * Adds a Channel to this StateCollector.
	 * 
	 * @param channel the Channel
	 */
	public void addChannel(StateChannel channel) {
		this.channels.put(channel.channelId(), channel);

		channel.onChange(value -> {
			this.onChangeFunction.accept(channel, value);
		});
	}

	/**
	 * Removes a Channel from this StateCollector.
	 * <p>
	 * The onChange listener is removed by the {@link Channel#deactivate()} method.
	 * 
	 * @param channel the Channel
	 */
	public void removeChannel(StateChannel channel) {
		this.channels.remove(channel.channelId());
		this.onChangeFunction.accept(channel, null);
	}

	public String listStates() {
		return this.listStates(Level.INFO);
	}

	public String listStates(Level fromLevel) {
		StringBuilder result = new StringBuilder();
		for (Level level : Level.values()) {
			if (level.ordinal() < fromLevel.ordinal()) {
				// filter levels below 'fromLevel'
				continue;
			}
			Collection<ChannelId> channelIds = this.activeStates.get(level);
			if (channelIds.size() > 0) {
				if (result.length() > 0) {
					result.append("| ");
				}
				result.append(level.name() + ": ");
				for (ChannelId channelId : channelIds) {
					result.append(this.parent.channel(channelId).channelDoc().getText() + ",");
				}
			}
		}
		return result.toString();
	}
}
