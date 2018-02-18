package io.openems.backend.metadata.api;

import java.time.ZonedDateTime;

public interface OnSetLastMessage {
	public void call(ZonedDateTime lastMessage);
}
