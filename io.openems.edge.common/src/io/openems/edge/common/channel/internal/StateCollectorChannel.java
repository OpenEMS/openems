package io.openems.edge.common.channel.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Collects the values of all {@link StateChannel}s. This class is used for the
 * "State" Channel of every OpenEMS Component.
 */
public class StateCollectorChannel extends EnumReadChannel {

	/**
	 * Holds all Channels that are connected to and collected by this
	 * StateCollectorChannel.
	 */
	private final Map<io.openems.edge.common.channel.ChannelId, Channel<?>> channels = Collections
			.synchronizedMap(new HashMap<>());

	/**
	 * Holds Channels that have an active (true) value.
	 */
	private final Multimap<Level, io.openems.edge.common.channel.ChannelId> activeStates = HashMultimap.create();

	protected StateCollectorChannel(OpenemsComponent parent, ChannelId channelId, StateCollectorChannelDoc channelDoc) {
		super(parent, channelId, channelDoc, Level.OK);
	}

	@Override
	public Value<Integer> value() {
		return super.value();
	}

	private final BiConsumer<StateChannel, Value<Boolean>> onChangeFunction = (channel, value) -> {
		/*
		 * update activeStates
		 */
		if (value != null && value.orElse(false)) {
			// Value is true -> add to activeStates
			this.activeStates.put(channel.getLevel(), channel.channelId());
		} else {
			// Value is false or unknown -> remove from activeStates
			this.activeStates.remove(channel.getLevel(), channel.channelId());
		}

		/*
		 * Set my own next value according to activeStates.
		 *
		 * Higher value of Level beats lower value.
		 */
		var nextValue = 0;
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

		channel.onChange((oldValue, newValue) -> {
			this.onChangeFunction.accept(channel, newValue);
		});
	}

	/**
	 * Removes a Channel from this StateCollector.
	 * 
	 * <p>
	 * The onChange listener is removed by the {@link Channel#deactivate()} method.
	 *
	 * @param channel the Channel
	 */
	public void removeChannel(StateChannel channel) {
		this.channels.remove(channel.channelId());
		this.onChangeFunction.accept(channel, null);
	}

	/**
	 * Lists all States as Text.
	 *
	 * @return the text
	 */
	public String listStates() {
		return this.listStates(Level.INFO);
	}

	/**
	 * Lists all States that are at least 'fromLevel' as text.
	 *
	 * @param fromLevel the minimum Level
	 * @return the text
	 */
	public String listStates(Level fromLevel) {
		var result = new StringBuilder();
		for (Level level : Level.values()) {
			if (level.ordinal() < fromLevel.ordinal()) {
				// filter levels below 'fromLevel'
				continue;
			}
			var channelIds = this.activeStates.get(level);
			if (channelIds.size() > 0) {
				if (result.length() > 0) {
					result.append("| ");
				}
				result.append(level.name() + ": ");
				result.append(channelIds.stream() //
						.map(channelId -> {
							var docText = this.parent.channel(channelId).channelDoc().getText();
							if (!docText.isEmpty()) {
								return docText;
							}
							return channelId.id();
						}) //
						.collect(Collectors.joining(",")));
			}
		}
		return result.toString();
	}
}
