package io.openems.backend.timedata.influx;

import static java.util.Arrays.stream;

import com.google.common.collect.ImmutableSet;

public class ChannelFilter {

	/**
	 * Creates a new {@link ChannelFilter} from the provided arguments.
	 * 
	 * @param blacklistedChannelAddresses an array of blacklisted ChannelAddresses
	 * @param blacklistedChannelIds       an array of blacklisted ChannelIds
	 * @return the created {@link ChannelFilter}
	 */
	public static ChannelFilter from(String[] blacklistedChannelAddresses, String[] blacklistedChannelIds) {
		return new ChannelFilter(//
				stream(blacklistedChannelAddresses) //
						.collect(ImmutableSet.toImmutableSet()), //
				stream(blacklistedChannelIds) //
						.collect(ImmutableSet.toImmutableSet()));
	}

	private final ImmutableSet<String> blacklistedChannelAddresses;
	private final ImmutableSet<String> blacklistedChannelIds;

	private ChannelFilter(ImmutableSet<String> blacklistedChannelAddresses,
			ImmutableSet<String> blacklistedChannelIds) {
		this.blacklistedChannelAddresses = blacklistedChannelAddresses;
		this.blacklistedChannelIds = blacklistedChannelIds;
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

		// Blacklisted ChannelAddresses
		if (this.blacklistedChannelAddresses.contains(channelAddress)) {
			return false;
		}

		final var indexOfDelimiter = channelAddress.indexOf('/');
		if (indexOfDelimiter == -1) {
			return false;
		}

		// Valid Component-ID
		final var componentId = channelAddress.substring(0, indexOfDelimiter);
		if (!isValidComponentId(componentId)) {
			return false;
		}

		// Blacklisted ChannelIds.
		final var channelId = channelAddress.substring(indexOfDelimiter + 1);
		if (channelId.isEmpty() || this.blacklistedChannelIds.contains(channelId)) {
			return false;
		}

		return true;
	}

	/**
	 * Checks if the provided Component-ID is valid or not.
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
	 * 
	 * @param componentId the id to check
	 * @return true if it is a valid id; else false
	 */
	public static boolean isValidComponentId(String componentId) {
		if (componentId == null || componentId.length() < 2) {
			return false;
		}
		// core/singleton component
		if (componentId.startsWith("_")) {
			if (!isLatinLowerCaseLetter(componentId.charAt(1))) {
				return false;
			}

			for (int i = 2; i < componentId.length() - 1; i++) {
				if (!isValidChar(componentId.charAt(i))) {
					return false;
				}
			}

			if (!isLatinLetter(componentId.charAt(componentId.length() - 1))) {
				return false;
			}

			return true;
		}

		// factory component
		if (!isLatinLowerCaseLetter(componentId.charAt(0))) {
			return false;
		}

		var lastDigitIndex = -1;
		for (int i = componentId.length() - 1; i >= 0; i--) {
			final var c = componentId.charAt(i);
			if (!isDecimalNumber(c)) {
				break;
			}
			lastDigitIndex = i;
		}

		if (lastDigitIndex == -1) {
			return false;
		}

		for (int i = 1; i < lastDigitIndex; i++) {
			if (!isValidChar(componentId.charAt(i))) {
				return false;
			}
		}

		return true;
	}

	private static boolean isLatinLetter(char c) {
		return isLatinUpperCaseLetter(c) || isLatinLowerCaseLetter(c);
	}

	private static boolean isLatinUpperCaseLetter(char c) {
		return (c >= 'A' && c <= 'Z');
	}

	private static boolean isLatinLowerCaseLetter(char c) {
		return (c >= 'a' && c <= 'z');
	}

	private static boolean isDecimalNumber(char c) {
		return c >= '0' && c <= '9';
	}

	private static boolean isValidChar(char c) {
		return isLatinLetter(c) || isDecimalNumber(c);
	}

}
