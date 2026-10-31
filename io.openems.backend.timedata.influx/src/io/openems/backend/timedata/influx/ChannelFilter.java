package io.openems.backend.timedata.influx;

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
				ImmutableSet.copyOf(blacklistedChannelAddresses), //
				ImmutableSet.copyOf(blacklistedChannelIds));
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
		if (!isValidComponentId(channelAddress, 0, indexOfDelimiter)) {
			return false;
		}

		// Blacklisted ChannelIds.
		final var channelIdStart = indexOfDelimiter + 1;
		if (channelIdStart == channelAddress.length()) {
			return false;
		}

		return this.blacklistedChannelIds.isEmpty() //
				|| !this.blacklistedChannelIds.contains(channelAddress.substring(channelIdStart));
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
		return componentId != null && isValidComponentId(componentId, 0, componentId.length());
	}

	/**
	 * Checks if the Component-ID in the range [from, to) of the given String is
	 * valid.
	 *
	 * @param s    the String containing the Component-ID
	 * @param from the inclusive start index of the Component-ID
	 * @param to   the exclusive end index of the Component-ID
	 * @return true if it is a valid id; else false
	 */
	private static boolean isValidComponentId(String s, int from, int to) {
		if (to - from < 2) {
			return false;
		}

		// core/singleton component
		if (s.charAt(from) == '_') {
			return isLatinLowerCaseLetter(s.charAt(from + 1)) //
					&& isLatinLetter(s.charAt(to - 1)) //
					&& hasOnlyValidChars(s, from + 2, to);
		}

		// factory component
		return isLatinLowerCaseLetter(s.charAt(from)) //
				&& isDecimalNumber(s.charAt(to - 1)) //
				&& hasOnlyValidChars(s, from + 1, to);
	}

	private static boolean hasOnlyValidChars(String s, int from, int to) {
		for (int i = from; i < to; i++) {
			if (!isValidChar(s.charAt(i))) {
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
