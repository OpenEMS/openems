package io.openems.edge.common.channel;

import com.google.common.base.CaseFormat;

public interface ChannelDoc {

	Unit getUnit();

	String name();

	default String id() {
		return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.name());
	}

}
