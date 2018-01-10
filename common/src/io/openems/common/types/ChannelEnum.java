package io.openems.common.types;

import java.util.Locale;

public interface ChannelEnum {
	int getValue();

	String getName(Locale locale);
}
