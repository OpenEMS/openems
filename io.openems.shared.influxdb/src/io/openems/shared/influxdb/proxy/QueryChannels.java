package io.openems.shared.influxdb.proxy;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.types.ChannelAddress;

/**
 * A set of {@link ChannelAddress}es proven safe to interpolate into an InfluxQL
 * query identifier.
 *
 * <p>
 * The private constructor plus the validating {@link #of(Set)} factory make it
 * impossible to hold a {@link QueryChannels} that contains an unsafe channel.
 * Query builders therefore only need to require this type: the compiler forces
 * callers through {@link #of(Set)}, which drops every channel that fails
 * validation. This makes injection defense a property of the data rather than
 * a step each builder has to remember.
 */
public final class QueryChannels implements Iterable<ChannelAddress> {

	private static final Logger LOG = LoggerFactory.getLogger(QueryChannels.class);

	/**
	 * Strict allowlist for a single query-identifier segment (Component-ID or
	 * Channel-ID). Rejects wildcards and every character that could break out of a
	 * double-quoted InfluxQL identifier (e.g. {@code "}, {@code '}, {@code ;},
	 * spaces, NUL).
	 */
	private static final Pattern SAFE_SEGMENT = Pattern.compile("^[A-Za-z0-9_\\-]+$");

	private final Set<ChannelAddress> channels;

	private QueryChannels(Set<ChannelAddress> channels) {
		this.channels = channels;
	}

	/**
	 * Validates and wraps the given channels. Non-conforming channels are dropped
	 * with a warning rather than failing the entire query, preserving availability
	 * for deployments with legacy channel names that contain characters outside the
	 * strict allowlist.
	 *
	 * @param channels the channels
	 * @return the validated {@link QueryChannels}
	 */
	public static QueryChannels of(Set<ChannelAddress> channels) {
		var safe = new LinkedHashSet<ChannelAddress>();
		for (var channel : channels) {
			if (isQuerySafe(channel)) {
				safe.add(channel);
			} else {
				LOG.warn("Dropping channel [{}] from query: does not match InfluxQL identifier allowlist [{}]",
						channel, SAFE_SEGMENT.pattern());
			}
		}
		return new QueryChannels(Collections.unmodifiableSet(safe));
	}

	private static boolean isQuerySafe(ChannelAddress channel) {
		var componentId = channel.getComponentId();
		var channelId = channel.getChannelId();
		return componentId != null && SAFE_SEGMENT.matcher(componentId).matches() //
				&& channelId != null && SAFE_SEGMENT.matcher(channelId).matches() //
				// ChannelAddress.fromString keeps the raw input as toString(); reject extra
				// segments that would otherwise ride along even when the two parts look clean
				&& channel.toString().equals(componentId + "/" + channelId);
	}

	/**
	 * Gets the validated channels as a {@link Stream}.
	 *
	 * @return a {@link Stream} of the channels
	 */
	public Stream<ChannelAddress> stream() {
		return this.channels.stream();
	}

	/**
	 * Checks whether there are no channels.
	 *
	 * @return true if there are no channels
	 */
	public boolean isEmpty() {
		return this.channels.isEmpty();
	}

	@Override
	public Iterator<ChannelAddress> iterator() {
		return this.channels.iterator();
	}

	/**
	 * Returns the channels as an unmodifiable {@link Set}.
	 *
	 * @return the channels as a {@link Set}
	 */
	public Set<ChannelAddress> toSet() {
		return this.channels;
	}
}
