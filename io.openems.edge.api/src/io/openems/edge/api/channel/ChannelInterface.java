package io.openems.edge.api.channel;

import java.util.Locale;
import java.util.Optional;

public interface ChannelInterface<T> {

	String getId();
	
	String getName(Locale locale);
	
	String getDescription(Locale locale);
	
	Class<T> getDataType();
	
	Unit getUnit();
	
	//List<RecordInterface<T>> getRecords(LocalDateTime startTime, LocalDateTime endTime);
	
	Optional<RecordInterface<T>> getCurrentRecord();
	
	void setCurrentRecord(RecordInterface<T> record);
	
	void addRecordChangedListener(RecordChangedListener<T> listener);
	
	void removeRecordChangedListener(RecordChangedListener<T> listener);
}
