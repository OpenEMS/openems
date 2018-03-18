package io.openems.edge.api.channel;

import java.time.LocalDateTime;

public interface RecordInterface<T> {
	
	LocalDateTime getTimestamp();
	
	T getValue();
}
