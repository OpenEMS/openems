package io.openems.backend.metadata.api;

import java.time.ZonedDateTime;

public interface OnSetZonedDateTime {
	public void call(ZonedDateTime lastMessage);
}
