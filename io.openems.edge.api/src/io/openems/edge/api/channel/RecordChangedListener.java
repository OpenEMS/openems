package io.openems.edge.api.channel;

public interface RecordChangedListener<T> {

	void recordChanged(RecordInterface<T> oldRecord, RecordInterface<T> newRecord);
	
}
