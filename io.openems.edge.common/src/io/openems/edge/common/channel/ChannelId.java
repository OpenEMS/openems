package io.openems.edge.common.channel;

import com.google.common.base.CaseFormat;

/**
 * A {@link ChannelId} defines a Channel. It provides a unique Name and and a
 * {@link Doc}.
 *
 * <p>
 * This interface is typically implemented by an {@link Enum} type which
 * automatically provides a {@link ChannelId#name()} method.
 */
public interface ChannelId {

	/**
	 * The preferred way to define {@link ChannelId}s in OpenEMS Edge is via an
	 * {@code enum} that inherits {@code ChannelId}:
	 * 
	 * <pre>{@code
	 * public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
	 * 	MY_CHANNEL(Doc.of(OpenemsType.INTEGER));
	 * 
	 * 	private final Doc doc;
	 * 
	 * 	private ChannelId(Doc doc) {
	 * 		this.doc = doc;
	 * 	}
	 * 
	 * 	@Override
	 * 	public Doc doc() {
	 * 		return this.doc;
	 * 	}
	 * }
	 * }</pre>
	 * 
	 * <p>
	 * If instead you need to dynamically create ChannelIds at runtime - e.g.
	 * because at compile time you do not know the exact number of battery modules
	 * of a battery system - you may use {@link ChannelIdImpl} manually.
	 */
	public static record ChannelIdImpl(String name, Doc doc) implements ChannelId {
	}

	/**
	 * Converts a Channel-ID in UPPER_UNDERSCORE format (like from an {@link Enum})
	 * to the preferred UPPER_CAMEL format.
	 *
	 * <p>
	 * Examples: converts "ACTIVE_POWER" to "ActivePower".
	 *
	 * <p>
	 * Special reserved Channel-IDs starting with "_" have a special handling:
	 * "_PROPERTY_ENABLED" is converted to "_PropertyEnabled".
	 *
	 * @param name a Channel-ID in UPPER_UNDERSCORE format
	 * @return the Channel-ID in UPPER_CAMEL format.
	 */
	public static String channelIdUpperToCamel(String name) {
		if (name.startsWith("_")) {
			// special handling for reserved Channel-IDs starting with "_".
			return "_" + CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name.substring(1));
		}
		return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name);
	}

	/**
	 * Converts a Channel-ID in UPPER_CAMEL format to the UPPER_UNDERSCORE format.
	 *
	 * <p>
	 * Examples: converts "ActivePower" to "ACTIVE_POWER".
	 *
	 * @param name Channel-ID in UPPER_CAMEL format.
	 * @return the a Channel-ID in UPPER_UNDERSCORE format
	 */
	public static String channelIdCamelToUpper(String name) {
		if (name.startsWith("_")) {
			// special handling for reserved Channel-IDs starting with "_".
			return "_" + CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, name.substring(1));
		}
		return CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, name);
	}

	/**
	 * Gets the name in format {@link CaseFormat#UPPER_UNDERSCORE}. This is
	 * available by default for an Enum.
	 *
	 * <p>
	 * Names starting with underscore ("_") are reserved for internal usage.
	 *
	 * @return the name
	 */
	public String name();

	/**
	 * Gets the name in CamelCase.
	 *
	 * @return the Channel-ID in CamelCase
	 */
	default String id() {
		return channelIdUpperToCamel(this.name());
	}

	/**
	 * Gets the Channel Doc for this ChannelId.
	 *
	 * @return the Channel-Doc
	 */
	public Doc doc();

}
