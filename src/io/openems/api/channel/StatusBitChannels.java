package io.openems.api.channel;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

import io.openems.api.thing.Thing;

public class StatusBitChannels extends ReadChannel<Long> {
	private final Set<StatusBitChannel> channels = new HashSet<>();

	public StatusBitChannels(String id, Thing parent) {
		super(id, parent);
	}

	public StatusBitChannel channel(StatusBitChannel channel) {
		this.channels.add(channel);
		return channel;
	}

	public Set<String> labels() {
		Set<String> result = new HashSet<>();
		for (StatusBitChannel channel : channels) {
			result.addAll(channel.labels());
		}
		return result;
	}

	@Override public Optional<String> labelOptional() {
		Set<String> labels = this.labels();
		if (labels.isEmpty()) {
			return Optional.empty();
		} else {
			StringJoiner joiner = new StringJoiner(",");
			for (String label : labels) {
				joiner.add(label);
			}
			return Optional.of(joiner.toString());
		}
	};
}
