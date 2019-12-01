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
	 * Gets the name in format {@link CaseFormat#UPPER_UNDERSCORE}. This is
	 * available by default for an Enum.
	 * 
	 * <p>
	 * Names starting with underscore ("_") are reserved for internal usage.
	 * 
	 * @return the name
	 */
	String name();

	/**
	 * Gets the name in CamelCase.
	 * 
	 * @return the Channel-ID in CamelCase
	 */
	default String id() {
		if (this.name().startsWith("_")) {
			// special handling for reserved Channel-IDs starting with "_".
			return "_" + CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.name().substring(1));
		} else {
			return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.name());
		}
	}

	/**
	 * Gets the Channel Doc for this ChannelId.
	 * 
	 * @return the Channel-Doc
	 */
	Doc doc();
}
