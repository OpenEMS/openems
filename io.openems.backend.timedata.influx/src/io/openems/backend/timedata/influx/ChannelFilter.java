package io.openems.backend.timedata.influx;

import static java.util.stream.Collectors.toUnmodifiableSet;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class ChannelFilter {

	private static final Predicate<String> SUNSPEC_PATTERN = //
			Pattern.compile("^S[0-9]+[A-Z][a-zA-Z0-9]*$").asPredicate();

	/**
	 * Pattern for Component-IDs.
	 * 
	 * <p>
	 * Either:
	 * 
	 * <ul>
	 * <li>starts with lower case letter
	 * <li>contains only ASCII letters and numbers
	 * <li>ends with a number
	 * </ul>
	 * 
	 * <p>
	 * Or:
	 * <ul>
	 * <li>starts with underscore (by convention for singleton Components)
	 * <li>continues with lower case letter
	 * <li>contains only ASCII letters and numbers
	 * <li>ends with a letter
	 * </ul>
	 */
	// TODO move to io.openems.common and validate pattern on Edge
	private static final Predicate<String> COMPONENT_ID_PATTERN = //
			Pattern.compile("^([a-z][a-zA-Z0-9]+[0-9]+|_[a-z][a-zA-Z0-9]+[a-zA-Z])$").asPredicate();

	/**
	 * Creates a new {@link ChannelFilter} from the provided arguments.
	 * 
	 * @param blacklistedChannels a array of the blacklisted channels
	 * @return the created {@link ChannelFilter}
	 */
	public static ChannelFilter from(String[] blacklistedChannels) {
		return new ChannelFilter(Arrays.stream(blacklistedChannels) //
				.collect(toUnmodifiableSet()));
	}

	private final Set<String> blacklistedChannels;

	private ChannelFilter(Set<String> blacklistedChannels) {
		super();
		this.blacklistedChannels = blacklistedChannels;
	}

	/**
	 * Checks if the provided channel is valid or not.
	 * 
	 * @param channelAddress the channel to check with the format
	 *                       "component0/Channel"
	 * @return true if the channel is valid; else false
	 */
	public boolean isValid(String channelAddress) {
		if (channelAddress == null) {
			return false;
		}

		if (this.blacklistedChannels.contains(channelAddress)) {
			return false;
		}

		final var c = channelAddress.split("/");
		if (c.length != 2) {
			return false;
		}

		// Valid Component-ID
		final var componentId = c[0];
		if (!COMPONENT_ID_PATTERN.test(componentId)) {
			return false;
		}

		// Valid Channel-ID
		final var channelId = c[1];
		if (SUNSPEC_PATTERN.test(channelId)) {
			// SunSpec Channels
			return false;
		}

		return true;
	}


}
