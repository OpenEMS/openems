package io.openems.api.channel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.openems.api.exception.ConfigException;
import io.openems.api.thing.Thing;

public class ThingStateChannel extends ReadChannel<ThingState> implements ChannelChangeListener {

	private List<ReadChannel<Boolean>> warningChannels;
	private List<ReadChannel<Boolean>> faultChannels;
	private List<ThingStateChannel> childChannels;
	private Set<String> channelNames;

	public ThingStateChannel(Thing parent){
		super("State", parent);
		this.warningChannels = new ArrayList<>();
		this.faultChannels = new ArrayList<>();
		this.channelNames = new HashSet<>();
		this.childChannels = new ArrayList<>();
	}

	public void addWarningChannel(ReadChannel<Boolean> channel) throws ConfigException {
		if (!this.channelNames.contains(channel.address())) {
			this.warningChannels.add(channel);
			this.channelNames.add(channel.address());
			channel.addChangeListener(this);
		} else {
			throw new ConfigException("A channel with the name [" + channel.address() + "] is already registered!");
		}
	}

	public void removeWarningChannel(ReadChannel<Boolean> channel) {
		channel.removeChangeListener(this);
		this.channelNames.remove(channel.address());
		this.warningChannels.remove(channel);
	}

	public void addFaultChannel(ReadChannel<Boolean> channel) throws ConfigException {
		if (!this.channelNames.contains(channel.address())) {
			this.faultChannels.add(channel);
			this.channelNames.add(channel.address());
			channel.addChangeListener(this);
		} else {
			throw new ConfigException("A channel with the name [" + channel.address() + "] is already registered!");
		}
	}

	public void removeFaultChannel(ReadChannel<Boolean> channel) {
		channel.removeChangeListener(this);
		this.channelNames.remove(channel.address());
		this.faultChannels.remove(channel);
	}

	public List<ReadChannel<Boolean>> getWarningChannels() {
		List<ReadChannel<Boolean>> warningChannels = new ArrayList<>();
		warningChannels.addAll(this.warningChannels);
		for(ThingStateChannel child : this.childChannels) {
			warningChannels.addAll(child.getWarningChannels());
		}
		return warningChannels;
	}

	public List<ReadChannel<Boolean>> getFaultChannels() {
		List<ReadChannel<Boolean>> faultChannels = new ArrayList<>();
		faultChannels.addAll(this.faultChannels);
		for(ThingStateChannel child : this.childChannels) {
			faultChannels.addAll(child.getFaultChannels());
		}
		return this.faultChannels;
	}

	public void addChildChannel(ThingStateChannel child) {
		this.childChannels.add(child);
		child.addChangeListener(this);
	}

	public void removeChildChannel(ThingStateChannel child) {
		child.removeChangeListener(this);
		this.childChannels.add(child);
	}

	@Override
	public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
		for(ThingStateChannel child : this.childChannels) {
			if(child.isValuePresent()) {
				switch(child.getValue()) {
				case FAULT:
					updateValue(ThingState.FAULT);
					return;
				case WARNING:
					updateValue(ThingState.WARNING);
					return;
				default:
					break;
				}
			}
		}
		for (ReadChannel<Boolean> faultChannel : faultChannels) {
			if (faultChannel.isValuePresent() && faultChannel.getValue()) {
				updateValue(ThingState.FAULT);
				return;
			}
		}
		for (ReadChannel<Boolean> warningChannel : warningChannels) {
			if (warningChannel.isValuePresent() && warningChannel.getValue()) {
				updateValue(ThingState.WARNING);
				return;
			}
		}
		updateValue(ThingState.RUN);
	}

}
