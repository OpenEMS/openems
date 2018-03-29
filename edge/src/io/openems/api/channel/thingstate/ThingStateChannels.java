package io.openems.api.channel.thingstate;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.ThingStateChannel;
import io.openems.api.doc.ChannelDoc;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.exception.ConfigException;
import io.openems.api.thing.Thing;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Role;
import io.openems.common.types.ChannelAddress;

public class ThingStateChannels extends ReadChannel<ThingState> implements ChannelChangeListener {

	private final Logger log = LoggerFactory.getLogger(ThingStateChannels.class);
	private List<ThingStateChannel> warningChannels;
	private List<ThingStateChannel> faultChannels;
	private Set<ChannelAddress> channelNames;

	public ThingStateChannels(Thing parent){
		super("State", parent);
		this.warningChannels = new ArrayList<>();
		this.faultChannels = new ArrayList<>();
		this.channelNames = new HashSet<>();
		updateState();
	}

	public void addWarningChannel(ThingStateChannel channel) throws ConfigException {
		if (!this.channelNames.contains(channel.address())) {
			this.warningChannels.add(channel);
			this.channelNames.add(channel.address());
			channel.addChangeListener(this);
			addDefaultChannelDoc(channel);
			updateState();
		} else {
			throw new ConfigException("A channel with the name [" + channel.address() + "] is already registered!");
		}
	}

	public void removeWarningChannel(ThingStateChannel channel) {
		channel.removeChangeListener(this);
		this.channelNames.remove(channel.address());
		this.warningChannels.remove(channel);
		updateState();
	}

	public void addFaultChannel(ThingStateChannel channel) throws ConfigException {
		if (!this.channelNames.contains(channel.address())) {
			this.faultChannels.add(channel);
			this.channelNames.add(channel.address());
			channel.addChangeListener(this);
			addDefaultChannelDoc(channel);
			updateState();
		} else {
			throw new ConfigException("A channel with the name [" + channel.address() + "] is already registered!");
		}
	}

	public void removeFaultChannel(ThingStateChannel channel) {
		channel.removeChangeListener(this);
		this.channelNames.remove(channel.address());
		this.faultChannels.remove(channel);
		updateState();
	}

	private void addDefaultChannelDoc(Channel channel) {
		ChannelDoc channelDoc = new ChannelDoc(null, channel.id(), Optional.of(new ChannelInfo() {

			@Override
			public Class<? extends Annotation> annotationType() {
				return ChannelInfo.class;
			}

			@Override
			public Role[] writeRoles() {
				return ChannelInfo.DEFAULT_WRITE_ROLES.toArray(new Role[ChannelInfo.DEFAULT_WRITE_ROLES.size()]);
			}

			@Override
			public Class<?> type() {
				return Boolean.class;
			}

			@Override
			public String title() {
				return "";
			}

			@Override
			public Role[] readRoles() {
				return ChannelInfo.DEFAULT_READ_ROLES.toArray(new Role[ChannelInfo.DEFAULT_READ_ROLES.size()]);
			}

			@Override
			public String description() {
				return "";
			}

			@Override
			public boolean isOptional() {
				return false;
			}

			@Override
			public boolean isArray() {
				return false;
			}

			@Override
			public String defaultValue() {
				return "";
			}

			@Override
			public String jsonSchema() {
				return "";
			}
		}));
		try {
			channel.setChannelDoc(channelDoc);
		} catch (OpenemsException e) {
			log.error(e.getMessage());
		}
	}

	public List<ThingStateChannel> getWarningChannels() {
		List<ThingStateChannel> warningChannels = new ArrayList<>();
		warningChannels.addAll(this.warningChannels);
		return warningChannels;
	}

	public List<ThingStateChannel> getFaultChannels() {
		List<ThingStateChannel> faultChannels = new ArrayList<>();
		faultChannels.addAll(this.faultChannels);
		return this.faultChannels;
	}

	@Override
	public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
		updateState();
	}

	private void updateState() {
		ThingState currentState = ThingState.RUN;
		for (ThingStateChannel faultChannel : faultChannels) {
			if (faultChannel.isValuePresent() && faultChannel.getValue()) {
				currentState = ThingState.FAULT;
			}
		}
		for (ThingStateChannel warningChannel : warningChannels) {
			if (warningChannel.isValuePresent() && warningChannel.getValue()&&currentState != ThingState.FAULT) {
				currentState = ThingState.WARNING;
			}
		}
		updateValue(currentState);
	}

}
