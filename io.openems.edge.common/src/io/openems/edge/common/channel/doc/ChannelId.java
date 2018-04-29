package io.openems.edge.common.channel.doc;

import com.google.common.base.CaseFormat;

public interface ChannelId {

	/**
	 * Gets the name. This is available by default for an Enum.
	 * 
	 * @return
	 */
	String name();

	/**
	 * Gets the name in CamelCase.
	 * 
	 * @return
	 */
	default String id() {
		return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.name());
	}

	/**
	 * Gets the Channel Doc for this ChannelId.
	 * 
	 * @return
	 */
	Doc doc();
}
