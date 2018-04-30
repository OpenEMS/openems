package io.openems.edge.common.channel.converter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.Channel;

/**
 * Provides Functions to convert from Channel(s) to a Channel. Also has some
 * static convenience functions to facilitate conversion.
 */
public class ChannelConverter<T> {

	private final Logger log = LoggerFactory.getLogger(ChannelConverter.class);

	private final Function<Collection<T>, T> function;
	private final Map<Channel<T>, T> lastValues = new HashMap<>();

	public ChannelConverter(Function<Collection<T>, T> function, Channel<T> target, T defaultValue,
			@SuppressWarnings("unchecked") Channel<T>... sources) {
		this.function = function;
		/*
		 * Listen to Channel updates
		 */
		for (Channel<T> source : sources) {
			source.onUpdate(value -> {
				synchronized (this.lastValues) {
					if (value != null) {
						this.lastValues.put(source, value);
					} else {
						this.lastValues.put(source, defaultValue);
					}
					T functionResult = this.function.apply(this.lastValues.values());
					try {
						target.setNextValue(functionResult);
					} catch (OpenemsException e) {
						StringBuilder b = new StringBuilder("Unable to merge Channel [" + target.address() + "] from ");
						for (int i = 0; i < sources.length; i++) {
							b.append(sources[i].address());
							if (i < sources.length - 1) {
								b.append(",");
							}
						}
						b.append("]: " + e.getMessage());
						log.error(b.toString());
					}
				}
			});
		}
	}
}
